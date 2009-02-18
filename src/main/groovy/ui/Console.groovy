/*
 * Copyright 2003-2008 the original author or authors.
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
package groovy.ui

import groovy.inspect.swingui.ObjectBrowser
import groovy.swing.SwingBuilder
import groovy.ui.ConsoleTextEditor
import groovy.ui.SystemOutputInterceptor
import groovy.ui.text.FindReplaceUtility

import java.awt.Component
import java.awt.EventQueue
import java.awt.Font
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.ActionEvent
import java.util.prefs.Preferences
import javax.swing.*
import javax.swing.event.CaretEvent
import javax.swing.event.CaretListener
import javax.swing.event.HyperlinkListener
import javax.swing.event.HyperlinkEvent
import javax.swing.text.AttributeSet
import javax.swing.text.Element
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.Style
import javax.swing.text.StyleConstants
import javax.swing.text.html.HTML

import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.runtime.StackTraceUtils
import org.codehaus.groovy.control.ErrorCollector
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.codehaus.groovy.control.messages.SyntaxErrorMessage
import org.codehaus.groovy.syntax.SyntaxException
import org.codehaus.groovy.control.messages.ExceptionMessage

/**
 * Groovy Swing console.
 *
 * Allows user to interactively enter and execute Groovy.
 *
 * @version $Id$
 * @author Danno Ferrin
 * @author Dierk Koenig, changed Layout, included Selection sensitivity, included ObjectBrowser
 * @author Alan Green more features: history, System.out capture, bind result to _
 * @author Guillaume Laforge, stacktrace hyperlinking to the current script line
 */
class Console implements CaretListener, HyperlinkListener {

    static final String DEFAULT_SCRIPT_NAME_START = "ConsoleScript"

    static private prefs = Preferences.userNodeForPackage(Console)

    // Whether or not std output should be captured to the console
    static boolean captureStdOut = prefs.getBoolean('captureStdOut', true)
    static consoleControllers = []

    boolean fullStackTraces = prefs.getBoolean('fullStackTraces',
        Boolean.valueOf(System.getProperty("groovy.full.stacktrace", "false")))
    Action fullStackTracesAction

    boolean showScriptInOutput = prefs.getBoolean('showScriptInOutput', true)
    Action showScriptInOutputAction

    boolean visualizeScriptResults = prefs.getBoolean('visualizeScriptResults', false)
    Action visualizeScriptResultsAction

    boolean showToolbar = prefs.getBoolean('showToolbar', true)
    Component toolbar
    Action showToolbarAction

    // Maximum size of history
    int maxHistory = 10

    // Maximum number of characters to show on console at any time
    int maxOutputChars = 20000

    // UI
    SwingBuilder swing
    RootPaneContainer frame
    ConsoleTextEditor inputEditor
    JTextPane inputArea
    JTextPane outputArea
    JLabel statusLabel
    JDialog runWaitDialog
    JLabel rowNumAndColNum

    // row info
    Element rootElement
    int cursorPos
    int rowNum
    int colNum

    // Styles for output area
    Style promptStyle
    Style commandStyle
    Style outputStyle
    Style stacktraceStyle
    Style hyperlinkStyle
    Style resultStyle

    // Internal history
    List history = []
    int historyIndex = 1 // valid values are 0..history.length()
    HistoryRecord pendingRecord = new HistoryRecord( allText: "", selectionStart: 0, selectionEnd: 0)
    Action prevHistoryAction
    Action nextHistoryAction

    // Current editor state
    boolean dirty
    Action saveAction
    int textSelectionStart  // keep track of selections in inputArea
    int textSelectionEnd
    def scriptFile
    File currentFileChooserDir = new File(Preferences.userNodeForPackage(Console).get('currentFileChooserDir', '.'))
    File currentClasspathJarDir = new File(Preferences.userNodeForPackage(Console).get('currentClasspathJarDir', '.'))
    File currentClasspathDir = new File(Preferences.userNodeForPackage(Console).get('currentClasspathDir', '.'))

    // Running scripts
    GroovyShell shell
    int scriptNameCounter = 0
    SystemOutputInterceptor systemOutInterceptor
    Thread runThread = null
    Closure beforeExecution
    Closure afterExecution

    public static String ICON_PATH = '/groovy/ui/ConsoleIcon.png' // used by ObjectBrowser too

