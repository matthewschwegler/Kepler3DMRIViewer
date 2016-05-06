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
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.ssh.ExecException;
import org.kepler.ssh.SftpExec;
import org.kepler.ssh.SshException;
import org.sdm.spa.actors.transport.vo.ConnectionDetails;

/**
 * This class copies files/directories from one machine to another using the
 * sftp protocol. It uses the <code>SftpExec</code> to copy file/directory from
 * local machine to a remote machine or vice-versa. Copy between remote machines
 * is achieved using sftp batch mode.
 * <P>
 * Copy operation will overwrite existing files by default. Copy between local
 * host and remote host does not used the sftp command and hence would ignore
 * the variables cmdLineOptions, protocolPathSrc, forcedCleanup, and timeout.
 * <P>
 * Class expect sftp to be available on standard path on the destination
 * machine. On source machine, if sftp is not on standard path and if the
 * variable protocolPathSrc is not set, the class attempts to search in the most
 * common locations - user's home directory, /usr/bin, /bin, and /usr/local/bin
 * 
 * @author Chandrika Sivaramakrishnan
 *  
 */

/**
 * SFTP provides functionality to copy files, regular expressions,
 * directories(with dir: option). It does not provide list of file/dir/regular
 * expression. To achieve that we loop through list of files and execute sftp
 * copy for each statement.\
 * 
 * @author
 * 
 */

public class SftpCopier extends FileCopierBase {

