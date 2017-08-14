package com.archmage.jobsheetmaker.model

import java.io._

import com.opencsv.CSVReader
import java.time.LocalDateTime

import scala.collection.mutable.ListBuffer
import java.time.format.DateTimeFormatter
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.time.LocalTime

import org.apache.poi.openxml4j.exceptions.InvalidFormatException
import org.apache.poi.ss.usermodel.{CellType, WorkbookFactory}

import collection.JavaConverters._

// prolly gonna make this a singleton
object JobList {
	val jobs = ListBuffer[Job]()
	val columns = 12
	val stringLengthForDuration = 19

	def loadLinesFromCSV(fileref: File): Seq[Seq[String]] = {
		if (!fileref.exists()) return Seq()
		val reader = new CSVReader(new FileReader(fileref))
		reader.readAll().asScala.map(line => line.toSeq)
	}

	def loadLinesFromXLSX(fileref: File): Seq[Seq[String]] = {
		var inp: InputStream = null
		var lines = ListBuffer[Seq[String]]()
		var line = ""
		try {
			inp = new FileInputStream(fileref)
			val workbook = WorkbookFactory.create(inp)
			for(sheetIndex <- 0 until workbook.getNumberOfSheets) {
				val sheet = workbook.getSheetAt(sheetIndex)
				for(rowIndex <- 0 to sheet.getLastRowNum) {
					var line = ListBuffer[String]()
					val row = sheet.getRow(rowIndex)
					for(cellIndex <- 0 until row.getLastCellNum) {
						val cell = row.getCell(cellIndex)
						cell.getCellTypeEnum match {
							case CellType.NUMERIC => line += cell.getNumericCellValue.toString
							case CellType.STRING => line += cell.getStringCellValue
							case CellType.BLANK => ()
							case _ => print(cell.getCellTypeEnum.name)
						}
					}
					lines += line.toSeq
				}
			}
			lines
		}
		catch {
			case _: InvalidFormatException => Seq()
			case _: FileNotFoundException => Seq()
			case _: IOException => Seq()
		}
		finally {
			try {
				inp.close()
			}
			catch {
				case _: IOException => {}
			}
		}
	}

	// only works on XLSX or plaintext files!
	def load(fileref: File): Boolean = {
		if (!fileref.exists()) return false
		var lines = Seq(Seq(""))
		if(fileref.getName.split('.').last == "xlsx") lines = loadLinesFromXLSX(fileref)
		else lines = loadLinesFromCSV(fileref)
		for(nextLine <- lines) {
			if (nextLine == lines.head) {
				if(nextLine == null || nextLine.length < columns ||
					nextLine(1) != "Start At" || nextLine(2) != "End At") return false
			}
			else if (nextLine(1) != "Start At" || nextLine(2) != "End At") {
				val datetime = LocalDateTime.parse(nextLine(1).substring(0, 19),
					DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
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
		}
		return true
	}
}