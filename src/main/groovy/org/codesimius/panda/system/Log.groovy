package org.codesimius.panda.system

import java.text.MessageFormat
import java.text.SimpleDateFormat

class Log {

	static def DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
	static def LOG_FILE = new File("build/logs/panda.log")

	static {
		LOG_FILE.parentFile.mkdirs()
	}

	static void info(def tag = null, def msg) {
		msg = "${DATE_FORMAT.format(new Date())} $tag: $msg"
		System.out.println msg
		LOG_FILE.append msg
	}

	private static def prob(def tag, def loc = null, Error errorId, Object... values) {
		def rawMsg = "${MessageFormat.format(Error.msgMap[errorId], values)} -- [$errorId]"
		def msg = "${DATE_FORMAT.format(new Date())} $tag: $rawMsg${loc ? "\n$loc" : ""}"
		System.err.println msg
		LOG_FILE.append msg
		return rawMsg
	}

	static void warn(def loc = null, Error errorId, Object... values) {
		prob "WARNING", loc, errorId, values
	}

	static void error(def loc = null, Error errorId, Object... values) {
		def msg = prob "ERROR", loc, errorId, values
		throw new PandaException(msg, errorId)
	}

	static void error(Exception e) {
		def msg = e.message
		System.err.println msg
		LOG_FILE.append msg
		throw e
	}
}
