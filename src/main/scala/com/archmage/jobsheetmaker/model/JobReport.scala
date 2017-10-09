package com.archmage.jobsheetmaker.model

import java.io._
import java.time.{Duration, LocalDateTime, LocalTime}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import com.archmage.jobsheetmaker.model.cases.{Address, Client, Job}
import org.apache.poi.openxml4j.exceptions.InvalidFormatException
import org.apache.poi.ss.usermodel.{CellType, Row, Workbook, WorkbookFactory}

import scala.collection.mutable.ListBuffer

/**
	* A helper class that loads job reports
	*/
object JobReport {
	val stringLengthForDuration = 19

	// this is Bad Practice, but makes sense here
	val columns = Vector("SiteFull Address", "Start At", "End At", "Which Clients",
		"Event Comment", "Pretty Items", "Which Users", "Charge", "Cancelled?",
		"Clients Telephones", "Clients Mobiles", "Confirmed by:")

	def validateFile(file: File): Boolean = {
		if(!file.exists() || !file.canRead || !file.isDirectory) return false
		if(file.getName.split(".").last != "xlsx") return false
		val fis = new FileInputStream(file)
		val outcome = validateWorkbook(WorkbookFactory.create(fis))
		fis.close()
		outcome
	}

	def validateWorkbook(workbook: Workbook): Boolean = {
		validateWorkbook(workbook.getSheetAt(0).getRow(0))
	}

	def validateWorkbook(row: Row): Boolean = {
		// match column length
		if(row.getLastCellNum != columns.length) return false
		else for (cellIndex <- 0 until row.getLastCellNum) {
			val cell = row.getCell(cellIndex)
			cell.getCellTypeEnum match {
				case CellType.STRING =>
					// match every single column value; this is inflexible by design
					if(cell.getStringCellValue != columns(cellIndex)) return false

				case _ =>
					// no match means not valid
					return false

			}
		}
		true
	}

	def loadLinesFromXLSX(fileref: File): Option[Vector[Vector[String]]] = {
		var inp: InputStream = null
		var lines = ListBuffer[Vector[String]]()
		try {
			inp = new FileInputStream(fileref)
			val workbook = WorkbookFactory.create(inp)

			// only checking one sheet!
			val sheet = workbook.getSheetAt(0)
			for(rowIndex <- 0 to sheet.getLastRowNum) {
				var line = ListBuffer[String]()
				val row = sheet.getRow(rowIndex)
				if(row != null) {
					if(rowIndex == 0 && !validateWorkbook(row)) return None
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
					lines += line.toVector
				}
			}

			Option(lines.toVector)
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
	def load(file: File): Option[Set[Job]] = {
		// get lines from the xlsx file
		if(!validateFile(file)) return None
		val lines = loadLinesFromXLSX(file).getOrElse(return None)

		// this could be recursive, but it's 1am
		// EDIT: holy crap I did it
		def parseLine(lines: Vector[Vector[String]], jobs: Set[Job] =  Set()): Set[Job] = {
			if(lines.head == columns) parseLine(lines.tail, jobs)
			else {
				// start parsing cells
				val datetime = LocalDateTime.parse(lines.head(1).substring(0, 19),
					DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
				val client = Client(lines.head(3), "", Address(), "")
				val duration = if (lines.head(1).isEmpty || lines.head(2).isEmpty ||
					lines.head(1).length < stringLengthForDuration ||
					lines.head(2).length < stringLengthForDuration) {
					Duration.ofMinutes(0)
				} else {
					Duration.ofMinutes(ChronoUnit.MINUTES.between(
						LocalTime.parse(lines.head(1).substring(11, 19), DateTimeFormatter.ISO_LOCAL_TIME),
						LocalTime.parse(lines.head(2).substring(11, 19), DateTimeFormatter.ISO_LOCAL_TIME)))
				}
				val job = Job(client, null, datetime, duration, "", "", lines.head(4).replaceAll("\n", "; "))
				if(lines.tail.isEmpty) jobs + job
				else parseLine(lines.tail, jobs + job)
			}
		}

		Option(parseLine(lines))
	}
}