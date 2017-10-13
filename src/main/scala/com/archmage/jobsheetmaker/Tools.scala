package com.archmage.jobsheetmaker

import java.io.File
import java.io.InputStream
import java.io.FileInputStream

object Tools {
	def getFirstExistingStream(streams: InputStream*): InputStream = {
		var output: InputStream = null
		var existingFiles = streams.filter(stream => stream != null)
		if(existingFiles.nonEmpty) output = existingFiles.head
		output
	}

	def getStreamIfFileExists(file: File): InputStream = {
		if (file.exists) new FileInputStream(file.getPath) else null
	}
}