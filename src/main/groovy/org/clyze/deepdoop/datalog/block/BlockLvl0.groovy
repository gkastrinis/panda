package org.clyze.deepdoop.datalog.block

import groovy.transform.Canonical
import groovy.transform.ToString
import org.clyze.deepdoop.datalog.IVisitable
import org.clyze.deepdoop.datalog.clause.RelDeclaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.clause.TypeDeclaration

@Canonical
@ToString(includePackage = false)
class BlockLvl0 implements IVisitable {

	Set<RelDeclaration> relDeclarations = [] as Set
	Set<TypeDeclaration> typeDeclarations = [] as Set
	Set<Rule> rules = [] as Set
}
