package org.codesimius.panda

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
	fw.close()
}

["LoadInstanceField", "StoreInstanceField", "LoadStaticField", "StoreStaticField",
 "LoadArrayIndex", "StoreArrayIndex", "AssignLocal", "AssignCast", "AssignInstanceOf",
 "Return", "ReturnVoid",
 "VirtualMethodInvocation", "StaticMethodInvocation", "SpecialMethodInvocation"].each { work it }
dropLast = true
work "AssignHeapAllocation"

fw = new FileWriter(new File("build/Instruction_Desc.facts"))
instructionsDescs.each { fw.write(it) }
fw.close()
fw = new FileWriter(new File("build/Instruction0.facts"))
instructions.each { fw.write(it) }
fw.close()


fw = new FileWriter(new File("build/FormalParam0.facts"))
new File("build/FormalParam.facts").eachLine {
	def (index, method, var) = it.split("\t")
	fw.write("$method\t$index\t$var\n")
}
fw.close()
fw = new FileWriter(new File("build/ActualParam0.facts"))
new File("build/ActualParam.facts").eachLine {
	def (index, invo, var) = it.split("\t")
	fw.write("$invo\t$index\t$var\n")
}
fw.close()
