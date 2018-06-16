package org.codesimius.panda

import org.apache.log4j.*
import org.codesimius.panda.system.Compiler

def logDir = new File("build/logs")
if (!logDir) logDir.mkdir()

def root = Logger.rootLogger
root.setLevel(Level.toLevel("INFO", Level.WARN))
root.addAppender(new DailyRollingFileAppender(new PatternLayout("%d [%t] %-5p %c - %m%n"), "$logDir/panda.log", "'.'yyyy-MM-dd"))
root.addAppender(new ConsoleAppender(new PatternLayout("%m%n")))

Compiler.compileToLB3(args[0], "build/out")
Compiler.compileToSouffle(args[0], "build/out")
