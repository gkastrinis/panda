package org.codesimius.panda.system

import groovy.util.logging.Log4j
import org.antlr.v4.runtime.ANTLRFileStream
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.codesimius.panda.DatalogParserImpl
import org.codesimius.panda.actions.code.LBCodeGenerator
import org.codesimius.panda.actions.code.SouffleCodeGenerator
import org.codesimius.panda.datalog.DatalogLexer
import org.codesimius.panda.datalog.DatalogParser

import static org.codesimius.panda.system.Error.tag

@Log4j
class Compiler {

	static List<Result> compileToLB3(String filename, String outDir) {
		compile(filename, new LBCodeGenerator(outDir))
	}

	static List<Result> compileToSouffle(String filename, String outDir) {
		compile(filename, new SouffleCodeGenerator(outDir))
	}

	static List<Result> compile(String filename, def codeGenActor) {
		try {
			log.info(tag("$filename with ${codeGenActor.class.name}", "COMPILE"))
			return compile0(new ANTLRFileStream(filename), filename, codeGenActor)
		} catch (e) {
			log.error(tag(e.message, "ERROR"), e)
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
