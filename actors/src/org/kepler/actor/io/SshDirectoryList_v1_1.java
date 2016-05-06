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

import java.util.Arrays;

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
import ptolemy.data.IntToken;
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
 * The underlying command is 'ls -l' to list a directory for all masks on a remote machine. 
 * In case of a parallel file system 
 * and a directory of thousands of files, this can be a very costly operation. If you do 
 * not need the date and size information, check 'useLsOnly'. This will use the 'ls' command
 * to list a directory and both size and date will be set to -1. If 'checkSizeAndDate' is 
 * set, then do not set this flag because size and date values cannot be compared with
 * previous listing.
 * This operation should be much less costly than listing with 'ls -l'.
 * Note, that if you are looking for modified files (checkSizeAndDate) and you have a 
 * ? or * in the mask, the 'ls -l &lt;mask&gt;' command will ls the whole directory and
 * thus can be costly. 
 * 
 * </p>
 * <p>
 * On error, an empty string is returned.
 * 
 * </p>
 * <p>
 * Note: the trigger port is needed only if you do not connect any of the other
 * ports.
 * </p>
 * 
 * @author Norbert Podhorszki
 * @version $Id: SshDirectoryList_v1_1.java 14369 2008-03-12 22:11:35Z podhorsz$
 * @since Ptolemy II 5.0.1
 */
public class SshDirectoryList_v1_1 extends TypedAtomicActor {
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
	public SshDirectoryList_v1_1(CompositeEntity container, String name)
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

		// masks are the file masks for the files to be listed
		masks = new PortParameter(this, "masks", new ArrayToken("{'*'}"));
		new Parameter(masks.getPort(), "_showName", BooleanToken.TRUE);

		// a trigger to read it again
		trigger = new TypedIOPort(this, "trigger", true, false);
		trigger.setTypeEquals(BaseType.UNKNOWN);
		new Parameter(trigger, "_showName", BooleanToken.FALSE);

		// new files only or all?
		newFilesOnly = new Parameter(this, "newFilesOnly", new BooleanToken(true));
		newFilesOnly.setTypeEquals(BaseType.BOOLEAN);

		// check file changes in size and date? Or just look for brand new
		// files?
		checkSizeAndDate = new Parameter(this, "checkSizeAndDate",
				new BooleanToken(false));
		checkSizeAndDate.setTypeEquals(BaseType.BOOLEAN);

		// output empty array if nothing found?
		sendEmpty = new Parameter(this, "sendEmpty", new BooleanToken(true));
		sendEmpty.setTypeEquals(BaseType.BOOLEAN);

		// Delay the listing with one step?
		delay = new Parameter(this, "delay", new BooleanToken(false));
		delay.setTypeEquals(BaseType.BOOLEAN);

		// use 'ls -l' or just ls to be faster
		useLsOnly = new Parameter(this, "useLsOnly", new BooleanToken(false));
		useLsOnly.setTypeEquals(BaseType.BOOLEAN);

		// Frequency of checking
		frequencySec = new Parameter(this, "frequencySec", new IntToken(0));
		frequencySec.setTypeEquals(BaseType.INT);

		// stop mask is the file masks for which this actor should stop when
		// found
		stopmask = new PortParameter(this, "stopmask", new StringToken(""));
		new Parameter(stopmask.getPort(), "_showName", BooleanToken.TRUE);

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
	 */
	public PortParameter target;

	/**
	 * The path to the directory to be read on the target machines.
	 */
	public PortParameter dir;

	/**
	 * The file masks for listing only such files.
	 */
	public PortParameter masks;

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
	 * Specifying whether the underlying command should be only an 'ls' instead
	 * of the default 'ls -l'. If not 'checkSizeAndDate', size and date fields
	 * in the output will be -1.
	 */
	public Parameter useLsOnly;

	/**
	 * Specifying whether an empty ArrayToken should be emitted if nothing is
	 * found. Needed for SDF like directors or any workflow that needs to keep
	 * the input-output token ratio constant.
	 */
	public Parameter sendEmpty;

