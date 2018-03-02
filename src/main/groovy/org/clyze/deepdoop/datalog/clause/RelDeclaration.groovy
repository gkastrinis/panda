package org.clyze.deepdoop.datalog.clause

import groovy.transform.Canonical
import groovy.transform.ToString
import org.clyze.deepdoop.datalog.Annotation
import org.clyze.deepdoop.datalog.IVisitable
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type

@Canonical
@ToString(includePackage = false)
class RelDeclaration implements IVisitable {

	Relation relation
	List<Type> types
	Set<Annotation> annotations = [] as Set
}
