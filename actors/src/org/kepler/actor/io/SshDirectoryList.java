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

package org.kepler.actor.io;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.io.DirectoryListing;
import org.kepler.io.FileInfo;
import org.kepler.ssh.ExecException;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.LongToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.RecordType;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// SshDirectoryList

/**
 * <p>
 * <b>Obsolete actor.</b> Use SshDirectoryList-v1.1 instead.
 * </p>
 * <p>
 * Actor for directory listing.
 * <ul>
 * <li>local or remote dir (using ssh)</li>
 * <li>get new files from previous call</li>
 * </ul>
 * 
 * </p>
 * <p>
 * This actor uses org.kepler.io.DirectoryListing class to get file listing
 * 
 * </p>
 * <p>
 * The initial input should be:
 * <ul>
 * <li>the target machine, either null, "" or "local" to denote the local
 * machine to be used by Java I/O commands, OR "[user@]host[:port]" to denote a
 * remote machine to be used by an ssh connection.</li>
 * <li>the directory to be listed as String</li>
 * <li>the file mask for listing files according to the pattern only, "" means
 * all files</li>
 * </ul>
 * 
 * </p>
 * <p>
 * The actor takes the target and directory arguments only once! After that it
 * works with that setting at each fire. The mask can be reset to something else
 * for each firing but in such a case, the previous listings are deleted (as if
 * starting fresh).
 * 
 * </p>
 * <p>
 * Depending on the boolean parameter 'newFilesOnly' there are two different
 * behavior: <br/>
 * If 'newFilesOnly' is set (default), for each trigger, the file list is
 * updated, and the array of 'new' files will be the output. 'New' means the
 * difference between the current and the previous listings. <br/>
 * If 'newFilesOnly' is not set, for each trigger, the whole list is returned.
 * This can be used for watching a specific file and look for changes in size or
 * access time of the file.
 * 
 * </p>
 * <p>
 * Depending on the boolean parameter 'checkSizeAndDate' the meaning 'new' is
 * handled as following: <br/>
 * If 'checkSizeAndDate' is set, modified files are also listed besides brand
 * new files. I.e., a new file is that is not found in previous listing OR the
 * size or date of the file has changed between the previous and current
 * listings. <br/>
 * If 'checkSizeAndDate' is not set (default), only brand new files are listed.
 * I.e. only the file names are checked, and file modifications have no effect.
 * 
 * </p>
 * <p>
 * The files are listed in the following format: Each element in the array will
 * be a RecordToken: {name=String, size=long, date=long}, where the size is
 * given in bytes, the date is given in UTC seconds. The resolution depends on
 * the 'ls -l' output, i.e. at most a minute, and for old files a day. In the
 * local case, it depends on the resolution of Java and the local OS, usually
 * less than a millisecond.
 * 
 * </p>
 * <p>
 * On error, an empty string is returned.
 * 
 * </p>
 * <p>
 * Note: even if you pass a token to the mask port parameter, you must pass a
 * token to the trigger port as well anyway.
 * </p>
 * 
 * @author Norbert Podhorszki
 * @version $Id: SshDirectoryList.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 5.0.1
 */
public class SshDirectoryList extends TypedAtomicActor {
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
	public SshDirectoryList(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// Uncomment the next line to see debugging statements
		// addDebugListener(new ptolemy.kernel.util.StreamListener());

		// target selects the machine where the directory is to be accessed
		target = new PortParameter(this, "target", new StringToken(
				"[ local | [user@]host[:port] ]"));
		new Parameter(target.getPort(), "_showName", BooleanToken.TRUE);

		// dir is the path to the directory to be listed on the target machine
		dir = new PortParameter(this, "dir", new StringToken("/path/to/dir"));
		new Parameter(dir.getPort(), "_showName", BooleanToken.TRUE);

		// mask is the file mask for the files to be listed
		mask = new PortParameter(this, "mask", new StringToken("*"));
		new Parameter(mask.getPort(), "_showName", BooleanToken.TRUE);

		// a trigger to read it again
		trigger = new TypedIOPort(this, "trigger", true, false);
		trigger.setTypeEquals(BaseType.UNKNOWN);
		new Parameter(trigger, "_showName", BooleanToken.FALSE);

		// new files only or all?
		newFilesOnly = new Parameter(this, "newFilesOnly", new BooleanToken(
				true));
		newFilesOnly.setTypeEquals(BaseType.BOOLEAN);

		// new files only or all?
		checkSizeAndDate = new Parameter(this, "checkSizeAndDate",
				new BooleanToken(false));
		checkSizeAndDate.setTypeEquals(BaseType.BOOLEAN);

		// the output: an array of filenames
		newFiles = new TypedIOPort(this, "newFiles", false, true);
		String[] labels = { "name", "size", "date" };
		Type[] ctypes = { BaseType.STRING, BaseType.LONG, BaseType.LONG };
		_etype = new RecordType(labels, ctypes);
		newFiles.setTypeEquals(new ArrayType(_etype));
		// newFiles.setTypeAtMost(new RecordType(new String[0], new Type[0]));
		// newFiles.setTypeEquals(new ArrayType(BaseType.UNKNOWN));
		// newFiles.setTypeEquals(new BaseType.UNKNOWN);
		new Parameter(newFiles, "_showName", BooleanToken.FALSE);
	}

