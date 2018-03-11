package org.clyze.deepdoop.actions.tranform

import org.clyze.deepdoop.actions.DefaultVisitor
import org.clyze.deepdoop.actions.TDummyActor
import org.clyze.deepdoop.datalog.IVisitable
import org.clyze.deepdoop.datalog.block.BlockLvl0
import org.clyze.deepdoop.datalog.block.BlockLvl1
import org.clyze.deepdoop.datalog.block.BlockLvl2
import org.clyze.deepdoop.datalog.clause.RelDeclaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.clause.TypeDeclaration
import org.clyze.deepdoop.datalog.element.*
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.*

class DummyTransformer extends DefaultVisitor<IVisitable> implements TDummyActor<IVisitable> {

	protected Set<RelDeclaration> extraRelDecls = [] as Set
	protected Set<TypeDeclaration> extraTypeDecls = [] as Set
	protected Set<Rule> extraRules = [] as Set

	DummyTransformer() { actor = this }

	IVisitable exit(BlockLvl2 n, Map m) {
		new BlockLvl2(m[n.datalog] as BlockLvl0, n.components.collect { m[it] as BlockLvl1 } as Set, n.instantiations)
	}

	IVisitable exit(BlockLvl1 n, Map m) {
		new BlockLvl1(n.name, n.superComponent, n.parameters, n.superParameters, m[n.datalog] as BlockLvl0)
	}

	IVisitable exit(BlockLvl0 n, Map m) {
		// grep() returns all elements which satisfy Groovy truth (i.e. not null)
		def relDs = (n.relDeclarations.collect { m[it] as RelDeclaration } + extraRelDecls).grep() as Set
		def typeDs = (n.typeDeclarations.collect { m[it] as TypeDeclaration } + extraTypeDecls).grep() as Set
		def rs = (n.rules.collect { m[it] as Rule } + extraRules).grep() as Set
		new BlockLvl0(relDs, typeDs, rs)
	}

	IVisitable exit(RelDeclaration n, Map m) {
		new RelDeclaration(m[n.relation] as Relation, n.types.collect { m[it] as Type }, n.annotations)
	}

	IVisitable exit(TypeDeclaration n, Map m) {
		new TypeDeclaration(m[n.type] as Type, m[n.supertype] as Type , n.annotations)
	}

	IVisitable exit(Rule n, Map m) {
		new Rule(m[n.head] as IElement, m[n.body] as IElement, n.annotations)
	}

	IVisitable exit(AggregationElement n, Map m) {
		new AggregationElement(m[n.var] as VariableExpr, m[n.relation] as Relation, m[n.body] as IElement)
	}

	IVisitable exit(ComparisonElement n, Map m) {
		new ComparisonElement(m[n.expr] as BinaryExpr)
	}

	IVisitable exit(ConstructionElement n, Map m) {
		new ConstructionElement(m[n.constructor] as Constructor, m[n.type] as Type)
	}

	IVisitable exit(GroupElement n, Map m) {
		new GroupElement(m[n.element] as IElement)
	}

	IVisitable exit(LogicalElement n, Map m) {
		new LogicalElement(n.type, n.elements.collect { m[it] as IElement })
	}

	IVisitable exit(NegationElement n, Map m) {
		new NegationElement(m[n.element] as IElement)
	}

	IVisitable exit(Constructor n, Map m) {
		new Constructor(n.name, n.exprs.collect { m[it] as IExpr })
	}

	IVisitable exit(Relation n, Map m) {
		new Relation(n.name, n.exprs.collect { m[it] as IExpr })
	}

	IVisitable exit(Type n, Map m) { n }

	IVisitable exit(BinaryExpr n, Map m) {
		new BinaryExpr(m[n.left] as IExpr, n.op, m[n.right] as IExpr)
	}

	IVisitable exit(ConstantExpr n, Map m) { n }

	IVisitable exit(GroupExpr n, Map m) {
		new GroupExpr(m[n.expr] as IExpr)
	}

	IVisitable exit(VariableExpr n, Map m) { n }
}
