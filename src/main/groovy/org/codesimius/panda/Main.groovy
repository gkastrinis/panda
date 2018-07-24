package org.codesimius.panda

import org.codesimius.panda.system.Compiler
import org.codesimius.panda.system.Error

Error.initializeLogging()
Compiler.compileToLB3(args[0], "build/out")
Compiler.compileToSouffle(args[0], "build/out")
