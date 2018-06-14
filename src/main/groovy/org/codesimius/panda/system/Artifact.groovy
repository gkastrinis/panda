package org.codesimius.panda.system

import groovy.transform.Canonical

@Canonical
class Artifact {

	enum Kind {
		LOGIC, GRAPH
	}

	Kind kind
	File file

	String toString() { "($kind) $file" }
}
