/*
 * Copyright (c) 2005-2010 The Regents of the University of California.
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

import java.io.IOException;
import java.io.Writer;

import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Workspace;

/**
 * Deprecated. Please use the ptolemy.vergil.basic.KeplerDocumentationAttribute
 * (found in the ptII CVS repository) instead. The exportMoML() method of this
 * class will write out an empty string no matter what is in this attribute.
 * 
 *@author berkley
 *@deprecated Use ptolemy.vergil.basic.KeplerDocumentationAttribute instead.
 *@created March 1, 2005
 */
public class DocumentationAttribute extends Attribute {
	/** Constructor */
	public DocumentationAttribute() {
		super();
	}

	/**
	 * Constructor
	 * 
	 *@param container
	 *            Description of the Parameter
	 *@param name
	 *            Description of the Parameter
	 *@exception IllegalActionException
	 *                Description of the Exception
	 *@exception NameDuplicationException
	 *                Description of the Exception
	 */
	public DocumentationAttribute(NamedObj container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
	}

	/**
	 * Constructor
	 * 
	 *@param workspace
	 *            Description of the Parameter
	 */
	public DocumentationAttribute(Workspace workspace) {
		super(workspace);
	}

	/**
   *
   */
	public void updateContent() throws InternalErrorException {
		// do nothing
	}

	public void exportMoML(Writer output, int depth, String name)
			throws IOException {
		// Since this is deprecated, just write out nothing.
		output.write("");
	}
}