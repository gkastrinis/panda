package org.codesimius.panda.datalog.element.relation

import groovy.transform.Canonical
import org.codesimius.panda.datalog.element.IElement
import org.codesimius.panda.datalog.expr.IExpr

@Canonical
class Relation implements IElement {

	String name
	List<IExpr> exprs = []

	int getArity() { exprs.size() }

	String toString() { "R##$name$exprs" }
}
