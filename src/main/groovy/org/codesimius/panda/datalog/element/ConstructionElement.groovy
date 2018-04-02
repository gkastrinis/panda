package org.codesimius.panda.datalog.element

import groovy.transform.Canonical
import groovy.transform.ToString
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Type

@Canonical
@ToString(includePackage = false)
class ConstructionElement implements IElement {

	Constructor constructor
	Type type
}
