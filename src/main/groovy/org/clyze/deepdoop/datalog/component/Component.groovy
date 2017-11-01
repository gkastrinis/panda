package org.clyze.deepdoop.datalog.component

import groovy.transform.Canonical
import groovy.transform.ToString
import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.actions.IVisitor
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule

@Canonical
@ToString(includePackage = false)
class Component implements IVisitable {

	String name
	String superComp
	Set<Declaration> declarations = [] as Set
	Set<Rule> rules = [] as Set

	void add(Component other) {
		declarations += other.declarations
		rules += other.rules
	}

	def <T> T accept(IVisitor<T> v) { v.visit(this) }
}
