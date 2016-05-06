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

import ptolemy.actor.NoTokenException;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.BooleanToken;
import ptolemy.data.ObjectToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.gui.GraphicalMessageHandler;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import edu.sdsc.grid.io.GeneralFile;
import edu.sdsc.grid.io.MetaDataCondition;
import edu.sdsc.grid.io.MetaDataRecordList;
import edu.sdsc.grid.io.MetaDataSelect;
import edu.sdsc.grid.io.MetaDataSet;
import edu.sdsc.grid.io.MetaDataTable;
import edu.sdsc.grid.io.local.LocalFile;
import edu.sdsc.grid.io.srb.SRBFile;
import edu.sdsc.grid.io.srb.SRBFileSystem;
import edu.sdsc.grid.io.srb.SRBMetaDataSet;

//////////////////////////////////////////////////////////////////////////
//// SRBGetMD
/**
 * <p>
 * SRBGetMD is a Kepler Actor which gets user defined metadeta for an SRB
 * dataset or collection.
 * 
 * The following actor expects as input a reference to the SRB file system. This
 * reference connection is obtained via the SRBConnect Actor in Kepler. <i>See
 * SRBConnect and its documentation.</i>
 * </p>
 * <p>
 * The file reference system is created with a unique SRB user account and with
 * this connection reference as input the SRBGetMD actor is able to gain access
 * to the SRB file space. Once an alive SRB file connection system has been
 * established the actor gets the remode SRB file/directory and access the
 * appropriate metadata via jargon API methods.If the recursive option is true,
 * then recursively get the metadata for all sub directories of the files.
 * </p>
 * <p>
 * <B>Actor Input:</B> Accepts a reference to the SRB files system, an SRB
 * remote file/directory path
 * </p>
 * <p>
 * <B>Actor Output:</B> Outputs the String representation of the SRB
 * dataset/collection's metadata.h
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
 * @version $Id: SRBGetMD.java 24234 2010-05-06 05:21:26Z welker $
 * @category.name srb
 * @category.name put
 */
public class SRBGetMD extends TypedAtomicActor {

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
	public SRBGetMD(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		SRBFileSystem = new TypedIOPort(this, "SRBFileSystem", true, false);
		SRBFileSystem.setTypeEquals(BaseType.GENERAL);
		new Attribute(SRBFileSystem, "_showName");

		srbFilePath = new TypedIOPort(this, "srbFilePath", true, false);
		srbFilePath.setTypeEquals(BaseType.STRING); // or should it be an array
													// of strings.
		new Attribute(srbFilePath, "_showName");

		metadata = new TypedIOPort(this, "metadata", false, true);
		metadata.setTypeEquals(BaseType.STRING);
		new Attribute(metadata, "_showName");

		/*
		 * exitCode = new TypedIOPort(this, "exitCode", false, true);
		 * exitCode.setTypeEquals(BaseType.STRING); new Attribute(exitCode,
		 * "_showName");
		 */
		recursive = new Parameter(this, "recursive", new BooleanToken(false));
		recursive.setTypeEquals(BaseType.BOOLEAN);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"150\" height=\"40\" " + "style=\"fill:white\"/>\n"
				+ "<text x=\"7\" y=\"30\""
				+ "style=\"font-size:12; fill:black; font-family:SansSerif\">"
				+ "SRB$</text>\n" + "<text x=\"41\" y=\"31\""
				+ "style=\"font-size:16; fill:blue; font-family:SansSerif\">"
				+ "Get MetaData</text>\n" + "</svg>\n");
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
	 * srb file's metadata
	 */
	public TypedIOPort metadata;
	// public TypedIOPort exitCode;

