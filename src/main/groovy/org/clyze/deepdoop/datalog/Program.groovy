package org.clyze.deepdoop.datalog

import groovy.transform.Canonical
import groovy.transform.ToString
import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.actions.IVisitor
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.component.Initialization

@Canonical
@ToString(includePackage = false)
class Program implements IVisitable {

	Component globalComp = new Component()
	Map<String, Component> comps = [:]
	Set<Initialization> inits = [] as Set

	def <T> T accept(IVisitor<T> v) { v.visit(this) }
}
