package org.clyze.deepdoop.actions.tranform

import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.actions.TypeHierarchyVisitingActor
import org.clyze.deepdoop.datalog.Annotation
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.element.ComparisonElement
import org.clyze.deepdoop.datalog.element.ConstructionElement
import org.clyze.deepdoop.datalog.element.LogicalElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.BinaryExpr
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.datalog.expr.GroupExpr

import static org.clyze.deepdoop.datalog.expr.VariableExpr.gen1 as var1
import static org.clyze.deepdoop.datalog.expr.VariableExpr.genN as varN

class TypeValuesTransformer extends DummyTransformer {

	TypeHierarchyVisitingActor typeHierarchyVA
	Component currComp
	Set<Declaration> extraDecls
	Set<Rule> extraRules

	TypeValuesTransformer(TypeHierarchyVisitingActor typeHierarchyVA) {
		actor = this
		this.typeHierarchyVA = typeHierarchyVA
	}

	void enter(Component n) {
		currComp = n
		extraDecls = [] as Set
		extraRules = [] as Set

		typeHierarchyVA.typeToRootType.values()
		typeHierarchyVA.superTypesOrdered[n].findAll { !it.value }.each { t, superTs ->
			extraDecls << new Declaration(
					new Constructor("${t.name}:byStr", varN(2)),
					[new Type("string"), t],
					[new Annotation("constructor")] as Set)
		}
	}

	IVisitable exit(Component n, Map m) {
		def ds = (n.declarations.collect { m[it] as Declaration } + extraDecls) as Set
		def rs = (n.rules.collect { m[it] as Rule } + extraRules) as Set
		new Component(n.name, n.superComp, n.parameters, n.superParameters, ds, rs)
	}

	IVisitable exit(Type n, Map m) {
		def rootT = typeHierarchyVA.typeToRootType[currComp][n]
		n.initValues.each { key, ConstantExpr value ->
			def rel = new Relation("${n.name}:$key", [var1()])
			def con = new Constructor("${rootT.name}:byStr", [value, var1()])
			extraDecls << new Declaration(rel, [n])
			extraRules << new Rule(new LogicalElement([new ConstructionElement(con, n), rel]), null)
		}
		return n
	}

	// Overrides to avoid unneeded allocations

	IVisitable exit(LogicalElement n, Map m) { n }

	IVisitable exit(Declaration n, Map m) { n }

	IVisitable exit(ComparisonElement n, Map m) { n }

	IVisitable exit(ConstructionElement n, Map m) { n }

	IVisitable exit(Constructor n, Map m) { n }

	IVisitable exit(Relation n, Map m) { n }

	IVisitable exit(BinaryExpr n, Map m) { n }

	IVisitable exit(GroupExpr n, Map m) { n }
}
