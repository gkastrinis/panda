package org.clyze.deepdoop.datalog.clause

import groovy.transform.Canonical
import groovy.transform.ToString
import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.datalog.Annotation
import org.clyze.deepdoop.datalog.element.LogicalElement

@Canonical
@ToString(includePackage = false)
class Rule implements IVisitable {

	LogicalElement head
	LogicalElement body
	//Map<Kind, Annotation> annotations = [:]
	Set<Annotation> annotations = [] as Set
}
