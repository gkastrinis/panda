package org.codesimius.panda.datalog.clause

import groovy.transform.Canonical
import groovy.transform.ToString
import org.codesimius.panda.datalog.AnnotationSet
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.element.IElement

@Canonical
@ToString(includePackage = false)
class Rule implements IVisitable {

	IElement head
	IElement body
	AnnotationSet annotations = new AnnotationSet()

	@Deprecated
	def loc() { annotations.findLoc() }
}
