package org.clyze.deepdoop.datalog.component

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.transform.TupleConstructor
import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.RelDeclaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.clause.TypeDeclaration

@EqualsAndHashCode(includes = "name")
@ToString(includePackage = false)
@TupleConstructor
class Component implements IVisitable {

	String name
	String superComp
	List<String> parameters = []
	List<String> superParameters = []

	Set<Declaration> declarations = [] as Set
	Set<Rule> rules = [] as Set

	private Set<RelDeclaration> relDeclarations

	Set<RelDeclaration> getRelDeclarations() {
		if (!relDeclarations)
			relDeclarations = declarations.findAll { it instanceof RelDeclaration }.collect { it as RelDeclaration }
		relDeclarations
	}

	private Set<TypeDeclaration> typeDeclarations

	Set<TypeDeclaration> getTypeDeclarations() {
		if (!typeDeclarations)
			typeDeclarations = declarations.findAll { it instanceof TypeDeclaration }.collect { it as TypeDeclaration }
		typeDeclarations
	}
}
