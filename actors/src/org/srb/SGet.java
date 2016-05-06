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

package org.srb;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.Vector;

import ptolemy.actor.NoTokenException;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.ObjectToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.util.MessageHandler;
import edu.sdsc.grid.io.local.LocalFile;
import edu.sdsc.grid.io.srb.SRBFile;
import edu.sdsc.grid.io.srb.SRBFileSystem;
import edu.sdsc.grid.io.srb.SRBRandomAccessFile;

//////////////////////////////////////////////////////////////////////////
//// SGet
/**
 * <p>
 * SGet is a Kepler Actor which has a functionality similar to the SRB command
 * namely "Sget". Sget exports one or more objects from SRB space into the local
 * file system. SGet actor downloads an SRB file to the local drive. The
 * following actor expects as input a reference to the SRB file system. This
 * reference connection is obtained via the SRBConnect Actor in Kepler. <i>See
 * SRBConnect and its documentation.</i>
 * </p>
 * <p>
 * The file reference system is created with a unique SRB user account and with
 * this connection reference as input the SGet actor is able to gain access to
 * various files on the SRB file systems. Once an alive SRB file connection
 * system has been established the actor gets the local directory and the files
 * to establish the SRB file path. If the SRB directory doesn't exist, a new
 * directory is created. Once the SRB files path are determined, the files are
 * copied from the SRB file space to the local drive.In case the above process
 * of parallel copy fails, a streaming copy process is carried out with
 * SRBRandomAccess streams where the file is downloaded as a sequence of byte
 * arrays.
 * </p>
 * <p>
 * The user is also asked for confirmation on overwriting existing local files
 * if they exist or simply appending them.
 * </p>
 * <p>
 * <B>Actor Input:</B> Accepts a reference to the SRB files system, a local
 * directory and an aray of SRB remote file paths.
 * </p>
 * <p>
 * <B>Actor Output:</B> Outputs the local file paths and an exit status. The
 * exit status gives a message of "success" or appropriate error to indicate the
 * status of file get process.
 * 
 * </p>
 * <p>
 * The following actor accesses SRB file reference system and SRB file space
 * with the SRB Jargon API provided. The JARGON is a pure API for developing
 * programs with a data grid interface and I/O for SRB file systems.
 * </p>
 * <A href="http://www.sdsc.edu/srb"><I>Further information on SRB</I> </A>
 * 
 * @author Efrat Jaeger
 * @version $Id: SGet.java 24234 2010-05-06 05:21:26Z welker $
 * @category.name srb
 * @category.name put
 */

public class SGet extends TypedAtomicActor {

	/**
	 * Construct a constant source with the given container and name. Create the
	 * <i>value</i> parameter, initialize its value to the default value of an
	 * IntToken with value 1.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public SGet(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		SRBFileSystem = new TypedIOPort(this, "SRBFileSystem", true, false);
		SRBFileSystem.setTypeEquals(BaseType.GENERAL);
		new Attribute(SRBFileSystem, "_showName");

		filesToGet = new TypedIOPort(this, "filesToGet", true, false);
		filesToGet.setTypeEquals(new ArrayType(BaseType.STRING));
		new Attribute(filesToGet, "_showName");

		fetchedFiles = new TypedIOPort(this, "fetchedFiles", false, true);
		fetchedFiles.setTypeEquals(new ArrayType(BaseType.STRING));
		new Attribute(fetchedFiles, "_showName");

		exitCode = new TypedIOPort(this, "exitCode", false, true);
		exitCode.setTypeEquals(BaseType.STRING);
		new Attribute(exitCode, "_showName");

		localDir = new TypedIOPort(this, "localDir", true, false);
		localDir.setTypeEquals(BaseType.STRING);
		new Attribute(localDir, "_showName");

		localDirParameter = new FileParameter(this, "localDirParameter");
		localDirParameter.setDisplayName("local Dir");

		append = new Parameter(this, "append");
		append.setTypeEquals(BaseType.BOOLEAN);
		append.setToken(BooleanToken.FALSE);

		confirmOverwrite = new Parameter(this, "confirmOverwrite");
		confirmOverwrite.setTypeEquals(BaseType.BOOLEAN);
		confirmOverwrite.setToken(BooleanToken.FALSE);

		_attachText("_iconDescription", "<svg>\n"
				+ "<rect x=\"-25\" y=\"-20\" " + "width=\"50\" height=\"40\" "
				+ "style=\"fill:white\"/>\n"
				+ "<polygon points=\"-15,-10 -12,-10 -8,-14 -1,-14 3,-10"
				+ " 15,-10 15,10, -15,10\" " + "style=\"fill:red\"/>\n"
				+ "<text x=\"-13.5\" y=\"7\" " + "style=\"font-size:14\">\n"
				+ "SRB \n" + "</text>\n" + "<text x=\"-12\" y=\"19\""
				+ "style=\"font-size:11; fill:black; font-family:SansSerif\">"
				+ "SGet</text>\n" + "</svg>\n");

	}

	/**
	 * SRB file system reference.
	 */
	public TypedIOPort SRBFileSystem;