	// private variables
	private static final Log log = LogFactory
			.getLog(SftpCopier.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	// public/protected methods
	/*
	 * Uses the <code>SftpExec</code> class to copy files from a remote host to
	 * the local machine.
	 * 
	 * @see
	 * org.sdm.spa.actors.transport.FileCopier#copyFrom(org.sdm.spa.actors.transport
	 * .vo.ConnectionDetails, java.lang.String, java.lang.String, boolean)
	 */

	// Anusua Change - STARTS
	@Override
	protected CopyResult copyFrom(ConnectionDetails srcDetails, String srcFile,
			String destFile, boolean recursive) throws ExecException {

		if (isDebugging) {
			log.debug("sftp copy from");
		}

		StringBuffer warn = new StringBuffer(100);
		boolean warn_flag = false;

		SftpExec sftpObject = new SftpExec(srcDetails.getUser(), srcDetails
				.getHost(), srcDetails.getPort());
		sftpObject.setTimeout(getTimeout(), false, false);
		File destFileObj = new File(destFile);

		try {

			// Transfer of multiple files to directory level
			if (srcFile.contains(",")) {
				String[] srcFile_list = srcFile.split(",");
				for (int i = 0; i < srcFile_list.length; i++) {
					if ((srcFile_list[i].trim()).startsWith("/")) {
						sftpObject.copyFrom(srcFile_list[i].trim(),
								destFileObj, recursive);
					} else {
						warn.append(srcFile_list[i] + " ");
						if (!warn_flag)
							warn_flag = true;
					}
				}
			} else { // single file specified
				if ((srcFile.trim()).startsWith("/")) {
					sftpObject.copyFrom(srcFile.trim(), destFileObj, recursive);
				} else {
					warn.append(srcFile + " ");
					if (!warn_flag)
						warn_flag = true;
				}
				sftpObject.copyFrom(srcFile.trim(), destFileObj, recursive);
			}

			if (warn_flag) {
				warn
						.append(" does not contain full path to the source file. Please provide full path. ");
				return new CopyResult(1, warn.toString(), ""); // sftp returns
				// without
				// exception in
				// case
			}
			return new CopyResult(); // sftp returns without exception in case
			// of
			// success
		} catch (ExecException e) {
			return new CopyResult(1, e.getMessage(), "");
		}

	}

	/*
	 * Uses <code>SftpExec</code> to copy files from a local host to remote
	 * machine
	 * 
	 * @see org.sdm.spa.actors.transport.FileCopier#copyTo(java.lang.String,
	 * org.sdm.spa.actors.transport.vo.ConnectionDetails, java.lang.String,
	 * boolean)
	 */
	@Override
	protected CopyResult copyTo(String srcFile, ConnectionDetails destDetails,
			String destFile, boolean recursive) throws ExecException {

		StringBuffer warn = new StringBuffer(100);
		boolean warn_flag = false;
		boolean isWindows = false;
		if (isDebugging) {
			log.debug("sftp copy to");
		}
		SftpExec sftpObject = new SftpExec(destDetails.getUser(), destDetails
				.getHost(), destDetails.getPort());
		sftpObject.setTimeout(getTimeout(), false, false);
		String osname = (System.getProperty("os.name")).toLowerCase();
		String userhome = (System.getProperty("user.home")).toLowerCase();
		if (osname.equalsIgnoreCase("windows")) {
			isWindows = true;
		}
		File destFileObj = new File(destFile);
		File srcFileObj = null;

		try {
			// Transfer of multiple files to directory level
			if (srcFile.contains(",")) {
				String[] srcFile_list = srcFile.split(",");
				for (int i = 0; i < srcFile_list.length; i++) {
					//Chandrika - Assume that file has full path.
					//Parent class would have fixed the local relative paths
					//before calling copyTo. Do not enclose filename in "" to escape spaces.
					//sftpObject handles it. 
					srcFile_list[i] = srcFile_list[i].trim() ;
					srcFileObj = new File(srcFile_list[i]);
					// if (srcFileObj.exists())
					sftpObject.copyTo(srcFileObj, destFile, recursive);
					// else{
					// warn_flag = true;
					// warn.append("Source file " + srcFile_list[i] +
					// " does not exist!\n");
					// }
				}
			} else {
				// Anand: Adding else to handle single file transfer
				srcFileObj = new File(srcFile);
				// if (srcFileObj.exists())
				sftpObject.copyTo(srcFileObj, destFile, recursive);
				// else{
				// warn_flag = true;
				// warn.append("Source file " + srcFile + " does not exist!\n");
				// }
			}
			if (warn_flag)
				return new CopyResult(1, warn.toString(), "");
			else
				return new CopyResult();
		} catch (ExecException e) {
			return new CopyResult(1, e.getMessage(), "");
		}
	}

	/*
	 * Copies files between two remote machines using the sftp command. Stores
	 * the sequence of mkdir and put command in a file on source machine and
	 * runs the sftp command in batch mode. The command file is deleted at the
	 * end of transaction
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
		if (isDebugging)
			log.debug("remote sftp copy");
		ByteArrayOutputStream cmdStdout = new ByteArrayOutputStream();
		ByteArrayOutputStream cmdStderr = new ByteArrayOutputStream();
		String cmdWithPath = null;
		int exitCode = 0;
		String cmdFile = "keplercmd_" + System.currentTimeMillis() + ".cmd";
		SftpExec sshObjectSrc = null;
		SftpExec sshObjectDest = null;
		String remoteHostStr = "";
		boolean warn_flag = false;
		StringBuffer warn = new StringBuffer(100);

		sshObjectSrc = new SftpExec(srcDetails.getUser(), srcDetails.getHost(),
				srcDetails.getPort());
		sshObjectDest = new SftpExec(destDetails.getUser(), destDetails
				.getHost(), destDetails.getPort());
		// SSH Session initialization
		if (srcDetails.isConnectionOrigin()) {
			// Connenct to source by ssh
			sshObjectSrc.setTimeout(timeout, false, false);
			sshObjectSrc.setForcedCleanUp(forcedCleanup);
			sshObjectSrc.setPseudoTerminal(true);
			remoteHostStr = destDetails.toString();
		} else {
			sshObjectDest.setTimeout(timeout, false, false);
			sshObjectDest.setForcedCleanUp(forcedCleanup);
			sshObjectDest.setPseudoTerminal(true);
			remoteHostStr = srcDetails.toString();
		}

		String[] srcFile_list = srcFile.split(",");

		try {
			// Transfer of multiple files to directory level
			if (srcFile.contains(",")) {
				// for each file in the list
				for (int index = 0; index < srcFile_list.length; index++) {
					cmdFile = "keplercmd_" + System.currentTimeMillis()
							+ ".cmd";
					// cmdFile = "keplercmd_" + System.currentTimeMillis() +
					// ".cmd";
					// Anand: The file name contains regular expression - what
					// about +, ?
					if (srcFile_list[index].contains("*")) {
						// if file name is a wildcard
						cmdWithPath = buildSftpWildcardBatch(srcDetails,
								srcFile_list[index].trim(), destDetails,
								destFile.trim(), recursive, sshObjectSrc,
								cmdFile);
					} else {
						// if file name is a regular file/directory
						cmdWithPath = buildSftpCommand(srcDetails,
								srcFile_list[index].trim(), destDetails,
								destFile.trim(), recursive, sshObjectSrc,
								cmdFile);
					}
					System.out.println("***************Rmote command : "
							+ cmdWithPath);

					// Anand: Executing each file from the list separately is
					// inefficient
					// This may have been used to process each file separately,
					// and continue
					// copying even in case of failure of one of the files.
					if (srcDetails.isConnectionOrigin())
						exitCode = sshObjectSrc.executeCmd(cmdWithPath,
								cmdStdout, cmdStderr, remoteHostStr);
					else
						exitCode = sshObjectDest.executeCmd(cmdWithPath,
								cmdStdout, cmdStderr, remoteHostStr);
					if (exitCode > 0) {
						warn_flag = true;
						warn.append("Could not copy " + srcFile_list[index]
								+ ".\n");
					}

					log.debug("exit code=" + exitCode);
					log.error("Stdout: " + cmdStdout);
					log.error("Error stream out: " + cmdStderr);
					String fileDeleteMsg = "";
					try {
						boolean success = false;
						if (srcDetails.isConnectionOrigin())
							success = sshObjectSrc.deleteFile(cmdFile, false,
									false);
						else
							success = sshObjectDest.deleteFile(cmdFile, false,
									false);

						if (success) {
							log.debug("deleted the sftp command file "
									+ cmdFile + " created for file copy");
						}
						if (!success) {
							log.warn("Unable to delete the sftp command file "
									+ cmdFile + " created for file copy");
							warn
									.append("Unable to delete the sftp command file "
											+ cmdFile
											+ " created for file copy\n");
						}
					} catch (ExecException e) {
						log.warn("Unable to delete the sftp command file "
								+ cmdFile + " created for file copy : "
								+ e.toString());
					}
				}
			} else { // single file source file is a remote file/dir/wildcard
				if (srcFile.contains("*")) {
					cmdWithPath = buildSftpWildcardBatch(srcDetails, srcFile,
							destDetails, destFile, recursive, sshObjectSrc,
							cmdFile);
				} else {
					cmdWithPath = buildSftpCommand(srcDetails, srcFile,
							destDetails, destFile, recursive, sshObjectSrc,
							cmdFile);
				}
				// cmdWithPath = cmd + "\"";
				System.out.println("***************Rmote command : "
						+ cmdWithPath);
				if (srcDetails.isConnectionOrigin())
					exitCode = sshObjectSrc.executeCmd(cmdWithPath, cmdStdout,
							cmdStderr, remoteHostStr);
				else
					exitCode = sshObjectDest.executeCmd(cmdWithPath, cmdStdout,
							cmdStderr, remoteHostStr);

				String fileDeleteMsg = "";
				try {
					boolean success = false;
					if (srcDetails.isConnectionOrigin())
						success = sshObjectSrc
								.deleteFile(cmdFile, false, false);
					else
						success = sshObjectDest.deleteFile(cmdFile, false,
								false);

					if (success) {
						log.debug("deleted the sftp command file " + cmdFile
								+ " created for file copy");
					}
					if (!success) {
						log.warn("Unable to delete the sftp command file "
								+ cmdFile + " created for file copy");
						warn.append("Unable to delete the sftp command file "
								+ cmdFile + " created for file copy\n");
					}
				} catch (ExecException e) {
					log.warn("Unable to delete the sftp command file "
							+ cmdFile + " created for file copy : "
							+ e.toString());
				}
			}
			if (warn_flag)
				return new CopyResult(1, warn.toString(), "");

			String message = cmdStdout.toString();
			return new CopyResult(exitCode, message, "");
		} catch (Exception e) {
			e.printStackTrace();
			return new CopyResult(1, e.getMessage(), "");
		}
	}

	/**
	 * Anand: Create a command for remote to remote SFTP copy. This function
	 * does not copy the command file to remote directory. It just creates echo
	 * statement to copy the command file and the command. command format : bash
	 * -c 'export PATH=/usr/bin:/bin:/usr/local/bin:~:.:$PATH; echo -e
	 * "put <sourcefile destinationfile>" \n "put <sourcefile destinationfile>"
	 * > batchfile; sftp -o "batchmode no" -b <batchfile> -oPort=22
	 * gs3dev@130.20.107.80' The batch file contains series of put/get commands
	 * to be executed of sftp prompt.
	 * 
	 * @param srcDetails
	 *            - Source connection details
	 * @param srcFile
	 *            - Source file name
	 * @param destDetails
	 *            - Destination connection details
	 * @param destFile
	 *            - destination file name
	 * @param recursive
	 *            - true/false for directory
	 * @param sshObject
	 *            - used to connect to remote machine to get file list matching
	 *            wildcard
	 * @param cmdFile
	 *            - batch file name
	 * @return String - command which is to be run to copy files
	 * @throws Exception
	 */
	public String buildSftpWildcardBatch(ConnectionDetails srcDetails,
			String srcFile, ConnectionDetails destDetails, String destFile,
			boolean recursive, SftpExec sshObject, String cmdFile)
			throws Exception {

		StringBuffer warn = new StringBuffer(100);
		StringBuffer sftp_cmd = new StringBuffer(150);
		String cmdWithPath;
		Vector<String> fileList = null;
		try {
			System.out.println("***********WIld card detected :fname::"
					+ srcFile);
			if (srcFile.contains("//"))
				fileList = sshObject.getWildcardFileListing(srcFile, "\\");
			else
				fileList = sshObject.getWildcardFileListing(srcFile, "/");

			if (null != fileList) {
				sftp_cmd.append("echo -e \"");
				// loop for all the files in the list
				for (String curFile : fileList) {
					if (srcDetails.isConnectionOrigin())
						sftp_cmd.append("put ");
					else
						sftp_cmd.append("get ");
					sftp_cmd.append("\\\"");
					sftp_cmd.append(curFile.trim());
					sftp_cmd.append("\\\"");
					sftp_cmd.append(" ");
					sftp_cmd.append("\\\"");
					sftp_cmd.append(destFile);
					sftp_cmd.append("\\\"");
					sftp_cmd.append("\\n");
				}
			} else {
				warn.append("No files found for wildcard patter " + srcFile);
			}
			sftp_cmd.append("\" > ");
			sftp_cmd.append(cmdFile);
			sftp_cmd.append("; ");

			if (isDebugging) {
				log.debug("protocolpath=" + protocolPathSrc);
			}

			sftp_cmd.append(protocolPathSrc);
			sftp_cmd.append("sftp -o \"batchmode no\" -b ");
			sftp_cmd.append(cmdFile);
			sftp_cmd.append(" ");

			if (srcDetails.isConnectionOrigin()) {
				if (destDetails.getPort() != -1) {
					sftp_cmd.append(" -oPort=");
					sftp_cmd.append(destDetails.getPort());
					sftp_cmd.append(" ");
				}
				sftp_cmd.append(cmdLineOptions);
				sftp_cmd.append("  ");
				sftp_cmd.append(destDetails.getUser());
				sftp_cmd.append("@");
				sftp_cmd.append(destDetails.getHost());

				if (protocolPathSrc.equals("")) {
					cmdWithPath = getCmdWithDefaultPath(sftp_cmd);
				} else {
					cmdWithPath = "bash -c '" + sftp_cmd.toString() + " '";
				}
			} else {
				if (srcDetails.getPort() != -1) {
					sftp_cmd.append(" -oPort=");
					sftp_cmd.append(srcDetails.getPort());
					sftp_cmd.append(" ");
				}

				sftp_cmd.append(cmdLineOptions);
				sftp_cmd.append(" ");
				sftp_cmd.append(srcDetails.getUser());
				sftp_cmd.append("@");
				sftp_cmd.append(srcDetails.getHost());

				if (protocolPathSrc.equals("")) {
					cmdWithPath = getCmdWithDefaultPath(sftp_cmd);
				} else {
					cmdWithPath = "bash -c '" + sftp_cmd.toString() + " '";
				}
			}

			if (isDebugging)
				log.debug("remote copy cmd=" + cmdWithPath);

			return cmdWithPath;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Anand: Create a batch file for a file or directory and give that to sftp
	 * to perform copy This function creates batch file, and builds sftp copy
	 * command and returns it to calling function.
	 * 
	 * @param srcDetails
	 *            - Source connection details
	 * @param srcFile
	 *            - Source file name
	 * @param destDetails
	 *            - Destination connection details
	 * @param destFile
	 *            - destination file name
	 * @param recursive
	 *            - true/false for directory
	 * @param sshObject
	 *            - used to connect to remote machine to get file list matching
	 *            wildcard
	 * @param cmdFile
	 *            - batch file name
	 * @return String : command which is to be run to copy files
	 * @throws Exception
	 */
	public String buildSftpCommand(ConnectionDetails srcDetails,
			String srcFile, ConnectionDetails destDetails, String destFile,
			boolean recursive, SftpExec sshObject, String cmdFile)
			throws Exception {

		StringBuffer sftp_cmd = new StringBuffer();
		String cmdWithPath = null;
		sftp_cmd.append("echo -e \"");
		if (srcDetails.isConnectionOrigin()) {
			srcFile = srcFile.trim();
			// checks if the file is regular
			if (sshObject.isRegularOrLinkFile(srcFile.trim())) {
				sftp_cmd.append("put ");
				sftp_cmd.append("\\\"");
				sftp_cmd.append(srcFile);
				sftp_cmd.append("\\\"");
				sftp_cmd.append(" ");
				sftp_cmd.append("\\\"");
				sftp_cmd.append(destFile);
				sftp_cmd.append("\\\"");
			} else {
				if (recursive) {
					// TODO Anand: funtions getRecursiveCopyCmd does not handle
					// spaces in file names
					sftp_cmd.append(sshObject.getRecursiveCopyCmd(srcFile,
							destFile, true));
				} else {
					// print error
					throw new SshException(
							"Unable to determine file type of source file "
									+ srcFile + "\n");
				}
			}
		} else {
			if (sshObject.isRegularOrLinkFile(srcFile.trim())) {
				sftp_cmd.append("get ");
				sftp_cmd.append("\\\"");
				sftp_cmd.append(srcFile);
				sftp_cmd.append("\\\"");
				sftp_cmd.append(" ");
				sftp_cmd.append("\\\"");
				sftp_cmd.append(destFile);
				sftp_cmd.append("\\\"");
			} else {
				if (recursive) {
					// TODO Anand: funtions getRecursiveCopyCmd does not handle
					// spaces in file names
					sftp_cmd.append(sshObject.getRecursiveCopyCmd(srcFile,
							destFile, false));
				} else {
					// print error
					throw new SshException(
							"Unable to determine file type of source file "
									+ srcFile + "\n");
				}
			}
		}
		// sftp_cmd.append("this is \\\"my\\\" string");
		sftp_cmd.append("\" >> ");
		sftp_cmd.append(cmdFile);
		sftp_cmd.append("; ");

		if (isDebugging) {
			log.debug("protocolpath=" + protocolPathSrc);
		}

		sftp_cmd.append(protocolPathSrc);
		sftp_cmd.append("sftp -o \"batchmode no\" -b ");
		sftp_cmd.append(cmdFile);
		sftp_cmd.append(" ");

		if (srcDetails.isConnectionOrigin()) {
			if (destDetails.getPort() != -1) {
				sftp_cmd.append(" -oPort=");
				sftp_cmd.append(destDetails.getPort());
				sftp_cmd.append(" ");
			}
			sftp_cmd.append(cmdLineOptions);
			sftp_cmd.append("  ");
			sftp_cmd.append(destDetails.getUser());
			sftp_cmd.append("@");
			sftp_cmd.append(destDetails.getHost());

			if (protocolPathSrc.equals("")) {
				cmdWithPath = getCmdWithDefaultPath(sftp_cmd);
			} else {
				cmdWithPath = "bash -c '" + sftp_cmd.toString() + " '";
			}
		} else {
			if (srcDetails.getPort() != -1) {
				sftp_cmd.append(" -oPort=");
				sftp_cmd.append(srcDetails.getPort());
				sftp_cmd.append(" ");
			}

			sftp_cmd.append(cmdLineOptions);
			sftp_cmd.append(" ");
			sftp_cmd.append(srcDetails.getUser());
			sftp_cmd.append("@");
			sftp_cmd.append(srcDetails.getHost());

			if (protocolPathSrc.equals("")) {
				cmdWithPath = getCmdWithDefaultPath(sftp_cmd);
			} else {
				cmdWithPath = "bash -c '" + sftp_cmd.toString() + " '";
			}
		}

		if (isDebugging)
			log.debug("remote copy cmd=" + cmdWithPath);
		return cmdWithPath;

	}

	@Override
	protected int getDefaultPort() {
		return 22;
	}

}
