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

import groovy.lang.GroovyObject;
import groovy.lang.MetaClassImpl;
import groovy.lang.MetaMethod;
import org.codehaus.groovy.runtime.MetaClassHelper;

/**
 * POGO call site
 *   meta class - cached
 *   method - cached
*/
public class PogoMetaMethodSite extends MetaMethodSite {
    public PogoMetaMethodSite(String name, MetaClassImpl metaClass, MetaMethod metaMethod, Class params[]) {
        super(name, metaClass, metaMethod, params);
    }

    public Object call(Object receiver, Object[] args) {
        MetaClassHelper.unwrap(args);
        return metaMethod.doMethodInvoke(receiver,  args);
    }

    public final boolean accept(Object receiver, Object[] args) {
        return receiver.getClass() == metaClass.getTheClass() // meta class match receiver
           && ((GroovyObject)receiver).getMetaClass() == metaClass // metaClass still be valid
           && MetaClassHelper.sameClasses(params, args, false); // right arguments
    }

    public static PogoMetaMethodSite createPogoMetaMethodSite(MetaClassImpl metaClass, MetaMethod metaMethod, String name, Class[] params, Object[] args) {
        if (metaMethod.correctArguments(args) == args) {
            if (CallSiteArray.noWrappers(args)) {
                if (CallSiteArray.noCoerce(metaMethod,args))
                    return new PogoMetaMethodSiteNoUnwrap(name, metaClass, metaMethod, params);
                else
                    return new PogoMetaMethodSiteNoUnwrapNoCoerce(name, metaClass, metaMethod, params);
            }
        }
        return new PogoMetaMethodSite(name, metaClass, metaMethod, params);
    }

    /**
     * Call site where we know there is no need to unwrap arguments
     */
    public static class PogoMetaMethodSiteNoUnwrap extends PogoMetaMethodSite {

        public PogoMetaMethodSiteNoUnwrap(String name, MetaClassImpl metaClass, MetaMethod metaMethod, Class params[]) {
            super(name, metaClass, metaMethod, params);
        }

        public final Object call(Object receiver, Object[] args) {
            return metaMethod.doMethodInvoke(receiver,  args);
        }
    }

    /**
     * Call site where we know there is no need neither unwrap nor coerce arguments
     */
    public static class PogoMetaMethodSiteNoUnwrapNoCoerce extends PogoMetaMethodSite {

        public PogoMetaMethodSiteNoUnwrapNoCoerce(String name, MetaClassImpl metaClass, MetaMethod metaMethod, Class params[]) {
            super(name, metaClass, metaMethod, params);
        }

        public final Object call(Object receiver, Object[] args) {
            return metaMethod.invoke(receiver,  args);
        }
    }
}