	/***********************************************************
	 * ports and parameters
	 */

	/**
	 * The machine to be used at job submission. It should be null, "" or
	 * "local" for the local machine or [user@]host[:port] to denote a remote
	 * machine accessible with ssh.
	 * 
	 * This parameter is read once at initialize.
	 */
	public PortParameter target;

	/**
	 * The path to the directory to be read on the target machines. This
	 * parameter is read once at initialize.
	 */
	public PortParameter dir;

	/**
	 * The file mask for listing only such files. This parameter is read once at
	 * initialize.
	 */
	public PortParameter mask;

	/**
	 * The trigger port to do the reading. This port is an output port of type
	 * ArrayToken.
	 */
	public TypedIOPort trigger;

	/**
	 * Specifying whether the output should contain only new files or all. 'New'
	 * means the difference in the current and previous listing.
	 */
	public Parameter newFilesOnly;

	/**
	 * Specifying whether the output should contain modified files as well, or
	 * only brand new files.
	 */
	public Parameter checkSizeAndDate;

	/**
	 * The output: array of new files since the previous read. This port is an
	 * output port of type ArrayToken. Each element is a RecordToken of
	 * {name=String, size=long, date=long}
	 */
	public TypedIOPort newFiles;

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
		_firstFire = true;
	}

	/**
	 * fire
	 * 
	 * @exception IllegalActionException
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		// consume the trigger token
		trigger.get(0);

		FileInfo[] _newFiles = null;
		if (_firstFire) {
			// update PortParameters
			target.update();
			dir.update();
			mask.update();

			_target = ((StringToken) target.getToken()).stringValue();
			_dir = ((StringToken) dir.getToken()).stringValue();
			_mask = ((StringToken) mask.getToken()).stringValue();
			_prevmask = _mask;
			_newOnly = ((BooleanToken) newFilesOnly.getToken()).booleanValue();
			_checkModifications = ((BooleanToken) checkSizeAndDate.getToken())
					.booleanValue();

			if (isDebugging)
				log.debug("Create DirectoryListing object: " + "target = "
						+ _target + "; dir = " + _dir + "; filemask = " + _mask
						+ "; newFilesOnly = " + _newOnly
						+ "; checkSizeAndDate = " + _checkModifications);

			String masks[] = new String[1];
			masks[0] = _mask;
			_dl = new DirectoryListing(_target, _dir, masks);

			_firstFire = false;
		} else {
			// mask can be reset
			mask.update();
			_mask = ((StringToken) mask.getToken()).stringValue();
			if (!_mask.equals(_prevmask)) {
				String masks[] = new String[1];
				masks[0] = _mask;
				_dl.setMask(masks);
				_prevmask = _mask;
			}
		}

		try {
			// list directory
			int n = _dl.list();
			if (isDebugging)
				log.debug("Number of total files = " + n);
			if (n > 0) {
				// get new files
				if (_newOnly)
					_newFiles = _dl.getNewFiles(_checkModifications);
				else
					_newFiles = _dl.getList();

				if (isDebugging || _newFiles.length > 0)
					log.info("Number of " + (_newOnly ? "NEW" : "")
							+ " files = " + _newFiles.length);
			}

		} catch (ExecException ex) {
			log.error("SshDirectoryList error at remote directory reading. "
					+ ex);
		}

		// create the result
		ArrayToken at;
		if (_newFiles != null && _newFiles.length > 0) {
			RecordToken[] rt = new RecordToken[_newFiles.length];
			for (int i = 0; i < _newFiles.length; i++) {
				rt[i] = createRecordToken(_newFiles[i]);
				// newFiles.send(0, rt[i]);
			}
			at = new ArrayToken(_etype, rt);
		}
		// else at = ArrayToken.NIL;
		else
			at = new ArrayToken(_etype, new RecordToken[0]);

		newFiles.send(0, at);
	}

	/*
	 * Create one RecordToken of format {name=String, size=long, date=long} from
	 * the FileInfo struct.
	 */
	private RecordToken createRecordToken(FileInfo fi) {
		String[] labels = { "name", "size", "date" };
		Token[] values = new Token[3];
		values[0] = new StringToken(fi.getName());
		values[1] = new LongToken(fi.getSize());
		values[2] = new LongToken(fi.getDate());
		RecordToken rt = null;
		try {
			rt = new RecordToken(labels, values);
		} catch (IllegalActionException ex) {
			log
					.error("SshDirectoryList: Error at creating a record token for fileinfo: "
							+ fi
							+ "\nlabels = "
							+ labels
							+ "\nvalues = "
							+ values);
		}
		return rt;
	}

	private boolean _firstFire;
	private String _target;
	private String _dir;
	private String _mask;
	private String _prevmask;
	private boolean _newOnly;
	private boolean _checkModifications;
	private DirectoryListing _dl;
	private Type _etype;

	private static final Log log = LogFactory.getLog(SshDirectoryList.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

}