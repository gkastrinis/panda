package org.clyze.deepdoop.actions

import org.clyze.deepdoop.datalog.IVisitable
import org.clyze.deepdoop.datalog.block.BlockLvl0
import org.clyze.deepdoop.datalog.block.BlockLvl2
import org.clyze.deepdoop.datalog.element.relation.Type

class TypeInfoVisitingActor extends PostOrderVisitor<IVisitable> implements TDummyActor<IVisitable> {

	Map<BlockLvl0, Map<Type, List<Type>>> superTypesOrdered = [:].withDefault { [:] }
	Map<BlockLvl0, Map<Type, Set<Type>>> subTypes = [:].withDefault { [:].withDefault { [] as Set } }
	Map<BlockLvl0, Map<Type, Type>> typeToRootType = [:].withDefault{ [:] }

	TypeInfoVisitingActor() { actor = this }

	IVisitable exit(BlockLvl2 n, Map m) { n }

	IVisitable exit(BlockLvl0 n, Map m) {
		n.typeDeclarations.each { d ->
			superTypesOrdered[n][d.type] = []
			def currDecl = d
			while (currDecl.supertype) {
				superTypesOrdered[n][d.type] << currDecl.supertype
				currDecl = n.typeDeclarations.find { it.type == currDecl.supertype }
			}

			superTypesOrdered[n].each { t, superTs ->
				superTs.each { subTypes[n][it] << t }

				typeToRootType[n][t] = superTs ? superTs.last() : t
			}
		}
		return n
	}
}
