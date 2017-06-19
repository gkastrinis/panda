package org.clyze.deepdoop.actions

import groovy.transform.Canonical
import groovy.transform.ToString

interface IType {
    void merge(IType t)

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

    void merge(IType t) { throw UnsupportedOperationException() }

    IType join(IType t) {
        def open = new OpenType()
        open.merge(this)
        open.merge(t)
        return open
    }
}

// Type with candidate values
@Canonical
@ToString(includePackage = false)
class OpenType implements IType {

    @Delegate Set<String> values = [] as Set

    void merge(IType t) {
        if (!t) return
        (t instanceof ClosedType) ? values << t.value : values.addAll((t as OpenType).values)
    }

    IType join(IType t) {
        def open = new OpenType()
        open.merge(this)
        open.merge(t)
        return open
    }
}

// Representing a tuple of types
// Each component might be closed or open
@Canonical
@ToString(includePackage = false)
class ComplexType {
    @Delegate List<IType> components = []
}
