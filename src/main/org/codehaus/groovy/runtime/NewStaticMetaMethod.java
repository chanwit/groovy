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
package org.codehaus.groovy.runtime;

import groovy.lang.MetaMethod;

/**
 * A MetaMethod implementation where the underlying method is really a static
 * helper method on some class.
 * 
 * This implementation is used to add new static methods to the JDK writing them as normal
 * static methods with the first parameter being the class on which the method is added.
 * 
 * @author Guillaume Laforge
 * @version $Revision$
 */
public class NewStaticMetaMethod extends MetaMethod {

    private static final Class[] EMPTY_TYPE_ARRAY = {};
    
    private MetaMethod metaMethod;
    private Class[] logicalParameterTypes;
    
    public NewStaticMetaMethod(MetaMethod metaMethod) {
        super(metaMethod);
        this.metaMethod = metaMethod;
        init();
    }
    
    public NewStaticMetaMethod(String name, Class declaringClass, Class[] parameterTypes, Class returnType, int modifiers) {
        super(name, declaringClass, parameterTypes, returnType, modifiers);
        this.metaMethod = new MetaMethod(name, declaringClass, parameterTypes,returnType, modifiers);
        init();
    }
    
    private void init() {
        Class[] realParameterTypes = metaMethod.getParameterTypes();
        int size = realParameterTypes!=null ? realParameterTypes.length : 0;
        if (size <= 1) {
            logicalParameterTypes = EMPTY_TYPE_ARRAY;
        } else {
            logicalParameterTypes = new Class[--size];
            System.arraycopy(realParameterTypes, 1, logicalParameterTypes, 0, size);
        }
    }

    public Class getDeclaringClass() {
        return getBytecodeParameterTypes()[0];
    }

    public boolean isStatic() {
        return true;
    }

    public int getModifiers() {
        return super.getModifiers();
    }

    public Class[] getParameterTypes() {
        return logicalParameterTypes;
    }

    public Class[] getBytecodeParameterTypes() {
        return super.getParameterTypes();
    }

    public Object invoke(Object object, Object[] arguments) {
        int size = arguments.length;
        Object[] newArguments = new Object[size + 1];
        System.arraycopy(arguments, 0, newArguments, 1, size);
        newArguments[0] = null;
        return metaMethod.invoke(null, newArguments);
    }
}
