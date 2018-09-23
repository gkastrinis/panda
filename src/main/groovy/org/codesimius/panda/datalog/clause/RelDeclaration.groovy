package org.codesimius.panda.datalog.clause

import groovy.transform.Canonical
import groovy.transform.ToString
import org.codesimius.panda.datalog.AnnotationSet
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.element.relation.Type

@Canonical
@ToString(includePackage = false)
class RelDeclaration implements IVisitable {

	Relation relation
	List<Type> types
	AnnotationSet annotations = new AnnotationSet()

	def loc() { annotations.findLoc() }
}
