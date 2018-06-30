package org.globalnames.parser
package formatters

trait CommonOps {
  protected val preprocessorResult: Preprocessor.Result

  private[parser] def stringOf(astNode: AstNode): String =
    preprocessorResult.unescaped.substring(astNode.pos.start, astNode.pos.end)

  private[parser] def namesEqual(name1: Name, name2: Name): Boolean = {
    val name1str = stringOf(name1)
    val name2str = stringOf(name2)
    !name1.uninomial.implied && !name2.uninomial.implied &&
      (name1str.startsWith(name2str) ||
        (name2str.endsWith(".") && name1str.startsWith(name2str.substring(0, name2str.length - 1))))
  }
}
