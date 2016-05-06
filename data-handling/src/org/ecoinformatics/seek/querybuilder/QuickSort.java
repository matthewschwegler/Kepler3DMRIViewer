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

/**
 * @author Rod Spears
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Generation - Code and Comments
 */
public class QuickSort {

	/**
	 * Sorts an Object array of items and uses toString to do String compares
	 * for collation
	 * 
	 * @param aList
	 *            The array of items
	 * @param aLeft
	 *            left index
	 * @param aRight
	 *            the right index
	 */
	public static void doSort(Object aList[], int aLeft, int aRight) {

		if (aRight > aLeft) {
			int i = aLeft - 1;
			int j = aRight;

			while (true) {
				while (aList[++i].toString()
						.compareTo(aList[aRight].toString()) < 0)
					;
				while (j > 0)
					if (aList[--j].toString().compareTo(
							aList[aRight].toString()) <= 0)
						break; // out of while

				if (i >= j)
					break;

				swap(aList, i, j);
			}

			swap(aList, i, aRight);

			doSort(aList, aLeft, i - 1);
			doSort(aList, i + 1, aRight);
		}
	}

	/**
	 * Swap two Objects in the array
	 * 
	 * @param aList
	 *            The array of items
	 * @param aInx1
	 *            index to swap
	 * @param aInx2
	 *            index to swap
	 */
	private static void swap(Object aList[], int aInx1, int aInx2) {
		Object tmp = aList[aInx1];
		aList[aInx1] = aList[aInx2];
		aList[aInx2] = tmp;
	}
}