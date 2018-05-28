package org.codesimius.panda.system

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.log4j.Level

class Logger {
	static final String prefix = "paNda"
	static Log logger = LogFactory.getLog(Error.class)

	static def log(String msg, def tag, Level lvl = Level.INFO, Throwable e = null) {
		msg = "[$prefix] $tag: $msg"
		if (lvl == Level.WARN)
			e ? logger.warn(msg, e) : logger.warn(msg)
		else if (lvl == Level.ERROR)
			e ? logger.error(msg, e) : logger.error(msg)
		else
			e ? logger.info(msg, e) : logger.info(msg)
	}
}
