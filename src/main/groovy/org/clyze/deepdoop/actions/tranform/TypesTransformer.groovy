package org.clyze.deepdoop.actions.tranform

import groovy.transform.Canonical
import org.clyze.deepdoop.actions.RelationInfoVisitingActor
import org.clyze.deepdoop.datalog.IVisitable
import org.clyze.deepdoop.datalog.block.BlockLvl0
import org.clyze.deepdoop.datalog.clause.RelDeclaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.clause.TypeDeclaration
import org.clyze.deepdoop.datalog.element.ComparisonElement
import org.clyze.deepdoop.datalog.element.ConstructionElement
import org.clyze.deepdoop.datalog.element.LogicalElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.BinaryExpr
import org.clyze.deepdoop.datalog.expr.GroupExpr

import static org.clyze.deepdoop.datalog.Annotation.CONSTRUCTOR
import static org.clyze.deepdoop.datalog.Annotation.TYPEVALUES
import static org.clyze.deepdoop.datalog.element.relation.Type.TYPE_STRING
import static org.clyze.deepdoop.datalog.expr.VariableExpr.gen1 as var1

@Canonical
class TypesTransformer extends DefaultTransformer {

	RelationInfoVisitingActor relationInfo

	void enter(BlockLvl0 n) {
		relationInfo.rootTypes.findAll { !(it in relationInfo.typesToOptimize) }.each { root ->
			extraRelDecls << new RelDeclaration(new Constructor(root.defaultConName, []), [TYPE_STRING, root], [CONSTRUCTOR] as Set)
		}
	}

	IVisitable exit(RelDeclaration n) { n }

	IVisitable exit(TypeDeclaration n) {
		if (TYPEVALUES in n.annotations) {
			def rootT = relationInfo.typeToRootType[n.type]
			n.annotations.find { it == TYPEVALUES }.args.each { key, value ->
				def relName = "${n.type.name}:$key"

				if (!(n.type in relationInfo.typesToOptimize)) {
					def rel = new Relation(relName, [var1()])
					extraRelDecls << new RelDeclaration(rel, [n.type])
					def con = new ConstructionElement(new Constructor(rootT.defaultConName, [value, var1()]), n.type)
					extraRules << new Rule(new LogicalElement([con, rel]), null)
				} else {
					def rel = new Relation(relName, [value])
					extraRelDecls << new RelDeclaration(rel, [TYPE_STRING])
					def typeRel = new Relation(n.type.name, [value])
					extraRules << new Rule(new LogicalElement([typeRel, rel]), null)
				}
			}
		}
		return n
	}

	IVisitable exit(LogicalElement n) { n }

	IVisitable exit(ComparisonElement n) { n }

	IVisitable exit(ConstructionElement n) { n }

	IVisitable exit(Constructor n) { n }

	IVisitable exit(Relation n) { n }

	IVisitable exit(Type n) { n }

	IVisitable exit(BinaryExpr n) { n }

	IVisitable exit(GroupExpr n) { n }
}
