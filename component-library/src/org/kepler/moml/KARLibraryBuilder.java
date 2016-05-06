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

package org.kepler.moml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.objectmanager.library.LibraryManager;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Workspace;
import ptolemy.moml.LibraryBuilder;

/**
 * This class is used by ptolemy to initialize the library. It is one of the
 * first things that happens during Kepler startup. It is really a wrapper for
 * the LibraryManager which does the heavy lifting. This classes' main purpose
 * is to return a ComponentEntity library from the buildLibrary method. The
 * ptolemy extension point that this class is an extension of can be found in
 * ptolemy.actor.gui.UserActorLibrary.openLibrary(Configuration,File)
 */
public class KARLibraryBuilder extends LibraryBuilder {
	private static final Log log = LogFactory.getLog(KARLibraryBuilder.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	/**
	 * constructor.
	 */
	public KARLibraryBuilder() {
		super();
	}

	/**
	 * Build the library. This should be built in the form of a ComponentEntity
	 * See the ptolemy code if you want an example of what the ComponentEntity
	 * should look like
	 * 
	 * @return ComponentEntity
	 * @throws Exception
	 */
	public CompositeEntity buildLibrary(Workspace workspace) throws Exception {

		long starttime = System.currentTimeMillis();
		if (isDebugging) {
			log.debug("Starting KARLibraryBuilder.buildLibrary: " + starttime
					+ " ms");
		}

		LibraryManager lm = LibraryManager.getInstance();
		lm.setActorLibraryWorkspace(workspace);
		lm.buildLibrary();

		CompositeEntity actorLib = lm.getActorLibrary();

		if (isDebugging) {
			long stoptime = System.currentTimeMillis();
			log.debug("Finishing KARLibraryBuilder.buildLibrary: "
					+ (stoptime - starttime) + " ms");
		}

		return actorLib;

	}
}