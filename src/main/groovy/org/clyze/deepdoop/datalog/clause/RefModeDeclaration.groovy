package org.clyze.deepdoop.datalog.clause

import groovy.transform.Canonical
import org.clyze.deepdoop.actions.IVisitor
import org.clyze.deepdoop.datalog.element.relation.Predicate
import org.clyze.deepdoop.datalog.element.relation.Primitive
import org.clyze.deepdoop.datalog.element.relation.RefMode

@Canonical
class RefModeDeclaration extends Declaration {

	RefModeDeclaration(RefMode refmode, Predicate entity, Primitive primitive) {
		super(refmode, [entity, primitive])
	}

	def <T> T accept(IVisitor<T> v) { v.visit(this) }
}
