package com.archmage.jobsheetmaker.model

case class Client(name: String, phone: String, address: Address, groups: String) {
	override def toString = s"$name ($phone) at $address ($groups)"
}