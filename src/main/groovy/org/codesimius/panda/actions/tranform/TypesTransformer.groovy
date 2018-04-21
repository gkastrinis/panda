package org.codesimius.panda.actions.tranform

import groovy.transform.Canonical
import org.codesimius.panda.actions.TypeInfoVisitor
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl0
import org.codesimius.panda.datalog.block.BlockLvl2
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

	TypeInfoVisitor typeInfo

	IVisitable exit(BlockLvl2 n) {
		typeInfo.removeOptimizedTypes()
		super.exit n
	}

	void enter(BlockLvl0 n) {
		// Add default constructors
		typeInfo.rootTypes.findAll { !(it in typeInfo.typesToOptimize) }.each { root ->
			def conDecl = new RelDeclaration(new Constructor(root.defaultConName, []), [TYPE_STRING, root], [CONSTRUCTOR] as Set)
			extraRelDecls << conDecl
			typeInfo.constructorsPerType[root] << conDecl
		}
	}

	IVisitable exit(TypeDeclaration n) {
		def optimized = n.type in typeInfo.typesToOptimize

		if (TYPEVALUES in n.annotations) {
			def rootT = typeInfo.typeToRootType[n.type]
			n.annotations.find { it == TYPEVALUES }.args.each { key, value ->
				def relName = "${n.type.name}:$key"

				if (optimized) {
					def rel = new Relation(relName, [value])
					extraRelDecls << new RelDeclaration(rel, [TYPE_STRING])
					def typeRel = new Relation(n.type.name, [value])
					extraRules << new Rule(new LogicalElement([typeRel, rel]), null)
				} else {
					def rel = new Relation(relName, [var1()])
					extraRelDecls << new RelDeclaration(rel, [n.type])
					def con = new ConstructionElement(new Constructor(rootT.defaultConName, [value, var1()]), n.type)
					extraRules << new Rule(new LogicalElement([con, rel]), null)
				}
			}
		}
		if (optimized) {
			extraRelDecls << new RelDeclaration(new Relation(n.type.name), [TYPE_STRING])
			return null
		} else
			return n
	}

	IVisitable exit(LogicalElement n) { n }

	IVisitable exit(ComparisonElement n) { n }

	IVisitable exit(ConstructionElement n) { n }

	IVisitable exit(Constructor n) { n }

	IVisitable exit(Relation n) { n }

	IVisitable exit(Type n) { n in typeInfo.typesToOptimize ? TYPE_STRING : n }

	IVisitable exit(BinaryExpr n) { n }

	IVisitable exit(GroupExpr n) { n }
}