    static void main(args) {
        // allow the full stack traces to bubble up to the root logger
        java.util.logging.Logger.getLogger(StackTraceUtils.STACK_LOG_NAME).useParentHandlers = true

        //when starting via main set the look and feel to system
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        def console = new Console(Console.class.classLoader?.getRootLoader())
        console.run()
        if (args.length == 1) console.loadScriptFile(args[0] as File)
    }

    Console() {
        this(new Binding())
    }

    Console(Binding binding) {
        this(null, binding)
    }

    Console(ClassLoader parent) {
        this(parent, new Binding())
    }

    Console(ClassLoader parent, Binding binding) {
        newScript(parent, binding);
        try {
            System.setProperty("groovy.full.stacktrace",
                Boolean.toString(Boolean.valueOf(System.getProperty("groovy.full.stacktrace", "false"))))
        } catch (SecurityException se) {
            fullStackTracesAction.enabled = false;
        }
        consoleControllers += this

        binding.variables._outputTransforms = OutputTransforms.loadOutputTransforms()
    }

    void newScript(ClassLoader parent, Binding binding) {
        shell = new GroovyShell(parent, binding)
    }

    static def frameConsoleDelegates = [
            rootContainerDelegate:{
                frame(
                    title: 'GroovyConsole',
                    //location: [100,100], // in groovy 2.0 use platform default location
                    iconImage: imageIcon("/groovy/ui/ConsoleIcon.png").image,
                    defaultCloseOperation: JFrame.DO_NOTHING_ON_CLOSE,
                ) {
                    try {
                        current.locationByPlatform = true
                    } catch (Exception e) {
                        current.location = [100, 100] // for 1.4 compatibility
                    }
                    containingWindows += current
                }
            },
            menuBarDelegate: {arg->
                current.JMenuBar = build(arg)}
        ];

    void run() {
        run(frameConsoleDelegates)
    }

    void run(JApplet applet) {
        run([
            rootContainerDelegate:{
                containingWindows += SwingUtilities.getRoot(applet.getParent())
                applet
            },
            menuBarDelegate: {arg->
                current.JMenuBar = build(arg)}
        ])
    }

    void run(Map defaults) {

        swing = new SwingBuilder()
        defaults.each{k, v -> swing[k] = v}

        // tweak what the stack traces filter out to be fairly broad
        System.setProperty("groovy.sanitized.stacktraces", """org.codehaus.groovy.runtime.
                org.codehaus.groovy.
                groovy.lang.
                gjdk.groovy.lang.
                sun.
                java.lang.reflect.
                java.lang.Thread
                groovy.ui.Console""")


        // add controller to the swingBuilder bindings
        swing.controller = this

        // create the actions
        swing.build(ConsoleActions)

        // create the view
        swing.build(ConsoleView)

        bindResults()

        // stitch some actions together
        swing.bind(source:swing.inputEditor.undoAction, sourceProperty:'enabled', target:swing.undoAction, targetProperty:'enabled')
        swing.bind(source:swing.inputEditor.redoAction, sourceProperty:'enabled', target:swing.redoAction, targetProperty:'enabled')

        if (swing.consoleFrame instanceof java.awt.Window) {
            swing.consoleFrame.pack()
            swing.consoleFrame.show()
        }
        installInterceptor()
        swing.doLater inputArea.&requestFocus
    }


    public void installInterceptor() {
        systemOutInterceptor = new SystemOutputInterceptor(this.&notifySystemOut)
        systemOutInterceptor.start()
    }

    void addToHistory(record) {
        history.add(record)
        // history.size here just retrieves method closure
        if (history.size() > maxHistory) {
            history.remove(0)
        }
        // history.size doesn't work here either
        historyIndex = history.size()
        updateHistoryActions()
    }

    // Ensure we don't have too much in console (takes too much memory)
    private ensureNoDocLengthOverflow(doc) {
        if (doc.length > maxOutputChars) {
            doc.remove(0, doc.length - maxOutputChars)
        }
    }

    // Append a string to the output area
    void appendOutput(String text, AttributeSet style){
        def doc = outputArea.styledDocument
        doc.insertString(doc.length, text, style)
        ensureNoDocLengthOverflow(doc)
    }

