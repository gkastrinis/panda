package org.clyze.deepdoop.datalog.element.relation

import groovy.transform.Canonical
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.transform.TupleConstructor
import org.clyze.deepdoop.actions.IVisitor
import org.clyze.deepdoop.datalog.expr.IExpr
import org.clyze.deepdoop.datalog.expr.VariableExpr

@Canonical
@EqualsAndHashCode(callSuper = true)
@ToString(includeSuper = true, includePackage = false)
@TupleConstructor(callSuper = true, includeSuperProperties = true)
class Functional extends Relation {

	List<IExpr> keyExprs
	IExpr valueExpr

	int getArity() { (keyExprs ? keyExprs.size() : 0) + 1 }

	Relation newRelation(String stage, List<VariableExpr> vars) {
		newAlias(name, stage, vars)
	}

	Relation newAlias(String name, String stage, List<VariableExpr> vars) {
		assert arity == vars.size()
		def varsCopy = [] << vars
		def valueVar = varsCopy.pop() as VariableExpr
		return new Functional(name, stage, varsCopy, valueVar)
	}

	def <T> T accept(IVisitor<T> v) { v.visit(this) }
}
