package groovy.jmx.builder.vm5

import javax.management.MBeanServerConnection
import javax.management.ObjectName
import groovy.jmx.builder.JmxBuilder

class JmxBeanExportFactoryTest extends GroovyTestCase {
  JmxBuilder builder
  MBeanServerConnection server

  void setUp() {
    builder = new JmxBuilder()
    server = builder.getMBeanServer()
  }

  void testBeanExportReplacePolicy() {
    def object = new MockManagedObject()
    // defaults to regpolicy = replace
    def beans = builder.export {
      bean(object)
      bean(object)
    }
    assert beans
    assert beans.size() == 1
  }

  void testBeanExportIgnorePolicy() {
    def object = new MockManagedObject()
    def beans = builder.export(regPolicy: "ignore") {
      bean(object)
      bean(object)
    }
    assert beans
    assert beans.size() == 1
  }

  void testBeanExportErrorPolicy() {
    def object = new MockManagedObject()
    shouldFail {
      def beans = builder.export(regPolicy: "error") {
        bean(object)
        bean(object)
      }
      assert beans
      assert beans.size() == 1
    }
  }

  void testImplicitBeanGeneration() {
    def object = new MockManagedObject()
    def objName = "jmx.builder:type=ExportedObject,name=${object.class.canonicalName}@${object.hashCode()}"

    def beans = builder.export {
      bean(object)
    }

    GroovyMBean bean = new GroovyMBean(server, new ObjectName(objName))

    assert server.isRegistered(new ObjectName(objName))

    assert bean.info().getConstructors().size() == 3

    assert bean
    assert bean.name().toString() == objName

    assert bean.info().getAttribute("Something").name == "Something"
    assert bean.info().getAttribute("Something").type == "java.lang.String"
    assert bean.info().getAttribute("Something").descriptor.getFieldValue("name") == "Something"
    assert bean.info().getAttribute("Something").descriptor.getFieldValue("readable")
    assert !bean.info().getAttribute("Something").descriptor.getFieldValue("writable")

    assert bean.info().getOperation("doSomething").name == "doSomething"
    assert bean.info().getOperation("doSomething").signature.size() == 0
    assert bean.info().getOperation("doSomethingElse").name == "doSomethingElse"
    assert bean.info().getOperation("doSomethingElse").signature.size() == 2
    assert bean.info().getOperation("doSomethingElse").signature[0].type == "int"
    assert bean.info().getOperation("doSomethingElse").signature[1].type == "java.lang.String"
  }

  void testImplicitWithObjectName() {
    def objName = "jmx.builder:type=ImplicitDescriptor"
    def object = new MockManagedObject()

    def beans = builder.export {
      bean([target: object, name: objName])
    }

    assert beans

    def bean = beans[0]
    assert bean

    assert bean.info().getConstructors().size() == 3

    assert bean
    assert bean.name().toString() == objName

    assert bean.info().getAttribute("Something")
    assert bean.info().getAttribute("SomethingElse")
    assert bean.info().getOperation("doSomething")
    assert bean.info().getOperation("doSomething").signature.size() == 0
    assert bean.info().getOperation("doSomethingElse")
    assert bean.info().getOperation("doSomethingElse").signature.size() == 2
  }

  void testEmbeddedBeanExport() {
    def object = new MockManagedGroovyObject()
    def beans = builder.export {
      bean(object)
    }
    assert beans
    def bean = beans[0]

    assert bean.name().toString() == "jmx.builder:type=EmbeddedObject"
    assert bean.info().getAttribute("Id").name == "Id"
    assert bean.info().getAttribute("Id").descriptor.getFieldValue("name") == "Id"
    assert bean.info().getAttribute("Id").descriptor.getFieldValue("readable")
    assert !bean.info().getAttribute("Id").descriptor.getFieldValue("writable")

    assert bean.info().getAttribute("Location").name == "Location"
    assert bean.info().getAttribute("Id").name == "Id"

    assert bean.info().getOperation("doSomething").name == "doSomething"
    assert bean.info().getOperation("doSomething").signature.size() == 0
    assert bean.info().getOperation("doSomethingElse").name == "doSomethingElse"
    assert bean.info().getOperation("doSomethingElse").signature.size() == 0
  }

