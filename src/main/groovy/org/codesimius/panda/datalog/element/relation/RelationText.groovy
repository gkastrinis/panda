package org.codesimius.panda.datalog.element.relation

import groovy.transform.Canonical

@Canonical
// Internal representation until the list of tokens is resolved to a normal relation
class RelationText extends Relation {

	List tokens

	RelationText(def tokens) {
		// Remove '{' and '}' used for disambiguation
		this.tokens = tokens.first() == "{" ? tokens.drop(1).dropRight(1) : tokens
	}

	String toString() { "R##<$tokens>" }
}
