package com.archmage.jobsheetmaker.model

import java.io.{File, FileReader, InputStream}
import java.time.{Duration, LocalDate, LocalDateTime, LocalTime}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javafx.beans.property.{BooleanProperty, SimpleBooleanProperty}

import com.archmage.jobsheetmaker.Tools
import com.archmage.jobsheetmaker.model.cases.{Address, Client, Job, Worker}
import com.opencsv.CSVReader
import org.apache.pdfbox.io.MemoryUsageSetting
import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm

import scala.collection.mutable.ListBuffer

/**
	* A day of work. Contains a worker, their jobs, the date of work and an optional source
	* fileref for deleting.
	*/
case class WorkDay(worker: Worker = Worker(), date: LocalDate = LocalDate.MIN, jobs: List[Job] = List(),
	source: File = null, export: BooleanProperty = new SimpleBooleanProperty(true)) {

	def outputOverview: PDDocument = {
		val document = PDDocument.load(WorkDay.template)
		val acroForm = document.getDocumentCatalog.getAcroForm

		acroForm.getField("Title").setValue(s"Day Overview for ${worker.name}, ${
			date.format(DateTimeFormatter.ofPattern("EEEE dd/MM/uuuu"))
		}")
		var index = 0

		val fieldNames = Array("Client", "Contact", "Address", "Services", "Comments")

		for (job <- jobs) {
			if (!job.cancelled) {
				val values = Array(job.client.name, job.client.phone, job.client.address.toString(), job.services, job.comments)
				for (i <- fieldNames.indices) {
					WorkDay.setField(acroForm, s"${fieldNames(i)}_${index + 1}", values(i))
				}
				index += 1
			}
		}

		document
	}

	def exportOverview(): Unit = {
		outputOverview.save(s"$worker - Overview, $date.pdf")
		outputOverview.close()
	}

	def outputIndividuals: ListBuffer[PDDocument] = {
		var output: ListBuffer[PDDocument] = ListBuffer()
		for (job <- jobs) output += job.outputJobsheet
		output
	}

	def exportIndividuals(): Unit = {
		for (job <- jobs) job.exportJobsheet
	}

	def exportCombined(): Unit = exportCombined(new File("."), true)

	def exportCombined(dir: File, replace: Boolean): Unit = {
		if (export.get && worker.name != WorkDay.noWorker.name) {
			val exportFilename = s"${dir.getAbsolutePath}/$getExportFilename"
			if (replace || !new File(exportFilename).exists()) {
				val ut = new PDFMergerUtility
				val output = outputOverview
				val jobsTemp = outputIndividuals
				for (jobSheet <- jobsTemp) ut.appendDocument(output, jobSheet)
				ut.setDestinationFileName(getExportFilename)
				ut.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())
				output.save(exportFilename)
				output.close()
				for (jobSheet <- jobsTemp) jobSheet.close()
			}
		}
	}

	def getExportFilename =  s"${worker.name} - Complete, $date.pdf"

	def checkExportExists(dir: File): Boolean = {
		val exportFilename = s"${dir.getAbsolutePath}/${worker.name} - Complete, $date.pdf"
		new File(exportFilename).exists()
	}
}

object WorkDay {
	def template: InputStream = Tools.getFirstExistingStream(
		Tools.getStreamIfFileExists(new File("Day Overview Template")),
		getClass.getResourceAsStream("/Day Overview Template.pdf"),
		// final line is debugging
		Tools.getStreamIfFileExists(new File("src/main/resources/Day Overview Template.pdf")))

	val noWorker = Worker()
	val columnsInDayOverviewExport = 18
	val stringLengthForDuration = 19

	def setField(form: PDAcroForm, fieldName: String, value: String): Boolean = {
		for (elem <- Array(form, fieldName, value)) if (elem == null) return false
		val field = form.getField(fieldName)
		if (field == null) return false
		field.setValue(value)
		true
	}

	def loadFromCSV(fileref: File): Option[WorkDay] = {
		if (!fileref.exists()) return None
		val reader = new CSVReader(new FileReader(fileref))
		var worker: Worker = null
		var date = LocalDate.MIN
		var jobs: ListBuffer[Job] = ListBuffer()
		var nextLine = reader.readNext()
		if (nextLine == null || nextLine.length < columnsInDayOverviewExport ||
			nextLine(0) != "Start" || nextLine(1) != "End") return null
		while (nextLine != null) {
			if (nextLine(0) != "Start" || nextLine(1) != "End") {
				val datetime = LocalDateTime.parse(nextLine(0).substring(0, 19), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
				if (date == LocalDate.MIN) {
					if (nextLine(0).length == 0) return null
					else date = datetime.toLocalDate
				}
				if (worker == null) {
					if (nextLine(2).split(" and ").length > 1) {
						var workers = nextLine(2).split(", ")
						workers = workers.dropRight(1) ++ workers.last.split(" and ")
						workers.foreach(textWorker => {
							val workerWithUnderscores = textWorker.trim.replace(" ", "_").replace("-", "_").toLowerCase
							println(workerWithUnderscores)
							println(workerWithUnderscores.replace("_", ""))
							if (fileref.getName.contains(workerWithUnderscores) ||
								fileref.getName.contains(workerWithUnderscores.replace("_", ""))) {
								println(textWorker)
								if (worker == null) worker = Worker(textWorker)
							}
						})
						if (worker == null) worker = Worker("Unknown Worker")
					} else worker = Worker(nextLine(2))
				}
				val address = Address(nextLine(5), nextLine(6), nextLine(7), nextLine(8), nextLine(10))
				val client = Client(nextLine(3), if (nextLine(4).isEmpty) nextLine(11) else nextLine(4), address, nextLine(14))
				val duration = if (nextLine(0).isEmpty || nextLine(1).isEmpty ||
					nextLine(0).length < stringLengthForDuration ||
					nextLine(1).length < stringLengthForDuration) {
					Duration.ofMinutes(0)
				} else {
					Duration.ofMinutes(ChronoUnit.MINUTES.between(
						LocalTime.parse(nextLine(0).substring(11, 19), DateTimeFormatter.ISO_LOCAL_TIME),
						LocalTime.parse(nextLine(1).substring(11, 19), DateTimeFormatter.ISO_LOCAL_TIME)))
				}
				val job = new Job(client, Array(worker), datetime, duration, nextLine(12), nextLine(16), nextLine(17).replaceAll("\n", "; "), nextLine(15) == "Yes")
				jobs += job
			}
			nextLine = reader.readNext()
		}
		if (worker == null) worker = noWorker
		reader.close()
		Option(WorkDay(worker, date, jobs.toList, fileref))
	}
}
