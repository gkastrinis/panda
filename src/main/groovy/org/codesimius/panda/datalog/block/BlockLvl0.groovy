package org.codesimius.panda.datalog.block

import groovy.transform.Canonical
import groovy.transform.ToString
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration

@Canonical
@ToString(includePackage = false)
class BlockLvl0 implements IVisitable {

	Set<RelDeclaration> relDeclarations = [] as Set
	Set<TypeDeclaration> typeDeclarations = [] as Set
	Set<Rule> rules = [] as Set
}
