package org.clyze.deepdoop.actions.tranform.souffle

import groovy.transform.Canonical
import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.actions.InfoCollectionVisitingActor
import org.clyze.deepdoop.actions.TypeInferenceVisitingActor
import org.clyze.deepdoop.actions.tranform.DummyTransformer
import org.clyze.deepdoop.datalog.Program
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.clause.Rule
import org.clyze.deepdoop.datalog.component.Component
import org.clyze.deepdoop.datalog.element.LogicalElement
import org.clyze.deepdoop.datalog.element.relation.Constructor
import org.clyze.deepdoop.datalog.element.relation.Functional
import org.clyze.deepdoop.datalog.element.relation.Predicate
import org.clyze.deepdoop.datalog.element.relation.Relation
import org.clyze.deepdoop.datalog.expr.ConstantExpr
import org.clyze.deepdoop.datalog.expr.IExpr
import org.clyze.deepdoop.datalog.expr.RecordExpr
import org.clyze.deepdoop.datalog.expr.VariableExpr

import static org.clyze.deepdoop.datalog.Annotation.Kind.CONSTRUCTOR
import static org.clyze.deepdoop.datalog.Annotation.Kind.TYPE
import static org.clyze.deepdoop.datalog.expr.VariableExpr.gen1 as var1
import static org.clyze.deepdoop.datalog.expr.VariableExpr.genN as varN

@Canonical
class ConstructorTransformer extends DummyTransformer {

	InfoCollectionVisitingActor infoActor
	TypeInferenceVisitingActor typeInferenceActor

	// Recurring constant
	IExpr NIL = new ConstantExpr("nil")
	// Map each constructor to an internal record type
	// e.g. constructorA --> _R0
	Map<String, String> recordTypes = [:]
	// Extra declarations for record types
	Set<Declaration> recordDeclarations = [] as Set
	// Extra rules for record types
	// Entries for a subtype are also added in the supertype, if any
	Set<Rule> recordRules = [] as Set
	// Map external types to internal record ones
	// e.g. T1 --> [_R0, _R1, _R2]
	// Each type in the hierarchy needs to have the same structure
	// in order to achieve polymorphism
	Map<String, List<String>> typeToRecords = [:]
	// Each type in the hierarchy is mapped to a common raw type
	// representing the common structure
	Map<String, String> typeToRawType = [:]

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
		recordTypes = infoActor.allConstructors.withIndex()
				.collectEntries { con, i -> [(con): "_R$i" as String] }

		def mapToRecords = { String t -> infoActor.constructorsPerType[t].collect { recordTypes[it] } }
		// Find all types that are roots in the type hierarchy trees
		infoActor.allTypes
				.findAll { !infoActor.directSuperType[it] }
				.each { root ->

			def record = mapToRecords(root) + infoActor.subTypes[root].collect { mapToRecords(it) }.flatten()

			([root] + infoActor.subTypes[root]).each {
				typeToRecords[it] = record
				typeToRawType[it] = "_${root}_RAW" as String
			}

			def rawType = new Predicate(typeToRawType[root], null, varN(record.size()))
			def typePreds = record.withIndex()
					.collect { String t, int i -> new Predicate(t, null, [var1(i)]) }
			recordDeclarations << new Declaration(rawType, typePreds)
		}
	}

	IVisitable exit(Component n, Map<IVisitable, IVisitable> m) {
		def ds = (n.declarations.collect { m[it] as Declaration } + recordDeclarations) as Set
		def rs = (n.rules.collect { m[it] as Rule } + recordRules) as Set
		new Component(n.name, n.superComp, ds, rs)
	}

	IVisitable exit(Declaration n, Map<IVisitable, IVisitable> m) {
		if (CONSTRUCTOR in n.annotations) {
			def name = n.atom.name
			def record = typeInferenceActor.inferredTypes[name].withIndex()
					.dropRight(1)
					.collect { String t, int i -> new Predicate(t, null, [var1(i)]) }
			recordDeclarations << new Declaration(new Predicate(recordTypes[name], null, varN(record.size())), record, n.annotations)
		}

		if (TYPE in n.annotations) {
			def type = n.atom.name
			def rawType = new Predicate(typeToRawType[type], null, [var1(0)])
			n = new Declaration(new Predicate(type, null, [var1(0)]), [rawType], n.annotations)
			recordDeclarations << n

			// Generate extra rule for supertype
			def superType = infoActor.directSuperType[type]
			if (superType) {
				def vars = [var1(0)]
				recordRules << new Rule(
						new LogicalElement(new Predicate(superType, null, vars)),
						new LogicalElement(new Predicate(type, null, vars)))
			}
		}
		return n
	}

	IVisitable visit(Rule n) {
		actor.enter(n)

		inRuleHead = true
		def head = n.head
		infoActor.constructorsOrderedPerRule[n].each {
			// Map to the updated (from a previous iteration)
			// version of the constructor, if any
			def con = (m[it] ?: it) as Constructor
			constructedVar = con.valueExpr as VariableExpr
			constructedRecord = new RecordExpr(con.keyExprs)
			constructedRecordType = recordTypes[con.name]
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
			currentType = typeInferenceActor.inferredTypes[n.name][i]
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

		def rawRecord = typeToRecords[currentType]
		def record = (0..<rawRecord.size()).collect { NIL as IExpr }
		record[rawRecord.indexOf(constructedRecordType)] = constructedRecord
		new RecordExpr(record)
	}
}