    void appendOutput(Window window, AttributeSet style) {
        append(window.toString(), style)
    }

    void appendOutput(Object object, AttributeSet style) {
        append(object.toString(), style)
    }

    void appendOutput(Component component, AttributeSet style) {
        SimpleAttributeSet sas = new SimpleAttributeSet();
        sas.addAttribute(StyleConstants.NameAttribute, "component")
        StyleConstants.setComponent(sas, component)
        appendOutput(component.toString(), sas)
    }

    void appendOutput(Icon icon, AttributeSet style) {
        SimpleAttributeSet sas = new SimpleAttributeSet();
        sas.addAttribute(StyleConstants.NameAttribute, "icon")
        StyleConstants.setIcon(sas, icon)
        appendOutput(icon.toString(), sas)
    }

    void appendStacktrace(text) {
        def doc = outputArea.styledDocument

        // split lines by new line separator
        def lines = text.split(/(\n|\r|\r\n|\u0085|\u2028|\u2029)/)

        // Java Identifier regex
        def ji = /([\p{Alnum}_\$][\p{Alnum}_\$]*)/

        // stacktrace line regex
        def stacktracePattern = /\tat $ji(\.$ji)+\((($ji(\.(java|groovy))?):(\d+))\)/

        lines.each { line ->
            int initialLength = doc.length

            def matcher = line =~ stacktracePattern
            def fileName =  matcher.matches() ? matcher[0][-5] : ""

            if (fileName == scriptFile?.name || fileName.startsWith(DEFAULT_SCRIPT_NAME_START)) {
                def fileNameAndLineNumber = matcher[0][-6]
                def length = fileNameAndLineNumber.length()
                def index = line.indexOf(fileNameAndLineNumber)

                def style = hyperlinkStyle
                def hrefAttr = new SimpleAttributeSet()
                // don't pass a GString as it won't be coerced to String as addAttribute takes an Object
                hrefAttr.addAttribute(HTML.Attribute.HREF, "file://" + fileNameAndLineNumber)
                style.addAttribute(HTML.Tag.A, hrefAttr);

                doc.insertString(initialLength,                     line[0..<index],                    stacktraceStyle)
                doc.insertString(initialLength + index,             line[index..<(index + length)],     style)
                doc.insertString(initialLength + index + length,    line[(index + length)..-1] + '\n',  stacktraceStyle)
            } else {
                doc.insertString(initialLength, line + '\n', stacktraceStyle)
            }
        }

        ensureNoDocLengthOverflow(doc)
    }

    // Append a string to the output area on a new line
    void appendOutputNl(text, style){
        def doc = outputArea.styledDocument
        def len = doc.length
        if (len > 0 && doc.getText(len - 1, 1) != "\n") {
            appendOutput("\n", style)
        }
        appendOutput(text, style)
    }

    // Return false if use elected to cancel
    boolean askToSaveFile() {
        if (scriptFile == null || !dirty) {
            return true
        }
        switch (JOptionPane.showConfirmDialog(frame,
            "Save changes to " + scriptFile.name + "?",
            "GroovyConsole", JOptionPane.YES_NO_CANCEL_OPTION))
        {
            case JOptionPane.YES_OPTION:
                return fileSave()
            case JOptionPane.NO_OPTION:
                return true
            default:
                return false
        }
    }

    void beep() {
        Toolkit.defaultToolkit.beep()
    }

    // Binds the "_" and "__" variables in the shell
    void bindResults() {
        shell.setVariable("_", getLastResult()) // lastResult doesn't seem to work
        shell.setVariable("__", history.collect {it.result})
    }

    // Handles menu event
    static void captureStdOut(EventObject evt) {
        captureStdOut = evt.source.selected
        prefs.putBoolean('captureStdOut', captureStdOut)
    }

    void fullStackTraces(EventObject evt) {
        fullStackTraces = evt.source.selected
        System.setProperty("groovy.full.stacktrace",
            Boolean.toString(fullStackTraces))
        prefs.putBoolean('fullStackTraces', fullStackTraces)
    }

    void showScriptInOutput(EventObject evt) {
        showScriptInOutput = evt.source.selected
        prefs.putBoolean('showScriptInOutput', showScriptInOutput)
    }

