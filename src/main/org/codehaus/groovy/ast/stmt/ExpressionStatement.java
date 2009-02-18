/*
 * Copyright 2003-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.ast.stmt;

import org.codehaus.groovy.ast.GroovyCodeVisitor;
import org.codehaus.groovy.ast.expr.Expression;


/**
 * A simple statement such as a method call where the return value is ignored
 * 
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public class ExpressionStatement extends Statement {

    private Expression expression;
    
    public ExpressionStatement(Expression expression) {
        if (expression == null) {
            throw new IllegalArgumentException("expression cannot be null");
        }
        this.expression = expression;
    }
    
    public void visit(GroovyCodeVisitor visitor) {
        visitor.visitExpressionStatement(this);
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public String getText() {
    	return this.toString();
    }
    public String toString() {
        return super.toString() + "[expression:" + expression + "]";
    }

}
