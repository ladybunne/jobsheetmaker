package com.archmage.jobsheetmaker

import java.io._
import java.lang.Boolean
import java.nio.charset.Charset
import java.nio.file.{Files, Paths}
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}
import java.util.Scanner
import javafx.application.Platform
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.concurrent.Task
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.cell.CheckBoxTableCell
import javafx.scene.control.{Alert, MultipleSelectionModel, SelectionMode}
import javafx.scene.input.{KeyCode, KeyEvent, MouseEvent}
import javafx.scene.{control => jfxsc}
import javafx.stage.{DirectoryChooser, Modality, Stage}

import com.archmage.jobsheetmaker.model.{JobReport, ModelF, SourceLoader, WorkDay}

import scala.collection.JavaConversions
import scala.collection.mutable.ListBuffer

/**
	* The business.
	*/
class Controller {
	// -- fxml references --
	@FXML var labelCustomDirectory: jfxsc.Label = _
	@FXML var buttonRefresh: jfxsc.Button = _
	@FXML var buttonSetCustomFolder: jfxsc.Button = _
	@FXML var tableFiles: jfxsc.TableView[WorkDay] = _
	@FXML var columnDuplicate: jfxsc.TableColumn[WorkDay, String] = _
	@FXML var columnCheckbox: jfxsc.TableColumn[WorkDay, Boolean] = _
	@FXML var columnDate: jfxsc.TableColumn[WorkDay, LocalDate] = _
	@FXML var columnWorkerName: jfxsc.TableColumn[WorkDay, String] = _
	@FXML var columnJobs: jfxsc.TableColumn[WorkDay, Int] = _
	@FXML var progressBar: jfxsc.ProgressBar = _
	@FXML var labelStatus: jfxsc.Label = _
	@FXML var buttonOpenExportFolder: jfxsc.Button = _
	@FXML var buttonExport: jfxsc.Button = _
	@FXML var menuOptionsExportPrompt: jfxsc.CheckMenuItem = _
	@FXML var menuOptionsDeleteInput: jfxsc.CheckMenuItem = _
	@FXML var menuOptionsPreserveDuplicates: jfxsc.CheckMenuItem = _

	// -- variables for program logic --
	var model = ModelF()

	val inputDir = new File("input")
	val outputDir = new File("output")
	val logDir = new File("logs")

	var customDirectory: File = new File(System.getProperty("user.home") + "/Downloads/")

	val user = System.getProperty("user.name")
	val customDirFilename = s"options_$user.ini"

	var stage: Stage = _

	// -- helper functions - currently fucked atm --

	// performs a task on an iterable, while updating the progress bar's progress
	def doTaskWithProgress[T](list: Iterable[T], func: (T) => Unit, completion: () => Unit): Unit = {
		progressBar.setStyle("")
		var count = 0
		progressBar.setProgress(0)

		def taskClosure: Task[Unit] = () => {
			list.foreach(f = element => {
				func.apply(element)
				count += 1

				Platform.runLater(() => {
					progressBar.setProgress(Math.min(Math.max(count.floatValue / list.size, 0), 1))
					labelStatus.setText(s"Performing task $count/${list.size}...")
				})
			})

			Platform.runLater(() => {
				progressBar.setProgress(1)
				progressBar.setStyle("-fx-accent: MediumSeaGreen")
				completion.apply()
			})
		}

		val taskThread = new Thread(taskClosure)
		taskThread.start()
	}

	// attempts an unsafe operation, and handles it with a prompt on an error
	def tryWithErrorPrompt = (task: () => Unit) => {
		try {
			task.apply
		} catch {
			case ex: Throwable => {
				val alert = new Alert(AlertType.ERROR)
				alert.setTitle("Error")
				alert.setHeaderText(null)
				alert.initOwner(stage)
				alert.initModality(Modality.APPLICATION_MODAL)

				val logFile = new File(s"$logDir/error_${
					LocalDateTime.now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH_mm_ss"))
				}.log")
				logFile.createNewFile

				val fstream = new FileWriter(logFile, true)
				val out = new BufferedWriter(fstream)
				val pWriter = new PrintWriter(out, true)
				ex.printStackTrace(pWriter)

				alert.setContentText(s"""An error has occurred:
${ex.getClass.getCanonicalName}: ${ex.getMessage}
Please see the '${logFile.getName}` file in the 'logs' folder for more info.""")
				alert.showAndWait()
			}
		}
	}

	// -- setup / options functions --

	// ensure program directories exist
	def createProgramDirs() : Unit = Seq(inputDir, outputDir, logDir).foreach(dir => if (!dir.exists) dir.mkdir)

