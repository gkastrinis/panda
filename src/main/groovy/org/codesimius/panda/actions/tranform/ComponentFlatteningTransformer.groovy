package org.codesimius.panda.actions.tranform

import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl0
import org.codesimius.panda.datalog.block.BlockLvl1
import org.codesimius.panda.datalog.block.BlockLvl2
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.ComparisonElement
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.element.relation.Type
import org.codesimius.panda.datalog.expr.BinaryExpr
import org.codesimius.panda.datalog.expr.GroupExpr

class ComponentFlatteningTransformer extends DefaultTransformer {

	// Program after instantiation (only a global component)
	private BlockLvl2 instantiatedP
	// Current component being instantiated
	private BlockLvl1 currComp

	void enter(BlockLvl2 n) { instantiatedP = new BlockLvl2() }

	IVisitable exit(BlockLvl2 n) { instantiatedP }

	void enter(BlockLvl1 n) { currComp = n }

	IVisitable exit(BlockLvl1 n) { currComp = null }

	IVisitable exit(BlockLvl0 n) {
		instantiatedP.datalog.with {
			relDeclarations += n.relDeclarations.collect { m[it] as RelDeclaration }
			typeDeclarations += n.typeDeclarations.collect { m[it] as TypeDeclaration }
			rules += n.rules.collect { m[it] as Rule }
		}
		null
	}

	IVisitable exit(ComparisonElement n) { n }

	IVisitable exit(Constructor n) { new Constructor(rename(n.name), n.exprs) }

	IVisitable exit(Relation n) {
		if (!n.name.contains("@"))
			return new Relation(rename(n.name), n.exprs)
		else {
			def (simpleName, parameter) = n.name.split("@")
			return new Relation(parameter == "_" ? simpleName : "$parameter:$simpleName", n.exprs)
		}
	}

	IVisitable exit(Type n) { n.isPrimitive() ? n : new Type(rename(n.name)) }

	IVisitable exit(BinaryExpr n) { n }

	IVisitable exit(GroupExpr n) { n }

	def rename(def name) { currComp ? "${currComp.name}:$name" : name }
}
