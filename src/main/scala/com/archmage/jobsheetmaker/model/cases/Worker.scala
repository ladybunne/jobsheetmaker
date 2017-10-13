package com.archmage.jobsheetmaker.model.cases

case class Worker(name: String = "[UNALLOCATED]") {
	override def toString: String = name
}