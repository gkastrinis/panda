package org.clyze.deepdoop.datalog.expr

import groovy.transform.Canonical
import org.clyze.deepdoop.actions.IVisitor

@Canonical
class VariableExpr implements IExpr {

	String name

	def <T> T accept(IVisitor<T> v) { v.visit(this) }

	String toString() { name }
}
