/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
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

package org.ecoinformatics.seek.querybuilder;

import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceContext;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

/**
 * The JTable of the table schema for "overview", meaning it only displays the
 * field name and the datatype. This also enables the field items to be draged
 * from
 */
public class DBSelectTableOverviewTable extends JTable implements
		DragSourceListener, DragGestureListener,
		DBSelectTableFieldChangedListener {
	protected int mAceptableActions = DnDConstants.ACTION_COPY_OR_MOVE;
	protected DragSource mDragSource = DragSource.getDefaultDragSource();
	protected DataFlavor mDataFlavor = new DataFlavor(
			DBSelectTableModelItem.class, "DBSelectTableModelItem");

	/**
	 * Constructor
	 * 
	 * @param aModel
	 *            table overview model
	 */
	public DBSelectTableOverviewTable(DBSelectTableOverviewModel aModel) {
		super(aModel);
		mDragSource.createDefaultDragGestureRecognizer(this, mAceptableActions,
				this);
	}

	/**
	 * Makes sure the entire "panel" will be redrawn by marking the entire
	 * bounds "dirty"
	 */
	protected void dirtyAll() {
		Rectangle rect = getBounds();
		RepaintManager mgr = RepaintManager.currentManager(this);
		mgr.addDirtyRegion((JComponent) this, rect.x, rect.y, rect.width,
				rect.height);
	}

	/**
	 * Makes sure the entire "panel" will be redrawn
	 * 
	 */
	public void makeDirty() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				dirtyAll();
			}
		});
	}

	// --------------------------------------------------------------
	// ------------------ Drag Source Methods -----------------------
	// --------------------------------------------------------------

	/**
	 * stubbed
	 */
	public void dragDropEnd(DragSourceDropEvent e) {
	}

	/**
   *
   */
	public void dragEnter(DragSourceDragEvent e) {
		DragSourceContext context = e.getDragSourceContext();

		// intersection of the users selected action, and the source and target
		// actions
		int myaction = e.getDropAction();
		// System.out.println("dragEnter Src- dropAction: "+myaction +
		// " mAceptableActions "+mAceptableActions +"  "+(((myaction &
		// mAceptableActions) != 0)));
		if ((myaction & mAceptableActions) != 0) {
			context.setCursor(DragSource.DefaultLinkDrop);
		} else {
			context.setCursor(DragSource.DefaultLinkNoDrop);
		}
	}

	/**
	 * sets the cursor
	 */
	public void dragExit(DragSourceEvent e) {
		e.getDragSourceContext().setCursor(DragSource.DefaultLinkNoDrop);
	}

	/**
	 * Same as drag enter
	 */
	public void dragOver(DragSourceDragEvent e) {
		dragEnter(e);
	}

	/**
	 * stubbed
	 */
	public void dropActionChanged(DragSourceDragEvent DragSourceDragEvent) {
	}

	// --------------------------------------------------------------
	// ------- DBSelectTableFieldChangedListener Methods ------------
	// --------------------------------------------------------------

	/**
	 * When notified, it makes everything as dirty for an visual update
	 */
	public void notifyFieldChanged() {
		makeDirty();
	}

	// --------------------------------------------------------------
	// -------------- DragGestureListener Methods -------------------
	// --------------------------------------------------------------

	/**
	 * Starts drag
	 */
	public void dragGestureRecognized(DragGestureEvent dragGestureEvent) {
		Object obj = null;// getSelectedValue();
		if (getSelectedRowCount() == 1) {
			int rowInx = this.getSelectedRow();
			int colInx = this.getSelectedColumn();
			DBSelectTableModelItem item = (DBSelectTableModelItem) getModel()
					.getValueAt(rowInx, colInx);
			// if (!item.isDisplayed()) // XXX not about this, it might confuse
			// the user
			obj = item;
		}

		if (obj == null) {
			// Nothing selected, nothing to drag
			// System.out.println ("Nothing selected - beep");
			getToolkit().beep();
		} else {
			DBSelectTableModelItem item = (DBSelectTableModelItem) obj;
			dragGestureEvent
					.startDrag(DragSource.DefaultLinkNoDrop, item, this);
		}
	}

}