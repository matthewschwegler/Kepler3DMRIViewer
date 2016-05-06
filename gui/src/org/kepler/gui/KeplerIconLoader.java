/*
 * Copyright (c) 2006-2010 The Regents of the University of California.
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

package org.kepler.gui;

import java.io.IOException;

import org.kepler.icon.ComponentEntityConfig;

import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.moml.IconLoader;

//////////////////////////////////////////////////////////////////////////
//// KeplerIconLoader

/**
 * @author Christopher Brooks, contributor: Edward A. Lee
 * @version $Id: KeplerIconLoader.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 6.0
 * @Pt.ProposedRating
 * @Pt.AcceptedRating
 */
public class KeplerIconLoader implements IconLoader {
	/**
	 * Load an icon for a class in a particular context.
	 * 
	 * @param className
	 *            The name of the class for which the icon is to be loaded.
	 * @param context
	 *            The context in which the icon is loaded.
	 * @return true if the icon was successfully loaded.
	 * @exception IllegalActionException
	 *                If there is a problem adding the icon.
	 * @exception NameDuplicationException
	 *                If the icon being added has the same name as an element
	 *                already in the context.
	 * @exception IOException
	 *                If the icon base path cannot be found.
	 */
	public boolean loadIconForClass(String className, NamedObj context)
			throws IllegalActionException, NameDuplicationException,
			IOException {
		ComponentEntityConfig.addSVGIconTo(context);
		// FIXME: need to return a better value here
		return true;
	}
}