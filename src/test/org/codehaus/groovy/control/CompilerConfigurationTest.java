
/*
 * $Id:$
 *
 * Copyright (c) 2007 James P. White
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package org.codehaus.groovy.control;

import groovy.util.GroovyTestCase;
import org.codehaus.groovy.control.messages.WarningMessage;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Make sure CompilerConfiguration works.
 */
public class CompilerConfigurationTest extends GroovyTestCase {
    Properties savedProperties;

    // Use setUp/tearDown to avoid mucking with system properties for other tests...

    public void setUp() {
        savedProperties = System.getProperties();
        System.setProperties(new Properties(savedProperties));
    }

    public void tearDown() {
        System.setProperties(savedProperties);
    }

    public void testDefaultConstructor() {
        final CompilerConfiguration config = CompilerConfiguration.DEFAULT;

        assertEquals(WarningMessage.LIKELY_ERRORS, config.getWarningLevel());
        assertEquals(Boolean.getBoolean("groovy.output.debug"), config.getDebug());
        assertEquals(Boolean.getBoolean("groovy.output.verbose"), config.getVerbose());
        assertEquals(false, config.getDebug());
        assertEquals(false, config.getVerbose());
        assertEquals(10, config.getTolerance());
        assertEquals(100, config.getMinimumRecompilationInterval());
        assertNull(config.getScriptBaseClass());
        assertEquals(getSystemEncoding(), config.getSourceEncoding());
        assertEquals(getVMVersion(), config.getTargetBytecode());
        assertEquals(false, config.getRecompileGroovySource());
        {
            final List listCP = config.getClasspath();
            assertNotNull(listCP);
            assertEquals(0, listCP.size());
        }
        assertNotNull(config.getOutput());
        assertNull(config.getTargetDirectory());
        assertEquals(".groovy", config.getDefaultScriptExtension());
        assertNull(config.getJointCompilationOptions());
        assertNotNull(config.getPluginFactory());
    }

    private String getSystemEncoding() {
        return System.getProperty("file.encoding", "US-ASCII");
    }

    private static String getVMVersion() {
        try {
            Class.forName("java.lang.annotation.Annotation");
            return CompilerConfiguration.POST_JDK5;
        }
        catch(Exception ex) {
            // IGNORE
        }

        return CompilerConfiguration.PRE_JDK5;
    }

    public void testSetViaSystemProperties() {
        System.setProperty("groovy.warnings", "PaRaNoiA");
        System.setProperty("groovy.output.verbose", "trUE");
        System.setProperty("groovy.recompile.minimumInterval", "867892345");

        assertEquals("PaRaNoiA", System.getProperty("groovy.warnings"));

        final CompilerConfiguration config = new CompilerConfiguration(System.getProperties());

        assertEquals(WarningMessage.PARANOIA, config.getWarningLevel());
        assertEquals(false, config.getDebug());
        assertEquals(true, config.getVerbose());
        assertEquals(10, config.getTolerance());
        assertEquals(867892345, config.getMinimumRecompilationInterval());
        assertNull(config.getScriptBaseClass());
        assertEquals(getSystemEncoding(), config.getSourceEncoding());
        assertEquals(getVMVersion(), config.getTargetBytecode());
        assertEquals(false, config.getRecompileGroovySource());
        {
            final List listCP = config.getClasspath();
            assertNotNull(listCP);
            assertEquals(0, listCP.size());
        }
        assertNotNull(config.getOutput());
        assertNull(config.getTargetDirectory());
        assertEquals(".groovy", config.getDefaultScriptExtension());
        assertNull(config.getJointCompilationOptions());
        assertNotNull(config.getPluginFactory());
    }

