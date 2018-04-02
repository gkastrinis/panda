package org.codesimius.panda.datalog.element

import groovy.transform.Canonical
import groovy.transform.ToString
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.expr.VariableExpr

@Canonical
@ToString(includePackage = false)
class AggregationElement implements IElement {

	VariableExpr var
	Relation relation
	IElement body
}
