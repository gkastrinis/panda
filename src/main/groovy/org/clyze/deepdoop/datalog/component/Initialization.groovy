package org.clyze.deepdoop.datalog.component

import groovy.transform.Canonical
import groovy.transform.ToString

@Canonical
@ToString(includePackage = false)
class Initialization {

	String compName
	List<String> parameters
	String id
}
