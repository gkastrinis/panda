package org.clyze.deepdoop.actions

import org.clyze.deepdoop.datalog.IVisitable
import org.clyze.deepdoop.datalog.block.BlockLvl0
import org.clyze.deepdoop.datalog.block.BlockLvl2
import org.clyze.deepdoop.datalog.clause.RelDeclaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.clause.TypeDeclaration
import org.clyze.deepdoop.datalog.element.ConstructionElement
import org.clyze.deepdoop.datalog.element.LogicalElement
import org.clyze.deepdoop.datalog.element.NegationElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.VariableExpr

class SymbolTableVisitingActor extends DefaultVisitor<IVisitable> {

	@Delegate
	TypeInfoCollector typeInfoCollector = new TypeInfoCollector()
	@Delegate
	RelInfoCollector relInfoCollector = new RelInfoCollector()
	@Delegate
	ConInfoCollector conInfoCollector = new ConInfoCollector()

	IVisitable exit(BlockLvl2 n) { n }

	void enter(BlockLvl0 n) {
		typeInfoCollector.enter n
		relInfoCollector.enter n
	}

	IVisitable exit(BlockLvl0 n) {
		conInfoCollector.exit(n, typeInfoCollector.rootTypes, typeInfoCollector.subTypes)
		null
	}

	void enter(RelDeclaration n) {
		relInfoCollector.enter n
		conInfoCollector.enter n
	}

	void enter(TypeDeclaration n) {
		conInfoCollector.enter n
	}

	void enter(Rule n) {
		relInfoCollector.enter n
		conInfoCollector.enter n
	}

	IVisitable exit(Rule n) {
		relInfoCollector.exit n
		conInfoCollector.exit n
		null
	}

	void enter(ConstructionElement n) { conInfoCollector.enter n }

	IVisitable exit(LogicalElement n) {
		relInfoCollector.exit n
		null
	}

	IVisitable exit(NegationElement n) {
		relInfoCollector.exit n
		null
	}

	void enter(Constructor n) { enter(n as Relation) }

	void enter(Relation n) { relInfoCollector.enter(n, inRuleHead, inRuleBody) }

	void enter(Type n) { relInfoCollector.enter n }

	void enter(VariableExpr n) { relInfoCollector.enter(n, inRuleHead, inRuleBody) }
}
