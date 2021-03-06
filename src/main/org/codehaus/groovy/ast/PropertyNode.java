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
package org.codehaus.groovy.ast;

import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.stmt.Statement;
import org.objectweb.asm.Opcodes;

/**
 * Represents a property (member variable, a getter and setter)
 * 
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public class PropertyNode extends AnnotatedNode implements Opcodes,Variable {

    private FieldNode field;

    private Statement getterBlock;
    private Statement setterBlock;
    private final int modifiers;
    private boolean closureShare = false;

    public PropertyNode(
        String name, int modifiers, ClassNode type, ClassNode owner,
        Expression initialValueExpression, Statement getterBlock,
        Statement setterBlock)
    {
        this(new FieldNode(name, modifiers & ACC_STATIC, type, owner, initialValueExpression), modifiers, getterBlock, setterBlock);
    }

    public PropertyNode(FieldNode field, int modifiers, Statement getterBlock, Statement setterBlock) {
        this.field = field;
        this.modifiers = modifiers;
        this.getterBlock = getterBlock;
        this.setterBlock = setterBlock;
    }

    public Statement getGetterBlock() {
        return getterBlock;
    }

    public Expression getInitialExpression() {
        return field.getInitialExpression();
    }

    public void setGetterBlock(Statement getterBlock) {
        this.getterBlock = getterBlock;
    }

    public void setSetterBlock(Statement setterBlock) {
        this.setterBlock = setterBlock;
    }

    public int getModifiers() {
        return modifiers;
    }

    public String getName() {
        return field.getName();
    }

    public Statement getSetterBlock() {
        return setterBlock;
    }

    public ClassNode getType() {
        return field.getType();
    }

    public void setType(ClassNode t) {
        field.setType(t);
    }
    
    public FieldNode getField() {
        return field;
    }
    
    public void setField(FieldNode fn) {
    	field = fn;
    }

    public boolean isPrivate() {
        return (modifiers & ACC_PRIVATE) != 0;
    }
    
    public boolean isPublic() {
        return (modifiers & ACC_PUBLIC) != 0;
    }

    public boolean isStatic() {
        return (modifiers & ACC_STATIC) != 0;
    }

    public boolean hasInitialExpression() {
        return field.hasInitialExpression();
    }

    public boolean isInStaticContext() {
        return field.isInStaticContext();
    }

    public boolean isDynamicTyped() {
        return field.isDynamicTyped();
    }

    public boolean isClosureSharedVariable() {
        return false;
    }
    
    public void setClosureSharedVariable(boolean inClosure) {
        closureShare = inClosure;        
    }

	public ClassNode getOriginType() {
		return getType();
	}
}
