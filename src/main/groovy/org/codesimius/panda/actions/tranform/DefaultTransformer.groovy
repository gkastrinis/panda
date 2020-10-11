package org.codesimius.panda.actions.tranform

import org.codesimius.panda.actions.DefaultVisitor
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl0
import org.codesimius.panda.datalog.block.BlockLvl1
import org.codesimius.panda.datalog.block.BlockLvl2
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.*
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.element.relation.Type
import org.codesimius.panda.datalog.expr.*

import static org.codesimius.panda.datalog.element.LogicalElement.combineElements

abstract class DefaultTransformer extends DefaultVisitor<IVisitable> {

	Set<RelDeclaration> extraRelDecls = [] as Set
	Set<TypeDeclaration> extraTypeDecls = [] as Set
	Set<Rule> extraRules = [] as Set

	IVisitable exit(BlockLvl2 n) {
		def newDatalog = m[n.datalog] as BlockLvl0
		def newTemplates = n.templates.collect { m[it] as BlockLvl1 } as Set
		n.datalog === newDatalog && allSame(n.templates, newTemplates) ? n : new BlockLvl2(newDatalog, newTemplates, n.instantiations)
	}

	IVisitable exit(BlockLvl1 n) {
		def newDatalog = m[n.datalog] as BlockLvl0
		n.datalog === newDatalog ? n : new BlockLvl1(n.name, n.superTemplate, n.parameters, n.superParameters, newDatalog)
	}

	void enter(BlockLvl0 n) {
		extraRelDecls = [] as Set
		extraTypeDecls = [] as Set
		extraRules = [] as Set
	}

	IVisitable exit(BlockLvl0 n) {
		def newRDS = n.relDeclarations.collect { m[it] as RelDeclaration }
		def newTDS = n.typeDeclarations.collect { m[it] as TypeDeclaration }
		def newRLS = n.rules.collect { m[it] as Rule }

		if (extraRelDecls.empty && extraTypeDecls.empty && extraRules.empty &&
				allSame(n.relDeclarations, newRDS) && allSame(n.typeDeclarations, newTDS) && allSame(n.rules, newRLS)) return n
		// grep() returns all elements which satisfy Groovy truth (i.e. not null)
		return new BlockLvl0((newRDS + extraRelDecls).grep() as Set, (newTDS + extraTypeDecls).grep() as Set, (newRLS + extraRules).grep() as Set)
	}

	IVisitable exit(RelDeclaration n) {
		def newRelation = m[n.relation] as Relation
		def newTypes = n.types.collect { m[it] as Type }
		n.relation === newRelation && allSame(n.types, newTypes) ? n : new RelDeclaration(newRelation, newTypes, n.annotations)
	}

	IVisitable exit(TypeDeclaration n) {
		def newType = m[n.type] as Type
		def newSupertype = m[n.supertype] as Type
		n.type === newType && n.supertype === newSupertype ? n : new TypeDeclaration(newType, newSupertype, n.annotations)
	}

	IVisitable exit(Rule n) {
		def newHead = m[n.head] as IElement
		def newBody = m[n.body] as IElement
		n.head === newHead && n.body === newBody ? n : new Rule(newHead, newBody, n.annotations)
	}

	IVisitable exit(AggregationElement n) {
		def newVar = m[n.var] as VariableExpr
		def newRelation = m[n.relation] as Relation
		def newBody = m[n.body] as IElement
		n.var === newVar && n.relation === newRelation && n.body === newBody ? n : new AggregationElement(newVar, newRelation, newBody)
	}

	IVisitable exit(ComparisonElement n) {
		def newExpr = m[n.expr] as BinaryExpr
		n.expr === newExpr ? n : new ComparisonElement(newExpr)
	}

	IVisitable exit(ConstructionElement n) {
		def newConstructor = m[n.constructor] as Constructor
		def newType = m[n.type] as Type
		n.constructor === newConstructor && n.type === newType ? n : new ConstructionElement(newConstructor, newType)
	}

	IVisitable exit(LogicalElement n) {
		def newElements = n.elements.collect { m[it] as IElement }
		allSame(n.elements, newElements) ? n : combineElements(n.kind, newElements)
	}

	IVisitable exit(NegationElement n) {
		def newElement = m[n.element] as IElement
		n.element === newElement ? n : new NegationElement(newElement)
	}

	IVisitable exit(Constructor n) {
		def newExprs = n.exprs.collect { m[it] as IExpr }
		allSame(n.exprs, newExprs) ? n : new Constructor(n.name, newExprs)
	}

	IVisitable exit(Relation n) {
		def newExprs = n.exprs.collect { m[it] as IExpr }
		allSame(n.exprs, newExprs) ? n : new Relation(n.name, newExprs)
	}

	IVisitable exit(Type n) { n }

	IVisitable exit(BinaryExpr n) {
		def newLeft = m[n.left] as IExpr
		def newRight = m[n.right] as IExpr
		n.left === newLeft && n.right === newRight ? n : new BinaryExpr(newLeft, n.op, newRight)
	}

	IVisitable exit(UnaryExpr n) {
		def newExpr = m[n.expr] as IExpr
		n.expr === newExpr ? n : new UnaryExpr(n.op, newExpr)
	}

	IVisitable exit(ConstantExpr n) { n }

	IVisitable exit(GroupExpr n) {
		def newExpr = m[n.expr] as IExpr
		n.expr === newExpr ? n : new GroupExpr(newExpr)
	}

	IVisitable exit(VariableExpr n) { n }

	static def allSame(Collection orig, Collection other) { orig.withIndex().every { it, int i -> it === other[i] } }
}
