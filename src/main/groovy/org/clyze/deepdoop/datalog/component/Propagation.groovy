package org.clyze.deepdoop.datalog.component

import groovy.transform.Canonical

@Canonical
class Propagation {
	String fromId
	Set<String> preds
	String toId
}
