package org.codesimius.panda.datalog

import groovy.transform.Canonical
import groovy.transform.InheritConstructors
import org.codesimius.panda.system.Error

import static org.codesimius.panda.datalog.Annotation.METADATA
import static org.codesimius.panda.system.Error.warn

@Canonical
@InheritConstructors
class AnnotationSet extends LinkedHashSet<Annotation> {

	//AnnotationSet(Set<Annotation> rawAnnotations = [] as Set) { this.rawAnnotations = rawAnnotations }

	AnnotationSet(Annotation annotation) { this << annotation }

	Annotation getAt(Annotation annotation) { find { it == annotation } }

	AnnotationSet plus(Collection<? extends Annotation> others) {
		others.each { this << it }
		this
	}

	boolean addAll(Collection<? extends Annotation> others) {
		others.each { this << it }
		true
	}

	boolean add(Annotation annotation) {
		this << annotation
		true
	}

	AnnotationSet leftShift(Annotation annotation) {
		if (annotation in this && annotation.isInternal)
			this[annotation].args += annotation.args
		else if (annotation in this)
			warn(findLoc(), Error.ANNOTATION_MULTIPLE, annotation)
		else
			super.add annotation
		this
	}

	def findLoc() { this[METADATA]?.args?.loc }
}
