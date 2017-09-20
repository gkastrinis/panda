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
import org.clyze.deepdoop.datalog.element.LogicalElement
import org.clyze.deepdoop.datalog.element.relation.*
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.datalog.expr.IExpr
import org.clyze.deepdoop.datalog.expr.RecordExpr
import org.clyze.deepdoop.datalog.expr.VariableExpr

import static org.clyze.deepdoop.datalog.Annotation.Kind.CONSTRUCTOR
import static org.clyze.deepdoop.datalog.Annotation.Kind.TYPE
import static org.clyze.deepdoop.datalog.expr.VariableExpr.gen1 as var1
import static org.clyze.deepdoop.datalog.expr.VariableExpr.genN as varN

// (1) For each constructor, a new type is generated that
// represents its structure. The structure is a record of
// all key expressions.
// (2) Every type in the same hierarchy must have the same
// structure in order to enable polymorphism. The structure
// comprises of a selector record, i.e. only one part is
// non-nil at a time. Each part in the record represents a
// constructor type (from (1)). The order is not strictly
// defined but it is consistent throughout the transformations.
// (3) For every type, a new relation is generated in order
// to keep entries of that type.
// (4) Also, additional rules are generate to propagate entries
// from subtypes to supertypes.
@Canonical
class ConstructorTransformer extends DummyTransformer {

	InfoCollectionVisitingActor I
	TypeInferenceVisitingActor T

	Set<Declaration> extraDecls = [] as Set
	Set<Rule> extraRules = [] as Set

	// Recurring constant
	IExpr NIL = new ConstantExpr("nil")
	// Re: 2
	Map<String, List<Type>> typeToRecord = [:]
	Map<String, String> typeToCommonType = [:]

	// Relations that have an explicit declaration
	Set<String> explicitDeclarations = [] as Set

	boolean inRuleHead
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
		I.allTypes.findAll { !I.directSuperType[it] }.each { root ->
			def types = [root] + I.subTypes[root]
			def record = types
					.collect { I.constructorsPerType[it] }
					.flatten()
					.withIndex()
					.collect { String con, int i -> new Type(con, var1(i)) }

			extraDecls << new Declaration(
					new Predicate("ROOT_${root}", null, []),
					record,
					[(TYPE): new Annotation("type")])

			types.each {
				typeToRecord[it] = record
				typeToCommonType[it] = "ROOT_${root}"
			}
		}
	}

	IVisitable exit(Component n, Map<IVisitable, IVisitable> m) {
		// Handle explicit declarations
		T.inferredTypes
				.findAll { rel, typeNames -> !(rel in explicitDeclarations) }
				.each { rel, typeNames ->
			def types = typeNames.withIndex().collect { String t, int i ->
				Primitive.isPrimitive(t) ?
						new Primitive(t, var1(i)) :
						new Type(typeToCommonType[t], var1(i))
			}
			extraDecls << new Declaration(new Predicate(rel, null, varN(types.size())), types)
		}

		def ds = (n.declarations.collect { m[it] as Declaration } + extraDecls) as Set
		def rs = (n.rules.collect { m[it] as Rule } + extraRules) as Set
		new Component(n.name, n.superComp, ds, rs)
	}

	IVisitable exit(Declaration n, Map<IVisitable, IVisitable> m) {
		explicitDeclarations << n.atom.name

		if (CONSTRUCTOR in n.annotations) {
			// Re: (1)
			def newTypes = n.types.collect {
				it instanceof Type ? new Type(typeToCommonType[it.name], it.exprs.first()) : it
			}
			extraDecls << new Declaration(
					new Predicate(n.atom.name, null, []),
					newTypes.dropRight(1),
					n.annotations + [(TYPE): new Annotation("type")])
			n = new Declaration(n.atom, newTypes, n.annotations)
		}
		else if (TYPE in n.annotations) {
			def type = n.atom.name
			// Re: 3
			n = new Declaration(
					new Predicate(type, null, [var1(0)]),
					[new Type(typeToCommonType[type], var1(0))])

			// Re: 4
			def superType = I.directSuperType[type]
			if (superType)
				extraRules << new Rule(
						new LogicalElement(new Predicate(superType, null, [var1(0)])),
						new LogicalElement(new Predicate(type, null, [var1(0)])))
		}
		return n
	}

	IVisitable visit(Rule n) {
		actor.enter(n)

		inRuleHead = true
		def head = n.head
		I.constructorsOrderedPerRule[n].each {
			// Map to the updated (from a previous iteration)
			// version of the constructor, if any
			def con = (m[it] ?: it) as Constructor
			constructedVar = con.valueExpr as VariableExpr
			constructedRecord = new RecordExpr(con.keyExprs)
			constructedRecordType = con.name
			head = head.accept(this)
		}
		m[n.head] = head
		inRuleHead = false
		m[n.body] = n.body?.accept(this)

		actor.exit(n, m)
	}

	IVisitable visit(Constructor n) { visit(n as Functional) }

	IVisitable visit(Functional n) { visit(n, n.keyExprs + [n.valueExpr]) }

	IVisitable visit(Predicate n) { visit(n, n.exprs) }

	IVisitable visit(Relation n, List<IExpr> exprs) {
		actor.enter(n)
		if (!inRuleHead) return n
		exprs.withIndex().each { IExpr e, int i ->
			currentType = T.inferredTypes[n.name][i]
			m[e] = e.accept(this)
		}
		actor.exit(n, m)
	}

	// Must override since the default implementation throws an exception
	IVisitable visit(RecordExpr n) {
		actor.enter(n)
		n.exprs.each { m[it] = it.accept(this) }
		actor.exit(n, m)
	}

	// Must override since the default implementation throws an exception
	void enter(RecordExpr n) {}

	IVisitable exit(RecordExpr n, Map<IVisitable, IVisitable> m) {
		new RecordExpr(n.exprs.collect { m[it] as IExpr })
	}

	IVisitable exit(VariableExpr n, Map<IVisitable, IVisitable> m) {
		if (!inRuleHead || n != constructedVar) return n

		def rawRecord = typeToRecord[currentType]
		def record = (0..<rawRecord.size()).collect { NIL as IExpr }
		record[rawRecord.findIndexOf { it.name == constructedRecordType }] = constructedRecord
		new RecordExpr(record)
	}
}
