package com.archmage.jobsheetmaker.model

import javafx.application.Application

import com.archmage.jobsheetmaker.MainApp
import org.scalatest.FlatSpec

class ControllerSpec extends FlatSpec {

  "Starting a JobsheetMaker instance" should "succeed without throwing any exceptions." in {
    Application.launch(classOf[MainApp], "/quit")
    true
  }
}
