package org.codesimius.panda.actions.symbol

class SymbolTable {

	@Delegate
	TypeInfoVisitor typeInfo = new TypeInfoVisitor()
	@Delegate
	RelationInfoVisitor relationInfo = new RelationInfoVisitor()
	@Delegate
	VarInfoVisitor varInfo = new VarInfoVisitor()
}
