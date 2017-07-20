package org.clyze.deepdoop.actions

import groovy.transform.Canonical
import groovy.transform.ToString

// Type with candidate values
@Canonical
@ToString(includePackage = false)
class OpenType implements IType {

	@Delegate
	Set<String> values = [] as Set

	IType join(IType t) {
		return new OpenType().merge(this).merge(t)
	}

	def merge(def t) {
		if (t && t instanceof ClosedType) values << t.value
		else if (t && t instanceof OpenType) values.addAll(t.values)
		return this
	}
}
