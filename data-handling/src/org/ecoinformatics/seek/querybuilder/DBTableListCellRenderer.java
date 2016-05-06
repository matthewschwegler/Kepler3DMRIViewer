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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.kepler.objectmanager.data.db.DSTableKeyIFace;

/**
 * This is class that is used to display the field items in the Table Frames
 * list
 */
class DBTableListCellRenderer extends JLabel implements ListCellRenderer {
	protected DBTableField mItem = null;
	protected DBTableListModel mModel = null;

	private static final int ICON_SIZES = 20;

	/**
	 * Constructor
	 * 
	 */
	public DBTableListCellRenderer() {
		setOpaque(true);
	}

	/**
	 * Sets the Model
	 * 
	 * @param aDataModel
	 */
	public void setModel(DBTableListModel aDataModel) {
		mModel = aDataModel;
	}

	/**
	 * Get the Preferrred Size which is the super's preferred size + extra for
	 * icons
	 */
	public Dimension getPreferredSize() {
		Dimension dim = super.getPreferredSize();
		if (mItem != null) {
			mItem.setPreferredDim(dim);
		}
		dim.width += ICON_SIZES;
		return dim;
	}

	/**
	 * Override paint so we can track the bounds of the items
	 */
	public void paint(Graphics g) {

		if (mItem != null) {
			mItem.setRect(getBounds());
		}
		super.paint(g);
		if (mItem != null) {
			Dimension dim = mItem.getPreferredDim();

			// draw the half circle "displayed" indicator
			int x = dim.width - ICON_SIZES + 4;
			if (mItem.isDisplayed()) {
				g.fillArc(x, 2, dim.height - 2, dim.height - 2, 270, 180);
			}

			// Draw the "key" icon
			DSTableKeyIFace key = mItem.getTableKeyIFace();
			if (key != null && key.getKeyType() != DSTableKeyIFace.UNDEFINEDKEY) {
				Color color = key.getKeyType() == DSTableKeyIFace.PRIMARYKEY ? Color.red
						: Color.green;
				g.setColor(color);
				g.fillOval(x, 2, 4, 4);
				g.drawLine(x, 2, x, dim.height - 2);
				int ht = 8;
				g.drawLine(x, ht, x + 2, ht);
				int hb = dim.height - 3;
				g.drawLine(x, hb, x + 2, hb);
				int hm = ht + (hb - ht) / 2;
				// g.setColor(Color.green);
				g.drawLine(x, hm, x + 2, hm);
				// g.fillArc(dim.width, 2, dim.height-2, dim.height-2, 270,
				// 180);
			}
		}
	}

	/**
	 * Get the proper renderer
	 */
	public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean cellHasFocus) {
		mItem = (DBTableField) value;
		setText(value.toString());

		if (!isSelected) {
			if (mItem.isDragOver() && !mItem.getLinked()) {
				setForeground(Color.black);
				setBackground(Color.gray);
			} else {
				setForeground(mItem.getLinked() ? Color.white : Color.black);
				setBackground(mItem.getLinked() ? Color.blue : Color.white);
			}
		} else {
			setForeground(Color.white);
			setBackground(Color.black);
		}
		return this;
	}

}