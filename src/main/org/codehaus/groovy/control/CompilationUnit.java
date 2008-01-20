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


package org.codehaus.groovy.control;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyRuntimeException;
import org.codehaus.groovy.GroovyBugError;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.CompileUnit;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.classgen.*;
import org.codehaus.groovy.control.io.InputStreamReaderSource;
import org.codehaus.groovy.control.io.ReaderSource;
import org.codehaus.groovy.control.messages.ExceptionMessage;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.control.messages.SimpleMessage;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.tools.GroovyClass;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.util.*;

/**
 * Collects all compilation data as it is generated by the compiler system.
 * Allows additional source units to be added and compilation run again (to
 * affect only the deltas).
 *
 * @author <a href="mailto:cpoirier@dreaming.org">Chris Poirier</a>
 * @author <a href="mailto:blackdrag@gmx.org">Jochen Theodorou</a>
 * @version $Id$
 */

public class CompilationUnit extends ProcessingUnit {

    //---------------------------------------------------------------------------
    // CONSTRUCTION AND SUCH


    protected Map sources;    // The SourceUnits from which this unit is built
    protected Map summariesBySourceName;      // Summary of each SourceUnit
    protected Map summariesByPublicClassName;       // Summary of each SourceUnit
    protected Map classSourcesByPublicClassName;    // Summary of each Class
    protected List names;      // Names for each SourceUnit in sources.
    protected LinkedList queuedSources;

    protected CompileUnit ast;        // The overall AST for this CompilationUnit.
    protected List generatedClasses;    // The classes generated during classgen.

    protected Verifier verifier;   // For use by verify().

    protected boolean debug;      // Controls behaviour of classgen() and other routines.
    protected boolean configured; // Set true after the first configure() operation

    protected ClassgenCallback classgenCallback;  // A callback for use during classgen()
    protected ProgressCallback progressCallback;  // A callback for use during compile()
    protected ResolveVisitor resolveVisitor;
    protected StaticImportVisitor staticImportVisitor;
    protected OptimizerVisitor optimizer;

    LinkedList[] phaseOperations;


    /**
     * Initializes the CompilationUnit with defaults.
     */
    public CompilationUnit() {
        this(null, null, null);
    }


    /**
     * Initializes the CompilationUnit with defaults except for class loader.
     */
    public CompilationUnit(GroovyClassLoader loader) {
        this(null, null, loader);
    }


    /**
     * Initializes the CompilationUnit with no security considerations.
     */
    public CompilationUnit(CompilerConfiguration configuration) {
        this(configuration, null, null);
    }

    /**
     * Initializes the CompilationUnit with a CodeSource for controlling
     * security stuff and a class loader for loading classes.
     */
    public CompilationUnit(CompilerConfiguration configuration, CodeSource security, GroovyClassLoader loader) {
        super(configuration, loader, null);
        this.names = new ArrayList();
        this.queuedSources = new LinkedList();
        this.sources = new HashMap();
        this.summariesBySourceName = new HashMap();
        this.summariesByPublicClassName = new HashMap();
        this.classSourcesByPublicClassName = new HashMap();

        this.ast = new CompileUnit(this.classLoader, security, this.configuration);
        this.generatedClasses = new ArrayList();


        this.verifier = new Verifier();
        this.resolveVisitor = new ResolveVisitor(this);
        this.staticImportVisitor = new StaticImportVisitor(this);
        this.optimizer = new OptimizerVisitor(this);

        phaseOperations = new LinkedList[Phases.ALL + 1];
        for (int i = 0; i < phaseOperations.length; i++) {
            phaseOperations[i] = new LinkedList();
        }
        addPhaseOperation(new SourceUnitOperation() {
            public void call(SourceUnit source) throws CompilationFailedException {
                source.parse();
            }
        }, Phases.PARSING);
        addPhaseOperation(convert, Phases.CONVERSION);
        addPhaseOperation(new PrimaryClassNodeOperation() {
            public void call(SourceUnit source, GeneratorContext context,
                             ClassNode classNode) throws CompilationFailedException {
                EnumVisitor ev = new EnumVisitor(CompilationUnit.this, source);
                ev.visitClass(classNode);
            }
        }, Phases.CONVERSION);
        addPhaseOperation(resolve, Phases.SEMANTIC_ANALYSIS);
        addPhaseOperation(staticImport, Phases.SEMANTIC_ANALYSIS);
        addPhaseOperation(compileCompleteCheck, Phases.CANONICALIZATION);
        addPhaseOperation(classgen, Phases.CLASS_GENERATION);
        addPhaseOperation(output);

        this.classgenCallback = null;
    }

