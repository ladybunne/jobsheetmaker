package com.archmage.jobsheetmaker.model.cases

case class Address(line1: String = "", line2: String = "", line3: String = "", suburb: String = "",
	postcode: String = "", state: String = "NSW") {

	override def toString: String = {
		val lines = Seq(line1, line2, line3).foldLeft("") { (c, n) =>
			// if n is blank, c
			if(n == "") c
			// if c and n are both not blank, ", "
			else c + (if(c != "") ", " else "") + n
		}
		s"$lines${if(suburb != "") s" $suburb" else ""} $state${if(postcode != "") s" $postcode" else ""}".trim
	}
}
