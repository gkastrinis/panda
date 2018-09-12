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

	static final Set<String> SUPPORTED_PREDICATES = ["count", "min", "max", "sum"] as Set
	static final Map<String, Integer> PREDICATE_ARITIES = ["count": 0, "min": 1, "max": 1, "sum": 1]
}
