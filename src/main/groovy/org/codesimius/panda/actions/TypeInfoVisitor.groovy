package org.codesimius.panda.actions

import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl0
import org.codesimius.panda.datalog.block.BlockLvl2
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.relation.Type

import static org.codesimius.panda.datalog.Annotation.CONSTRUCTOR
import static org.codesimius.panda.datalog.Annotation.TYPE

class TypeInfoVisitor extends DefaultVisitor<IVisitable> {

	Map<Type, List<Type>> superTypesOrdered = [:]
	Map<Type, Set<Type>> subTypes = [:].withDefault { [] as Set }
	Map<Type, Type> typeToRootType = [:]

	Set<Type> getAllTypes() { typeToRootType.keySet() }

	Set<Type> getRootTypes() { typeToRootType.values() as Set }

	// Types with no constructor in the hierarchy can be treated as symbols (strings)
	Set<Type> typesToOptimize = [] as Set
	Map<String, Type> constructorBaseType = [:]
	Map<Type, Set<RelDeclaration>> constructorsPerType = [:].withDefault { [] as Set }
	private Set<Type> typesWithDefaultCon = [] as Set

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
		rootTypes.each { root ->
			def types = [root] + subTypes[root]
			def constructors = types.collect { constructorsPerType[it] }.flatten()
			if (!constructors && !types.any { it in typesWithDefaultCon }) typesToOptimize += types
		}
		null
	}

	void enter(RelDeclaration n) {
		if (CONSTRUCTOR in n.annotations) {
			constructorBaseType[n.relation.name] = n.types.last()
			constructorsPerType[n.types.last()] << n
		}
	}

	void enter(TypeDeclaration n) {
		if (n.annotations.find { it == TYPE }.args["defaultConstructor"]) {
			typesWithDefaultCon << n.type
			constructorBaseType[n.type.defaultConName] = n.type
		}
	}

	void removeOptimizedTypes() {
		typesToOptimize.each { t ->
			superTypesOrdered.remove t
			subTypes.remove t
			typeToRootType.remove t
		}
	}
}
