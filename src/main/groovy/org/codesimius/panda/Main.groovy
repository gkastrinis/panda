package org.codesimius.panda

import org.codesimius.panda.system.Compiler

println Compiler.compileToLB3(args[0], "build")
println Compiler.compileToSouffle(args[0], "build")
