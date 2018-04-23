package org.codesimius.panda.actions.symbol

import org.codesimius.panda.actions.DefaultVisitor
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl0
import org.codesimius.panda.datalog.block.BlockLvl2
import org.codesimius.panda.datalog.element.relation.Type

class TypeInfoVisitor extends DefaultVisitor<IVisitable> {

	Map<Type, List<Type>> superTypesOrdered = [:]
	Map<Type, Set<Type>> subTypes = [:].withDefault { [] as Set }
	Map<Type, Type> typeToRootType = [:]

	Set<Type> getAllTypes() { typeToRootType.keySet() }

	Set<Type> getRootTypes() { typeToRootType.values() as Set }

	IVisitable exit(BlockLvl2 n) { n }

	IVisitable exit(BlockLvl0 n) {
		n.typeDeclarations.each { d ->
			superTypesOrdered[d.type] = []
			def currDecl = d
			while (currDecl.supertype) {
				superTypesOrdered[d.type] << currDecl.supertype
				currDecl = n.typeDeclarations.find { it.type == currDecl.supertype }
			}
			superTypesOrdered.each { t, ts ->
				ts.each { subTypes[it] << t }

				typeToRootType[t] = ts ? ts.last() : t
			}
		}
		null
	}
}
