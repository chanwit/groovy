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

import groovy.ui.view.*
import javax.swing.UIManager
import static javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE
import javax.swing.event.DocumentListener

switch (UIManager.getSystemLookAndFeelClassName()) {
    case 'com.sun.java.swing.plaf.windows.WindowsLookAndFeel':
    case 'com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel':
        build(WindowsDefaults)
        break

    case 'apple.laf.AquaLookAndFeel':
        build(MacOSXDefaults)
        break

    case 'com.sun.java.swing.plaf.gtk.GTKLookAndFeel':
        build(GTKDefaults)
        break

    default:
        build(Defaults)
        break
}

consoleFrame = frame(
    title: 'GroovyConsole',
    //location: [100,100], // in groovy 2.0 use platform default location
    iconImage: imageIcon("/groovy/ui/ConsoleIcon.png").image,
    defaultCloseOperation: DO_NOTHING_ON_CLOSE,
) {
    try {
        current.locationByPlatform = true
    } catch (Exception e) {
        current.location = [100, 100] // for 1.4 compatibility
    }

    build(menuBarClass)

    build(contentPaneClass)

    build(toolBarClass)

    build(statusBarClass)

    runWaitDialog = dialog(
        title: 'Groovy executing',
        modal: true,
        pack:true,
    ) {
        vbox(border: emptyBorder(6)) {
            label(text: "Groovy is now executing. Please wait.", alignmentX: 0.5f)
            vstrut()
            button(interruptAction,
                margin: [10, 20, 10, 20],
                alignmentX: 0.5f
            )
        }
    }
}


controller.promptStyle = promptStyle
controller.commandStyle = commandStyle
controller.outputStyle = outputStyle
controller.resultStyle = resultStyle

// add the window close handler
consoleFrame.windowClosing = controller.&exit

// link in references to the controller
controller.inputEditor = inputEditor
controller.inputArea = inputEditor.textEditor
controller.outputArea = outputArea
controller.statusLabel = status
controller.frame = consoleFrame
controller.runWaitDialog = runWaitDialog
controller.rowNumAndColNum = rowNumAndColNum
controller.toolbar = toolbar

// link actions
controller.saveAction = saveAction
controller.prevHistoryAction = historyPrevAction
controller.nextHistoryAction = historyNextAction
controller.fullStackTracesAction = fullStackTracesAction
controller.showToolbarAction = showToolbarAction

// some more UI linkage
controller.inputArea.addCaretListener(controller)
controller.inputArea.document.addDocumentListener({ controller.setDirty(true) } as DocumentListener)
controller.rootElement = inputArea.document.defaultRootElement


import java.awt.dnd.DropTargetEvent
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetListener
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DnDConstants

def dtListener =  [
    dragEnter:{DropTargetDragEvent evt ->
        if (evt.dropTargetContext.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            evt.acceptDrag(DnDConstants.ACTION_COPY)
        } else {
            evt.rejectDrag()
        }
    },
    dragOver:{DropTargetDragEvent evt ->
        //dragEnter(evt)
    },
    dropActionChanged:{DropTargetDragEvent evt ->
        //dragEnter(evt)
    },
    dragExit:{DropTargetEvent evt  ->
    },
    drop:{DropTargetDropEvent evt  ->
        evt.acceptDrop DnDConstants.ACTION_COPY
        //println "Dropping! ${evt.transferable.getTransferData(DataFlavor.javaFileListFlavor)}"
        if (controller.askToSaveFile()) {
            controller.loadScriptFile(evt.transferable.getTransferData(DataFlavor.javaFileListFlavor)[0])
        }
    },
] as DropTargetListener

[consoleFrame, inputArea, outputArea].each {
    new DropTarget(it, DnDConstants.ACTION_COPY, dtListener)
}

// don't send any return value from the view, all items should be referenced via the bindings
return null
