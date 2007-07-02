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

package groovy.ui

import groovy.swing.SwingBuilder
import groovy.inspect.swingui.ObjectBrowser

import java.awt.BorderLayout
import java.awt.EventQueue
import java.awt.Color
import java.awt.Font
import java.awt.Insets
import java.awt.Toolkit
import java.awt.event.KeyEvent
import java.io.PrintWriter
import java.io.StringWriter
import java.util.EventObject

import javax.swing.*
import javax.swing.text.*
import javax.swing.event.*

import org.codehaus.groovy.runtime.InvokerHelper

/**
 * Groovy Swing console.
 *
 * Allows user to interactively enter and execute Groovy.
 *
 * @author Danno Ferrin
 * @author Dierk Koenig, changed Layout, included Selection sensitivity, included ObjectBrowser
 * @author Alan Green more features: history, System.out capture, bind result to _
 */
class Console implements CaretListener {

    // Whether or not std output should be captured to the console
    def captureStdOut = true

    // Maximum size of history
    int maxHistory = 10

    // Maximum number of characters to show on console at any time
    int maxOutputChars = 10000

    // UI
    SwingBuilder swing
    JFrame frame
    JTextArea inputArea
    JTextPane outputArea
    JLabel statusLabel
    JDialog runWaitDialog

    // Styles for output area
    Style promptStyle;
    Style commandStyle;
    Style outputStyle;
    Style resultStyle;

    // Internal history
    List history = []
    int historyIndex = 1 // valid values are 0..history.length()

    // Current editor state
    boolean dirty
    int textSelectionStart  // keep track of selections in inputArea
    int textSelectionEnd
    def scriptFile

    // Running scripts
    GroovyShell shell
    int scriptNameCounter = 0
    def systemOutInterceptor
    def runThread = null
    Closure beforeExecution
    Closure afterExecution
    

    static void main(args) {
        def console = new Console()
        console.run()
    }

    Console() {
        shell = new GroovyShell()
    }

    Console(Binding binding) {
        shell = new GroovyShell(binding)
    }

    Console(ClassLoader parent, Binding binding) {
        shell = new GroovyShell(parent,binding)
    }

