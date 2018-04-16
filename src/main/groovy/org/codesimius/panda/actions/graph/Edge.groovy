package org.codesimius.panda.actions.graph

import groovy.transform.Canonical

@Canonical
class Edge {

	enum Kind {
		INHERITANCE,    // Template component inherits from component
		INSTANCE,       // Instantiation of a template component
		FORMAL_PARAM,   // Relation refers to formal param (inside a component)
		ACTUAL_PARAM,   // Instantiation takes another one as parameter
		REVERSE_PARAM,  // Relation refers to a certain instantiation (in global space)
		NEGATION,       // Relation depends on another one, through negation
		RELATION,       // Relation depends on another one
	}

	Node node
	Kind kind
	String label

	String toString() { "$kind:$label:${node.title}" }
}
