package org.clyze.deepdoop.actions.tranform.souffle

import groovy.transform.Canonical
import org.clyze.deepdoop.actions.SymbolTableVisitingActor
import org.clyze.deepdoop.actions.tranform.DefaultTransformer
import org.clyze.deepdoop.actions.tranform.TypeInferenceTransformer
import org.clyze.deepdoop.datalog.IVisitable
import org.clyze.deepdoop.datalog.block.BlockLvl0
import org.clyze.deepdoop.datalog.clause.RelDeclaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.clause.TypeDeclaration
import org.clyze.deepdoop.datalog.element.ConstructionElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.RecordType
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.datalog.expr.IExpr
import org.clyze.deepdoop.datalog.expr.RecordExpr
import org.clyze.deepdoop.datalog.expr.VariableExpr

import static org.clyze.deepdoop.datalog.element.relation.Type.TYPE_STRING
import static org.clyze.deepdoop.datalog.expr.VariableExpr.gen1 as var1

// (1) For each constructor, a new type is generated that
// represents its structure. The structure is a record of
// all key parameters.
// (2) Every type in the same hierarchy must have the same
// structure in order to enable polymorphism. The structure
// comprises of a selector record, i.e. only one part is
// non-nil at a time. Each part in the record represents a
// constructor type (from (1)). The order is not strictly
// defined but it is consistent throughout the transformations.
// (3) For every type, a new relation is generated in order
// to keep entries of that type.
// (4) Additionally, rules are generate to propagate entries
// from subtypes to supertypes.
@Canonical
class ConstructorTransformer extends DefaultTransformer {

	SymbolTableVisitingActor symbolTable
	TypeInferenceTransformer typeInferenceActor

	// Recurring constant
	private IExpr NIL = new ConstantExpr("nil")

	// Re: 2
	private Map<Type, List<Type>> typeToRecord = [:]
	private Map<Type, Type> typeToCommonType = [:]

	// The variable currently being constructed
	private VariableExpr tmpConVar
	// The internal record representing the constructed value
	private RecordExpr tmpConRecord
	// The internal record type, e.g. ConType01
	private Type tmpConRecordType
	// Type for current relation parameter (in rule head)
	private Type tmpCurrType

	void enter(BlockLvl0 n) {
		super.enter n
		// Re: (2)
		symbolTable.rootTypes.each { root ->
			if (root in symbolTable.typesToOptimize) return

			def rootInternalType = new Type("_${root.name}")
			def types = [root] + symbolTable.subTypes[root]
			types.each { typeToCommonType[it] = rootInternalType }
			def constructors = types.collect {
				symbolTable.constructorsPerType[it]
			}.flatten() as Set<RelDeclaration>
			constructors.each {
				extraTypeDecls << new TypeDeclaration(new Type(it.relation.name), new RecordType(it.types.dropRight(1)), [] as Set)
			}
			def record = constructors.collect { new Type(it.relation.name) }
			types.each { typeToRecord[it] = record }
			extraTypeDecls << new TypeDeclaration(rootInternalType, new RecordType(record), [] as Set)
		}
	}

	IVisitable exit(RelDeclaration n) {
		// Re: (1)
		new RelDeclaration(n.relation, n.types.collect { map(it) }, n.annotations)
	}

	IVisitable exit(TypeDeclaration n) {
		// Re: 3
		extraRelDecls << new RelDeclaration(new Relation(n.type.name), [map(n.type)], n.annotations)
		// Re: 4
		if (n.supertype)
			extraRules << new Rule(new Relation(n.supertype.name, [var1()]), new Relation(n.type.name, [var1()]))
		null
	}

	IVisitable visit(Rule n) {
		inRuleHead = true
		def head = n.head
		symbolTable.constructionsOrderedPerRule[n].each {
			// Map to the updated (from a previous iteration)
			// version of the constructor, if any
			def con = (m[it] ?: it) as ConstructionElement
			tmpConVar = con.constructor.valueExpr as VariableExpr
			tmpConRecord = new RecordExpr(con.constructor.keyExprs)
			tmpConRecordType = new Type(con.constructor.name)
			head = visit head
		}
		// Remove construction from global map `m`
		// since they might reappear in a different rule
		symbolTable.constructionsOrderedPerRule[n].each { m.remove(it) }
		m[n.head] = head
		inRuleHead = false

		inRuleBody = true
		if (n.body) m[n.body] = visit n.body
		inRuleBody = false

		super.exit n
	}

	IVisitable visit(Constructor n) { visit(n as Relation) }

	IVisitable visit(Relation n) {
		if (!inRuleHead) return n
		n.exprs.withIndex().each { IExpr e, int i ->
			tmpCurrType = typeInferenceActor.inferredTypes[n.name][i]
			m[e] = visit e
		}
		super.exit n
	}

	// Must override since the default implementation throws an exception
	IVisitable visit(RecordExpr n) {
		n.exprs.each { m[it] = visit it }
		new RecordExpr(n.exprs.collect { m[it] as IExpr })
	}

	IVisitable exit(VariableExpr n) {
		if (!inRuleHead || n != tmpConVar) return n

		def rawRecord = typeToRecord[tmpCurrType]
		// rawRecord is null when the constructed type is optimized (replaced by the string type)
		if (!rawRecord) return tmpConRecord.exprs[0]

		def record = (0..<rawRecord.size()).collect { NIL as IExpr }
		record[rawRecord.findIndexOf { it == tmpConRecordType }] = tmpConRecord
		new RecordExpr(record)
	}

	Type map(Type t) { (t in symbolTable.typesToOptimize) ? TYPE_STRING : (typeToCommonType[t] ?: t) }
}
