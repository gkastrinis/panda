package org.clyze.deepdoop.datalog.block

import groovy.transform.Canonical
import groovy.transform.ToString
import org.clyze.deepdoop.actions.DefaultVisitor
import org.clyze.deepdoop.datalog.IVisitable

@Canonical
@ToString(includePackage = false)
class BlockLvl2 implements IVisitable {

	BlockLvl0 datalog = new BlockLvl0()
	Set<BlockLvl1> components = [] as Set
	Set<Instantiation> instantiations = [] as Set

	BlockLvl2 accept(DefaultVisitor<IVisitable> v) { v.visit this }
}
