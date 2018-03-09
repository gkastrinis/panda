package org.clyze.deepdoop.actions

import org.clyze.deepdoop.datalog.IVisitable
import org.clyze.deepdoop.datalog.block.BlockLvl0
import org.clyze.deepdoop.datalog.block.BlockLvl1
import org.clyze.deepdoop.datalog.block.BlockLvl2
import org.clyze.deepdoop.datalog.element.relation.Type

class TypeInfoVisitingActor extends DefaultVisitor<IVisitable> implements TDummyActor<IVisitable> {

	Map<Type, List<Type>> superTypesOrdered = [:]
	Map<Type, Set<Type>> subTypes = [:].withDefault { [] as Set }
	Map<Type, Type> typeToRootType = [:]

	Set<Type> getAllTypes() { typeToRootType.keySet() }

	TypeInfoVisitingActor() { actor = this }

	IVisitable exit(BlockLvl2 n, Map m) { n }

	IVisitable visit(BlockLvl1 n) { throw new UnsupportedOperationException() }

	void enter(BlockLvl0 n) {
		n.typeDeclarations.each { d ->
			superTypesOrdered[d.type] = []
			def currDecl = d
			while (currDecl.supertype) {
				superTypesOrdered[d.type] << currDecl.supertype
				currDecl = n.typeDeclarations.find { it.type == currDecl.supertype }
			}

			superTypesOrdered.each { t, superTs ->
				superTs.each { subTypes[it] << t }

				typeToRootType[t] = superTs ? superTs.last() : t
			}
		}
	}
}
