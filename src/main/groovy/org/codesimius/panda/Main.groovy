package org.codesimius.panda

import org.codesimius.panda.system.Compiler

println Compiler.compileToLB3(args[0], new File("build"))
println Compiler.compileToSouffle(args[0], new File("build"))
