package org.codesimius.panda.actions.symbol

class SymbolTable {
	@Delegate
	RelationInfoVisitor relationInfo = new RelationInfoVisitor()
	@Delegate
	VarInfoVisitor varInfo = new VarInfoVisitor()
}
