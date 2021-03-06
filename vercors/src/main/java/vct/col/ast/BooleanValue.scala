package vct.col.ast

case class BooleanValue(val value:Boolean) extends Value {
  override def toString() = if (value) "true" else "false"
  override def equals(o:Any) = o.equals(value)
}