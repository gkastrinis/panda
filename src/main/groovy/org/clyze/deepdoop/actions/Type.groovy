package org.clyze.deepdoop.actions

import groovy.transform.Canonical
import groovy.transform.ToString

interface IType {
    IType join(IType t)
}

// Type with fixed value
@Canonical
@ToString(includePackage = false)
class ClosedType implements IType {

    static final ClosedType INT_T = new ClosedType("int")
    static final ClosedType REAL_T = new ClosedType("float")
    static final ClosedType BOOL_T = new ClosedType("bool")
    static final ClosedType STR_T = new ClosedType("string")

    @Delegate String value

    IType join(IType t) {
        return new OpenType().merge(this).merge(t)
    }
}

// Type with candidate values
@Canonical
@ToString(includePackage = false)
class OpenType implements IType {

    @Delegate Set<String> values = [] as Set

    IType join(IType t) {
        return new OpenType().merge(this).merge(t)
    }

    def merge(def t) {
        if (t && t instanceof ClosedType) values << t.value
        else if (t && t instanceof OpenType) values.addAll(t.values)
        return this
    }
}

// Representing a tuple of types
// Each component might be closed or open
@Canonical
@ToString(includePackage = false)
class ComplexType {
    @Delegate List<IType> components = []
}
