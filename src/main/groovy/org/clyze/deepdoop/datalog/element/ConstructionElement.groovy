package org.clyze.deepdoop.datalog.element

import groovy.transform.Canonical
import groovy.transform.ToString
import org.clyze.deepdoop.actions.IVisitor
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Type

@Canonical
@ToString(includePackage = false)
class ConstructionElement implements IElement {

	Constructor constructor
	Type type

	def <T> T accept(IVisitor<T> v) { v.visit(this) }
}
