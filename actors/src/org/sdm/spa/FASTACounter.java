/*
 * Copyright (c) 2007-2010 The Regents of the University of California.
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

package org.sdm.spa;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.FilePortParameter;
import ptolemy.data.IntToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// FASTACounter

/**
 * On each firing, output the number of FASTA entries in a file.
 * 
 * @author Daniel Crawl
 * @version $Id: FASTACounter.java 24234 2010-05-06 05:21:26Z welker $
 */

public class FASTACounter extends TypedAtomicActor {

	/**
	 * Construct a FASTACounter source with the given container and name.
	 * 
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public FASTACounter(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		filename = new FilePortParameter(this, "filename");
		filename.setTypeEquals(BaseType.STRING);

		count = new TypedIOPort(this, "count", false, true);
		count.setTypeEquals(BaseType.INT);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/** The FASTA file name. */
	public FilePortParameter filename = null;

	/** The number of FASTA entries. */
	public TypedIOPort count = null;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/** Count the number of FASTA entries. */
	public void fire() throws IllegalActionException {
		super.fire();

		filename.update();

		File file = filename.asFile();
		if (!file.isFile()) {
			throw new IllegalActionException(this, filename.stringValue()
					+ " is not a file.");
		}

		BufferedReader br = filename.openForReading();
		int num = 0;
		String line;

		try {
			while ((line = br.readLine()) != null) {
				// a FASTA entry must begin with ">".
				// http://www.ncbi.nlm.nih.gov/BLAST/fasta.shtml
				if (line.length() > 0 && line.charAt(0) == '>') {
					num++;
				}
			}
		} catch (IOException e) {
			throw new IllegalActionException(this, "IOException: "
					+ e.getMessage());
		}

		count.broadcast(new IntToken(num));
	}
}