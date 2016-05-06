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

package org.ecoinformatics.seek.ecogrid;

import java.awt.Component;
import java.util.Vector;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JTable;

/**
 * This class is cell editor for check box renderer
 * 
 * @author Jing Tao
 */

public class CheckBoxTableCellEditor extends DefaultCellEditor {
	private JTable topTable = null;
	private Vector selectedServiceList = null;
	private int topRowNum = CheckBoxTableCellRenderer.DEFAUTTOPROW;

	/**
	 * Constructor with serviceList and topRowNum
	 * 
	 * @param checkBox
	 *            JCheckBox
	 * @param selectedServiceList
	 *            Vector
	 * @param topRowNum
	 *            int
	 */
	public CheckBoxTableCellEditor(JTable topTable, JCheckBox checkBox,
			Vector selectedServiceList, int topRowNum) {
		super(checkBox);
		this.topTable = topTable;
		this.selectedServiceList = selectedServiceList;
		this.topRowNum = topRowNum;

	}// CheckBoxTableCellEditor

	/**
	 * Overwrite the parent class
	 * 
	 * @param table
	 *            JTable
	 * @param value
	 *            Object
	 * @param isSelected
	 *            boolean
	 * @param row
	 *            int
	 * @param column
	 *            int
	 * @return Component
	 */
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int row, int column) {
		CheckBoxTableCellRenderer render = new CheckBoxTableCellRenderer(
				topTable, selectedServiceList, topRowNum);
		render.getTableCellRendererComponent(table, value, true, isSelected,
				row, column);
		return render.getTableCellRendererComponent(table, value, true,
				isSelected, row, column);

	}// getTableCellEditorComponent

}// CheckBoxTableCellEditor