	/**
	 * -r ; Returns metadata recursively for directories.
	 */
	public Parameter recursive;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////
	/**
	 * Get the user defined metadata for SRB file paths. If the recursive flag
	 * is true, than recursively get the metadata for all sub directories of the
	 * files.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown if the SRB file cannot be accessed or the
	 *                current directory cannot be broadcasted.
	 */
	public void fire() throws IllegalActionException {

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

			boolean r = ((BooleanToken) recursive.getToken()).booleanValue();

			MetaDataRecordList[] record = null;
			if (srbFile.exists()) {
				if (r || !srbFile.isDirectory()) {
					MetaDataCondition conditions[] = { MetaDataSet
							.newCondition(SRBMetaDataSet.USER_GROUP_NAME,
									MetaDataCondition.LIKE, srbFileSystem
											.getUserName()), };

					String[] selectFieldNames;

					if (srbFile.isDirectory()) {
						selectFieldNames = new String[2];
						selectFieldNames[0] = SRBMetaDataSet.DEFINABLE_METADATA_FOR_FILES;
						selectFieldNames[1] = SRBMetaDataSet.DEFINABLE_METADATA_FOR_DIRECTORIES;
					} else { // if it's a file.
						selectFieldNames = new String[1];
						selectFieldNames[0] = SRBMetaDataSet.DEFINABLE_METADATA_FOR_FILES;
					}
					MetaDataSelect selects[] = MetaDataSet
							.newSelection(selectFieldNames);

					record = ((GeneralFile) srbFile).query(conditions, selects);
				} else { // get data for a directory non-recursively.
					MetaDataCondition[] conditions = { MetaDataSet
							.newCondition(SRBMetaDataSet.DIRECTORY_NAME,
									MetaDataCondition.EQUAL, srbFile
											.getAbsolutePath()) };
					String[] selectFieldNames = { SRBMetaDataSet.DEFINABLE_METADATA_FOR_DIRECTORIES };

					MetaDataSelect selects[] = MetaDataSet
							.newSelection(selectFieldNames);

					record = srbFileSystem.query(conditions, selects);
					if (record.length > 1) {
						// looking for the record that holds the metadata.
						for (int i = 0; i < record.length; i++) {
							int mdInd = record[i]
									.getFieldIndex(SRBMetaDataSet.DEFINABLE_METADATA_FOR_DIRECTORIES);
							if (mdInd > -1) {
								MetaDataRecordList res = record[i];
								record = new MetaDataRecordList[1];
								record[0] = res;
								break;
							}
						}
					}

				}
				if (record == null)
					System.out.println("no metadata");
				_sendResults(record, srbFile);

			} else
				GraphicalMessageHandler.error(srbFile.getAbsolutePath()
						+ "does not exist.");

			// FIXME: what should be returned here..
			if (_exitCode.equals("")) {
				_exitCode = "success";
			}
			// exitCode.broadcast(new StringToken(_exitCode));
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

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	/**
	 * Outputs the metadata
	 * 
	 * @param rl
	 *            metadata record list.
	 * @throws IllegalActionException
	 *             broadcast exception
	 */
	private void _sendResults(MetaDataRecordList[] rl, SRBFile srbFile)
			throws IllegalActionException {
		if (srbFile != null) {
			String results = "";
			// boolean r = ((BooleanToken)recursive.getToken()).booleanValue();
			if (rl != null) { // Nothing in the database matched the query
				for (int i = 0; i < rl.length; i++) {
					int collInd = rl[i]
							.getFieldIndex(SRBMetaDataSet.DIRECTORY_NAME);
					String dirName = rl[i].getStringValue(collInd);
					int fileNameInd = rl[i]
							.getFieldIndex(SRBMetaDataSet.FILE_NAME);
					if (fileNameInd != -1) { // this is a file.
						String fileName = rl[i].getStringValue(fileNameInd);
						/*
						 * if (!r) { //non-recursive. if (srbFile.isDirectory()
						 * || // if the srbFile is a directory
						 * (!srbFile.isDirectory() && //or it's a file but has a
						 * differnet name or different dir, continue
						 * (!fileName.equals(srbFile.getName()) ||
						 * !dirName.equals(srbFile.getParent())))) continue; }
						 */
						results += "file name: " + fileName + "\n";
						/*
						 * } else { // this is a directory if (!r) { if
						 * (!srbFile.isDirectory() || //if the srbFile is not a
						 * directory or it's a directory with a diff name, cont
						 * (srbFile.isDirectory() &&
						 * dirName.equals(srbFile.getAbsolutePath()))) continue;
						 * }
						 */
					}
					results += "directory name: " + dirName;
					int ind = rl[i]
							.getFieldIndex(SRBMetaDataSet.DEFINABLE_METADATA_FOR_FILES);
					if (ind != -1) { // this is metadata for a file.
						results += "\ndefinable metadata for files: \n";
					} else if (ind == -1) { // this is not metadata for a file.
						ind = rl[i]
								.getFieldIndex(SRBMetaDataSet.DEFINABLE_METADATA_FOR_DIRECTORIES);
						if (ind != -1) { // this is metadata for a directory
							results += "\ndefinable metadata for directories: \n";
						} else {
							results += "\nno metadata\n";
						}
					}
					if (ind != -1) { // there is metadata for a file/dir
						MetaDataTable mdt = rl[i].getTableValue(ind);
						String tmp = mdt.toString();
						for (int j = 0; j < mdt.getRowCount(); j++) {
							results += mdt.getStringValue(j, 0);
							results += " = ";
							results += mdt.getStringValue(j, 1) + "\n";
							System.out.println(mdt.getStringValue(j, 0));
							System.out.println(mdt.getStringValue(j, 1));
						}
					}
				}
			}
			metadata.broadcast(new StringToken(results));
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	/**
	 * An srb file variable.
	 */
	private SRBFile srbFile = null;

	/**
	 * An srb file system variable.
	 */
	private SRBFileSystem srbFileSystem = null;
}