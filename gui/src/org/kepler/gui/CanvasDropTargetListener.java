/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: welker $'
 * '$Date: 2010-05-05 22:21:26 -0700 (Wed, 05 May 2010) $' 
 * '$Revision: 24234 $'
 * 
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, provided that the above
 * copyright notice and the following two paragraphs appear in all copies
 * of this software.
 *
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 * FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 * THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 * PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 * CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 * ENHANCEMENTS, OR MODIFICATIONS.
 *
 */

package org.kepler.gui;

import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.util.Vector;

/**
 * This class allows any class that wants to listen for canvas drop events to
 * register a listener without having a direct reference to the canvas
 * (BasicGraphFrame) itself.
 * 
 *@author Chad Berkley
 *@since Jan. 4, 2007
 *@version $Id: CanvasDropTargetListener.java 13389 2007-01-04 19:56:50Z
 *          berkley $
 *@since Kepler 1.0
 */
public class CanvasDropTargetListener implements DropTargetListener {
	private static CanvasDropTargetListener listener = null;
	Vector listeners = new Vector();

	protected CanvasDropTargetListener() {

	}

	/**
	 * returns a singleton instance of this listener
	 */
	public static CanvasDropTargetListener getInstance() {
		if (listener == null) {
			listener = new CanvasDropTargetListener();
		}
		return listener;
	}

	/**
	 * register a listener to listen to this listener. in this way, any class
	 * can get an instance of this listener without having to get a reference to
	 * the Canvas, then listen for canvas events
	 */
	public void registerListener(DropTargetListener l) {
		listeners.addElement(l);
	}

	/**
	 * called when a drag enters the canvas
	 */
	public void dragEnter(DropTargetDragEvent dtde) {
		for (int i = 0; i < listeners.size(); i++) {
			DropTargetListener l = (DropTargetListener) listeners.elementAt(i);
			l.dragEnter(dtde);
		}
	}

	/**
	 * called when a drag exits the canvas
	 */
	public void dragExit(DropTargetEvent dtde) {
		for (int i = 0; i < listeners.size(); i++) {
			DropTargetListener l = (DropTargetListener) listeners.elementAt(i);
			l.dragExit(dtde);
		}
	}

	/**
	 * called when the drag moves over the canvas
	 */
	public void dragOver(DropTargetDragEvent dtde) {
		for (int i = 0; i < listeners.size(); i++) {
			DropTargetListener l = (DropTargetListener) listeners.elementAt(i);
			l.dragOver(dtde);
		}
	}

	/**
	 * called when a drag is dropped over the canvas
	 */
	public void drop(DropTargetDropEvent dtde) {
		for (int i = 0; i < listeners.size(); i++) {
			DropTargetListener l = (DropTargetListener) listeners.elementAt(i);
			l.drop(dtde);
		}
	}

	/**
	 * called when a drag action changes
	 */
	public void dropActionChanged(DropTargetDragEvent dtde) {
		for (int i = 0; i < listeners.size(); i++) {
			DropTargetListener l = (DropTargetListener) listeners.elementAt(i);
			l.dropActionChanged(dtde);
		}
	}
}