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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.actor.ssh.SshSession;
import org.kepler.ssh.ExecException;
import org.kepler.ssh.SshException;
import org.kepler.ssh.SshExec;
import org.sdm.spa.actors.transport.vo.ConnectionDetails;

import com.jcraft.jsch.Session;

/**
 * Copies files using SCP protocol. Uses the SshExec class to run the scp
 * command.
 * <P>
 * Copy operation will overwrite existing files by default. Copy between a local
 * host and a remote host ignores the variables forcedCleanup, and timeout.
 * <P>
 * When transferring files between local machine and a remote machine, if the
 * remote machine does not have scp in standard path, you can set it using the
 * variables protocolPathSrc/protocolPathDest.
 * <P>
 * When transferring files between two remote machines, the scp command is run
 * from the source machine. Is scp is not in standard path on source machine,
 * set the path in protocolPathSrc variable. If this variable is not set, class
 * attempts to search in the most common locations - /usr/bin, /bin,
 * /usr/local/bin, . , and ~
 * <P>
 * 
 * @author Chandrika Sivaramakrishnan
 * 
 */
public class ScpCopier extends FileCopierBase {

	// /////////////////////Private Variables/////////////////////////////////
	private static final Log log = LogFactory.getLog(ScpCopier.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();
	protected SshSession session;
	protected Session jschSession;

	// //////////////////Protected Methods////////////////////////////////////

	@Override
	// Anusua Change - Starts
	/**
	 * Copy file from a remote source to local destination
	 */
	protected CopyResult copyFrom(ConnectionDetails srcDetails, String srcFile,
			String destFile, boolean recursive) {
		StringBuffer warn = new StringBuffer(100);

		try {
			int count = 0;
			String srcFileList = "";
			SshExec sshObject = new SshExec(srcDetails.getUser(), srcDetails
					.getHost(), srcDetails.getPort());
			sshObject.setTimeout(getTimeout(), false, false);
			sshObject.setProtocolPath(protocolPathSrc);
			sshObject.setcmdLineOptions(cmdLineOptions);
			File destFileObj = new File(destFile);
			boolean warn_flag = false;

			// Transfer of multiple files to directory level
			if (srcFile.contains(",")) {

				String[] srcFile_list = srcFile.split(",");
				for (int i = 0; i < srcFile_list.length; i++) {
					srcFile_list[i] = srcFile_list[i].trim();
					if (srcFile_list[i].startsWith("/")) {
						if (srcFile_list[i].contains("*")
								|| srcFile_list[i].contains("+")) {
							// add the list of files matching wildcard to
							// srcfilelist
							srcFileList += srcFile_list[i] + " ";
							// count = sshObject.copyFrom(srcFile_list[i],
							// destFileObj, recursive);
						} else {
							//escape spaces and other special characters in filename
							srcFileList += "\"" + srcFile_list[i] + "\" ";
						}
					} else {
						warn.append(srcFile_list[i] + " ");
						if (!warn_flag)
							warn_flag = true;
						// Anand: replaced warn_flag with return error statement
						// return new CopyResult(1,"",srcFile_list[i]
						// +" does not contain full path to the file. Please provide full path. ");
					}
				}
				if (warn_flag) {
					warn
							.append(" does not contain full path to the source file. Please provide full path. ");
				}

				count = sshObject.copyFrom(srcFileList, destFileObj, recursive);

			} else { // Anand: single file case
				srcFile = srcFile.trim();
				if (srcFile.startsWith("/")) {
					// pass the source file in quotes
					if (!(srcFile.contains("*") || srcFile.contains("+"))) {
						srcFile = "\"" + srcFile + "\"";
					}
					count = sshObject.copyFrom(srcFile, destFileObj, recursive);
				} else {
					warn
							.append(srcFile
									+ " does not contain full path to the file. Please provide full path. ");
				}
			}
			if (count <= 0) {
				warn_flag = true;
				warn
						.append("\n No files were copied. Please check your input!");
			}
			// Anusua - end
			if (count > 0 && !warn_flag) {
				// return success if copyFrom completed without exceptionc
				return new CopyResult(0, "", warn.toString());
			}

			return new CopyResult(1, warn.toString(), "");

		} catch (Exception e) {
			log.error(e);
			return new CopyResult(1, e.getMessage(), warn.toString());
		}

	}

	@Override
	protected CopyResult copyTo(String srcFile, ConnectionDetails destDetails,
			String destFile, boolean recursive) throws SshException {

		try {
			int count = 0;
			SshExec sshObject = new SshExec(destDetails.getUser(), destDetails
					.getHost(), destDetails.getPort());
			if (isDebugging)
				log.debug("SshExec object created");
			sshObject.setTimeout(getTimeout(), false, false);
			sshObject.setProtocolPath(protocolPathDest);
			sshObject.setcmdLineOptions(cmdLineOptions);
			
			log.debug("******* SCPCopier :: CopyTo called********");

			

			// Transfer of multiple files to directory level
			if (srcFile.contains(",")) {
				log.debug("***** Detected file list *******");

				String[] srcFile_list = srcFile.split(",");
				//Modified by - Chandrika - moved path check to parent  class
				List<File> srcFile_List = new ArrayList<File>();
				for (int i = 0; i < srcFile_list.length; i++) {
					srcFile_List.add(new File(srcFile_list[i]));
				}
				//call multiple file copyTo method
				count = sshObject.copyTo(srcFile_List, destFile, recursive); 
			} else {
				// Transfer of single file to directory level or individual file
				// level
				// The first predicate applies to Windows; the selected offset
				// assumes that
				// drive identifiers will be 1 character long.
				srcFile = srcFile.trim();
				
				//Modified by Chandrika S - Moved relative path check to parent class
				File srcFileObj = new File(srcFile);

				// Anand: If file exists - initiate copy
				count = sshObject.copyTo(srcFileObj, destFile, recursive);
			}
			
			if (count > 0) {
				// return success if copyFrom completed without exceptions
				return new CopyResult();
			}
			return new CopyResult(
					1,
					"Unknown Error. No files where copied. Please check the input provided",
					"");

		} catch (Exception e) {
			log.error(e);
			e.printStackTrace();
			return new CopyResult(1, e.getMessage(), "");
		}
	}

	@Override
	protected CopyResult copyRemote(ConnectionDetails srcDetails,
			String srcFile, ConnectionDetails destDetails, String destFile,
			boolean recursive) throws ExecException {
		// Anand: Passing list of files in case of remote copy from destination
		// to source does not work
		// Reason: When we specify a list of files on remote destination the
		// command format looks like:
		// scp srcDetails:/filePath1 srcDetails:/filePath2
		// /LocalfilePathDestination
		// for each of the files in the list SCP sends a password request.
		// the function exectueCommand (which executes commands on remote
		// machine handles only the
		// first password request, and the program hangs for preceding password
		// requests.

		if (isDebugging)
			log.debug("remote scp copy");
		ByteArrayOutputStream cmdStdout = new ByteArrayOutputStream();
		ByteArrayOutputStream cmdStderr = new ByteArrayOutputStream();
		String remoteHostStr = "";
		String srcFileList = "";

		SshExec sshObject = null;
		if (srcDetails.isConnectionOrigin()) {
			sshObject = new SshExec(srcDetails.getUser(), srcDetails.getHost(),
					srcDetails.getPort());
			remoteHostStr = destDetails.toString();

		} else {
			sshObject = new SshExec(destDetails.getUser(), destDetails
					.getHost(), destDetails.getPort());
			remoteHostStr = srcDetails.toString();
		}

		sshObject.setTimeout(timeout, false, false);
		sshObject.setForcedCleanUp(forcedCleanup);

		try {
			// scp needs pseudo terminal enabled, so that password request is
			// sent to
			// stdout instead of terminal
			sshObject.setPseudoTerminal(true);
			StringBuffer cmd = new StringBuffer(100);
			String cmdWithPath;
			int exitCode = 0;

			if (srcDetails.isConnectionOrigin()) {
				// escape spaces in file names
				if (srcFile.contains(",")) {
					// list of files
					String[] srcFile_list = srcFile.split(",");
					for (int i = 0; i < srcFile_list.length; i++) {
						// Anand: wild card cannot be processed in SCP if they
						// are enclosed in ""
						if (srcFile_list[i].contains("*")
								|| srcFile_list[i].contains("*"))
							srcFileList += srcFile_list[i].trim() + " ";
						else
							srcFileList += "\"" + srcFile_list[i].trim() + "\""
									+ " ";
					}
				} else {
					// Single file
					if (srcFile.contains("*") || srcFile.contains("*"))
						srcFileList = srcFile.trim() + " ";
					else
						srcFileList = "\"" + srcFile.trim() + "\"" + " ";
				}

				// Anand: Following code can go in function
				// buildCommandForRemoteCopy(src, dest);
				// just swap src, dest parameters for 2 cases
				cmd.append("scp -P ");
				cmd.append(destDetails.getPort());
				if (recursive) {
					cmd.append(" -r ");
				} else {
					cmd.append("  ");
				}
				cmd.append(cmdLineOptions);
				cmd.append("  ");
				cmd.append(srcFileList);
				cmd.append("  ");
				cmd.append(destDetails.getUser());
				cmd.append("@");
				cmd.append(destDetails.getHost());
				cmd.append(":");
				cmd.append(destFile);
				if (protocolPathSrc.equals("")) {
					cmdWithPath = getCmdWithDefaultPath(cmd);
				} else {
					cmdWithPath = protocolPathSrc + cmd;
				}
				if (isDebugging)
					log.debug("remote copy cmd=" + cmdWithPath);

				System.out
						.println("*************remote command " + cmdWithPath);

				exitCode = sshObject.executeCmd(cmdWithPath, cmdStdout,
						cmdStderr, remoteHostStr);

				log.debug("ExitCode:" + exitCode);
				log.debug("stdout:" + cmdStdout);
				log.debug("stderr:" + cmdStderr);

				String message = cmdStderr.toString();
				if (message == null || message.trim().equals("")) {
					message = cmdStdout.toString();
				}
				return new CopyResult(exitCode, message, "");
			} else {
				// Destination is Origin
				// Anand: We need execute each file in a loop
				StringBuffer warn = new StringBuffer(100);
				boolean warn_flag = false;
				String message = "";
				if (srcFile.contains(",")) {
					// list of files
					String[] srcFile_list = srcFile.split(",");
					for (int i = 0; i < srcFile_list.length; i++) {
						// skip spaces in file names
						// Anand: look up replaceAll for more info on why to use
						// \\\\ instead of \\
						srcFile_list[i] = srcFile_list[i].trim().replaceAll(
								" ", "\\\\ ");
						System.out.println("*********************file:"
								+ srcFile_list[i]);
						if (srcFile_list[i].contains("*")
								|| srcFile_list[i].contains("*"))
							srcFileList = srcDetails.getUser() + "@"
									+ srcDetails.getHost() + ":"
									+ srcFile_list[i] + " ";
						else
							srcFileList = srcDetails.getUser() + "@"
									+ srcDetails.getHost() + ":" + "\""
									+ srcFile_list[i] + "\"" + " ";
						cmd = buildSCPRemoteDestinationCommand(srcFileList,
								destDetails.getPort(), destFile, recursive);
						if (protocolPathDest.equals("")) {
							cmdWithPath = getCmdWithDefaultPath(cmd);
						} else {
							cmdWithPath = protocolPathDest + cmd;
						}

						if (isDebugging)
							log.debug("remote copy cmd=" + cmdWithPath);

						System.out.println("*************remote command "
								+ cmdWithPath);

						exitCode = sshObject.executeCmd(cmdWithPath, cmdStdout,
								cmdStderr, remoteHostStr);

						log.debug("ExitCode:" + exitCode);
						log.debug("stdout:" + cmdStdout);
						log.debug("stderr:" + cmdStderr);

						if (exitCode != 0) {
							warn.append("Could not copy file " + srcFileList
									+ ".\n");
							warn_flag = true;
						}
						message = cmdStderr.toString();
						if (message == null || message.trim().equals("")) {
							message = cmdStdout.toString();
						}
					}
					if (warn_flag)
						return new CopyResult(1, warn.toString(), "");
					else
						return new CopyResult(0, message, "");
				} else {
					srcFile = srcFile.trim().replaceAll(" ", "\\\\ ");
					// Anand: single file
					if (srcFile.contains("*") || srcFile.contains("*"))
						srcFileList = srcDetails.getUser() + "@"
								+ srcDetails.getHost() + ":" + srcFile.trim()
								+ " ";
					else
						srcFileList = srcDetails.getUser() + "@"
								+ srcDetails.getHost() + ":" + "\""
								+ srcFile.trim() + "\"" + " ";
					cmd = buildSCPRemoteDestinationCommand(srcFileList,
							destDetails.getPort(), destFile, recursive);
					if (protocolPathDest.equals("")) {
						cmdWithPath = getCmdWithDefaultPath(cmd);
					} else {
						cmdWithPath = protocolPathDest + cmd;
					}
					// Anand: Code till here goes into function

					if (isDebugging)
						log.debug("remote copy cmd=" + cmdWithPath);

					System.out.println("*************remote command "
							+ cmdWithPath);

					exitCode = sshObject.executeCmd(cmdWithPath, cmdStdout,
							cmdStderr, remoteHostStr);

					log.debug("ExitCode:" + exitCode);
					log.debug("stdout:" + cmdStdout);
					log.debug("stderr:" + cmdStderr);

					message = cmdStderr.toString();
					if (message == null || message.trim().equals("")) {
						message = cmdStdout.toString();
					}
					return new CopyResult(exitCode, message, "");
				}
			}

		} catch (Exception e) {
			return new CopyResult(1, e.getMessage(), "");
		}

	}

	public StringBuffer buildSCPRemoteDestinationCommand(String fileName,
			int destPort, String destFile, boolean recursive) {
		StringBuffer cmd = new StringBuffer(100);
		cmd.append("scp -P ");
		cmd.append(destPort);
		if (recursive) {
			cmd.append(" -r ");
		} else {
			cmd.append("  ");
		}
		cmd.append(cmdLineOptions);
		cmd.append("  ");
		cmd.append(fileName);
		cmd.append(destFile);
		return cmd;
	}

	// NEW METHOD ADDED BY ANUSUA
	protected boolean isScpPresent(ConnectionDetails srcDetails,
			ConnectionDetails destDetails) {
		StringBuffer cmd = new StringBuffer(100);
		ByteArrayOutputStream cmdStdout = new ByteArrayOutputStream();
		ByteArrayOutputStream cmdStderr = new ByteArrayOutputStream();
		SshExec sshObject = null;
		String cmdWithPath = null;
		String remoteHostStr = "";
		boolean isScpPresent = false;
		int exitCode = 0;

		if (srcDetails.isLocal() || destDetails.isLocal()) {
			isScpPresent = true; // Comment - localhost always has scp api
									// present
		} else {
			if ((!(srcDetails.isLocal())) && (!(destDetails.isLocal()))) { // Comment
																			// -
																			// check
																			// for
																			// remote
																			// to
																			// remote
																			// connection
				sshObject = new SshExec(srcDetails.getUser(), srcDetails
						.getHost(), srcDetails.getPort());
				remoteHostStr = destDetails.toString();
				sshObject.setTimeout(1000, false, false);
				try {
					cmd.append("which scp");
					cmdWithPath = getCmdWithDefaultPath(cmd);

					exitCode = sshObject.executeCmd(cmdWithPath, cmdStdout,
							cmdStderr, remoteHostStr);
					String message = cmdStderr.toString();
					if (message == null || message.trim().equals("")) {
						message = cmdStdout.toString();
						isScpPresent = true;
					}
				} catch (ExecException e) {
					log.error("Exception from execution:" + e);
				} catch (Exception e) {
					log.error("Exception from isScpPresent():" + e);

				}
			}
		}

		return isScpPresent;
	}

	@Override
	protected int getDefaultPort() {
		return 22;
	}

}
