package org.clyze.deepdoop.datalog.clause

import groovy.transform.TupleConstructor
import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.datalog.Annotation

@TupleConstructor
abstract class Declaration implements IVisitable {

	Set<Annotation> annotations = [] as Set
}
