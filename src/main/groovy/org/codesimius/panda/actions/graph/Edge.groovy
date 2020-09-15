package org.codesimius.panda.actions.graph

import groovy.transform.Canonical

@Canonical
class Edge {

	enum Kind {
		INHERITANCE, INSTANCE, ACTUAL_PARAM, INDIRECT_PARAM,  // Templates
		NEGATION, AGGREGATION, RELATION,                      // Relations
		SUBTYPE,                                              // Types
	}

	Node node
	Kind kind
	String label

	String toString() { "$kind:$label:${node.title}" }
}
