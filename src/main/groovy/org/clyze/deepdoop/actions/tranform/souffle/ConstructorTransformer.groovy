package org.clyze.deepdoop.actions.tranform.souffle

import groovy.transform.Canonical
import org.clyze.deepdoop.actions.ConstructionInfoVisitingActor
import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.actions.TypeInferenceVisitingActor
import org.clyze.deepdoop.actions.TypeInfoVisitingActor
import org.clyze.deepdoop.actions.tranform.DummyTransformer
import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.RelDeclaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.clause.TypeDeclaration
import org.clyze.deepdoop.datalog.element.ConstructionElement
import org.clyze.deepdoop.datalog.element.LogicalElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.RecordType
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.datalog.expr.IExpr
import org.clyze.deepdoop.datalog.expr.RecordExpr
import org.clyze.deepdoop.datalog.expr.VariableExpr

import static org.clyze.deepdoop.datalog.Annotation.TYPE
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
class ConstructorTransformer extends DummyTransformer {

	TypeInfoVisitingActor typeInfoActor
	TypeInferenceVisitingActor typeInferenceActor
	ConstructionInfoVisitingActor constructionInfoActor

	// Recurring constant
	IExpr NIL = new ConstantExpr("nil")

	// Re: 2
	Map<Type, List<Type>> typeToRecord = [:]
	Map<Type, Type> typeToCommonType = [:]

	// Optimize for the case of a single constructor
	Set<Type> optimizedTypes = [] as Set
	Set<RelDeclaration> optimizedConstructors = [] as Set

	// The variable currently being constructed
	VariableExpr tmpConVar
	// The internal record representing the constructed value
	RecordExpr tmpConRecord
	// The internal record type, e.g. ConType01
	Type tmpConRecordType
	// Type for current relation parameter (in rule head)
	Type tmpCurrType

	void enter(Program n) {
		// Re: (2)
		// Find all types that are roots in the type hierarchy
		(typeInfoActor.typeToRootType[n.globalComp].values() as Set).each { root ->
			def rootType = new Type("R_${root.name}")
			def types = [root] + typeInfoActor.subTypes[n.globalComp][root]
			types.each { typeToCommonType[it] = rootType }
			def constructors = types.collect { constructionInfoActor.constructorsPerType[it] }.flatten() as Set<RelDeclaration>

			// Optimize in the case of a single constructor with a single string key
			if (constructors.size() == 1 && constructors[0].types.size() == 2 && constructors[0].types[0] == TYPE_STRING) {
				types.each { optimizedTypes << it }
				optimizedConstructors << constructors[0]
			} else {
				constructors.each {
					extraDecls << new TypeDeclaration(new Type(it.relation.name), new RecordType(it.types.dropRight(1)), [TYPE] as Set)
				}
				def record = constructors.collect { new Type(it.relation.name) }
				types.each { typeToRecord[it] = record }
				extraDecls << new TypeDeclaration(rootType, new RecordType(record), [TYPE] as Set)
			}
		}
	}

	IVisitable exit(RelDeclaration n, Map m) {
		// Re: (1)
		new RelDeclaration(n.relation, n.types.collect { map(it) }, n.annotations)
	}

	IVisitable exit(TypeDeclaration n, Map m) {
		// Re: 3
		extraDecls << new RelDeclaration(new Relation(n.type.name), [map(n.type)], n.annotations)

		// Re: 4
		if (n.supertype)
			extraRules << new Rule(
					new LogicalElement(new Relation(n.supertype.name, [var1()])),
					new LogicalElement(new Relation(n.type.name, [var1()])))
		null
	}

	IVisitable visit(Rule n) {
		actor.enter(n)

		inRuleHead = true
		def head = n.head
		constructionInfoActor.constructionsOrderedPerRule[n].each {
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
		constructionInfoActor.constructionsOrderedPerRule[n].each { m.remove(it) }

		m[n.head] = head
		inRuleHead = false

		actor.exit(n, m)
	}

	IVisitable visit(Constructor n) { visit(n as Relation) }

	IVisitable visit(Relation n) {
		actor.enter(n)
		if (!inRuleHead) return n
		n.exprs.withIndex().each { IExpr e, int i ->
			tmpCurrType = typeInferenceActor.inferredTypes[n.name][i]
			m[e] = visit e
		}
		actor.exit(n, m)
	}

	// Must override since the default implementation throws an exception
	IVisitable visit(RecordExpr n) {
		actor.enter(n)
		n.exprs.each { m[it] = visit it }
		actor.exit(n, m)
	}

	// Must override since the default implementation throws an exception
	void enter(RecordExpr n) {}

	// Must override since the default implementation throws an exception
	IVisitable exit(RecordExpr n, Map m) { new RecordExpr(n.exprs.collect { m[it] as IExpr }) }

	IVisitable exit(VariableExpr n, Map m) {
		if (!inRuleHead || n != tmpConVar) return n

		def rawRecord = typeToRecord[tmpCurrType]
		// rawRecord is null when the constructed type is optimized (replaced by the string type)
		if (!rawRecord) return tmpConRecord.exprs[0]

		def record = (0..<rawRecord.size()).collect { NIL as IExpr }
		record[rawRecord.findIndexOf { it == tmpConRecordType }] = tmpConRecord
		new RecordExpr(record)
	}

	Type map(Type t) { (t in optimizedTypes) ? TYPE_STRING : (typeToCommonType[t] ?: t) }
}
