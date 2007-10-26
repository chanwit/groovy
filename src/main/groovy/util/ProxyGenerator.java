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
package groovy.util;

import groovy.lang.Binding;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.typehandling.GroovyCastException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Classes to generate 'Proxy' objects which implement interfaces
 * and/or extend classes.
 *
 * @author Paul King
 * @author Guillaume Laforge
 */
public class ProxyGenerator {

    public static Object instantiateAggregateFromBaseClass(Map map, Class clazz) {
        return instantiateAggregate(map, null, clazz);
    }

    public static Object instantiateAggregateFromInterface(Class clazz) {
        return instantiateAggregateFromInterface(null, clazz);
    }

    public static Object instantiateAggregateFromInterface(Map map, Class clazz) {
        List interfaces = new ArrayList();
        interfaces.add(clazz);
        return instantiateAggregate(map, interfaces, null);
    }

    public static Object instantiateAggregate(Map closureMap, List interfaces, Class clazz) {
        Map map = new HashMap();
        if (closureMap != null) {
            map = closureMap;
        }
        List interfacesToImplement = new ArrayList();
        if (interfaces != null) {
            interfacesToImplement = interfaces;
        }
        Class baseClass = GroovyObjectSupport.class;
        if (clazz != null) {
            baseClass = clazz;
        }
        String name = shortName(baseClass.getName()) + "_groovyProxy";
        StringBuffer buffer = new StringBuffer();

        // add class header with constructor
        buffer.append("class ").append(name);
        if (clazz != null) {
            buffer.append(" extends ").append(baseClass.getName());
        }
        for (int i = 0; i < interfacesToImplement.size(); i++) {
            Class thisInterface = (Class) interfacesToImplement.get(i);
            if (i == 0) {
                buffer.append(" implements ");
            } else {
                buffer.append(", ");
            }
            buffer.append(thisInterface.getName());
        }
        buffer.append(" {\n")
                .append("    private closureMap\n    ")
                .append(name).append("(map) {\n")
                .append("        super()\n")
                .append("        this.closureMap = map\n")
                .append("    }\n");

        // add overwriting methods
        List selectedMethods = new ArrayList();
        List publicAndProtectedMethods = DefaultGroovyMethods.toList(baseClass.getMethods());
        publicAndProtectedMethods.addAll(getInheritedMethods(baseClass));
        for (int i = 0; i < publicAndProtectedMethods.size(); i++) {
            Method method = (Method) publicAndProtectedMethods.get(i);
            if (map.containsKey(method.getName())) {
                selectedMethods.add(method.getName());
                addOverridingMapCall(buffer, method);
            }
        }

        // add interface methods
        List interfaceMethods = new ArrayList();
        for (int i = 0; i < interfacesToImplement.size(); i++) {
            Class thisInterface = (Class) interfacesToImplement.get(i);
            interfaceMethods.addAll(DefaultGroovyMethods.toList(thisInterface.getMethods()));
            interfaceMethods.addAll(getInheritedMethods(thisInterface));
        }
        for (int i = 0; i < interfaceMethods.size(); i++) {
            Method method = (Method) interfaceMethods.get(i);
            if (!containsEquivalentMethod(publicAndProtectedMethods, method)) {
                selectedMethods.add(method.getName());
                addMapOrDummyCall(map, buffer, method);
            }
        }

        // add leftover methods from the map
        for (Iterator iterator = map.keySet().iterator(); iterator.hasNext();) {
            String methodName = (String) iterator.next();
            if (selectedMethods.contains(methodName)) continue;
            addNewMapCall(buffer, methodName);
        }

        // end class

        buffer.append("}\n").append("new ").append(name).append("(map)");

        Binding binding = new Binding();
        binding.setVariable("map", map);
        ClassLoader cl = baseClass.getClassLoader();
        if (clazz == null && interfacesToImplement.size() > 0) {
            Class c = (Class) interfacesToImplement.get(0);
            cl = c.getClassLoader();
        }
        GroovyShell shell = new GroovyShell(cl, binding);
        try {
            return shell.evaluate(buffer.toString());
        } catch (MultipleCompilationErrorsException err) {
            throw new GroovyCastException(map, baseClass);
        }
    }

