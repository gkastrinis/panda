package org.codesimius.panda.actions.graph

import groovy.transform.Canonical

@Canonical
class Edge {

	enum Kind {
		INHERITANCE, INSTANCE, ACTUAL_PARAM, INDIRECT_PARAM, NEGATION, RELATION,
	}

	Node node
	Kind kind
	String label

	String toString() { "$kind:$label:${node.title}" }
}
