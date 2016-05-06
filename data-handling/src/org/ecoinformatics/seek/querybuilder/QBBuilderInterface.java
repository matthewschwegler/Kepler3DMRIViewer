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
 * @author Rod SPears
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface QBBuilderInterface {
	public static final int STANDARD = 0;
	public static final int INTERMEDIATE = 1;
	public static final int ADVANCED = 2;

	/**
	 * 
	 * @return returns the "type" of builder it is as defined by the constants
	 *         in this interface
	 */
	public int getType();

	/**
	 * A textual name for this builder
	 * 
	 * @return string of the name
	 */
	public String getName();

	/**
	 * This checks to see if this type of builder can convert the internal SQL
	 * to a more complex or less complex form.
	 * 
	 * This is typically called when switching from a more complex builder to a
	 * less complex builder
	 * 
	 * @param aBldr
	 *            The "receiving" builder, in other words can this builder
	 *            convert the SQL to the new builder
	 * @return true if it can convert it, false if it can not
	 */
	public boolean canConvertTo(QBBuilderInterface aBldr);

	/**
	 * 
	 * @return Returns a string representing the interal SQL Query
	 */
	public String createSQL();

	/**
	 * Build UI from the Query Definition Object
	 */
	public int buildFromQueryDef(DBQueryDef aQueryDef);

	/**
	 * Fill the QueryDef from the Model
	 * 
	 * @param aQueryDef
	 *            the query
	 */
	public void fillQueryDef(DBQueryDef aQueryDef);

}