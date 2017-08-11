package com.archmage.jobsheetmaker

import java.io.{FileInputStream, FileNotFoundException, IOException, InputStream}

import org.apache.poi.openxml4j.exceptions.InvalidFormatException
import org.apache.poi.ss.usermodel.{CellType, WorkbookFactory}

object Main extends App {
	var inp: InputStream = null
	try {
		inp = new FileInputStream("job_report.xlsx")
		val workbook = WorkbookFactory.create(inp)
		for(sheetIndex <- 0 until workbook.getNumberOfSheets) {
			val sheet = workbook.getSheetAt(sheetIndex)
			for(rowIndex <- 0 to sheet.getLastRowNum) {
				val row = sheet.getRow(rowIndex)
				for(cellIndex <- 0 until row.getLastCellNum) {
					val cell = row.getCell(cellIndex)
					cell.getCellTypeEnum match {
						case CellType.NUMERIC => print(cell.getNumericCellValue)
						case CellType.STRING => print(cell.getStringCellValue)
						case CellType.BLANK => print("")
						case _ => print(cell.getCellTypeEnum.name)
					}
					print("\t")
				}
				println()
			}
		}
	}
	catch {
		case _: InvalidFormatException => {}
		case _: FileNotFoundException => {}
		case _: IOException => {}
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
