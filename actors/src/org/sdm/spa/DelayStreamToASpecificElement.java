/*
 * Copyright (c) 2010 The Regents of the University of California.
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

/* CPES Actor for processing a stream of files and
   bringing a specific element into the front.
   Do not use in SDF!
 */
/**
 *    '$RCSfile$'
 *
 *     '$Author: welker $'
 *       '$Date: 2010-05-05 22:21:26 -0700 (Wed, 05 May 2010) $'
 *   '$Revision: 24234 $'
 *
 *  For Details: http://www.kepler-project.org
 *
 * Copyright (c) 2004 The Regents of the University of California.
 * All rights reserved.
 *
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, provided that the
 * above copyright notice and the following two paragraphs appear in
 * all copies of this software.
 *
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 * FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN
 * IF THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 * PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY
 * OF CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */

package org.sdm.spa;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.BooleanToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.RecordType;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// DelayStreamToASpecificElement

/**
 * <p>
 * Postpone a stream of files until a specific element is found and bring that
 * element in front.<br/>
 * The input should be a stream of tokens of file information: a record of
 * {name=<filename>, size=<size in bytes>, date=<date in seconds>} Such tokens
 * are produced by org.kepler.actor.io.SshDirectoryListing.
 * </p>
 * 
 * <p>
 * Input files are gathered as long as such an element is found. The
 * specificElement is emitted first and then all other files. This works only
 * for the first time, after that any input is immediately emitted.
 * </p>
 * 
 * <p>
 * For the case when there is no specific element in the stream, a stopping
 * element can also be defined. If such element appears, this actor will emit
 * all stored elements immediately and will not wait for the specific element
 * any more.
 * </p>
 * 
 * <p>
 * This actor is a CPES specific actor. When watching for .bp files of a
 * simulation, the first set of files can have any order. For postprocessing,
 * however, the mesh file should be transferred before processing any other
 * files. So this actor brings the mesh file in front. It can be the case,
 * however, that there are no .bp files at all. The stream always terminated by
 * a final element (stopfile) which should be emitted anyways.
 * </p>
 * 
 * <p>
 * The actor outputs the stream of files
 * </p>
 * 
 * <p>
 * This actor does not produce any tokens for an unknown number of firings, then
 * suddenly it produces several tokens. Thus, it cannot be used in SDF.
 * </p>
 * 
 * @author Norbert Podhorszki
 * @version $Id: DelayStreamToASpecificElement.java 13512 2007-04-13 00:02:17Z
 *          podhorsz $
 * @since Ptolemy II 5.0.1
 */
public class DelayStreamToASpecificElement extends TypedAtomicActor {
	/**
	 * Construct an actor with the given container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the actor cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public DelayStreamToASpecificElement(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// File port type is a record:
		String[] labels = { "name", "size", "date" };
		Type[] ctypes = { BaseType.STRING, BaseType.LONG, BaseType.LONG };
		_filetype = new RecordType(labels, ctypes);

		/*
		 * Input ports and port parameters
		 */

		// file name
		infile = new TypedIOPort(this, "infile", true, false);
		infile.setTypeEquals(_filetype);
		new Parameter(infile, "_showName", BooleanToken.TRUE);

		// The specific element to wait for
		specificElement = new Parameter(this, "specificElement",
				new StringToken("pattern"));
		specificElement.setTypeEquals(BaseType.STRING);

		// The stopping element for the store
		stopElement = new Parameter(this, "stopElement", new StringToken(
				"pattern"));
		stopElement.setTypeEquals(BaseType.STRING);

		/*
		 * Output ports
		 */

		// file name
		outfile = new TypedIOPort(this, "outfile", false, true);
		outfile.setTypeEquals(_filetype);
		new Parameter(outfile, "_showName", BooleanToken.FALSE);

	}

	/***********************************************************
	 * ports and parameters
	 */

	/**
	 * File info record as outputted by org.kepler.actor.io.SshDirectoryList:
	 * {name=&lt;filename&gt;, size=&lt;size in bytes&gt;, date=&lt;date in UTC
	 * seconds&gt;}
	 */
	public TypedIOPort infile;

	/**
	 * The string pattern of the specific element to wait for. Files are
	 * gathered as long as such an element is found. Then first the
	 * specificElement is emitted and then all other files. This works only for
	 * the first time, after that any input is immediately emitted.
	 */
	public Parameter specificElement;

	/**
	 * The string pattern of the stopping element. Stored files are immediately
	 * emitted when such an element is found, even if there were no
	 * specificElement found. This works only once, after that any input is
	 * immediately emitted.
	 */
	public Parameter stopElement;

	/**
	 * The output file info record. Same type as input file infor record.
	 */
	public TypedIOPort outfile;

	/***********************************************************
	 * public methods
	 */

	/**
	 * initialize() runs once before first exec
	 * 
	 * @exception IllegalActionException
	 *                If the parent class throws it.
	 */
	public void initialize() throws IllegalActionException {
		super.initialize();
		_gatherMode = true;
		_files = new ArrayList();
	}

	/**
	 * fire
	 * 
	 * @exception IllegalActionException
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		// get parameters
		StringToken spec = (StringToken) specificElement.getToken();
		String specpattern = spec.stringValue();

		// get parameters
		StringToken stop = (StringToken) stopElement.getToken();
		String stoppattern = stop.stringValue();

		// consume the tokens
		RecordToken fileInfo = (RecordToken) infile.get(0);
		String fn = null;
		fn = ((StringToken) fileInfo.get("name")).stringValue();

		// if (isDebugging) log.debug("Element = " + fn +
		// "  specpattern = " + specpattern +
		// "  stoppattern = " + stoppattern);

		if (_gatherMode) {
			// still looking for the specific element
			if (fn.matches(specpattern)) {
				// found specific element:
				// emit this token and all the stored ones
				if (isDebugging)
					log.debug("Send specific element " + fn);
				outfile.send(0, fileInfo);
				Iterator files = _files.iterator();
				while (files.hasNext()) {
					if (isDebugging)
						log.debug("Send stored element ");
					outfile.send(0, (RecordToken) files.next());
				}
				_gatherMode = false;
			} else if (fn.matches(stoppattern)) {
				// found stop element:
				// emit all stored tokens and then the stop element
				Iterator files = _files.iterator();
				while (files.hasNext()) {
					if (isDebugging)
						log.debug("Send stored element ");
					outfile.send(0, (RecordToken) files.next());
				}
				if (isDebugging)
					log.debug("Send stop element " + fn);
				outfile.send(0, fileInfo);
				_gatherMode = false;

			} else {
				// just store this token for later emission
				_files.add(fileInfo);
				if (isDebugging)
					log.debug("Store file " + fn);
			}
		} else {
			// immediately emit this token
			if (isDebugging)
				log.debug("Pass on element " + fn);
			outfile.send(0, fileInfo);
		}

	}

	private Type _filetype;

	private boolean _gatherMode; // wait for the specific element?
	private ArrayList _files; // of file info RecordTokens

	private static final Log log = LogFactory
			.getLog(DelayStreamToASpecificElement.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

}
