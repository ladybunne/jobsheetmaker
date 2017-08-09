package com.archmage.jobsheetmaker.legacy

import java.io.File
import scala.collection.mutable.ListBuffer
import com.archmage.jobsheetmaker.model.WorkDay

object MainLegacy extends App {
	val workDays: ListBuffer[WorkDay] = ListBuffer()
	for (str <- args) workDays += WorkDay.loadFromCSV(new File(str))
	if (args.length <= 0) workDays += WorkDay.loadFromCSV(new File("input.csv"))
	for (workDay <- workDays)
		if (workDay != null) {
			println(workDay)
			workDay.exportCombined
		}
}
