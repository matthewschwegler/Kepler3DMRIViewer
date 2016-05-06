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

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.ssh.ExecException;
import org.kepler.ssh.LocalExec;
import org.kepler.ssh.SshException;
import org.kepler.ssh.SshExec;
import org.sdm.spa.actors.transport.vo.ConnectionDetails;

/**
 * This class copies files/directories from one machine to another using the
 * srmlite protocol. It uses the <code>LocalExec</code> to copy file/directory
 * between local host and a remote host. To copy files between remote machines,
 * uses <code>SshExec</code>, connects to source machine using ssh and execute
 * srmlite command to copy to remote machine
 * <P>
 * scp is used as the default protocol passed to srmlite. It can be overriden by
 * setting the srmProtocol variable
 * <P>
 * Copy operation will overwrite existing files by default. Retry feature of
 * srmlite is not currently supported.
 * <P>
 * On the source machine, if srmlite is not on standard path and if the variable
 * protocolPathSrc is not set, the class attempts to search in the most common
 * locations - user's home directory, /usr/bin, /bin, and /usr/local/bin
 * 
 * @author Chandrika Sivaramakrishnan
 * 
 *         Anand: We assume that target location is always a directory.
 */
public class SrmliteCopier extends FileCopierBase {

	// /////////////////////Private Variables/////////////////////////////////
	private static final Log log = LogFactory.getLog(SrmliteCopier.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();
	private String srmProtocol = "";

	// //////////////////Protected Methods////////////////////////////////////

	/*
	 * Generates the srmlite command to copy from remote host to local host.
	 * Executes the command using LocalExec class
	 * 
	 * @see
	 * org.sdm.spa.actors.transport.FileCopier#copyFrom(org.sdm.spa.actors.transport
	 * .vo.ConnectionDetails, java.lang.String, java.lang.String, boolean)
	 */
	@Override
	// Anusua Change - Starts
	protected CopyResult copyFrom(ConnectionDetails srcDetails, String srcFile,
			String destFile, boolean recursive) throws SshException {
		int exitCode = 0;
		StringBuffer cmd = new StringBuffer(100);
		OutputStream streamOut = new ByteArrayOutputStream();
		OutputStream streamErr = new ByteArrayOutputStream();
		LocalExec localObject = new LocalExec();
		localObject.setTimeout(timeout, false, false);
		String[] srcFileList = null;
		String[] srcFile_list = null;
		StringBuffer warn = new StringBuffer(100);
		boolean warn_flag = false;
		String cmdWithPath;
		String osname = (System.getProperty("os.name")).toLowerCase();
		String userhome = (System.getProperty("user.home")).toLowerCase();
		String cmdFile = userhome + File.separatorChar + "srmlite_" + System.currentTimeMillis() + ".xml";


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
			if (warn_flag) {
				warn
						.append(" does not contain full path to the source file. Please provide full path. ");
				warn_flag = false;
			}
		} else {
			if (!srcFile.startsWith("/")) {
				throw new SshException(
						srcFile
								+ "does not contain full path to the file. Please provide full path.");
			}
		}
		try {
			if (srcFile.contains(",")) {
				build_xmlForRemoteSource(cmdFile, srcFile_list, srcDetails);
			}
		} catch (Exception e) {// Catch exception if any
			e.printStackTrace();
			return new CopyResult(1, e.getMessage(), warn.toString());
		}

		// command format
		// "<path>/srmlite -s file:////<srcfile> -t scp://user@host/<dest path>"
		// srmlite -f <cmdFile>
		if (srcFile.contains(",")) {
			if (osname.contains("windows"))
				cmd.append("srmlite.bat -f ");
			else
				cmd.append("srmlite -f ");
			cmd.append("file:///");
			cmd.append(cmdFile);
			cmd.append(" -td ");
		} else {
			if (osname.contains("windows"))
				cmd.append("srmlite.bat -s ");
			else
				cmd.append("srmlite -s ");
			cmd.append("\"");
			if (srmProtocol.equals("")) {
				cmd.append("scp://");
			} else {
				cmd.append(srmProtocol);
				cmd.append("://");
			}
			cmd.append(srcDetails.toString());
			// Anand: we dont need this /
			// cmd.append("/");
			cmd.append(srcFile);
			cmd.append("\"");
			if (recursive) {
				cmd.append(" -recursive");
			}
			File tempSrcFile = new File(destFile);
			if (tempSrcFile.isDirectory())
				cmd.append(" -td ");
			else
				cmd.append(" -t ");
		}
		cmd.append("file:///");
		cmd.append(destFile);

		if (protocolPathSrc.equals("")) {
			if (osname.contains("windows")) {
				cmdWithPath = cmd.toString();
			} else {
				cmdWithPath = getCmdWithDefaultPath(cmd);
			}
		} else {
			cmdWithPath = protocolPathSrc + cmd;
		}
		// TODO Anand: Adjust path variable
		// cmdWithPath = "C:\\Projects\\srmlite\\bin\\" + cmd;

		System.out.println("*************Full command executed is ::  "
				+ cmdWithPath);
		try {
			if (isDebugging)
				log.debug("copy cmd=" + cmdWithPath);
			streamOut = new ByteArrayOutputStream();
			streamErr = new ByteArrayOutputStream();
			try {
				exitCode = localObject.executeCmd(cmdWithPath, streamOut,
						streamErr, srcDetails.toString());
			} catch (ExecException e) {
				return new CopyResult(exitCode, e.toString(), "");
			}
			if (isDebugging) {
				log.error("Output on stdout:" + streamOut);
				log.error("Output on stderr:" + streamErr);
			}
		} catch (Exception e) {
			return new CopyResult(1, "SRMLite Copy failed!\n"
					+ streamErr.toString(), "");
		}
		String message = streamErr.toString();
		message = message + " \n\n" + streamOut.toString();
		String fileDeleteMsg = "";
		if (srcFile.contains(",")) {
			try {
				boolean success = false;
				success = localObject.deleteFile(cmdFile, false, false);
				if (success) {
					log.debug("deleted the xml script file " + cmdFile
							+ " created for file copy");
				}
				if (!success) {
					log.warn("Unable to delete the xml script file " + cmdFile
							+ " created for file copy");
					fileDeleteMsg = "Unable to delete the xml script file "
							+ cmdFile + " created for file copy";
				}
			} catch (ExecException e) {
				log.warn("Unable to delete the xml script file " + cmdFile
						+ " created for file copy : " + e.toString());
			}
		}
		return new CopyResult(exitCode, message + fileDeleteMsg, "");
	}