    void run() {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        // if menu modifier is two keys we are out of luck as the javadocs
        // indicates it returns "Control+Shift" instead of "Control Shift"
        def menuModifier = KeyEvent.getKeyModifiersText(
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()).toLowerCase() + ' '

        swing = new SwingBuilder()

        swing.actions {
            action(id:'newFileAction',
                name:'New File', 
                closure: this.&fileNewFile, 
                mnemonic: 'N',
                accelerator: menuModifier + 'Q'
            )
            action(id:'newWindowAction',
                name:'New Window', 
                closure: this.&fileNewWindow, 
                mnemonic: 'W'
            )
            action(id:'openAction',
                name:'Open', 
                closure: this.&fileOpen, 
                mnemonic: 'O', 
                accelerator: menuModifier + 'O'
            )
            action(id:'saveAction',
                name:'Save', 
                closure: this.&fileSave,  
                mnemonic: 'S', 
                accelerator: menuModifier + 'S'
            )
            action(id:'exitAction',
                name:'Exit', 
                closure: this.&exit, 
                mnemonic: 'x', 
                accelerator: 'alt F4'
            )
            action(id:'historyPrevAction',
                name:'Previous', 
                closure: this.&historyPrev, 
                mnemonic: 'P', 
                accelerator: 'ctrl P'
            )
            action(id:'historyNextAction', 
                name: 'Next', 
                closure: this.&historyNext, 
                mnemonic: 'N', 
                accelerator: 'ctrl N'
            )
            action(id:'clearOutputAction',
                name:'Clear Output', 
                closure: this.&clearOutput, 
                mnemonic: 'l', 
                keyStroke: 'ctrl W',
                accelerator: 'ctrl W'
            )
            action(id:'runAction',
                name:'Run', 
                closure: this.&runScript, 
                mnemonic: 'R', 
                keyStroke: 'ctrl ENTER',
                accelerator: 'ctrl R'
            )
            action(id:'inspectLastAction',
                name:'Inspect Last', 
                closure: this.&inspectLast, 
                mnemonic: 'I', 
                keyStroke: 'ctrl I',
                accelerator: 'ctrl I'
            )
            action(id:'inspectVariablesAction',
                name:'Inspect Variables', 
                closure: this.&inspectVariables, 
                mnemonic: 'V', 
                keyStroke: 'ctrl J',
                accelerator: 'ctrl J'
            )
            action(id:'captureStdOutAction',
                name:'Capture Standard Output', 
                closure: this.&captureStdOut, 
                mnemonic: 'C'
            )
            action(id:'largerFontAction',
                name:'Larger Font', 
                closure: this.&largerFont, 
                mnemonic: 'L', 
                keyStroke: 'alt shift L',
                accelerator: 'alt shift L'
            )
            action(id:'smallerFontAction',
                name:'Smaller Font', 
                closure: this.&smallerFont, 
                mnemonic: 'S', 
                keyStroke: 'alt shift S',
                accelerator: 'alt shift S'
            )
            action(id:'aboutAction',
                name:'About', 
                closure: this.&showAbout, 
                mnemonic: 'A'
            )
            action(id:'interruptAction',
                name: 'Interrupt', 
                closure: this.&confirmRunInterrupt
            )
        }

        frame = swing.frame(
            title:'GroovyConsole',
            location:[100,100],
            size:[500,400],
            defaultCloseOperation:WindowConstants.DO_NOTHING_ON_CLOSE
        ) {
            menuBar {
                menu(text:'File', mnemonic: 'F') {
                    menuItem(newFileAction)
                    menuItem(newWindowAction)
                    menuItem(openAction)
                    separator()
                    menuItem(saveAction)
                    separator()
                    menuItem(exitAction)
                }
                menu(text:'Edit', mnemonic: 'E') {
                    menuItem(historyNextAction)
                    menuItem(historyPrevAction)
                    separator()
                    menuItem(clearOutputAction)
                }
                menu(text:'Actions', mnemonic: 'A') {
                    menuItem(runAction)
                    menuItem(inspectLastAction)
                    menuItem(inspectVariablesAction)
                    separator()
                    checkBoxMenuItem(captureStdOutAction, selected: captureStdOut)
                    separator()
                    menuItem(largerFontAction)
                    menuItem(smallerFontAction)
                }
                menu(text:'Help', mnemonic: 'H') {
                    menuItem(aboutAction)
                }
            }

            borderLayout()

            splitPane(id:'splitPane', resizeWeight:0.50F,
                orientation:JSplitPane.VERTICAL_SPLIT, constraints: BorderLayout.CENTER)
            {
                scrollPane {
                    inputArea = textArea(id:'inputArea',
                        margin: new Insets(3,3,3,3), 
                        font: new Font('Monospaced', Font.PLAIN,12)
                    ) { action(runAction) }
                }
                scrollPane {
                    outputArea = textPane(id:'outputArea',
                        editable:false, 
                        background: new Color(255,255,218)
                    )
                    addStylesToDocument(outputArea)
                }
            }

            statusLabel = label(id:'status', 
                text: 'Welcome to the Groovy.', 
                constraints: BorderLayout.SOUTH,
                border: BorderFactory.createLoweredBevelBorder()
            )
        }   // end of frame

        runWaitDialog = swing.dialog(title: 'Groovy executing', 
                owner: frame, 
                modal: true
        ) {
            vbox(border: BorderFactory.createEmptyBorder(6, 6, 6, 6)) {  
                label(text: "Groovy is now executing. Please wait.", alignmentX: 0.5f)
                vstrut()
                button(interruptAction,
                    margin: new Insets(10, 20, 10, 20), 
                    alignmentX: 0.5f
                )
            }
        } // end of runWaitDialog

        // add listeners
        frame.windowClosing = this.&exit
        inputArea.addCaretListener(this)
        inputArea.document.undoableEditHappened = { setDirty(true) }

        systemOutInterceptor = new SystemOutputInterceptor(this.&notifySystemOut)
        systemOutInterceptor.start();

        bindResults()

        frame.show()
        SwingUtilities.invokeLater({inputArea.requestFocus()});
    }

