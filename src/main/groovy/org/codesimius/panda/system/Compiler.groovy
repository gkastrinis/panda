package org.codesimius.panda.system

import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.codesimius.panda.DatalogParserImpl
import org.codesimius.panda.datalog.DatalogLexer
import org.codesimius.panda.datalog.DatalogParser

import static org.codesimius.panda.system.Log.info

class Compiler {
	static def run(ANTLRInputStream inputStream, String filename, def codeGenerator) {
		SourceManager.init()
		info("COMPILE", "${new File(filename).canonicalPath} with ${codeGenerator.class.simpleName}")
		def listener = new DatalogParserImpl(filename)
		def parser = new DatalogParser(new CommonTokenStream(new DatalogLexer(inputStream)))
		ParseTreeWalker.DEFAULT.walk(listener, parser.program())

		codeGenerator.visit(listener.program)
		codeGenerator.artifacts.each { info(it.kind as String, it.file.canonicalPath) }
	}
}
