package org.codesimius.panda.datalog.block

import groovy.transform.Canonical
import groovy.transform.ToString
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.relation.Type

@Canonical
@ToString(includePackage = false)
class BlockLvl0 implements IVisitable {

	Set<RelDeclaration> relDeclarations = [] as Set
	Set<TypeDeclaration> typeDeclarations = [] as Set
	Set<Rule> rules = [] as Set

	private boolean infoCollected
	private Map<Type, List<Type>> superTypesOrdered0 = [:].withDefault { [] }
	private Map<Type, Set<Type>> subTypes0 = [:].withDefault { [] as Set }
	private Map<Type, Type> typeToRootType0 = [:]

	Map<Type, List<Type>> getSuperTypesOrdered() {
		if (!infoCollected) updateInfo()
		superTypesOrdered0
	}

	Map<Type, Set<Type>> getSubTypes() {
		if (!infoCollected) updateInfo()
		subTypes0
	}

	Map<Type, Type> getTypeToRootType() {
		if (!infoCollected) updateInfo()
		typeToRootType0
	}

	Set<Type> getAllTypes() {
		if (!infoCollected) updateInfo()
		typeToRootType0.keySet()
	}

	Set<Type> getRootTypes() {
		if (!infoCollected) updateInfo()
		typeToRootType0.values() as Set
	}

	void updateInfo() {
		typeDeclarations.each { d ->
			superTypesOrdered0[d.type] = []
			def currDecl = d
			while (currDecl.supertype) {
				superTypesOrdered0[d.type] << currDecl.supertype
				currDecl = typeDeclarations.find { it.type == currDecl.supertype }
			}

			superTypesOrdered0.each { t, ts ->
				ts.each { subTypes0[it] << t }

				typeToRootType0[t] = ts ? ts.last() : t
			}
		}
		infoCollected = true
	}
}
