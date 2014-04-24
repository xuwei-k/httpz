package httpz

import scala.reflect.macros.Context

object Macros{

  def assertEqual(act: Any, exp: Any): Unit = macro assertEqualImpl

  def assertNotEqual(act: Any, exp: Any): Unit = macro assertNotEqualImpl

  def assertNotEqualImpl(c: Context)(act: c.Expr[Any],exp: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._
    def treeString[A](expr: c.Expr[A]) = {
      c.Expr[String](Literal(Constant(show(expr.tree))))
    }
    reify({
      if(act.splice == exp.splice) {
        sys.error(s"AssertionError: ${treeString(act).splice} [${act.splice}] is equal to ${treeString(exp).splice} [${exp.splice}]")
      }
    })
  }

  def assertEqualImpl(c: Context)(act: c.Expr[Any],exp: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._
    def treeString[A](expr: c.Expr[A]) = {
      c.Expr[String](Literal(Constant(show(expr.tree))))
    }
    reify({
      if(act.splice!=exp.splice) {
        sys.error(s"AssertionError: ${treeString(act).splice} [${act.splice}] is not equal to ${treeString(exp).splice} [${exp.splice}]")
      }
    })
  }

}
