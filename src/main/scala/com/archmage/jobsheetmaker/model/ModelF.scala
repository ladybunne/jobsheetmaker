package com.archmage.jobsheetmaker.model

import java.io.File

import com.archmage.jobsheetmaker.model.cases.Job

case class ModelF(loader: SourceLoader = SourceLoader(), days: Set[WorkDay] = Set(), jobs: Set[Job] = Set()) {

  def exportCount(duplicates: Boolean = false, outputDir: File = null) = {
    days.count(d => {
      d.export.get && (!duplicates || d.checkExportExists(outputDir))
    })
  }

  def deleteWorkDay(day: WorkDay): ModelF = {
    if(days.contains(day)) {
      if(day.source != null) {
        // attempt to delete source
        val success = day.source.delete()
      }
      ModelF(loader, days - day)
    }
    else this
  }


}
