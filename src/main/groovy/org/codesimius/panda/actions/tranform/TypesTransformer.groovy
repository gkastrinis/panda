package org.codesimius.panda.actions.tranform

import groovy.transform.Canonical
import org.codesimius.panda.actions.symbol.SymbolTable
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl0
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.ComparisonElement
import org.codesimius.panda.datalog.element.ConstructionElement
import org.codesimius.panda.datalog.element.LogicalElement
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.element.relation.Type
import org.codesimius.panda.datalog.expr.BinaryExpr
import org.codesimius.panda.datalog.expr.GroupExpr

import static org.codesimius.panda.datalog.Annotation.CONSTRUCTOR
import static org.codesimius.panda.datalog.Annotation.TYPEVALUES
import static org.codesimius.panda.datalog.element.relation.Type.TYPE_STRING
import static org.codesimius.panda.datalog.expr.VariableExpr.gen1 as var1

@Canonical
class TypesTransformer extends DefaultTransformer {

	SymbolTable symbolTable

	void enter(BlockLvl0 n) {
		// Add default constructors
		symbolTable.rootTypes.each { root ->
			extraRelDecls << new RelDeclaration(new Constructor(root.defaultConName, []), [TYPE_STRING, root], [CONSTRUCTOR] as Set)
		}
	}

	IVisitable exit(TypeDeclaration n) {
		def ann = n.annotations.find { it == TYPEVALUES }
		if (ann) {
			def rootT = symbolTable.typeToRootType[n.type]
			ann.args.each { key, value ->
				def rel = new Relation("${n.type.name}:$key", [var1()])
				extraRelDecls << new RelDeclaration(rel, [n.type])
				def con = new ConstructionElement(new Constructor(rootT.defaultConName, [value, var1()]), n.type)
				extraRules << new Rule(new LogicalElement([con, rel]))
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
