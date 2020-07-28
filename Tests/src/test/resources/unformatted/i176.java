package src.test.resources;
interface
InterfaceWithPrivateStaticMethod
{
  private static void bar() { }
  default void foo() { bar(); }
}
