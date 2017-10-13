package com.archmage.jobsheetmaker.model

import java.io.File

import org.scalatest.FlatSpec

class SourceLoaderSpec extends FlatSpec {

  "An empty loader, given a directory containing files" should "enqueue all valid files" in {
    val dir = new File("testfiles")
    // testfiles
    val filteredFiles = SourceLoader.filterValidSourceFiles(dir, "csv").getOrElse(Seq())
    val loader = SourceLoader().addDir(new File("testfiles"))
    assert(loader.csvFiles == filteredFiles)
  }
}
