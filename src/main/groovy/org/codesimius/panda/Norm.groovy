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


def swap = { oldName, newName, List<Integer> swaps ->
	fw = new FileWriter(new File("build/${newName}.facts"))
	new File("build/${oldName}.facts").eachLine {
		def parts = it.split("\t")
		def line = swaps ? swaps.collect { i -> parts[i] }.join("\t") : it
		fw.write("$line\n")
	}
	fw.close()
}
swap("DirectSuperinterface", "DirectSuperInterface", null)
swap("ComponentType", "ArrayComponentType", null)
swap("ThisVar", "Method_ThisVar", null)
swap("ArrayInsnIndex", "ArrayInstruction_IndexVar", null)
swap("Method-DeclaresException", "Method_DeclaresException", [1, 0])
swap("ClassModifier", "Class_Modifier", [1, 0])
swap("Method-Modifier", "Method_Modifier", [1, 0])
swap("Field-Modifier", "Field_Modifier", [1, 0])
swap("FormalParam", "FormalParam0", [1, 0, 2])
swap("ActualParam", "ActualParam0", [1, 0, 2])

def varType = [:]
def varMethod = [:]
new File("build/Var-Type.facts").eachLine {
	def (var, type) = it.split("\t")
	varType[var] = type
}
new File("build/Var-DeclaringMethod.facts").eachLine {
	def (var, method) = it.split("\t")
	varMethod[var] = method
}
def fw = new FileWriter(new File("build/Var0.facts"))
varType.each { var, type -> fw.write("$var\t$type\t${varMethod[var]}\n") }
fw.close()
