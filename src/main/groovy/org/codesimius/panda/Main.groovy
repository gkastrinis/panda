package org.codesimius.panda

import org.antlr.v4.runtime.ANTLRFileStream
import org.codesimius.panda.actions.code.SouffleCodeGenerator
import org.codesimius.panda.system.Compiler

def compiler = new Compiler(
		SouffleCodeGenerator,
		new File(args[0]),
		new File("build/out_dl"),
		args.length == 2 ? new File(args[1]) : null,
		null)
try {
	compiler.run(new ANTLRFileStream(args[0]))
	compiler.codeGenerator.artifacts
			.findAll { it instanceof Compiler.LogicFile }
			.each { println it.text }
} catch (e) {
	compiler.error e
}