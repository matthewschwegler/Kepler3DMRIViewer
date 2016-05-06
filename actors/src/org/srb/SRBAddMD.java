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

import java.util.StringTokenizer;

import ptolemy.actor.NoTokenException;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.ObjectToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.gui.GraphicalMessageHandler;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import edu.sdsc.grid.io.MetaDataCondition;
import edu.sdsc.grid.io.MetaDataRecordList;
import edu.sdsc.grid.io.MetaDataTable;
import edu.sdsc.grid.io.local.LocalFile;
import edu.sdsc.grid.io.srb.SRBFile;
import edu.sdsc.grid.io.srb.SRBFileSystem;
import edu.sdsc.grid.io.srb.SRBMetaDataRecordList;
import edu.sdsc.grid.io.srb.SRBMetaDataSet;

//////////////////////////////////////////////////////////////////////////
//// SRBAddMD
/**
 * <p>
 * SRBAddMD is a Kepler Actor which adds user defined metadeta to an SRB dataset
 * or collection. When ingesting metadata, only equal operator is used. The
 * following actor expects as input a reference to the SRB file system. This
 * reference connection is obtained via the SRBConnect Actor in Kepler. <i>See
 * SRBConnect and its documentation.</i>
 * </p>
 * <p>
 * The file reference system is created with a unique SRB user account and with
 * this connection reference as input the SRBAddMD actor is able to gain access
 * to the SRB file space. Once an alive SRB file connection system has been
 * established the actor gets the remode SRB file/directory and the attribute
 * value pair conditions defining the metadata. If the file/directory exists the
 * conditions are added via jargon API methods to the SRB file or directory.
 * </p>
 * <p>
 * <B>Actor Input:</B> Accepts a reference to the SRB files system, an SRB
 * remote file/directory path and a list of attribute value pairs.
 * </p>
 * <p>
 * Sample Attribute Value pairs : "a 5","b 10","c abc"
 * </p>
 * <p>
 * The above is a string array of 3 conditions(a,b,c) and their values to be
 * added to the SRB file/dir metadata.
 * </p>
 * <p>
 * <B>Actor Output:</B> Outputs an exit status.The exit status gives a message
 * of "success" or appropriate error to indicate the status of adding metadata
 * process.
 * 
 * </p>
 * <p>
 * The following actor accesses SRB file reference system and SRB file space
 * with the SRB Jargon API provided. The JARGON is a pure API for developing
 * programs with a data grid interface and I/O for SRB file systems.
 * </p>
 * 
 * <A href="http://www.sdsc.edu/srb"><I>Further information on SRB</I> </A>
 * 
 * @author Efrat Jaeger
 * @version $Id: SRBAddMD.java 24234 2010-05-06 05:21:26Z welker $
 * @category.name srb
 * @category.name put
 */
public class SRBAddMD extends TypedAtomicActor {

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
	public SRBAddMD(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		SRBFileSystem = new TypedIOPort(this, "SRBFileSystem", true, false);
		SRBFileSystem.setTypeEquals(BaseType.GENERAL);
		new Attribute(SRBFileSystem, "_showName");

		srbFilePath = new TypedIOPort(this, "srbFilePath", true, false);
		srbFilePath.setTypeEquals(BaseType.STRING); // or should it be an array
													// of strings.
		new Attribute(srbFilePath, "_showName");

		conditions = new TypedIOPort(this, "conditions", true, false);
		conditions.setTypeEquals(new ArrayType(BaseType.STRING));
		new Attribute(conditions, "_showName");

		trigger = new TypedIOPort(this, "trigger", false, true);
		trigger.setTypeEquals(BaseType.STRING);
		new Attribute(trigger, "_showName");

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"150\" height=\"40\" " + "style=\"fill:white\"/>\n"
				+ "<text x=\"7\" y=\"30\""
				+ "style=\"font-size:12; fill:black; font-family:SansSerif\">"
				+ "SRB$</text>\n" + "<text x=\"41\" y=\"31\""
				+ "style=\"font-size:16; fill:blue; font-family:SansSerif\">"
				+ "Add MetaData</text>\n" + "</svg>\n");
	}

	/**
	 * pointer to the SRB file system.
	 */
	public TypedIOPort SRBFileSystem;

	/**
	 * Path to SRB file.
	 */
	public TypedIOPort srbFilePath;

	/**
	 * {"att val"}
	 */
	public TypedIOPort conditions;

	/**
	 * An input trigger.
	 */
	public TypedIOPort trigger;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////
	/**
	 * Add the metadata conditions to the specified file/directory. The
	 * conditions are in the form of "Att Val".
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
			if (srbFileSystem == null) {
				throw new IllegalActionException(this,
						"No SRB connection available in actor "
								+ this.getName() + ".");
			}

			String srbFileStr = ((StringToken) srbFilePath.get(0))
					.stringValue();
			srbFile = new SRBFile(srbFileSystem, srbFileStr);

			if (srbFile.exists()) {
				Token[] conds = ((ArrayToken) conditions.get(0)).arrayValue();
				int numConds = conds.length;
				String[][] definableMetaDataValues = new String[numConds][2];
				int operators[] = new int[numConds];

				for (int i = 0; i < numConds; i++) {
					// ignoring [-dcur] for the mean time.
					String condition = ((StringToken) conds[i]).stringValue();
					StringTokenizer st = new StringTokenizer(condition);
					int j = 0;
					while (st.hasMoreTokens()) {
						definableMetaDataValues[i][j++] = st.nextToken();
					}
					if (j < 2) // should be an attribute value pair.
						GraphicalMessageHandler.error("incomplete condition '"
								+ condition + "'.");
					operators[i] = MetaDataCondition.EQUAL;
				}
				MetaDataTable metaDataTable = new MetaDataTable(operators,
						definableMetaDataValues);

				MetaDataRecordList[] record = new MetaDataRecordList[1];

				if (srbFile.isDirectory()) {
					record[0] = new SRBMetaDataRecordList(
							SRBMetaDataSet
									.getField(SRBMetaDataSet.DEFINABLE_METADATA_FOR_DIRECTORIES),
							metaDataTable);

				} else {
					record[0] = new SRBMetaDataRecordList(
							SRBMetaDataSet
									.getField(SRBMetaDataSet.DEFINABLE_METADATA_FOR_FILES),
							metaDataTable);

				}

				srbFile.modifyMetaData(record[0]);

			} else
				GraphicalMessageHandler.error(srbFile.getAbsolutePath()
						+ " does not exist.");

			// FIXME: what should be returned here..
			if (_exitCode.equals("")) {
				_exitCode = "success";
			}
			trigger.broadcast(new StringToken(_exitCode));
		} catch (Exception ex) {
			srbFileSystem = SRBUtil.closeConnection(srbFileSystem);
			ex.printStackTrace();
			throw new IllegalActionException(this, ex.getMessage()
					+ ". in actor " + this.getName());
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