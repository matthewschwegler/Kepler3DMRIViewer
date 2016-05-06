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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.SwingUtilities;

import org.kepler.objectmanager.data.db.DSSchemaIFace;

/**
 * This class maintains all the joins for the query. It is a model, a view, and
 * a controller.<br>
 * Model - A vector of all the join items<br>
 * View - It is responsible for drawing all the lines representing the joins
 * Controller - When clicks occur it is asked for hit detection as to whether a
 * link was clicked on.
 */
public class DBTableJoin {
	private static final int LINE_LEN = 10;
	private static final int TRI_SIZE = 3;

	private int[] xArray = new int[5];
	private int[] yArray = new int[5];

	protected Vector mJoinItems = new Vector();
	protected DSSchemaIFace mSchema = null;
	protected DBTableDesktopPane mDesktopPane = null;
	protected DBTableJoinItem mSelectedItem = null;

	/**
	 * Constructor takes Schema object (interface)
	 * 
	 * @param aDeskTopPane
	 *            desktop pane that implements DSSchemaIFace
	 */
	public DBTableJoin(DBTableDesktopPane aDeskTopPane) {
		mDesktopPane = aDeskTopPane;
		mSchema = (DSSchemaIFace) mDesktopPane;
	}

	/**
	 * Returns the Vector of Joins
	 * 
	 * @return vector
	 */
	public Vector getJoins() {
		return mJoinItems;
	}

	/**
	 * Clears all of the join items
	 * 
	 */
	public void clear() {
		for (Enumeration e = mJoinItems.elements(); e.hasMoreElements();) {
			DBTableJoinItem joinItem = (DBTableJoinItem) e.nextElement();
			joinItem.mItemLeft.setLinked(false);
			joinItem.mItemRight.setLinked(false);
			;
		}
		mJoinItems.clear();
	}

	/**
	 * 
	 * @param aItem
	 * @param aTableId
	 * @param aTableName
	 * 	 */
	private boolean isTableMatch(DBTableField aItem, int aTableId,
			String aTableName) {
		return ((aTableId != -1 && aItem.getTable().getId() == aTableId) || (aTableId == -1 && aTableName
				.equals(aItem.getTable().getName())));
	}

	/**
	 * Removes a tables to and from joins. It looks up by id first and then
	 * table name
	 * 
	 * @param aTableId
	 *            the id of the table
	 * @param aTableName
	 *            the name of the table
	 */
	public void removeTable(int aTableId, String aTableName) {
		// System.out.println("Removing table: "+aTableId+"  "+aTableName);
		// first collect all the join items to be removed
		Vector joinsToRemove = new Vector();
		for (Enumeration e = mJoinItems.elements(); e.hasMoreElements();) {
			DBTableJoinItem joinItem = (DBTableJoinItem) e.nextElement();
			if (isTableMatch(joinItem.mItemLeft, aTableId, aTableName)
					|| isTableMatch(joinItem.mItemRight, aTableId, aTableName)) {
				// System.out.println("Removing join: "+joinItem.mItemLeft.getTable().getId()+"  "+joinItem.mItemLeft.getTable().getName());
				// System.out.println("               "+joinItem.mItemRight.getTable().getId()+"  "+joinItem.mItemRight.getTable().getName());
				joinsToRemove.add(joinItem);
			}
		}
		// now remove all the join items we have collected
		for (Enumeration e = joinsToRemove.elements(); e.hasMoreElements();) {
			mJoinItems.remove((DBTableJoinItem) e.nextElement());
		}
	}

	/**
	 * Add a Join
	 * 
	 * @param aItem1
	 *            the left item
	 * @param aItem2
	 *            the right item
	 */
	public void addJoin(DBTableField aItem1, DBTableField aItem2) {
		if (aItem1 != null && aItem2 != null) {
			DBTableJoinItem item = new DBTableJoinItem(aItem1, aItem2);

			aItem1.setLinked(true);
			aItem2.setLinked(true);
			mJoinItems.add(item);
		}
	}

	/**
	 * Add a Join by item
	 * 
	 * @param aLeft
	 *            the left part of the link
	 * @param aRight
	 *            the right part of the link
	 */
	public void addJoin(DBSelectTableModelItem aLeft,
			DBSelectTableModelItem aRight) {
		DBTableField field1 = mDesktopPane.getFieldById(aLeft);
		DBTableField field2 = mDesktopPane.getFieldById(aRight);
		if (field1 != null && field2 != null) {
			addJoin(field1, field2);
		}
	}

