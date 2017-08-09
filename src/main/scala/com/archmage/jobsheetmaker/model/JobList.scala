package com.archmage.jobsheetmaker.model

import java.io.File
import com.opencsv.CSVReader
import java.io.FileReader
import java.time.LocalDateTime
import scala.collection.mutable.ListBuffer
import java.time.format.DateTimeFormatter
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.time.LocalTime

// prolly gonna make this a singleton
object JobList {
	val jobs = ListBuffer[Job]()
	val columns = 12
	val stringLengthForDuration = 19

	def loadFromCSV(fileref: File): Boolean = {
		if (!fileref.exists()) return false
		val reader = new CSVReader(new FileReader(fileref))
		var nextLine = reader.readNext()
		if (nextLine == null || nextLine.length < columns ||
			nextLine(1) != "Start At" || nextLine(2) != "End At") return false
		while (nextLine != null) {
			if (nextLine(1) != "Start At" || nextLine(2) != "End At") {
				val datetime = LocalDateTime.parse(nextLine(1).substring(0, 19), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
				// println(nextLine(1).substring(0, 19) + " " + datetime)
				val client = new Client(nextLine(3), "", new Address(), "")
				val duration = if (nextLine(1).isEmpty() || nextLine(2).isEmpty() ||
					nextLine(1).length < stringLengthForDuration ||
					nextLine(2).length < stringLengthForDuration) {
					Duration.ofMinutes(0)
				} else {
					Duration.ofMinutes(ChronoUnit.MINUTES.between(
						LocalTime.parse(nextLine(1).substring(11, 19), DateTimeFormatter.ISO_LOCAL_TIME),
						LocalTime.parse(nextLine(2).substring(11, 19), DateTimeFormatter.ISO_LOCAL_TIME)))
				}
				val job = new Job(client, null, datetime, duration, "", "", nextLine(4).replaceAll("\n", "; "), false)
				jobs += (job)
			}
			nextLine = reader.readNext()
		}
		return true
	}
}