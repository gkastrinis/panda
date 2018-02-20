package org.clyze.deepdoop.datalog.clause

import groovy.transform.ToString
import org.clyze.deepdoop.datalog.Annotation
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type

@ToString(includePackage = false, includeSuperProperties = true)
class RelDeclaration extends Declaration {

	Relation relation
	List<Type> types

	RelDeclaration(Relation relation, List<Type> types, Set<Annotation> annotations = [] as Set) {
		super(annotations)
		this.relation = relation
		this.types = types
	}
}