	/**
	 * Helper class for debugging
	 * 
	 * @param aMsg
	 *            the text
	 * @param r
	 *            the rect
	 */
	/*
	 * protected void printRect(String aMsg, Rectangle r){
	 * System.out.print(aMsg+" ["+r.x+","+r.y+","+r.width+","+r.height+"]"); }
	 */

	/**
	 * Implements a paint method for painting all joins
	 * 
	 * @param g
	 *            the graphics object
	 */
	public void paint(Graphics g) {
		for (Enumeration e = mJoinItems.elements(); e.hasMoreElements();) {
			DBTableJoinItem joinItem = (DBTableJoinItem) e.nextElement();
			DBTableField item1 = joinItem.mItemLeft;
			DBTableField item2 = joinItem.mItemRight;

			DBTableFrame tableFrame1 = item1.getTable();
			DBTableFrame tableFrame2 = item2.getTable();

			int val1 = tableFrame1.getScrollValue();
			int val2 = tableFrame2.getScrollValue();

			Rectangle tfListRect1 = tableFrame1.getListBounds();
			Rectangle tfListRect2 = tableFrame2.getListBounds();
			Rectangle tfBnds1 = tableFrame1.getBounds();
			Rectangle tfBnds2 = tableFrame2.getBounds();

			Rectangle r1 = item1.getBounds();
			Rectangle r2 = item2.getBounds();
			// printRect("r1 ", r1);
			// System.out.println(" ");
			// printRect("r2 ", r2);
			// System.out.println(" ");

			r1.x = tfBnds1.x;
			r1.width = tfBnds1.width;
			int y = r1.y - val1;
			if (y < 0) {
				r1.setBounds(tfBnds1.x, tfBnds1.y + 5, tfBnds1.width, 1);
			} else if (y > tfListRect1.height) {
				r1.setBounds(tfBnds1.x, tfBnds1.y + tfBnds1.height - 1 - 5,
						tfBnds1.width, 1);
			} else {
				r1.y = y + tfListRect1.y + tfBnds1.y;
			}

			r2.x = tfBnds2.x;
			r2.width = tfBnds2.width;

			y = r2.y - val2;
			if (y < 0) {
				r2.setBounds(tfBnds2.x, tfBnds2.y + 5, tfBnds2.width, 2);

			} else if (y > tfListRect2.height) {
				r2.setBounds(tfBnds2.x, tfBnds2.y + tfBnds2.height - 2 - 5,
						tfBnds2.width, 2);

			} else {
				r2.y = y + tfListRect2.y + tfBnds2.y;
			}

			int r1HalfX = (int) r1.getCenterX();
			int r1HalfY = (int) r1.getCenterY();
			int r2HalfX = (int) r2.getCenterX();
			int r2HalfY = (int) r2.getCenterY();

			int r1Right = r1.x + r1.width;
			int r2Right = r2.x + r2.width;

			g.setColor(joinItem.isSelected() ? Color.yellow : Color.blue);

			if (r1.x > r2Right || r1.x >= r2.x && r1.x <= r2Right) {
				int xPntsRight[] = { r1.x, r1.x - TRI_SIZE, r1.x };
				int yPntsRight[] = { r1HalfY - TRI_SIZE, r1HalfY,
						r1HalfY + TRI_SIZE };
				int xPntsLeft[] = { r2Right, r2Right + TRI_SIZE, r2Right };
				int yPntsLeft[] = { r2HalfY + TRI_SIZE, r2HalfY,
						r2HalfY - TRI_SIZE };

				g.setColor(joinItem.isSelected() ? Color.yellow : Color.red);
				g.fillPolygon(xPntsRight, yPntsRight, 3);
				g.drawLine(r1.x, r1HalfY, r1.x - LINE_LEN, r1HalfY);
				g.drawLine(r1.x - LINE_LEN, r1HalfY, r2Right + LINE_LEN,
						r2HalfY);
				g.drawLine(r2Right + LINE_LEN, r2HalfY, r2Right, r2HalfY);
				g.fillPolygon(xPntsLeft, yPntsLeft, 3);

				joinItem.changePolygon(0, r1.x, r1HalfY, r1.x - LINE_LEN,
						r1HalfY);
				joinItem.changePolygon(1, r1.x - LINE_LEN, r1HalfY, r2Right
						+ LINE_LEN, r2HalfY);
				joinItem.changePolygon(2, r2Right + LINE_LEN, r2HalfY, r2Right,
						r2HalfY);

			} else if (r2.x > r1Right || r2.x >= r1.x && r2.x <= r1Right) {
				int xPntsRight[] = { r2.x, r2.x - TRI_SIZE, r2.x };
				int yPntsRight[] = { r2HalfY - TRI_SIZE, r2HalfY,
						r2HalfY + TRI_SIZE };
				int xPntsLeft[] = { r1Right, r1Right + TRI_SIZE, r1Right };
				int yPntsLeft[] = { r1HalfY + TRI_SIZE, r1HalfY,
						r1HalfY - TRI_SIZE };

				g.fillPolygon(xPntsLeft, yPntsLeft, 3);
				g.drawLine(r2.x, r2HalfY, r2.x - LINE_LEN, r2HalfY);
				g.drawLine(r2.x - LINE_LEN, r2HalfY, r1Right + LINE_LEN,
						r1HalfY);
				g.drawLine(r1Right + LINE_LEN, r1HalfY, r1Right, r1HalfY);
				g.fillPolygon(xPntsRight, yPntsRight, 3);

				joinItem.changePolygon(0, r2.x, r2HalfY, r2.x - LINE_LEN,
						r2HalfY);
				joinItem.changePolygon(1, r2.x - LINE_LEN, r2HalfY, r1Right
						+ LINE_LEN, r1HalfY);
				joinItem.changePolygon(2, r1Right + LINE_LEN, r1HalfY, r1Right,
						r1HalfY);
			}

			// debug only do not remove
			/*
			 * Polygon[] p = joinItem.getPolygons(); g.setColor(Color.red);
			 * g.drawPolygon(p[0]); g.setColor(Color.green);
			 * g.drawPolygon(p[1]); g.setColor(Color.magenta);
			 * g.drawPolygon(p[2]);
			 */

		}
	}

