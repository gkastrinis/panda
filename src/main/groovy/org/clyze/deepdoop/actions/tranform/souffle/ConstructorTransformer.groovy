package org.clyze.deepdoop.actions.tranform.souffle

import groovy.transform.Canonical
import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.actions.InfoCollectionVisitingActor
import org.clyze.deepdoop.actions.TypeInferenceVisitingActor
import org.clyze.deepdoop.actions.tranform.DummyTransformer
import org.clyze.deepdoop.datalog.Annotation
import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.element.ConstructionElement
import org.clyze.deepdoop.datalog.element.LogicalElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.element.relation.Type
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.datalog.expr.IExpr
import org.clyze.deepdoop.datalog.expr.RecordExpr
import org.clyze.deepdoop.datalog.expr.VariableExpr

import static org.clyze.deepdoop.datalog.Annotation.Kind.CONSTRUCTOR
import static org.clyze.deepdoop.datalog.Annotation.Kind.TYPE
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

	InfoCollectionVisitingActor infoActor
	TypeInferenceVisitingActor typeActor

	Set<Declaration> extraDecls = [] as Set
	Set<Rule> extraRules = [] as Set

	// Recurring constant
	IExpr NIL = new ConstantExpr("nil")
	// Re: 2
	Map<String, List<Type>> typeToRecord = [:]
	Map<String, String> typeToCommonType = [:]

	// Relations that have an explicit declaration
	Set<String> explicitDeclarations = [] as Set

	// The variable currently being constructed
	VariableExpr constructedVar
	// The internal record representing the constructed value
	RecordExpr constructedRecord
	// The internal record type, e.g. _R01
	String constructedRecordType
	// Type for current relation parameter (in rule head)
	String currentType

	void enter(Program n) {
		// Re: (2)
		// Find all types that are roots in the type hierarchy
		infoActor.allTypes.findAll { !infoActor.directSuperType[it] }.each { root ->
			def rootTypeName = "R_${root}"
			def types = [root] + infoActor.subTypes[root]
			def record = types
					.collect { infoActor.constructorsPerType[it] }
					.flatten()
					.withIndex()
					.collect { String con, int i -> new Type(con, var1(i)) }

			extraDecls << new Declaration(new Type(rootTypeName), record, [(TYPE): new Annotation("type")])

			types.each {
				typeToRecord[it] = record
				typeToCommonType[it] = rootTypeName
			}
		}
	}

	IVisitable exit(Component n, Map m) {
		def ds = (n.declarations.collect { m[it] as Declaration } + extraDecls) as Set
		def rs = (n.rules.collect { m[it] as Rule } + extraRules) as Set
		new Component(n.name, n.superComp, [], [], ds, rs)
	}

	IVisitable exit(Declaration n, Map m) {
		explicitDeclarations << n.relation.name

		if (n.annotations[CONSTRUCTOR]) {
			// Re: (1)
			def newTypes = n.types.collect {
				new Type(typeToCommonType[it.name] ?: it.name)
			}
			extraDecls << new Declaration(
					new Type(n.relation.name),
					newTypes.dropRight(1),
					n.annotations + [(TYPE): new Annotation("type")])
			n = new Declaration(n.relation, newTypes, n.annotations)
		} else if (n.annotations[TYPE]) {
			def type = n.relation.name
			// Re: 3
			n = new Declaration(
					new Type(type),
					[new Type(typeToCommonType[type])])

			// Re: 4
			def superType = infoActor.directSuperType[type]
			if (superType)
				extraRules << new Rule(
						new LogicalElement(new Relation(superType, [var1()])),
						new LogicalElement(new Relation(type, [var1()])))
		} else {
			def newTypes = n.types.collect { new Type(typeToCommonType[it.name] ?: it.name) }
			n = new Declaration(n.relation, newTypes, n.annotations)
		}
		return n
	}

	IVisitable visit(Rule n) {
		actor.enter(n)

		inRuleHead = true
		def head = n.head as LogicalElement
		infoActor.constructionsOrderedPerRule[n].each {
			// Map to the updated (from a previous iteration)
			// version of the constructor, if any
			def con = (m[it] ?: it) as ConstructionElement
			constructedVar = con.constructor.valueExpr as VariableExpr
			constructedRecord = new RecordExpr(con.constructor.keyExprs)
			constructedRecordType = con.constructor.name
			head = visit head
		}
		// Remove constructors from global map `m`
		// since they might reappear in a different rule
		infoActor.constructionsOrderedPerRule[n].each { m.remove(it) }
		m[n.head] = head
		inRuleHead = false
		if (n.body) m[n.body] = visit n.body

		actor.exit(n, m)
	}

	IVisitable visit(Constructor n) { visit(n as Relation) }

	IVisitable visit(Relation n) {
		actor.enter(n)
		if (!inRuleHead) return n
		n.exprs.withIndex().each { IExpr e, int i ->
			currentType = typeActor.inferredTypes[n.name][i]
			m[e] = visit e
		}
		actor.exit(n, m)
	}

	IVisitable visit(Type n) { visit(n as Relation) }

	// Must override since the default implementation throws an exception
	IVisitable visit(RecordExpr n) {
		actor.enter(n)
		n.exprs.each { m[it] = visit it }
		actor.exit(n, m)
	}

	// Must override since the default implementation throws an exception
	void enter(RecordExpr n) {}

	// Must override since the default implementation throws an exception
	IVisitable exit(RecordExpr n, Map m) {
		new RecordExpr(n.exprs.collect { m[it] as IExpr })
	}

	IVisitable exit(VariableExpr n, Map m) {
		if (!inRuleHead || n != constructedVar) return n

		def rawRecord = typeToRecord[currentType]
		def record = (0..<rawRecord.size()).collect { NIL as IExpr }
		record[rawRecord.findIndexOf { it.name == constructedRecordType }] = constructedRecord
		new RecordExpr(record)
	}
}
