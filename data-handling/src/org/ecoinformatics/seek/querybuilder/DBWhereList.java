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
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.IOException;

import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import org.kepler.objectmanager.data.db.DSSchemaIFace;

/**
 * This class is used to draw the nested where clause. The needs all the items
 * in a linar vector. But the where object is really a nested object tree. Also,
 * each operator needs to have a terminating node be displayed, which a problem
 * because the terminator really isn't in the model
 */
public class DBWhereList extends javax.swing.JList implements
		DropTargetListener {
	protected DBWhereModel mModel = null;
	protected DSSchemaIFace mSchema = null;

	protected int mAceptableActions = DnDConstants.ACTION_COPY_OR_MOVE;
	protected DBWhereListCellRenderer mRenderer = new DBWhereListCellRenderer();
	protected DropTarget mDropTarget = new DropTarget(this, mAceptableActions,
			this);
	protected DataFlavor mDataFlavor = new DataFlavor(DBTableField.class,
			"DBTableField");
	protected DBWhereIFace mDragOverItem = null;

	/**
	 * Constructor with DBWhereModel DataModel
	 * 
	 * @param aDataModel
	 */
	public DBWhereList(DSSchemaIFace aSchema, DBWhereModel aDataModel) {
		super(aDataModel);
		mModel = aDataModel;
		mSchema = aSchema;
		setCellRenderer(mRenderer);
	}

	/**
	 * Dirties the "this" list
	 * 
	 */
	protected void dirtyAll() {
		RepaintManager mgr = RepaintManager.currentManager(this);
		mgr.markCompletelyDirty(this);
	}

	/**
	 * Dirties the Root (the TableDesktopPane)
	 */
	protected void dirtyRoot() {
		RepaintManager mgr = RepaintManager.currentManager(this);
		mgr.markCompletelyDirty(this);

	}

	// --------------------------------------------------------------
	// ------------------ Drag Target Methods -----------------------
	// --------------------------------------------------------------
	/**
   *
   */
	public void dragEnter(DropTargetDragEvent e) {
		dragOver(e);
	}

	/**
	 * update to remove and highlighted item rendering
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
	 * Make sure the right item is highlighted
	 */
	public void dragOver(DropTargetDragEvent e) {
		if (!isDragOk(e)) {
			e.rejectDrag();
			return;
		}

		Point pnt = e.getLocation();
		for (int i = 0; i < mModel.getSize(); i++) {
			DBWhereIFace whereCell = (DBWhereIFace) mModel.getElementAt(i);

			Rectangle rect = whereCell.getBounds();
			if (rect.contains(pnt)) {
				e.acceptDrag(mAceptableActions);
				if (mDragOverItem != whereCell) {
					if (mDragOverItem != null) {
						mDragOverItem.setDragOver(false);
					}

					boolean isOK = true;
					if (whereCell.isOperator()
							&& whereCell instanceof DBWhereOperator) {
						if (((DBWhereOperator) mModel.getElementAt(0))
								.getClosure() == whereCell) {
							isOK = false;
						}
					}

					mDragOverItem = whereCell;
					mDragOverItem.setDragOver(isOK);

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
	 * Indicates whether a drop can happen
	 * 
	 * @param e
	 * 	 */
	private boolean isDragOk(DropTargetDragEvent e) {
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
   *
   */
	public void dropActionChanged(DropTargetDragEvent dropTargetDragEvent) {
	}

	/*
	 * protected boolean isOKToDropOnOper(DBWhereOperator aOper) { if
	 * (aOper.isClosure()) { for (Enumeration e = tables.elements();
	 * e.hasMoreElements();) { DBWhereIFace item =
	 * (DBWhereIFace)e.nextElement(); if (item.isOperator() && item instanceof
	 * DBWhereOperator) { DBWhereOperator oper = (DBWhereOperator)item; if
	 * (oper.isClosure() && oper.getClosure() == aOper) { } if
	 * (aOper.getStartClose().getParent() == null) { return false; }
	 * 
	 * } return true; }
	 */

	/**
	 * Allows the drop from the table frame (JList) Checks to see if there was
	 * already some items there (mDragOverItem) and it will insert it below the
	 * item. The import part is figuring out who the parent is in order for it
	 * to get inserts correctly.
	 */
	public synchronized void drop(DropTargetDropEvent e) {
		try {
			Transferable tr = e.getTransferable();
			if (tr.isDataFlavorSupported(mDataFlavor)) {
				e.acceptDrop(mAceptableActions);
				DBTableField dbTableField = (DBTableField) tr
						.getTransferData(mDataFlavor);
				e.getDropTargetContext().dropComplete(true);
				if (dbTableField != null) {
					DBWhereOperator oper = null;
					if (mDragOverItem != null) {
						DBWhereIFace parent = mDragOverItem;
						if (!(parent instanceof DBWhereOperator)) {
							parent = parent.getParent();
						}

						if (parent instanceof DBWhereOperator) {
							if (((DBWhereOperator) mModel.getElementAt(0))
									.getClosure() != (DBWhereOperator) parent) {
								oper = (DBWhereOperator) parent;
							}
						}
					}

					if ((oper == null && mModel.getSize() == 0 || oper != null)) {
						DBWhereCondition cond = new DBWhereCondition(oper,
								dbTableField.getTable().getName(), dbTableField
										.getName(), dbTableField.getDataType());
						cond.setDepth(oper != null ? oper.getDepth() + 1 : 1);
						int inx = mModel.add(cond);
						this.setSelectedIndex(inx);
						mModel.fireContentsChanged();
					}

					// mModel.fireContentsChanged();
					if (mDragOverItem != null) {
						mDragOverItem.setDragOver(false);
					}
				}
				mDragOverItem = null;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						dirtyRoot();
					}
				});
				dragExit((DropTargetEvent) null);
				e.dropComplete(true);
				return;
			} else {
				// System.err.println ("Rejected");
			}
		} catch (IOException io) {
			io.printStackTrace();

		} catch (UnsupportedFlavorException ufe) {
			ufe.printStackTrace();
		}
		e.dropComplete(false);
		dragExit((DropTargetEvent) null);
	}

}