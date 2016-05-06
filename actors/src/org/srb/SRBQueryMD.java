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
//// SRBQueryMD
/**
 * <p>
 * Query the metadata. Get all files satisfying the conditions. Queries the SRB
 * metadata from a specific location with user defined conditions.
 * </p>
 * <p>
 * The conditions are generated as follows:
 * </p>
 * <p>
 * <b>SRBCreateQueryInterface</b>: Creates an html interface for querying the
 * SRB metadata. Will be replaced with a jsp page within a Kepler server.
 * </p>
 * <p>
 * 
 * <b>SRBCreateQueryConditions</b>: Creates conditions for querying the SRB
 * metadata from a user xml string conditions, returned by the BrowserUI actor.
 * </p>
 * <p>
 * The following actor expects as input a reference to the SRB file system. This
 * reference connection is obtained via the SRBConnect Actor in Kepler. <i>See
 * SRBConnect and its documentation.</i>
 * </p>
 * <p>
 * The file reference system is created with a unique SRB user account and with
 * this connection reference as input the SRBQueryMD actor is able to gain
 * access to the SRB file space. Once an alive SRB file connection system has
 * been established the actor gets the remote SRB file/directory and the
 * appropriate metadata conditions. The SRB files are then queried with the
 * metadata constraints via jargon API methods.
 * </p>
 * <p>
 * <B>Actor Input:</B> Accepts a reference to the SRB files system, an SRB
 * remote file/directory path and conditions.
 * </p>
 * <p>
 * There exist separate conditions format for metadatas for datasets,
 * collections and pre-defined metadata.
 * </p>
 * <p>
 * The conditions for files and directories are specified as follows: { metadata
 * for datasets, "|", metadata for collections, "|", predefined metadata}
 * </p>
 * <p>
 * E.g. {"a = 5", "b = 3", "|", "x = 8", "|", "owner = efrat"}
 * </p>
 * <p>
 * Straight slashes ("|") separate between the different metadata types. Both
 * slashes are MANDATORY, even if no condition is specified!
 * </p>
 * <p>
 * The following operators apply: =, !=, not=, <, lt, num_lt, <=, le, num_le, >,
 * gt, num_gt, >=, ge, num_ge, in, notin, not_in, between, num_between,
 * notbetween, not_between, num_not_between, like, notlike, not_like, in,
 * contains, notin, not_in, , not_contains sounds_like, sounds_not_like,
 * sounds_not_like
 * 
 * </p>
 * <p>
 * <B>Actor Output:</B> Returns an array of all the file paths satisfying the
 * constraints.
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
 * @version $Id: SRBQueryMD.java 24234 2010-05-06 05:21:26Z welker $
 * @category.name srb
 * @category.name put
 */
public class SRBQueryMD extends TypedAtomicActor {

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
	public SRBQueryMD(CompositeEntity container, String name)
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

