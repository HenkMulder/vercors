package silAST.methods.implementations

import silAST.ASTNode
import silAST.expressions.PExpression
import silAST.types.DataType
import silAST.expressions.Expression
import silAST.expressions.PredicateExpression
import silAST.source.SourceLocation
import silAST.expressions.util.PTermSequence
import silAST.programs.symbols.{ProgramVariableSequence, Field, ProgramVariable}
import silAST.methods.Method
import silAST.expressions.terms.PTerm

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
sealed abstract class Statement private[silAST](
                                                 sl: SourceLocation
                                                 ) extends ASTNode(sl) {
  override def toString: String
}

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
final case class AssignmentStatement private[silAST](
                                             sl: SourceLocation,
                                             target: ProgramVariable,
                                             source: PTerm
                                             )
  extends Statement(sl) {
  override def toString: String = target.name + ":=" + source.toString

}

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
case class FieldAssignmentStatement private[silAST](
                                            sl: SourceLocation,
                                            target: ProgramVariable,
                                            field: Field,
                                            source: PTerm
                                            )
  extends Statement(sl) {
  override def toString: String = target.name + "." + field.name + " := " + source.toString
}

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
case class NewStatement private[silAST](
                                         sl: SourceLocation,
                                         target: ProgramVariable,
                                         dataType: DataType
                                         )
  extends Statement(sl) {
  override def toString: String = target.name + ":= new " + dataType.toString
}

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//TODO:check signature
final case class CallStatement private[silAST]
(
  sl: SourceLocation,
  targets: ProgramVariableSequence,
  receiver: PTerm,
  method: Method,
  arguments: PTermSequence
  )
  extends Statement(sl) {
  override def toString: String = targets.toString + " := " + receiver.toString + "." + method.name + arguments.toString
}

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
final case class InhaleStatement private[silAST](
                                         sl: SourceLocation,
                                         expression: Expression
                                         )
  extends Statement(sl) {
  override def toString: String = "inhale " + expression.toString
}

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
final case class ExhaleStatement private[silAST](
                                         sl: SourceLocation,
                                         expression: Expression
                                         )
  extends Statement(sl) {
  override def toString: String = "exhale " + expression.toString
}

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//TODO:FoldStatement/UnfoldStatement arrays?
final case class FoldStatement private[silAST](
                                       sl: SourceLocation,
                                       predicate: PredicateExpression
                                       )
  extends Statement(sl) {
  override def toString: String = "fold " + predicate.toString
}

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
final case class UnfoldStatement private[silAST](
                                         sl: SourceLocation,
                                         predicate: PredicateExpression
                                         )
  extends Statement(sl) {
  override def toString: String = "unfold " + predicate.toString
}
