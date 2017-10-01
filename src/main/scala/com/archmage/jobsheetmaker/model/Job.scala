package com.archmage.jobsheetmaker.model

import java.io.{File, InputStream}
import java.time.{Duration, LocalDateTime}
import java.time.format.DateTimeFormatter

import com.archmage.jobsheetmaker.Tools
import org.apache.pdfbox.pdmodel.PDDocument

object Job {
	def template: InputStream = Tools.getFirstExistingStream(
		Tools.getStreamIfFileExists(new File("Single Job Template.pdf")),
		WorkDay.getClass.getResourceAsStream("/Single Job Template.pdf"),
		Tools.getStreamIfFileExists(new File("src/main/resources/Single Job Template.pdf"))) // final line is debugging
}

case class Job(
	client: Client,
	worker: Array[Worker],
	datetime: LocalDateTime,
	duration: Duration,
	services: String,
	confirmed: String = "",
	comments: String = "",
	cancelled: Boolean = false) {

	val date = datetime.toLocalDate

	override def toString = {
		var status = if (cancelled) "[CANCELLED] " else ""
		if (status == "" && confirmed == "") status += "[UNCONFIRMED] "
		s"$status$date ${client.name} - ${worker(0)}, $services"
	}

	def durationAsString = {
		val hours = duration.toHours
		val minutes = duration.minusHours(hours).toMinutes
		s"$hours:${if (minutes < 10) "0" else ""}$minutes"
	}

	def outputJobsheet = {
		val document = PDDocument.load(Job.template)
		val acroForm = document.getDocumentCatalog.getAcroForm
		val fieldNames = Array("Title", "Client", "Contact", "Confirmed", "Address", "Comments", "Duration")
		val values = Array(s"${client.name} - $services on ${
			date.format(DateTimeFormatter.ofPattern("EEEE dd/MM/uuuu"))
		} ", client.name, client.phone,
			confirmed, client.address.toString(), comments, durationAsString)
		for (i <- 0 until fieldNames.length) {
			WorkDay.setField(acroForm, fieldNames(i), values(i))
		}

		document
	}

	def exportJobsheet = {
		outputJobsheet.save(s"${worker(0)} - Job for ${client.name}, $date.pdf")
		outputJobsheet.close()
	}

	def checkEquality(other: Job) = {
		datetime == other.datetime && duration == other.duration && client.name == other.client.name
	}
}