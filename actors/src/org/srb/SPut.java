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

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Vector;

import ptolemy.actor.NoTokenException;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.ObjectToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.attributes.URIAttribute;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.util.MessageHandler;
import edu.sdsc.grid.io.local.LocalFile;
import edu.sdsc.grid.io.srb.SRBFile;
import edu.sdsc.grid.io.srb.SRBFileSystem;
import edu.sdsc.grid.io.srb.SRBRandomAccessFile;

//////////////////////////////////////////////////////////////////////////
//// SPut
/**
 * <p>
 * SPut is a Kepler Actor which has a functionality similar to the SRB command
 * namely "Sput". Sput imports one or more local files and/or directories into
 * SRB space. SPut actor uploads a local file to the SRB. The following actor
 * expects as input a reference to the SRB file system. This reference
 * connection is obtained via the SRBConnect Actor in Kepler. <i>See SRBConnect
 * & its documentation.</i>
 * </p>
 * <p>
 * The file reference system is created with a unique SRB user account and with
 * this connection reference as input the SPut actor is able to gain access to
 * the SRB file space. Once an alive SRB file connection system has been
 * established the actor gets the remode SRB directory specified and a remote
 * file path is created. The local file(s) is then copied to the SRB space with
 * the established file path. If the SRB file path is not specified, upload to
 * the current working directory. In case the above parallel put process fails ,
 * a streaming put process is carried out with SRBRandomAccess streams where the
 * file is uploaded as a sequence of byte arrays instead.
 * </p>
 * <p>
 * The user is also asked for confirmation on overwriting existing SRB remote
 * files if they exist or simply appending them.
 * </p>
 * <p>
 * <B>Actor Input:</B> Accepts a reference to the SRB files system, an SRB
 * remote location and an aray of local file paths.
 * </p>
 * <p>
 * <B>Actor Output:</B> Outputs the remote file paths and an exit status.The
 * exit status gives a message of "success" or appropriate error to indicate the
 * status of file put process.
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
 * @version $Id: SPut.java 24234 2010-05-06 05:21:26Z welker $
 * @category.name srb
 * @category.name put
 */

public class SPut extends TypedAtomicActor {

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
	public SPut(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		SRBFileSystem = new TypedIOPort(this, "SRBFileSystem", true, false);
		SRBFileSystem.setTypeEquals(BaseType.GENERAL);
		new Attribute(SRBFileSystem, "_showName");

		filesToPut = new TypedIOPort(this, "filesToPut", true, false);
		filesToPut.setTypeEquals(new ArrayType(BaseType.STRING));
		new Attribute(filesToPut, "_showName");

		uploadedFiles = new TypedIOPort(this, "uploadedFiles", false, true);
		uploadedFiles.setTypeEquals(new ArrayType(BaseType.STRING));
		new Attribute(uploadedFiles, "_showName");

		exitCode = new TypedIOPort(this, "exitCode", false, true);
		exitCode.setTypeEquals(BaseType.STRING);
		new Attribute(exitCode, "_showName");

		remoteDir = new PortParameter(this, "remoteDir");
		new Attribute(remoteDir, "_showName");

		confirmOverwrite = new Parameter(this, "confirmOverwrite");
		confirmOverwrite.setTypeEquals(BaseType.BOOLEAN);
		confirmOverwrite.setToken(BooleanToken.FALSE);

		_attachText("_iconDescription", "<svg>\n"
				+ "<rect x=\"-25\" y=\"-20\" " + "width=\"50\" height=\"40\" "
				+ "style=\"fill:white\"/>\n"
				+ "<polygon points=\"-15,-10 -12,-10 -8,-14 -1,-14 3,-10"
				+ " 15,-10 15,10, -15,10\" " + "style=\"fill:red\"/>\n"
				+ "<text x=\"-13.5\" y=\"7\" " + "style=\"font-size:14\">\n"
				+ "SRB</text>\n" + "<text x=\"-12\" y=\"19\""
				+ "style=\"font-size:11; fill:black; font-family:SansSerif\">"
				+ "SPut</text>\n" + "</svg>\n");
	}

	/**
	 * SRB file system reference.
	 */
	public TypedIOPort SRBFileSystem;

	/**
	 * Paths to the remote location.
	 */
	public TypedIOPort uploadedFiles;

	/**
	 * Paths to the files to upload
	 */
	public TypedIOPort filesToPut;

	/**
	 * Exit status of the operation
	 */
	public TypedIOPort exitCode;

	/**
	 * Where to upload the files.
	 */
	public PortParameter remoteDir;

	/**
	 * Overwrite existing srb file
	 */
	public Parameter confirmOverwrite;

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
		String srbFilePath;
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
			if (srbFileSystem == null) {
				throw new IllegalActionException(this,
						"No SRB connection available in actor "
								+ this.getName() + ".");
			}

			// The srb remote directory to fetch the files.
			remoteDir.update();
			String remoteDirStr = ((StringToken) remoteDir.getToken())
					.stringValue();
			SRBFile parent = new SRBFile(srbFileSystem, remoteDirStr);

