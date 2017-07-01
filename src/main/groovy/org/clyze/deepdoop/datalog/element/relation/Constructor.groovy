package org.clyze.deepdoop.datalog.element.relation

import groovy.transform.Canonical
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.clyze.deepdoop.actions.IVisitor
import org.clyze.deepdoop.datalog.expr.VariableExpr

@Canonical
@EqualsAndHashCode(callSuper = true)
@ToString(includeSuper = true, includePackage = false)
class Constructor extends Functional {

	// The constructed type
	Relation type

	Constructor(Functional f, Relation type) {
		super(f.name, f.stage, f.keyExprs, f.valueExpr)
		this.type = type
	}

	def getType() {
		// TODO fix check
		//if (type instanceof Relation)
		type = new Type(type.name, valueExpr)
		return type
	}

	Relation newAlias(String name, String stage, List<VariableExpr> vars) {
		new Constructor(super.newAlias(name, stage, vars))
	}

	def <T> T accept(IVisitor<T> v) { v.visit(this) }
}
