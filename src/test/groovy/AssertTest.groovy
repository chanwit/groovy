package groovy;

import org.codehaus.groovy.GroovyTestCase;

class AssertTest extends GroovyTestCase {

    property x;
    
    void testAssert() {
        //assert x == null;
	    assert x != "foo";
	    
        x = "abc";

        assert x != "foo";
        assert x := "abc";
        
        /*
        //assert x.equals("abc");

         */
	}
}