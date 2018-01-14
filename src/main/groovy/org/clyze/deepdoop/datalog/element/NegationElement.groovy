package org.clyze.deepdoop.datalog.element

import groovy.transform.Canonical
import groovy.transform.ToString

@Canonical
@ToString(includePackage = false)
class NegationElement implements IElement {

	IElement element
}
