/*
 * $Id$version
 * Nov 23, 2003 9:02:55 PM $user Exp $
 * 
 * Copyright 2003 (C) Sam Pullara. All Rights Reserved.
 * 
 * Redistribution and use of this software and associated documentation
 * ("Software"), with or without modification, are permitted provided that the
 * following conditions are met: 1. Redistributions of source code must retain
 * copyright statements and notices. Redistributions must also contain a copy
 * of this document. 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in
 * the documentation and/or other materials provided with the distribution. 3.
 * The name "groovy" must not be used to endorse or promote products derived
 * from this Software without prior written permission of The Codehaus. For
 * written permission, please contact info@codehaus.org. 4. Products derived
 * from this Software may not be called "groovy" nor may "groovy" appear in
 * their names without prior written permission of The Codehaus. "groovy" is a
 * registered trademark of The Codehaus. 5. Due credit should be given to The
 * Codehaus - http://groovy.codehaus.org/
 * 
 * THIS SOFTWARE IS PROVIDED BY THE CODEHAUS AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE CODEHAUS OR ITS CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 *  
 */
package groovy.lang;

import groovy.util.GroovyTestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author sam
 */
public class GroovyShellTest extends GroovyTestCase {

    private String script1 = "test = 1";

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(GroovyShellTest.class);
    }

    public void testExecuteScript() {
        GroovyShell shell = new GroovyShell();
        try {
            Object result = shell.evaluate(new ByteArrayInputStream(script1.getBytes()), "Test.groovy");
            assertEquals(new Integer(1), result);
        }
        catch (Exception e) {
            fail(e.toString());
        }
    }

    private static class PropertyHolder {
        private Map map = new HashMap();

        public void set(String key, Object value) {
            map.put(key, value);
        }

        public Object get(String key) {
            return map.get(key);
        }
    }

    private String script2 = "test.prop = 2\nreturn test.prop";

    public void testExecuteScriptWithContext() {
        Binding context = new Binding();
        context.setVariable("test", new PropertyHolder());
        GroovyShell shell = new GroovyShell(context);
        try {
            Object result = shell.evaluate(new ByteArrayInputStream(script2.getBytes()), "Test.groovy");
            assertEquals(new Integer(2), result);
        }
        catch (Exception e) {
            fail(e.toString());
        }
    }

    public void testScriptWithDerivedBaseClass() throws Exception {
        Binding context = new Binding();
        CompilerConfiguration config = new CompilerConfiguration();
        config.setScriptBaseClass(DerivedScript.class.getName());
        GroovyShell shell = new GroovyShell(context, config);
        Object result = shell.evaluate("x = 'abc'; doSomething(cheese)");
        assertEquals("I like Cheddar", result);
        assertEquals("abc", context.getVariable("x"));
    }
}
