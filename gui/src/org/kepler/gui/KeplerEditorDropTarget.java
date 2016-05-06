/* A drop target for the ptolemy editor.

 Copyright (c) 1999-2010 The Regents of the University of California.
 All rights reserved.
 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the above
 copyright notice and the following two paragraphs appear in all copies
 of this software.

 IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.

 PT_COPYRIGHT_VERSION_2
 COPYRIGHTENDKEY

 */
package org.kepler.gui;

import java.util.TooManyListenersException;

import ptolemy.vergil.basic.EditorDropTarget;
import diva.graph.JGraph;

///////////////////////////////////////////////////////////////////
//// KeplerEditorDropTarget

/**
 * This class is an extension of EditorDropTarget that adds support
 * for Drag and Drop insert and replace.
 * @author Sven Koehler
 */
public class KeplerEditorDropTarget extends EditorDropTarget {

    /** Construct a new graph target to operate on the given JGraph.
     *  @param graph The diva graph panel.
     */
    public KeplerEditorDropTarget(JGraph graph) {
        super();
        setComponent(graph);

        try {
            KeplerDropTargetListener l = new KeplerDropTargetListener();
            l.setDropTarget(this);
            addDropTargetListener(l);
        } catch (TooManyListenersException wow) {
        }
    }
}