package org.codesimius.panda.actions.tranform

import groovy.transform.Canonical
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl0
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.ConstructionElement
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.system.Compiler
import org.codesimius.panda.system.Error

import static org.codesimius.panda.datalog.Annotation.*
import static org.codesimius.panda.datalog.element.LogicalElement.combineElements
import static org.codesimius.panda.datalog.element.relation.Type.TYPE_STRING
import static org.codesimius.panda.datalog.expr.VariableExpr.gen1 as var1

@Canonical
class TypesTransformer extends DefaultTransformer {

	@Delegate
	Compiler compiler
	private BlockLvl0 currDatalog

	IVisitable visit(BlockLvl0 n) {
		n.typeDeclarations
				.findAll { it.supertype }
				.findAll { decl -> !n.typeDeclarations.any { it.type == decl.supertype } }
				.each { error(loc(it), Error.TYPE_UNKNOWN, it.supertype.name) }

		// Add default constructors
		n.rootTypes.findAll { !it.primitive }.each { root ->
			n.relDeclarations
					.findAll { it.relation.name == root.defaultConName }
					.each { error(loc(it), Error.REL_NAME_DEFCONSTR, it.relation.name) }

			extraRelDecls << new RelDeclaration(new Constructor(root.defaultConName, []), [TYPE_STRING, root], [CONSTRUCTOR] as Set)
		}
		currDatalog = n
		n.typeDeclarations.each { visit it }
		new BlockLvl0(n.relDeclarations + extraRelDecls, n.typeDeclarations, n.rules + extraRules)
	}

	IVisitable exit(TypeDeclaration n) {
		if (n.annotations[TYPEVALUES]) {
			def rootT = currDatalog.typeToRootType[n.type]
			n.annotations[TYPEVALUES].args.each { key, value ->
				def rel = new Relation("${n.type.name}:$key", [var1()])
				extraRelDecls << new RelDeclaration(rel, [n.type], [n.annotations[METADATA]] as Set)
				def con = new ConstructionElement(new Constructor(rootT.defaultConName, [value, var1()]), n.type)
				extraRules << new Rule(combineElements([con, rel]))
			}
		}
		return n
	}
}
