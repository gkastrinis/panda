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
import org.codesimius.panda.datalog.expr.UnaryExpr

class TemplateFlatteningTransformer extends DefaultTransformer {

	// Program after instantiation (only a global "template" / scope)
	private BlockLvl2 instantiatedP
	// Current template being instantiated
	private BlockLvl1 currTemplate

	void enter(BlockLvl2 n) { instantiatedP = new BlockLvl2() }

	IVisitable exit(BlockLvl2 n) { instantiatedP }

	void enter(BlockLvl1 n) { currTemplate = n }

	IVisitable exit(BlockLvl1 n) { currTemplate = null }

	IVisitable exit(BlockLvl0 n) {
		instantiatedP.datalog.with {
			relDeclarations += n.relDeclarations.collect { m[it] as RelDeclaration }
			typeDeclarations += n.typeDeclarations.collect { m[it] as TypeDeclaration }
			rules += n.rules.collect { m[it] as Rule }
		}
		null
	}

	IVisitable exit(ComparisonElement n) { n }

	IVisitable exit(Constructor n) {
		def name = rename(n.name)
		return n.name == name ? n : new Constructor(name, n.exprs)
	}

	IVisitable exit(Relation n) {
		def name = rename(n.name)
		return n.name == name ? n : new Relation(name, n.exprs)
	}

	IVisitable exit(Type n) {
		def name = rename(n.name)
		return n.name == name ? n : new Type(name)
	}

	IVisitable exit(BinaryExpr n) { n }

	IVisitable exit(UnaryExpr n) { n }

	IVisitable exit(GroupExpr n) { n }

	def rename(def name) {
		// Global scope, primitive type, or qualified name (from another template)
		if (!currTemplate || Type.isPrimitive(name) || (name.contains(".") && !name.startsWith("_."))) return name
		// Inside a template and starts with "_."
		if (name.startsWith("_.")) return name[2..-1]
		// Inside a template
		return "${currTemplate.name}.$name"
	}
}
