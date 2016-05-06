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
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * Extends JLabel to to render the label, it will draw a half fill circle
 * indicating that the field it represents will be displayed in the select
 * statement
 */
class DBSelectTableOverviewCellRenderer extends JLabel implements
		TableCellRenderer {
	private DBSelectTableModelItem mItem = null;

	private static final int ICON_SIZES = 20;

	/**
	 * Constructor
	 * 
	 */
	public DBSelectTableOverviewCellRenderer() {
		setOpaque(true);
	}

	/**
	 * Override paint so we can track the bounds of the items
	 */
	public void paint(Graphics g) {
		super.paint(g);
		if (mItem != null) {
			Rectangle rect = getBounds();

			int x = rect.width - ICON_SIZES + 4;
			if (mItem.isDisplayed()) {
				g.fillArc(x, 2, rect.height - 2, rect.height - 2, 270, 180);
			}
		}
	}

	/**
	 * Returns the component used to render the cell
	 * 
	 * @return the renderer
	 */
	public Component getTableCellRendererComponent(JTable aTable,
			Object aValue, boolean aIsSelected, boolean aHasFocus, int aRow,
			int aColumn) {
		if (aValue == null) {
			aValue = (DBSelectTableModelItem) aTable.getModel().getValueAt(
					aRow, aColumn);
		}
		mItem = (DBSelectTableModelItem) aValue;
		setText(aValue.toString());

		if (!aIsSelected) {

			setForeground(Color.black);
			setBackground(Color.white);
		} else {
			setForeground(Color.white);
			setBackground(Color.black);
		}
		return this;
	}
}