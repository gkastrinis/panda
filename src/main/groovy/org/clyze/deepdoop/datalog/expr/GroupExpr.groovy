package org.clyze.deepdoop.datalog.expr

import groovy.transform.Canonical
import groovy.transform.ToString
import org.clyze.deepdoop.actions.IVisitor

@Canonical
@ToString(includePackage = false)
class GroupExpr implements IExpr {

	@Delegate
	IExpr expr

	def <T> T accept(IVisitor<T> v) { v.visit(this) }
}
