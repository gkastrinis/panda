package org.clyze.deepdoop.actions.tranform

import org.clyze.deepdoop.actions.IActor
import org.clyze.deepdoop.actions.IVisitable
import org.clyze.deepdoop.actions.PostOrderVisitor
import org.clyze.deepdoop.actions.TDummyActor

class DummyTransformer extends PostOrderVisitor<IVisitable> implements IActor<IVisitable>, TDummyActor<IVisitable> {
}
