/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: brooks $'
 * '$Date: 2012-08-06 22:37:20 -0700 (Mon, 06 Aug 2012) $' 
 * '$Revision: 30364 $'
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

package org.kepler.moml.filter;

import java.util.HashMap;

//////////////////////////////////////////////////////////////////////////
//// PortNameChanges

/**
 * This class updates Ptolemy's PortNameChanges MoML filter with Kepler-specific
 * actors with port name changes.
 * 
 * @author Daniel Crawl
 * @version $Id: KeplerPortNameChanges.java 30364 2012-08-07 05:37:20Z brooks $
 */

public class KeplerPortNameChanges {

	/** This class cannot be instantiated. */
	private KeplerPortNameChanges() {
	}

	/** Update Ptolemy's PortNameChanges filter. */
	public static void initialize() {
		ptolemy.moml.filter.PortNameChanges changes = new ptolemy.moml.filter.PortNameChanges();

		// Changes made after Kepler 1.x release:

		// OpenDBConnection
		HashMap<String, String> databaseWriterPortNameChanges = new HashMap<String, String>();

		databaseWriterPortNameChanges.put("query", "input");

		changes
				.put("org.sdm.spa.DatabaseWriter",
						databaseWriterPortNameChanges);

		// OpendapDataSource
		HashMap<String, String> opendapPortNameChanges = new HashMap<String, String>();

		opendapPortNameChanges.put("opendapURLParameter", "DAP2 URL");
		opendapPortNameChanges.put("opendapCEParameter",
				"DAP2 Constraint Expression");

		changes.put(
				"org.kepler.dataproxy.datasource.opendap.OpendapDataSource",
				opendapPortNameChanges);
	}
}