	/**
	 * Specifying whether the output listings should be delayed by one step.
	 * This option can be used to ensure that we list 'not so new' files, so
	 * avoid that a new file is listed which is still under construction by
	 * another program. If delay is set, sendEmpty will be ignored.
	 */
	public Parameter delay;

	/**
	 * Specifying whether this actor should loop inside and check for files
	 * regularly and stop only when a file that matches the stopmask is found.
	 */
	public Parameter frequencySec;

	/**
	 * The stop file mask for stopping when such file(s) found. Meaningful only
	 * if frequency is set to be greater than zero.
	 */
	public PortParameter stopmask;

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
		if (trigger.getWidth() > 0) {
			// if (!((BooleanToken) trigger.get(0)).equals(null)) {
			trigger.get(0);
			// log.debug("consume the token at the trigger port.");
			// }
		}

		// save previous values
		_prevtarget = _target;
		_prevdir = _dir;
		_prevmasks = _masks;
		_prevstopmask = _stopmask;

		// update PortParameters
		target.update();
		dir.update();
		masks.update();
		stopmask.update();
		_target = ((StringToken) target.getToken()).stringValue().trim();
		_dir = ((StringToken) dir.getToken()).stringValue().trim();
		_stopmask = ((StringToken) stopmask.getToken()).stringValue().trim();

		// process array of masks
		ArrayToken maskTokens = (ArrayToken) masks.getToken();
		if (maskTokens.length() >= 1) {
			_masks = new String[maskTokens.length()];
			for (int i = 0; i < maskTokens.length(); i++) {
				_masks[i] = ((StringToken) maskTokens.getElement(i))
						.stringValue().trim();
				log.debug(this.getDisplayName() + ": Mask added: " + _masks[i]);
			}
		}

		FileInfo[] _newFiles = null;

		// if target or dir changes, we have to create a new DirectoryListing
		// object
		if (_firstFire || !_prevtarget.equals(_target)
				|| !_prevdir.equals(_dir)) {

			_newOnly = ((BooleanToken) newFilesOnly.getToken()).booleanValue();
			_checkModifications = ((BooleanToken) checkSizeAndDate.getToken())
					.booleanValue();
			_freq = ((IntToken) frequencySec.getToken()).intValue();
			_sendEmpty = ((BooleanToken) sendEmpty.getToken()).booleanValue();
			_useLsOnly = ((BooleanToken) useLsOnly.getToken()).booleanValue();
			_delay = ((BooleanToken) delay.getToken()).booleanValue();

			// constraint: delay & sendEmpty = False
			if (_delay && _sendEmpty) {
				log
						.error(this.getFullName()
								+ ": Both sendEmpty and delay is set to true, which is not allowed. Delay will be considered as true, sendEmpty as false.");
				_sendEmpty = false;
			}

			// set initial values
			_delayedList = null;

			if (isDebugging)
				log.debug(this.getDisplayName()
						+ ": Create DirectoryListing object: " + "target = "
						+ _target + "; dir = " + _dir + "; filemasks = "
						+ _masks + "; newFilesOnly = " + _newOnly
						+ "; checkSizeAndDate = " + _checkModifications);

			_dl = new DirectoryListing(_target, _dir, _masks);
			if (_stopmask != null && !_stopmask.equals("")) {
				String[] sa = { _stopmask };
				_dl_stop = new DirectoryListing(_target, _dir, sa);
			}
			_firstFire = false;

		} else {
			if (!Arrays.equals(_masks, _prevmasks)) { // masks can be reset
				_dl.setMask(_masks);
				_prevmasks = _masks;
			}
			if (!_stopmask.equals(_prevstopmask)) { // stopmask can be reset
				String[] sa = { _stopmask };
				_dl_stop.setMask(sa);
				_prevstopmask = _stopmask;
			}
		}

