package org.codesimius.panda.datalog.element

import groovy.transform.Canonical
import groovy.transform.ToString

@Canonical
@ToString(includePackage = false)
class LogicalElement implements IElement {

	enum LogicType {
		AND, OR
	}

	LogicType type
	List<IElement> elements

	LogicalElement(LogicType type = LogicType.AND, List<IElement> elements) {
		this.type = type
		this.elements = elements
	}

	LogicalElement(IElement element) {
		this.type = LogicType.AND
		this.elements = [element]
	}
}