	private void build_xmlForRemoteSource(String fileName,
			String[] srcFile_list, ConnectionDetails srcDetails)
			throws Exception {
		FileWriter fstream;
		fstream = new FileWriter(fileName);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		out.write("\n");
		out.write("<request>");
		out.write("\n");

		SshExec sshObject = new SshExec(srcDetails.getUser(), srcDetails
				.getHost(), srcDetails.getPort());

		for (int i = 0; i < srcFile_list.length; i++) {
			out.write("<file>");
			out.write("\n");
			out.write("<sourceurl>");
			if (srmProtocol.equals("")) {
				out.write("scp://");
			} else {
				out.write(srmProtocol);
				out.write("://");
			}
			out.write(srcDetails.toString());
			out.write("/");
			out.write(srcFile_list[i]);
			out.write("</sourceurl>");
			out.write("\n");
			// if src_file is not a file add recursive
			if (sshObject.isRemoteFileDirectory(srcFile_list[i])) {
				out.write("<recursive>");
				out.write("true");
				out.write("</recursive>");
				out.write("\n");
			}
			out.write("</file>");
			out.write("\n");
		}
		out.write("</request>");
		out.close();
		System.out.println("exit function build xml****************");
	}

	/*
	 * Generates the srmlite command to copy from local host to remote host.
	 * Executes the command using LocalExec class
	 * 
	 * @see org.sdm.spa.actors.transport.FileCopier#copyTo(java.lang.String,
	 * org.sdm.spa.actors.transport.vo.ConnectionDetails, java.lang.String,
	 * boolean)
	 */
	@Override
	protected CopyResult copyTo(String srcFile, ConnectionDetails destDetails,
			String destFile, boolean recursive) throws SshException {
		int exitCode = 0;
		String cmdWithPath = null;

		StringBuffer cmd = new StringBuffer(100);
		LocalExec localObject = new LocalExec();
		localObject.setTimeout(timeout, false, false);
		String[] srcFileList = null;
		String[] srcFile_list = null;
		StringBuffer warn = new StringBuffer(100);
		boolean warn_flag = false;
		String osname = (System.getProperty("os.name")).toLowerCase();
		String cmdFile = "srmlite_" + System.currentTimeMillis() + ".xml";
		String userhome = (System.getProperty("user.home")).toLowerCase();

		// srmlite -f xml_file -td target_directory
		if (srcFile.contains(",")) { // List of files is provided
			srcFileList = srcFile.split(",");
			srcFile_list = new String[srcFileList.length];
			for (int count = 0; count < srcFileList.length; count++) {
				if ((srcFileList[count].trim()).startsWith("/")
						|| (srcFileList[count].trim()).startsWith(":/", 1) 
						|| (srcFileList[count].trim()).contains(":\\")) {
					srcFile_list[count] = srcFileList[count].trim();
				} else {
					warn.append(srcFile_list[count].trim() + " ");
					if (!warn_flag)
						warn_flag = true;
				}
			}
			if (warn_flag) {
				warn
						.append(" does not contain full path to the source file. Please provide full path. ");
				warn_flag = false;
			}
		}

		try {
			if (srcFile.contains(",")) {
				build_xml_ForLocalSource(userhome + File.separatorChar
						+ cmdFile, srcFile_list, destFile);
			}
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}

		if (srcFile.contains(",")) {
			// command format
			// "<path>/srmlite -f xml_input -td scp://user@host/<dest path>"
			if (osname.contains("windows"))
				cmd.append("srmlite.bat -f ");
			else
				cmd.append("srmlite -f ");
			cmd.append("file:///");
			cmd.append(userhome + File.separatorChar + cmdFile);
			cmd.append(" -td ");
			if (srmProtocol.equals("")) {
				cmd.append("scp://");
			} else {
				cmd.append(srmProtocol);
				cmd.append("://");
			}
			cmd.append(destDetails.toString());
			cmd.append(destFile);
			// cmd.append("/");
		} else {
			// command format
			// "<path>/srmlite -s file:////<srcfile> -t scp://user@host/<dest path>"
			if (osname.contains("windows"))
				cmd.append("srmlite.bat -s ");
			else
				cmd.append("srmlite -s ");
			cmd.append("\"");
			// Anand: might need file:////
			cmd.append("file:///");
			cmd.append(srcFile);
			cmd.append("\"");
			if (recursive)
				cmd.append(" -recursive");
			// TODO: We assume that target is always a directory.
			cmd.append(" -td ");

			if (srmProtocol.equals("")) {
				cmd.append("scp://");
			} else {
				cmd.append(srmProtocol);
				cmd.append("://");
			}

			cmd.append(destDetails.toString());
			// Anand: Commented because it shows as host@ip//root instead of
			// /root
			// cmd.append("/");
			cmd.append(destFile);
		}

		log.debug("Command without path = " + cmd);

		if (protocolPathSrc.equals("")) {
			if (osname.contains("windows")) {
				System.out.println("OSNAME WINDOWS*****************");
				cmdWithPath = cmd.toString();
			} else {
				cmdWithPath = getCmdWithDefaultPath(cmd);
			}
		} else {
			cmdWithPath = protocolPathSrc + cmd;
		}
		
		log.debug("*************Full command executed is ::  "
				+ cmdWithPath);

		if (isDebugging)
			log.debug("copy cmd=" + cmdWithPath);
		OutputStream streamOut = new ByteArrayOutputStream();
		OutputStream streamErr = new ByteArrayOutputStream();
		try {
			exitCode = localObject.executeCmd(cmdWithPath, streamOut,
					streamErr, destDetails.toString());
		} catch (Exception e) {
			return new CopyResult(1, e.toString(), "");
		}
		if (isDebugging) {
			log.error("Output on stdout:" + streamOut);
			log.error("Output on stderr:" + streamErr);
		}
		String message = streamErr.toString();
		message = message + " \n\n" + streamOut.toString();
		String fileDeleteMsg = "";
		if (srcFile.contains(",")) {
			try {
				boolean success = false;
				success = localObject.deleteFile(cmdFile, false, false);
				if (success) {
					log.debug("deleted the xml script file " + cmdFile
							+ " created for file copy");
				}
				if (!success) {
					log.warn("Unable to delete the xml script file " + cmdFile
							+ " created for file copy");
					fileDeleteMsg = "Unable to delete the xml script file "
							+ cmdFile + " created for file copy";
				}
			} catch (ExecException e) {
				log.warn("Unable to delete the xml script file " + cmdFile
						+ " created for file copy : " + e.toString());
			}
		}
		return new CopyResult(exitCode, message + fileDeleteMsg, "");
	}

