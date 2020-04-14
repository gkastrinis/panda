package org.codesimius.panda

import org.codesimius.panda.actions.code.SouffleCodeGenerator
import org.codesimius.panda.system.Compiler
import org.codesimius.panda.system.Error

Error.initializeLogging()
Compiler.compile(args[0], new SouffleCodeGenerator("build/out_dl"))