package com.archmage.jobsheetmaker

import java.io.{File, FilenameFilter}

import com.archmage.jobsheetmaker.model.WorkDay

import scala.collection.mutable.ListBuffer

object Model {
	val inputFiles: ListBuffer[File] = ListBuffer()
	var workDays: ListBuffer[WorkDay] = ListBuffer()

	def exportCount = workDays.count(workDay => workDay.export.get)

	// prepare a directory's csv files for loading
	val prepareFilesInDir = (dir: File) => {
		if (!dir.exists || !dir.isDirectory) {
			false
		} else {
			val directory = dir.listFiles(new FilenameFilter() {
				override def accept(dir: File, name: String): Boolean = {
					name.endsWith(".csv") || name.endsWith(".xlsx")
				}
			})
			directory.foreach(file => inputFiles += file)
			true
		}
	}

	def loadWorkDay(file: File) = {
		val tempWorkDay = WorkDay.loadFromCSV(file)
		if (tempWorkDay != null && tempWorkDay.jobs.nonEmpty &&
			tempWorkDay.worker.name != WorkDay.noWorker.name) {
			Model.workDays += tempWorkDay
			tempWorkDay
		} else null
	}

	def deleteWorkDay(workDay: WorkDay, preserveDuplicates: Boolean, output: File) = {
		var outcome = ""
		if (!workDays.contains(workDay) || workDay.source == null) { outcome = "not found" }
		else if (!workDay.export.get) { outcome = "not set to export" }
		else if (preserveDuplicates && workDay.checkExportExists(output)) { outcome = "duplicate exists and respecting" }
		else if (workDay.source.delete) outcome = "deleted"
		else outcome = "failed to delete"
		println(s"$outcome ${workDay.source.getAbsolutePath}")
	}

	def clear = {
		inputFiles.clear
		workDays.clear
	}
}