	public void build_xml_ForLocalSource(String fileName, String[] fileList,
			String destFile) throws IOException {
		FileWriter fstream;
		fstream = new FileWriter(fileName);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		out.write("\n");
		out.write("<request>");
		out.write("\n");
		for (int i = 0; i < fileList.length; i++) {
			out.write("<file>");
			out.write("\n");
			out.write("<sourceurl>");
			/*
			 * if (destFile.startsWith("/")) { out.write("file:////"); } else {
			 * out.write("file:////"); }
			 */
			out.write("file:////");
			out.write(fileList[i]);
			out.write("</sourceurl>");
			out.write("\n");

			File fileObj = new File(fileList[i]);
			if (!fileObj.isFile()) {// if the file is a dir or wildcard
				out.write("<recursive>");
				out.write("true");
				out.write("</recursive>");
				out.write("\n");
			}
			out.write("</file>");
			out.write("\n");
		}
		out.write("</request>");
		// Close the output stream
		out.close();
	}

	/*
	 * Generates the srmlite command to copy from a remote host to another
	 * remote host. Executes the command using SshExec class
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
			log.debug("remote srmlite copy");
		ByteArrayOutputStream cmdStdout = new ByteArrayOutputStream();
		ByteArrayOutputStream cmdStderr = new ByteArrayOutputStream();
		String remoteHostStr = "";
		SshExec sshObjectSrc = null;
		SshExec sshObjectDest = null;
		// String file_transfer_cmd = null;
		String filehome = null;
		// File single_srcFileObj = null;
		// File mult_srcFileObj = null;
		String[] srcFileList = null;
		String[] srcFile_list = null;
		StringBuffer warn = new StringBuffer(100);
		boolean warn_flag = false;
		// FileWriter fstream;
		StringBuffer cmd = new StringBuffer(100);
		String cmdWithPath = null;
		int exitCode = 0;
		OutputStream streamOut = new ByteArrayOutputStream();
		OutputStream streamErr = new ByteArrayOutputStream();
		StringBuffer xml_file_contents = new StringBuffer();

		String osname = (System.getProperty("os.name")).toLowerCase();
		String cmdFile = "srmlite_" + System.currentTimeMillis() + ".xml";
		String userhome = (System.getProperty("user.home")).toLowerCase();
		String getRemoteHome_Cmd = "echo $HOME";

		// File destfileobj = new File(destFile);
		// File srcFileObj = new File(srcFile);

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
			if (warn_flag) {
				warn
						.append(" does not contain full path to the source file. Please provide full path. ");
				warn_flag = false;
			}
		} else {
			if (!srcFile.startsWith("/")) {
				throw new SshException(
						srcFile
								+ "does not contain full path to the file. Please provide full path.");
			}
		}
		sshObjectSrc = new SshExec(srcDetails.getUser(), srcDetails.getHost(),
				srcDetails.getPort());
		sshObjectDest = new SshExec(destDetails.getUser(), destDetails
				.getHost(), destDetails.getPort());
		if (srcDetails.isConnectionOrigin()) {
			// Conenct to source by ssh
			System.out.println("***********SRC is connection origin");

			remoteHostStr = destDetails.toString();
			exitCode = sshObjectSrc.executeCmd(getRemoteHome_Cmd, streamOut,
					streamErr, remoteHostStr);
			filehome = streamOut.toString().trim();
			sshObjectSrc.setTimeout(timeout, false, false);
			sshObjectSrc.setForcedCleanUp(forcedCleanup);
			// srmlite doesn't need a pseudo terminal
			sshObjectSrc.setPseudoTerminal(false);

		} else {
			System.out.println("***********DEST is connection origin");
			remoteHostStr = srcDetails.toString();
			exitCode = sshObjectDest.executeCmd(getRemoteHome_Cmd, streamOut,
					streamErr, remoteHostStr);
			filehome = streamOut.toString().trim();
			sshObjectDest.setTimeout(timeout, false, false);
			sshObjectDest.setForcedCleanUp(forcedCleanup);
			// srmlite doesn't need a pseudo terminal
			sshObjectDest.setPseudoTerminal(false);
		}

		cmdFile = filehome + "/" + cmdFile;

		if (srcFile.contains(",")) {
			System.out.println("***********Building xml file");
			try {
				// Create xml file
				// fstream = new FileWriter(userhome + File.separatorChar
				// + cmdFile);
				// BufferedWriter out = new BufferedWriter(fstream);

				xml_file_contents
						.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
				xml_file_contents.append("\n");
				xml_file_contents.append("<request>");
				xml_file_contents.append("\n");

				if (srcDetails.isConnectionOrigin()) {
					// Create list of source files to be copied
					for (int i = 0; i < srcFile_list.length; i++) {
						xml_file_contents.append("<file>");
						xml_file_contents.append("\n");
						xml_file_contents.append("<sourceurl>");
						xml_file_contents.append("file:///");
						xml_file_contents.append(srcFile_list[i]);
						xml_file_contents.append("</sourceurl>");
						xml_file_contents.append("\n");
						// File fileObj = new File(srcFile_list[i]);
						if (sshObjectSrc.isRemoteFileDirectory(srcFile_list[i])) {
							// if the file is a dir
							xml_file_contents.append("<recursive>");
							xml_file_contents.append("true");
							xml_file_contents.append("</recursive>");
							xml_file_contents.append("\n");
						}
						xml_file_contents.append("</file>");
						xml_file_contents.append("\n");
					}
					xml_file_contents.append("</request>");
					// Close the output stream
					// out.close();
				} else {
					for (int i = 0; i < srcFile_list.length; i++) {
						xml_file_contents.append("<file>");
						xml_file_contents.append("\n");
						xml_file_contents.append("<sourceurl>");
						if (srmProtocol.equals("")) {
							xml_file_contents.append("scp://");
						} else {
							xml_file_contents.append(srmProtocol);
							xml_file_contents.append("://");
						}
						xml_file_contents.append(srcDetails.toString());
						xml_file_contents.append("/");
						xml_file_contents.append(srcFile_list[i]);
						xml_file_contents.append("</sourceurl>");
						xml_file_contents.append("\n");
						if (sshObjectSrc.isRemoteFileDirectory(srcFile_list[i])) {
							// if the file is a dir
							xml_file_contents.append("<recursive>");
							xml_file_contents.append("true");
							xml_file_contents.append("</recursive>");
							xml_file_contents.append("\n");
						}
						xml_file_contents.append("</file>");
						xml_file_contents.append("\n");
					}
					xml_file_contents.append("</request>");
					// out.close();
				}
			} catch (Exception e) {// Catch exception if any
				System.err
						.println("Error creating xml file: " + e.getMessage());
			}

			System.out.println("**************Xml file created...");
			System.out.println("**************Transferring sml file...");
			try {
				String temp_cmd = "echo -e '" + xml_file_contents + "' >> "
						+ cmdFile;
				System.out.println("***************XML command : " + temp_cmd);
				if (srcDetails.isConnectionOrigin())
					exitCode = sshObjectSrc.executeCmd(temp_cmd, streamOut,
							streamErr, remoteHostStr);
				else
					exitCode = sshObjectDest.executeCmd(temp_cmd, streamOut,
							streamErr, remoteHostStr);
				if (exitCode != 0)
					throw new Exception(
							"XML file creation failed with exit code : "
									+ exitCode);
			} catch (Exception e) {
				return new CopyResult(1,
						"Failed to copy XML file to remote source!!!", e
								.toString());
			}
		}// end of build & transfer xml file
		/*
		 * //command format
		 * //"<path>/srmlite -s file:////<srcfile> -t scp://user@host/<dest path>"
		 */

