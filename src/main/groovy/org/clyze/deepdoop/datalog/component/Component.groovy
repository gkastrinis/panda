package org.clyze.deepdoop.datalog.component

import groovy.transform.Canonical
import groovy.transform.ToString
import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule

@Canonical
@ToString(includePackage = false)
class Component implements IVisitable {

	String name
	String superComp
	List<String> parameters = []
	List<String> superParameters = []
	Set<Declaration> declarations = [] as Set
	Set<Rule> rules = [] as Set
}
