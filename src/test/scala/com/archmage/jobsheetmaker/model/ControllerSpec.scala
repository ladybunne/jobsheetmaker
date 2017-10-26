package com.archmage.jobsheetmaker.model

import com.archmage.jobsheetmaker.Controller
import org.scalatest.FlatSpec

/**
  * testing actual UI interactions is difficult
  * instead, test the underlying functionality
  */
class ControllerSpec extends FlatSpec {

  // instantiate a controller and see if things don't blow up
  "An attempt to instantiate Controller" should "successfully produce an instance" in {
    new Controller()
    assert(true)
  }

  // test each button's function
  "Attempting to load all with an orphaned Controller" should "not work for some reason" in {
    val controller = new Controller()
    var exceptionCaught = false
    try {
      controller.loadAll()
    }
    catch {
      case Throwable => exceptionCaught = true
    }
    assert(exceptionCaught)
  }


  // test each menu option's function

  // test option saving and loading
}
