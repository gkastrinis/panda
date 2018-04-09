package org.codesimius.panda.actions.graph

import groovy.transform.Canonical
import groovy.transform.ToString

@Canonical
@ToString(includePackage = false)
class Graph {

	String name
	Map<String, Node> nodes = [:]

	Node getHeadNode() {
		if (!nodes[name]) nodes[name] = new Node(name, name, Node.Kind.TEMPLATE)
		nodes[name]
	}
}
