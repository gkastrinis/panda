package org.codesimius.panda.datalog.element.relation

import groovy.transform.Canonical

@Canonical
// Internal representation until the list of tokens is resolved to a normal relation
class RelationText extends Relation {

	List tokens

	String toString() { "R##<$tokens>" }
}
