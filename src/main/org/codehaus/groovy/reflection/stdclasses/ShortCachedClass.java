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
package org.codehaus.groovy.reflection.stdclasses;

import org.codehaus.groovy.reflection.ClassInfo;

/**
 * @author Alex.Tkachman
 */
public class ShortCachedClass extends NumberCachedClass {
	private boolean allowNull;
    public ShortCachedClass(Class klazz, ClassInfo classInfo, boolean allowNull) {
        super(klazz, classInfo);
        this.allowNull = allowNull;
    }

    public Object coerceArgument(Object argument) {
        if (argument instanceof Short) {
            return argument;
        }

        return new Short(((Number) argument).shortValue());
    }

    public boolean isDirectlyAssignable(Object argument) {
        return (allowNull && argument == null) || argument instanceof Short;
    }

    public boolean isAssignableFrom(Class classToTransformFrom) {
        return (allowNull && classToTransformFrom == null)
            || classToTransformFrom == Short.class
            || classToTransformFrom == Byte.class
            || classToTransformFrom == Short.TYPE
            || classToTransformFrom == Byte.TYPE;
    }
}
