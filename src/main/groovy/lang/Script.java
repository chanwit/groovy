/*
 $Id$

 Copyright 2003 (C) James Strachan and Bob Mcwhirter. All Rights Reserved.

 Redistribution and use of this software and associated documentation
 ("Software"), with or without modification, are permitted provided
 that the following conditions are met:

 1. Redistributions of source code must retain copyright
    statements and notices.  Redistributions must also contain a
    copy of this document.

 2. Redistributions in binary form must reproduce the
    above copyright notice, this list of conditions and the
    following disclaimer in the documentation and/or other
    materials provided with the distribution.

 3. The name "groovy" must not be used to endorse or promote
    products derived from this Software without prior written
    permission of The Codehaus.  For written permission,
    please contact info@codehaus.org.

 4. Products derived from this Software may not be called "groovy"
    nor may "groovy" appear in their names without prior written
    permission of The Codehaus. "groovy" is a registered
    trademark of The Codehaus.

 5. Due credit should be given to The Codehaus -
    http://groovy.codehaus.org/

 THIS SOFTWARE IS PROVIDED BY THE CODEHAUS AND CONTRIBUTORS
 ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 THE CODEHAUS OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 OF THE POSSIBILITY OF SUCH DAMAGE.

 */
package groovy.lang;

import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * This object represents a Groovy script
 * 
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public abstract class Script extends GroovyObjectSupport {
    private Binding binding = new Binding();

    public Binding getBinding() {
        return binding;
    }

    public void setBinding(Binding binding) {
        this.binding = binding;
    }

    public Object getProperty(String property) {
        //System.out.println("Script.getProperty for: " + property + " with binding: " + binding.getVariables());
        return binding.getVariable(property);
    }

    public void setProperty(String property, Object newValue) {
        //System.out.println("Script.setProperty for: " + property + " with newValue: " + newValue);
        binding.setVariable(property, newValue);
        //System.out.println("binding are now: " + binding.getVariables());
    }

    /**
     * The main instance method of a script which has variables in scope
     * as defined by the current {@link ShellContext} instance
     * @return
     */
    public abstract Object run();
    
    
    // println helper methods
    
    public void println() {
        Object object = getProperty("out");
        if (object != null) {
            InvokerHelper.invokeMethod(object, "println", ArgumentListExpression.EMPTY_ARRAY);
        }
        else {
            System.out.println();
        }
    }
    
    public void print(Object value) {
        Object object = getProperty("out");
        if (object != null) {
            InvokerHelper.invokeMethod(object, "print", new Object[] { value });
        }
        else {
            System.out.print(value);
        }
    }
    
    public void println(Object value) {
        Object object = getProperty("out");
        if (object != null) {
            InvokerHelper.invokeMethod(object, "println", new Object[] { value });
        }
        else {
            System.out.println(value);
        }
    }
}
