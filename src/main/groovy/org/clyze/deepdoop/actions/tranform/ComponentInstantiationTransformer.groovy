package org.clyze.deepdoop.actions.tranform

import org.clyze.deepdoop.actions.ConstructionInfoVisitingActor
import org.clyze.deepdoop.actions.RelationInfoVisitingActor
import org.clyze.deepdoop.datalog.IVisitable
import org.clyze.deepdoop.datalog.block.BlockLvl0
import org.clyze.deepdoop.datalog.block.BlockLvl1
import org.clyze.deepdoop.datalog.block.BlockLvl2
import org.clyze.deepdoop.datalog.clause.RelDeclaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.clause.TypeDeclaration
import org.clyze.deepdoop.datalog.element.ComparisonElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.BinaryExpr
import org.clyze.deepdoop.datalog.expr.GroupExpr
import org.clyze.deepdoop.system.ErrorId
import org.clyze.deepdoop.system.ErrorManager
import org.clyze.deepdoop.system.SourceManager

class ComponentInstantiationTransformer extends DummyTransformer {

	private RelationInfoVisitingActor relInfoActor
	// Info collection actor for original program
	private ConstructionInfoVisitingActor constructionInfoActor
	// Original program before instantiation
	private BlockLvl2 origP
	// Program after instantiation (only a global component)
	private BlockLvl2 instantiatedP
	// Current name used for instantiation
	private String currInstanceName
	// Current component being instantiated
	private BlockLvl1 currComp

	ComponentInstantiationTransformer() { actor = this }

	// Instantiate components (add transformed contents in a single component)
	// A component might be visited multiple times (depending on instantiations)
	// Components with no instantiations are dropped
	IVisitable visit(BlockLvl2 n) {
		origP = n
		instantiatedP = new BlockLvl2()

		relInfoActor = new RelationInfoVisitingActor()
		relInfoActor.visit origP
		constructionInfoActor = new ConstructionInfoVisitingActor()
		constructionInfoActor.visit origP

		visit origP.datalog
		origP.instantiations.each { inst ->
			currInstanceName = inst.id
			currComp = n.components.find { it.name == inst.component }
			if (!currComp)
				ErrorManager.error(ErrorId.COMP_UNKNOWN, inst.component)
			if (currComp.parameters.size() != inst.parameters.size())
				ErrorManager.error(ErrorId.COMP_INST_ARITY, inst.parameters, inst.component, inst.id)
			visit currComp.datalog
		}
		instantiatedP
	}

	IVisitable exit(BlockLvl1 n, Map m) { null }

	IVisitable exit(BlockLvl0 n, Map m) {
		n.relDeclarations.each { instantiatedP.datalog.relDeclarations << (m[it] as RelDeclaration) }
		n.typeDeclarations.each { instantiatedP.datalog.typeDeclarations << (m[it] as TypeDeclaration) }
		n.rules.each { instantiatedP.datalog.rules << (m[it] as Rule) }
		null
	}

	IVisitable exit(Constructor n, Map m) { new Constructor(rename(n.name), n.exprs) }

	IVisitable exit(Relation n, Map m) {
		def loc = SourceManager.instance.recall(n)
		if (inRuleHead && n.name.contains("@"))
			ErrorManager.error(loc, ErrorId.REL_EXT_INVALID)

		def origName = n.name
		if (!origName.contains("@"))
			return new Relation(rename(origName), n.exprs)

		def (simpleName, parameter) = origName.split("@")
		// Global space
		if (!currComp) {
			if (!origP.instantiations.any { it.id == parameter })
				ErrorManager.error(loc, ErrorId.COMP_UNKNOWN, parameter as String)
			return new Relation("$parameter:$simpleName", n.exprs)
		} else {
			def paramIndex = currComp.parameters.findIndexOf { it == parameter }
			if (paramIndex == -1)
				ErrorManager.error(loc, ErrorId.REL_EXT_UNKNOWN, parameter as String)

			def instParameter = origP.instantiations.find { it.id == currInstanceName }.parameters[paramIndex]
			def externalName = instParameter == "_" ? simpleName : "$instParameter:$simpleName"

			BlockLvl0 externalTemplateDatalog
			if (instParameter == "_")
				externalTemplateDatalog = origP.datalog
			else {
				def name = origP.instantiations.find { it.id == instParameter }.component
				externalTemplateDatalog = origP.components.find { it.name == name }.datalog
			}
			if (!relInfoActor.declaredRelations[externalTemplateDatalog].any { it.name == simpleName })
				ErrorManager.error(loc, ErrorId.REL_NO_DECL_REC, simpleName as String)

			return new Relation(externalName, n.exprs)
		}
	}

	IVisitable exit(Type n, Map m) { n.isPrimitive() ? n : new Type(rename(n.name)) }

	String rename(String name) { currInstanceName ? "$currInstanceName:$name" : name }

	// Overrides to avoid unneeded allocations

	IVisitable exit(ComparisonElement n, Map m) { n }

	IVisitable exit(BinaryExpr n, Map m) { n }

	IVisitable exit(GroupExpr n, Map m) { n }
}
