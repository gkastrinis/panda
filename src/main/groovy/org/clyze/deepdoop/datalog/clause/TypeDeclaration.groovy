package org.clyze.deepdoop.datalog.clause

import groovy.transform.Canonical
import groovy.transform.ToString
import org.clyze.deepdoop.datalog.Annotation
import org.clyze.deepdoop.datalog.IVisitable
import org.clyze.deepdoop.datalog.element.relation.Type

@Canonical
@ToString(includePackage = false)
class TypeDeclaration implements IVisitable {

	Type type
	Type supertype = null
	Set<Annotation> annotations = [] as Set
}
