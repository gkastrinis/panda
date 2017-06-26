package org.clyze.deepdoop.datalog.element

import groovy.transform.Canonical
import groovy.transform.ToString
import org.clyze.deepdoop.actions.IVisitor

@Canonical
@ToString(includePackage = false)
class GroupElement implements IElement {

	@Delegate
	IElement element

	def <T> T accept(IVisitor<T> v) { v.visit(this) }
}
