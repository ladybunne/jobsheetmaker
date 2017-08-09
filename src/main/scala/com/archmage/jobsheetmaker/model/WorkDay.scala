package com.archmage.jobsheetmaker.model

import java.io.File
import com.opencsv.CSVReader
import java.io.FileReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.collection.mutable.ListBuffer
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm
import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.apache.pdfbox.io.MemoryUsageSetting
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.Duration
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.BooleanProperty
import com.archmage.jobsheetmaker.Tools
import java.io.InputStream
import java.io.FileInputStream
import scala.util.matching.Regex.Replacement
import java.time.LocalDateTime

object WorkDay {
	def template: InputStream = Tools.getFirstExistingStream(
		Tools.getStreamIfFileExists(new File("Day Overview Template")),
		WorkDay.getClass.getResourceAsStream("/Day Overview Template.pdf"),
		Tools.getStreamIfFileExists(new File("src/main/resources/Day Overview Template.pdf"))) // final line is debugging

	val noWorker = new Worker
	val columnsInDayOverviewExport = 18
	val stringLengthForDuration = 19

	def setField: (PDAcroForm, String, String) => Boolean = (form: PDAcroForm, fieldName: String, value: String) => {
		for (elem <- Array(form, fieldName, value)) if (elem == null) false
		val field = form.getField(fieldName)
		if (field == null) false
		field.setValue(value)
		true
	}

	def loadFromCSV(fileref: File): WorkDay = {
		if (!fileref.exists()) return null
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
					else date = datetime.toLocalDate()
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
								if (worker == null) worker = new Worker(textWorker)
							}
						})
						if (worker == null) worker = new Worker("Unknown Worker")
					} else worker = new Worker(nextLine(2))
				}
				val address = new Address(nextLine(5), nextLine(6), nextLine(7), nextLine(8), nextLine(10))
				val client = new Client(nextLine(3), if (nextLine(4).isEmpty) nextLine(11) else nextLine(4), address, nextLine(14))
				val duration = if (nextLine(0).isEmpty() || nextLine(1).isEmpty() ||
					nextLine(0).length < stringLengthForDuration ||
					nextLine(1).length < stringLengthForDuration) {
					Duration.ofMinutes(0)
				} else {
					Duration.ofMinutes(ChronoUnit.MINUTES.between(
						LocalTime.parse(nextLine(0).substring(11, 19), DateTimeFormatter.ISO_LOCAL_TIME),
						LocalTime.parse(nextLine(1).substring(11, 19), DateTimeFormatter.ISO_LOCAL_TIME)))
				}
				val job = new Job(client, Array(worker), datetime, duration, nextLine(12), nextLine(16), nextLine(17).replaceAll("\n", "; "), nextLine(15) == "Yes")
				jobs += (job)
			}
			nextLine = reader.readNext()
		}
		if (worker == null) worker = noWorker
		reader.close
		new WorkDay(worker, date, jobs.toArray, fileref)
	}
}

class WorkDay(
	val worker: Worker,
	val date: LocalDate,
	val jobs: Array[Job],
	val source: File = null) {

	val export: BooleanProperty = new SimpleBooleanProperty(true)

	def outputOverview = {
		val document = PDDocument.load(WorkDay.template)
		val acroForm = document.getDocumentCatalog().getAcroForm()

		acroForm.getField("Title").setValue(s"Day Overview for ${worker.name}, ${
			date.format(DateTimeFormatter.ofPattern("EEEE dd/MM/uuuu"))
		}")
		var index = 0

		val fieldNames = Array("Client", "Contact", "Address", "Services", "Comments")

		for (job <- jobs) {
			if (!job.cancelled) {
				val fieldNames = Array("Client", "Contact", "Address", "Services", "Comments")
				val values = Array(job.client.name, job.client.phone, job.client.address.toString(), job.services, job.comments)
				for (i <- 0 to fieldNames.length - 1) {
					WorkDay.setField(acroForm, s"${fieldNames(i)}_${index + 1}", values(i))
				}
				index += 1
			}
		}

		//		for (i <- index to 6) {
		//			for (fieldName <- fieldNames) acroForm.getField(s"${fieldName}_${index + 1}").setNoExport(true)
		//		}

		document
	}

	def exportOverview = {
		outputOverview.save(s"${worker} - Overview, $date.pdf")
		outputOverview.close()
	}

	def outputIndividuals = {
		var output: ListBuffer[PDDocument] = ListBuffer()
		for (job <- jobs) output += job.outputJobsheet
		output
	}

	def exportIndividuals = {
		for (job <- jobs) job.exportJobsheet
	}

	def exportCombined: Unit = {
		exportCombined(new File("."), true)
	}

	def exportCombined(dir: File, replace: Boolean) = {
		if (export.get && worker.name != WorkDay.noWorker.name) {
			val exportFilename = s"${dir.getAbsolutePath}/${getExportFilename.apply}"
			if (replace || !new File(exportFilename).exists()) {
				val ut = new PDFMergerUtility
				val output = outputOverview
				val jobsTemp = outputIndividuals
				for (jobSheet <- jobsTemp) ut.appendDocument(output, jobSheet)
				ut.setDestinationFileName(getExportFilename.apply)
				ut.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())
				output.save(exportFilename)
				output.close()
				for (jobSheet <- jobsTemp) jobSheet.close()
			}
		}
	}

	def getExportFilename = () => {
		s"${worker.name} - Complete, ${date}.pdf"
	}

	def checkExportExists(dir: File): Boolean = {
		val exportFilename = s"${dir.getAbsolutePath}/${worker.name} - Complete, ${date}.pdf"
		new File(exportFilename).exists()
	}

	override def toString = {
		if (jobs.length == 0) { "" }
		else {
			var output: String = ""
			for (job <- jobs) output += s"$job\n"
			output.substring(0, output.length() - 1)
		}
	}
}