    void addStylesToDocument(JTextPane outputArea) {
        StyledDocument doc = outputArea.getStyledDocument();

        Style defStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

        Style regular = doc.addStyle("regular", defStyle);
        StyleConstants.setFontFamily(regular, "Monospaced")

        promptStyle = doc.addStyle("prompt", regular)
        StyleConstants.setForeground(promptStyle, Color.BLUE)

        commandStyle = doc.addStyle("command", regular);
        StyleConstants.setForeground(commandStyle, Color.MAGENTA)

        outputStyle = regular

        resultStyle = doc.addStyle("result", regular)
        StyleConstants.setBackground(resultStyle, Color.BLUE)
        StyleConstants.setBackground(resultStyle, Color.YELLOW)
    }

    void addToHistory(record) {
        history.add(record)
        // history.size here just retrieves method closure
        if (history.size() > maxHistory) {
            history.remove(0)
        }
        // history.size doesn't work here either
        historyIndex = history.size()
    }

    // Append a string to the output area
    void appendOutput(text, style){
        def doc = outputArea.styledDocument
        doc.insertString(doc.length, text, style)

        // Ensure we don't have too much in console (takes too much memory)
        if (doc.length > maxOutputChars) {
            doc.remove(0, doc.length - maxOutputChars)
        }
    }

