package org.codesimius.panda.actions.graph

import groovy.transform.Canonical
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@Canonical
@EqualsAndHashCode(includes = ["id", "kind"])
@ToString(includePackage = false)
class Node {

	enum Kind {
		TEMPLATE, INSTANCE, PARAMETER, RELATION, CONSTRUCTOR
	}

	String id
	String title
	Kind kind
	Set<Edge> outEdges = [] as Set
	Set<Edge> inEdges = [] as Set

	void connectTo(Node to, Edge.Kind kind, String label = "") {
		outEdges << new Edge(to, kind, label)
		to.inEdges << new Edge(this, kind)
	}
}