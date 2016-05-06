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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.event.TableModelListener;

import org.kepler.objectmanager.data.db.DSSchemaIFace;
import org.kepler.objectmanager.data.db.DSTableIFace;

/**
 * Overrides JDesktopPane for laying out all the tables
 */
public class DBTableDesktopPane extends JDesktopPane implements DSSchemaIFace,
		DBSelectTableFieldChangedListener, InternalFrameListener {

	protected DBTableJoin mTableJoins = new DBTableJoin(this);
	protected DSSchemaIFace mSchema = null;
	protected Vector mTables = new Vector();
	protected TableModelListener mTableModelListener = null;

	private int mJifX = 0;

	/**
	 * DBTableDesktopPane Constructor
	 */
	public DBTableDesktopPane() {
		setDoubleBuffered(true);

		MouseListener mouseListener = new MouseAdapter() {
			public void mousePressed(MouseEvent ev) {
				mTableJoins.selectLink(ev);
			}
		};
		addMouseListener(mouseListener);
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
	 *            the listener
	 */
	public void setTableModelListener(TableModelListener aL) {
		mTableModelListener = aL;
	}

	/**
	 * Returns the TableJoin Object
	 * 
	 * @return a joing object
	 */
	public DBTableJoin getTableJoins() {
		return mTableJoins;
	}

	/**
	 * Sets the Data Src Schema Object and creates all the tables for it
	 * 
	 * @param aSchemaDef
	 *            the schema
	 */
	public void setSchema(DSSchemaIFace aSchemaDef) {
		mSchema = aSchemaDef;
		createSchemaTables(mSchema);
	}

	/**
	 * Returns the Schema Object
	 * 
	 * @return schema object
	 */
	public DSSchemaIFace getSchema() {
		return mSchema;
	}

	/**
	 * Return the appropriate DBTableField from the a desktoppane table frame by
	 * its Id (or table name if the id is -1, and then the field name
	 * 
	 * @param aItem
	 *            the item to have its field looked up
	 * @return the field in the table frame
	 */
	public DBTableField getFieldById(DBSelectTableModelItem aItem) {
		for (Enumeration et = mTables.elements(); et.hasMoreElements();) {
			DBTableFrame tblFrame = (DBTableFrame) et.nextElement();
			int id = aItem.getTableId();
			if ((id != -1 && tblFrame.getId() == id)
					|| (id == -1 && aItem.getTableName().equals(
							tblFrame.getName()))) {
				return (DBTableField) DBUIUtils.getFieldByName(tblFrame, aItem
						.getName());
			}
		}
		return null;
	}

	/**
	 * Creates a table frame for each table in the schema
	 * 
	 * @param aSchemaDef
	 *            the schema
	 */
	protected void createSchemaTables(DSSchemaIFace aSchemaDef) {
		// Remove all current table frames here
		clearTables();

		if (mSchema != null) {
			// Create a table for each one in the schema
			int id = 0;
			for (Enumeration et = mSchema.getTables().elements(); et
					.hasMoreElements();) {
				createTable((DSTableIFace) et.nextElement(), id++, -1, -1);
			}
		}
	}

	/**
	 * Deletes and adds all the necessary frames
	 */
	public void clearTables() {
		// Remove all current table frames here
		for (Enumeration et = mTables.elements(); et.hasMoreElements();) {
			this.remove((DBTableFrame) et.nextElement());
		}
		mTables.clear();
		mJifX = 0;
	}

	/**
	 * Creates a single table frame from the table schema
	 * 
	 * @param aTableDef
	 *            the table schema
	 * @param aId
	 *            the unique id of the table
	 * @param aX
	 *            the x coord of the table's location in the builder
	 * @param aY
	 *            the y coord of the table's location in the builder
	 */
	protected void createTable(DSTableIFace aTableDef, int aId, int aX, int aY) {
		if (aTableDef == null)
			return;

		if (aId == -1) {
			aId = this.getNewId();
		}

		// Create a table frame
		int windowCount = this
				.getComponentCountInLayer(JDesktopPane.DEFAULT_LAYER.intValue());
		int width = 100;

		DBTableFrame jif = new DBTableFrame(aTableDef, aId);
		jif.setClosable(true);
		jif.addInternalFrameListener(this);
		jif.setJoins(mTableJoins);

		add(jif, JDesktopPane.DEFAULT_LAYER);

		Dimension dim = jif.getPreferredSize();

		// make sure when it is added that it is always visible
		int panelWidth = getLayeredPaneAbove(jif).getBounds().width;
		int panelHeight = getLayeredPaneAbove(jif).getBounds().height;
		if (panelWidth > 0 && mJifX > panelWidth)
			mJifX = 0;

		// this might not make it visible in the "y" direction
		// but it will atleast be placed on the canvas
		int yCoord = 20 * (windowCount % 10);
		// if (panelHeight > 0 && (yCoord+dim.height) > panelHeight)
		// yCoord = 0;

		if (aX > -1 && aY > -1) {
			jif.setBounds(aX, aY, dim.width, dim.height);
		} else {
			jif.setBounds(mJifX, yCoord, dim.width, dim.height);
		}

		mJifX += dim.width + 20;

		// Set this internal frame to be selected

		try {
			jif.setSelected(true);
		} catch (java.beans.PropertyVetoException e2) {
		}

		jif.show();
		mTables.add(jif);

		KeyListener keyListener = new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_DELETE) {
					if (mTableJoins.removeSelectedLink()) {
						mTableModelListener.tableChanged(null);
						e.consume();
					}
				}
			}
		};
		jif.getList().addKeyListener(keyListener);
		jif.setTableModelListener(mTableModelListener);
	}

	/**
	 * Returns a unique and unused id for a new table
	 * 
	 * @return a unique (unused) id
	 */
	private int getNewId() {
		Hashtable hash = new Hashtable();
		for (Enumeration et = mTables.elements(); et.hasMoreElements();) {
			String idStr = Integer.toString(((DBTableFrame) et.nextElement())
					.getId());
			hash.put(idStr, idStr);
		}

		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			if (hash.get(Integer.toString(i)) == null) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Adds a new table by name to the workspace, this method looks up the table
	 * schema and adds it
	 * 
	 * @param aTableName
	 *            the tables name
	 */
	public void addTableToWorkspace(String aTableName) {
		createTable(DBUIUtils.getTableByName(mSchema, aTableName), getNewId(),
				-1, -1);
		if (mTableModelListener != null)
			mTableModelListener.tableChanged(null);
	}

	/**
	 * Adds a new table by name to the workspace with a DBQueryDefTable object
	 * 
	 * @param aTable
	 *            the table object
	 */
	public void addTableToWorkspace(DBQueryDefTable aTable) {
		createTable(DBUIUtils.getTableByName(mSchema, aTable.getName()), aTable
				.getId(), aTable.getPnt().x, aTable.getPnt().y);
		if (mTableModelListener != null)
			mTableModelListener.tableChanged(null);
	}

	/**
	 * Returns the preferred size of the desktop pane so it is large enough for
	 * all the tables (meaning we want to be able to scroll and see all the
	 * tables and not have them clipped)
	 * 
	 * @return the dimension
	 */
	public Dimension getPreferredSize() {
		int maxWidth = 0;
		int maxHeight = 0;
		Component[] components = getComponents();
		for (int i = 0; i < components.length; i++) {
			Rectangle rect = components[i].getBounds();
			maxWidth = Math.max(rect.x + rect.width, maxWidth);
			maxHeight = Math.max(rect.y + rect.height, maxHeight);
		}
		return new Dimension(maxWidth, maxHeight);
	}

	/**
	 * Overrides paintChildren to make sure all the "joins" get painted
	 */
	public void paintChildren(Graphics g) {
		mTableJoins.paint(g);
		super.paintChildren(g);
	}

	/**
	 * Overrides paint so it can "dirty" everything
	 */
	public void repaint(long tm, int x, int y, int width, int height) {
		Rectangle paintRect = new Rectangle(x, y, width, height);
		Component[] components = getComponents();
		for (int i = 0; i < components.length; i++) {
			Rectangle rect = components[i].getBounds();
			if (paintRect.intersects(rect)) {
				RepaintManager mgr = RepaintManager
						.currentManager(components[i]);
				mgr.addDirtyRegion((JComponent) components[i], rect.x, rect.y,
						rect.width, rect.height);
				/*
				 * Component[] children =
				 * ((Container)components[i]).getComponents(); for (int
				 * j=0;j<children.length;j++) { Rectangle childRect =
				 * children[j].getBounds(); RepaintManager childMgr =
				 * RepaintManager.currentManager(children[j]);
				 * mgr.addDirtyRegion((JComponent)children[j], childRect.x,
				 * childRect.y, childRect.width, childRect.height); }
				 */
			}
		}

		super.repaint(tm, x, y, width, height);

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
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				repaint();
			}
		});

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
	// ------------ DSSchemaIFace -----------------
	// --------------------------------------------------------------

	/**
	 * Returns the Vector of TableFrame Objects
	 * 
	 * @return vector
	 */
	public Vector getTables() {
		return mTables;
	}

	/**
	 * Returns the name of the schema
	 * 
	 * @return string
	 */
	public String getName() {
		return mSchema != null ? mSchema.getName() : "";
	}

	// --------------------------------------------------------------
	// ------- DBSelectTableFieldChangedListener Methods ------------
	// --------------------------------------------------------------
	/**
	 * Makes everything "dirty" and forces an update.
	 */
	public void notifyFieldChanged() {
		makeDirty();
	}

	// -----------------------------------------------
	// ---------- InternalFrameListener --------------
	// -----------------------------------------------
	/**
	 * stubbed
	 */
	public void internalFrameActivated(InternalFrameEvent e) {
	}

	/**
	 * Recieces notification that a table frame was closed. It removes all the
	 * joins to and from the table.
	 * 
	 * @param e
	 *            the event
	 */
	public void internalFrameClosed(InternalFrameEvent e) {
		// Remove it from our taable list
		DBTableFrame tableFrame = (DBTableFrame) e.getInternalFrame();
		mTables.remove(tableFrame);

		// Remove any joins to and from the table
		mTableJoins.removeTable(tableFrame.getId(), tableFrame.getName());
		repaint();

		if (mTableModelListener != null)
			mTableModelListener.tableChanged(null);
	}

	/**
	 * stubbed
	 */
	public void internalFrameClosing(InternalFrameEvent e) {
	}

	/**
	 * stubbed
	 */
	public void internalFrameDeactivated(InternalFrameEvent e) {
	}

	/**
	 * stubbed
	 */
	public void internalFrameDeiconified(InternalFrameEvent e) {
	}

	/**
	 * stubbed
	 */
	public void internalFrameIconified(InternalFrameEvent e) {
	}

	/**
	 * stubbed
	 */
	public void internalFrameOpened(InternalFrameEvent e) {
	}

}