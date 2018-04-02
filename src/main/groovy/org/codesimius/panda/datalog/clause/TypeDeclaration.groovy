package org.codesimius.panda.datalog.clause

import groovy.transform.Canonical
import groovy.transform.ToString
import org.codesimius.panda.datalog.Annotation
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.element.relation.Type

@Canonical
@ToString(includePackage = false)
class TypeDeclaration implements IVisitable {

	Type type
	Type supertype = null
	Set<Annotation> annotations = [] as Set
}
