package com.archmage.jobsheetmaker.model

case class Address(line1: String = "", line2: String = "", line3: String = "", suburb: String = "",
	postcode: String = "", state: String = "NSW") {

	override def toString: String = {
		var output = ""
		for (str <- Array(line1, line2, line3) if str != "") output += str + ", "
		if (suburb != "") output += suburb + " "
		output += state
		if (postcode != "") output += " " + postcode
		output
	}
}