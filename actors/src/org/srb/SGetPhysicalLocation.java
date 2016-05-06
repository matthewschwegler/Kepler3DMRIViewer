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

import java.util.Vector;

import ptolemy.actor.NoTokenException;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.ObjectToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import edu.sdsc.grid.io.MetaDataRecordList;
import edu.sdsc.grid.io.local.LocalFile;
import edu.sdsc.grid.io.srb.SRBFile;
import edu.sdsc.grid.io.srb.SRBFileSystem;
import edu.sdsc.grid.io.srb.SRBMetaDataSet;

//////////////////////////////////////////////////////////////////////////
//// SGetPhysicalLocation
/**
 * <p>
 * Get the physical location of SRB files. Returns the physical location of a
 * logical SRB path. This remote location cannot be accessed through windows.
 * The following actor expects as input a reference to the SRB file system. This
 * reference connection is obtained via the SRBConnect Actor in Kepler. <i>See
 * SRBConnect and its documentation.</i>
 * </p>
 * <p>
 * The file reference system is created with a unique SRB user account and with
 * this connection reference as input the SRBPhysicalLocation actor is able to
 * gain access to the SRB file space. Once an alive SRB file connection system
 * has been established the actor gets the remode SRB file path and checks for
 * it's existence. The SRB file is queried for its PATH_NAME (Physical Location)
 * from its Metadata.
 * </p>
 * <p>
 * <B>Actor Input:</B> Accepts a reference to the SRB files system, an SRB
 * remote file name(s) as input. SRB remote file name is the logical file path.
 * </p>
 * <p>
 * <B>SRB Logical Name Space</B> It is easy to think of SRB Collections as Unix
 * directories (or Windows folders), but there is a fundamental difference. Each
 * individual data object (file) in a collection can be stored on a different
 * physical device. Unix directories and Windows folders use space from the
 * physical device on which they reside, but SRB collections are part of a
 * "logical name space" that exists in the MCAT and maps individual data objects
 * (files) to physical files. The logical name space is the set of names of
 * collections (directories) and data objects (files) maintained by the SRB.
 * Users see and interact with the logical name space, and the physical location
 * is handled by the SRB system and administrators. The SRB system adds this
 * logical name space on top of the physcial name space, and derives much of its
 * power and functionality from that.
 * </p>
 * <p>
 * <B>Actor Output:</B> The actor returns the physical location of SRB path.Also
 * returned is an exit code status indicating either success or failure/errors
 * in getting file's physical location.
 * </p>
 * <p>
 * <P>
 * The following actor accesses SRB file reference system and SRB file space
 * with the SRB Jargon API provided. The JARGON is a pure API for developing
 * programs with a data grid interface and I/O for SRB file systems.
 * </p>
 * <A href="http://www.sdsc.edu/srb"><I>Further information on SRB</I> </A>h
 * 
 * @author Efrat Jaeger
 * @version $Id: SGetPhysicalLocation.java 24234 2010-05-06 05:21:26Z welker $
 * @category.name srb
 * @category.name put
 */

public class SGetPhysicalLocation extends TypedAtomicActor {

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
	public SGetPhysicalLocation(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		SRBFileSystem = new TypedIOPort(this, "SRBFileSystem", true, false);
		SRBFileSystem.setTypeEquals(BaseType.GENERAL);
		new Attribute(SRBFileSystem, "_showName");

		logicalPath = new TypedIOPort(this, "logicalPath", true, false);
		logicalPath.setTypeEquals(new ArrayType(BaseType.STRING));
		new Attribute(logicalPath, "_showName");

		physicalPath = new TypedIOPort(this, "physicalPath", false, true);
		physicalPath.setTypeEquals(new ArrayType(BaseType.STRING));
		new Attribute(physicalPath, "_showName");

		/*
		 * exitCode = new TypedIOPort(this, "exitCode", false, true);
		 * exitCode.setTypeEquals(BaseType.STRING); new Attribute(exitCode,
		 * "_showName");
		 */
		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"158\" height=\"30\" " + "style=\"fill:white\"/>\n"
				+ "<text x=\"7\" y=\"24\" "
				+ "style=\"font-size:12; fill:black; font-family:SansSerif\">"
				+ "SRB$</text>\n" + "<text x=\"41\" y=\"25\" "
				+ "style=\"font-size:14; fill:blue; font-family:SansSerif\">"
				+ "Physical Location</text>\n" + "</svg>\n");
	}

	/**
	 * Connection reference
	 */
	public TypedIOPort SRBFileSystem;

	/**
	 * Logical path to SRB file
	 */
	public TypedIOPort logicalPath;

	/**
	 * Paths to the local location.
	 */
	public TypedIOPort physicalPath;

	// public TypedIOPort exitCode;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////
	/**
	 * Get the physical location of SRB logical file paths.
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

				// Getting the list of file to query for logical path.
				ArrayToken logFilesTokenArr = null;
				try {
					logFilesTokenArr = (ArrayToken) logicalPath.get(0);
				} catch (Exception ex) {
					_debug("logFilesTokenArr port is null.");
				}
				if (logFilesTokenArr != null) {
					Token[] logFilesToken = logFilesTokenArr.arrayValue();
					Vector physicalLocVec = new Vector();
					for (int i = 0; i < logFilesToken.length; i++) {

						// srb file path.
						String logFileStr = ((StringToken) logFilesToken[i])
								.stringValue();
						_debug("<FILE_TO_PUT>" + logFileStr + "<FILE_TO_PUT>");

						srbFile = new SRBFile(srbFileSystem, logFileStr);

						if (srbFile.exists()) {
							MetaDataRecordList[] record = srbFile
									.query(SRBMetaDataSet.PATH_NAME);

							// extract the physical location
							String physicalLocation = record[0]
									.getStringValue(0);
							physicalLocVec
									.add(new StringToken(physicalLocation));
						} else
							_exitCode += srbFile.getAbsolutePath()
									+ "does not exist.\n";
					}
					if (physicalLocVec.size() > 0) {
						Token[] physicalPathArr = new Token[physicalLocVec
								.size()];
						physicalLocVec.toArray(physicalPathArr);
						physicalPath.broadcast(new ArrayToken(physicalPathArr));
					}
					if (_exitCode.equals("")) {
						_exitCode = "success";
					}
					// exitCode.broadcast(new StringToken(_exitCode));
				}
			} else
				throw new IllegalActionException(this,
						"No SRB connection available in actor "
								+ this.getName() + ".");
		} catch (Exception ex) {
			srbFile = null;
			srbFileSystem = SRBUtil.closeConnection(srbFileSystem);
			throw new IllegalActionException(this, ex.getMessage()
					+ ". in actor " + this.getName() + ".");
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

	private SRBFileSystem srbFileSystem = null;
}