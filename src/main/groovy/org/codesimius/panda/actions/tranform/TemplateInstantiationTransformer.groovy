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

class TemplateInstantiationTransformer extends DefaultTransformer {

	// Current template being instantiated
	private BlockLvl1 currTemplate
	// Current resulting template from an instantiation
	private BlockLvl1 newCurrTemplate
	// Keep a mapping from current template parameter to the appropriate instantiation parameter
	private Map<String, String> mapParams

	// Create new templates for each instantiation with the appropriate parameter mappings
	// A template might be visited multiple times (depending on instantiations)
	// Templates with no instantiations are skipped
	IVisitable visit(BlockLvl2 n) {
		// Extra dummy templates to keep track of inheritance relationships
		def extraTemplates = []

		def newTemplates = n.instantiations.collect { inst ->
			newCurrTemplate = new BlockLvl1(inst.id)

			currTemplate = n.templates.find { it.name == inst.template }
			// A list of indexes of super parameters in the original parameter list
			// e.g. in `template A <X,Y,Z> : B <Z, X>`, we get [2, 0] (initially it is [0, 1, 2])
			def indexes = (0..<inst.parameters.size())
			def computeMappings = {
				mapParams = currTemplate.parameters.withIndex().collectEntries { param, int i -> [(param): inst.parameters[indexes[i]]] }
			}
			computeMappings()
			visit currTemplate

			while (currTemplate.superTemplate) {
				extraTemplates << new BlockLvl1(currTemplate.name, currTemplate.superTemplate)

				indexes = currTemplate.superParameters.collect { indexes[currTemplate.parameters.indexOf(it)] }
				currTemplate = n.templates.find { it.name == currTemplate.superTemplate }
				computeMappings()
				visit currTemplate
			}

			newCurrTemplate
		}

		// Keep instantiations to use in the dependency graph generation
		new BlockLvl2(n.datalog, newTemplates + extraTemplates, n.instantiations)
	}

	IVisitable exit(BlockLvl1 n) { n }

	IVisitable exit(BlockLvl0 n) {
		newCurrTemplate.datalog.with {
			relDeclarations += n.relDeclarations
			typeDeclarations += n.typeDeclarations
			rules += n.rules.collect { m[it] as Rule }
		}
		null
	}

	IVisitable exit(ComparisonElement n) { n }

	IVisitable exit(Constructor n) { n }

	IVisitable exit(Relation n) {
		if (!n.name.contains(".") || !currTemplate)
			return n
		else {
			def (String parameter, simpleName) = n.name.split("\\.")
			return new Relation("${mapParams[parameter]}.$simpleName", n.exprs)
		}
	}

	IVisitable exit(BinaryExpr n) { n }

	IVisitable exit(GroupExpr n) { n }
}
