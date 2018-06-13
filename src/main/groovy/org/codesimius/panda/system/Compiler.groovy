package org.codesimius.panda.system

import org.antlr.v4.runtime.ANTLRFileStream
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.apache.log4j.Level
import org.codesimius.panda.DatalogParserImpl
import org.codesimius.panda.actions.code.LBCodeGenerator
import org.codesimius.panda.actions.code.SouffleCodeGenerator
import org.codesimius.panda.datalog.DatalogLexer
import org.codesimius.panda.datalog.DatalogParser

class Compiler {

	static List<Result> compileToLB3(String filename, String outDir) {
		compile(filename, new LBCodeGenerator(outDir))
	}

	static List<Result> compileToSouffle(String filename, String outDir) {
		compile(filename, new SouffleCodeGenerator(outDir))
	}

	static List<Result> compile(String filename, def codeGenActor) {
		try {
			Logger.log("$filename with ${codeGenActor.class.name}", "COMPILE")
			return compile0(new ANTLRFileStream(filename), filename, codeGenActor)
		} catch (e) {
			Logger.log(e.message, "ERROR", Level.ERROR, e)
			return null
		}
	}

	static List<Result> compile0(ANTLRInputStream inputStream, String filename, def codeGen) {
		def parser = new DatalogParser(new CommonTokenStream(new DatalogLexer(inputStream)))
		def listener = new DatalogParserImpl(filename)
		ParseTreeWalker.DEFAULT.walk(listener, parser.program())

		codeGen.visit(listener.program)
		codeGen.results
	}
}
