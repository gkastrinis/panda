package org.clyze.deepdoop.actions

import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.element.relation.Type

import static org.clyze.deepdoop.datalog.Annotation.TYPE

class TypeHierarchyVisitingActor extends PostOrderVisitor<IVisitable> implements TDummyActor<IVisitable> {

	// Component x (Type x SuperType)
	Map<Component, Map<Type, Type>> types = [:]
	Map<Component, Map<Type, List<Type>>> superTypesOrdered = [:]
	Map<Component, Map<Type, Set<Type>>> subTypes = [:]
	// Component x (Type x Root Type)
	Map<Component, Map<Type, Type>> typeToRootType = [:]

	// Type x SuperType name
	Map<Type, String> tmpCurrentTypes

	TypeHierarchyVisitingActor() { actor = this }

	IVisitable exit(Program n, Map m) { n }

	void enter(Component n) {
		tmpCurrentTypes = [:]
	}

	IVisitable exit(Component n, Map m) {
		def directHierarchy = tmpCurrentTypes.collectEntries { t, superTName ->
			[(t): superTName ? tmpCurrentTypes.keySet().find { it.name == superTName } : null]
		} as Map

		types[n] = directHierarchy

		superTypesOrdered[n] = directHierarchy.keySet().collectEntries() { t ->
			def superTypes = []
			def currT = t
			def superT
			while ((superT = directHierarchy[currT])) {
				superTypes << superT
				currT = superT
			}
			[(t): superTypes]
		}

		subTypes[n] = [:].withDefault { [] as Set }
		superTypesOrdered[n].each { t, superTypes -> superTypes.each { subTypes[n][it] << t } }

		typeToRootType[n] = superTypesOrdered[n].collectEntries { t, superTypes ->
			[(t): superTypes ? superTypes.last() : t]
		} as Map

		null
	}

	IVisitable exit(Declaration n, Map m) {
		if (TYPE in n.annotations)
			tmpCurrentTypes[n.relation as Type] = n.types ? n.types.first().name : null
		null
	}
}
