/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: aschultz $'
 * '$Date: 2011-04-14 20:58:02 -0700 (Thu, 14 Apr 2011) $' 
 * '$Revision: 27518 $'
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

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;

/**
 * The root of a result tree. This is a marker class to identify the entity that
 * makes up the root, so that it can be differentiated from other containers in
 * which a ResultRecord might be placed.
 * 
 * @author Matt Jones
 */
public class ResultTreeRoot extends CompositeEntity {
	/**
	 * Construct the root node, setting its name to "name".
	 * 
	 * @param name
	 *            the name of the root node.
	 * @throws IllegalActionException
	 *             when the action can not be completed
	 * @throws NameDuplicationException
	 *             when the name is already in use
	 */
	public ResultTreeRoot(String name) throws IllegalActionException,
			NameDuplicationException {
		this(name,new Workspace());
	}
	
	public ResultTreeRoot(String name, Workspace workspace) throws IllegalActionException,
			NameDuplicationException {
		super(workspace);
		this.setName(name);
	}
}