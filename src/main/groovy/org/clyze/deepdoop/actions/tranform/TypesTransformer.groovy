package org.clyze.deepdoop.actions.tranform

import groovy.transform.Canonical
import org.clyze.deepdoop.actions.SymbolTableVisitingActor
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
class TypesTransformer extends DummyTransformer {

	SymbolTableVisitingActor symbolTable

	void enter(BlockLvl0 n) {
		symbolTable.rootTypes.findAll { !(it in symbolTable.typesToOptimize) }.each { root ->
			extraRelDecls << new RelDeclaration(new Constructor(root.defaultConName, []), [TYPE_STRING, root], [CONSTRUCTOR] as Set)
		}
	}

	IVisitable exit(TypeDeclaration n, Map m) {
		if (TYPEVALUES in n.annotations) {
			def rootT = symbolTable.typeToRootType[n.type]
			n.annotations.find { it == TYPEVALUES }.args.each { key, value ->
				def relName = "${n.type.name}:$key"

				if (!(n.type in symbolTable.typesToOptimize)) {
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