	/**
	 * Paths to the SRB files to fetch
	 */
	public TypedIOPort filesToGet;

	/**
	 * Paths to the local location.
	 */
	public TypedIOPort fetchedFiles;

	/**
	 * Exit status of the operation.
	 */
	public TypedIOPort exitCode;

	/**
	 * Where to fetch the files on the local drive.
	 */
	public TypedIOPort localDir;

	/**
	 * Where to fetch the files on the local drive.
	 */
	public FileParameter localDirParameter;

	/**
	 * Overwrite when SRB file is copied to loalFile
	 */
	public Parameter confirmOverwrite;

	/**
	 * Append when SRB file is copied to loalFile
	 */
	public Parameter append;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////
	/**
	 * Upload the file to the SRB. If the SRB file path is not specified, upload
	 * to the current working directory. Output the current working directory.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown if the SRB file cannot be accessed or the
	 *                current directory cannot be broadcasted.
	 */
	public void fire() throws IllegalActionException {

		SRBFile srbFile;
		LocalFile localFile;
		String localFilePath;
		String _exitCode = "";

		try {
			// make sure there is an alive connection.
			try {
				srbFileSystem.getHost();
			} catch (Exception ex) { // connection was closed.
				srbFileSystem = null;
				ObjectToken SRBConOT = null;
				try { // try to get a new connection in case the previous one
						// has terminated.
					SRBConOT = (ObjectToken) SRBFileSystem.get(0);
				} catch (NoTokenException ntex) {
				}
				if (SRBConOT != null) {
					srbFileSystem = (SRBFileSystem) SRBConOT.getValue();
				}
			}
			if (srbFileSystem != null) {
				// The local directory to fetch the files.
				if (localDir.getWidth() > 0) {
					String localDirStr = ((StringToken) localDir.get(0))
							.stringValue();
					localDirParameter.setExpression(localDirStr);
				}

				String localDirStr = ((StringToken) localDirParameter
						.getToken()).stringValue();

				if (localDirStr.startsWith("file:/")) {
					localDirStr = localDirStr.substring(6);
					while (localDirStr.startsWith("/")) {
						localDirStr = localDirStr.substring(1);
					}
				}

				localDirStr = URLDecoder.decode(localDirStr);

				File dir = new File(localDirStr);
				// if the directory doesn't exist, create it.
				if (!dir.exists()) {
					dir.mkdirs();
				}

				// Getting the srb files list token and copying each file to the
				// local drive.
				ArrayToken srbFilesTokenArr = null;
				try {
					srbFilesTokenArr = (ArrayToken) filesToGet.get(0);
				} catch (Exception ex) {
					_debug("filesToGet port is null.");
				}
				if (srbFilesTokenArr != null) {
					Token[] srbFilesToken = srbFilesTokenArr.arrayValue();
					Vector fetchedFilesVec = new Vector();
					for (int i = 0; i < srbFilesToken.length; i++) {

						// srb file path.
						String srbFileStr = ((StringToken) srbFilesToken[i])
								.stringValue();
						_debug("<FILE_TO_GET>" + srbFileStr + "<FILE_TO_GET>");

						srbFile = new SRBFile(srbFileSystem, srbFileStr);
						if (srbFile.exists()) {
							// setting the local file path.
							int slashIndex = srbFileStr.lastIndexOf('/');
							if (slashIndex == -1) {
								srbFileSystem = SRBUtil
										.closeConnection(srbFileSystem);
								throw new IllegalActionException(
										"No absolute srb file path!");
							}
							String tmpFileNameString = srbFileStr
									.substring(slashIndex + 1);

							localFilePath = dir.getAbsolutePath() + "/";
							localFilePath += tmpFileNameString;

							_debug("<LOCAL_FILE_PATH>" + localFilePath
									+ "</LOCAL_FILE_PATH>");

							// copying the SRB file to the local drive.
							localFile = new LocalFile(localFilePath);

							boolean appendValue = ((BooleanToken) append
									.getToken()).booleanValue();
							boolean confirmOverwriteValue = ((BooleanToken) confirmOverwrite
									.getToken()).booleanValue();
							// Don't ask for confirmation in append mode, since
							// there
							// will be no loss of data.
							if (localFile.exists() && !appendValue
									&& confirmOverwriteValue) {
								// Query for overwrite.
								// FIXME: This should be called in the event
								// thread!
								// There is a chance of deadlock since it is
								// not.
								if (!MessageHandler
										.yesNoQuestion("OK to overwrite "
												+ localFile + "?")) {
									srbFileSystem = SRBUtil
											.closeConnection(srbFileSystem);
									throw new IllegalActionException(this,
											"Please select another file name.");
								}
							}
							try {
								srbFile.copyTo(localFile, !appendValue);
								fetchedFilesVec.add(new StringToken(localFile
										.getAbsolutePath()));
							} catch (Exception ex) {
								// If the paralel copy fails try to do a stream
								// copy.
								System.out.println("Paralel get failed due to "
										+ ex.getMessage());
								System.out.println("Trying Stream get.");
								try {
									if (localFile.exists()) {
										localFile.delete();
									}
									_streamGet(srbFile, localFile
											.getAbsolutePath());
									// adding successfully fetched files output
									// path to the fetched files array.
									fetchedFilesVec.add(new StringToken(
											localFile.getAbsolutePath()));

								} catch (Exception stex) {
									stex.printStackTrace();
									// even if there is an execption output the
									// successfully fetched files.
									System.out.println("failed to copy file "
											+ srbFileStr + " to "
											+ localFilePath + ".");
									_exitCode += "unable to fetch file "
											+ srbFileStr;
									_exitCode += " to " + localFilePath + ".\n";
								}
							}
						} else {
							System.out.println("file " + srbFileStr
									+ " does not exist.");
							_exitCode += "file " + srbFileStr
									+ " does not exist.\n";
						}
					}

					Token[] fetchedFilesArr = new StringToken[fetchedFilesVec
							.size()];
					fetchedFilesVec.toArray(fetchedFilesArr);
					// broadcast the array only if it's non-empty
					if (fetchedFilesArr.length > 0) {
						fetchedFiles.broadcast(new ArrayToken(fetchedFilesArr));
					} else {
						_exitCode = "no files were fetched.";
					}
					if (_exitCode.equals("")) {
						_exitCode = "success";
					}
					exitCode.broadcast(new StringToken(_exitCode));
				} else {
					// no more files to get.
					_refire = false;
				}
			} else
				throw new IllegalActionException(this,
						"No SRB connection available in actor "
								+ this.getName() + ".");
		} catch (Exception ex) {
			ex.printStackTrace();
			srbFile = null;
			srbFileSystem = SRBUtil.closeConnection(srbFileSystem);
			throw new IllegalActionException(this, ex.getMessage());
		}

	}

