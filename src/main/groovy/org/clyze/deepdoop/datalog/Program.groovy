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

	Program accept(IVisitor<IVisitable> v) { v.visit(this) as Program }
}
