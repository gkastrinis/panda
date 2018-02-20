package org.clyze.deepdoop.actions

import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.element.relation.Type

class TypeInfoVisitingActor extends PostOrderVisitor<IVisitable> implements TDummyActor<IVisitable> {

	Map<Component, Map<Type, List<Type>>> superTypesOrdered = [:].withDefault { [:] }
	Map<Component, Map<Type, Set<Type>>> subTypes = [:].withDefault { [:].withDefault { [] as Set } }
	Map<Component, Map<Type, Type>> typeToRootType = [:].withDefault{ [:] }

	TypeInfoVisitingActor() { actor = this }

	IVisitable exit(Program n, Map m) { n }

	IVisitable exit(Component n, Map m) {
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
