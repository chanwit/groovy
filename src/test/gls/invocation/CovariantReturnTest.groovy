package gls.invocation

import gls.scope.CompilableTestSupport

public class CovariantReturnTest extends CompilableTestSupport {

  void testCovariantReturn() {
    assertScript """
      class A {
        Object foo() {1}
      }
   
      class B extends A{
        String foo(){"2"}
      }
      def b = new B();
      assert b.foo()=="2"
      assert B.declaredMethods.findAll{it.name=="foo"}.size()==2
    """   
  }
  
  void testCovariantReturnOverwritingAbstractMethod() {
    assertScript """
       abstract class Numeric {
         abstract Numeric eval();
       }

       class Rational extends Numeric {
         Rational eval() {this}
       }
     
       assert Rational.declaredMethods.findAll{it.name=="eval"}.size()==2    
    """
  }
  
  void testCovariantReturnOverwritingObjectMethod() {
    shouldNotCompile """
      class X {
        Long toString() { 333L } 
        String hashCode() { "hash" }
      }
    """
  }
  
  void testCovariantOverwritingMethodWithPrimitives() {
    assertScript """
      class Base {
         Object foo(boolean i) {i}
      }
      class Child extends Base {
         String foo(boolean i) {""+super.foo(i)}
      }
      def x = new Child()
      assert x.foo(true) == "true"
      assert x.foo(false) == "false"
    """
  }

  void testCovariantOverwritingMethodWithInterface() {
    assertScript """
      interface Base {
        List foo()
        Base baz()
      }
      interface Child extends Base {
        ArrayList foo()
        Child baz()
      }
      class GroovyChildImpl implements Child {
        ArrayList foo() {}
        GroovyChildImpl baz() {}
      }

      def x = new GroovyChildImpl()
      x.foo()
      x.baz()
    """
  }

  void testCovariantOverwritingMethodWithInterfaceAndInheritance() {
    assertScript """
      interface Base {
        List foo()
        List bar()
        Base baz()
      }
      interface Child extends Base {
        ArrayList foo()
      }
      class MyArrayList extends ArrayList { }
      class GroovyChildImpl implements Child {
        MyArrayList foo() {}
        MyArrayList bar() {}
        GroovyChildImpl baz() {}
      }
      def x = new GroovyChildImpl()
      x.foo()
      x.bar()
      x.baz()
    """
  }

  void testCovariantMethodFromParentOverwritingMethodFromInterfaceInCurrentclass() {
    assertScript """
      interface I {
        def foo()
      } 
      class A {
        String foo(){""}
      }
      class B extends A implements I{}
      def b = new B()
      assert b.foo() == ""
    """
  
    // basically the same as above, but with an example
    // from an error report (GROOVY-2582)
    // Properties has a method "String getProperty(String)", this class
    // is also a GroovyObject, meaning a "Object getProperty(String)" method
    // should be implemented. But this method should not be the usual automatically
    // added getProperty, but a bridge to the getProperty method provided by Properties 
    assertScript """
      class Configuration extends java.util.Properties {}
      assert Configuration.declaredMethods.findAll{it.name=="getProperty"}.size() == 1
      def conf = new Configuration()
      conf.setProperty("a","b")           
      // the following assert would fail if standard getProperty method was added
      // by the compiler 
      assert conf.getProperty("a") == "b" 
    """
  }

  void testImplementedInterfacesNotInfluencing(){
    // in GROOVY-3229 some methods from Appendable were not correctly recognized
    // as already being overriden (PrintWriter<Writer<Appenable)
    shouldCompile """
        class IndentWriter extends java.io.PrintWriter {
           public IndentWriter(Writer w)  { super(w, true) }
        }
    """
  }
  
  void testCovariantMethodReturnTypeFromGenericsInterface() {
    shouldCompile """
        interface MyCallable<T> {
            T myCall() throws Exception;
        }

        class Task implements MyCallable<List> {
            List myCall() throws Exception {
                return [ 42 ]
            }
        } 
    """
  }
}
