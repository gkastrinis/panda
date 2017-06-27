package org.clyze.deepdoop.actions

import groovy.transform.Canonical
import groovy.transform.ToString

// Type with fixed value
@Canonical
@ToString(includePackage = false)
class ClosedType implements IType {

	static final ClosedType INT_T = new ClosedType("int")
	static final ClosedType REAL_T = new ClosedType("float")
	static final ClosedType BOOL_T = new ClosedType("bool")
	static final ClosedType STR_T = new ClosedType("string")

	@Delegate
	String value

	IType join(IType t) {
		return new OpenType().merge(this).merge(t)
	}
}