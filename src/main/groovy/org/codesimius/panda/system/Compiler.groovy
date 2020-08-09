package org.codesimius.panda.system

import org.antlr.v4.runtime.ANTLRFileStream
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.codesimius.panda.DatalogParserImpl
import org.codesimius.panda.actions.code.DefaultCodeGenerator
import org.codesimius.panda.datalog.DatalogLexer
import org.codesimius.panda.datalog.DatalogParser

import static org.codesimius.panda.system.Log.error
import static org.codesimius.panda.system.Log.info

class Compiler {

	static void run(String filename, DefaultCodeGenerator codeGenerator) {
		try {
			info("COMPILE", "${new File(filename).canonicalPath} with ${codeGenerator.class.simpleName}")
			run(new ANTLRFileStream(filename), filename, codeGenerator)
			codeGenerator.artifacts.each { info(it.kind as String, it.file.canonicalPath) }
		} catch (e) {
			error e
		}
	}

	static def run(ANTLRInputStream inputStream, String filename, def codeGenerator) {
		def listener = new DatalogParserImpl(filename)
		def parser = new DatalogParser(new CommonTokenStream(new DatalogLexer(inputStream)))
		ParseTreeWalker.DEFAULT.walk(listener, parser.program())

		codeGenerator.visit(listener.program)
	}
}