  void testExplicitAttributesDeclaration() {
    def objName = "jmx.builder:type=ExplicitDescriptor"
    def beans
    def gbean

    beans = builder.export {
      bean([target: new MockManagedObject(), name: objName,
              attributes: "*"])
    }

    assert beans
    gbean = beans[0]

    assert gbean
    assert gbean.info().getAttribute("Something")
    assert gbean.info().getAttribute("SomethingElse")
    assert gbean.info().getOperations().size() == 2

    beans = builder.export {
      bean([target: new MockManagedObject(), name: objName,
              attributes: ["Something", "SomethingElse"]
      ])
    }
    gbean = beans[0]
    assert gbean.info().getAttribute("Something")
    assert gbean.info().getAttribute("SomethingElse")
    assert gbean.info().getOperations().size() == 2

    beans = builder.export {
      bean([target: new MockManagedObject(), name: objName,
              attributes: ["Something"]
      ])
    }
    gbean = beans[0]
    assert gbean.info().getAttribute("Something")
    assert !gbean.info().getAttribute("SomethingElse")
    assert gbean.info().getOperations().size() == 1

    beans = builder.export {
      bean([target: new MockManagedObject(), name: objName,
              attributes: [
                      "Something": [desc: "Something description", readable: true, writeable: false],
                      "SomethingElse": [readable: true, writable: true, defaultValue: "Hello"]
              ]
      ])
    }

    gbean = beans[0]
    assert gbean
    assert gbean.info().getAttribute("Something")
    assert gbean.info().getAttribute("Something").description == "Something description"
    assert gbean.info().getAttribute("Something").descriptor.getFieldValue("readable")
    assert !gbean.info().getAttribute("Something").descriptor.getFieldValue("writable")
    assert gbean.info().getAttribute("SomethingElse")
    assert gbean.info().getAttribute("SomethingElse").descriptor.getFieldValue("readable")
    assert gbean.info().getAttribute("SomethingElse").descriptor.getFieldValue("writable")

    assert gbean.info().getAttribute("SomethingElse").descriptor.getFieldValue("default").equals("Hello")
    assert gbean.info().getOperations().size() == 3
  }


  void testExplicitConstructorDeclaration() {
    def objName = "jmx.builder:type=ExplicitDescriptor"
    def beans
    GroovyMBean gbean

    beans = builder.export {
      bean([target: new MockManagedObject(), name: objName,
              ctors: "*"
      ])
    }
    gbean = beans[0]
    assert gbean
    assert gbean.info().getConstructors().size() == 3

    beans = builder.export {
      bean([target: new MockManagedObject(), name: objName,
              ctors: [
                      "MockManagedObject": []
              ]
      ])
    }
    gbean = beans[0]
    assert gbean
    assert gbean.info().getConstructors().size() == 1
    assert gbean.info().getConstructors()[0].signature.size() == 0

    beans = builder.export {
      bean([target: new MockManagedObject(), name: objName,
              ctors: [
                      "MockManagedObject": ["String", "int"]
              ]
      ])
    }
    gbean = beans[0]
    assert gbean
    assert gbean.info().getConstructors().size() == 1
    assert gbean.info().getConstructors()[0].signature.size() == 2
    assert gbean.info().getConstructors()[0].signature[0].type == "java.lang.String"
    assert gbean.info().getConstructors()[0].signature[1].type == "int"

    beans = builder.export {
      bean([target: new MockManagedObject(), name: objName,
              ctors: [
                      "MockManagedObject": [
                              desc: "Class creator",
                              params: [
                                      "String": [name: "initialTarget", desc: "The constructor target"],
                                      "int": [name: "quantity"]
                              ]
                      ]
              ]
      ])
    }
    gbean = beans[0]
    assert gbean
    assert gbean.info().getConstructors().size() == 1
    assert gbean.info().getConstructors()[0].signature.size() == 2
    assert gbean.info().getConstructors()[0].signature[0].type == "java.lang.String"
    assert gbean.info().getConstructors()[0].signature[0].name == "initialTarget"
    assert gbean.info().getConstructors()[0].signature[0].description == "The constructor target"
    assert gbean.info().getConstructors()[0].signature[1].type == "int"
  }