		filePaths = new TypedIOPort(this, "filePaths", false, true);
		filePaths.setTypeEquals(new ArrayType(BaseType.STRING));
		new Attribute(filePaths, "_showName");

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"150\" height=\"40\" " + "style=\"fill:white\"/>\n"
				+ "<text x=\"7\" y=\"30\""
				+ "style=\"font-size:12; fill:black; font-family:SansSerif\">"
				+ "SRB$</text>\n" + "<text x=\"41\" y=\"31\""
				+ "style=\"font-size:14; fill:blue; font-family:SansSerif\">"
				+ "Query MetaData</text>\n" + "</svg>\n");
	}

	/**
	 * pointer to the SRB file system.
	 */
	public TypedIOPort SRBFileSystem;

	/**
	 * Collection path to begin querying from
	 */
	public TypedIOPort srbFilePath;

	/**
	 * {"dataset_att op val", "|","collection_att op val", "|",
	 * "predefined_att op val"}
	 * <P>
	 * The conditions for files and directories are specified as follows:
	 * Straight slashes ("|") separate between the different metadata types.
	 * Both slashes are MANDATORY, even if no condition was specified!
	 * 
	 * { metadata for datasets, "|", metadata for collections, "|", predefined
	 * metadata} E.g. {"a = 5", "b > 3", "|", "x < 8", "|", "owner = efrat"}
	 * <P>
	 * The following operators apply: =, !=, not=, <, lt, num_lt, <=, le,
	 * num_le, >, gt, num_gt, >=, ge, num_ge, in, notin, not_in, between,
	 * num_between, notbetween, not_between, num_not_between, like, notlike,
	 * not_like, in, contains, notin, not_in, , not_contains sounds_like,
	 * sounds_not_like, sounds_not_like
	 */
	public TypedIOPort conditions;

	/**
	 * Matching files paths.
	 */
	public TypedIOPort filePaths;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////
	/**
	 * Returns an array of all the file paths satisfying the constraints.
	 * 
	 * The conditions for files and directories are specified as follows: {
	 * metadata for datasets, "|", metadata for collections, "|", predefined
	 * metadata} E.g. {"a = 5", "b = 3", "|", "x = 8", "|", "owner = efrat"}
	 * 
	 * Straight slashes ("|") separate between the different metadata types.
	 * Both slashes are MANDATORY, even if no condition is specified!
	 * 
	 * The following operators apply: =, !=, not=, <, lt, num_lt, <=, le,
	 * num_le, >, gt, num_gt, >=, ge, num_ge, in, notin, not_in, between,
	 * num_between, notbetween, not_between, num_not_between, like, notlike,
	 * not_like, in, contains, notin, not_in, , not_contains sounds_like,
	 * sounds_not_like, sounds_not_like
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
				// String[][] definableMetaDataValues = new String[numConds][2];
				// int operators[] = new int[numConds];

				Vector conditionsVec = new Vector();
				Vector conditionTableVec = new Vector();

				int j = 0;

				// ///////////////////////
				// METADATA FOR DATASETS
				// ///////////////////////
				String condition = ((StringToken) conds[j++]).stringValue();
				while (!condition.equals("|")) { // getting datasets metadata.
					conditionTableVec.add(condition);
					condition = ((StringToken) conds[j++]).stringValue();
				}

				MetaDataTable mdt = null;
				// creating the metadata table for file conditions.
				if (conditionTableVec.size() > 0) {
					// creating an srb metadata table for the datasets
					// constraints.
					mdt = _createMDT(conditionTableVec);
					// creating an SRB metadata condition for datasets
					conditionsVec.add(MetaDataSet.newCondition(
							SRBMetaDataSet.DEFINABLE_METADATA_FOR_FILES, mdt));
				} else {
					_debug("no user defined metadata for files");
					System.out.println("no user defined metadata for files");
				}

				conditionTableVec.clear();

				// //////////////////////////
				// METADATA FOR COLLECTIONS
				// //////////////////////////
				condition = ((StringToken) conds[j++]).stringValue();
				while (!condition.equals("|")) {
					conditionTableVec.add(condition);
					condition = ((StringToken) conds[j++]).stringValue();
				}

				// creating the metadata table for collection conditions.
				if (conditionTableVec.size() > 0) {
					// creating an srb metadata table for the collections
					// constraints.
					mdt = _createMDT(conditionTableVec);
					// creating an SRB metadata condition for collections
					conditionsVec.add(MetaDataSet.newCondition(
							SRBMetaDataSet.DEFINABLE_METADATA_FOR_DIRECTORIES,
							mdt));
				} else {
					_debug("no user defined metadata for collections");
					System.out
							.println("no user defined metadata for collections");
				}

				// /////////////////////
				// PREDEFINED METADATA (the above is user defined metadata)
				// /////////////////////
				for (int i = j; i < numConds; i++) {
					condition = ((StringToken) conds[i]).stringValue();
					StringTokenizer st = new StringTokenizer(condition);

					String att = _getAttribute(st.nextToken());
					if (att.equals(""))
						GraphicalMessageHandler
								.error("query attribute is null");
					String opStr = st.nextToken();
					int op = _getOperator(opStr);
					if (op <= MetaDataCondition.GREATER_OR_EQUAL) {
						// this is an equality term.
						String val = st.nextToken();
						conditionsVec.add(MetaDataSet
								.newCondition(att, op, val));
					} else if (op >= MetaDataCondition.IN
							&& op <= MetaDataCondition.NOT_IN) {
						Vector valuesVec = new Vector();
						while (st.hasMoreTokens()) {
							String token = st.nextToken();
							int commaInd = token.indexOf(",");
							if (commaInd == -1) {
								valuesVec.add(token);
							} else {
								while (commaInd != -1) {
									valuesVec.add(token.substring(0, commaInd));
									token = token.substring(commaInd);
									commaInd = token.indexOf(",");
								}
								valuesVec.add(token);
							}

						}
						String[] values = new String[valuesVec.size()];
						valuesVec.toArray(values);
						conditionsVec.add(MetaDataSet.newCondition(att, op,
								values));
						/*
						 * if (opStr.equals("in") || opStr.equals("not_in")) {
						 * // create an array of val } else { // this is
						 * contains. // create an array of val?? // create an
						 * array of att. }
						 */
					} else if (op == MetaDataCondition.BETWEEN
							|| op == MetaDataCondition.NOT_BETWEEN) {
						String val1 = st.nextToken();
						String val2 = st.nextToken();
						conditionsVec.add(MetaDataSet.newCondition(att, op,
								val1, val2));
					} else if ((op >= MetaDataCondition.LIKE && op <= MetaDataCondition.NOT_LIKE)
							|| (op >= MetaDataCondition.SOUNDS_LIKE && op <= MetaDataCondition.SOUNDS_NOT_LIKE)) {
						String val = st.nextToken();
						conditionsVec.add(MetaDataSet
								.newCondition(att, op, val));
					}
				}
				// query for the conditions and the select fields.

				if (conditionsVec.size() == 0) {
					_debug("no conditions were specified");
					System.out.println("no conditions were specified");
				}
				MetaDataCondition mdconditions[] = new MetaDataCondition[conditionsVec
						.size()];
				conditionsVec.toArray(mdconditions);

				// The field selected in the query result.
				String[] selectFieldNames = {
						SRBMetaDataSet.DEFINABLE_METADATA_FOR_DIRECTORIES,
						SRBMetaDataSet.DEFINABLE_METADATA_FOR_FILES };
				MetaDataSelect selects[] = MetaDataSet
						.newSelection(selectFieldNames);

				// Querying for files that satisfy the metadata constraints.
				MetaDataRecordList[] rl = ((GeneralFile) srbFile).query(
						mdconditions, selects);
				_sendQueryResults(rl);

			} else
				GraphicalMessageHandler.error(srbFile.getAbsolutePath()
						+ "does not exist.");

		} catch (Exception ex) {
			srbFileSystem = SRBUtil.closeConnection(srbFileSystem);
			ex.printStackTrace();
			throw new IllegalActionException(this, ex.getMessage()
					+ ". in actor " + this.getName());
		}
	} // end of fire.

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
	 * Create a metadata table of the user defined conditions.
	 */
	private MetaDataTable _createMDT(Vector conds) {
		Vector ufmdVec = new Vector();
		Vector opVec = new Vector();
		String[] singleMD;
		for (int i = 0; i < conds.size(); i++) {
			StringTokenizer st = new StringTokenizer((String) conds.get(i));
			String att = st.nextToken();
			if (att.equals(""))
				GraphicalMessageHandler.error("query attribute is null");
			singleMD = new String[2];
			singleMD[0] = att;
			String opStr = st.nextToken();
			int op = _getOperator(opStr);
			if (op <= MetaDataCondition.GREATER_OR_EQUAL) {
				// this is an equality term.
				opVec.add(new Integer(op));
				String val = st.nextToken();
				singleMD[1] = val;
				ufmdVec.add(singleMD);
			} else if (op >= MetaDataCondition.IN
					&& op <= MetaDataCondition.NOT_IN) {
				opVec.add(new Integer(op));
				String val = st.nextToken();
				while (st.hasMoreTokens()) {
					val += "," + st.nextToken();
				}
				singleMD[1] = val;
				ufmdVec.add(singleMD);
				/*
				 * if (opStr.equals("in") || opStr.equals("not_in")) { // create
				 * an array of val } else { // this is contains. // create an
				 * array of val?? // create an array of att. }
				 */
			} else if (op == MetaDataCondition.BETWEEN) {
				opVec.add(new Integer(MetaDataCondition.GREATER_OR_EQUAL));
				String val = st.nextToken();
				singleMD[1] = val;
				ufmdVec.add(singleMD);

				singleMD = new String[2];
				singleMD[0] = att;
				opVec.add(new Integer(MetaDataCondition.LESS_OR_EQUAL));
				val = st.nextToken();
				singleMD[1] = val;
				ufmdVec.add(singleMD);
			} else if (op == MetaDataCondition.NOT_BETWEEN) {
				opVec.add(new Integer(MetaDataCondition.LESS_THAN));
				String val = st.nextToken();
				singleMD[1] = val;
				ufmdVec.add(singleMD);

				opVec.add(new Integer(MetaDataCondition.GREATER_THAN));
				val = st.nextToken();
				singleMD[1] = val;
				ufmdVec.add(singleMD);
			} else if ((op >= MetaDataCondition.LIKE && op <= MetaDataCondition.NOT_LIKE)
					|| (op >= MetaDataCondition.SOUNDS_LIKE && op <= MetaDataCondition.SOUNDS_NOT_LIKE)) {
				opVec.add(new Integer(op));
				String val = st.nextToken();
				singleMD[1] = val;
				ufmdVec.add(singleMD);
			}
		}
		String[][] definableMetaData = new String[ufmdVec.size()][2];
		// for (int i=0; i<ufmdVec.size(); i++) {
		// definableMetaData[i] = (String[])ufmdVec.get(i);
		// }
		ufmdVec.toArray(definableMetaData);
		int[] operators = new int[opVec.size()];
		for (int i = 0; i < opVec.size(); i++) {
			operators[i] = ((Integer) opVec.get(i)).intValue();
		}

		return new MetaDataTable(operators, definableMetaData);
	}

	/**
	 * Get the desired attribute name for predefined attributes.
	 * 
	 * @param att
	 * 	 */
	private String _getAttribute(String att) {
		att = att.trim().toLowerCase();
		if (att.equals("annotation"))
			return SRBMetaDataSet.FILE_ANNOTATION;
		if (att.equals("annotator"))
			return SRBMetaDataSet.FILE_ANNOTATION_USERNAME;
		if (att.equals("owner"))
			return SRBMetaDataSet.OWNER;
		if (att.equals("dataName"))
			return SRBMetaDataSet.FILE_NAME;
		if (att.equals("collName"))
			return SRBMetaDataSet.DIRECTORY_NAME;
		if (att.equals("dataType"))
			return SRBMetaDataSet.FILE_TYPE_NAME;
		return "";
	}

	/**
	 * Get the operator int representation for an operator string
	 * 
	 * @param op
	 * 	 */
	private int _getOperator(String op) {
		// currently only equality operators are supported.
		// if no operator is found assume it is equal.
		op = op.trim().toLowerCase();

		if (op.equals("="))
			return MetaDataCondition.EQUAL;
		if (op.equals("!=") || op.equals("not="))
			return MetaDataCondition.NOT_EQUAL;
		if (op.equals("<") || op.equals("lt") || op.equals("num_lt"))
			return MetaDataCondition.LESS_THAN;
		if (op.equals("<=") || op.equals("le") || op.equals("num_le"))
			return MetaDataCondition.LESS_OR_EQUAL;
		if (op.equals(">") || op.equals("gt") || op.equals("num_gt"))
			return MetaDataCondition.GREATER_THAN;
		if (op.equals(">=") || op.equals("ge") || op.equals("num_ge"))
			return MetaDataCondition.GREATER_OR_EQUAL;
		if (op.equals("in"))
			return MetaDataCondition.IN;
		if (op.equals("notin") || op.equals("not_in"))
			return MetaDataCondition.NOT_IN;
		if (op.equals("between") || op.equals("num_between"))
			return MetaDataCondition.BETWEEN;
		if (op.equals("notbetween") || op.equals("not_between")
				|| op.equals("num_not_between"))
			return MetaDataCondition.NOT_BETWEEN;
		if (op.equals("like"))
			return MetaDataCondition.LIKE;
		if (op.equals("notlike") || op.equals("not_like"))
			return MetaDataCondition.NOT_LIKE;
		if (op.equals("in") || op.equals("contains"))
			return MetaDataCondition.IN;
		if (op.equals("notin") || op.equals("not_in")
				|| op.equals("not_contains"))
			return MetaDataCondition.NOT_IN;
		if (op.equals("sounds_like"))
			return MetaDataCondition.SOUNDS_LIKE;
		if (op.equals("sounds_not_like") || op.equals("sounds_not_like"))
			return MetaDataCondition.SOUNDS_NOT_LIKE;
		System.out.println("operator " + op
				+ " is unsupported. Treated as equal.");
		return MetaDataCondition.EQUAL;
	}

	/**
	 * broadcast the query results.
	 * 
	 * @param rl
	 * @throws IllegalActionException
	 */
	private void _sendQueryResults(MetaDataRecordList[] rl)
			throws IllegalActionException {
		if (rl != null && rl.length > 0) { // Nothing in the database matched
											// the query
			Token[] results = new Token[rl.length];
			for (int i = 0; i < rl.length; i++) {
				int collInd = rl[i]
						.getFieldIndex(SRBMetaDataSet.DIRECTORY_NAME);
				String fileName = rl[i].getStringValue(collInd);
				int dataInd = rl[i].getFieldIndex(SRBMetaDataSet.FILE_NAME);
				if (dataInd != -1) { // this is a file.
					fileName = fileName + "/" + rl[i].getStringValue(dataInd);
				}
				System.out.println(fileName);
				results[i] = new StringToken(fileName);
			}
			filePaths.broadcast(new ArrayToken(results));
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	/**
	 * An srb file system variable.
	 */
	private SRBFileSystem srbFileSystem = null;

}