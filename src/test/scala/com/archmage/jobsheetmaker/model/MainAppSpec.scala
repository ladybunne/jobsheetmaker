package com.archmage.jobsheetmaker.model

import javafx.application.Application

import com.archmage.jobsheetmaker.MainApp
import org.scalatest.FlatSpec

class MainAppSpec extends FlatSpec {

  "Starting a JobsheetMaker instance" should "succeed without throwing any exceptions." in {
    Application.launch(classOf[MainApp], "/quit")

    // this is a check for exceptions, so reaching this point is the success criteria
    assert(true)
  }

  // do a test for /nogui
}