	// load options from file
	def loadOptions(): Unit = {
		val customDirFile = new File(customDirFilename)
		if (customDirFile.exists && !customDirFile.isDirectory) {
			val in = new Scanner(new FileReader(customDirFile))
			var line = ""
			while (in.hasNextLine) {
				line = in.nextLine
				if (line.startsWith("customDir=")) {
					val tempFile = new File(line.replace("customDir=", ""))
					if (tempFile.exists() && tempFile.isDirectory) {
						println("loaded custom directory from file")
						setCustomDir(tempFile)
					} else {
						println("could not load directory from file")
					}
				} else if (line.startsWith("exportPrompt=")) {
					menuOptionsExportPrompt.setSelected(line.endsWith("true"))
				} else if (line.startsWith("deleteInput=")) {
					menuOptionsDeleteInput.setSelected(line.endsWith("true"))
				} else if (line.startsWith("preserveDuplicates=")) {
					menuOptionsPreserveDuplicates.setSelected(line.endsWith("true"))
				}

			}
			in.close()
			println("loaded options from file")
		} else println("could not load options from file")

	}

	// save options to file
	def saveOptions(): Unit = {
		val customDirFile = new File(customDirFilename)
		if (!customDirFile.exists() || customDirFile.canWrite) {
			val output = ListBuffer(
				"[Options]",
				s"customDir=${customDirectory.getAbsolutePath}",
				s"exportPrompt=${menuOptionsExportPrompt.isSelected}",
				s"deleteInput=${menuOptionsDeleteInput.isSelected}",
				s"preserveDuplicates=${menuOptionsPreserveDuplicates.isSelected}")

			Files.write(Paths.get(customDirFilename), JavaConversions.asJavaIterable(output), Charset.forName("UTF-8"))
			println("saved custom directory to file")
		} else {
			println("could not save custom directory to file")
		}
	}

	// set custom directory
	def setCustomDir(dir: File) = {
		val exists = dir.exists && dir.isDirectory
		if (exists) {
			customDirectory = dir
			labelCustomDirectory.setText(s"Custom folder: ${customDirectory.getAbsolutePath}")
			saveOptions()
		}
		exists
	}

	// change custom directory
	def selectCustomDir(): Unit = {
		val chooser = new DirectoryChooser
		chooser.setTitle("Set New Custom Folder")
		chooser.setInitialDirectory(new File(System.getProperty("user.home")))
		val selectedDir = chooser.showDialog(stage)
		if (selectedDir != null) {
			setCustomDir(selectedDir)
			loadAll()
		}
	}

	// -- program logic --

	// prepare files for loading
	def prepareFiles(): Unit = {
		model = ModelF(
			SourceLoader().
				addDir(new File(".")).
				addDir(new File("input")).
				addDir(customDirectory)
		)
	}

	// load all prepared files
	def loadPreparedFiles(completion: () => Unit): Unit = {
		val fileCount = model.loader.fileCount
		val onEach = () => {
			Platform.runLater(() => {
				progressBar.setProgress(progressBar.getProgress + 1.0 / fileCount)
			})
		}

		new Thread(() => {
			val loaded = model.loadSourceFiles(onEach, completion)
			model = ModelF(model.loader, loaded.days, loaded.jobs)
		}).start()
	}

	// cross-reference job objects with jobs in the job list
	def crossReference(): Unit = {
		model = ModelF(model.loader,
			model.days.map(d => WorkDay(d.worker, d.date, jobs = d.jobs.map(j => {
				// get all equal jobs in model.jobs
				val matches = model.jobs.filter(j2 => j2.checkEquality(j))
				if(matches.isEmpty) j else j.appendComment(matches.head.comments.replace(j.comments, "") + "; ")
			}),
				d.source, d.export)),
			model.jobs)
	}

	// export all work days
	def exportAllWorkDays(confirmDialog: Boolean) = {
		tryWithErrorPrompt.apply(() => {
			val exportCountCache = model.exportCount()
			doTaskWithProgress[WorkDay](
				model.days, workDay => {
					if (menuOptionsDeleteInput.isSelected) {
						// do some business here
						// disabling this for now :)
						// model.deleteWorkDay(workDay, menuOptionsPreserveDuplicates.isSelected, outputDir)
					}
					workDay.exportCombined(outputDir, !menuOptionsPreserveDuplicates.isSelected)
				}, () => {
					val outcomeText = s"Finished exporting $exportCountCache jobsheet${if (exportCountCache != 1) "s" else ""}."
					labelStatus.setText(outcomeText)

					if (menuOptionsDeleteInput.isSelected || menuOptionsPreserveDuplicates.isSelected) loadAll()

					if (confirmDialog) {
						val alert = new Alert(AlertType.INFORMATION)
						alert.setTitle("Export Complete")
						alert.setHeaderText(null)
						alert.initOwner(stage)
						alert.initModality(Modality.APPLICATION_MODAL)
						alert.setContentText(outcomeText)
						alert.showAndWait()
					}
			})
		})
	}

