package org.codesimius.panda.datalog.element

import groovy.transform.Canonical

import static org.codesimius.panda.datalog.element.ComparisonElement.TRIVIALLY_TRUE

@Canonical
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

	String toString() { "$kind<$elements>" }

	static IElement combineElements(Kind kind = Kind.AND, List<IElement> elements) {
		// Flatten LogicalElement "trees"
		def newElements = []
		elements.each {
			if (it instanceof LogicalElement && it.kind == kind)
				newElements += it.elements
			else
				newElements << it
		}
		// Remove trivially true elements
		newElements = newElements.findAll { it != TRIVIALLY_TRUE } as List<IElement>
		if (newElements.size() > 1) new LogicalElement(kind, newElements)
		else if (newElements.size() == 1) newElements.first() as IElement
		else null
	}
}