    void visualizeScriptResults(EventObject evt) {
        visualizeScriptResults = evt.source.selected
        prefs.putBoolean('visualizeScriptResults', visualizeScriptResults)
    }

    void showToolbar(EventObject evt) {
        showToolbar = evt.source.selected
        prefs.putBoolean('showToolbar', showToolbar)
        toolbar.visible = showToolbar
    }

    void caretUpdate(CaretEvent e){
        textSelectionStart = Math.min(e.dot,e.mark)
        textSelectionEnd = Math.max(e.dot,e.mark)

        setRowNumAndColNum()
    }

    void clearOutput(EventObject evt = null) {
        outputArea.setText('')
    }

    // Confirm whether to interrupt the running thread
    void confirmRunInterrupt(EventObject evt) {
        def rc = JOptionPane.showConfirmDialog(frame, "Attempt to interrupt script?",
            "GroovyConsole", JOptionPane.YES_NO_OPTION)
        if (rc == JOptionPane.YES_OPTION) {
            runThread?.interrupt()
        }
    }

    void exit(EventObject evt = null) {
        if (askToSaveFile()) {
            if (frame instanceof java.awt.Window) {
                frame.hide()
                frame.dispose()
            }
            FindReplaceUtility.dispose()
            consoleControllers.remove(this)
            if (!consoleControllers) {
                systemOutInterceptor.stop()
            }
        }

    }

    void fileNewFile(EventObject evt = null) {
        if (askToSaveFile()) {
            scriptFile = null
            setDirty(false)
            inputArea.text = ''
        }
    }

    // Start a new window with a copy of current variables
    void fileNewWindow(EventObject evt = null) {
        Console consoleController = new Console(
            new Binding(
                new HashMap(shell.context.variables)))
        consoleController.systemOutInterceptor = systemOutInterceptor
        SwingBuilder swing = new SwingBuilder()
        frameConsoleDelegates.each {k, v -> swing[k] = v}
        swing.controller = consoleController
        swing.build(ConsoleActions)
        swing.build(ConsoleView)
        installInterceptor()
        swing.consoleFrame.pack()
        swing.consoleFrame.show()
        swing.doLater swing.inputArea.&requestFocus
    }

    void fileOpen(EventObject evt = null) {
        def scriptName = selectFilename()
        if (scriptName != null) {
            loadScriptFile(scriptName)
        }
    }

    void loadScriptFile(File file) {
        swing.edt {
            inputArea.editable = false
        }
        swing.doOutside {
            try {
                consoleText = file.readLines().join('\n')
                scriptFile = file
                swing.edt {
                    updateTitle()
                    inputArea.text = consoleText
                    setDirty(false)
                    inputArea.caretPosition = 0
                }
            } finally {
                swing.edt { inputArea.editable = true }
            }
        }
    }

    // Save file - return false if user cancelled save
    boolean fileSave(EventObject evt = null) {
        if (scriptFile == null) {
            return fileSaveAs(evt)
        } else {
            scriptFile.write(inputArea.text)
            setDirty(false)
            return true
        }
    }

    // Save file - return false if user cancelled save
    boolean fileSaveAs(EventObject evt = null) {
        scriptFile = selectFilename("Save")
        if (scriptFile != null) {
            scriptFile.write(inputArea.text)
            setDirty(false)
            return true
        } else {
            return false
        }
    }

    def finishException(Throwable t) {
        statusLabel.text = 'Execution terminated with exception.'
        history[-1].exception = t

        if (t instanceof MultipleCompilationErrorsException) {
            MultipleCompilationErrorsException mcee = t
            ErrorCollector collector = mcee.errorCollector
            int count = collector.errorCount
            appendOutputNl("${count} compilation error${count > 1 ? 's' : ''}:\n\n", commandStyle)

            collector.errors.each { error ->
                if (error instanceof SyntaxErrorMessage) {
                    SyntaxException se = error.cause
                    int errorLine = se.line
                    String message = se.originalMessage

                    String scriptFileName = scriptFile?.name ?: DEFAULT_SCRIPT_NAME_START

                    def doc = outputArea.styledDocument

                    def style = hyperlinkStyle
                    def hrefAttr = new SimpleAttributeSet()
                    // don't pass a GString as it won't be coerced to String as addAttribute takes an Object
                    hrefAttr.addAttribute(HTML.Attribute.HREF, "file://" + scriptFileName + ":" + errorLine)
                    style.addAttribute(HTML.Tag.A, hrefAttr);

                    doc.insertString(doc.length, message + " at ", stacktraceStyle)
                    doc.insertString(doc.length, "line: ${se.line}, column: ${se.column}\n\n", style)
                } else if (error instanceof Throwable) {
                    reportException(error)
                } else if (error instanceof ExceptionMessage) {
                    reportException(error.cause)
                }
            }
        } else {
            reportException(t)
        }
        bindResults()
    }

