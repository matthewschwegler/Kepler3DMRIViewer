/*
 * Copyright (c) 2010 The Regents of the University of California.
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

/*
 * Created on Jul 20, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.ecoinformatics.seek.querybuilder;

import java.util.Vector;

import org.kepler.objectmanager.data.db.DSTableIFace;

/**
 * @author Rod Spears
 * 
 *         Simple class used to track table to join relationships
 */
public class DBJoinPrcTable {
	protected DSTableIFace mTable = null;
	protected Vector mItems = new Vector();
	protected boolean mIsProcessed = false;

	public DBJoinPrcTable(DSTableIFace aTable) {
		mTable = aTable;
	}

	public DSTableIFace getTable() {
		return mTable;
	}

	public void add(DBTableJoinItem aItem) {
		mItems.add(aItem);
	}

	public int getNumItems() {
		return mItems.size();
	}

	public void setProcessed(boolean aVal) {
		mIsProcessed = aVal;
	}

	public boolean isProcessed() {
		return mIsProcessed;
	}

}