		if (srcDetails.isConnectionOrigin()) {
			System.out
					.println("*********************Building srmlite copy command");

			if (srcFile.contains(",")) {// list of files is provided.
				// TODO Anand: Do not use srmlite.bat - this is always a linux
				// machine
				cmd.append("srmlite -f ");
				cmd.append("file:///");
				cmd.append(cmdFile);
				cmd.append(" -td ");
			} else { // single file/directory to be copied
				// TODO Anand: Do not use srmlite.bat - this is always a linux
				// machine
				cmd.append("srmlite -s ");
				cmd.append("\"");
				cmd.append("file:///");
				cmd.append(srcFile);
				cmd.append("\"");
				if (recursive) {
					cmd.append(" -recursive");
				}
				try {
					if (sshObjectDest.isRemoteFileDirectory(destFile)) {
						// target is a directory
						cmd.append(" -td ");
					} else {// target is a file
						cmd.append(" -t ");
					}
				} catch (Exception e) {
					return new CopyResult(1, destFile + " not found!!!", e
							.toString());
				}
			}// single file else ends

			if (srmProtocol.equals("")) {
				cmd.append("scp://");
			} else {
				cmd.append(srmProtocol);
				cmd.append("://");
			}
			cmd.append(destDetails.toString());
			cmd.append(destFile);
		} else {
			// Anand: destination is selected as origin

			System.out
					.println("************* Building srmlite command: *****************");

			if (srcFile.contains(",")) {
				// TODO Anand: Add/remove .bat - this is always a linux machine
				cmd.append("srmlite -f ");
				cmd.append("file:///");
				cmd.append(cmdFile);
				cmd.append(" -td ");
			} else {
				// TODO Anand: Add/remove .bat - this is always a linux machine
				cmd.append("srmlite -s ");
				cmd.append("\"");
				if (srmProtocol.equals("")) {
					cmd.append("scp://");
				} else {
					cmd.append(srmProtocol);
					cmd.append("://");
				}
				cmd.append(srcDetails.toString());
				// cmd.append("/");
				cmd.append(srcFile);
				cmd.append("\"");
				if (recursive) // if copying directories
					cmd.append(" -recursive");

				try {
					if (sshObjectDest.isRemoteFileDirectory(destFile)) {
						// -td is used if target is a directory.
						cmd.append(" -td ");
					} else {
						cmd.append(" -t ");
					}
				} catch (Exception e) {
					return new CopyResult(1, destFile + " not found!!!", e
							.toString());
				}
			}
			cmd.append("file:///");
			cmd.append(destFile);
		}
		if (srcDetails.isConnectionOrigin()) {
			if (protocolPathSrc.equals("")) {
				if (osname.contains("windows")) {
					cmdWithPath = cmd.toString();
				} else {
					cmdWithPath = getCmdWithDefaultPath(cmd);
				}
			} else {
				cmdWithPath = protocolPathSrc + cmd;
			}
		} else {
			if (protocolPathDest.equals("")) {
				if (osname.contains("windows")) {
					cmdWithPath = cmd.toString();
				} else {
					cmdWithPath = getCmdWithDefaultPath(cmd);
				}
			} else {
				cmdWithPath = protocolPathDest + cmd;
			}
		}

