package org.clyze.deepdoop.datalog.element.relation

import groovy.transform.Canonical
import groovy.transform.ToString
import org.clyze.deepdoop.actions.IVisitor
import org.clyze.deepdoop.datalog.element.IElement

// Generic base class that is also used when only the predicate name (and maybe stage) is known
@Canonical
@ToString(includePackage = false)
class Relation implements IElement {

	String name

	String stage = null

	int getArity() { throw new UnsupportedOperationException() }

	def <T> T accept(IVisitor<T> v) { v.visit(this) }
}
