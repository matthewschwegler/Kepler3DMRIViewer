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

package org.kepler.objectmanager.data.text;

/**
 * This class express a width-fixed text format
 * 
 * @author Jing Tao
 */
public class TextWidthFixedDataFormat implements TextComplexDataFormat {
	private long lineNumber = -1;// this for a record expand couple physical
									// lines
	private int fieldWidth = 0;// width of filed(in character number)
	private int fieldStartColumn = -1;// start the field column number

	/**
	 * Constructor with field width
	 * 
	 * @param fieldWidth
	 *            int
	 */
	public TextWidthFixedDataFormat(int fieldWidth) {
		this.fieldWidth = fieldWidth;
	}

	/**
	 * Set line number
	 * 
	 * @param lineNumber
	 *            int
	 */
	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	/**
	 * Get line number
	 * 
	 * @return long
	 */
	public long getLineNumber() {
		return lineNumber;
	}

	/**
	 * Set field width
	 * 
	 * @param fieldWidth
	 *            int
	 */
	public void setFieldWidth(int fieldWidth) {
		this.fieldWidth = fieldWidth;
	}

	/**
	 * Get field width
	 * 
	 * @return int
	 */
	public int getFieldWidth() {
		return fieldWidth;
	}

	/**
	 * Set field start column number
	 * 
	 * @param fieldStartColumn
	 *            long
	 */
	public void setFieldStartColumn(int fieldStartColumn) {
		this.fieldStartColumn = fieldStartColumn;
	}

	/**
	 * Get field start column
	 * 
	 * @return long
	 */

	public int getFieldStartColumn() {
		return fieldStartColumn;
	}

}