	/**
	 * Initialize the srb file system to null.
	 */
	public void initialize() throws IllegalActionException {
		super.initialize();
		srbFileSystem = null;
	}

	/**
	 * Post fire the actor. Return false to indicated that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 */
	public boolean postfire() throws IllegalActionException {
		if (_refire) {
			return super.postfire();
		} else
			return _refire;
	}

	/**
	 * Reset the _refire variable and disconnect
	 */
	public void wrapup() {
		_refire = true;
		srbFileSystem = SRBUtil.closeConnection(srbFileSystem);
	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	/**
	 * Stream read the file. Use in case the parallel get fails.
	 */
	private void _streamGet(SRBFile srbFile, String localFilePath)
			throws Exception {

		SRBRandomAccessFile srbRandomAccessFile = null;
		byte[] bytesRead = new byte[20000];
		;
		int nBytesRead;
		OutputStream out = new FileOutputStream(localFilePath);

		srbRandomAccessFile = new SRBRandomAccessFile(srbFile, "r");
		nBytesRead = srbRandomAccessFile.read(bytesRead);
		while (nBytesRead > 0) {
			out.write(bytesRead);
			nBytesRead = srbRandomAccessFile.read(bytesRead);
		}
		out.close();
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	/**
	 * Indicator whether the actor should fire again
	 */
	private boolean _refire = true;

	/**
	 * SRB file system reference.
	 */
	private SRBFileSystem srbFileSystem = null;
}