    private reportException(Throwable t) {
        appendOutputNl("Exception thrown: ", commandStyle)
        appendOutput(t.message + '\n', stacktraceStyle)

        StringWriter sw = new StringWriter()
        new PrintWriter(sw).withWriter {pw -> StackTraceUtils.deepSanitize(t).printStackTrace(pw) }
        appendStacktrace("\n${sw.buffer}\n")
    }

    def finishNormal(Object result) {
        // Take down the wait/cancel dialog
        history[-1].result = result
        if (result != null) {
            statusLabel.text = 'Execution complete.'
            appendOutputNl("Result: ", promptStyle)
            def obj = (visualizeScriptResults
                ? OutputTransforms.transformResult(result, shell.context._outputTransforms)
                : result.toString())

            // multi-methods are magical!
            appendOutput(obj, resultStyle)
        } else {
            statusLabel.text = 'Execution complete. Result was null.'
        }
        bindResults()
    }

    // Gets the last, non-null result
    def getLastResult() {
        // runtime bugs in here history.reverse produces odd lookup
        // return history.reverse.find {it != null}
        if (!history) {
            return
        }
        for (i in (history.size() - 1)..0) {
            if (history[i].result != null) {
                return history[i].result
            }
        }
        return null
    }

    void historyNext(EventObject evt = null) {
        if (historyIndex < history.size()) {
            setInputTextFromHistory(historyIndex + 1)
        } else {
            statusLabel.text = "Can't go past end of history (time travel not allowed)"
            beep()
        }
    }

    void historyPrev(EventObject evt = null) {
        if (historyIndex > 0) {
            setInputTextFromHistory(historyIndex - 1)
        } else {
            statusLabel.text = "Can't go past start of history"
            beep()
        }
    }

    void inspectLast(EventObject evt = null){
        if (null == lastResult) {
            JOptionPane.showMessageDialog(frame, "The last result is null.",
                "Cannot Inspect", JOptionPane.INFORMATION_MESSAGE)
            return
        }
        ObjectBrowser.inspect(lastResult)
    }

    void inspectVariables(EventObject evt = null) {
        ObjectBrowser.inspect(shell.context.variables)
    }

    void largerFont(EventObject evt = null) {
        if (inputArea.font.size > 40) return
        // don't worry, the fonts won't be changed to monospaced face, the styles will only derive from this
        def newFont = new Font('Monospaced', Font.PLAIN, inputArea.font.size + 2)
        inputArea.font = newFont
        outputArea.font = newFont
    }

    static boolean notifySystemOut(String str) {
        if (!captureStdOut) {
            // Output as normal
            return true
        }

        // Put onto GUI
        if (EventQueue.isDispatchThread()) {
            consoleControllers.each {it.appendOutput(str, it.outputStyle)}
        }
        else {
            SwingUtilities.invokeLater {
                consoleControllers.each {it.appendOutput(str, it.outputStyle)}
            }
        }
        return false
    }

    // actually run the script

    void runScript(EventObject evt = null) {
        runScriptImpl(false)
    }

    void runSelectedScript(EventObject evt = null) {
        runScriptImpl(true)
    }

    void addClasspathJar(EventObject evt = null) {
        def fc = new JFileChooser(currentClasspathJarDir)
        fc.fileSelectionMode = JFileChooser.FILES_ONLY
        fc.acceptAllFileFilterUsed = true
        if (fc.showDialog(frame, "Add") == JFileChooser.APPROVE_OPTION) {
            currentClasspathJarDir = fc.currentDirectory
            Preferences.userNodeForPackage(Console).put('currentClasspathJarDir', currentClasspathJarDir.path)
            shell.getClassLoader().addURL(fc.selectedFile.toURL())
        }
    }

