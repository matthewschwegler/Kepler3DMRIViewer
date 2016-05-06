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

/* CPES Actor for chopping up a stream of files into 
   archive lists of minimum size
   Numbering in the file names can denote timesteps of
   a simulation, and this actor keeps files of the same timestep
   together.
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
import ptolemy.data.IntToken;
import ptolemy.data.LongToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.RecordType;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// ArchiveCounter

/**
 * <p>
 * Chop up list of files to archive lists with a minimum size.<br/>
 * The input should be a stream of tokens of file information: a record of
 * {name=<filename>, size=<size in bytes>, date=<date in seconds>} Such tokens
 * are produced by org.kepler.actor.io.SshDirectoryListing.
 * </p>
 * 
 * <p>
 * Numbering (first number field) in the file names can denote timesteps of a
 * simulation, and this actor keeps files of the same timestep together. If
 * there is no number in the file names, then they are considered to be
 * stand-alone steps.
 * </p>
 * 
 * <p>
 * The actor outputs a list of files to be archived whenever the processed
 * files' total size overcomes the specified minimum (and full set of timesteps
 * are available). The output is a record of (a) string containing the list of
 * files to be archived together (separated with \n), (b) the total size and (c)
 * first and (d) last timestep included in this list: {list=&lt;string&gt;,
 * size=&lt;long&gt;, firstTS=&lt;int&gt;, lastTS=&lt;int&gt;}.
 * </p>
 * 
 * <p>
 * The actor outputs also the file info's of the files to be archived
 * one-by-one, for checkpointing and logging purposes, but after such firing
 * when outputs the list.
 * </p>
 * 
 * <p>
 * This actor does not produce any tokens for an unknown number of firings, then
 * suddenly it produces a token. Thus, it cannot be used in SDF.
 * </p>
 * 
 * <p>
 * If the finish flag is set to true, the actor will emit the (last) list
 * without considering its total size. The actual input file will not be
 * considered and listed at all. Thus, the flag can be used to stop the
 * counting, using a special file for this purpose.
 * </p>
 * 
 * <p>
 * The unit of the specified archive minimum size is MB (1024*1024 bytes).
 * </p>
 * 
 * @author Norbert Podhorszki
 * @version $Id: ArchiveCounter.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 5.0.1
 */
public class ArchiveCounter extends TypedAtomicActor {
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
	public ArchiveCounter(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// File port type is a record:
		String[] labels = { "name", "size", "date" };
		Type[] ctypes = { BaseType.STRING, BaseType.LONG, BaseType.LONG };
		_filetype = new RecordType(labels, ctypes);

		// Archive list port type is a record:
		// filename is the local filename,
		// firstTS is the first timestep stored in this list
		// lastTS is the last timestep stored in this list
		labels = new String[] { "list", "size", "firstTS", "lastTS" };
		ctypes = new Type[] { BaseType.STRING, BaseType.LONG, BaseType.INT,
				BaseType.INT };
		_listtype = new RecordType(labels, ctypes);

		/*
		 * Input ports and port parameters
		 */

		// file name
		file = new TypedIOPort(this, "file", true, false);
		file.setTypeEquals(_filetype);
		new Parameter(file, "_showName", BooleanToken.TRUE);

		// finish counting (the current list will be emitted)
		finish = new TypedIOPort(this, "finish", true, false);
		finish.setTypeEquals(BaseType.BOOLEAN);
		new Parameter(finish, "_showName", BooleanToken.TRUE);

		// Minimum archive size in MBs
		archMinSizeMB = new Parameter(this, "archMinSizeMB", new LongToken(
				1000L));
		archMinSizeMB.setTypeEquals(BaseType.LONG);

		/*
		 * Output ports
		 */

		// the output: the record of a string which contains the name of files
		// to be archived (separated by \n) and the first and last
		// timesteps involved
		list = new TypedIOPort(this, "list", false, true);
		list.setTypeEquals(_listtype);
		new Parameter(list, "_showName", BooleanToken.TRUE);

	}

	/***********************************************************
	 * ports and parameters
	 */

	/**
	 * File info record as outputted by org.kepler.actor.io.SshDirectoryList:
	 * {name=&lt;filename&gt;, size=&lt;size in bytes&gt;, date=&lt;date in UTC
	 * seconds&gt;}
	 */
	public TypedIOPort file;

	/**
	 * Finish flag for counting. It must be false for all files to be considered
	 * in archiving. Use true flag e.g. to stop counting and emitting the last
	 * list, giving a 'fake' file for input for such firing. Type of the port:
	 * boolean
	 */
	public TypedIOPort finish;

	/**
	 * The minimum size for an archive list given in MBs. Files are gathered as
	 * long as their total sum reaches the minimum, and they represent complete
	 * timesteps.
	 */
	public Parameter archMinSizeMB;