  void testExplicitOperationDeclaration() {
    def objName = "jmx.builder:type=ExplicitDescriptor"
    def beans
    GroovyMBean gbean

    beans = builder.export {
      bean(target: new MockManagedObject(), name: objName, ops: "*")
    }
    gbean = beans[0]
    assert gbean
    assert gbean.info().getOperation("doSomething")
    assert gbean.info().getOperation("doSomethingElse")
    assert gbean.info().getOperation("dontDoThis")
    assert gbean.info().getAttributes().size() == 0
    assert gbean.info().getConstructors().size() == 0

    beans = builder.export {
      bean([target: new MockManagedObject(), name: objName,
              ops: ["doSomething", "dontDoThis"]
      ])
    }
    gbean = beans[0]
    assert gbean
    assert gbean.info().getOperation("doSomething")
    assert !gbean.info().getOperation("doSomethingElse")
    assert gbean.info().getOperation("dontDoThis")
    assert gbean.info().getAttributes().size() == 0
    assert gbean.info().getConstructors().size() == 0

    beans = builder.export {
      bean([target: new MockManagedObject(), name: objName,
              ops: [
                      "doSomething": "*",
                      "doSomethingElse": ["int", "String"]
              ]
      ])
    }
    gbean = beans[0]
    assert gbean
    assert gbean.info().getOperation("doSomething")
    assert gbean.info().getOperation("doSomethingElse")
    assert !gbean.info().getOperation("dontDoThis")
    assert gbean.info().getAttributes().size() == 0
    assert gbean.info().getConstructors().size() == 0

    beans = builder.export {
      bean([target: new MockManagedObject(), name: objName,
              ops: [
                      "doSomething": [params: []],

                      "doSomethingElse": [
                              desc: "SomethingElse will be done",
                              params: ["int", "java.lang.String"]
                      ]
              ]
      ])
    }

    gbean = beans[0]
    assert gbean
    assert gbean.info().getOperation("doSomething")
    assert gbean.info().getOperation("doSomething").signature.size() == 0

    assert gbean.info().getOperation("doSomethingElse")
    assert gbean.info().getOperation("doSomethingElse").description == "SomethingElse will be done"
    assert gbean.info().getOperation("doSomethingElse").signature.size() == 2
    assert gbean.info().getOperation("doSomethingElse").signature[0].type == "int"
    assert gbean.info().getOperation("doSomethingElse").signature[1].type == "java.lang.String"

    assert !gbean.info().getOperation("dontDoThis")
    assert gbean.info().getAttributes().size() == 0
    assert gbean.info().getConstructors().size() == 0

  } 
 


  void testAttributeChangeListener() {
    def objName = "jmx.builder:type=ExplicitDescriptor"
    def object = new MockManagedObject()
    def testFlag = "0"

    MockManagedObject.metaClass."dynaMethod" = {
      testFlag = "dyna"
    }
    def beans = builder.export {
      bean(target: object, name: objName,
              attributes: [
                      "Something": [desc: "Something description", defaultValue: "Hello", writable: true,
                              onChange: {event ->
                                testFlag = 1
                              }
                      ],

                      "SomethingElse": [
                              writable: true,
                              onChange: object.&dynaMethod
                      ]
              ]
      )
    }

    assert beans
    beans[0].Something = "A Great Day"
    assert testFlag == 1
    testFlag = "0"
    beans[0].Something = "A Car"
    assert testFlag == 1

    beans[0].SomethingElse = 3
    assert testFlag == "dyna"
  }


  void testOperationCallListener() {
    def object = new MockManagedObject()
    def testFlag1 = "0"
    def testFlag2 = 1
    def testFlag3 = "2"
    def testFlag4 = 4
    def beans = builder.export {
      bean(
              target: object, name: "jmx.builder:type=ExplicitObject",
              operations: [
                      doSomethingElse: [params: ["int", "String"],
                              onCall: {event ->
                                testFlag1 = "1"
                              }
                      ],
                      doSomething: [
                              onCall: {event ->
                                testFlag2 = "2"
                              }
                      ],
                      dontDoThis: [
                              params: ["Object"],
                              onCall: {->
                                testFlag3 = 3 + 5
                              }
                      ],
                      doWork: [
                              params: ["int", "String"],
                              onCall: {->
                                testFlag4 = 4
                              }
                      ]
              ]
      )
    }

    assert beans
    beans[0].doSomethingElse(40, "foo")
    assert testFlag1 == "1"
    beans[0].doSomething()
    assert testFlag2 == "2"
    beans[0].dontDoThis("work")
    assert testFlag3 == 8
  }
  

  void testExportMBeanObject() {
    def object = new MockSimpleObject(id: "0001", priority: 99)
    def object2 = new MockSimpleObject(id: "0003", priority: 100)

    assert object.id == "0001"
    assert object.priority == 99

    def beans = builder.export {
      bean(object)
      bean(target: object2, name: "jmx.builder:type=MBeanObject")
    }
    assert beans
    assert beans[0].Id == "0001"
    beans[0].Id = "0008"
    shouldFail {
      assert beans[0].Id == "0001"
    }

    assert beans[1].Id == "0003"
    assert beans[1].Priority == 100
  }
}