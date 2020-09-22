package org.codesimius.panda

import org.antlr.v4.runtime.ANTLRFileStream
import org.codesimius.panda.actions.code.SouffleCodeGenerator
import org.codesimius.panda.system.Artifact
import org.codesimius.panda.system.Compiler

def compiler = new Compiler(new File(args[0]).canonicalFile, SouffleCodeGenerator, "build/out_dl")
try {
	compiler.run(new ANTLRFileStream(args[0]))
	compiler.codeGenerator.artifacts
			.findAll { it.kind == Artifact.Kind.LOGIC }
			.each { println it.file.text }
} catch (e) {
	compiler.error e
}