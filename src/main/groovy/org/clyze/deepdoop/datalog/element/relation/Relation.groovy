package org.clyze.deepdoop.datalog.element.relation

import groovy.transform.Canonical
import org.clyze.deepdoop.actions.IVisitor
import org.clyze.deepdoop.datalog.element.IElement
import org.clyze.deepdoop.datalog.expr.VariableExpr

// Generic base class that is also used when only the predicate name (and maybe stage) is known
@Canonical
class Relation implements IElement {

	String name

	String stage = null

	int getArity() { throw new UnsupportedOperationException() }

	Relation newRelation(String stage, List<VariableExpr> vars) { throw new UnsupportedOperationException() }

	Relation newAlias(String name, String stage, List<VariableExpr> vars) { throw new UnsupportedOperationException() }

	def <T> T accept(IVisitor<T> v) { v.visit(this) }
}
