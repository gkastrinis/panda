package org.clyze.deepdoop.datalog.element

import groovy.transform.Canonical
import groovy.transform.ToString
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.expr.VariableExpr

@Canonical
@ToString(includePackage = false)
class AggregationElement implements IElement {

	VariableExpr var
	Relation relation
	IElement body
}
