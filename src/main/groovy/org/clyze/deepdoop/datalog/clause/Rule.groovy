package org.clyze.deepdoop.datalog.clause

import groovy.transform.Canonical
import groovy.transform.ToString
import org.clyze.deepdoop.datalog.Annotation
import org.clyze.deepdoop.datalog.IVisitable
import org.clyze.deepdoop.datalog.element.IElement

@Canonical
@ToString(includePackage = false)
class Rule implements IVisitable {

	IElement head
	IElement body
	Set<Annotation> annotations = [] as Set
}
