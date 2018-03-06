package org.clyze.deepdoop.actions

import org.clyze.deepdoop.datalog.IVisitable
import org.clyze.deepdoop.datalog.block.BlockLvl0
import org.clyze.deepdoop.datalog.block.BlockLvl1
import org.clyze.deepdoop.datalog.block.BlockLvl2
import org.clyze.deepdoop.datalog.element.relation.Type

class TypeInfoVisitingActor extends DefaultVisitor<IVisitable> implements TDummyActor<IVisitable> {

	Map<BlockLvl1, Map<Type, List<Type>>> superTypesOrdered = [:].withDefault { [:] }
	Map<BlockLvl1, Map<Type, Set<Type>>> subTypes = [:].withDefault { [:].withDefault { [] as Set } }
	Map<BlockLvl1, Map<Type, Type>> typeToRootType = [:].withDefault { [:] }

	private BlockLvl1 currBlock

	TypeInfoVisitingActor() { actor = this }

	IVisitable exit(BlockLvl2 n, Map m) { n }

	void enter(BlockLvl1 n) { currBlock = n }

	IVisitable exit(BlockLvl1 n, Map m) { currBlock = null }

	IVisitable exit(BlockLvl0 n, Map m) {
		n.typeDeclarations.each { d ->
			superTypesOrdered[currBlock][d.type] = []
			def currDecl = d
			while (currDecl.supertype) {
				superTypesOrdered[currBlock][d.type] << currDecl.supertype
				currDecl = n.typeDeclarations.find { it.type == currDecl.supertype }
			}

			superTypesOrdered[currBlock].each { t, superTs ->
				superTs.each { subTypes[currBlock][it] << t }

				typeToRootType[currBlock][t] = superTs ? superTs.last() : t
			}
		}
		return n
	}
}
