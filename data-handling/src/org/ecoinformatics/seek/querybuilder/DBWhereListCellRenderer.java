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
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * This class is used to render the items in the nested where clause. THe hard
 * part is to render a different color block for each level of nesting and then
 * have the text indented correctly.<br>
 * The text is indented by overriding the getInsets, that is what the JLabel
 * uses to determine where the text should be drawn.
 */
class DBWhereListCellRenderer extends JLabel implements ListCellRenderer {
	/**
	 * arbitrary list of colors. the rendering uses a "mod" for the index so the
	 * selection of a color loops back around to the begining, so new colors can
	 * be easily added
	 **/
	protected final String gColorStrs[] = { "FFE0E0", "E0E0E0", "E0FFFF",
			"E0E0FF", "E0FFE0", "FFFFE0", "FFE0FF" };
	protected DBWhereIFace mItem = null;
	protected boolean mIsSelected = false;

	/**
	 * Constructor
	 * 
	 */
	public DBWhereListCellRenderer() {
		setOpaque(false);
	}

	/**
	 * Override paint so we can track the bounds of the items
	 */
	public void paint(Graphics g) {
		Rectangle rect = getBounds();
		if (mItem != null) {
			mItem.setBounds(rect);
		}

		if (mItem.isDragOver()) {
			g.setColor(Color.BLUE);
			g.fillRect(0, 0, rect.width, rect.height);

		} else if (!mIsSelected) {
			int x = 0;
			int lastInx = mItem.getDepth() - 1;
			for (int i = 0; i < mItem.getDepth(); i++) {

				Color color = new Color(Integer.parseInt(gColorStrs[i
						% gColorStrs.length], 16));
				g.setColor(color);

				g.fillRect(x, 0, i == lastInx ? rect.width - x : x + 10,
						rect.height);
				x += 10;
			}
		} else {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, rect.width, rect.height);
		}
		super.paint(g);
	}

	/**
	 * Get the proper renderer
	 */
	public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean cellHasFocus) {
		mItem = (DBWhereIFace) value;
		setText(mItem.toString());
		if (!isSelected) {
			Color fg = Color.black;
			if (!mItem.isOperator() && mItem instanceof DBWhereCondition
					&& ((DBWhereCondition) mItem).getCriteria().length() == 0) {
				fg = Color.red;
			}
			setForeground(fg);
			setBackground(Color.white);
			mIsSelected = false;
		} else {
			mIsSelected = true;
			setForeground(Color.white);
			setBackground(Color.black);
		}
		return this;
	}

	/**
	 * Adjust insets for the displaying of text, the text must be indented
	 * properly
	 * 
	 * @return the insets for the text
	 */
	public Insets getInsets() {
		Insets insets = super.getInsets();
		if (mItem != null) {
			insets.left = mItem.getDepth() * 10;
		}
		return insets;
	}

	/**
	 * Adjust insets for the displaying of text, the text must be indented
	 * properly
	 * 
	 * @param aInsets
	 *            the insets object to be adjusted
	 * @return the insets for the text
	 */
	public Insets getInsets(Insets aInsets) {
		Insets insets = super.getInsets(aInsets);
		if (mItem != null) {
			insets.left = mItem.getDepth() * 10;
		}
		return insets;
	}

}