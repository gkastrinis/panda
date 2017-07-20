package org.clyze.deepdoop.datalog.element.relation

import groovy.transform.Canonical
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.clyze.deepdoop.actions.IVisitor
import org.clyze.deepdoop.datalog.expr.IExpr

@Canonical
@EqualsAndHashCode(callSuper = true)
@ToString(includeSuper = true, includePackage = false)
class Type extends Predicate {

	Type(String name, IExpr expr) {
		super(name, null, [expr])
	}

	def <T> T accept(IVisitor<T> v) { v.visit(this) }
}