		// List directory in a loop if frequency is set, until a stop file is
		// found
		boolean doloop = true;
		while (doloop) {

			try {
				// list directory for the given masks
				int n = _dl.list(_useLsOnly);
				if (isDebugging)
					log.debug(this.getDisplayName()
							+ ": Number of total files = " + n);
				if (n > 0) {
					// get new files
					if (_newOnly)
						_newFiles = _dl.getNewFiles(_checkModifications);
					else
						_newFiles = _dl.getList();

					if (isDebugging || _newFiles.length > 0)
						log.info(this.getDisplayName() + ": Number of "
								+ (_newOnly ? "NEW" : "") + " files = "
								+ _newFiles.length);
				}

			} catch (ExecException ex) {
				log
						.error(this.getFullName()
								+ ": SshDirectoryList error at remote directory reading. "
								+ ex);
			}

			// create the result and send
			if (_newFiles != null && _newFiles.length > 0) {
				RecordToken[] rt = new RecordToken[_newFiles.length];
				for (int i = 0; i < _newFiles.length; i++) {
					rt[i] = createRecordToken(_newFiles[i]);
				}
				if (_delay) { // postpone the current list and send the previous
								// if there is such
					if (_delayedList != null) {
						newFiles.send(0, new ArrayToken(_etype, _delayedList));
						log.debug(this.getDisplayName()
								+ ": Send delayed list: " + _delayedList);
					}
					_delayedList = rt;
				} else { // send the new list
					newFiles.send(0, new ArrayToken(_etype, rt));
					log.debug(this.getDisplayName() + ": Found new files: "
							+ rt);
				}
			} else if (_sendEmpty) { // send an empty array of RecordTokens
				newFiles.send(0, new ArrayToken(_etype, new RecordToken[0]));
				log.debug(this.getDisplayName()
						+ ": Found no files at this time.");
			}

			if (_freq == 0)
				doloop = false; // exit from loop if we need to run once
			else { // check for the stop files and then sleep if not found
					// anything
				try {
					// list directory for stop files
					int n = _dl_stop.list();
					if (isDebugging)
						log.debug(this.getDisplayName()
								+ ": Number of total stop files = " + n);
					if (n > 0) {
						doloop = false; // exit from loop if found any stop file
						_newFiles = _dl_stop.getList(); // we have to send them
														// out
						RecordToken[] rt = new RecordToken[_newFiles.length];
						for (int i = 0; i < _newFiles.length; i++) {
							rt[i] = createRecordToken(_newFiles[i]);
						}
						if (_delayedList != null) { // we have to send out the
													// stored list in delayed
													// mode
							newFiles.send(0, new ArrayToken(_etype,
									_delayedList));
							log.debug(this.getDisplayName()
									+ ": Send delayed list: " + _delayedList);
						}
						newFiles.send(0, new ArrayToken(_etype, rt));
						log.debug(this.getDisplayName()
								+ ": Found stop files: " + _newFiles);
					}
				} catch (ExecException ex) {
					log
							.error(this.getFullName()
									+ ": SshDirectoryList error at remote directory reading. "
									+ ex);
				}

				if (doloop) // if we are still looping, let sleep a bit
					try {
						java.lang.Thread.sleep(_freq * 1000L);
					} catch (InterruptedException ex) {
					}
			}

		} // end while loop

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
					.error(this.getFullName()
							+ ": SshDirectoryList: Error at creating a record token for fileinfo: "
							+ fi + "\nlabels = " + labels + "\nvalues = "
							+ values);
		}
		return rt;
	}

	private boolean _firstFire;
	private String _target;
	private String _dir;
	private String[] _masks;
	private String _stopmask;
	private String _prevtarget;
	private String _prevdir;
	private String[] _prevmasks;
	private String _prevstopmask;
	private boolean _newOnly;
	private boolean _checkModifications;
	private int _freq;
	private boolean _sendEmpty;
	private boolean _useLsOnly;
	private boolean _delay;
	private RecordToken[] _delayedList;
	private DirectoryListing _dl; // the major directory listing object
	private DirectoryListing _dl_stop; // separate listing for the stop mask
	private Type _etype;

	private static final Log log = LogFactory
			.getLog(SshDirectoryList_v1_1.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

}