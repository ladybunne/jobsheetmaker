package com.archmage.jobsheetmaker

import java.time.LocalDate
import java.time.Duration
import com.archmage.jobsheetmaker.model.Job
import com.archmage.jobsheetmaker.model.Worker
import com.archmage.jobsheetmaker.model.Client
import com.archmage.jobsheetmaker.model.Address
import com.archmage.jobsheetmaker.model.JobList
import java.io.File
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import com.opencsv.CSVReader
import java.io.FileReader
import scala.collection.mutable.ListBuffer

object Main extends App {

	def read(filename: String, function: (Array[String]) => Unit) = {
		val reader = new CSVReader(new FileReader(filename))
		var nextLine = reader.readNext()
		while (nextLine != null) {
			function.apply(nextLine)
			nextLine = reader.readNext()
		}
		reader.close
	}

	def items(input: Array[String]) = {
		if (input(0) != "Name")
			println(s"""[h1]${input(0)}[/h1]
				|[i]${input(1)}[/i]
				|[b]Description:[/b] ${input(2)}
				|[b]Type:[/b] ${input(3)}${if (!input(4).isEmpty) s"\n[b]Notes:[/b] ${input(4)}" else ""}
				|""".stripMargin)
	}

	def weapons(input: Array[String]) = {
		if (input(0) != "Name")
			println(s"""[h1]${input(0)}[/h1]
				|[i]${input(1)}[/i]
				|[b]Description:[/b] ${input(2)}
				|[b]Damage:[/b] ${input(3)}
				|[b]Range:[/b] ${input(4)}
				|[b]Speed:[/b] ${input(5)}${if (!input(6).isEmpty) s"\n[b]Notes:[/b] ${input(6)}" else ""}
				|""".stripMargin)
	}

	def subweapons(input: Array[String]) = {
		if (input(0) != "Name")
			println(s"""[h1]${input(0)}[/h1]
				|[i]${input(1)}[/i]
				|[b]Description:[/b] ${input(2)}
				|[b]Type:[/b] ${input(3)}
				|[b]Cost:[/b] ${input(4)}${if (!input(5).isEmpty) s"\n[b]Notes:[/b] ${input(5)}" else ""}
				|""".stripMargin)
	}

	def legacy(input: Array[String]) = {
		if (input(0) != "Name")
			println(s"""[h1]${input(0)}[/h1]
				|[b]Description:[/b] ${input(1)}${if (!input(2).isEmpty) s"\n[b]Notes:[/b] ${input(2)}" else ""}
				|""".stripMargin)
	}

	def characters(input: Array[String]) = {
		if (input(0) != "Name")
			println(s"""[h1]${input(0)}[/h1]
				|[b]Hearts:[/b] ${input(1)}
				|[b]Coins:[/b] ${input(2)}
				|[b]Ammo:[/b] ${input(3)}
				|[b]Keys:[/b] ${input(4)}
				|[b]Bombs:[/b] ${input(5)}
				|[b]Weapon:[/b] ${input(6)}
				|[b]Items:[/b] ${input(7)}${if (!input(8).isEmpty) s"\n[b]Notes:[/b] ${input(8)}" else ""}
				|""".stripMargin)
	}

	read("items.csv", items)
	read("weapons.csv", weapons)
	read("subweapons.csv", subweapons)
	read("legacy.csv", legacy)
	read("characters.csv", characters)
}
