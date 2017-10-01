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

	def loadLinesFromXLSX(fileref: File): Option[Seq[Seq[String]]] = {
		var inp: InputStream = null
		var lines = ListBuffer[Seq[String]]()
		try {
			inp = new FileInputStream(fileref)
			val workbook = WorkbookFactory.create(inp)

			// only checking one sheet!
			val sheet = workbook.getSheetAt(0)
			for(rowIndex <- 0 to sheet.getLastRowNum) {
				var line = ListBuffer[String]()
				val row = sheet.getRow(rowIndex)
				if(row != null) {
					// check sheet headings
					if(rowIndex == 0) {
						if(row.getLastCellNum <= columns) return None
						if(!row.getCell(1).getStringCellValue.contains("Start") ||
							!row.getCell(2).getStringCellValue.contains("End")) return None
						// we've now verified that this file is valid
						// this should solve most of the problems
						// doing it here prevents fucky XML loading
						// it's a bit of a hack, it might be fixed later
					}
					for (cellIndex <- 0 until row.getLastCellNum) {
						val cell = row.getCell(cellIndex)
						cell.getCellTypeEnum match {
							case CellType.NUMERIC => line += cell.getNumericCellValue.toString
							case CellType.STRING => line += cell.getStringCellValue
							case CellType.FORMULA => line += cell.getCellFormula
							case CellType.BLANK => ()
							case _ => print(cell.getCellTypeEnum.name)
						}
					}
					lines += line
				}
			}

			Option(lines)
		}
		catch {
			case _: InvalidFormatException => None
			case _: FileNotFoundException => None
			case _: IOException => None
		}
		finally {
			try {
				inp.close()
			}
			catch {
				case _: IOException => ()
			}
		}
	}

	// only works on XLSX or plaintext files!
	def load(fileref: File): Boolean = {
		if (!fileref.exists()) return false
		println(fileref)
		var lines = Seq(Seq(""))
		if(fileref.getName.split('.').last == "xlsx") {
			val maybelines = loadLinesFromXLSX(fileref)
			if(maybelines.isEmpty) return false
			else lines = maybelines.getOrElse(Seq(Seq("")))
		}
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
				val duration = if (nextLine(1).isEmpty || nextLine(2).isEmpty ||
					nextLine(1).length < stringLengthForDuration ||
					nextLine(2).length < stringLengthForDuration) {
					Duration.ofMinutes(0)
				} else {
					Duration.ofMinutes(ChronoUnit.MINUTES.between(
						LocalTime.parse(nextLine(1).substring(11, 19), DateTimeFormatter.ISO_LOCAL_TIME),
						LocalTime.parse(nextLine(2).substring(11, 19), DateTimeFormatter.ISO_LOCAL_TIME)))
				}
				val job = new Job(client, null, datetime, duration, "", "", nextLine(4).replaceAll("\n", "; "), false)
				jobs += job
			}
		}
		true
	}
}