		System.out.println("****Srmlite command executed is : " + cmdWithPath);

		try {
			if (srcDetails.isConnectionOrigin())
				exitCode = sshObjectSrc.executeCmd(cmdWithPath, cmdStdout,
						cmdStderr, remoteHostStr);
			else
				exitCode = sshObjectDest.executeCmd(cmdWithPath, cmdStdout,
						cmdStderr, remoteHostStr);
		} catch (ExecException e) {
			return new CopyResult(1, e.toString(), "");
		}
		if (isDebugging) {
			log.error("Output on stdout:" + cmdStdout);
			log.error("Output on stderr:" + cmdStderr);
		}
		String message = cmdStderr.toString();
		message = message + " \n\n" + cmdStdout.toString();

		String fileDeleteMsg = "";
		try {
			boolean success = false;
			if (srcDetails.isConnectionOrigin())
				success = sshObjectSrc.deleteFile(cmdFile, false, false);
			else
				success = sshObjectDest.deleteFile(cmdFile, false, false);

			if (success) {
				log.debug("deleted the xml script file " + cmdFile
						+ " created for file copy");
			}
			if (!success) {
				log.warn("Unable to delete the xml script file " + cmdFile
						+ " created for file copy");
				fileDeleteMsg = "Unable to delete the xml script file "
						+ cmdFile + " created for file copy";
			}
		} catch (ExecException e) {
			log.warn("Unable to delete the xml script file " + cmdFile
					+ " created for file copy : " + e.toString());
		}
		return new CopyResult(exitCode, message + fileDeleteMsg, "");
	}

	@Override
	protected int getDefaultPort() {
		return -1;
	}

	public String getSrmProtocol() {
		return srmProtocol;
	}

	public void setSrmProtocol(String srmProtocol) {
		if (null == srmProtocol) {
			this.srmProtocol = "";
		} else {
			this.srmProtocol = srmProtocol.trim();
		}
	}

	// New Method added by ANUSUA
	protected int fileListCopyFrom(String cmdWithPath, String HostStr)
			throws Exception {

		int exitCode = 0;
		LocalExec localObject = new LocalExec();
		localObject.setTimeout(timeout, false, false);
		OutputStream streamOut = new ByteArrayOutputStream();
		OutputStream streamErr = new ByteArrayOutputStream();
		exitCode = localObject.executeCmd(cmdWithPath, streamOut, streamErr,
				HostStr);
		if (isDebugging) {
			log.error("Output on stdout:" + streamOut);
			log.error("Output on stderr:" + streamErr);
		}
		String message = streamErr.toString();
		message = message + " \n\n" + streamOut.toString();
		log.debug(message);
		return exitCode;
	}

}