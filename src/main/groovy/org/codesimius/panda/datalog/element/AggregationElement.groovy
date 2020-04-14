package org.codesimius.panda.datalog.element

import groovy.transform.Canonical
import groovy.transform.ToString
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.element.relation.Type
import org.codesimius.panda.datalog.expr.VariableExpr

import static org.codesimius.panda.datalog.element.relation.Type.TYPE_INT

@Canonical
@ToString(includePackage = false)
class AggregationElement implements IElement {

	VariableExpr var
	Relation relation
	IElement body

	static final Set<String> SUPPORTED_PREDICATES = [
			"count",
			"min",
			"max",
			"sum"] as Set
	static final Map<String, List<Type>> PREDICATE_TYPES = [
			"count": [],
			"min"  : [TYPE_INT],
			"max"  : [TYPE_INT],
			"sum"  : [TYPE_INT]]
	static final Map<String, Type> PREDICATE_RET_TYPE = [
			"count": TYPE_INT,
			"min"  : TYPE_INT,
			"max"  : TYPE_INT,
			"sum"  : TYPE_INT]
}
