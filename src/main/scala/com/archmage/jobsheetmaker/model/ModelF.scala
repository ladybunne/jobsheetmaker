package com.archmage.jobsheetmaker.model

import java.io.File

import com.archmage.jobsheetmaker.model.cases.Job

case class ModelF(loader: SourceLoader = SourceLoader(), days: Set[WorkDay] = Set(), jobs: Set[Job] = Set()) {

  def exportCount(duplicates: Boolean = false, outputDir: File = null): Int = {
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

  // run this on a separate thread
  def loadSourceFiles(onEach: () => Unit, onComplete: () => Unit): ModelF = {
    if(loader.csvFiles.isEmpty && loader.xlsxFiles.isEmpty) {
      onComplete.apply()
      this
    }
    else {
      // load csv workday
      if(loader.csvFiles.nonEmpty) {
        val day = loader.loadSingle(loader.csvFiles.head)
        onEach.apply()
        ModelF(SourceLoader(loader.csvFiles.tail, loader.xlsxFiles), days ++ day, jobs).loadSourceFiles(onEach, onComplete)
      }
      // load xlsx job report
      else {
        val loadedJobs = JobReport.load(loader.xlsxFiles.head).getOrElse(Set())
        onEach.apply()
        ModelF(SourceLoader(loader.csvFiles, loader.xlsxFiles.tail), days, jobs ++ loadedJobs).loadSourceFiles(onEach, onComplete)
      }
    }
  }
}
