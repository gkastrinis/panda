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

	// The type of the constructed entity
	Relation entity

	Constructor(Functional f, Relation entity) {
		super(f.name, f.stage, f.keyExprs, f.valueExpr)
		this.entity = entity
	}

	def getEntity() {
		// TODO fix check
		//if (entity instanceof Relation)
			entity = new Entity(entity.name, valueExpr)
		return entity
	}

	Relation newAlias(String name, String stage, List<VariableExpr> vars) {
		new Constructor(super.newAlias(name, stage, vars))
	}

	def <T> T accept(IVisitor<T> v) { v.visit(this) }
}
