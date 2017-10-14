package com.archmage.jobsheetmaker.model.cases

import java.io.{File, InputStream}
import java.time.format.DateTimeFormatter
import java.time.{Duration, LocalDate, LocalDateTime}

import com.archmage.jobsheetmaker.Tools
import com.archmage.jobsheetmaker.model.WorkDay
import org.apache.pdfbox.pdmodel.PDDocument

case class Job(client: Client = Client(), worker: Array[Worker] = Array(), datetime: LocalDateTime = LocalDateTime.MIN,
	duration: Duration = Duration.ZERO, services: String = "", confirmed: String = "", comments: String = "",
	cancelled: Boolean = false) {

	val date: LocalDate = datetime.toLocalDate

	override def toString: String = {
		var status = if (cancelled) "[CANCELLED] " else ""
		if (status == "" && confirmed == "") status += "[UNCONFIRMED] "
		s"$status$date ${client.name} - ${worker(0)}, $services"
	}

	def durationString: String = {
		val hours = duration.toHours
		val minutes = duration.minusHours(hours).toMinutes
		s"$hours:${if (minutes < 10) "0" else ""}$minutes"
	}

	def outputJobsheet: PDDocument = {
		val document = PDDocument.load(Job.template)
		val acroForm = document.getDocumentCatalog.getAcroForm
		val fieldNames = Array("Title", "Client", "Contact", "Confirmed", "Address", "Comments", "Duration")
		val values = Array(s"${client.name} - $services on ${
			date.format(DateTimeFormatter.ofPattern("EEEE dd/MM/uuuu"))
		} ", client.name, client.phone,
			confirmed, client.address.toString(), comments, durationString)
		for (i <- fieldNames.indices) {
			WorkDay.setField(acroForm, fieldNames(i), values(i))
		}

		document
	}

	def exportJobsheet(): Unit = {
		outputJobsheet.save(s"${worker(0)} - Job for ${client.name}, $date.pdf")
		outputJobsheet.close()
	}

	def checkEquality(other: Job): Boolean = {
		datetime == other.datetime &&
			duration == other.duration &&
			client.name == other.client.name
	}

	def appendComment(comment: String): Job = {
		Job(client, worker, datetime, duration, services, confirmed, comments + comment, cancelled)
	}
}

object Job {
	def template: InputStream = Tools.getFirstExistingStream(
		Tools.getStreamIfFileExists(new File("Single Job Template.pdf")),
		getClass.getResourceAsStream("/Single Job Template.pdf"),
		Tools.getStreamIfFileExists(new File("src/main/resources/Single Job Template.pdf"))) // final line is debugging
}