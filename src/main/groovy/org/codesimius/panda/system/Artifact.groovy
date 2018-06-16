package org.codesimius.panda.system

import groovy.transform.Canonical
import groovy.transform.ToString

@Canonical
@ToString(includePackage = false)
class Artifact {

	enum Kind {
		LOGIC, GRAPH
	}

	Kind kind
	File file
}
