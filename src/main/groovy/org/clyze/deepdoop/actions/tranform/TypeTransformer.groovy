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

import static org.clyze.deepdoop.datalog.Annotation.*
import static org.clyze.deepdoop.datalog.element.relation.Type.TYPE_STR
import static org.clyze.deepdoop.datalog.expr.VariableExpr.gen1 as var1
import static org.clyze.deepdoop.datalog.expr.VariableExpr.genN as varN

class TypeTransformer extends DummyTransformer {

	TypeHierarchyVisitingActor typeHierarchyVA
	Component currComp

	TypeTransformer(TypeHierarchyVisitingActor typeHierarchyVA) {
		actor = this
		this.typeHierarchyVA = typeHierarchyVA
	}

	void enter(Component n) {
		currComp = n
		extraDecls = [] as Set
		extraRules = [] as Set

		typeHierarchyVA.typeToRootType.values()
		// For all root types
		typeHierarchyVA.superTypesOrdered[n].findAll { !it.value }.each { t, superTs ->
			extraDecls << new Declaration(
					new Constructor("${t.name}:byStr", []),
					[TYPE_STR, t],
					[new Annotation("constructor")] as Set)
		}
	}

	IVisitable exit(Declaration n, Map m) {
		if (TYPE in n.annotations) {
			def t = n.relation as Type
			def rootT = typeHierarchyVA.typeToRootType[currComp][t]
			if (TYPEVALUES in n.annotations) {
				n.annotations.find { it == TYPEVALUES }.args.each { key, value ->
					def rel = new Relation("${t.name}:$key", [var1()])
					def con = new Constructor("${rootT.name}:byStr", [value, var1()])
					extraDecls << new Declaration(rel, [t])
					extraRules << new Rule(new LogicalElement([new ConstructionElement(con, t), rel]), null)
				}
			}
			if (INPUT in n.annotations) {
				def relName = "__SYS_INPUT_${n.relation.name}"
				def rel = new Relation(relName, varN(1))
				def con = new Constructor("${rootT.name}:byStr", varN(2))
				def a = new Annotation("INPUT", [
						"filename" : new ConstantExpr("${n.relation.name}.facts"),
						"delimeter": new ConstantExpr("\\t")])
				extraDecls << new Declaration(rel, [TYPE_STR], [a] as Set)
				extraRules << new Rule(new LogicalElement(new ConstructionElement(con, n.relation as Type)), new LogicalElement(rel))
				n.annotations.remove(INPUT)
			}
		}
		return n
	}

	// Overrides to avoid unneeded allocations

	IVisitable exit(LogicalElement n, Map m) { n }

	IVisitable exit(ComparisonElement n, Map m) { n }

	IVisitable exit(ConstructionElement n, Map m) { n }

	IVisitable exit(Constructor n, Map m) { n }

	IVisitable exit(Relation n, Map m) { n }

	IVisitable exit(Type n, Map m) { n }

	IVisitable exit(BinaryExpr n, Map m) { n }

	IVisitable exit(GroupExpr n, Map m) { n }
}
