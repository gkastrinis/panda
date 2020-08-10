package org.codesimius.panda

import org.antlr.v4.runtime.ANTLRFileStream
import org.codesimius.panda.actions.code.SouffleCodeGenerator
import org.codesimius.panda.system.Artifact
import org.codesimius.panda.system.Compiler
import org.codesimius.panda.system.Log

Log.disableFileLog()
try {
	def gen = new SouffleCodeGenerator("build/out_dl")
	Compiler.run(new ANTLRFileStream(args[0]), args[0], gen)
	gen.artifacts.findAll { it.kind == Artifact.Kind.LOGIC }.each { println it.file.text }
} catch (e) {
	Log.error e
}