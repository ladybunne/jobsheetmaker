package com.archmage.jobsheetmaker.model

import org.scalatest.FlatSpec

class ModelSpec extends FlatSpec {

  // test cross-referencing

  // test loading correct files

  // test not loading incorrect files

  // test deleting workdays

  // test export count

  "A model" should "represent its export count correctly" in {
    val workdays = Set(WorkDay())
    val model = Model(SourceLoader(), workdays)
    assert(model.exportCount() == workdays.size)
  }

}
