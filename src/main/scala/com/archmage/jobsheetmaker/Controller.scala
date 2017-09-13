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
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.collections.FXCollections
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.FXML
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.TableColumn.CellDataFeatures
import javafx.scene.control.cell.CheckBoxTableCell
import javafx.scene.control.{Alert, MultipleSelectionModel, SelectionMode}
import javafx.scene.input.{KeyCode, KeyEvent, MouseEvent}
import javafx.scene.{control => jfxsc}
import javafx.stage.{DirectoryChooser, Modality, Stage}
import javafx.util.Callback
import javafx.{fxml => jfxf, scene => jfxs}

import com.archmage.jobsheetmaker.model.{JobList, WorkDay}

import scala.collection.JavaConversions
import scala.collection.mutable.ListBuffer

class Controller {
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

	// variables for program logic
	val inputDir = new File("input")
	val outputDir = new File("output")
	val logDir = new File("logs")
	var customDirectory: File = new File(System.getProperty("user.home") + "/Downloads/")
	val user = System.getProperty("user.name")
	val customDirFilename = s"options_$user.ini"
	var stage: Stage = _

	// -- helper functions --

	// export counts, used for UI things
	def exportCount = Model.workDays.filter(workDay => workDay.export.get &&
		(!menuOptionsPreserveDuplicates.isSelected || !workDay.checkExportExists(outputDir))).length
	def exportCountPlusDuplicates = Model.workDays.filter(workDay => workDay.export.get).length

