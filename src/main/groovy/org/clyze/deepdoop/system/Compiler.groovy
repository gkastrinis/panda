package org.clyze.deepdoop.system

import org.antlr.v4.runtime.ANTLRFileStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.apache.commons.logging.LogFactory
import org.apache.log4j.ConsoleAppender
import org.apache.log4j.DailyRollingFileAppender
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.log4j.PatternLayout
import org.clyze.deepdoop.actions.LBCodeGenVisitingActor
import org.clyze.deepdoop.actions.SouffleCodeGenVisitingActor
import org.clyze.deepdoop.datalog.DatalogLexer
import org.clyze.deepdoop.datalog.DatalogListenerImpl
import org.clyze.deepdoop.datalog.DatalogParser

class Compiler {

	static List<Result> compileToLB(String filename, File outDir) {
		compile(filename, new LBCodeGenVisitingActor(outDir))
	}

	static List<Result> compileToSouffle(String filename, File outDir) {
		compile(filename, new SouffleCodeGenVisitingActor(outDir))
	}

	private static List<Result> compile(String filename, def codeGenActor) {
		initLogging("INFO", "./build/logs", true)

		def log = LogFactory.getLog(Compiler.class)
		log.info("[DD] COMPILE: $filename with ${codeGenActor.class.name}")

		try {
			def parser = new DatalogParser(
					new CommonTokenStream(
							new DatalogLexer(
									new ANTLRFileStream(filename))))
			def listener = new DatalogListenerImpl(filename)
			ParseTreeWalker.DEFAULT.walk(listener, parser.program())

			codeGenActor.visit(listener.program)
			return codeGenActor.results
		} catch (e) {
			log.error(e.message, e)
		}
		return null
	}

	private static void initLogging(String logLevel, String logDir, boolean console) {
		def dir = new File(logDir)
		if (!dir) dir.mkdir()

		def root  = Logger.rootLogger
		root.setLevel(Level.toLevel(logLevel, Level.WARN))
		root.addAppender(new DailyRollingFileAppender(new PatternLayout("%d [%t] %-5p %c - %m%n"), "$logDir/deepdoop.log", "'.'yyyy-MM-dd"))

		if (console)
			root.addAppender(new ConsoleAppender(new PatternLayout("%m%n")))
	}
}
