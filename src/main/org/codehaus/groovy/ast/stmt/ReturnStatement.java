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
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;

/**
 * A return statement
 * 
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public class ReturnStatement extends Statement {
    /**
     * Only used for synthetic return statements emitted by the compiler.
     * For comparisons use isReturningNullOrVoid() instead.
     */
    public static final ReturnStatement RETURN_NULL_OR_VOID = new ReturnStatement(ConstantExpression.NULL);

    private Expression expression;
    
    public ReturnStatement(ExpressionStatement statement) {
        this(statement.getExpression());
        setStatementLabel(statement.getStatementLabel());
    }
    
    public ReturnStatement(Expression expression) {
        this.expression = expression;
    }
    
    public void visit(GroovyCodeVisitor visitor) {
        visitor.visitReturnStatement(this);
    }

    public Expression getExpression() {
        return expression;
    }

    public String getText() {
        return "return " + expression.getText();
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public boolean isReturningNullOrVoid() {
        return expression instanceof ConstantExpression
            && ((ConstantExpression)expression).isNullExpression();
    }
}
