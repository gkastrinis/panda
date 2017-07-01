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

	void add(Declaration d) {
		/*if (d.atom instanceof Constructor) {
			def c = d.atom as Constructor
			def name = c.entity.name
			if (!constructionInfo[name]) constructionInfo[name] = []
			constructionInfo[name] << new Tuple2(d, c)
		}
		// forward patching
		if (d.atom.name in entities) {
			def p = d.atom as Predicate
			assert p.exprs.size() == 1
			def entity = new Type(p.name, p.stage, p.exprs.first())
			d = new Declaration(entity, [] + d.types)
		}*/
		declarations << d
	}

	void addCons(Constraint c) { constraints << c }

	void add(Rule r) { rules << r }

	void addAll(Component other) {
		declarations += other.declarations
		constraints += other.constraints
		rules += other.rules
	}

	/*void markEntity(String entityName) {
		entities << entityName
		// backwards patching
		def decl = declarations.find { it.relation.name == entityName }
		if (decl != null) {
			def p = decl.relation as Predicate
			assert p.exprs.size() == 1
			def entity = new Type(p.name, p.stage, p.exprs.first())
			declarations.remove(decl)
			declarations << new Declaration(entity, decl.types)
		}
	}*/

	def <T> T accept(IVisitor<T> v) { v.visit(this) }
}
