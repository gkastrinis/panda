package org.codesimius.panda.actions.tranform

import org.codesimius.panda.actions.RelationInfoVisitingActor
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
import org.codesimius.panda.system.Error

import static org.codesimius.panda.system.Error.error
import static org.codesimius.panda.system.SourceManager.recallStatic as recall

class ComponentInstantiationTransformer extends DefaultTransformer {

	// Info collection actor for original program
	private RelationInfoVisitingActor relationInfo = new RelationInfoVisitingActor()
	// Original program before instantiation
	private BlockLvl2 origP
	// Program after instantiation (only a global component)
	private BlockLvl2 instantiatedP
	// Current name used for instantiation
	private String currInstanceName
	// Current component being instantiated
	private BlockLvl1 currComp

	// Instantiate components (add transformed contents in a single component)
	// A component might be visited multiple times (depending on instantiations)
	// Components with no instantiations are dropped
	IVisitable visit(BlockLvl2 n) {
		origP = n
		instantiatedP = new BlockLvl2()

		relationInfo.visit origP

		visit origP.datalog
		origP.instantiations.each { inst ->
			currInstanceName = inst.id
			currComp = n.components.find { it.name == inst.component }
			if (!currComp)
				error(Error.COMP_UNKNOWN, inst.component)
			if (currComp.parameters.size() != inst.parameters.size())
				error(Error.COMP_INST_ARITY, inst.parameters, inst.component, inst.id)
			visit currComp.datalog
		}
		instantiatedP
	}

	IVisitable exit(BlockLvl1 n) { n }

	IVisitable exit(BlockLvl0 n) {
		n.relDeclarations.each { instantiatedP.datalog.relDeclarations << (m[it] as RelDeclaration) }
		n.typeDeclarations.each { instantiatedP.datalog.typeDeclarations << (m[it] as TypeDeclaration) }
		n.rules.each { instantiatedP.datalog.rules << (m[it] as Rule) }
		null
	}

	IVisitable exit(ComparisonElement n) { n }

	IVisitable exit(Constructor n) { new Constructor(rename(n.name), n.exprs) }

	IVisitable exit(Relation n) {
		if (n.name.contains("@") && (inDecl || inRuleHead)) error(recall(n), Error.REL_EXT_INVALID)

		def origName = n.name
		if (!origName.contains("@"))
			return new Relation(rename(origName), n.exprs)

		def (simpleName, parameter) = origName.split("@")
		// Global space
		if (!currComp) {
			if (!origP.instantiations.any { it.id == parameter })
				error(recall(n), Error.COMP_UNKNOWN, parameter as String)
			return new Relation("$parameter:$simpleName", n.exprs)
		} else {
			def paramIndex = currComp.parameters.findIndexOf { it == parameter }
			if (paramIndex == -1)
				error(recall(n), Error.COMP_UNKNOWN_PARAM, parameter as String)

			def instParameter = origP.instantiations.find {
				it.id == currInstanceName
			}.parameters[paramIndex]
			def externalName = instParameter == "_" ? simpleName : "$instParameter:$simpleName"

			BlockLvl0 externalTemplateDatalog
			if (instParameter == "_")
				externalTemplateDatalog = origP.datalog
			else {
				def name = origP.instantiations.find { it.id == instParameter }.component
				externalTemplateDatalog = origP.components.find { it.name == name }.datalog
			}
			if (!relationInfo.declaredRelationsPerBlock[externalTemplateDatalog].any { it == simpleName })
				error(recall(n), Error.REL_EXT_NO_DECL, simpleName as String)

			return new Relation(externalName, n.exprs)
		}
	}

	IVisitable exit(Type n) { n.isPrimitive() ? n : new Type(rename(n.name)) }

	IVisitable exit(BinaryExpr n) { n }

	IVisitable exit(GroupExpr n) { n }

	def rename(def name) { currInstanceName ? "$currInstanceName:$name" : name }
}