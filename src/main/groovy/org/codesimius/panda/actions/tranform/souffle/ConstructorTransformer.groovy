package org.codesimius.panda.actions.tranform.souffle

import groovy.transform.Canonical
import org.codesimius.panda.actions.symbol.ConstructionInfoVisitor
import org.codesimius.panda.actions.symbol.SymbolTable
import org.codesimius.panda.actions.tranform.DefaultTransformer
import org.codesimius.panda.actions.tranform.TypeInferenceTransformer
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.block.BlockLvl0
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.ConstructionElement
import org.codesimius.panda.datalog.element.relation.Constructor
import org.codesimius.panda.datalog.element.relation.RecordType
import org.codesimius.panda.datalog.element.relation.Relation
import org.codesimius.panda.datalog.element.relation.Type
import org.codesimius.panda.datalog.expr.IExpr
import org.codesimius.panda.datalog.expr.RecordExpr
import org.codesimius.panda.datalog.expr.VariableExpr

import static org.codesimius.panda.datalog.expr.ConstantExpr.NIL
import static org.codesimius.panda.datalog.expr.VariableExpr.gen1 as var1

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

	SymbolTable symbolTable
	TypeInferenceTransformer typeInference

	// Re: 1
	private Map<String, Type> constructorType = [:]
	// Re: 2
	private Map<Type, RecordType> typeToRecordType = [:]
	private Map<Type, Type> typeToCommonType = [:]

	// The variable currently being constructed
	private VariableExpr tmpConVar
	// The constructor currently used
	private String tmpCurrConstructor
	// Type for current relation parameter (in rule head)
	private Type tmpCurrType
	// The internal record representing the constructed value
	private RecordExpr tmpConRecord

	void enter(BlockLvl0 n) {
		super.enter n
		// Re: (2)
		symbolTable.rootTypes.each { root ->
			def rootInternalType = new Type("_${root.name}")
			def types = [root] + symbolTable.subTypes[root]
			types.each { typeToCommonType[it] = rootInternalType }

			def recordType = new RecordType([])
			types.each {
				typeToRecordType[it] = recordType
				// Re: (1)
				symbolTable.constructorsPerType[it].each { conDecl ->
					def conT = new Type(conDecl.relation.name)
					constructorType[conDecl.relation.name] = conT
					extraTypeDecls << new TypeDeclaration(conT, new RecordType(conDecl.types.dropRight(1)))
					recordType.innerTypes << conT
				}
			}
			extraTypeDecls << new TypeDeclaration(rootInternalType, recordType)
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
		return null
	}

	IVisitable visit(Rule n) {
		inRuleHead = true
		def head = n.head
		new ConstructionInfoVisitor().with {
			visit n
			constructionsOrderedPerRule[n].each {
				// Map to the updated (from a previous iteration) version of the constructor, if any
				def con = (m[it] ?: it) as ConstructionElement
				tmpConVar = con.constructor.valueExpr as VariableExpr
				tmpCurrConstructor = con.constructor.name
				tmpConRecord = new RecordExpr(con.constructor.keyExprs)
				head = visit head
			}
		}
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
		n.exprs.eachWithIndex { IExpr e, int i ->
			tmpCurrType = typeInference.inferredTypes[n.name][i]
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

		def conT = constructorType[tmpCurrConstructor]
		def recordType = typeToRecordType[tmpCurrType]
		def record = new RecordExpr((0..<recordType.innerTypes.size()).collect { NIL as IExpr })
		record.exprs[recordType.findIndexOf { it == conT }] = tmpConRecord
		return record
	}

	Type map(Type t) { typeToCommonType[t] ?: t }
}
