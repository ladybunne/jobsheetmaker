package com.archmage.jobsheetmaker.legacy

import java.io.File
import java.time.LocalDate
import scala.collection.mutable.ListBuffer
import javafx.{ fxml => jfxf }
import javafx.{ scene => jfxs }
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import com.archmage.jobsheetmaker.model.WorkDay

object MainGUILegacy /* extends JFXApp */ {

	// javafx setup stuff
	val resource = new File("JobsheetMaker.fxml")
	val root: jfxs.Parent = jfxf.FXMLLoader.load(resource.toURI.toURL)

	// variables for program logic
	val filesToLoad: ListBuffer[File] = ListBuffer()
	val workDays: ListBuffer[WorkDay] = ListBuffer()
	val inputDir = new File("input")
	val outputDir = new File("output")
	var customDirectory: File = new File(System.getProperty("user.home") + "/Downloads/")
	val customDirFilename = "customdir.txt"

	// javafx element references
	val labelCustomDirectory = root.lookup("#labelCustomDirectory").asInstanceOf[Label]
	val buttonRefresh = root.lookup("#buttonRefresh").asInstanceOf[Button]
	val buttonSetCustomFolder = root.lookup("#buttonSetCustomFolder").asInstanceOf[Button]
	//	val tableFiles = root.lookup("#scrollTable").asInstanceOf[TableView[WorkDay]]
	//	val columnCheckbox: TableColumn[WorkDay, Boolean] = tableFiles.getColumns().filter(
	//		col => col.getId == "columnCheckbox")(0).asInstanceOf[TableColumn[WorkDay, Boolean]]
	//	val columnDate = tableFiles.getColumns().filter(
	//		col => col.getId == "columnDate")(0).asInstanceOf[TableColumn[WorkDay, LocalDate]]
	//	val columnWorkerName = tableFiles.getColumns().filter(
	//		col => col.getId == "columnWorkerName")(0).asInstanceOf[TableColumn[WorkDay, String]]
	//	val columnJobs = tableFiles.getColumns().filter(
	//		col => col.getId == "columnJobs")(0).asInstanceOf[TableColumn[WorkDay, Int]]
	val progressBar = root.lookup("#progressBar").asInstanceOf[ProgressBar]
	val labelStatus = root.lookup("#labelStatus").asInstanceOf[Label]
	val labelExport = root.lookup("#labelExport").asInstanceOf[Label]
	val buttonExport = root.lookup("#buttonExport").asInstanceOf[Button]

	// -- penultimate setup --

	// autosave on close
	//	override def stopApp {
	//		saveCustomDir.apply
	//		super.stopApp
	//	}

	// finally, create the UI and run it
	//	stage = new PrimaryStage() {
	//		title = "JobsheetMaker"
	//		scene = new Scene(root) {
	//			createProgramDirs.apply
	//			loadCustomDir.apply
	//			setCustomDir(customDirectory)
	//			loadAll.apply
	//			if (parameters.raw.filter(param => param == "/nogui").length > 0) {
	//				println("/nogui flag detected, running in no-gui mode.")
	//				exportAllWorkDays.apply
	//				System.exit(0)
	//			}
	//		}
	//	}
}