package org.codehaus.groovy.runtime.dgmimpl.arrays;

import groovy.lang.MetaClassImpl;
import groovy.lang.MetaMethod;
import org.codehaus.groovy.reflection.CachedClass;
import org.codehaus.groovy.reflection.ReflectionCache;
import org.codehaus.groovy.runtime.callsite.CallSite;
import org.codehaus.groovy.runtime.callsite.PojoMetaMethodSite;

/**
 * Created by IntelliJ IDEA.
* User: applerestore
* Date: Mar 16, 2008
* Time: 3:43:51 PM
* To change this template use File | Settings | File Templates.
*/
public class FloatArrayGetAtMetaMethod extends ArrayGetAtMetaMethod {
        private static final CachedClass ARR_CLASS = ReflectionCache.getCachedClass(float[].class);

        public Class getReturnType() {
            return Float.class;
        }

        public final CachedClass getDeclaringClass() {
            return ARR_CLASS;
        }

        public Object invoke(Object object, Object[] args) {
            final float[] objects = (float[]) object;
            return objects[normaliseIndex(((Integer) args[0]).intValue(), objects.length)];
        }

        public CallSite createPojoCallSite(CallSite site, MetaClassImpl metaClass, MetaMethod metaMethod, Class[] params, Object receiver, Object[] args) {
            if (!(args [0] instanceof Integer))
              return PojoMetaMethodSite.createNonAwareCallSite(site, metaClass, metaMethod, params, args);
            else
                return new PojoMetaMethodSite(site, metaClass, metaMethod, params) {
                    public Object invoke(Object receiver, Object[] args) {
                        final float[] objects = (float[]) receiver;
                        return objects[normaliseIndex(((Integer) args[0]).intValue(), objects.length)];
                    }

                    public Object callBinop(Object receiver, Object arg) {
                        if ((receiver instanceof float[] && arg instanceof Integer)
                                && checkPojoMetaClass()) {
                            final float[] objects = (float[]) receiver;
                            return objects[normaliseIndex(((Integer) arg).intValue(), objects.length)];
                        }
                        else
                          return super.callBinop(receiver,arg);
                    }

                    public Object invokeBinop(Object receiver, Object arg) {
                        final float[] objects = (float[]) receiver;
                        return objects[normaliseIndex(((Integer) arg).intValue(), objects.length)];
                    }
                };
        }
     }