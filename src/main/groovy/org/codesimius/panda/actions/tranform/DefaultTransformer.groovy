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

class DefaultTransformer extends DefaultVisitor<IVisitable> {

	Set<RelDeclaration> extraRelDecls = [] as Set
	Set<TypeDeclaration> extraTypeDecls = [] as Set
	Set<Rule> extraRules = [] as Set

	IVisitable exit(BlockLvl2 n) {
		new BlockLvl2(m[n.datalog] as BlockLvl0, n.components.collect { m[it] as BlockLvl1 } as Set, n.instantiations)
	}

	IVisitable exit(BlockLvl1 n) {
		new BlockLvl1(n.name, n.superComponent, n.parameters, n.superParameters, m[n.datalog] as BlockLvl0)
	}

	void enter(BlockLvl0 n) {
		extraRelDecls = [] as Set
		extraTypeDecls = [] as Set
		extraRules = [] as Set
	}

	IVisitable exit(BlockLvl0 n) {
		// grep() returns all elements which satisfy Groovy truth (i.e. not null)
		def relDs = (n.relDeclarations.collect { m[it] as RelDeclaration } + extraRelDecls).grep() as Set
		def typeDs = (n.typeDeclarations.collect {
			m[it] as TypeDeclaration
		} + extraTypeDecls).grep() as Set
		def rs = (n.rules.collect { m[it] as Rule } + extraRules).grep() as Set
		new BlockLvl0(relDs, typeDs, rs)
	}

	IVisitable exit(RelDeclaration n) {
		new RelDeclaration(m[n.relation] as Relation, n.types.collect { m[it] as Type }, n.annotations)
	}

	IVisitable exit(TypeDeclaration n) { new TypeDeclaration(m[n.type] as Type, m[n.supertype] as Type, n.annotations) }

	IVisitable exit(Rule n) { new Rule(m[n.head] as IElement, m[n.body] as IElement, n.annotations) }

	IVisitable exit(AggregationElement n) {
		new AggregationElement(m[n.var] as VariableExpr, m[n.relation] as Relation, m[n.body] as IElement)
	}

	IVisitable exit(ComparisonElement n) { new ComparisonElement(m[n.expr] as BinaryExpr) }

	IVisitable exit(ConstructionElement n) {
		new ConstructionElement(m[n.constructor] as Constructor, m[n.type] as Type)
	}

	IVisitable exit(GroupElement n) { new GroupElement(m[n.element] as IElement) }

	IVisitable exit(LogicalElement n) { new LogicalElement(n.type, n.elements.collect { m[it] as IElement }) }

	IVisitable exit(NegationElement n) { new NegationElement(m[n.element] as IElement) }

	IVisitable exit(Constructor n) { new Constructor(n.name, n.exprs.collect { m[it] as IExpr }) }

	IVisitable exit(Relation n) { new Relation(n.name, n.exprs.collect { m[it] as IExpr }) }

	IVisitable exit(Type n) { n }

	IVisitable exit(BinaryExpr n) { new BinaryExpr(m[n.left] as IExpr, n.op, m[n.right] as IExpr) }

	IVisitable exit(ConstantExpr n) { n }

	IVisitable exit(GroupExpr n) { new GroupExpr(m[n.expr] as IExpr) }

	IVisitable exit(VariableExpr n) { n }
}