	// prepare all file references, then load all files
	def loadAll(): Unit = {
		prepareFiles()
		loadPreparedFiles(() => ())
		crossReference()
		updateTable()
		updateExportUI()
	}

	// -- interface-modifying functions --

	def setStatusUIDefault(): Unit = {
		val dayCount = model.days.size
		if (dayCount > 0) {
			labelStatus.setText(s"$dayCount source file${if (dayCount != 1) "s" else ""} found, ${
				model.days.size} jobsheet${if (model.days.size != 1) "s" else ""} loaded.")
			progressBar.setStyle("-fx-accent: MediumSeaGreen")
		}
		else {
			labelStatus.setText("Ready")
			progressBar.setProgress(1)
			progressBar.setStyle("")
		}
	}

	// update table
	def updateTable(): Unit = {
		Platform.runLater(() => {
			tableFiles.setItems(FXCollections.observableList(JavaConversions.seqAsJavaList(model.days.toSeq)))
		})
	}

	// update export button and label
	def updateExportUI(): Unit = {
		columnDuplicate.setVisible(menuOptionsPreserveDuplicates.isSelected)
		if (model.exportCount() == 0) {
			if (model.exportCount(true, outputDir) != 0) {
				// handle duplicates
			}
			buttonExport.setText("Export (0)")
			buttonExport.setDisable(true)

		} else {
			buttonExport.setText(s"Export (${model.exportCount()})")
			buttonExport.setDisable(false)
		}
	}

	// -- javafx event handling --

	@FXML def buttonRefreshHover(event: MouseEvent): Unit = labelStatus.setText("Reload input files.")
	@FXML def buttonRefreshAction(event: ActionEvent): Unit = loadAll()

	@FXML def buttonSetCustomFolderHover(event: MouseEvent): Unit = labelStatus.setText("Set a new custom input folder.")
	@FXML def buttonSetCustomFolderAction(event: ActionEvent): Unit = selectCustomDir()

	@FXML def buttonOpenExportFolderHover(event: MouseEvent): Unit = labelStatus.setText("Open the folder where exported jobsheets go.")
	@FXML def buttonOpenExportFolderAction(event: ActionEvent): Unit = Runtime.getRuntime.exec(s"explorer.exe ${outputDir.getAbsolutePath}")

	@FXML def buttonExportHover(event: MouseEvent): Unit = labelStatus.setText("Export the selected jobsheets.")
	@FXML def buttonExportAction(event: ActionEvent): Unit = exportAllWorkDays(menuOptionsExportPrompt.isSelected)

	@FXML def toggleExportOnAllSelectedRows(event: KeyEvent): Unit = {
		if (event.getCode == KeyCode.SPACE) {
			JavaConversions.asScalaBuffer(
				tableFiles.getSelectionModel.asInstanceOf[MultipleSelectionModel[WorkDay]].
					getSelectedItems).foreach(workDay => workDay.export.setValue(!workDay.export.getValue))
		}
	}

	@FXML def elementUnhover(event: MouseEvent): Unit = setStatusUIDefault()

	// -- startup and shutdown code! --

	def initialize(): Unit = {
		tableFiles.getSelectionModel.setSelectionMode(SelectionMode.MULTIPLE)

		// cell factories
		columnCheckbox.setCellFactory(CheckBoxTableCell.forTableColumn[WorkDay](columnCheckbox))

		// cell value factories
		columnDuplicate.setCellValueFactory((p) => {
			new ReadOnlyObjectWrapper(if (p.getValue.checkExportExists(outputDir)) "!" else "")
		})
		columnCheckbox.setCellValueFactory((p) => p.getValue.export.asInstanceOf[ObservableValue[Boolean]])
		columnDate.setCellValueFactory((p) => new ReadOnlyObjectWrapper(p.getValue.date))
		columnWorkerName.setCellValueFactory((p) => new ReadOnlyObjectWrapper(p.getValue.worker.name))
		columnJobs.setCellValueFactory((p) => new ReadOnlyObjectWrapper(p.getValue.jobs.length))

		menuOptionsPreserveDuplicates.setOnAction(_ => updateExportUI())

		new Thread(() => {
			createProgramDirs()
			loadOptions()
			setCustomDir(customDirectory)
			loadAll()
		}).run()
	}

	@FXML
	def exitApplication(event: ActionEvent) {
		Platform.exit()
	}
}