package org.codesimius.panda.actions.graph

import groovy.transform.Canonical

@Canonical
class Edge {

	enum Kind {
		INHERITANCE, INSTANCE, PARAMETER, RELATION, NEGATED
	}

	Node node
	Kind kind
	String label

	String toString() { "$kind:$label:${node.title}" }
}
