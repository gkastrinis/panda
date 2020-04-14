package org.codesimius.panda.system

import groovy.util.logging.Log4j
import org.antlr.v4.runtime.ANTLRFileStream
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.codesimius.panda.DatalogParserImpl
import org.codesimius.panda.actions.code.DefaultCodeGenerator
import org.codesimius.panda.datalog.DatalogLexer
import org.codesimius.panda.datalog.DatalogParser

import static org.codesimius.panda.system.Error.tag

@Log4j
class Compiler {

	static List<Artifact> artifacts

	static void compile(String filename, DefaultCodeGenerator codeGenerator) {
		try {
			log.info(tag("${new File(filename).canonicalPath} with ${codeGenerator.class.simpleName}", "COMPILE"))
			compile0(new ANTLRFileStream(filename), filename, codeGenerator)
			artifacts.each { log.info(tag(it.file.canonicalPath, it.kind as String)) }
		} catch (e) {
			(e instanceof PandaException) ? log.error(tag(e.message, "ERROR")) : log.error(e.message, e)
		}
	}

	static def compile0(ANTLRInputStream inputStream, String filename, def codeGenerator) {
		artifacts = []
		def listener = new DatalogParserImpl(filename)
		def parser = new DatalogParser(new CommonTokenStream(new DatalogLexer(inputStream)))
		ParseTreeWalker.DEFAULT.walk(listener, parser.program())

		codeGenerator.visit(listener.program)
		artifacts
	}
}