    public void addPhaseOperation(SourceUnitOperation op, int phase) {
        if (phase < 0 || phase > Phases.ALL) throw new IllegalArgumentException("phase " + phase + " is unknown");
        phaseOperations[phase].add(op);
    }

    public void addPhaseOperation(PrimaryClassNodeOperation op, int phase) {
        if (phase < 0 || phase > Phases.ALL) throw new IllegalArgumentException("phase " + phase + " is unknown");
        phaseOperations[phase].add(op);
    }

    public void addPhaseOperation(GroovyClassOperation op) {
        phaseOperations[Phases.OUTPUT].addFirst(op);
    }


    /**
     * Configures its debugging mode and classloader classpath from a given compiler configuration.
     * This cannot be done more than once due to limitations in {@link java.net.URLClassLoader URLClassLoader}.
     */
    public void configure(CompilerConfiguration configuration) {
        super.configure(configuration);
        this.debug = configuration.getDebug();

        if (!this.configured && this.classLoader instanceof GroovyClassLoader) {
            appendCompilerConfigurationClasspathToClassLoader(configuration, (GroovyClassLoader) this.classLoader);
        }

        this.configured = true;
    }

    private void appendCompilerConfigurationClasspathToClassLoader(CompilerConfiguration configuration, GroovyClassLoader classLoader) {
        /*for (Iterator iterator = configuration.getClasspath().iterator(); iterator.hasNext(); ) {
            classLoader.addClasspath((String) iterator.next());
        }*/
    }

    /**
     * Returns the CompileUnit that roots our AST.
     */
    public CompileUnit getAST() {
        return this.ast;
    }

    /**
     * Get the source summaries
     */
    public Map getSummariesBySourceName() {
        return summariesBySourceName;
    }

    public Map getSummariesByPublicClassName() {
        return summariesByPublicClassName;
    }

    public Map getClassSourcesByPublicClassName() {
        return classSourcesByPublicClassName;
    }

    public boolean isPublicClass(String className) {
        return summariesByPublicClassName.containsKey(className);
    }


    /**
     * Get the GroovyClasses generated by compile().
     */
    public List getClasses() {
        return generatedClasses;
    }


    /**
     * Convenience routine to get the first ClassNode, for
     * when you are sure there is only one.
     */
    public ClassNode getFirstClassNode() {
        return (ClassNode) ((ModuleNode) this.ast.getModules().get(0)).getClasses().get(0);
    }


    /**
     * Convenience routine to get the named ClassNode.
     */
    public ClassNode getClassNode(final String name) {
        final ClassNode[] result = new ClassNode[]{null};
        PrimaryClassNodeOperation handler = new PrimaryClassNodeOperation() {
            public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) {
                if (classNode.getName().equals(name)) {
                    result[0] = classNode;
                }
            }
        };

