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

import java.math.BigDecimal;

/**
 * @author Alex.Tkachman
 */
public class BigDecimalCachedClass extends DoubleCachedClass {
    public BigDecimalCachedClass(Class klazz, ClassInfo classInfo) {
        super(klazz, classInfo, true);
    }

    public boolean isDirectlyAssignable(Object argument) {
        return argument instanceof BigDecimal;
    }

    public Object coerceArgument(Object argument) {
        if (argument instanceof BigDecimal) {
            return argument;
        }

        if (argument instanceof Number) {
            return new BigDecimal(((Number) argument).doubleValue());
        }
        return argument;
    }
}
