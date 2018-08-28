package org.codesimius.panda.datalog.block

import groovy.transform.Canonical
import groovy.transform.ToString
import org.codesimius.panda.actions.symbol.RelationInfoVisitor
import org.codesimius.panda.actions.symbol.VarInfoVisitor
import org.codesimius.panda.datalog.IVisitable
import org.codesimius.panda.datalog.clause.RelDeclaration
import org.codesimius.panda.datalog.clause.Rule
import org.codesimius.panda.datalog.clause.TypeDeclaration
import org.codesimius.panda.datalog.element.relation.Type
import org.codesimius.panda.datalog.expr.VariableExpr

import static org.codesimius.panda.datalog.Annotation.CONSTRUCTOR

@Canonical
@ToString(includePackage = false)
class BlockLvl0 implements IVisitable {

	Set<RelDeclaration> relDeclarations = [] as Set
	Set<TypeDeclaration> typeDeclarations = [] as Set
	Set<Rule> rules = [] as Set

	// -- Symbol Table Information -- //

	private boolean typeInfoCollected
	private Map<Type, List<Type>> superTypesOrdered0 = [:]
	private Map<Type, Set<Type>> subTypes0 = [:].withDefault { [] as Set }
	private Map<Type, Type> typeToRootType0 = [:]
	private Map<String, Type> constructorToBaseType0 = [:]
	private Map<Type, Set<RelDeclaration>> constructorsPerType0 = [:].withDefault { [] as Set }

	private boolean relationInfoCollected
	private Set<String> declaredRelations0
	private Map<String, Set<Rule>> relationDefinedInRules0
	private Map<String, Set<Rule>> relationUsedInRule0

	private VarInfoVisitor varInfoVisitor = new VarInfoVisitor()
	private Map<Rule, Set<VariableExpr>> constructedVars0 = [:]
	private Map<Rule, List<VariableExpr>> headVars0 = [:]
	private Map<Rule, List<VariableExpr>> bodyVars0 = [:]
	private Map<Rule, List<VariableExpr>> boundBodyVars0 = [:]

	Map<Type, List<Type>> getSuperTypesOrdered() {
		if (!typeInfoCollected) collectTypeInfo()
		superTypesOrdered0
	}

	Map<Type, Set<Type>> getSubTypes() {
		if (!typeInfoCollected) collectTypeInfo()
		subTypes0
	}

	Map<Type, Type> getTypeToRootType() {
		if (!typeInfoCollected) collectTypeInfo()
		typeToRootType0
	}

	Set<Type> getAllTypes() {
		if (!typeInfoCollected) collectTypeInfo()
		typeToRootType0.keySet()
	}

	Set<Type> getRootTypes() {
		if (!typeInfoCollected) collectTypeInfo()
		typeToRootType0.values() as Set
	}

	Map<String, Type> getConstructorToBaseType() {
		if (!typeInfoCollected) collectTypeInfo()
		constructorToBaseType0
	}

	Map<Type, Set<RelDeclaration>> getConstructorsPerType() {
		if (!typeInfoCollected) collectTypeInfo()
		constructorsPerType0
	}

	Set<String> getAllConstructors() {
		if (!typeInfoCollected) collectTypeInfo()
		constructorToBaseType0.keySet()
	}

	void collectTypeInfo() {
		typeDeclarations.each { d ->
			superTypesOrdered0[d.type] = []
			def currDecl = d
			while (currDecl.supertype) {
				superTypesOrdered0[d.type] << currDecl.supertype
				currDecl = typeDeclarations.find { it.type == currDecl.supertype }
			}

			superTypesOrdered0.each { t, ts ->
				ts.each { subTypes0[it] << t }

				typeToRootType0[t] = ts ? ts.last() : t
			}
		}
		relDeclarations.each {
			if (CONSTRUCTOR in it.annotations) {
				constructorToBaseType0[it.relation.name] = it.types.last()
				constructorsPerType0[it.types.last()] << it
			}
		}
		typeInfoCollected = true
	}

	// ---- //

	Set<String> getDeclaredRelations() {
		if (!relationInfoCollected) collectRelationInfo()
		declaredRelations0
	}

	Map<String, Set<Rule>> getRelationDefinedInRules() {
		if (!relationInfoCollected) collectRelationInfo()
		relationDefinedInRules0
	}

	Map<String, Set<Rule>> getRelationUsedInRules() {
		if (!relationInfoCollected) collectRelationInfo()
		relationUsedInRule0
	}

	void collectRelationInfo() {
		// Implicitly, add relations supported in aggregation
		declaredRelations0 = ["count", "min", "max", "sum"] as Set
		// Each type introduces an implicit unary relation with the same name
		declaredRelations0 += typeDeclarations.collect { declaredRelations0 << it.type.name }
		// Relations with explicit declarations
		declaredRelations0 += relDeclarations.collect { declaredRelations0 << it.relation.name }

		def relInfoVisitor = new RelationInfoVisitor()
		relInfoVisitor.visit this
		declaredRelations0 += relInfoVisitor.implicitlyDeclared
		relationDefinedInRules0 = relInfoVisitor.relationDefinedInRules
		relationUsedInRule0 = relInfoVisitor.relationUsedInRules

		relationInfoCollected = true
	}

	// ---- //

	Set<VariableExpr> getConstructedVars(Rule n) {
		if (!constructedVars0[n]) collectVarInfo n
		constructedVars0[n]
	}

	List<VariableExpr> getHeadVars(Rule n) {
		if (!headVars0[n]) collectVarInfo n
		headVars0[n]
	}

	List<VariableExpr> getBodyVars(Rule n) {
		if (!bodyVars0[n]) collectVarInfo n
		bodyVars0[n]
	}

	List<VariableExpr> getAllVars(Rule n) {
		if (!headVars0[n] || !bodyVars0[n]) collectVarInfo n
		(headVars0[n] + bodyVars0[n])
	}

	List<VariableExpr> getBoundBodyVars(Rule n) {
		if (!boundBodyVars0[n]) collectVarInfo n
		boundBodyVars0[n]
	}

	void collectVarInfo(Rule n) {
		varInfoVisitor.visit n
		constructedVars0[n] = varInfoVisitor.constructedVars
		headVars0[n] = varInfoVisitor.headVars
		bodyVars0[n] = varInfoVisitor.bodyVars
		boundBodyVars0[n] = varInfoVisitor.boundBodyVars
	}
}
