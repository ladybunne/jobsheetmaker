package com.archmage.jobsheetmaker.model

import java.io.{File, FilenameFilter}

/**
  * case class for model file loading
  * handles enqueueing files to be loaded, and returning WorkDay cases
  * @param csvFiles - csv files, used as workday inputs
  * @param xlsxFiles - xlsx files, used for joblist inputs
  */
case class SourceLoader(csvFiles: Set[File] = Set(), xlsxFiles: Set[File] = Set()) {

  /**
    * enqueue all files in a directory to be loaded
    * @param dir - the directory to be enqueued
    * @return - a new case with the outcome
    */
  def addDir(dir: File): SourceLoader = {
    val csvs = SourceLoader.filterValidSourceFiles(dir, "csv").getOrElse(Set())
    val xlsxs = SourceLoader.filterValidSourceFiles(dir, "xlsx").getOrElse(Set())
    SourceLoader(csvFiles ++ csvs, xlsxFiles ++ xlsxs)
  }

  def loadSingle(file: File): Option[WorkDay] = WorkDay.loadFromCSV(file)

  def fileCount = csvFiles.size + xlsxFiles.size
}

object SourceLoader {
  /**
    * filter out non-csv non-xlsx files from a directory
    * @param dir - the directory
    * @return - an optional array of files, None if not a valid directory
    */
  def filterValidSourceFiles(dir: File, extension: String): Option[Set[File]] = {
    if(!dir.exists() || !dir.isDirectory) None
    else Option(dir.listFiles(new FilenameFilter() {
      override def accept(dir: File, name: String): Boolean = name.endsWith(s".$extension")
    }).toSet)
  }
}