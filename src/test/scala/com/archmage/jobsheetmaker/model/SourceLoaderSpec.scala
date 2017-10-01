package com.archmage.jobsheetmaker.model

import java.io.File

import org.scalatest.FlatSpec

class SourceLoaderSpec extends FlatSpec {

  "An empty loader, given a directory containing files" should "enqueue all valid files" in {
    val loader = SourceLoader()
    val dir = new File("testfiles")
    val filteredFiles = SourceLoader.filterValidSourceFiles(dir, ".csv").
      getOrElse(Seq())
    val newLoader = loader.addDir(new File("testfiles"))
    assert(newLoader.csvFiles == filteredFiles)
  }
}
