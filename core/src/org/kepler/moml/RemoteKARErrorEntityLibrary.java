/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:21:34 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31119 $'
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

/**
 * 
 */
package org.kepler.moml;

import org.kepler.kar.karxml.KarXml;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;

/**
 * A convenience class to determine the different types of EntityLibraries in
 * the trees for assigning icons.
 * 
 * @author Aaron Schultz
 * 
 */
public class RemoteKARErrorEntityLibrary extends RemoteKAREntityLibrary {


	public void setKarXml(KarXml karXml) {
		this.karXml = karXml;
	}

	/**
	 * 
	 */
	public RemoteKARErrorEntityLibrary() {
	}

	/**
	 * @param workspace
	 */
	public RemoteKARErrorEntityLibrary(Workspace workspace) {
		super(workspace);
	}

	/**
	 * @param container
	 * @param name
	 * @throws NameDuplicationException
	 * @throws IllegalActionException
	 */
	public RemoteKARErrorEntityLibrary(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
	}

}
