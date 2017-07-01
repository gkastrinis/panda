package org.clyze.deepdoop.datalog.element.relation

import groovy.transform.Canonical
import groovy.transform.ToString
import groovy.transform.TupleConstructor
import org.clyze.deepdoop.actions.IVisitor
import org.clyze.deepdoop.datalog.expr.VariableExpr

@Canonical
@ToString(includeSuper = true, includePackage = false)
@TupleConstructor(callSuper = true, includeSuperProperties = true, excludes = "stage")
class Primitive extends Relation {

	VariableExpr var

	int getArity() { 1 }

	Relation newRelation(String stage, List<VariableExpr> vars) {
		assert arity == vars.size()
		return this
	}

	Relation newAlias(String name, String stage, List<VariableExpr> vars) {
		throw new UnsupportedOperationException()
	}

	def <T> T accept(IVisitor<T> v) { v.visit(this) }

	static boolean isPrimitive(String name) {
		switch (name) {
			case "int":
			case "float":
			case "boolean":
			case "string":
				return true
			default:
				return false
		}
	}
}
