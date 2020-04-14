package org.codesimius.panda.datalog

import groovy.transform.Canonical
import org.codesimius.panda.system.Error

import static org.codesimius.panda.datalog.Annotation.METADATA
import static org.codesimius.panda.system.Error.warn

@Canonical
class AnnotationSet {

	// TODO why not extend set?
	Set<Annotation> rawAnnotations

	AnnotationSet(Set<Annotation> rawAnnotations = [] as Set) { this.rawAnnotations = rawAnnotations }

	AnnotationSet(Annotation rawAnnotation) { this([rawAnnotation] as Set) }

	boolean isCase(annotation) { annotation in rawAnnotations }

	Annotation getAt(Annotation annotation) { rawAnnotations.find { it == annotation } }

	AnnotationSet plus(AnnotationSet others) {
		others.rawAnnotations.each { this << it }
		this
	}

	AnnotationSet minus(AnnotationSet others) {
		rawAnnotations -= others.rawAnnotations
		this
	}

	AnnotationSet leftShift(Annotation annotation) {
		if (annotation in this && annotation.isInternal)
			this[annotation].args += annotation.args
		else if (annotation in this)
			warn(findLoc(), Error.ANNOTATION_MULTIPLE, annotation)
		else
			rawAnnotations << annotation
		this
	}

	AnnotationSet minus(Annotation annotation) {
		rawAnnotations.remove annotation
		this
	}

	def findLoc() { this[METADATA]?.args?.loc }

	String toString() { rawAnnotations as String }
}