    public void testCopyConstructor1() {
        final CompilerConfiguration init = new CompilerConfiguration();

        init.setWarningLevel(WarningMessage.POSSIBLE_ERRORS);
        init.setDebug(true);
        init.setVerbose(false);
        init.setTolerance(720);
        init.setMinimumRecompilationInterval(234);
        init.setScriptBaseClass("blarg.foo.WhatSit");
        init.setSourceEncoding("LEAD-123");
        init.setTargetBytecode(CompilerConfiguration.POST_JDK5);
        init.setRecompileGroovySource(true);
        init.setClasspath("File1" + File.pathSeparator + "Somewhere");

        final PrintWriter initOut = new PrintWriter(System.out);
        init.setOutput(initOut);

        final File initTDFile = new File("A wandering path");
        init.setTargetDirectory(initTDFile);
        init.setDefaultScriptExtension(".jpp");

        final Map initJoint = new HashMap();
        initJoint.put("somekey", "somevalue");
        init.setJointCompilationOptions(initJoint);

        final ParserPluginFactory initPPF = ParserPluginFactory.newInstance(true);
        init.setPluginFactory(initPPF);

        assertEquals(WarningMessage.POSSIBLE_ERRORS, init.getWarningLevel());
        assertEquals(true, init.getDebug());
        assertEquals(false, init.getVerbose());
        assertEquals(720, init.getTolerance());
        assertEquals(234, init.getMinimumRecompilationInterval());
        assertEquals("blarg.foo.WhatSit", init.getScriptBaseClass());
        assertEquals("LEAD-123", init.getSourceEncoding());
        assertEquals(CompilerConfiguration.POST_JDK5, init.getTargetBytecode());
        assertEquals(true, init.getRecompileGroovySource());
        {
            final List listCP = init.getClasspath();
            assertEquals("File1", listCP.get(0));
            assertEquals("Somewhere", listCP.get(1));
        }
        assertEquals(initOut, init.getOutput());
        assertEquals(initTDFile, init.getTargetDirectory());
        assertEquals(".jpp", init.getDefaultScriptExtension());
        assertEquals(initJoint, init.getJointCompilationOptions());
        assertEquals(initPPF, init.getPluginFactory());

        final CompilerConfiguration config = new CompilerConfiguration(init);

        assertEquals(WarningMessage.POSSIBLE_ERRORS, config.getWarningLevel());
        assertEquals(true, config.getDebug());
        assertEquals(false, config.getVerbose());
        assertEquals(720, config.getTolerance());
        assertEquals(234, config.getMinimumRecompilationInterval());
        assertEquals("blarg.foo.WhatSit", config.getScriptBaseClass());
        assertEquals("LEAD-123", config.getSourceEncoding());
        assertEquals(CompilerConfiguration.POST_JDK5, config.getTargetBytecode());
        assertEquals(true, config.getRecompileGroovySource());
        {
            final List listCP = config.getClasspath();
            assertEquals("File1", listCP.get(0));
            assertEquals("Somewhere", listCP.get(1));
        }
        assertEquals(initOut, config.getOutput());
        assertEquals(initTDFile, config.getTargetDirectory());
        assertEquals(".jpp", config.getDefaultScriptExtension());
        assertEquals(initJoint, config.getJointCompilationOptions());
        assertEquals(initPPF, config.getPluginFactory());

    }

    public void testCopyConstructor2() {
        final CompilerConfiguration init = new CompilerConfiguration();

        init.setWarningLevel(WarningMessage.POSSIBLE_ERRORS);
        init.setDebug(false);
        init.setVerbose(true);
        init.setTolerance(55);
        init.setMinimumRecompilationInterval(975);
        init.setScriptBaseClass("");
        init.setSourceEncoding("Gutenberg");
        init.setTargetBytecode(CompilerConfiguration.PRE_JDK5);
        init.setRecompileGroovySource(false);
        init.setClasspath("");

        final PrintWriter initOut = new PrintWriter(System.out);
        init.setOutput(initOut);

        final File initTDFile = new File("A wandering path");
        init.setTargetDirectory(initTDFile);

        assertEquals(WarningMessage.POSSIBLE_ERRORS, init.getWarningLevel());
        assertEquals(false, init.getDebug());
        assertEquals(true, init.getVerbose());
        assertEquals(55, init.getTolerance());
        assertEquals(975, init.getMinimumRecompilationInterval());
        assertEquals("", init.getScriptBaseClass());
        assertEquals("Gutenberg", init.getSourceEncoding());
        assertEquals(CompilerConfiguration.PRE_JDK5, init.getTargetBytecode());
        assertEquals(false, init.getRecompileGroovySource());
        {
            final List listCP = init.getClasspath();
            assertNotNull(listCP);
            assertEquals(0, listCP.size());
        }
        assertEquals(initOut, init.getOutput());
        assertEquals(initTDFile, init.getTargetDirectory());

        final CompilerConfiguration config = new CompilerConfiguration(init);

        assertEquals(WarningMessage.POSSIBLE_ERRORS, config.getWarningLevel());
        assertEquals(false, config.getDebug());
        assertEquals(true, config.getVerbose());
        assertEquals(55, config.getTolerance());
        assertEquals(975, config.getMinimumRecompilationInterval());
        assertEquals("", config.getScriptBaseClass());
        assertEquals("Gutenberg", config.getSourceEncoding());
        assertEquals(CompilerConfiguration.PRE_JDK5, config.getTargetBytecode());
        assertEquals(false, config.getRecompileGroovySource());
        {
            final List listCP = config.getClasspath();
            assertEquals(0, listCP.size());
        }
        assertEquals(initOut, config.getOutput());
        assertEquals(initTDFile, config.getTargetDirectory());
    }
}