	// performs a task on an iterable, while updating the progress bar's progress
	def doTaskWithProgress[T] = (list: Iterable[T], func: (T) => Unit) => {
		progressBar.setStyle("")
		var count = 0
		progressBar.setProgress(0)

		def taskClosure = () => list.foreach(element => {
			func.apply(element)
			count += 1
			// set this up with a Runnable and a Platform.later
			progressBar.setProgress(Math.min(Math.max(count.floatValue / list.size, 0), 1))
			labelStatus.setText(s"Performing task ${count}/${list.size}...")
		})

		// replace this with a new Thread() thing
		taskClosure.apply()

		progressBar.setProgress(1)
		progressBar.setStyle("-fx-accent: MediumSeaGreen")
		true
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

				val logFile = new File(s"${logDir}/error_${
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

	// -- functions for program logic --

	// load all prepared files
	val loadPreparedFiles = () => {
		doTaskWithProgress[File].apply(Model.inputFiles.distinct, file => {
			val tempWorkDay = Model.loadWorkDay(file)
			JobList.load(file)
			if (tempWorkDay != null) tempWorkDay.export.addListener(new ChangeListener[Boolean]() {
				override def changed(observable: ObservableValue[_ <: Boolean],
					oldValue: Boolean, newValue: Boolean) = {
					updateExportUI.apply
				}
			})
		})
		setStatusLabelDefault.apply
	}

	// cross-reference job objects with jobs in the job list
	val crossReference = () => {
		doTaskWithProgress[WorkDay].apply(Model.workDays, workday => {
			workday.jobs.foreach(job => {
				val matches = JobList.jobs.filter(joblistjob => joblistjob.checkEquality(job))
				if (!matches.isEmpty && job.comments != matches(0).comments) {
					job.comments += (if (job.comments.isEmpty) "" else ";") + matches(0).comments
				}
			})
		})
		setStatusLabelDefault.apply
	}

	// update export button and label
	val updateExportUI = () => {
		columnDuplicate.setVisible(menuOptionsPreserveDuplicates.isSelected)
		if (exportCount == 0) {
			if (exportCountPlusDuplicates != 0) {
				// handle duplicates
			}
			buttonExport.setText("Export (0)")
			buttonExport.setDisable(true)

		} else {
			buttonExport.setText(s"Export (${exportCount})")
			buttonExport.setDisable(false)
		}
	}

	// export all work days
	val exportAllWorkDays = (confirmDialog: Boolean) => {
		tryWithErrorPrompt.apply(() => {
			val exportCountCache = exportCount
			val succeeded = doTaskWithProgress[WorkDay].apply(
				Model.workDays, workDay => {
					if (menuOptionsDeleteInput.isSelected) {
						Model.deleteWorkDay(workDay, menuOptionsPreserveDuplicates.isSelected, outputDir)
					}
					workDay.exportCombined(outputDir, !menuOptionsPreserveDuplicates.isSelected)
				})
			val outcomeText = s"Finished exporting ${exportCountCache} jobsheet${if (exportCountCache != 1) "s" else ""}."
			labelStatus.setText(outcomeText)

			if (menuOptionsDeleteInput.isSelected) {
				loadAll.apply
				progressBar.setStyle("-fx-accent: MediumSeaGreen")
			}

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
	}

	// prepare all file references, then load all files
	val loadAll = () => {
		tryWithErrorPrompt.apply(() => {
			Model.clear
			Model.prepareFilesInDir.apply(new File("."))
			Model.prepareFilesInDir.apply(new File("input"))
			Model.prepareFilesInDir.apply(customDirectory)
			loadPreparedFiles.apply
			crossReference.apply
			Model.workDays.foreach(day => day.jobs.foreach(job => println(job.toString)))
			progressBar.setStyle("")
			updateTable.apply
			updateExportUI.apply
		})
	}

	// update table
	val updateTable = () => {
		tableFiles.setItems(FXCollections.observableList(JavaConversions.bufferAsJavaList(Model.workDays.toBuffer)))
	}

	val loadOptions = () => {
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
			in.close
			println("loaded options from file")
		} else println("could not load options from file")

	}

	val setCustomDir = (dir: File) => {
		val exists = dir.exists && dir.isDirectory
		if (exists) {
			customDirectory = dir
			labelCustomDirectory.setText(s"Custom folder: ${customDirectory.getAbsolutePath}")
			saveOptions()
		}
		exists
	}

	val saveOptions = () => {
		val customDirFile = new File(customDirFilename)
		if (!customDirFile.exists() || customDirFile.canWrite()) {
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

	val createProgramDirs = () => List(inputDir, outputDir, logDir).foreach(dir =>
		if (!dir.exists) dir.mkdir)

	val selectCustomDir = () => {
		val chooser = new DirectoryChooser
		chooser.setTitle("Set New Custom Folder")
		chooser.setInitialDirectory(new File(System.getProperty("user.home")))
		val selectedDir = chooser.showDialog(stage)
		if (selectedDir != null) {
			setCustomDir(selectedDir)
			loadAll.apply
		}
	}

	val setStatusLabelDefault = () => {
		if (Model.inputFiles.length > 0) {
			labelStatus.setText(s"${Model.inputFiles.length} source file${
				if (Model.inputFiles.length != 1) "s" else ""
			} found, ${
				Model.workDays.length
			} jobsheet${if (Model.workDays.length != 1) "s" else ""} loaded.")
		} else labelStatus.setText("Ready")
	}

	// -- modifying javafx elements --

	@FXML def buttonRefreshAction(event: ActionEvent): Unit = loadAll.apply
	@FXML def buttonRefreshHover(event: MouseEvent): Unit = {
		labelStatus.setText("Reload input files.")
	}

	@FXML def buttonSetCustomFolderAction(event: ActionEvent): Unit = selectCustomDir.apply
	@FXML def buttonSetCustomFolderHover(event: MouseEvent): Unit = {
		labelStatus.setText("Set a new custom input folder.")
	}

	@FXML def buttonOpenExportFolderAction(event: ActionEvent): Unit = {
		Runtime.getRuntime().exec(s"explorer.exe ${outputDir.getAbsolutePath}")
	}
	@FXML def buttonOpenExportFolderHover(event: MouseEvent): Unit = {
		labelStatus.setText("Open the folder where exported jobsheets go.")
	}

	@FXML def buttonExportAction(event: ActionEvent): Unit = {
		exportAllWorkDays.apply(menuOptionsExportPrompt.isSelected)
	}
	@FXML def buttonExportHover(event: MouseEvent): Unit = {
		labelStatus.setText("Export the selected jobsheets.")
	}

	@FXML def toggleExportOnAllSelectedRows(event: KeyEvent): Unit = {
		if (event.getCode == KeyCode.SPACE) {
			JavaConversions.asScalaBuffer(
				tableFiles.getSelectionModel.asInstanceOf[MultipleSelectionModel[WorkDay]].
					getSelectedItems).foreach(workDay => workDay.export.setValue(!workDay.export.getValue))
		}
	}

	@FXML def elementUnhover(event: MouseEvent): Unit = {
		setStatusLabelDefault.apply
	}

	// -- startup and shutdown code! --

	def initialize(): Unit = {
		tableFiles.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE)

		columnDuplicate.setCellValueFactory(new Callback[CellDataFeatures[WorkDay, String], ObservableValue[String]]() {
			override def call(p: CellDataFeatures[WorkDay, String]): ObservableValue[String] = {
				new ReadOnlyObjectWrapper(if (p.getValue().checkExportExists(outputDir)) "!" else "")
			}
		})

		columnCheckbox.setCellFactory(CheckBoxTableCell.forTableColumn[WorkDay](columnCheckbox))

		columnCheckbox.setCellValueFactory(new Callback[CellDataFeatures[WorkDay, Boolean], ObservableValue[Boolean]]() {
			override def call(p: CellDataFeatures[WorkDay, Boolean]): ObservableValue[Boolean] = {
				return p.getValue().export.asInstanceOf[ObservableValue[Boolean]]
			}
		})

		columnDate.setCellValueFactory(new Callback[CellDataFeatures[WorkDay, LocalDate], ObservableValue[LocalDate]]() {
			override def call(p: CellDataFeatures[WorkDay, LocalDate]): ObservableValue[LocalDate] = {
				new ReadOnlyObjectWrapper(p.getValue.date)
			}
		})

		columnWorkerName.setCellValueFactory(new Callback[CellDataFeatures[WorkDay, String], ObservableValue[String]]() {
			override def call(p: CellDataFeatures[WorkDay, String]): ObservableValue[String] = {
				new ReadOnlyObjectWrapper(p.getValue.worker.name)
			}
		})

		columnJobs.setCellValueFactory(new Callback[CellDataFeatures[WorkDay, Int], ObservableValue[Int]]() {
			override def call(p: CellDataFeatures[WorkDay, Int]): ObservableValue[Int] = {
				new ReadOnlyObjectWrapper(p.getValue.jobs.length)
			}
		})

		menuOptionsPreserveDuplicates.setOnAction(new EventHandler[ActionEvent] {
			override def handle(event: ActionEvent) = {
				updateExportUI.apply
			}
		})

		createProgramDirs.apply
		loadOptions.apply
		setCustomDir(customDirectory)
		loadAll.apply
	}

	@FXML
	def exitApplication(event: ActionEvent) {
		Platform.exit
	}
}