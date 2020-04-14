package org.codesimius.panda.datalog.block

import groovy.transform.Canonical
import groovy.transform.ToString

@Canonical
@ToString(includePackage = false)
class Instantiation {

	String template
	String id
	List<String> parameters = []
}
