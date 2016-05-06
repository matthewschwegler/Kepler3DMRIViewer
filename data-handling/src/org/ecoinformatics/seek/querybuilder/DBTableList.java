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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceContext;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

/**
 * The JList that resides inside the TableFrame. This listens for drops of other
 * field items to represent a join being made. It does not allow drops from
 * itself to itself.
 */
public class DBTableList extends javax.swing.JList implements
		DropTargetListener, DragSourceListener, DragGestureListener {
	protected int mAceptableActions = DnDConstants.ACTION_COPY_OR_MOVE;
	protected DropTarget mDropTarget = new DropTarget(this, mAceptableActions,
			this);
	protected DragSource mDragSource = DragSource.getDefaultDragSource();
	protected DataFlavor mDataFlavor = new DataFlavor(DBTableField.class,
			"DBTableField");
	protected DBTableField mDragOverItem = null;

	protected DBTableListCellRenderer mRenderer = new DBTableListCellRenderer();

	protected DBTableListModel mModel = null;
	protected DBTableJoin mTableJoins = null;

	protected static DBTableList mSrcList = null;

	/**
	 * Constructor with DBTableListModel DataModel
	 * 
	 * @param aDataModel
	 */
	public DBTableList(DBTableListModel aDataModel)

	{
		super(aDataModel);
		mModel = aDataModel;
		mRenderer.setModel(aDataModel);
		setCellRenderer(mRenderer);
		mDragSource.createDefaultDragGestureRecognizer(this, mAceptableActions,
				this);

		/*
		 * MouseListener mouseListener = new MouseAdapter() { public void
		 * mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) {
		 * 
		 * } } }; addMouseListener(mouseListener);
		 */
	}

	/**
	 * Sets the Joins object (TODO need to make a listener instead)
	 * 
	 * @param aJoins
	 */
	public void setJoins(DBTableJoin aJoins) {
		mTableJoins = aJoins;
	}

	/**
	 * Dirties the "this" list
	 */
	protected void dirtyAll() {
		RepaintManager mgr = RepaintManager.currentManager(this);
		mgr.markCompletelyDirty(this);
	}

	/**
	 * Dirties the Root (the TableDesktopPane)
	 */
	protected void dirtyRoot() {
		JComponent root = (JComponent) this.getParent();
		while (root.getClass() != DBTableDesktopPane.class) {
			root = (JComponent) root.getParent();
		}
		RepaintManager mgr = RepaintManager.currentManager(root);
		mgr.markCompletelyDirty(root);
	}

	// --------------------------------------------------------------
	// ------------------ Drag Source Methods -----------------------
	// --------------------------------------------------------------

	/**
	 * Clear the selection
	 */
	public void dragDropEnd(DragSourceDropEvent e) {
		clearSelection();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				dirtyAll();
			}
		});

	}

	/**
	 * Sets the cursor appropriately to whether it can be dropped here
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
	 * Sets the cursor
	 */
	public void dragExit(DragSourceEvent e) {
		e.getDragSourceContext().setCursor(DragSource.DefaultLinkNoDrop);
	}

	/**
	 * Do the same as Do the same as dragOver
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
	// ------------------ Drag Target Methods -----------------------
	// --------------------------------------------------------------

	/**
	 * Do the same as dragOver
	 */
	public void dragEnter(DropTargetDragEvent e) {

		dragOver(e);
	}

	/**
	 * Clear everything when it is dragged out
	 */
	public void dragExit(DropTargetEvent dropTargetEvent) {
		if (mDragOverItem != null) {
			mDragOverItem.setDragOver(false);
			mDragOverItem = null;
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				dirtyAll();
			}
		});
	}

	/**
	 * On DragOver highlight the appropriate item and save a pointer to it in a
	 * static data member so we know where it will be dropped
	 */
	public void dragOver(DropTargetDragEvent e) {
		if (!isDragOk(e) || mSrcList == this) {
			e.rejectDrag();
			return;
		}

		Point pnt = e.getLocation();
		for (int i = 0; i < mModel.getSize(); i++) {
			DBTableField dbTableField = (DBTableField) mModel.getElementAt(i);

			Rectangle rect = dbTableField.getBounds();
			if (rect.contains(pnt)) {
				if (dbTableField.getName().equals(DBUIUtils.ALL_FIELDS)) {
					e.rejectDrag();
					return;
				}
				e.acceptDrag(mAceptableActions);
				if (mDragOverItem != dbTableField) {
					if (mDragOverItem != null) {
						mDragOverItem.setDragOver(false);
					}
					mDragOverItem = dbTableField;
					mDragOverItem.setDragOver(true);
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							dirtyAll();
						}
					});
				}
				return;
			}
		}
	}

	/**
	 * Checks to make sure the action is correct for dropping
	 */
	private boolean isDragOk(DropTargetDragEvent e) {
		// System.out.println("isDragOk: "+e.getSourceActions() +
		// " mAceptableActions "+mAceptableActions +"  "+(((e.getSourceActions()
		// & mAceptableActions) != 0)));
		if (!e.isDataFlavorSupported(mDataFlavor)) {
			return false;
		}

		// the actions specified when the source
		// created the DragGestureRecognizer
		int sa = e.getSourceActions();

		// we're saying that these actions are necessary
		if ((sa & mAceptableActions) == 0)
			return false;

		return true;
	}

	/**
	 * stubbed
	 */
	public void dropActionChanged(DropTargetDragEvent dropTargetDragEvent) {
	}

	/**
	 * Drop craetes a link from one table to the other
	 */
	public synchronized void drop(DropTargetDropEvent e) {
		if (mSrcList != this) {
			try {
				Transferable tr = e.getTransferable();
				if (tr.isDataFlavorSupported(mDataFlavor)) {
					e.acceptDrop(mAceptableActions);
					DBTableField dbTableField = (DBTableField) tr
							.getTransferData(mDataFlavor);
					e.getDropTargetContext().dropComplete(true);
					if (mDragOverItem != null && dbTableField != null) {
						mTableJoins.addJoin(dbTableField, mDragOverItem);
					}
					mDragOverItem = null;
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							dirtyRoot();
						}
					});
					dragExit((DropTargetEvent) null);
					e.dropComplete(true);
					mSrcList = null;
					return;
				} else {
					// System.err.println ("Rejected");
				}
			} catch (IOException io) {
				io.printStackTrace();

			} catch (UnsupportedFlavorException ufe) {
				ufe.printStackTrace();
			}
		}
		e.dropComplete(false);
		dragExit((DropTargetEvent) null);
		mSrcList = null;
	}

	// --------------------------------------------------------------
	// -------------- DragGestureListener Methods -------------------
	// --------------------------------------------------------------
	public void dragGestureRecognized(DragGestureEvent dragGestureEvent) {
		if (getSelectedIndex() == -1)
			return;

		Object obj = getSelectedValue();
		if (obj == null) {
			// Nothing selected, nothing to drag
			// System.out.println ("Nothing selected - beep");
			getToolkit().beep();
		} else {
			DBTableField dbTableField = (DBTableField) obj;
			mSrcList = this;
			dragGestureEvent.startDrag(DragSource.DefaultLinkNoDrop,
					dbTableField, this);
		}
	}
}