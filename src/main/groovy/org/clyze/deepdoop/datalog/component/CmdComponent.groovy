package org.clyze.deepdoop.datalog.component

import groovy.transform.Canonical
import org.clyze.deepdoop.datalog.clause.Declaration
import org.clyze.deepdoop.datalog.element.relation.Relation

@Canonical
class CmdComponent extends Component {

	String eval
	Set<Relation> exports
	Set<Relation> imports

	CmdComponent(String name, Set<Declaration> declarations, String eval, Set<Relation> imports, Set<Relation> exports) {
		super(name, null, declarations, [])
		this.eval = eval
		this.imports = imports
		this.exports = exports
	}

	CmdComponent(String name) {
		this(name, [] as Set, null, [] as Set, [] as Set)
	}

	//void addFOO(Rule r) {
	//if (!r.isDirective) {
	//super.addFOO(r)
	//return
	//}
/*
		def d = r.getDirective()
		switch (d.name) {
			case "lang:cmd:EVAL":
				if (eval != null) ErrorManager.error(ErrorId.CMD_EVAL, name)
				eval = (d.constant.value as String).replaceAll('^\"|\"$', ""); break
			case "lang:cmd:export":
				exports << new Relation(d.backtick.name, "@past"); break
			case "lang:cmd:import":
				imports << new Relation(d.backtick.name); break
			default:
				ErrorManager.error(ErrorId.CMD_DIRECTIVE, name)
		}
		*/
	//}

	void add(Component other) {
		throw new UnsupportedOperationException("`add` is not supported on a command block")
	}
}
