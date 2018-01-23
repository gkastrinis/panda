package org.clyze.deepdoop.datalog.element.relation

import groovy.transform.Canonical
import org.clyze.deepdoop.datalog.element.IElement
import org.clyze.deepdoop.datalog.expr.IExpr

@Canonical
class Relation implements IElement {

	String name
	List<IExpr> exprs = []
	int getArity() { exprs.size() }

	String toString() { "REL##$name$exprs" }
}
