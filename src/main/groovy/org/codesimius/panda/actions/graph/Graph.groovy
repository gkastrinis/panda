package org.codesimius.panda.actions.graph

import groovy.transform.Canonical

@Canonical
class Graph {

	String name
	Map<String, Node> nodes = [:]

	Node getHeadNode() {
		if (!nodes[""]) nodes[""] = new Node(name, Node.Kind.TEMPLATE)
		nodes[""]
	}
}
