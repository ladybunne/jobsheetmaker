package com.archmage.jobsheetmaker.model

class Client(
	val name: String,
	val phone: String,
	val address: Address,
	val groups: String) {

	override def toString = s"$name ($phone) at $address ($groups)"
}