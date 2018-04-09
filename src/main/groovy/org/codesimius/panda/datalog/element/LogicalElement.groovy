package org.codesimius.panda.datalog.element

import groovy.transform.Canonical
import groovy.transform.ToString

@Canonical
@ToString(includePackage = false)
class LogicalElement implements IElement {

	enum Kind {
		AND, OR
	}

	Kind kind
	List<IElement> elements

	LogicalElement(Kind kind = Kind.AND, List<IElement> elements) {
		this.kind = kind
		this.elements = elements
	}

	LogicalElement(IElement element) {
		this.kind = Kind.AND
		this.elements = [element]
	}
}
