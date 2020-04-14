package org.codesimius.panda.actions.tranform

import groovy.transform.Canonical
import org.codesimius.panda.datalog.AnnotationSet
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl0
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.ComparisonElement
import org.codesimius.panda.datalog.element.ConstructionElement
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.element.relation.Type
import org.codesimius.panda.datalog.expr.BinaryOp
import org.codesimius.panda.datalog.expr.ConstantExpr
import org.codesimius.panda.datalog.expr.IExpr
import org.codesimius.panda.datalog.expr.VariableExpr

import static org.codesimius.panda.datalog.Annotation.*
import static org.codesimius.panda.datalog.element.relation.Type.TYPE_STRING
import static org.codesimius.panda.datalog.expr.VariableExpr.gen1 as var1

@Canonical
class TypesOptimizer extends DefaultTransformer {

	BlockLvl0 datalog

	private Set<Type> typesToOptimize = [] as Set
	private Map<IExpr, IExpr> mapExprs = [:]

	void enter(BlockLvl0 n) {
		datalog = n

		n.typeDeclarations
				.findAll { it.annotations[TYPE]["opt"] }
				.each { typesToOptimize += n.getExtendedSubTypesOf(it.type) }

		typesToOptimize.each { t ->
			n.superTypesOrdered.remove t
			n.subTypes.remove t
			n.typeToRootType.remove t
		}
	}

	IVisitable exit(RelDeclaration n) {
		// Remove constructor declarations for optimized types
		if (CONSTRUCTOR in n.annotations && n.types.last() in typesToOptimize) return null

		def metadata = METADATA.template([types: new ConstantExpr(n.types*.name.join(" x "))])
		new RelDeclaration(m[n.relation] as Relation, n.types.collect { m[it] as Type }, n.annotations << metadata)
	}

	IVisitable exit(TypeDeclaration n) {
		def metadata = METADATA.template([types: new ConstantExpr(n.type.name)])

		if (n.type in typesToOptimize) {
			extraRelDecls << new RelDeclaration(new Relation(n.type.name), [TYPE_STRING], new AnnotationSet(metadata))
			if (n.supertype)
				extraRules << new Rule(new Relation(n.supertype.name, [var1()]), new Relation(n.type.name, [var1()]))
			return null
		} else
			return new TypeDeclaration(m[n.type] as Type, m[n.supertype] as Type, n.annotations << metadata)
	}

	IVisitable visit(Rule n) {
		parentAnnotations = n.annotations
		mapExprs = [:]
		def head = n.head
		def body = n.body
		// 1st pass: find all changes (e.g. change x with y)
		// 2nd pass: apply changes
		2.times {
			inRuleHead = true
			head = visit head
			inRuleHead = false

			inRuleBody = true
			if (n.body) body = visit body
			inRuleBody = false
		}
		m[n.head] = head
		if (n.body) m[n.body] = body
		mapExprs = [:]

		super.exit n
	}

	IVisitable exit(ConstructionElement n) {
		if (n.type in typesToOptimize) {
			def keyExpr = n.constructor.keyExprs.first()
			mapExprs << [(n.constructor.valueExpr): keyExpr]
			return new Relation(n.type.name, [keyExpr])
		} else
			return n
	}

	IVisitable exit(Constructor n) {
		if (inDecl) return n

		def baseT = datalog.constructorToBaseType[n.name]
		if (baseT in typesToOptimize) {
			if (inRuleBody) {
				return new ComparisonElement(n.valueExpr, BinaryOp.EQ, n.keyExprs.first())
			} else {
				mapExprs << [(n.valueExpr): n.keyExprs.first()]
				return ComparisonElement.TRIVIALLY_TRUE
			}
		} else
			return super.exit(n)
	}

	IVisitable exit(Relation n) { inDecl ? n : super.exit(n) }

	IVisitable exit(Type n) { n in typesToOptimize ? TYPE_STRING : n }

	IVisitable exit(VariableExpr n) { mapExprs[n] ?: n }
}
