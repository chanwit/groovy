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

import groovy.lang.MetaClassImpl;
import org.codehaus.groovy.reflection.CachedConstructor;
import org.codehaus.groovy.runtime.MetaClassHelper;

import java.util.Map;

public class ConstructorSite extends MetaClassSite {
    final CachedConstructor constructor;
    final Class [] params;

    public ConstructorSite(CallSite site, MetaClassImpl metaClass, CachedConstructor constructor, Class params[]) {
        super(site, metaClass);
        this.constructor = constructor;
        this.params = params;
    }

    public Object invoke(Object receiver, Object[] args) {
        MetaClassHelper.unwrap(args);
        return constructor.doConstructorInvoke(args);
    }

    public final CallSite acceptConstructor(Object receiver, Object[] args) {
        if (receiver == metaClass.getTheClass() // meta class match receiver
           && MetaClassHelper.sameClasses(params, args) ) // right arguments
          return this;
        else
          return createCallConstructorSite((Class)receiver, args);
    }

    public static ConstructorSite createConstructorSite(CallSite site, MetaClassImpl metaClass, CachedConstructor constructor, Class[] params, Object[] args) {
        if (constructor.correctArguments(args) == args) {
            if (noWrappers(args)) {
                if (noCoerce(constructor,args))
                    return new ConstructorSiteNoUnwrap(site, metaClass, constructor, params);
                else
                    return new ConstructorSiteNoUnwrapNoCoerce(site, metaClass, constructor, params);
            }
        }
        return new ConstructorSite(site, metaClass, constructor, params);
    }


    /**
     * Call site where we know there is no need to unwrap arguments
     */
    public static class ConstructorSiteNoUnwrap extends ConstructorSite {

        public ConstructorSiteNoUnwrap(CallSite site, MetaClassImpl metaClass, CachedConstructor constructor, Class params[]) {
            super(site, metaClass, constructor, params);
        }

        public final Object invoke(Object receiver, Object[] args) {
            return constructor.doConstructorInvoke(args);
        }
    }

    /**
     * Call site where we know there is no need neither unwrap nor coerce arguments
     */
    public static class ConstructorSiteNoUnwrapNoCoerce extends ConstructorSite {

        public ConstructorSiteNoUnwrapNoCoerce(CallSite site, MetaClassImpl metaClass, CachedConstructor constructor, Class params[]) {
            super(site, metaClass, constructor, params);
        }

        public Object invoke(Object receiver, Object[] args) {
            return constructor.invoke(args);
        }
    }

    public static class NoParamSite extends ConstructorSiteNoUnwrapNoCoerce {
        private static final Object[] NO_ARGS = new Object[0];

        public NoParamSite(CallSite site, MetaClassImpl metaClass, CachedConstructor constructor, Class[] params) {
            super(site, metaClass, constructor, params);
        }

        public final Object invoke(Object receiver, Object[] args) {
            final Object bean = constructor.invoke(NO_ARGS);
            ((MetaClassImpl)metaClass).setProperties(bean, (Map) args[0]);
            return bean;
        }
    }
}