    // Append a string to the output area on a new line
    void appendOutputNl(text, style){
        def doc = outputArea.styledDocument
        def len = doc.length
        if (len > 0 && doc.getText(len - 1, 1) != "\n") {
            appendOutput("\n", style);
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

    private static void beep() {
        Toolkit.defaultToolkit.beep()
    }

    // Binds the "_" and "__" variables in the shell
    void bindResults() {
        shell.setVariable("_", getLastResult()) // lastResult doesn't seem to work
        shell.setVariable("__", history.collect {it.result})
    }

    // Handles menu event
    void captureStdOut(EventObject evt) {
        captureStdOut = evt.source.selected
    }

    void caretUpdate(CaretEvent e){
        textSelectionStart = Math.min(e.dot,e.mark)
        textSelectionEnd = Math.max(e.dot,e.mark)
    }


    void clearOutput(EventObject evt = null) {
        outputArea.setText('')
    }

    // Confirm whether to interrupt the running thread
    void confirmRunInterrupt(EventObject evt) {
        def rc = JOptionPane.showConfirmDialog(frame, "Attempt to interrupt script?",
            "GroovyConsole", JOptionPane.YES_NO_OPTION)
        if (rc == JOptionPane.YES_OPTION && runThread != null) {
            runThread.interrupt()
        }
    }

    void exit(EventObject evt = null) {
        if (askToSaveFile()) {
            frame.hide()
            frame.dispose()
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
      (new Console(new Binding(new HashMap(shell.context.variables)))).run()
    }

    void fileOpen(EventObject evt = null) {
        scriptFile = selectFilename();
        if (scriptFile != null) {
            inputArea.text = scriptFile.readLines().join('\n');
            setDirty(false)
            inputArea.caretPosition = 0
        }
    }

    // Save file - return false if user cancelled save
    boolean fileSave(EventObject evt = null) {
        if (scriptFile == null) {
            scriptFile = selectFilename("Save");
        }
        if (scriptFile != null) {
            scriptFile.write(inputArea.text)
            setDirty(false);
            return true
        } else {
            return false
        }
    }

    def finishException(Throwable t) {
        statusLabel.text = 'Execution terminated with exception.'
        history[-1].exception = t

        appendOutputNl("Exception thrown: ", promptStyle)
        appendOutput(t.toString(), resultStyle)

        StringWriter sw = new StringWriter()
        new PrintWriter(sw).withWriter { pw -> t.printStackTrace(pw) }

        appendOutputNl("\n${sw.buffer}\n", outputStyle)
        bindResults()
    }

    def finishNormal(Object result) {
        // Take down the wait/cancel dialog
        history[-1].result = result
        if (result != null) {
            statusLabel.text = 'Execution complete.'
            appendOutputNl("Result: ", promptStyle)
            appendOutput("${InvokerHelper.inspect(result)}", resultStyle)
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

    // Allow access to shell from outside console
    // (useful for configuring shell before startup)
    GroovyShell getShell() {
        return shell
    }

    void historyNext(EventObject evt = null) {
        if (historyIndex < history.size()) {
            historyIndex++;
            setInputTextFromHistory()
        } else {
            statusLabel.text = "Can't go past end of history (time travel not allowed)"
            beep()
        }
    }

    void historyPrev(EventObject evt = null) {
        if (historyIndex > 0) {
            historyIndex--;
            setInputTextFromHistory()
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
        def newFont = new Font('Monospaced', Font.PLAIN, inputArea.font.size + 2)
        inputArea.font = newFont
        outputArea.font = newFont
    }

    Boolean notifySystemOut(String str) {
        if (!captureStdOut) {
            // Output as normal
            return true
        }

        // Put onto GUI
        if (EventQueue.isDispatchThread()) {
            appendOutput(str, outputStyle)
        }
        else {
            SwingUtilities.invokeLater {
                appendOutput(str, outputStyle)
            }
        }
        return false
    }

    // actually run the
    void runScript(EventObject evt = null) {
        def record = new HistoryRecord( allText: inputArea.getText(),
            selectionStart: textSelectionStart, selectionEnd: textSelectionEnd)
        addToHistory(record)

        // Print the input text
        for (line in record.textToRun.tokenize("\n")) {
            appendOutputNl('groovy> ', promptStyle)
            appendOutput(line, commandStyle)
        }

        //appendOutputNl("") - with wrong number of args, causes StackOverFlowError;
        appendOutputNl("\n", promptStyle)

        // Kick off a new thread to do the evaluation
        statusLabel.text = 'Running Script...'

        // Run in separate thread, so that System.out can be captured
        runThread = Thread.start {
            try {
                SwingUtilities.invokeLater { showRunWaitDialog() }
                String name = "Script${scriptNameCounter++}"
                if(beforeExecution) {
                    beforeExecution()
                }
                def result = shell.evaluate(record.textToRun, name);
                if(afterExecution) {
                    afterExecution()
                }
                SwingUtilities.invokeLater { finishNormal(result) }
            } catch (Throwable t) {
                SwingUtilities.invokeLater { finishException(t) }
            } finally {
                SwingUtilities.invokeLater {
                    runWaitDialog.hide();
                    runThread = null
                }
            }
        }
    }


    
    def selectFilename(name = "Open") {
        def fc = new JFileChooser()
        fc.fileSelectionMode = JFileChooser.FILES_ONLY
        fc.acceptAllFileFilterUsed = true
        if (fc.showDialog(frame, name) == JFileChooser.APPROVE_OPTION) {
            return fc.selectedFile
        } else {
            return null
        }
    }

    void setDirty(boolean newDirty) {
        dirty = newDirty
        updateTitle()
    }

    private void setInputTextFromHistory() {
        if (historyIndex < history.size()) {
            def record = history[historyIndex]
            inputArea.text = record.allText
            inputArea.selectionStart = record.selectionStart
            inputArea.selectionEnd = record.selectionEnd
            setDirty(true) // Should calculate dirty flag properly (hash last saved/read text in each file)
            statusLabel.text = "command history ${history.size() - historyIndex}"
        } else {
            inputArea.text = ""
            statusLabel.text = 'at end of history'
        }
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

    // Shows the 'wait' dialog
    void showRunWaitDialog() {
        runWaitDialog.pack()
        int x = frame.x + (frame.width - runWaitDialog.width) / 2
        int y = frame.y + (frame.height - runWaitDialog.height) / 2
        runWaitDialog.setLocation(x, y)
        runWaitDialog.show()
    }

    void smallerFont(EventObject evt = null){
        if (inputArea.font.size < 5) return
        def newFont = new Font('Monospaced', Font.PLAIN, inputArea.font.size - 2)
        inputArea.font = newFont
        outputArea.font = newFont
    }

    void updateTitle() {
        if (scriptFile != null) {
            frame.title = scriptFile.name + (dirty?" * ":"") + " - GroovyConsole"
        } else {
            frame.title = "GroovyConsole"
        }
    }
}

/** A single time when the user selected "run" */
class HistoryRecord {
    def allText
    def selectionStart
    def selectionEnd
    def scriptName
    def result
    def exception

    def getTextToRun() {
        if (selectionStart != selectionEnd) {
            return allText[selectionStart ..< selectionEnd]
        }
        return allText
    }

    def getValue() {
        return exception ? exception : result
    }
}
