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

import groovy.lang.GString;
import org.codehaus.groovy.reflection.CachedClass;
import org.codehaus.groovy.reflection.ClassInfo;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

import java.math.BigDecimal;

/**
 * @author Alex.Tkachman
 */
public class ArrayCachedClass extends CachedClass {
    public ArrayCachedClass(Class klazz, ClassInfo classInfo) {
        super(klazz, classInfo);
    }

    public Object coerceArgument(Object argument) {
        Class argumentClass = argument.getClass();
        if (argumentClass.getName().charAt(0) != '[') return argument;
        Class argumentComponent = argumentClass.getComponentType();

        Class paramComponent = getTheClass().getComponentType();
        if (paramComponent.isPrimitive()) {
        	argument = DefaultTypeTransformation.convertToPrimitiveArray(argument, paramComponent);
        } else if (paramComponent == String.class && argument instanceof GString[]) {
            GString[] strings = (GString[]) argument;
            String[] ret = new String[strings.length];
            for (int i = 0; i < strings.length; i++) {
                ret[i] = strings[i].toString();
            }
            argument = ret;
        } else if (paramComponent==Object.class && argumentComponent.isPrimitive()){
            argument = DefaultTypeTransformation.primitiveArrayBox(argument);
        }
        return argument;
    }

}
