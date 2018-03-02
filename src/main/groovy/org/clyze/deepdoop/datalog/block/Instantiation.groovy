package org.clyze.deepdoop.datalog.block

import groovy.transform.Canonical
import groovy.transform.ToString

@Canonical
@ToString(includePackage = false)
class Instantiation {

	String component
	String id
	List<String> parameters = []
}
