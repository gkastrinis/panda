package org.clyze.deepdoop.actions.tranform

import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.actions.TypeInfoVisitingActor
import org.clyze.deepdoop.datalog.Annotation
import org.clyze.deepdoop.datalog.clause.RelDeclaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.clause.TypeDeclaration
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
import static org.clyze.deepdoop.datalog.element.relation.Type.TYPE_STRING
import static org.clyze.deepdoop.datalog.expr.VariableExpr.gen1 as var1
import static org.clyze.deepdoop.datalog.expr.VariableExpr.genN as varN

class TypeTransformer extends DummyTransformer {

	private TypeInfoVisitingActor typeInfoActor
	private Component currComp

	TypeTransformer(TypeInfoVisitingActor typeInfoActor) {
		actor = this
		this.typeInfoActor = typeInfoActor
	}

	void enter(Component n) {
		currComp = n
		extraDecls = [] as Set
		extraRules = [] as Set

		(typeInfoActor.typeToRootType[n].values() as Set).each {
			extraDecls << new RelDeclaration(new Constructor("${it.name}:byStr", []), [TYPE_STRING, it], [CONSTRUCTOR] as Set)
		}
	}

	IVisitable exit(TypeDeclaration n, Map m) {
		def rootT = typeInfoActor.typeToRootType[currComp][n.type]
		if (TYPEVALUES in n.annotations) {
			n.annotations.find { it == TYPEVALUES }.args.each { key, value ->
				def rel = new Relation("${n.type.name}:$key", [var1()])
				def con = new Constructor("${rootT.name}:byStr", [value, var1()])
				extraDecls << new RelDeclaration(rel, [n.type])
				extraRules << new Rule(new LogicalElement([new ConstructionElement(con, n.type), rel]), null)
			}
		}
		if (INPUT in n.annotations) {
			def relName = "__SYS_INPUT_${n.type.name}"
			def rel = new Relation(relName, varN(1))
			def con = new Constructor("${rootT.name}:byStr", varN(2))
			def a = new Annotation("INPUT", [
					"filename" : new ConstantExpr("${n.type.name}.facts"),
					"delimeter": new ConstantExpr("\\t")])
			extraDecls << new RelDeclaration(rel, [TYPE_STRING], [a] as Set)
			extraRules << new Rule(new LogicalElement(new ConstructionElement(con, n.type)), new LogicalElement(rel))
			n.annotations.remove(INPUT)
		}
		return n
	}

	// Overrides to avoid unneeded allocations

	IVisitable exit(RelDeclaration n, Map m) { n }

	IVisitable exit(LogicalElement n, Map m) { n }

	IVisitable exit(ComparisonElement n, Map m) { n }

	IVisitable exit(ConstructionElement n, Map m) { n }

	IVisitable exit(Constructor n, Map m) { n }

	IVisitable exit(Relation n, Map m) { n }

	IVisitable exit(Type n, Map m) { n }

	IVisitable exit(BinaryExpr n, Map m) { n }

	IVisitable exit(GroupExpr n, Map m) { n }
}
