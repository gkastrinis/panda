package org.clyze.deepdoop.datalog.clause

import groovy.transform.Canonical
import groovy.transform.ToString
import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.actions.IVisitor
import org.clyze.deepdoop.datalog.Annotation
import org.clyze.deepdoop.datalog.Annotation.Kind
import org.clyze.deepdoop.datalog.element.relation.Relation

@Canonical
@ToString(includePackage = false)
class Declaration implements IVisitable {

	Relation atom
	List<Relation> types
	Map<Kind, Annotation> annotations

	def <T> T accept(IVisitor<T> v) { v.visit(this) }
}