    private static boolean containsEquivalentMethod(List publicAndProtectedMethods, Method candidate) {
        for (int i = 0; i < publicAndProtectedMethods.size(); i++) {
            Method method = (Method) publicAndProtectedMethods.get(i);
            if (candidate.getName().equals(method.getName()) &&
                    candidate.getParameterTypes().length == method.getParameterTypes().length &&
                    candidate.getReturnType().equals(method.getReturnType())) {
                return true;
            }
        }
        return false;
    }

    private static List getInheritedMethods(Class baseClass) {
        List protectedMethodList = new ArrayList();
        Class currentClass = baseClass;
        while (currentClass != null) {
            Method[] protectedMethods = currentClass.getDeclaredMethods();
            for (int i = 0; i < protectedMethods.length; i++) {
                Method method = protectedMethods[i];
                if (Modifier.isProtected(method.getModifiers()))
                    protectedMethodList.add(method);
            }
            currentClass = currentClass.getSuperclass();
        }
        return protectedMethodList;
    }

    private static void addNewMapCall(StringBuffer buffer, String methodName) {
        buffer.append("    def ").append(methodName).append("(Object[] args) { \n")
                .append("        this.@closureMap['").append(methodName).append("'] (*args)\n    }\n");
    }

    private static void addOverridingMapCall(StringBuffer buffer, Method method) {
        Class[] parameterTypes = addMethodPrefix(buffer, method);
        addMethodBody(buffer, method, parameterTypes);
        addMethodSuffix(buffer);
    }

    private static void addMapOrDummyCall(Map map, StringBuffer buffer, Method method) {
        Class[] parameterTypes = addMethodPrefix(buffer, method);
        if (map.containsKey(method.getName())) {
            addMethodBody(buffer, method, parameterTypes);
        }
        addMethodSuffix(buffer);
    }

    private static Class[] addMethodPrefix(StringBuffer buffer, Method method) {
        buffer.append("    ").append(getSimpleName(method.getReturnType()))
                .append(" ").append(method.getName()).append("(");
        Class[] parameterTypes = method.getParameterTypes();
        for (int parameterTypeIndex = 0; parameterTypeIndex < parameterTypes.length; parameterTypeIndex++) {
            Class parameter = parameterTypes[parameterTypeIndex];
            if (parameterTypeIndex != 0) {
                buffer.append(", ");
            }
            buffer.append(getSimpleName(parameter)).append(" ")
                    .append("p").append(parameterTypeIndex);
        }
        buffer.append(") { ");
        return parameterTypes;
    }

    private static void addMethodBody(StringBuffer buffer, Method method, Class[] parameterTypes) {
        buffer.append("this.@closureMap['").append(method.getName()).append("'] (");
        for (int j = 0; j < parameterTypes.length; j++) {
            if (j != 0) {
                buffer.append(", ");
            }
            buffer.append("p").append(j);
        }
        buffer.append(")");
    }

    private static void addMethodSuffix(StringBuffer buffer) {
        buffer.append(" }\n");
    }

    /**
     * TODO once we switch to Java 1.5 bt default, use Class#getSimpleName() directly
     *
     * @param c the class of which we want the readable simple name
     * @return the readable simple name
     */
    private static String getSimpleName(Class c) {
        if (c.isArray()) {
            int dimension = 0;
            Class componentClass = c;
            while (componentClass.isArray()) {
                componentClass = componentClass.getComponentType();
                dimension++;
            }
            return componentClass.getName().replaceAll("\\$", "\\.") +
                    DefaultGroovyMethods.multiply("[]", new Integer(dimension));
        } else {
            return c.getName().replaceAll("\\$", "\\.");
        }
    }

    public static String shortName(String name) {
        int index = name.lastIndexOf('.');
        if (index == -1) return name;
        return name.substring(index + 1, name.length());
    }

}