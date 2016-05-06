/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2012-10-30 15:37:51 -0700 (Tue, 30 Oct 2012) $' 
 * '$Revision: 30990 $'
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.util.FilenameFilter_RegularPattern;

import ptolemy.util.StringUtilities;

/**
 * Local command execution. This class implements the ExecInterface to provide
 * the same functionality for local operations as what Ssh does for remote ones.
 * Thus, other classes can hide the difference between an ssh execution and
 * local execution.
 */
public class LocalExec implements ExecInterface {

	private static final Log log = LogFactory.getLog(LocalExec.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	private static int nInstances = 0; // to catch the very first instantiation

	// timeout variables
	private int timeout = 0; // timeout in seconds
	private boolean timeoutRestartOnStdout = false; // restart timer if stdout
													// has data
	private boolean timeoutRestartOnStderr = false; // restart timer if stderr
													// has data

	// public final static int timeoutErrorCode = -32767;

	public LocalExec() {
		_commandCount = getSystemProps();
		nInstances++;

		/*
		 * On local host we have no session opening/closing, so this is the
		 * place to generate a SESSION_OPENED event, but only once (and there
		 * will be no SESSION_CLOSED event)
		 */
		if (nInstances == 1){
			SshEventRegistry.instance.notifyListeners(new SshEvent(
					SshEvent.SESSION_OPENED, "local"));
		}

	}

	/**
	 * addIdentity, useless for local exec 
	 */
	public void addIdentity(String identity) { 
		// do nothing 
	}

	/**
	 * port forwarding not working on local exec
	 */
	public void setPortForwardingL(String spec) throws ExecException {
            // do nothing
	}

	/**
	 * port forwarding not working on local exec
	 */
	public void setPortForwardingR(String spec) throws ExecException {
            // do nothing
	}

        public boolean openConnection() throws ExecException {
            return true;
        }

        public void closeConnection() {
            // do nothing
        }

	/**
	 * Specify if killing of external processes (i.e. clean-up) after error or
	 * timeout is required. Not implemented for local execution.
	 */
	public void setForcedCleanUp(boolean foo) {
	}

	/**
	 * Set timeout for the operations. Timeout should be given in seconds. If
	 * 'stdout' is set to true, the timer is restarted whenever there is data on
	 * stdout. If 'stderr' is set to true, the timer is restarted whenever there
	 * is data on stderr. executeCmd will throw an ExecException, an instance of
	 * ExecTimeoutException if the timeout limit is reached. 'seconds' = 0 means
	 * no timeout at all.
	 */
	public void setTimeout(int seconds, boolean stdout, boolean stderr) {
		timeout = seconds;
		timeoutRestartOnStdout = stdout;
		timeoutRestartOnStderr = stderr;
	}
	
	public void setTimeout(int seconds) {
		timeout = seconds;
	}

	/**
	 * Execute a command on the local machine 'command' is the full command with
	 * arguments to be executed return: the exit code of the command additional
	 * effects: streamOut is continuously written during execution the stdout of
	 * the command. Similarly, streamErr is continuously written during exec the
	 * stderr. It forwards all Exceptions that arise during java exec.
	 */
	public int executeCmd(String command, OutputStream streamOut,
			OutputStream streamErr) throws ExecException {
		return executeCmd(command, streamOut, streamErr, null);
	}

	public int executeCmd(String command, OutputStream streamOut,
			OutputStream streamErr, String thirdPartyTarget)
			throws ExecException {
		_commandArr[_commandCount] = command;

		Runtime rt = Runtime.getRuntime();
		Process proc;

		// get the pwd/passphrase to the third party (and perform authentication
		// if not yet done)
		String pwd = SshSession.getPwdToThirdParty(thirdPartyTarget);
		

		try {
			proc = rt.exec(_commandArr);
		} catch (Exception ex) {
			//ex.printStackTrace();
			throw new ExecException("Cannot execute cmd ** : " + _commandArr[_commandCount] + ex);
		}

		// System.out.println("%%% Process started");

		// the streams from the process: stdout and stderr
		BufferedReader out_in = new BufferedReader(new InputStreamReader(proc
				.getInputStream())); // stdout
		BufferedReader err_in = new BufferedReader(new InputStreamReader(proc
				.getErrorStream())); // stderr

		// the streams towards the caller: stdout and stderr
		BufferedWriter out_out = new BufferedWriter(new OutputStreamWriter(
				streamOut));
		BufferedWriter err_out = new BufferedWriter(new OutputStreamWriter(
				streamErr));

		BufferedWriter proc_in = new BufferedWriter(new OutputStreamWriter(proc
				.getOutputStream())); // stdin

		String line; // Temp for each line of output.
		int exitVal = -32766;
		boolean readOut = true;
		boolean readErr = true;
		boolean finished = false;
		boolean checkForPwd = (pwd != null);
		char c[] = new char[256];
		int charsRead;

		// variables for the timeout checking
		long start = System.currentTimeMillis();
		long current = 0;
		long maxtime = timeout * 1000L;

		while (!finished) { // will stop when the process terminates or after
							// timeout
			// check the status of the process
			try {
				exitVal = proc.exitValue();
				finished = true; // process terminated so exit this loop after
									// reading the buffers
			} catch (IllegalThreadStateException ex) {
				// process not yet terminated so we go further
			}

			// read stdout
			if (readOut) {
				try {
					while (out_in.ready()) {
						charsRead = out_in.read(c, 0, 256);
						out_out.write(c, 0, charsRead);

						// System.out.println("%%% "+ new String(c, 0,
						// charsRead));
						/*
						 * try { proc_in.write("Anyadat\n", 0, 8); // send the
						 * password proc_in.flush(); } catch (Exception ex) {
						 * System.out.println("### "+ex);
						 * 
						 * }
						 */
						if (checkForPwd
								&& containsPasswordRequest(c, 0, charsRead)) {

							// System.out.println("%%% Found password request");

							out_out.flush(); // so you may see the request on
												// stdout already
							proc_in.write(pwd + "\n", 0, pwd.length() + 1); // send
																			// the
																			// password
							proc_in.flush();
							log.info("Sent password to third party.");
							checkForPwd = false; // even if it's wrong, do not
													// do it again
						}
						if (timeoutRestartOnStdout)
							start = System.currentTimeMillis(); // restart
																// timeout timer
					}
				} catch (IOException ioe) {
					log.error("<IOException> when reading the stdout: " + ioe
							+ "</IOException>");
					readOut = false;
				}
			}

			// read stderr
			if (readErr) {
				try {
					while (err_in.ready()) {
						charsRead = err_in.read(c, 0, 256);
						err_out.write(c, 0, charsRead);
						System.out
								.println("### " + new String(c, 0, charsRead));
						if (checkForPwd
								&& containsPasswordRequest(c, 0, charsRead)) {

							System.out.println("### Found password request");

							out_out.flush(); // so you may see the request on
												// stdout already
							proc_in.write(pwd + "\n", 0, pwd.length() + 1); // send
																			// the
																			// password
							proc_in.flush();
							log.info("Sent password to third party.");
							checkForPwd = false; // even if it's wrong, do not
													// do it again
						}
						if (timeoutRestartOnStderr)
							start = System.currentTimeMillis(); // restart
																// timeout timer
					}
				} catch (IOException ioe) {
					log.error("<IOException> when reading the stderr: " + ioe
							+ "</IOException>");
					readErr = false;
				}
			}

			// sleep a bit to not overload the system
			if (!finished)
				try {
					java.lang.Thread.sleep(100);
				} catch (InterruptedException ex) {
				}

			// check timeout
			current = System.currentTimeMillis();
			if (timeout > 0 && maxtime < current - start) {
				log.error("Timeout: " + timeout + "s elapsed for command "
						+ command);
				proc.destroy();
				throw new ExecTimeoutException(command);
				// exitVal = timeoutErrorCode;
				// finished = true;
			}

		}

		try {
			// flush to caller
			out_out.flush();
			err_out.flush();
			// close streams from/to child process
			out_in.close();
			err_in.close();
			proc_in.close();
		} catch (IOException ex) {
			log.error("Could not flush output streams: " + ex);
		}

		// System.out.println("ExitValue: " + exitVal);
		return exitVal;

	}

	/**
	 * Look for one of the strings password/passphrase/passcode in the char[]
	 * array. Return true if found any. Case insensitive search.
	 */
	private boolean containsPasswordRequest(char[] buf, int startPos, int endPos) {
		// look for strings password/passphrase/passcode
		int i = startPos;
		while (i +7 < endPos) {
			if (Character.toLowerCase(buf[i]) == 'p'
					&& Character.toLowerCase(buf[i + 1]) == 'a'
					&& Character.toLowerCase(buf[i + 2]) == 's'
					&& Character.toLowerCase(buf[i + 3]) == 's') {

				// found "pass", look further for word/code/phrase
				if (Character.toLowerCase(buf[i + 4]) == 'w'
						&& Character.toLowerCase(buf[i + 5]) == 'o'
						&& Character.toLowerCase(buf[i + 6]) == 'r'
						&& Character.toLowerCase(buf[i + 7]) == 'd') {
					log.info("PWDSearch: found request for password.");
					return true;
				} else if (Character.toLowerCase(buf[i + 4]) == 'c'
						&& Character.toLowerCase(buf[i + 5]) == 'o'
						&& Character.toLowerCase(buf[i + 6]) == 'd'
						&& Character.toLowerCase(buf[i + 7]) == 'e') {
					log.info("PWDSearch: found request for passcode.");
					return true;
        			} else if ((i + 9 < endPos)
					&& (Character.toLowerCase(buf[i + 4]) == 'p'
						&& Character.toLowerCase(buf[i + 5]) == 'h'
						&& Character.toLowerCase(buf[i + 6]) == 'r'
						&& Character.toLowerCase(buf[i + 7]) == 'a'
						&& Character.toLowerCase(buf[i + 8]) == 's'
						&& Character.toLowerCase(buf[i + 9]) == 'e')) {
					log.info("PWDSearch: found request for passphrase.");
					return true;
				}
			}
			i = i + 1;
		}
		return false;
	}

	/**
	 * Create a directory given as String parameter. It calls File.mkdir() or
	 * File.mkdirs() depending on the parentflag. Returns true iff directory is
	 * created. False is not returned but an exception otherwise. It catches
	 * SecurityException (from File.mkdir() or File.mkdirs()) and rethrows it as
	 * ExecException.
	 * 
	 * The method works equivalently with SshExec.createDir(). That is, if the
	 * directory exists and parentflag is set, true is returned; if parentflag
	 * is not set, an exception is thrown.
	 */
	public boolean createDir(String dir, boolean parentflag)
			throws ExecException {

		if (dir == null || dir.trim().length() == 0) {
			log.error("Directory name not given");
			return false;
		}

		boolean b = false;
		try {
			File d = new File(dir);

			// error check: a file with this name exists and is not a directory
			if (d.exists() && !d.isDirectory()) {
				throw new ExecException("File " + dir
						+ " exists but is not a directory.");
			}

			// error check: directory exists
			if (d.isDirectory())
				if (parentflag) // parentflag is set: we are done and return
								// success
					return true;
				else
					// parentflag is not set: we should return error
					throw new ExecException("Directory " + dir
							+ " already exists.");

			// call mkdir or mkdirs
			if (parentflag)
				b = d.mkdirs();
			else
				b = d.mkdir();

			if (!b) {
				throw new ExecException("Directory " + dir
						+ " has NOT been created for unknown reasons");
			}
		} catch (SecurityException ex) {
			throw new ExecException("Security error: " + ex);
		}

		return b;
	}

	/**
	 * To be implemented. Delete files or directories! BE CAREFUL It should be
	 * relative to the current dir, or an absolute path For safety, * and ? is
	 * allowed in filename string only if explicitely asked with allowFileMask =
	 * true If you want to delete a directory, recursive should be set to true
	 * 
	 * @return true if succeeded throws ExecException
	 */
	public boolean deleteFile(String fname, boolean recursive,
			boolean allowFileMask) throws ExecException {

		if (fname == null || fname.trim().length() == 0)
			throw new ExecException("File name not given");

		// some error checking to avoid malicious removals
		if (!allowFileMask) {
			if (fname.indexOf('*') != -1 || fname.indexOf('?') != -1)
				throw new ExecException(
						"File name contains file mask, but this was not allowed: "
								+ fname);
		}

		if (fname.equals("*") || fname.equals("./*") || fname.equals("../*")
				|| fname.equals("/*"))
			throw new ExecException(
					"All files in directories like . .. / are not allowed to be removed: "
							+ fname);

		String temp = fname;
		if (temp.length() > 1 && temp.endsWith(File.separator)) {
			temp = temp.substring(0, temp.length() - 1);
			if (isDebugging)
				log.debug("  %  " + fname + " -> " + temp);
		}

		if (temp.equals(".") || temp.equals("..") || temp.equals("/")
				|| temp.equals(File.separator))
			throw new ExecException(
					"Directories like . .. / are not allowed to be removed: "
							+ fname);

		// end of error checking

		// to be implemented...
		LocalDelete ld = new LocalDelete();
		return ld.deleteFiles(fname, recursive);

	}

	/**
	 * Copy local files to a local/remote directory Input: files is a Collection
	 * of files of type File, targetPath is either a directory in case of
	 * several files, or it is either a dir or filename in case of one single
	 * local file recursive: true if you want traverse directories
	 * 
	 * @return number of files copied successfully
	 */
	public int copyTo(Collection files, String targetPath, boolean recursive)
			throws ExecException {

		int numberOfCopiedFiles = 0;

		Iterator fileIt = files.iterator();
		while (fileIt.hasNext()) {
			File lfile = (File) fileIt.next();
			numberOfCopiedFiles += copyTo(lfile, targetPath, recursive);
		}
		return numberOfCopiedFiles;
	}

	/**
	 * Copy a local file to a local directory/path Input: file of type File
	 * (which can be a directory). The file name can be wildcarded too (but not
	 * the path elements!). targetPath is either a directory or filename
	 * 
	 * @return number of files copied successfully (i.e either returns true or
	 *         an exception is raised)
	 */
	public int copyTo(File lfile, String targetPath, boolean recursive)
			throws ExecException {

		File[] files = null;
		// if the file is wildcarded, we need the list of files
		// Anand : Changed getName() to getPath()
		//getName fails in case of *.txt - indexOf returns '0'
		String name = lfile.getPath();


		if (name.indexOf("*") > 0 || name.indexOf("?") > 0) {
			String pattern = name.replaceAll("\\.", "\\\\.").replaceAll("\\*",
					".*").replaceAll("\\?", ".");

			FilenameFilter_RegularPattern filter = new FilenameFilter_RegularPattern(
					pattern);
			String dirname = lfile.getParent();
			if (dirname == null || dirname == "")
				dirname = ".";
			File dir = new File(dirname);
			files = dir.listFiles(filter);

		} else { // no wildcards
			files = new File[1];
			files[0] = lfile;
		}

		int numberOfCopiedFiles = 0;
		for (int i = 0; i < files.length; i++)
			numberOfCopiedFiles += _copyTo(files[i], targetPath, recursive);

		return numberOfCopiedFiles;
	}

	/**
	 * Copy _one_ local file to a local directory/path Input: file of type File
	 * (which can be a directory) Input must not have wildcards. targetPath is
	 * either a directory or filename
	 * 
	 * @return number of files copied successfully (i.e either returns true or
	 *         an exception is raised)
	 */
	private int _copyTo(File lfile, String targetPath, boolean recursive)
			throws ExecException {

		if (!lfile.exists()) {
			throw new ExecException("File does not exist: " + lfile);
		}

		// check: recursive traversal of directories enabled?
		if (lfile.isDirectory()) {
			if (!recursive)
				throw new SshException("File " + lfile
						+ " is a directory. Set recursive copy!");
		}

		int numberOfCopiedFiles = 0;

		try {
			// recursive handling of directories
			if (lfile.isDirectory()) {
				numberOfCopiedFiles = copyDir(lfile, new File(targetPath));
			} else {
				// copy one file
				File target = new File(targetPath);
				if (target.exists() && target.isDirectory())
					target = new File(targetPath, lfile.getName());
				copyFile(lfile, target);
				numberOfCopiedFiles++;
			}
		} catch (IOException ex) {
			log.error(ex);
			throw new ExecException("Cannot copy " + lfile + " to "
					+ targetPath + ":\n" + ex);
		}

		return numberOfCopiedFiles;
	}

	/**
	 * Copy files from a directory to a local path Input: 'files' is a
	 * Collection of files of type String (! not like at copyTo !), 'sourcePath'
	 * is either empty string (or null) in case the 'files' contain full paths
	 * to the individual files, or it should be a remote dir, and in this case
	 * each file name in 'files' will be extended with the remote dir name
	 * before copy. 'localPath' should be a directory name in case of several
	 * files. It can be a filename in case of a single file to be copied. This
	 * is a convenience method for copyFrom on several remote files. recursive:
	 * true if you want traverse directories
	 * 
	 * @return number of files copied successfully
	 */
	public int copyFrom(String sourcePath, Collection files, File localPath,
			boolean recursive) throws ExecException {

		int numberOfCopiedFiles = 0;

		String sdir;
		if (sourcePath == null || sourcePath.trim().equals("")) {
			sdir = "";
		} else {
			sdir = sourcePath;
			if (!sdir.endsWith(File.separator))
				sdir = sdir + File.separator;
		}

		Iterator fileIt = files.iterator();
		while (fileIt.hasNext()) {
			String sfile = (String) fileIt.next();
			numberOfCopiedFiles += copyFrom(sdir + sfile, localPath, recursive);
		}
		return numberOfCopiedFiles;
	}

	/**
	 * Copy a local file into a local file Input: 'sfile' of type String (can be
	 * a directory or filename) 'localPath' is either a directory or filename
	 * Only if 'recursive' is set, will directories copied recursively.
	 * 
	 * @return number of files copied successfully (i.e either returns true or
	 *         an exception is raised) Note: on local filesystem, this method
	 *         does the same as copyTo
	 */
	public int copyFrom(String sourcePath, File localPath, boolean recursive)
			throws ExecException {

		return copyTo(new File(sourcePath), localPath.getAbsolutePath(),
				recursive);

	}

	/*
	 * 
	 * Private methods
	 */

	/**
	 * Copies src directory to dst. It assumes that src exists and is a
	 * directory. If the dst directory does not exist, it is created. If it
	 * exists, a subdirectory with the name of src is created. Thus, this method
	 * works the same way as 'cp' and 'scp' and org.kepler.ssh.SshExec
	 */
	private int copyDir(File src, File dst) throws IOException {

		int numberOfCopiedFiles = 0;

		if (dst.exists()) {
			// create a subdirectory withing dst (to be compliant with 'cp' and
			// 'scp')
			dst = new File(dst, src.getName());
		}
		dst.mkdir();

		numberOfCopiedFiles++; // the directory counts one

		File[] files = src.listFiles();
		for (int i = 0; i < files.length; i++) {
			// if (isDebugging) log.debug(" %     " + files[i]);
			if (files[i].isDirectory()) {
				numberOfCopiedFiles += copyDir(files[i], new File(dst, files[i]
						.getName()));
			} else {
				copyFile(files[i], new File(dst, files[i].getName()));
				numberOfCopiedFiles++;
			}
		}
		return numberOfCopiedFiles;
	}

	/**
	 * Copies src file to dst file. If the dst file does not exist, it is
	 * created
	 */
	private void copyFile(File src, File dst) throws IOException {
	    // see if source and destination are the same
	    if(src.equals(dst)) {
	        // do not copy
	        return;
	    }
	    
	    //System.out.println("copying " + src + " to " + dst);
	    
		FileChannel srcChannel = new FileInputStream(src).getChannel();
		FileChannel dstChannel = new FileOutputStream(dst).getChannel();
		dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
		srcChannel.close();
		dstChannel.close();

		/* hacking for non-windows */
		// set the permission of the target file the same as the source file
		if (_commandArr[0] == "/bin/sh") {

		    String osName = StringUtilities.getProperty("os.name");
		    if(osName.startsWith("Mac OS X")) {

		        // chmod --reference does not exist on mac, so do the best
		        // we can using the java file api
		        // WARNING: this relies on the umask to set the group, world
		        // permissions.
		        dst.setExecutable(src.canExecute());
		        dst.setWritable(src.canWrite());
		        
		    } else {
    
    			String cmd = "chmod --reference=" + src.getAbsolutePath() + " "
    					+ dst.getAbsolutePath();
    			try {
    				ByteArrayOutputStream streamOut = new ByteArrayOutputStream();
    				ByteArrayOutputStream streamErr = new ByteArrayOutputStream();
    				executeCmd(cmd, streamOut, streamErr);
    			} catch (ExecException e) {
    				log
    						.warn("Tried to set the target file permissions the same as "
    								+ "the source but the command failed: "
    								+ cmd
    								+ "\n" + e);
    			}
		    }
		}
	}

	//TODO: Anand: this function does not handle case of windows 7.
	//It considers windows 7 to be in last else block, hence added
	//and extra condition for contains(windows).
	private int getSystemProps() {
		// Get OS name
		String osName = System.getProperty("os.name");
		//System.out.println("<OS>" + osName + "</OS>");
		if (osName.equals("Windows 95")) {
			_commandArr[0] = "command.com";
			_commandArr[1] = "/C";
			_charsToSkip = 6;
			return 2;
		} else if (osName.equals("Windows NT") || osName.equals("Windows XP")
				|| osName.equals("Windows 2000") || osName.toLowerCase().contains("windows") ) {
			_commandArr[0] = "cmd.exe";
			_commandArr[1] = "/C";
			_charsToSkip = 6;
			return 2;
		} else {
			_commandArr[0] = "/bin/sh";
			_commandArr[1] = "-c";
			_charsToSkip = 5;
			return 2;
		}
	} // end-of-getSystemProps

	// ////////////////////////////////////////////////////////////////////
	// // private variables ////

	// The combined command to execute.
	private int _commandCount;
	private String _commandStr = "";
	private String _commandArr[] = new String[3];
	private int _charsToSkip = 6;

} // end-of-class-CondorJob
