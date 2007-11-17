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

package org.codehaus.groovy.tools.javac;

import groovy.lang.GroovyClassLoader;

import java.io.File;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.messages.ExceptionMessage;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

public class JavacJavaCompiler implements JavaCompiler {
    private CompilerConfiguration config;

    public JavacJavaCompiler(CompilerConfiguration config) {
        this.config = config;
    }

    public void compile(List files, CompilationUnit cu) {
        String[] javacParameters = makeParameters(files);
        org.apache.tools.ant.taskdefs.Javac c;
        try {
            Class javac = findJavac(cu);
            Method method = javac.getMethod("compile", new Class[]{String[].class});
            method.invoke(null, new Object[]{javacParameters});
        } catch (Exception e) {
            cu.getErrorCollector().addFatalError(new ExceptionMessage(e, true, cu));
        }
    }

    private String[] makeParameters(List files) {
        Map options = config.getJointCompilationOptions();
        LinkedList paras = new LinkedList();

        File target = config.getTargetDirectory();
        if (target == null) target = new File(".");

        // defaults
        paras.add("-d");
        paras.add(target.getAbsolutePath());
        paras.add("-sourcepath");
        paras.add(((File) options.get("stubDir")).getAbsolutePath());
        
        // add flags
        String[] flags = (String[]) options.get("flags");
        if (flags != null) {
            for (int i = 0; i < flags.length; i++) {
                paras.add('-' + flags[i]);
            }
        }

        boolean hadClasspath=false;
        // add namedValues
        String[] namedValues = (String[]) options.get("namedValues");
        if (namedValues != null) {
            for (int i = 0; i < namedValues.length; i += 2) {
                String name = namedValues[i];
                if (name.equals("classpath")) hadClasspath = true;
                paras.add('-' + name);
                paras.add(namedValues[i + 1]);
            }
        }
        
        // append classpath if not already defined
        if (!hadClasspath) {
            paras.add("-classpath");
            List classpath = config.getClasspath();
            String resultPath = DefaultGroovyMethods.join(classpath, File.pathSeparator);
            paras.add(resultPath);
        }
        
        // files to compile
        paras.addAll(files);

        return (String[]) paras.toArray(new String[paras.size()]);
    }

    private Class findJavac(CompilationUnit cu) throws ClassNotFoundException {
        String main = "com.sun.tools.javac.Main";
        try {
            return Class.forName(main);
        } catch (ClassNotFoundException e) {}
            
        try {
            ClassLoader cl = this.getClass().getClassLoader();
            return cl.loadClass(main);
        } catch (ClassNotFoundException e) {}
        
        try {
            return ClassLoader.getSystemClassLoader().loadClass(main);
        } catch (ClassNotFoundException e) {}
        
        try {
            return cu.getClassLoader().getParent().loadClass(main);
        } catch (ClassNotFoundException e3) {}
        
        
        // couldn't find compiler - try to find tools.jar
        // based on java.home setting
        String javaHome = System.getProperty("java.home");
        if (javaHome.toLowerCase(Locale.US).endsWith("jre")) {
            javaHome = javaHome.substring(0, javaHome.length() - 4);
        }
        File toolsJar = new File((javaHome + "/lib/tools.jar"));
        if (toolsJar.exists()) {
            GroovyClassLoader loader = cu.getClassLoader();
            loader.addClasspath(toolsJar.getAbsolutePath());
            return loader.loadClass(main);
        }
        
        throw new ClassNotFoundException("unable to locate the java compiler com.sun.tools.javac.Main, please change your classloader settings");
    }
}
