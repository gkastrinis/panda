package org.clyze.deepdoop

def instructionsDescs = [] as Set
def instructions = [] as Set
def res = [] as Set
def dropLast = false

def work = {
	new File("build/${it}.facts").eachLine {
		def parts = it.split("\t")
		def hash = parts[0].hashCode()
		instructionsDescs << "$hash\t${parts[0]}\n"
		def last = parts.last()
		instructions << "$hash\t${parts[1]}\t$last\n"
		if (dropLast) parts = parts.dropRight(1)
		res << "$hash\t${parts.drop(2).dropRight(1).join("\t")}\n"
	}
	def fw = new FileWriter(new File("build/${it}0.facts"))
	res.each { fw.write(it) }
	res = [] as Set
}

["LoadInstanceField", "StoreInstanceField", "LoadStaticField", "StoreStaticField",
 "LoadArrayIndex", "StoreArrayIndex", "AssignLocal", "AssignCast", "AssignInstanceOf",
 "Return", "ReturnVoid",
 "VirtualMethodInvocation", "StaticMethodInvocation", "SpecialMethodInvocation"].each { work it }
dropLast = true
work "AssignHeapAllocation"

fw = new FileWriter(new File("build/Instruction_Desc.facts"))
instructionsDescs.each { fw.write(it) }
fw = new FileWriter(new File("build/Instruction0.facts"))
instructions.each { fw.write(it) }
