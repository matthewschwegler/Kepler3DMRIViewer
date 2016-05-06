/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jianwu $'
 * '$Date: 2012-09-14 15:53:57 -0700 (Fri, 14 Sep 2012) $' 
 * '$Revision: 30680 $'
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
//// PropertyClassChanges

/**
 * This class updates Ptolemy's PropertyClassChanges MoML filter with
 * Kepler-specific actors that have property class changes.
 * 
 * @author Daniel Crawl
 * @version $Id: KeplerPropertyClassChanges.java 30680 2012-09-14 22:53:57Z jianwu $
 */

public class KeplerPropertyClassChanges {

	/** This class cannot be instantiated. */
	private KeplerPropertyClassChanges() {
	}

	/** Update Ptolemy's PropertyClassChanges filter. */
	public static void initialize() {
		ptolemy.moml.filter.PropertyClassChanges changes = new ptolemy.moml.filter.PropertyClassChanges();

		// Changes made after Kepler 1.x release:

		// OpenDBConnection
		HashMap<String, String> openDatabaseClassChanges = new HashMap<String, String>();

		openDatabaseClassChanges.put("databaseURL", STRING_PARAMETER);

		changes.put("org.geon.OpenDBConnection", openDatabaseClassChanges);

		// OpendapDataSource
		HashMap<String, String> opendapDataSourceChanges = new HashMap<String, String>();

		opendapDataSourceChanges.put("DAP2 URL", PORT_PARAMETER);
		opendapDataSourceChanges.put("DAP2 Constraint Expression",
				PORT_PARAMETER);

		changes.put(
				"org.kepler.dataproxy.datasource.opendap.OpendapDataSource",
				opendapDataSourceChanges);

		// DirectoryMaker
		HashMap<String, String> directoryMakerChanges = new HashMap<String, String>();

		directoryMakerChanges.put("Directory name", PORT_PARAMETER);
		
		changes.put("org.resurgence.actor.DirectoryMaker",
			directoryMakerChanges);
		
		
		// Changes made after Kepler 2.1 release:
		
		// FTPClient: operation and mode parameters changed to StringParameter
		HashMap<String,String> ftpClientChanges = new HashMap<String,String>();
		ftpClientChanges.put("operation", STRING_PARAMETER);
		ftpClientChanges.put("mode", STRING_PARAMETER);
		
		changes.put("org.geon.FTPClient", ftpClientChanges);
		
		// Changes made after Kepler 2.3 release:
		
		final HashMap<String,String> wsWithComplexTypesChanges = new HashMap<String,String>();
		wsWithComplexTypesChanges.put("wsdl", PORT_PARAMETER);
		
		changes.put("org.sdm.spa.WSWithComplexTypes", wsWithComplexTypesChanges);
		
		final HashMap<String,String> genericJobLauncherChanges = new HashMap<String,String>();
		genericJobLauncherChanges.put("jobSubmitOptions", STRING_PARAMETER);
		genericJobLauncherChanges.put("binPath", STRING_PARAMETER);
		genericJobLauncherChanges.put("executable", STRING_PARAMETER);		
		
		changes.put("org.kepler.actor.job.GenericJobLauncher", genericJobLauncherChanges);
	}
	
	/** String constant of StringParameter class name. */
	private static String STRING_PARAMETER = "ptolemy.data.expr.StringParameter";

	/** String constant of PortParameter class name. */
	private static String PORT_PARAMETER = "ptolemy.actor.parameters.PortParameter";
}