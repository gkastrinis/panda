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

	static List<Artifact> artifacts

	static List<Artifact> compileToLB3(String filename, String outDir) {
		compile(filename, new LBCodeGenerator(outDir))
	}

	static List<Artifact> compileToSouffle(String filename, String outDir) {
		compile(filename, new SouffleCodeGenerator(outDir))
	}

	static List<Artifact> compile(String filename, def codeGenActor) {
		try {
			log.info(tag("${new File(filename).canonicalPath} with ${codeGenActor.class.simpleName}", "COMPILE"))
			compile0(new ANTLRFileStream(filename), filename, codeGenActor)
			artifacts.each { log.info(tag(it.file.canonicalPath, it.kind as String)) }
		} catch (e) {
			(e instanceof PandaException) ? log.error(tag(e.message, "ERROR")) : log.error(e.message, e)
			null
		}
	}

	static List<Artifact> compile0(ANTLRInputStream inputStream, String filename, def codeGen) {
		def parser = new DatalogParser(new CommonTokenStream(new DatalogLexer(inputStream)))
		def listener = new DatalogParserImpl(filename)
		ParseTreeWalker.DEFAULT.walk(listener, parser.program())

		artifacts = []
		codeGen.visit(listener.program)
		artifacts
	}
}
