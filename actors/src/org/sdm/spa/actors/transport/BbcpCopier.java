/*
 * Copyright (c) 2009-2012 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:19:36 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31113 $'
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

package org.sdm.spa.actors.transport;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.ssh.ExecException;
import org.kepler.ssh.SshException;
import org.kepler.ssh.SshExec;
import org.sdm.spa.actors.transport.vo.ConnectionDetails;

/**
 * This class provides methods to copy files across two remote machines using
 * bbcp. It generates the command based on the user input, connects to the
 * source machine using ssh and executes the commands. Internally uses
 * <code>SshExec</code> methods to execute command.
 * <p>
 * Copy operation would fail if file with the same name already exists. Command
 * line options can be set to override the default behavior
 * <p>
 * The path of bbcp executable on the remote machine should be set in the class
 * variable 'protocolPathDest', if bbcp is installed on a non standard path. On
 * the local machine or remote source machine, if the variable protocolPathSrc
 * is not set, attempt is made to search the most common locations user's home
 * directory, /usr/bin, /bin, and /usr/local/bin. If the executable is not found
 * in the above paths, an error is reported
 * <p>
 * 
 * @author Chandrika Sivaramakrishnan
 * 
 */
public class BbcpCopier extends FileCopierBase {

	// /////////////////////Private Variables/////////////////////////////////
	private static final Log log = LogFactory
			.getLog(BbcpCopier.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	// //////////////////Protected Methods////////////////////////////////////

	/*
	 * Generates the bbcp command to copy from remote host to local host.
	 * Executes the command using SshExec class
	 * 
	 * @see
	 * org.sdm.spa.actors.transport.FileCopier#copyFrom(org.sdm.spa.actors.transport
	 * .vo.ConnectionDetails, java.lang.String, java.lang.String, boolean)
	 */
	@Override
	// ANUSUA Change -Starts
	protected CopyResult copyFrom(ConnectionDetails srcDetails, String srcFile,
			String destFile, boolean recursive) throws SshException {
		// force copy - overwrite files if already exist
		cmdLineOptions += "-f";
		String osname = (System.getProperty("os.name")).toLowerCase();
		/*
		 * if (osname.contains("windows")) { throw new
		 * SshException("BBCP is not supported on Windows machines"); }
		 */

		int exitCode = 0;
		StringBuffer cmd = new StringBuffer(100);
		OutputStream streamOut = new ByteArrayOutputStream();
		OutputStream streamErr = new ByteArrayOutputStream();
		// Connecting to local destination
		SshExec localSshObj = new SshExec(System.getProperty("user.name"),
				"localhost");
		localSshObj.setTimeout(timeout, false, false);
		localSshObj.setPseudoTerminal(true);
		localSshObj.setForcedCleanUp(forcedCleanup);
		String cmdWithPath="";
		File single_srcFileObj = null;
		File srcFile_Obj = null;
		File srcFileObjlst = null;
		boolean flag = false;
		String[] srcFileList = null;
		String[] srcFile_list = null;
		String wildcardFileList;
		StringBuffer warn = new StringBuffer(100);
		boolean warn_flag = false;

		SshExec sshObject = new SshExec(srcDetails.getUser(), srcDetails
				.getHost(), srcDetails.getPort());

		if (!(srcFile.contains(","))) {
			if (srcFile.startsWith("/")) {
				srcFile_list = new String[1];
				srcFile_list[0] = srcFile.trim();
			} else {
				return new CopyResult(
						1,
						"",
						srcFile
								+ " does not contain full path to the file. Please provide full path. ");
			}
		} else {
			if (srcFile.contains(",")) {
				srcFileList = srcFile.split(",");
				srcFile_list = new String[srcFileList.length];
				for (int count = 0; count < srcFileList.length; count++) {
					if ((srcFileList[count].trim()).startsWith("/")) {
						srcFile_list[count] = srcFileList[count].trim();
					} else {
						warn.append(srcFile_list[count].trim() + " ");
						if (!warn_flag)
							warn_flag = true;
					}
				}
			}
		}

		if (warn_flag) {
			warn
					.append(" does not contain full path to the source file. Please provide full path. ");
			warn_flag = false;
		}

		try {
			cmd.append("bbcp ");
			cmd.append(cmdLineOptions); // Review - Check if cmdLineOptions
			// already has a -z******************#DONE
			if (!(cmdLineOptions.equals("-z"))) {// dealing with firewall
				cmd.append(" -z ");
			}
			// BBCP does not handle error when directory is specified without
			// recursive flag.
			// Better to have recursive always on, rather than giving out wrong
			// output.
			cmd.append("-r ");

			if (!protocolPathSrc.equals("")) {
				cmd.append("-S \"ssh -l %U %H ");
				cmd.append(protocolPathSrc);
				cmd.append("bbcp\" ");
			}
			if (!protocolPathDest.equals("")) {
				cmd.append("-T \"ssh -l %U %H ");
				cmd.append(protocolPathDest);
				cmd.append("bbcp\" ");
			}
			cmd.append(srcDetails.toString());
			cmd.append(":");

			for (int i = 0; i < srcFile_list.length; i++) {
				if (srcFile_list[i].contains("*")
						|| (srcFile_list[i].contains("+"))) {
					// BBCP cannot handle wildcard pattern * if copy is
					// being done from destination to source. We need
					// function to list files matching the pattern
					System.out.println("wildcard found in filename :"
							+ srcFile_list[i]);
					wildcardFileList = sshObject.getwildcardFileListingBBCP(
							srcDetails.toString(), srcFile_list[i]);
					cmd.append(wildcardFileList);
				} else {
					// quotes if file contains wildcard - it might contain space
					// in file name
					cmd.append(srcDetails.toString());
					cmd.append(":");
					cmd.append("\"");
					cmd.append(srcFile_list[i]);
					cmd.append("\"");
					cmd.append(" ");
				}
			}
			cmd.append(" ");
			cmd.append(destFile);// Review - check if dest is
			// dir**********#DONE, Checked earlier

			if (protocolPathDest.equals("")) {
				cmdWithPath = getCmdWithDefaultPath(cmd);
			} else {
				cmdWithPath = protocolPathDest + cmd;
			}
			// Execute bbcp command
			if (isDebugging)
				log.debug("copy cmd=" + cmdWithPath);

			System.out.println("BBCP command is : " + cmdWithPath);

			exitCode = localSshObj.executeCmd(cmdWithPath, streamOut,
					streamErr, srcDetails.toString());

		} catch (Exception e) {
			return new CopyResult(1, e.toString(), "");
		}
		if (exitCode > 0) {
			log.error("Output on stdout:" + streamOut);
			log.error("Output on stderr:" + streamErr);
		}

		String message = streamErr.toString();
		if (message == null || message.trim().equals("")) {
			message = streamOut.toString();
		}
		return new CopyResult(exitCode, message, warn.toString());

	}

	/*
	 * Generates the bbcp command to copy from local host to remote host.
	 * Executes the command using SshExec class
	 * 
	 * @see org.sdm.spa.actors.transport.FileCopier#copyTo(java.lang.String,
	 * org.sdm.spa.actors.transport.vo.ConnectionDetails, java.lang.String,
	 * boolean)
	 */
	@Override
	protected CopyResult copyTo(String srcFile, ConnectionDetails destDetails,
			String destFile, boolean recursive) throws SshException {
		cmdLineOptions = ""; // Review - don't forcefully rewrite.
		// If user wants to overwrite he can set it as command line option, but
		// if you
		// set it, he can't override it with any command line
		// option*********#DONE
		String osname = (System.getProperty("os.name")).toLowerCase();
		String userhome = (System.getProperty("user.home")).toLowerCase();
		if (osname.contains("windows")) {
			throw new SshException("BBCP is not supported on Windows machines");
		}

		int exitCode = 0;
		String cmdWithPath;
		File srcFile_Obj = null;
		boolean flag = false;
		String[] srcFileList = null;
		String[] srcFile_list = null;
		StringBuffer warn = new StringBuffer(100);

		OutputStream streamOut = new ByteArrayOutputStream();
		OutputStream streamErr = new ByteArrayOutputStream();
		StringBuffer cmd = new StringBuffer(100);
		SshExec localSshObj = new SshExec(System.getProperty("user.name"),
				"localhost");
		localSshObj.setTimeout(timeout, false, false);
		localSshObj.setPseudoTerminal(true);
		localSshObj.setForcedCleanUp(forcedCleanup);
		System.out.println("Initial str : " + srcFile);
		if (srcFile.contains(",")) {
			// Anand: list of files
			System.out.println("list of files************");
			srcFileList = srcFile.split(",");
			srcFile_list = new String[srcFileList.length];
			for (int count = 0; count < srcFileList.length; count++) {
				System.out.println("before : " + srcFileList[count]);

				if ((srcFileList[count].trim()).startsWith("/")) {
					srcFile_list[count] = srcFileList[count].trim();
				} else {
					srcFile_list[count] = userhome + "/"
							+ srcFileList[count].trim();
				}
				srcFile_Obj = new File(srcFileList[count]);
				if (srcFile_Obj.isDirectory()) {
					flag = true;
				}
				System.out.println("after : " + srcFile_list[count]);
			}
		} else {
			System.out.println("single files************");
			// Anand: single file
			srcFile_list = new String[1];
			srcFile = srcFile.trim();
			if (srcFile.startsWith("/")) {
				srcFile_list[0] = srcFile;
			} else {
				srcFile_list[0] = userhome + "/" + srcFile;
			}
			srcFile_Obj = new File(srcFile);
			if (srcFile_Obj.isDirectory()) {
				flag = true;
			}
		}

		// build bbcp command
		cmd.append("bbcp ");
		cmd.append(cmdLineOptions);
		cmd.append(" ");
		if (recursive || flag) {
			cmd.append("-r ");
		}
		if (!protocolPathSrc.equals("")) {
			cmd.append("-S \"ssh -l %U %H ");
			cmd.append(protocolPathSrc);
			cmd.append("bbcp\" ");
		}
		if (!protocolPathDest.equals("")) {
			cmd.append("-T \"ssh -l %U %H ");
			cmd.append(protocolPathDest);
			cmd.append("bbcp\" ");
		}
		// all files are in the srcFile_list (single file too)
		for (int i = 0; i < srcFile_list.length; i++) {
			if (srcFile_list[i].contains("*")
					|| (srcFile_list[i].contains("*"))) {
				// no quotes if file contains wildcard
				cmd.append(srcFile_list[i]);
				cmd.append(" ");
			} else {
				// quotes if file does not contain wildcard - it might contain
				// space in file name
				cmd.append("\"");
				cmd.append(srcFile_list[i]);
				cmd.append("\"");
				cmd.append(" ");
			}
		}
		cmd.append(destDetails.toString());
		cmd.append(":");
		cmd.append(destFile);

		if (protocolPathSrc.equals("")) {
			cmdWithPath = getCmdWithDefaultPath(cmd);
		} else {
			cmdWithPath = protocolPathSrc + cmd;
		}
		System.out.println("BBCP Command to be executed is : " + cmdWithPath);
		if (isDebugging)
			log.debug("copy cmd=" + cmdWithPath);
		try {
			exitCode = localSshObj.executeCmd(cmdWithPath, streamOut,
					streamErr, destDetails.toString());
		} catch (ExecException e) {
			return new CopyResult(1, e.toString(), "");
		}
		if (exitCode > 0) {
			log.error("Output on stdout:" + streamOut);
			log.error("Output on stderr:" + streamErr);
		}
		String message = streamErr.toString();
		if (message == null || message.trim().equals("")) {
			message = streamOut.toString();
		}
		return new CopyResult(exitCode, message, warn.toString());
	}

	/*
	 * Generates the bbcp command to copy from a remote host to another remote
	 * host. Executes the command using SshExec class
	 * 
	 * @see
	 * org.sdm.spa.actors.transport.FileCopier#copyRemote(org.sdm.spa.actors
	 * .transport.vo.ConnectionDetails, java.lang.String,
	 * org.sdm.spa.actors.transport.vo.ConnectionDetails, java.lang.String,
	 * boolean)
	 */
	@Override
	protected CopyResult copyRemote(ConnectionDetails srcDetails,
			String srcFile, ConnectionDetails destDetails, String destFile,
			boolean recursive) throws ExecException {
		cmdLineOptions = ""; // Review - don't force overwrite***********#DONE
		if (isDebugging)
			log.debug("remote bbcp copy");
		OutputStream cmdStdout = new ByteArrayOutputStream();
		OutputStream cmdStderr = new ByteArrayOutputStream();
		// OutputStream streamOut = new ByteArrayOutputStream();
		// OutputStream streamErr = new ByteArrayOutputStream();
		String remoteHostStr = "";
		SshExec sshObjectSrc = null;
		SshExec sshObjectDest = null;
		// File single_srcFileObj = null;
		// File srcFile_Obj = null;
		// File srcFileObjlst = null;
		// boolean flag = false;
		String[] srcFileList = null;
		String[] srcFile_list = null;
		String wildcardFileList = "";
		StringBuffer warn = new StringBuffer(100);
		boolean warn_flag = false;

		// bbcp needs pseudo terminal enabled, so that password request is sent
		// to stdout instead of terminal
		// source connection object
		sshObjectSrc = new SshExec(srcDetails.getUser(), srcDetails.getHost(),
				srcDetails.getPort());
		sshObjectSrc.setTimeout(timeout, false, false);
		sshObjectSrc.setForcedCleanUp(forcedCleanup);
		sshObjectSrc.setPseudoTerminal(true);

		// destination connection object
		sshObjectDest = new SshExec(destDetails.getUser(), destDetails
				.getHost(), destDetails.getPort());
		sshObjectDest.setTimeout(timeout, false, false);
		sshObjectDest.setForcedCleanUp(forcedCleanup);
		sshObjectDest.setPseudoTerminal(true);

		if (srcDetails.isConnectionOrigin()) {
			remoteHostStr = destDetails.toString();
		} else {
			remoteHostStr = srcDetails.toString();
		}

		StringBuffer cmd = new StringBuffer(100);
		String cmdWithPath = null;
		int exitCode = 0;

		if (srcFile.contains(",")) {
			// list of files
			srcFileList = srcFile.split(",");
			srcFile_list = new String[srcFileList.length];
			for (int count = 0; count < srcFileList.length; count++) {
				if ((srcFileList[count].trim()).startsWith("/")) {
					srcFile_list[count] = srcFileList[count].trim();
				} else {
					warn.append(srcFileList[count].trim() + " ");
					if (!warn_flag)
						warn_flag = true;
				}
			}
			if (warn_flag) {
				warn
						.append(" does not contain full path to the source file. Please provide full path. ");
				warn_flag = false;
			}
		} else {
			// single file
			if (srcFile.startsWith("/")) {
				srcFile_list = new String[1];
				srcFile_list[0] = srcFile.trim();
			} else {
				throw new SshException(
						srcFile
								+ "does not contain full path to the file. Please provide full path.");
			}
		}
		// build bbcp command
		try {
			cmd.append("bbcp ");
			cmd.append(cmdLineOptions);
			if (!srcDetails.isConnectionOrigin()) {
				// -z option for destination to source copy
				cmd.append(" -z ");
			}
			// if (recursive) {
			// BBCP does not handle error when directory is specified without
			// recursive flag.
			// Better to have recursive always on, rather than giving out wrong
			// output.
			cmd.append("-r ");
			// }
			log.debug("Protocol path src =" + protocolPathSrc);
			if (!protocolPathSrc.equals("")) {
				cmd.append("-S \"ssh -l %U %H ");
				cmd.append(protocolPathSrc);
				cmd.append("bbcp\" ");
			}
			if (!protocolPathDest.equals("")) {
				cmd.append("-T \"ssh -l %U %H ");
				cmd.append(protocolPathDest);
				cmd.append("bbcp\" ");
			}
			if (srcDetails.isConnectionOrigin()) {
				// BBCP can handle wildcard pattern * if copy is done
				// from source machine to destination machine
				for (int i = 0; i < srcFile_list.length; i++) {
					if (srcFile_list[i].contains("*")
							|| (srcFile_list[i].contains("+"))) {
						cmd.append(srcFile_list[i]);
						cmd.append(" ");
					} else {
						cmd.append("\"");
						cmd.append(srcFile_list[i]);
						cmd.append("\"");
						cmd.append(" ");
					}
				}
				cmd.append(destDetails.toString());
				cmd.append(":");
				cmd.append(destFile);
			} else {
				for (int i = 0; i < srcFile_list.length; i++) {
					// BBCP cannot handle wildcard pattern * if copy is
					// being done from destination to source. We need
					// function to list files matching the pattern
					if (srcFile_list[i].contains("*")
							|| (srcFile_list[i].contains("+"))) {
						wildcardFileList = sshObjectSrc
								.getwildcardFileListingBBCP(srcDetails
										.toString(), srcFile_list[i]);
						cmd.append(wildcardFileList);
					} else {
						cmd.append(srcDetails.toString());
						cmd.append(":");
						cmd.append("\"");
						cmd.append(srcFile_list[i]);
						cmd.append("\"");
						cmd.append(" ");
					}
				}
				// cmd.append(destDetails.toString());
				// cmd.append(":");
				cmd.append(destFile);
			}
			if (protocolPathDest.equals("")) {
				cmdWithPath = getCmdWithDefaultPath(cmd);
			} else {
				cmdWithPath = protocolPathDest + cmd;
			}

			log.debug("copy cmd without default path=" + cmd);
			if (isDebugging)
				log.debug("remote copy cmd=" + cmdWithPath);

			System.out.println("cmdwithpath : " + cmdWithPath);
			if (srcDetails.isConnectionOrigin()) {
				exitCode = sshObjectSrc.executeCmd(cmdWithPath, cmdStdout,
						cmdStderr, remoteHostStr);
			} else {
				exitCode = sshObjectDest.executeCmd(cmdWithPath, cmdStdout,
						cmdStderr, remoteHostStr);
			}

		} catch (ExecException e) {
			return new CopyResult(1, e.toString(), "");
		}
		if (exitCode > 0) {
			log.error("Output on stdout:" + cmdStdout);
			log.error("Output on stderr:" + cmdStderr);
		}
		String message = cmdStderr.toString();
		if (message == null || message.trim().equals("")) {
			message = cmdStdout.toString();
		}
		return new CopyResult(exitCode, message, warn.toString());
	}

	@Override
	protected int getDefaultPort() {
		return -1;
	}

}
