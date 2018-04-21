package org.codesimius.panda.actions.validation

import groovy.transform.Canonical
import org.codesimius.panda.actions.DefaultVisitor
import org.codesimius.panda.actions.RelationInfoVisitor
import org.codesimius.panda.actions.TypeInfoVisitor
import org.codesimius.panda.actions.VarInfoVisitor
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl2
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.ConstructionElement
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.system.Error

import static org.codesimius.panda.system.Error.error
import static org.codesimius.panda.system.Error.warn
import static org.codesimius.panda.system.SourceManager.recallStatic as recall

@Canonical
class MainValidator extends DefaultVisitor<IVisitable> {

	TypeInfoVisitor typeInfo
	RelationInfoVisitor relationInfo
	VarInfoVisitor varInfo

	private Set<String> tmpDeclaredTypes = [] as Set
	private Map<String, Integer> arities = [:]

	IVisitable exit(BlockLvl2 n) { n }

	void enter(RelDeclaration n) {
		checkArity(n.relation.name, n.types.size(), n)

		n.types.findAll { !it.isPrimitive() }
				.findAll { !(it in typeInfo.allTypes) }
				.each { error(recall(it), Error.TYPE_UNKNOWN, it.name) }
	}

	void enter(TypeDeclaration n) {
		if (n.type.name in tmpDeclaredTypes)
			error(recall(n), Error.DECL_MULTIPLE, n.type.name)
		tmpDeclaredTypes << n.type.name
	}

	void enter(Rule n) {
		def varsInHead = varInfo.vars[n.head]
		def varsInBody = varInfo.vars[n.body]
		def conVars = relationInfo.constructedVars[n]
		varsInHead.findAll { !(it in varsInBody) && !(it in conVars) }
				.each { error(recall(n), Error.VAR_UNKNOWN, it.name) }

		varsInBody.findAll { it.name != "_" }
				.findAll { !(it in varsInHead) }
				.findAll { varsInBody.count(it) == 1 }
				.each { warn(recall(n), Error.VAR_UNUSED, it.name) }
	}

	void enter(ConstructionElement n) {
		if (!(n.type in typeInfo.allTypes)) error(recall(n), Error.TYPE_UNKNOWN, n.type.name)
	}

	void enter(Constructor n) { if (!inDecl) checkRelation(n) }

	void enter(Relation n) { if (!inDecl) checkRelation(n) }

	def checkRelation(Relation n) {
		if (inRuleBody && !(n.name in relationInfo.declaredRelations))
			error(recall(n), Error.REL_NO_DECL, n.name)

		checkArity(n.name, n.arity, n)
	}

	def checkArity(String name, int arity, IVisitable n) {
		if (inRuleBody && typeInfo.allTypes.find { it.name == name } && arity != 1)
			error(recall(n), Error.REL_ARITY, name)

		def prevArity = arities[name]
		if (prevArity && prevArity != arity) error(recall(n), Error.REL_ARITY, name)
		if (!prevArity) arities[name] = arity
	}
}
