/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2012-09-17 21:38:03 -0700 (Mon, 17 Sep 2012) $' 
 * '$Revision: 30699 $'
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

//////////////////////////////////////////////////////////////////////////
//// ClassChanges

/**
 * This class updates Ptolemy's ClassChanges MoML filter with Kepler-specific
 * classes that have been renamed or deleted.
 * 
 * @author Daniel Crawl
 * @version $Id: KeplerClassChanges.java 30699 2012-09-18 04:38:03Z barseghian $
 */

public class KeplerClassChanges {

	/** This class cannot be instantiated. */
	private KeplerClassChanges() {
	}

	/** Update Ptolemy's ClassChanges filter. */
	public static void initialize() {
		ptolemy.moml.filter.ClassChanges changes = new ptolemy.moml.filter.ClassChanges();

		// Changes made after Kepler 1.x release:

		// ProvenanceListener renamed to ProvenanceRecorder
		changes.put("org.kepler.provenance.ProvenanceListener",
				"org.kepler.provenance.ProvenanceRecorder");
		
		// DataTurbine package changed from org.kepler.data.datasource.rbnb
		// to org.kepler.data.datasource.dataturbine
		changes.put("org.kepler.data.datasource.rbnb.DataTurbine",
				"org.kepler.data.datasource.dataturbine.DataTurbine");
		
		// ReportingListener package changed
		changes.put("org.kepler.module.reporting.ReportingListener",
				"org.kepler.reporting.ReportingListener");
		
		// Post 2.3 change. Remove unnecessary duplicate.
		changes.put("org.geon.ArrayRemoveElement",
				"ptolemy.actor.lib.ArrayRemoveElement");
		
		// Post 2.3 change. Renamed actor.
		changes.put("org.resurgence.actor.ArrayAccumulator",
				"org.resurgence.actor.ArrayToString");
	}
}