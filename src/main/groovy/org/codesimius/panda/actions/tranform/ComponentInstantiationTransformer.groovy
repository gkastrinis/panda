package org.codesimius.panda.actions.tranform

import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl0
import org.codesimius.panda.datalog.block.BlockLvl1
import org.codesimius.panda.datalog.block.BlockLvl2
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.element.ComparisonElement
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.expr.BinaryExpr
import org.codesimius.panda.datalog.expr.GroupExpr

class ComponentInstantiationTransformer extends DefaultTransformer {

	// Current component being instantiated
	private BlockLvl1 currComp
	// Current resulting component from an instantiation
	private BlockLvl1 newCurrComp
	// Keep a mapping from current component parameter to the appropriate instantiation parameter
	private Map<String, String> mapParams

	// External relations (with "@") that need to be checked for having a declaration
	//private Set<String> pendingRelations = [] as Set

	// Create new components for each instantiation with the appropriate parameter mappings
	// A component might be visited multiple times (depending on instantiations)
	// Components with no instantiations are skipped
	IVisitable visit(BlockLvl2 n) {
		// Extra dummy components to keep track of inheritance relationships
		def extraComponents = []

		def newComponents = n.instantiations.collect { inst ->
			newCurrComp = new BlockLvl1(inst.id)

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
				extraComponents << new BlockLvl1(currComp.name, currComp.superComponent)

				indexes = currComp.superParameters.collect { indexes[currComp.parameters.indexOf(it)] }
				currComp = n.components.find { it.name == currComp.superComponent }
				computeMappings()
				visit currComp
			}

			newCurrComp
		}

		//def relationInfo = new RelationInfoVisitor()
		//relationInfo.visit instantiatedP
		//pendingRelations.findAll { !(it in relationInfo.declaredRelations) }.each {
		//	error(recall(n), Error.REL_EXT_NO_DECL, it.split(":").drop(1).join(":"))
		//}

		// Keep instantiations to use in the dependency graph generation
		new BlockLvl2(n.datalog, newComponents + extraComponents, n.instantiations)
	}

	IVisitable exit(BlockLvl1 n) { n }

	IVisitable exit(BlockLvl0 n) {
		newCurrComp.datalog.with {
			relDeclarations += n.relDeclarations
			typeDeclarations += n.typeDeclarations
			rules += n.rules.collect { m[it] as Rule }
		}
		null
	}

	IVisitable exit(ComparisonElement n) { n }

	IVisitable exit(Constructor n) { n }

	IVisitable exit(Relation n) {
		if (!n.name.contains("@") || !currComp)
			return n
		else {
			def (simpleName, String parameter) = n.name.split("@")
			return new Relation("$simpleName@${mapParams[parameter]}", n.exprs)
		}
	}

	IVisitable exit(BinaryExpr n) { n }

	IVisitable exit(GroupExpr n) { n }
}
