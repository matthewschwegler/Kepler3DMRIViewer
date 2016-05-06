/*
 * Copyright (c) 2010-2011 The Regents of the University of California.
 * All rights reserved.
 *
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

import org.kepler.kar.karxml.KarXml;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;

/**
 * Created by IntelliJ IDEA.
 * User: sean
 * Date: Mar 23, 2010
 * Time: 4:53:01 PM
 */


// TODO: Actually use this class instead of plain EntityLibraries. Then test
// if right clicking one of these seems to work.

public class DownloadableKAREntityLibrary extends RemoteKAREntityLibrary {

	public DownloadableKAREntityLibrary() {}

	public DownloadableKAREntityLibrary(Workspace workspace) {
		super(workspace);
	}
	
	public DownloadableKAREntityLibrary(CompositeEntity container, String name, KarXml karXml) throws NameDuplicationException, IllegalActionException {
		super(container, name);
		this.karXml = karXml;
	}
	
	// Create a copy of an existing DownloadableKAREntityLibrary (same name,
	// KAR XML file) in a different container
	public DownloadableKAREntityLibrary(CompositeEntity container, DownloadableKAREntityLibrary entityLibrary) throws IllegalActionException, NameDuplicationException {
		this(container, entityLibrary.getName(), entityLibrary.getKarXml());
	}
	

	
	
}