	/**
	 * Refreshes the DesktopPane
	 */
	public void refresh() {
		mDesktopPane.makeDirty();
	}

	/**
	 * Removes a link or "join" between two tables
	 * 
	 * @return true if link was deleted, false if not
	 */
	public boolean removeSelectedLink() {
		if (mSelectedItem != null) {
			mJoinItems.remove(mSelectedItem);
			mSelectedItem = null;
			refresh();
			return true;
		}
		return false;
	}

	/**
	 * Uses the mouse event to select or deselect a link
	 * 
	 * @param ev
	 *            mouse event
	 */
	public void selectLink(MouseEvent ev) {
		DBTableJoinItem oldSelectedItem = mSelectedItem;
		DBTableJoinItem newSelectedItem = null;

		for (Enumeration e = mJoinItems.elements(); e.hasMoreElements();) {
			DBTableJoinItem joinItem = (DBTableJoinItem) e.nextElement();
			Polygon[] polygons = joinItem.getPolygons();
			for (int i = 0; i < polygons.length; i++) {
				if (polygons[i].contains(ev.getPoint())) {
					newSelectedItem = joinItem;
					break;
				}
			}
		}

		if (newSelectedItem != null) {
			if (newSelectedItem == oldSelectedItem) {
				newSelectedItem.setIsSelected(!newSelectedItem.isSelected());
			} else {
				if (mSelectedItem != null) {
					mSelectedItem.setIsSelected(false);
				}
				newSelectedItem.setIsSelected(true);

				mSelectedItem = newSelectedItem;
				// Make sure a list has focus so the delete key can be properly
				// processed
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						mDesktopPane.getDesktopManager().activateFrame(
								mSelectedItem.getItemLeft().getTable());
						mSelectedItem.getItemLeft().getTable().getList()
								.requestFocus();
					}
				});
			}
		} else {
			if (mSelectedItem != null) {
				mSelectedItem.setIsSelected(false);
				mSelectedItem = null;
			}
		}
		refresh();
	}

}