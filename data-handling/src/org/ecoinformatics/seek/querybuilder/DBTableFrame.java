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

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.Vector;

import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.kepler.objectmanager.data.DataType;
import org.kepler.objectmanager.data.db.DSTableFieldDef;
import org.kepler.objectmanager.data.db.DSTableFieldIFace;
import org.kepler.objectmanager.data.db.DSTableIFace;
import org.kepler.objectmanager.data.db.DSTableKeyIFace;

/**
 * Extends JInternalFrame and contains a list of the fields. This object needs
 * to tell its parent the DesktopPane when it moves so the desktop can size
 * appropriately and paint
 */
public class DBTableFrame extends JInternalFrame implements AdjustmentListener,
		TableModelListener, DSTableIFace {
	protected int mId = 0;
	protected DSTableIFace mTableDef = null;
	protected DBTableList mList = null;
	protected JScrollPane mScrollPane = null;

	protected DBTableListModel mModel = null;

	protected Rectangle mListBnds = null;
	protected int mValue = -1;
	protected boolean mBoundsChanging = false;
	protected TableModelListener mTableModelListener = null;

	/**
	 * Default Constructor
	 * 
	 */
	public DBTableFrame(DSTableIFace aTableDef, int aId) {
		mId = aId;
		mTableDef = aTableDef;
		setDoubleBuffered(true);
		this.setTitle(mTableDef.getName());

		setResizable(true);

		mModel = new DBTableListModel(this);
		Vector fields = aTableDef.getFields();
		if (fields != null && fields.size() > 0) {
			DSTableFieldDef fieldDef = new DSTableFieldDef("",
					DBUIUtils.ALL_FIELDS, DataType.STR, null);
			fieldDef.setTable(aTableDef);
			mModel.add(new DBTableField(fieldDef, this));

			DSTableKeyIFace primaryKey = aTableDef.getPrimaryKey();
			if (primaryKey != null) {
				mModel.add(primaryKey);
			}

			for (int i = 0; i < fields.size(); i++) {
				DSTableFieldIFace fld = (DSTableFieldIFace) fields.elementAt(i);
				if (fld instanceof DSTableKeyIFace) {
					DSTableKeyIFace key = (DSTableKeyIFace) fields.elementAt(i);
					if (key.getKeyType() != DSTableKeyIFace.PRIMARYKEY) {
						mModel.add(fld);
					}
				} else {
					mModel.add(fld);
				}
			}
		}

		mList = new DBTableList(mModel);
		mList.setDoubleBuffered(true);

		// Or in two steps:
		mScrollPane = new JScrollPane();
		mScrollPane.getViewport().setView(mList);
		mScrollPane.setDoubleBuffered(true);
		setContentPane(mScrollPane);
		mScrollPane.getVerticalScrollBar().addAdjustmentListener(this);
	}

	/**
	 * Finalize/Cleanup
	 */
	public void finalize() {
		mTableModelListener = null;
	}

	/**
	 * Sets a single listener of TableModel Changes
	 * 
	 * @param aL
	 *            a listener
	 */
	public void setTableModelListener(TableModelListener aL) {
		mTableModelListener = aL;
	}

	/**
	 * @return the unique id of the object
	 */
	public int getId() {
		return mId;
	}

	/**
	 * Uses to set the "Joins" data structure
	 * 
	 * @param aJoins
	 *            The data structure representing all the "joins"
	 */
	public void setJoins(DBTableJoin aJoins) {
		mList.setJoins(aJoins);
	}

	/**
	 * Returns a field item given an index
	 * 
	 * @param aInx
	 *            the index
	 * @return DBTableField
	 */
	public DBTableField getField(int aInx) {
		return (aInx > 0 && aInx < mModel.getSize()) ? (DBTableField) mModel
				.getElementAt(aInx) : null;
	}

	/**
	 * Returns the current value of the vertical scrollbar for the JList
	 * 
	 * @return current scrollbar value
	 */
	public int getScrollValue() {
		return mScrollPane.getVerticalScrollBar().getValue();
	}

	/**
	 * Return the bounds of the JList item in the frame
	 * 
	 * @return The bounds of the JList item in the frame
	 */
	public Rectangle getListBounds() {
		if (mListBnds == null) {
			mListBnds = mList.getBounds();
			Component parent = mList.getParent();
			while (parent != this) {
				Point pnt = parent.getLocation();
				mListBnds.translate(pnt.x, pnt.y);
				if (parent.getClass() == JScrollPane.class) {
					mListBnds.height = parent.getBounds().height;
				}
				parent = parent.getParent();
			}
			mListBnds.y += getScrollValue();
		}
		return mListBnds;
	}

	/**
	 * Gets the ListModel
	 * 
	 * @return the lis model
	 */
	public ListModel getModel() {
		return mModel;
	}

	/**
	 * Returns the DBTableList object (GUI)
	 * 
	 * @return table list
	 */
	public DBTableList getList() {
		return mList;
	}

	/**
	 * Returns the ScrollPane for the List
	 * 
	 * @return the scroll pane
	 */
	public JScrollPane getScrollPane() {
		return mScrollPane;
	}

	/**
	 * Makes sure the Parent object gets correctly resized
	 * 
	 */
	protected void resizeParent() {
		DBTableDesktopPane parent = (DBTableDesktopPane) super.getParent();
		if (parent != null && parent.getParent() != null) {
			parent.getParent().validate();
		}
	}

	/**
	 * Makes sure the entire DesktopPane gets "dirtied" so it all get repainted
	 * 
	 */
	void dirtyAll() {
		DBTableDesktopPane parent = (DBTableDesktopPane) super.getParent();
		if (parent != null) {
			RepaintManager mgr = RepaintManager.currentManager(parent);
			// mgr.addDirtyRegion((JComponent)parent, r.x, r.y, r.width,
			// r.height);
			mgr.markCompletelyDirty(parent);
		}
	}

	/**
	 * Overrides the super class so we can adjust the size of the DesktopPane
	 * when a * a TableFrame is moved or resized
	 */
	public void setBounds(int x, int y, int w, int h) {
		mBoundsChanging = true;
		DBTableDesktopPane parent = (DBTableDesktopPane) super.getParent();
		if (parent != null) {

			Rectangle r = super.getBounds();
			if (r.x != x || r.y != y) {
				if (mTableModelListener != null)
					mTableModelListener.tableChanged(null);

				// do the following on the gui thread
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						dirtyAll();
					}
				});
			}

			Rectangle parentBnds = parent.getBounds();
			int farX = r.x + r.width;
			int farY = r.y + r.height;
			if (farX > parentBnds.width || farY > parentBnds.height) {
				// do the following on the gui thread
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						resizeParent();
					}
				});
			}
		}
		super.setBounds(x, y, w, h);
		mBoundsChanging = false;
	}

	/**
	 * Overrides the super class so we can adjust the size of the DesktopPane
	 * when a TableFrame is moved or resized
	 */
	public void setBounds(java.awt.Rectangle r) {
		setBounds(r.x, r.y, r.width, r.height);
	}

	/**
	 * Overrides the super class so we can adjust the size of the DesktopPane
	 * when a * a TableFrame is moved or resized
	 */
	/*
	 * public void setLocation(int x, int y) { super.setLocation(x, y); }
	 */

	/**
	 * Overrides the super class so we can adjust the size of the DesktopPane
	 * when a * a TableFrame is moved or resized
	 */
	/*
	 * public void setLocation(Point p) { super.setLocation(p); }
	 */

	// ----------------------------------------------------
	// ------------ AdjustmentListener ------------------
	// ----------------------------------------------------
	/**
	 * Makes sure the Join lines gets drawn by dirtying
	 */
	public void adjustmentValueChanged(AdjustmentEvent e) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				dirtyAll();
			}
		});
	}

	// ----------------------------------------------------
	// ------------ TableModelListener ------------------
	// ----------------------------------------------------
	/**
	 * If the Model changes make sure everything is redrawn
	 */
	public void tableChanged(TableModelEvent e) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				dirtyAll();
			}
		});
	}

	// ----------------------------------------------------
	// ----------------- DSTableIFace ---------------------
	// ----------------------------------------------------
	/**
	 * Returns the name of the table
	 * 
	 * @return string
	 */
	public String getName() {
		return mTableDef != null ? mTableDef.getName() : "";
	}

	/**
	 * Returns the mapped name of the table
	 * 
	 * @return string
	 */
	public String getMappedName() {
		return getName();
	}

	/**
	 * Returns a Vector of the fields in the table
	 * 
	 * @return vector of field objects
	 */
	public Vector getFields() {
		if (mList != null) {
			return ((DBTableListModel) mList.getModel()).getFields();
		}
		return null;
	}

	/**
	 * Returns a the Primary Key Definition for the table
	 * 
	 * @return pointer to a key interface
	 */
	public DSTableKeyIFace getPrimaryKey() {
		return mTableDef != null ? mTableDef.getPrimaryKey() : null;
	}
}