	/**
	 * The output is record: a string containing the list of files to be
	 * archived together, and the first and last timesteps in the list. This
	 * port has a record type of {filename=&lt;string&gt;, firstTS=&lt;int&gt;,
	 * lastTS=&lt;int&gt;}.
	 */
	public TypedIOPort list;

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
		_archSize = 0L;
		_firstTimestep = -1;
		_lastTimestep = -1;
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
		LongToken archToken = (LongToken) archMinSizeMB.getToken();
		long _archMinSize = archToken.longValue();
		_archMinSize = _archMinSize * 1024 * 1024;

		// consume the tokens
		RecordToken fileInfo = (RecordToken) file.get(0);
		boolean bFinish = ((BooleanToken) finish.get(0)).booleanValue();

		// process file info if it is not about finish
		String fn = null;
		long fsize = 0L;
		int timestep = -1;

		if (!bFinish) {
			fn = ((StringToken) fileInfo.get("name")).stringValue();
			fsize = ((LongToken) fileInfo.get("size")).longValue();
			timestep = getTimestepFromFilename(fn);
		}

		// if (isDebugging)
		// log.debug("File = " + fn + ", size = " + fsize + ", ts = " + timestep
		// +
		// "\n_archSize = " + _archSize + ", firstTS = " + _firstTimestep +
		// ", lastTS = " + _lastTimestep);

		// check if we are asked to finish
		// OR timestep changed and archive is already big enough
		// OR timestep is irrelevant and we check only the size
		boolean doarch = bFinish;
		doarch = doarch
				|| (_archSize >= _archMinSize && (_lastTimestep < timestep || timestep == -1));

		// We have to avoid archiving empty lists
		doarch = doarch && _files.size() > 0;

		if (doarch) {

			// we have an archive list to publish now

			if (isDebugging)
				log
						.debug("New archive list. # of files = "
								+ _files.size() + ", size = " + _archSize
								+ ", first timestep = " + _firstTimestep
								+ ", last timestep = " + _lastTimestep);

			String listStr = createListString();
			RecordToken rt = createRecordToken(listStr, _archSize,
					_firstTimestep, _lastTimestep);

			list.send(0, rt); // emit output token now

			// restart counting
			_files = new ArrayList();
			_archSize = 0L;

		}

		if (!bFinish) {
			// we have to put the new file to the list of previous ones.
			if (_files.size() == 0)
				_firstTimestep = timestep;
			_files.add(fn);
			_archSize += fsize;
			_lastTimestep = timestep;
		} else {
			// finish (restart status except timestep counting)
			_files = new ArrayList();
			_archSize = 0L;
		}
	}

	/**
	 * Get the first number from the string. We assume that there is an
	 * extension which should be excluded. E.g. xgc.mesh.h5 will result in 0
	 */
	private int getTimestepFromFilename(String fn) {
		if (fn == null)
			return -1;
		if (!fn.matches(".*[0-9]\\..*"))
			return 0;

		int start = 0;
		int end;
		char c = fn.charAt(start);
		// look for the first number
		while (c < '0' || c > '9') {
			start++; // we must find one!
			c = fn.charAt(start);
		}

		end = start;
		// go to the end of the number
		while ('0' <= c && c <= '9') {
			end++;
			if (end < fn.length())
				c = fn.charAt(end);
			else
				break;
		}

		String tsStr;
		if (end >= fn.length())
			tsStr = fn.substring(start);
		else
			tsStr = fn.substring(start, end);
		int ts = Integer.parseInt(tsStr);
		return ts;
	}

	/*
	 * Create string of \n-separated list of file names from the stored data
	 */
	private String createListString() {
		StringBuffer sb = new StringBuffer();
		Iterator files = _files.iterator();
		while (files.hasNext()) {
			sb.append(files.next() + "\n");
		}
		return sb.toString();
	}

	/*
	 * Create one RecordToken of format {name=String, firstTS=int, lastTS=int}
	 * from the inputs.
	 */
	private RecordToken createRecordToken(String listStr, long size,
			int firstTS, int lastTS) {
		String[] labels = { "list", "size", "firstTS", "lastTS" };
		Token[] values = new Token[4];
		values[0] = new StringToken(listStr);
		values[1] = new LongToken(size);
		values[2] = new IntToken(firstTS);
		values[3] = new IntToken(lastTS);
		RecordToken rt = null;
		try {
			rt = new RecordToken(labels, values);
		} catch (IllegalActionException ex) {
			log
					.error("ArchiveCount: Error at creating a record token for the archive list of "
							+ "size = "
							+ size
							+ ", firstTS = "
							+ firstTS
							+ ", lastTS = " + lastTS + ", list:\n " + listStr);
		}
		return rt;
	}

	private Type _filetype;
	private Type _listtype;

	private long _archSize = 0L;
	private int _firstTimestep = -1;
	private int _lastTimestep = -1;
	private ArrayList _files; // of file info RecordTokens

	private static final Log log = LogFactory.getLog(ArchiveCounter.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

}
