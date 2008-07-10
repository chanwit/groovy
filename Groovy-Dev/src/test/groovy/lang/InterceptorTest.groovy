package groovy.lang

import org.codehaus.groovy.runtime.StringBufferWriter

/**
* Test for the Interceptor Interface usage as implemented by the
* TracingInterceptor. Makes also use of the ProxyMetaClass and
* shows the collaboration.
* As a side Effect, the ProxyMetaClass is also partly tested.
* @author Dierk Koenig
**/
class InterceptorTest extends GroovyTestCase{

    def Interceptor logInterceptor
    def StringBuffer log
    def interceptable   // the object to intercept method calls on
    def proxy

    void setUp() {
        logInterceptor = new TracingInterceptor()
        log = new StringBuffer("\n")
        logInterceptor.writer = new StringBufferWriter(log)
        // we intercept calls from Groovy to the java.lang.String object
        interceptable = 'Interceptable String'
        proxy = ProxyMetaClass.getInstance(interceptable.class)
        proxy.setInterceptor(logInterceptor)
    }

    void testSimpleInterception() {
        proxy.use {
            assertEquals 20, interceptable.size()
            assertEquals 20, interceptable.length()
            assertTrue interceptable.startsWith('I',0)
        }
        assertEquals(
"""
before java.lang.String.size()
after  java.lang.String.size()
before java.lang.String.length()
after  java.lang.String.length()
before java.lang.String.startsWith(java.lang.String, java.lang.Integer)
after  java.lang.String.startsWith(java.lang.String, java.lang.Integer)
""", log.toString())
    }

    void testNoInterceptionWithNullInterceptor() {
        proxy.setInterceptor(null)
        proxy.use {
            interceptable.size()
        }
    }

    void testConstructorInterception() {
        proxy.use {
            new String('some string')
        }
        assertEquals(
"""
before java.lang.String.ctor(java.lang.String)
after  java.lang.String.ctor(java.lang.String)
""", log.toString())
    }

    void testStaticMethodInterception() {
        proxy.use {
            assertEquals 'true', String.valueOf(true)
        }
        assertEquals(
"""
before java.lang.String.valueOf(java.lang.Boolean)
after  java.lang.String.valueOf(java.lang.Boolean)
""", log.toString())
    }

    void testInterceptionOfGroovyClasses(){
        def slicer = new groovy.mock.example.CheeseSlicer()
        def proxy = ProxyMetaClass.getInstance(slicer.class)
        proxy.setInterceptor(logInterceptor)
        proxy.use(slicer) {
            slicer.coffeeBreak('')
        }
        assertEquals(
"""
before groovy.mock.example.CheeseSlicer.coffeeBreak(java.lang.String)
after  groovy.mock.example.CheeseSlicer.coffeeBreak(java.lang.String)
""", log.toString())
    }
}