        try {
            applyToPrimaryClassNodes(handler);
        } catch (CompilationFailedException e) {
            if (debug) e.printStackTrace();
        }
        return result[0];
    }

    //---------------------------------------------------------------------------
    // SOURCE CREATION


    /**
     * Adds a set of file paths to the unit.
     */
    public void addSources(String[] paths) {
        for (int i = 0; i < paths.length; i++) {
            File file = new File(paths[i]);
            addSource(file);
        }
    }


    /**
     * Adds a set of source files to the unit.
     */
    public void addSources(File[] files) {
        for (int i = 0; i < files.length; i++) {
            addSource(files[i]);
        }
    }


    /**
     * Adds a source file to the unit.
     */
    public SourceUnit addSource(File file) {
        return addSource(new SourceUnit(file, configuration, classLoader, getErrorCollector()));
    }

    /**
     * Adds a source file to the unit.
     */
    public SourceUnit addSource(URL url) {
        return addSource(new SourceUnit(url, configuration, classLoader, getErrorCollector()));
    }


    /**
     * Adds a InputStream source to the unit.
     */
    public SourceUnit addSource(String name, InputStream stream) {
        ReaderSource source = new InputStreamReaderSource(stream, configuration);
        return addSource(new SourceUnit(name, source, configuration, classLoader, getErrorCollector()));
    }


    /**
     * Adds a SourceUnit to the unit.
     */
    public SourceUnit addSource(SourceUnit source) {
        String name = source.getName();
        source.setClassLoader(this.classLoader);
        for (Iterator iter = queuedSources.iterator(); iter.hasNext();) {
            SourceUnit su = (SourceUnit) iter.next();
            if (name.equals(su.getName())) return su;
        }
        queuedSources.add(source);
        return source;
    }


    /**
     * Returns an iterator on the unit's SourceUnits.
     */
    public Iterator iterator() {
        return new Iterator() {
            Iterator nameIterator = names.iterator();


            public boolean hasNext() {
                return nameIterator.hasNext();
            }


            public Object next() {
                String name = (String) nameIterator.next();
                return sources.get(name);
            }


            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }


    /**
     * Adds a ClassNode directly to the unit (ie. without source).
     * WARNING: the source is needed for error reporting, using
     * this method without setting a SourceUnit will cause
     * NullPinterExceptions
     */
    public void addClassNode(ClassNode node) {
        ModuleNode module = new ModuleNode(this.ast);
        this.ast.addModule(module);
        module.addClass(node);
    }

    //---------------------------------------------------------------------------
    // EXTERNAL CALLBACKS


    /**
     * A callback interface you can use to "accompany" the classgen()
     * code as it traverses the ClassNode tree.  You will be called-back
     * for each primary and inner class.  Use setClassgenCallback() before
     * running compile() to set your callback.
     */
    public abstract static class ClassgenCallback {
        public abstract void call(ClassVisitor writer, ClassNode node) throws CompilationFailedException;
    }


    /**
     * Sets a ClassgenCallback.  You can have only one, and setting
     * it to null removes any existing setting.
     */
    public void setClassgenCallback(ClassgenCallback visitor) {
        this.classgenCallback = visitor;
    }


    /**
     * A callback interface you can use to get a callback after every
     * unit of the compile process.  You will be called-back with a
     * ProcessingUnit and a phase indicator.  Use setProgressCallback()
     * before running compile() to set your callback.
     */
    public abstract static class ProgressCallback {

        public abstract void call(ProcessingUnit context, int phase) throws CompilationFailedException;
    }

    /**
     * Sets a ProgressCallback.  You can have only one, and setting
     * it to null removes any existing setting.
     */
    public void setProgressCallback(ProgressCallback callback) {
        this.progressCallback = callback;
    }

    //---------------------------------------------------------------------------
    // ACTIONS


    /**
     * Synonym for compile(Phases.ALL).
     */
    public void compile() throws CompilationFailedException {
        compile(Phases.ALL);
    }

    /**
     * Compiles the compilation unit from sources.
     */
    public void compile(int throughPhase) throws CompilationFailedException {
        //
        // To support delta compilations, we always restart
        // the compiler.  The individual passes are responsible
        // for not reprocessing old code.
        gotoPhase(Phases.INITIALIZATION);
        throughPhase = Math.min(throughPhase, Phases.ALL);

        while (throughPhase >= phase && phase <= Phases.ALL) {

            for (Iterator it = phaseOperations[phase].iterator(); it.hasNext();) {
                Object operation = it.next();
                if (operation instanceof PrimaryClassNodeOperation) {
                    applyToPrimaryClassNodes((PrimaryClassNodeOperation) operation);
                } else if (operation instanceof SourceUnitOperation) {
                    applyToSourceUnits((SourceUnitOperation) operation);
                } else {
                    applyToGeneratedGroovyClasses((GroovyClassOperation) operation);
                }
            }

            if (progressCallback != null) progressCallback.call(this, phase);
            completePhase();
            applyToSourceUnits(mark);

            if (dequeued()) continue;

            gotoPhase(phase + 1);

            if (phase == Phases.CLASS_GENERATION) {
                sortClasses();
            }
        }

        errorCollector.failIfErrors();
    }

    private void sortClasses() throws CompilationFailedException {
        Iterator modules = this.ast.getModules().iterator();
        while (modules.hasNext()) {
            ModuleNode module = (ModuleNode) modules.next();

            // before we actually do the sorting we should check
            // for cyclic references
            List classes = module.getClasses();
            for (Iterator iter = classes.iterator(); iter.hasNext();) {
                ClassNode start = (ClassNode) iter.next();
                ClassNode cn = start;
                Set parents = new HashSet();
                do {
                    if (parents.contains(cn.getName())) {
                        getErrorCollector().addErrorAndContinue(
                                new SimpleMessage("cyclic inheritance involving " + cn.getName() + " in class " + start.getName(), this)
                        );
                        cn = null;
                    } else {
                        parents.add(cn.getName());
                        cn = cn.getSuperClass();
                    }
                } while (cn != null);
            }
            errorCollector.failIfErrors();
            module.sortClasses();

        }
    }


    /**
     * Dequeues any source units add through addSource and resets the compiler phase
     * to initialization.
     * <p/>
     * Note: this does not mean a file is recompiled. If a SoucreUnit has already passed
     * a phase it is skipped until a higher phase is reached.
     *
     * @return true if there was a queued source
     * @throws CompilationFailedException
     */
    protected boolean dequeued() throws CompilationFailedException {
        boolean dequeue = !queuedSources.isEmpty();
        while (!queuedSources.isEmpty()) {
            SourceUnit su = (SourceUnit) queuedSources.removeFirst();
            String name = su.getName();
            names.add(name);
            sources.put(name, su);
        }
        if (dequeue) {
            gotoPhase(Phases.INITIALIZATION);
        }
        return dequeue;
    }

    /**
     * Resolves all types
     */
    private final SourceUnitOperation resolve = new SourceUnitOperation() {
        public void call(SourceUnit source) throws CompilationFailedException {
            List classes = source.ast.getClasses();
            for (Iterator it = classes.iterator(); it.hasNext();) {
                ClassNode node = (ClassNode) it.next();

                VariableScopeVisitor scopeVisitor = new VariableScopeVisitor(source);
                scopeVisitor.visitClass(node);

                resolveVisitor.startResolving(node, source);

                GenericsVisitor genericsVisitor = new GenericsVisitor(source);
                genericsVisitor.visitClass(node);
            }

        }
    };

    private PrimaryClassNodeOperation staticImport = new PrimaryClassNodeOperation() {
        public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
            staticImportVisitor.visitClass(classNode, source);
            optimizer.visitClass(classNode, source);
        }
    };

    /**
     * Runs convert() on a single SourceUnit.
     */
    private SourceUnitOperation convert = new SourceUnitOperation() {
        public void call(SourceUnit source) throws CompilationFailedException {
            source.convert();
            CompilationUnit.this.ast.addModule(source.getAST());


            if (CompilationUnit.this.progressCallback != null) {
                CompilationUnit.this.progressCallback.call(source, CompilationUnit.this.phase);
            }
        }
    };

    private GroovyClassOperation output = new GroovyClassOperation() {
        public void call(GroovyClass gclass) throws CompilationFailedException {
            boolean failures = false;
            String name = gclass.getName().replace('.', File.separatorChar) + ".class";
            File path = new File(configuration.getTargetDirectory(), name);

            //
            // Ensure the path is ready for the file
            //
            File directory = path.getParentFile();
            if (directory != null && !directory.exists()) {
                directory.mkdirs();
            }

            //
            // Create the file and write out the data
            //
            byte[] bytes = gclass.getBytes();

            FileOutputStream stream = null;
            try {
                stream = new FileOutputStream(path);
                stream.write(bytes, 0, bytes.length);
            } catch (IOException e) {
                getErrorCollector().addError(Message.create(e.getMessage(), CompilationUnit.this));
                failures = true;
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        }
    };

    /* checks if all needed classes are compiled before generating the bytecode */
    private SourceUnitOperation compileCompleteCheck = new SourceUnitOperation() {
        public void call(SourceUnit source) throws CompilationFailedException {
            List classes = source.ast.getClasses();
            for (Iterator it = classes.iterator(); it.hasNext();) {
                ClassNode node = (ClassNode) it.next();
                CompileUnit cu = node.getCompileUnit();
                for (Iterator iter = cu.iterateClassNodeToCompile(); iter.hasNext();) {
                    String name = (String) iter.next();
                    SourceUnit su = ast.getScriptSourceLocation(name);
                    List classesInSourceUnit = su.ast.getClasses();
                    StringBuffer message = new StringBuffer();
                    message
                            .append("Compilation incomplete: expected to find the class ")
                            .append(name)
                            .append(" in ")
                            .append(su.getName());
                    if (classesInSourceUnit.isEmpty()) {
                        message.append(", but the file seems not to contain any classes");
                    } else {
                        message.append(", but the file contains the classes: ");
                        boolean first = true;
                        for (Iterator suClassesIter = classesInSourceUnit
                                .iterator(); suClassesIter.hasNext();) {
                            ClassNode cn = (ClassNode) suClassesIter.next();
                            if (!first) {
                                message.append(", ");
                            } else {
                                first = false;
                            }
                            message.append(cn.getName());
                        }
                    }

                    getErrorCollector().addErrorAndContinue(
                            new SimpleMessage(message.toString(), CompilationUnit.this)
                    );
                    iter.remove();
                }
            }
        }
    };


    /**
     * Runs classgen() on a single ClassNode.
     */
    private PrimaryClassNodeOperation classgen = new PrimaryClassNodeOperation() {
        public boolean needSortedInput() {
            return true;
        }

        public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {

            //
            // Run the Verifier on the outer class
            //
            try {
                verifier.visitClass(classNode);
            } catch (GroovyRuntimeException rpe) {
                ASTNode node = rpe.getNode();
                getErrorCollector().addError(
                        new SyntaxException(rpe.getMessage(), null, node.getLineNumber(), node.getColumnNumber()),
                        source
                );
            }

            LabelVerifier lv = new LabelVerifier(source);
            lv.visitClass(classNode);

            ClassCompletionVerifier completionVerifier = new ClassCompletionVerifier(source);
            completionVerifier.visitClass(classNode);

            ExtendedVerifier xverifier = new ExtendedVerifier(source);
            xverifier.visitClass(classNode);

            // because the class may be generated even if a error was found
            // and that class may have an invalid format we fail here if needed
            getErrorCollector().failIfErrors();

            //
            // Prep the generator machinery
            //
            ClassVisitor visitor = createClassVisitor();


            String sourceName = (source == null ? classNode.getModule().getDescription() : source.getName());
            // only show the file name and its extension like javac does in its stacktraces rather than the full path
            // also takes care of both \ and / depending on the host compiling environment
            if (sourceName != null)
                sourceName = sourceName.substring(Math.max(sourceName.lastIndexOf('\\'), sourceName.lastIndexOf('/')) + 1);
            ClassGenerator generator = new AsmClassGenerator(context, visitor, classLoader, sourceName);

            //
            // Run the generation and create the class (if required)
            //
            generator.visitClass(classNode);


            byte[] bytes = ((ClassWriter) visitor).toByteArray();
            generatedClasses.add(new GroovyClass(classNode.getName(), bytes));

            //
            // Handle any callback that's been set
            //
            if (CompilationUnit.this.classgenCallback != null) {
                classgenCallback.call(visitor, classNode);
            }

            //
            // Recurse for inner classes
            //
            LinkedList innerClasses = generator.getInnerClasses();
            while (!innerClasses.isEmpty()) {
                classgen.call(source, context, (ClassNode) innerClasses.removeFirst());
            }
        }
    };


    protected ClassVisitor createClassVisitor() {
        return new ClassWriter(true);
    }

    //---------------------------------------------------------------------------
    // PHASE HANDLING


    /**
     * Updates the phase marker on all sources.
     */
    protected void mark() throws CompilationFailedException {
        applyToSourceUnits(mark);
    }


    /**
     * Marks a single SourceUnit with the current phase,
     * if it isn't already there yet.
     */
    private SourceUnitOperation mark = new SourceUnitOperation() {
        public void call(SourceUnit source) throws CompilationFailedException {
            if (source.phase < phase) {
                source.gotoPhase(phase);
            }


            if (source.phase == phase && phaseComplete && !source.phaseComplete) {
                source.completePhase();
            }
        }
    };

    //---------------------------------------------------------------------------
    // LOOP SIMPLIFICATION FOR SourceUnit OPERATIONS


    /**
     * An callback interface for use in the applyToSourceUnits loop driver.
     */
    public abstract static class SourceUnitOperation {
        public abstract void call(SourceUnit source) throws CompilationFailedException;
    }


    /**
     * A loop driver for applying operations to all SourceUnits.
     * Automatically skips units that have already been processed
     * through the current phase.
     */
    public void applyToSourceUnits(SourceUnitOperation body) throws CompilationFailedException {
        Iterator keys = names.iterator();
        while (keys.hasNext()) {
            String name = (String) keys.next();
            SourceUnit source = (SourceUnit) sources.get(name);
            if ((source.phase < phase) || (source.phase == phase && !source.phaseComplete)) {
                try {
                    body.call(source);
                } catch (CompilationFailedException e) {
                    throw e;
                } catch (Exception e) {
                    GroovyBugError gbe = new GroovyBugError(e);
                    changeBugText(gbe, source);
                    throw gbe;
                } catch (GroovyBugError e) {
                    changeBugText(e, source);
                    throw e;
                }
            }
        }


        getErrorCollector().failIfErrors();
    }

    //---------------------------------------------------------------------------
    // LOOP SIMPLIFICATION FOR PRIMARY ClassNode OPERATIONS


    /**
     * An callback interface for use in the applyToSourceUnits loop driver.
     */
    public abstract static class PrimaryClassNodeOperation {
        public abstract void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException;

        public boolean needSortedInput() {
            return false;
        }
    }

    public abstract static class GroovyClassOperation {
        public abstract void call(GroovyClass gclass) throws CompilationFailedException;
    }

    private List getPrimaryClassNodes(boolean sort) {
        List unsorted = new ArrayList();
        Iterator modules = this.ast.getModules().iterator();
        while (modules.hasNext()) {
            ModuleNode module = (ModuleNode) modules.next();

            Iterator classNodes = module.getClasses().iterator();
            while (classNodes.hasNext()) {
                ClassNode classNode = (ClassNode) classNodes.next();
                unsorted.add(classNode);
            }
        }

        if (sort == false) return unsorted;

        int[] indexClass = new int[unsorted.size()];
        int[] indexInterface = new int[unsorted.size()];
        {
            int i = 0;
            for (Iterator iter = unsorted.iterator(); iter.hasNext(); i++) {
                ClassNode node = (ClassNode) iter.next();
                int count = 0;
                ClassNode element = node;
                while (element != null) {
                    count++;
                    element = element.getSuperClass();
                }
                if (node.isInterface()) {
                    indexInterface[i] = count;
                    indexClass[i] = -1;
                } else {
                    indexClass[i] = count;
                    indexInterface[i] = -1;
                }
            }
        }

        List sorted = getSorted(indexInterface, unsorted);
        sorted.addAll(getSorted(indexClass, unsorted));

        return sorted;
    }

    private List getSorted(int[] index, List unsorted) {
        List sorted = new ArrayList(unsorted.size());
        int start = 0;
        for (int i = 0; i < unsorted.size(); i++) {
            int min = -1;
            for (int j = 0; j < unsorted.size(); j++) {
                if (index[j] == -1) continue;
                if (min == -1) {
                    min = j;
                } else if (index[j] < index[min]) {
                    min = j;
                }
            }
            if (min == -1) break;
            sorted.add(unsorted.get(min));
            index[min] = -1;
        }
        return sorted;
    }

    /**
     * A loop driver for applying operations to all primary ClassNodes in
     * our AST.  Automatically skips units that have already been processed
     * through the current phase.
     */
    public void applyToPrimaryClassNodes(PrimaryClassNodeOperation body) throws CompilationFailedException {
        Iterator classNodes = getPrimaryClassNodes(body.needSortedInput()).iterator();
        while (classNodes.hasNext()) {
            SourceUnit context = null;
            try {
                ClassNode classNode = (ClassNode) classNodes.next();
                context = classNode.getModule().getContext();
                if (context == null || context.phase <= phase) {
                    body.call(context, new GeneratorContext(this.ast), classNode);
                }
            } catch (CompilationFailedException e) {
                // fall thorugh, getErrorREporter().failIfErrors() will triger
            } catch (NullPointerException npe) {
                throw npe;
            } catch (GroovyBugError e) {
                changeBugText(e, context);
                throw e;
            } catch (Exception e) {
                // check the exception for a nested compilation exception
                ErrorCollector nestedCollector = null;
                for (Throwable next = e.getCause(); next != e && next != null; next = next.getCause()) {
                    if (!(next instanceof MultipleCompilationErrorsException)) continue;
                    MultipleCompilationErrorsException mcee = (MultipleCompilationErrorsException) next;
                    nestedCollector = mcee.collector;
                    break;
                }

                if (nestedCollector != null) {
                    getErrorCollector().addCollectorContents(nestedCollector);
                } else {
                    getErrorCollector().addError(new ExceptionMessage(e, configuration.getDebug(), this));
                }
            }
        }

        getErrorCollector().failIfErrors();
    }

    public void applyToGeneratedGroovyClasses(GroovyClassOperation body) throws CompilationFailedException {
        if (this.phase != Phases.OUTPUT && !(this.phase == Phases.CLASS_GENERATION && this.phaseComplete)) {
            throw new GroovyBugError("CompilationUnit not ready for output(). Current phase=" + getPhaseDescription());
        }

        boolean failures = false;

        Iterator iterator = this.generatedClasses.iterator();
        while (iterator.hasNext()) {
            //
            // Get the class and calculate its filesystem name
            //
            GroovyClass gclass = (GroovyClass) iterator.next();
            try {
                body.call(gclass);
            } catch (CompilationFailedException e) {
                // fall thorugh, getErrorREporter().failIfErrors() will triger
            } catch (NullPointerException npe) {
                throw npe;
            } catch (GroovyBugError e) {
                changeBugText(e, null);
                throw e;
            } catch (Exception e) {
                GroovyBugError gbe = new GroovyBugError(e);
                throw gbe;
            }
        }

        getErrorCollector().failIfErrors();
    }

    private void changeBugText(GroovyBugError e, SourceUnit context) {
        e.setBugText("exception in phase '" + getPhaseDescription() + "' in source unit '" + ((context != null) ? context.getName() : "?") + "' " + e.getBugText());
    }
}