    void addClasspathDir(EventObject evt = null) {
        def fc = new JFileChooser(currentClasspathDir)
        fc.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        fc.acceptAllFileFilterUsed = true
        if (fc.showDialog(frame, "Add") == JFileChooser.APPROVE_OPTION) {
            currentClasspathDir = fc.currentDirectory
            Preferences.userNodeForPackage(Console).put('currentClasspathDir', currentClasspathDir.path)
            shell.getClassLoader().addURL(fc.selectedFile.toURL())
        }
    }

    void clearContext(EventObject evt = null) {
        newScript(null, new Binding())
    }

    private void runScriptImpl(boolean selected) {
        def endLine = System.getProperty('line.separator')
        def record = new HistoryRecord( allText: inputArea.getText().replaceAll(endLine, '\n'),
            selectionStart: textSelectionStart, selectionEnd: textSelectionEnd)
        addToHistory(record)
        pendingRecord = new HistoryRecord(allText:'', selectionStart:0, selectionEnd:0)

        // Print the input text
        if (showScriptInOutput) {
            for (line in record.getTextToRun(selected).tokenize("\n")) {
                appendOutputNl('groovy> ', promptStyle)
                appendOutput(line, commandStyle)
            }
        }

        //appendOutputNl("") - with wrong number of args, causes StackOverFlowError
        appendOutputNl("\n", promptStyle)

        // Kick off a new thread to do the evaluation
        statusLabel.text = 'Running Script...'

        // Run in a thread outside of EDT, this method is usually called inside the EDT
        runThread = Thread.start {
            try {
                SwingUtilities.invokeLater { showRunWaitDialog() }
                String name = scriptFile?.name ?: (DEFAULT_SCRIPT_NAME_START + scriptNameCounter++)
                if(beforeExecution) {
                    beforeExecution()
                }
                def result = shell.run(record.getTextToRun(selected), name, [])
                if(afterExecution) {
                    afterExecution()
                }
                SwingUtilities.invokeLater { finishNormal(result) }
            } catch (Throwable t) {
                SwingUtilities.invokeLater { finishException(t) }
            } finally {
                runThread = null
            }
        }
        // Use a watchdog thread to close waiting dialog
        // apparently invokeLater paired with show/hide does not insure
        // ordering or atomic execution, likely because of native AWT issues
        Thread.start {
            while (!(runWaitDialog?.visible)) {
                sleep(10)
            }
            while (runThread?.alive) {
                try {
                    runThread?.join(100)
                } catch (InterruptedException ie) {
                    // we got interrupted, just loop again.
                }
            }
            runWaitDialog.hide()
        }
    }

    def selectFilename(name = "Open") {
        def fc = new JFileChooser(currentFileChooserDir)
        fc.fileSelectionMode = JFileChooser.FILES_ONLY
        fc.acceptAllFileFilterUsed = true
        if (fc.showDialog(frame, name) == JFileChooser.APPROVE_OPTION) {
            currentFileChooserDir = fc.currentDirectory
            Preferences.userNodeForPackage(Console).put('currentFileChooserDir', currentFileChooserDir.path)
            return fc.selectedFile
        } else {
            return null
        }
    }

    void setDirty(boolean newDirty) {
        //TODO when @BoundProperty is live, this should be handled via listeners
        dirty = newDirty
        saveAction.enabled = newDirty
        updateTitle()
    }

    private void setInputTextFromHistory(newIndex) {
        def endLine = System.getProperty('line.separator')
        if (historyIndex >= history.size()) {
            pendingRecord = new HistoryRecord( allText: inputArea.getText().replaceAll(endLine, '\n'),
                selectionStart: textSelectionStart, selectionEnd: textSelectionEnd)
        }
        historyIndex = newIndex
        def record
        if (historyIndex < history.size()) {
            record = history[historyIndex]
            statusLabel.text = "command history ${history.size() - historyIndex}"
        } else {
            record = pendingRecord
            statusLabel.text = 'at end of history'
        }
        inputArea.text = record.allText
        inputArea.selectionStart = record.selectionStart
        inputArea.selectionEnd = record.selectionEnd
        setDirty(true) // Should calculate dirty flag properly (hash last saved/read text in each file)
        updateHistoryActions()
    }

