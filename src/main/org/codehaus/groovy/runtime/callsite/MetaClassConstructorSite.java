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
package org.codehaus.groovy.runtime.callsite;

import groovy.lang.MetaClass;

/**
 * Call site for constructor
 *   meta class - cached
 *   method - not cached
 *
 * @author Alex Tkachman
*/
public class MetaClassConstructorSite extends MetaClassSite {
    public MetaClassConstructorSite(CallSite site, MetaClass metaClass) {
        super(site, metaClass);
    }

    public final Object invoke(Object receiver, Object[] args) {
        return metaClass.invokeConstructor(args);
    }

    public boolean accept(Object receiver, Object[] args) {
        return receiver == metaClass.getTheClass();
    }
}