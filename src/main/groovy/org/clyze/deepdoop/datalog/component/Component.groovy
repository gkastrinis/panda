package org.clyze.deepdoop.datalog.component

import groovy.transform.Canonical
import groovy.transform.ToString
import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.actions.IVisitor
import org.clyze.deepdoop.datalog.clause.Constraint
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.element.relation.Constructor

@Canonical
@ToString(includePackage = false)
class Component implements IVisitable {

	String name
	String superComp
	Set<Declaration> declarations = [] as Set
	Set<Constraint> constraints = [] as Set
	Set<Rule> rules = [] as Set
	Set<String> entities = [] as Set

	Map<String, List<Tuple2<Declaration, Constructor>>> constructionInfo = [:]

	Component clone() {
		new Component(
				name: name,
				superComp: superComp,
				declarations: [] + declarations,
				rules: [] + rules,
				entities: [] + entities,
				constructionInfo: [:] << constructionInfo)
	}

	void add(Declaration d) { declarations << d }

	void addCons(Constraint c) { constraints << c }

	void add(Rule r) { rules << r }

	void addAll(Component other) {
		declarations += other.declarations
		constraints += other.constraints
		rules += other.rules
	}

	def <T> T accept(IVisitor<T> v) { v.visit(this) }
}
