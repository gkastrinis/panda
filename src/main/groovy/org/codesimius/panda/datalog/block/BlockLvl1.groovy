package org.codesimius.panda.datalog.block

import groovy.transform.Canonical
import groovy.transform.ToString
import org.codesimius.panda.datalog.IVisitable

@Canonical
@ToString(includePackage = false)
class BlockLvl1 implements IVisitable {

	String name
	String superTemplate
	List<String> parameters = []
	List<String> superParameters = []
	BlockLvl0 datalog = new BlockLvl0()
}