    private void updateHistoryActions() {
        nextHistoryAction.enabled = historyIndex < history.size()
        prevHistoryAction.enabled = historyIndex > 0
    }

    // Adds a variable to the binding
    // Useful for adding variables before openning the console
    void setVariable(String name, Object value) {
        shell.context.setVariable(name, value)
    }

    void showAbout(EventObject evt = null) {
        def version = InvokerHelper.getVersion()
        def pane = swing.optionPane()
         // work around GROOVY-1048
        pane.setMessage('Welcome to the Groovy Console for evaluating Groovy scripts\nVersion ' + version)
        def dialog = pane.createDialog(frame, 'About GroovyConsole')
        dialog.show()
    }

    void find(EventObject evt = null) {
        FindReplaceUtility.showDialog()
    }

    void findNext(EventObject evt = null) {
        FindReplaceUtility.FIND_ACTION.actionPerformed(evt)
    }

    void findPrevious(EventObject evt = null) {
        def reverseEvt = new ActionEvent(
            evt.getSource(), evt.getID(),
            evt.getActionCommand(), evt.getWhen(),
            ActionEvent.SHIFT_MASK) //reverse
        FindReplaceUtility.FIND_ACTION.actionPerformed(reverseEvt)
    }

    void replace(EventObject evt = null) {
        FindReplaceUtility.showDialog(true)
    }


    // Shows the 'wait' dialog
    void showRunWaitDialog() {
        runWaitDialog.pack()
        runWaitDialog.setLocationRelativeTo(frame)
        runWaitDialog.show()
    }

    void smallerFont(EventObject evt = null){
        if (inputArea.font.size < 5) return
        // don't worry, the fonts won't be changed to monospaced face, the styles will only derive from this
        def newFont = new Font('Monospaced', Font.PLAIN, inputArea.font.size - 2)
        inputArea.font = newFont
        outputArea.font = newFont
    }

    void updateTitle() {
        if (frame.properties.containsKey('title')) {
            if (scriptFile != null) {
                frame.title = scriptFile.name + (dirty?" * ":"") + " - GroovyConsole"
            } else {
                frame.title = "GroovyConsole"
            }
        }
    }

    void invokeTextAction(evt, closure) {
        def source = evt.getSource()
        if (source != null) {
            closure(inputArea)
        }
    }

    void cut(EventObject evt = null) {
        invokeTextAction(evt, { source -> source.cut() })
    }

    void copy(EventObject evt = null) {
        invokeTextAction(evt, { source -> source.copy() })
    }

    void paste(EventObject evt = null) {
        invokeTextAction(evt, { source -> source.paste() })
    }

    void selectAll(EventObject evt = null) {
        invokeTextAction(evt, { source -> source.selectAll() })
    }

    void setRowNumAndColNum() {
        cursorPos = inputArea.getCaretPosition()
        rowNum = rootElement.getElementIndex(cursorPos) + 1

        def rowElement = rootElement.getElement(rowNum - 1)
        colNum = cursorPos - rowElement.getStartOffset() + 1

        rowNumAndColNum.setText("$rowNum:$colNum")
    }

    void print(EventObject evt = null) {
        inputEditor.printAction.actionPerformed(evt)
    }

    void undo(EventObject evt = null) {
        inputEditor.undoAction.actionPerformed(evt)
    }

    void redo(EventObject evt = null) {
        inputEditor.redoAction.actionPerformed(evt)
    }

    void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
            // URL of the form: file://myscript.groovy:32
            String url = e.getURL()
            int lineNumber = url[(url.lastIndexOf(':') + 1)..-1].toInteger()

            def editor = inputEditor.textEditor
            def text = editor.text

            int newlineBefore = 0
            int newlineAfter = 0
            int currentLineNumber = 1

            // let's find the previous and next newline surrounding the offending line
            int i = 0
            for (ch in text) {
                if (ch == '\n') {
                    currentLineNumber++
                }
                if (currentLineNumber == lineNumber) {
                    newlineBefore = i
                    def nextNewline = text.indexOf('\n', i + 1)
                    newlineAfter = nextNewline > -1 ? nextNewline : text.length()
                    break
                }
                i++
            }

            // highlight / select the whole line
            editor.setCaretPosition(newlineBefore)
            editor.moveCaretPosition(newlineAfter)
        }
    }
}