			// Getting the local files list token and copying each file to SRB.
			ArrayToken localFilesTokenArr = null;
			try {
				localFilesTokenArr = (ArrayToken) filesToPut.get(0);
			} catch (Exception ex) {
				_debug("filesToGet port is null.");
			}
			if (localFilesTokenArr != null) {
				Token[] localFilesToken = localFilesTokenArr.arrayValue();
				Vector uploadedFilesVec = new Vector();
				for (int i = 0; i < localFilesToken.length; i++) {

					// local file path.
					String localFileStr = ((StringToken) localFilesToken[i])
							.stringValue();
					System.out.println(localFileStr);
					if (localFileStr.startsWith("file:/")) {
						localFileStr = localFileStr.substring(6);
						while (localFileStr.startsWith("/")) {
							localFileStr = localFileStr.substring(1);
						}
					}
					localFileStr = URLDecoder.decode(localFileStr);
					// File tmp = new File(localFileStr);
					// localFileStr = tmp.getAbsolutePath();
					_debug("<FILE_TO_PUT>" + localFileStr + "<FILE_TO_PUT>");
					int slashIndex = localFileStr.lastIndexOf('/');
					if (slashIndex == -1) {
						slashIndex = localFileStr.lastIndexOf("\\");
					}
					String tmpFileNameString = localFileStr;
					if (slashIndex > -1) {
						tmpFileNameString = localFileStr
								.substring(slashIndex + 1);
					} else {
						URIAttribute modelURI = null;
						NamedObj container = this;
						while (container != null && modelURI == null) {
							try {
								modelURI = (URIAttribute) container
										.getAttribute("_uri",
												URIAttribute.class);
							} catch (IllegalActionException ex) {
								// An attribute was found with name "_uri", but
								// it is not
								// an instance of URIAttribute. Continue the
								// search.
								modelURI = null;
							}
							container = (NamedObj) container.getContainer();
						}
						if (modelURI != null) {
							String modelPath = modelURI.getURI().toURL()
									.getPath();
							int ind = modelPath.lastIndexOf("/");
							localFileStr = modelPath.substring(0, ind + 1)
									+ localFileStr;
						}
					}
					_debug("<FILE_NAME_STR>" + tmpFileNameString
							+ "</FILE_NAME_STR>");

					localFile = new LocalFile(localFileStr);
					if (localFile.exists()) {
						// setting the remote file path.

						// copying the local file to SRB.
						srbFile = new SRBFile(parent, tmpFileNameString);
						boolean confirmOverwriteValue = ((BooleanToken) confirmOverwrite
								.getToken()).booleanValue();
						if (srbFile.exists() && confirmOverwriteValue) {
							// Query for overwrite.
							// FIXME: This should be called in the event thread!
							// There is a chance of deadlock since it is not.
							if (!MessageHandler
									.yesNoQuestion("OK to overwrite "
											+ localFile + "?")) {
								srbFile = null;
								srbFileSystem = SRBUtil
										.closeConnection(srbFileSystem);
								throw new IllegalActionException(this,
										"Please select another file name.");
							}
						}
						try {
							srbFile.copyFrom(localFile, true);
							uploadedFilesVec.add(new StringToken(srbFile
									.getAbsolutePath()));
						} catch (Exception ex) {
							// If the paralel put fails try to do a stream put.
							System.out.println("Parallel put failed due to "
									+ ex.getMessage());
							System.out.println("Trying stream put.");
							try {
								_streamPut(srbFile, localFile.getAbsolutePath());
								// adding the successfully uploaded files paths
								// to the uploaded files array.
								uploadedFilesVec.add(new StringToken(srbFile
										.getAbsolutePath()));
							} catch (Exception stex) {
								stex.printStackTrace();
								// even if there is an exception output the
								// successfully uploaded files.
								System.out.println("failed to upload file "
										+ localFile.getAbsolutePath() + " to "
										+ parent.getAbsolutePath() + ".\n");
								_exitCode += "unable to upload file "
										+ localFile.getAbsolutePath();
								_exitCode += " to " + parent.getAbsolutePath()
										+ ".\n";
							}
						}
					} else {
						System.out.println("file "
								+ localFile.getAbsolutePath()
								+ " does not exist.");
						_exitCode += "file " + localFile.getAbsolutePath()
								+ " does not exist.\n";
					}
				}
				Token[] uploadedFilesArr = new Token[uploadedFilesVec.size()];
				uploadedFilesVec.toArray(uploadedFilesArr);
				// broadcast the array only if it's non-empty.
				if (uploadedFilesArr.length > 0) {
					uploadedFiles.broadcast(new ArrayToken(uploadedFilesArr));
				} else {
					_exitCode = "no files were uploaded.";
				}
				if (_exitCode.equals("")) {
					_exitCode = "success";
				}
				exitCode.broadcast(new StringToken(_exitCode));
			}
		} catch (Exception ex) {
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
	 * Disconnect from SRB.
	 */
	public void wrapup() {
		srbFileSystem = SRBUtil.closeConnection(srbFileSystem);
	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	/**
	 * Stream put the file. Use in case the parallel put fails.
	 */
	private void _streamPut(SRBFile srbFile, String localFilePath)
			throws Exception {

		SRBRandomAccessFile srbRandomAccessFile = null;
		byte[] bytesRead = new byte[20000];
		;
		int nBytesRead;
		InputStream in = new FileInputStream(localFilePath);
		srbRandomAccessFile = new SRBRandomAccessFile(srbFile, "rw");

		nBytesRead = in.read(bytesRead);
		while (nBytesRead > 0) {
			srbRandomAccessFile.write(bytesRead);
			nBytesRead = in.read(bytesRead);
		}
		in.close();
	}

	private SRBFileSystem srbFileSystem = null;
}