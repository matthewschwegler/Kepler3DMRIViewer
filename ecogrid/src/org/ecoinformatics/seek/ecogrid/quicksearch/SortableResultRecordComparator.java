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

package org.ecoinformatics.seek.ecogrid.quicksearch;

import java.util.Comparator;

/**
 * Class to compare two ResultRecord. We will compare them base on their name
 * 
 * @author Jing Tao
 */

public class SortableResultRecordComparator implements Comparator {
	/**
	 * Default constructor
	 */
	public SortableResultRecordComparator() {

	}

	/**
	 * Compare to objects (ResultRecord) base one there name
	 * 
	 * @param o1
	 *            Object
	 * @param o2
	 *            Object
	 * @return int 0 means equal, -1 means less than, 1 means greater than
	 */
	public int compare(Object o1, Object o2) {
		SortableResultRecord record1 = (SortableResultRecord) o1;
		SortableResultRecord record2 = (SortableResultRecord) o2;
		if (record1 == null && record2 == null) {
			return 0;
		}
		if (record1 == null && record2 != null) {
			return -1;
		}
		if (record1 != null && record2 == null) {
			return 1;
		}
		String title1 = record1.getTitle();
		String title2 = record2.getTitle();
		if (title1 == null && title2 == null) {
			return 0;
		}
		if (title1 == null && title2 != null) {
			return -1;
		}
		if (title1 != null && title2 == null) {
			return 1;
		}
		return title1.compareTo(title2);

	}

}