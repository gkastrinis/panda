package org.codesimius.panda.actions.graph

import groovy.transform.Canonical
import groovy.transform.ToString

@Canonical
@ToString(includePackage = false)
class Graph {

	String name
	Map<String, Node> nodes = [:]

	Node touch(String nodeName, Node.Kind kind) {
		def id = "$name:$nodeName" as String
		if (!nodes[id]) nodes[id] = new Node(id, nodeName, kind)
		nodes[id]
	}
}
