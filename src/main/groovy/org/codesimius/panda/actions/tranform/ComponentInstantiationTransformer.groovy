package org.codesimius.panda.actions.tranform

import org.codesimius.panda.actions.RelationInfoVisitor
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

	// Program after instantiation (only a global component)
	private BlockLvl2 instantiatedP
	// Current name used for instantiation
	private String currInstanceName
	// Current component being instantiated
	private BlockLvl1 currComp
	// Keep a mapping from current component parameter to the appropriate instantiation parameter
	private Map<String, String> mapParams
	// External relations (with "@") that need to be checked for having a declaration
	private Set<String> pendingRelations = [] as Set

	// Instantiate components (add transformed contents in a single component)
	// A component might be visited multiple times (depending on instantiations)
	// Components with no instantiations are skipped
	IVisitable visit(BlockLvl2 n) {
		instantiatedP = new BlockLvl2()

		visit n.datalog

		n.instantiations.each { inst ->
			currInstanceName = inst.id
			currComp = n.components.find { it.name == inst.component }
			// A list of indexes of super parameters in the original parameter list
			// e.g. in `component A <X,Y,Z> : B <Z, X>`, we get [2, 0] (initially it is [0, 1, 2])
			def indexes = (0..<inst.parameters.size())
			def computeMappings = {
				mapParams = currComp.parameters.withIndex().collectEntries { param, int i -> [(param): inst.parameters[indexes[i]]] }
			}
			computeMappings()
			visit currComp
			while (currComp.superComponent) {
				indexes = currComp.superParameters.collect { indexes[currComp.parameters.indexOf(it)] }
				currComp = n.components.find { it.name == currComp.superComponent }
				computeMappings()
				visit currComp
			}
		}

		def relationInfo = new RelationInfoVisitor()
		relationInfo.visit instantiatedP
		pendingRelations.findAll { !(it in relationInfo.declaredRelations) }.each {
			error(recall(n), Error.REL_EXT_NO_DECL, it.split(":").drop(1).join(":"))
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
		if (!n.name.contains("@"))
			return new Relation(rename(n.name), n.exprs)

		def (String simpleName, String parameter) = n.name.split("@")
		// null when in global space
		if (!currComp) {
			pendingRelations << ("$parameter:$simpleName" as String)
			return new Relation("$parameter:$simpleName", n.exprs)
		} else {
			def newParameter = mapParams[parameter]
			def newName = newParameter == "_" ? simpleName : "$newParameter:$simpleName"
			pendingRelations << (newName as String)
			return new Relation(newName, n.exprs)
		}
	}

	IVisitable exit(Type n) { n.isPrimitive() ? n : new Type(rename(n.name)) }

	IVisitable exit(BinaryExpr n) { n }

	IVisitable exit(GroupExpr n) { n }

	def rename(def name) { currInstanceName ? "$currInstanceName:$name" : name }
}
