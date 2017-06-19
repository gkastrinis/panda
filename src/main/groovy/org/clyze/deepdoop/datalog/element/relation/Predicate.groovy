package org.clyze.deepdoop.datalog.element.relation

import groovy.transform.Canonical
import groovy.transform.ToString
import groovy.transform.TupleConstructor
import org.clyze.deepdoop.actions.IVisitor
import org.clyze.deepdoop.datalog.expr.IExpr
import org.clyze.deepdoop.datalog.expr.VariableExpr

@Canonical
@ToString(includeSuper = true, includePackage = false)
@TupleConstructor(callSuper = true, includeSuperProperties = true)
class Predicate extends Relation {

	List<IExpr> exprs

	int getArity() { exprs.size() }

	Relation newRelation(String stage, List<VariableExpr> vars) {
		newAlias(name, stage, vars)
	}

	Relation newAlias(String name, String stage, List<VariableExpr> vars) {
		assert arity == vars.size()
		return new Predicate(name, stage, [] + vars)
	}

	def <T> T accept(IVisitor<T> v) { v.visit(this) }
}
