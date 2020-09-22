package org.codesimius.panda.datalog.clause

import groovy.transform.Canonical
import groovy.transform.ToString
import org.codesimius.panda.datalog.Annotation
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.element.IElement

@Canonical
@ToString(includePackage = false)
class Rule implements IVisitable {

	IElement head
	IElement body
	Set<Annotation> annotations = [] as Set
}
