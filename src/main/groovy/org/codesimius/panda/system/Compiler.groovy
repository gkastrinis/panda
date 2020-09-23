package org.codesimius.panda.system

import groovy.transform.InheritConstructors
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.codesimius.panda.DatalogParserImpl
import org.codesimius.panda.actions.code.DefaultCodeGenerator
import org.codesimius.panda.datalog.Annotation
import org.codesimius.panda.datalog.DatalogLexer
import org.codesimius.panda.datalog.DatalogParser
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration

import static org.codesimius.panda.datalog.Annotation.METADATA

class Compiler {
	@Delegate
	Log log
	@Delegate
	SourceManager sourceManager
	DefaultCodeGenerator codeGenerator

	Compiler(String logFilename = null, File mainFile, Class<? extends DefaultCodeGenerator> codeGenClass, String outDir) {
		log = new Log(logFilename)
		sourceManager = new SourceManager(mainFile)
		codeGenerator = codeGenClass.newInstance(this, outDir, mainFile) as DefaultCodeGenerator

		// Add a getAt method to the Set class to allow for search as in a map (i.e., set[element])
		Set.metaClass.getAt = { element -> delegate.find { it == element} }

		// Add a leftShift method to the Set class to allow for additional checks
		Set.metaClass.leftShift = { element ->
			if (element instanceof Annotation && element in delegate && element.isInternal)
				delegate[element].args += element.args
			else
				delegate.add element
			return delegate
		}
	}

	def run(ANTLRInputStream inputStream) {
		info("COMPILE", "${activeFiles.first()} with ${codeGenerator.class.simpleName}")

		def listener = new DatalogParserImpl(this)
		def parser = new DatalogParser(new CommonTokenStream(new DatalogLexer(inputStream)))
		ParseTreeWalker.DEFAULT.walk(listener, parser.program())

		codeGenerator.visit(listener.program)
		codeGenerator.artifacts.each { info(it.kind, it.canonicalPath) }
	}

	// Manually simulate what the delegate annotation would do, because recursive delegation does not
	// seem to work properly. Instead use the delegate annotation for a compiler object in other classes.

	void info(def tag = null, def msg) { log.info(tag, msg) }

	void warn(def loc = null, Error errorId, Object... values) { log.warn(loc, errorId, values) }

	void error(def loc = null, Error errorId, Object... values) { log.error(loc, errorId, values) }

	void error(Exception e) { log.error(e) }

	void enterInclude(File toFile, int fromLine) { sourceManager.enterInclude(toFile, fromLine) }

	void exitInclude() { sourceManager.exitInclude() }

	String rec(Object o, int line) { sourceManager.rec(o, line) }

	static String loc(RelDeclaration n) { n.annotations[METADATA]?.args?.loc }

	static String loc(TypeDeclaration n) { n.annotations[METADATA]?.args?.loc }

	static String loc(Rule n) { n.annotations[METADATA]?.args?.loc }

	String loc(Object o) { sourceManager.loc(o) }


	@InheritConstructors
	abstract static class Artifact extends File {
		static Artifact createUniqueFile(String prefix, String suffix, File parent) { null }

		abstract String getKind()
	}

	@InheritConstructors
	static class LogicFile extends Artifact {
		static Artifact createUniqueFile(String prefix, String suffix, File parent) {
			def raw = createTempFile(prefix, suffix, parent)
			new LogicFile(raw.canonicalPath)
		}

		String getKind() { "LOGIC" }
	}

	@InheritConstructors
	static class GraphFile extends Artifact {
		static Artifact createUniqueFile(String prefix, String suffix, File parent) {
			def raw = createTempFile(prefix, suffix, parent)
			new GraphFile(raw.canonicalPath)
		}

		String getKind() { "GRAPH" }
	}
}
