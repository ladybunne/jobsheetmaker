package com.archmage.jobsheetmaker

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.{ Scene, Parent }
import javafx.stage.Stage
import java.io.File

class MainApp extends Application {
	override def start(primaryStage: Stage): Unit = {
		var loader: FXMLLoader = null
		val fxmlFromJar = MainApp.getClass.getResource("/JobsheetMaker.fxml")
		if (fxmlFromJar != null) {
			loader = new FXMLLoader(fxmlFromJar)
		} else {
			// debugging
			loader = new FXMLLoader(new File("src/main/resources/JobsheetMaker.fxml").toURI.toURL)
		}
		val root: Parent = loader.load()
		MainApp.controller = loader.getController[Controller]
		MainApp.controller.stage = primaryStage
		val scene = new Scene(root)

		if (getParameters.getRaw.contains("/nogui")) {
			println("/nogui flag detected, running in no-gui mode.")
			MainApp.controller.exportAllWorkDays(false)
			System.exit(0)
		}

		primaryStage.setTitle("JobsheetMaker " + MainApp.VERSION)
		primaryStage.setScene(scene)
		primaryStage.show()
	}

	override def stop(): Unit = {
		MainApp.controller.saveOptions
	}
}

object MainApp {
	def main(args: Array[String]) {
		Application.launch(classOf[MainApp], args: _*)
	}

	var controller: Controller = _

	val VERSION = getClass.getPackage.getImplementationVersion
}