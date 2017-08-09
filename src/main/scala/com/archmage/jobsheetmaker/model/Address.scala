package com.archmage.jobsheetmaker.model

class Address(
	val line1: String = "",
	val line2: String = "",
	val line3: String = "",
	val suburb: String = "",
	val postcode: String = "",
	val state: String = "NSW") {

	override def toString = {
		var output = ""
		for (str <- Array(line1, line2, line3) if str != "") output += str + ", "
		if (suburb != "") output += suburb + " "
		output += state
		if (postcode != "") output += " " + postcode
		output
	}
}