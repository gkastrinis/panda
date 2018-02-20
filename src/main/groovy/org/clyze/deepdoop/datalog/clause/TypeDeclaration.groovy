package org.clyze.deepdoop.datalog.clause

import groovy.transform.ToString
import org.clyze.deepdoop.datalog.Annotation
import org.clyze.deepdoop.datalog.element.relation.Type

@ToString(includePackage = false, includeSuperProperties = true)
class TypeDeclaration extends Declaration {

	Type type
	Type supertype

	TypeDeclaration(Type type, Type supertype = null, Set<Annotation> annotations = [] as Set) {
		super(annotations)
		this.type = type
		this.supertype = supertype
	}
}
