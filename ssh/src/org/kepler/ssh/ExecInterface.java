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

package org.kepler.ssh;

import java.io.File;
import java.io.OutputStream;
import java.util.Collection;

/**
 * This interface defines functionality to execute local/remote commands and to
 * transfer files. It is implemented by LocalExec and RemoteExec classes.
 * 
 * <p>
 * 
 * @author Norbert Podhorszki
 */

public interface ExecInterface {

	/**
	 * Execute a command externally. The streams <i>streamOut</i> and
	 * <i>streamErr</i> should be provided to get the output and errors.
	 * 
	 * @return exit code of command if execution succeeded, throws
	 *         ExecException, which can also be an instance of
	 *         ExecTimeoutException
	 */
	public abstract int executeCmd(String command, OutputStream streamOut,
			OutputStream streamErr) throws ExecException;

	/**
	 * Execute a command externally where the command connects to a third party
	 * and needs to authenticate to that third party. The streams
	 * <i>streamOut</i> should be provided to get the output and errors (merged
	 * together in case of remote targets).
	 * 
	 * @return exit code of command if execution succeeded, throws
	 *         ExecException, which can also be an instance of
	 *         ExecTimeoutException
	 */
	public abstract int executeCmd(String command, OutputStream streamOut,
			OutputStream streamErr, String thirdParty) throws ExecException;

	/**
	 * Create directory. It should be relative to the user's home dir in case of
	 * remote machines, or to the current dir in case of local machine, or an
	 * absolute path. If parentflag is true, parent directories in the path
	 * should be created as well if necessary. Moreover, in this case, if the
	 * directory already exists, this method should return true (otherwise it
	 * returns an error).
	 * 
	 * @return true if succeeded
	 */
	public boolean createDir(String dir, boolean parentflag)
			throws ExecException;

	/**
	 * Delete file or directory on the local/remote site It should be relative
	 * to the user's home dir in case of remote machines, or to the current dir
	 * in case of local machine, or an absolute path For safety, * and ? is
	 * allowed in filename string only if explicitely asked with allowFileMask =
	 * true If you want to delete a directory, recursive should be set to true
	 * 
	 * @return true if succeeded
	 */
	public boolean deleteFile(String fname, boolean recursive,
			boolean allowFileMask) throws ExecException;

	/**
	 * Copy local files to a local/remote directory Input: files is a Collection
	 * of files of type File, targetPath is either a directory in case of
	 * several files, or it is either a dir or filename in case of one single
	 * local file recursive: true if you want traverse directories The files can
	 * have wildcards in their names but not in their path.
	 * 
	 * @return number of files copied successfully
	 */
	public int copyTo(Collection files, String targetPath, boolean recursive)
			throws ExecException;

	/**
	 * Copy a local file to a local/remote directory Input: file of type File
	 * (which can be a directory) The input can have wildcards in its name but
	 * not in its path. targetPath is either a directory or filename
	 * 
	 * @return number of files copied successfully (i.e either returns true or
	 *         an exception is raised)
	 */
	public int copyTo(File lfile, String targetPath, boolean recursive)
			throws ExecException;

	/**
	 * Copy files from a local/remote directory to a local path Input: 'files'
	 * is a Collection of files of type String (! not like at copyTo !),
	 * 'sourcePath' is either empty string (or null) in case the 'files' contain
	 * full paths to the individual files, or it should be a directory, and in
	 * this case each file name in 'files' will be extended with the directory
	 * name before copy. 'localPath' should be a directory name in case of
	 * several files. It can be a filename in case of a single file to be
	 * copied. This is a convenience method for copyFrom on several remote
	 * files. recursive: true if you want traverse directories
	 * 
	 * @return number of files copied successfully
	 */
	public int copyFrom(String sourcePath, Collection files, File localPath,
			boolean recursive) throws ExecException;

	/**
	 * Copy a local/remote file into a local file Input: 'sourcePath' of type
	 * String (can be a directory or filename) 'localPath' is either a directory
	 * or filename Only if 'recursive' is set, will directories copied
	 * recursively.
	 * 
	 * @return number of files copied successfully (i.e either returns true or
	 *         an exception is raised)
	 */
	public int copyFrom(String sourcePath, File localPath, boolean recursive)
			throws ExecException;

	/**
	 * Specify if killing of external processes (i.e. clean-up) after error or
	 * timeout is required. Not implemented for local execution. Use only if you
	 * are connecting to a unix machine.
	 */
	public void setForcedCleanUp(boolean foo);

	/**
	 * Set timeout for the operations. Timeout should be given in seconds. If
	 * 'stdout' is set to true, the timer is restarted whenever there is data on
	 * stdout. If 'stderr' is set to true, the timer is restarted whenever there
	 * is data on stderr. An operation will throw an ExecException, an instance
	 * of ExecTimeoutException if the timeout limit is reached. 'seconds' = 0
	 * means no timeout at all.
	 */
	public void setTimeout(int seconds, boolean stdout, boolean stderr);

	/**
	 * Set timeout for the operations.
	 */
	public void setTimeout(int seconds);

        /**
         * addIdentity, useless for local exec 
         */
        public void addIdentity(String identity);

	/**
	 * port forwarding not working on local exec
	 */
	public void setPortForwardingL(String spec) 
                        throws ExecException;

	/**
	 * port forwarding not working on local exec
	 */
	public void setPortForwardingR(String spec) 
                        throws ExecException;

        public boolean openConnection() 
                        throws ExecException;

        public void closeConnection();

} // end-of-interface

