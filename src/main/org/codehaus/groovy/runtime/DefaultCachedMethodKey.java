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

import org.codehaus.groovy.reflection.CachedClass;


/**
 * A default implementation of MethodKey
 *
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision: 7922 $
 */
public class DefaultCachedMethodKey extends MethodKey{

    private final CachedClass[] parameterTypes;

    public DefaultCachedMethodKey(Class sender, String name, CachedClass[] parameterTypes, boolean isCallToSuper) {
        super(sender, name,isCallToSuper);
        this.parameterTypes = parameterTypes;
    }

    public int getParameterCount() {
        return parameterTypes.length;
    }

    public Class getParameterType(int index) {
        CachedClass c = parameterTypes[index];
        if (c==null) return Object.class;
        return c.getTheClass();
    }
}
