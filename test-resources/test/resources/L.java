package test.resources;

/** Tests for LabeledStatements and LambdaExpressions. */
class L {
  // TODO(jdd): Include high language-level tests.

  void f() {
    LABEL:
    for (int i = 0; i < 10; i++) {}
  }
}
