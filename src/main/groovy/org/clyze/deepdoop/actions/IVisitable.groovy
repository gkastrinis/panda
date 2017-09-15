package org.clyze.deepdoop.actions

interface IVisitable {
	def <T> T accept(IVisitor<T> v)
}
