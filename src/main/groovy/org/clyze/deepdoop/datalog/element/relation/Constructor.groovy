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
		if (!(type instanceof Type))
			type = new Type(type.name, valueExpr)
		type
	}

	def <T> T accept(IVisitor<T> v) { v.visit(this) }
}
