package org.clyze.deepdoop.datalog.component

import groovy.transform.Canonical
import org.clyze.deepdoop.datalog.element.relation.Relation

@Canonical
class Propagation {

	@Canonical
	static class Alias {
		Relation orig
		Relation alias
	}

	String fromId
	Set<Alias> preds
	String toId
}
