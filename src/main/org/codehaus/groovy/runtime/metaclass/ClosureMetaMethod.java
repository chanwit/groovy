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
package org.codehaus.groovy.runtime.metaclass;

import groovy.lang.Closure;
import groovy.lang.ClosureInvokingMethod;
import groovy.lang.MetaMethod;
import org.codehaus.groovy.reflection.CachedClass;
import org.codehaus.groovy.reflection.CachedMethod;
import org.codehaus.groovy.reflection.ReflectionCache;
import org.codehaus.groovy.runtime.GeneratedClosure;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.MethodClosure;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * A MetaMethod that accepts a closure in the constructor which is invoked when the MetaMethod is called.
 * The delegate of the closure is set to the instance that the MetaMethod is invoked on when called.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
public class ClosureMetaMethod extends MetaMethod implements ClosureInvokingMethod {

	private final Closure callable;
    private final CachedMethod doCall;
    private final String name;
    private final CachedClass declaringClass;

    public ClosureMetaMethod(String name, Closure c, CachedMethod doCall) {
		this(name, c.getOwner().getClass(), c, doCall);
	}

    public ClosureMetaMethod(String name, Class declaringClass, Closure c, CachedMethod doCall) {
        super (doCall.getNativeParameterTypes());
        this.name = name;
        callable = c;
        this.doCall = doCall;
        this.declaringClass = ReflectionCache.getCachedClass(declaringClass);
    }


    public int getModifiers() {
        return Modifier.PUBLIC;
    }

    public String getName() {
        return name;
    }

    public Class getReturnType() {
        return Object.class;
    }

	public CachedClass getDeclaringClass() {
		return declaringClass;
	}

	public Object invoke(final Object object, Object[] arguments) {
		Closure cloned = (Closure) callable.clone();
		cloned.setDelegate(object);

        arguments = coerceArgumentsToClasses(arguments);
        return doCall.invoke(cloned, arguments);
	}

  /**
     * Retrieves the closure that is invoked by this MetaMethod
     *
     * @return The closure
     */
    public Closure getClosure() {
		return callable;
	}

    public static List<MetaMethod> createMethodList(final String name, final Class declaringClass, final Closure closure) {
        ArrayList<MetaMethod> res = new ArrayList<MetaMethod>();
        if (closure instanceof MethodClosure) {
            MethodClosure methodClosure = (MethodClosure) closure;
            Object owner = closure.getOwner();
            Class ownerClass = (Class) (owner instanceof Class ? owner : owner.getClass());
            for (CachedMethod method : ReflectionCache.getCachedClass(ownerClass).getMethods() ) {
                if (method.getName().equals(methodClosure.getMethod())) {
                    res.add(new MethodClosureMetaMethod(name, declaringClass, closure, method));
                }
            }
        }
        else {
            if (closure instanceof GeneratedClosure) {
                for (CachedMethod method : ReflectionCache.getCachedClass(closure.getClass()).getMethods() ) {
                    if (method.getName().equals("doCall")) {
                        res.add(new ClosureMetaMethod(name, declaringClass, closure, method));
                    }
                }
            }
            else {
                res.add(new MetaMethod(closure.getParameterTypes()){
                    public int getModifiers() {
                        return Modifier.PUBLIC;
                    }

                    public String getName() {
                        return name;
                    }

                    public Class getReturnType() {
                        return Object.class;
                    }

                    public CachedClass getDeclaringClass() {
                        return ReflectionCache.getCachedClass(declaringClass);
                    }

                    public Object invoke(Object object, Object[] arguments) {
                        Closure cloned = (Closure) closure.clone();
                        cloned.setDelegate(object);
                        arguments = coerceArgumentsToClasses(arguments);
                        return InvokerHelper.invokeMethod(cloned, "call", arguments);
                    }
                });
            }
        }
        return res;
    }

    public CachedMethod getDoCall() {
        return doCall;
    }

    public static ClosureMetaMethod copy(ClosureMetaMethod closureMethod) {
        if (closureMethod instanceof MethodClosureMetaMethod)
          return new MethodClosureMetaMethod(closureMethod.getName(), closureMethod.getDeclaringClass().getTheClass(), closureMethod.getClosure(), closureMethod.getDoCall());
        else
          return new ClosureMetaMethod(closureMethod.getName(), closureMethod.getDeclaringClass().getTheClass(), closureMethod.getClosure(), closureMethod.getDoCall());
    }

    private static class MethodClosureMetaMethod extends ClosureMetaMethod {
        public MethodClosureMetaMethod(String name, Class declaringClass, Closure closure, CachedMethod method) {
            super(name, declaringClass, closure, method);
        }

        public Object invoke(Object object, Object[] arguments) {
            return getDoCall().invoke(getClosure().getOwner(), arguments);
        }
    }
}
