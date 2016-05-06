/*
 * Copyright (c) 2010 The Regents of the University of California.
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

package org.ecoinformatics.seek.datasource.eml.eml2;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Vector;

import org.kepler.objectmanager.data.db.DSTableFieldIFace;

import ptolemy.actor.TypedIOPort;
import ptolemy.data.OrderedRecordToken;
import ptolemy.data.RecordToken;
import ptolemy.data.Token;
import ptolemy.data.type.RecordType;
import ptolemy.data.type.Type;
import ptolemy.kernel.util.IllegalActionException;

class Eml200DataOutputFormatRow extends Eml200DataOutputFormatBase {

	private static final String ROWDATAPORTNAME = "DataRow";
	private RecordType recordType;

	private static Collection portList;
	static {
		portList = new Vector(1);
		portList.add(ROWDATAPORTNAME);
		portList = Collections.unmodifiableCollection(portList);
	}

	Eml200DataOutputFormatRow(Eml200DataSource t) {
		super(t);
	}

	/*
	 * Intialize for output as field, row or Table. It will generate query from
	 * sqlDef attribute(if sqlDef is empty, the generate query as select from
	 * selectedEntity. Then run this query action and get resultset
	 */
	void initialize() throws IllegalActionException {
	}

	/**
	 * It reconfigures all the Ports to represent passing data back a row at a
	 * time.
	 * 
	 * @throws ptolemy.kernel.util.IllegalActionException
	 */
	void reconfigurePorts() throws IllegalActionException {
		// the record is an ArrayToken, we should transfer from base type to
		// array type
		if (that.getColumnTypes() == null) {
			Eml200DataSource.log
					.debug("The columns info is null and coudldn't conigure ports as row");
			return;
		}
		int size = that.getColumnTypes().length;
		Type[] typeList = new Type[size];
		for (int i = 0; i < size; i++) {
			typeList[i] = that.getColumnTypes()[i];
		}
		recordType = new RecordType(that.getColumnLabels(), typeList);

		that.removeOtherOutputPorts(portList);
		that.initializePort(ROWDATAPORTNAME, recordType);
	}

	/*
	 * Send a row as a single token over the port on each fire event.
	 * 
	 * @exception IllegalActionException If there is no director.
	 */
	void fire() throws IllegalActionException {

		TypedIOPort port = (TypedIOPort) that.getPort(ROWDATAPORTNAME);
		if (port != null) {
			Vector rowVector = null;
			try {
				rowVector = that.gotRowVectorFromSource();
				// System.out.println("the rowVector is "+rowVector);
			} catch (Exception e) {
				throw new IllegalActionException(e.getMessage());
			}
			int size = rowVector.size();
			if (size == 0) {
				return;
			}
			Token[] tokenArray = new Token[size];
			Vector attributes = that.getColumns();
			for (int i = 0; i < size; i++) {
				String eleStr = (String) rowVector.elementAt(i);
				DSTableFieldIFace colDef = (DSTableFieldIFace) attributes
						.elementAt(i);
				Vector missingValue = colDef.getMissingValueCode();
				Token singleToken = Eml200DataSource.transformStringToToken(
						eleStr, that.getColumnTypes()[i], missingValue, colDef
								.getName());
				tokenArray[i] = singleToken;

			}
			// System.out.println("the length of lables "+that.getColumnLabels().length);
			// System.out.println("lables "+that.getColumnLabels());
			// System.out.println("the length of tokenArray "+tokenArray.length);
			// System.out.println("the tokenArray "+tokenArray.toString());
			RecordToken rowRecordToken = new OrderedRecordToken(
					that.getColumnLabels(), tokenArray);
			port.send(0, rowRecordToken);

			// _recordCount++;
		}
	}

	/**
	 * If it is end of result return false
	 */
	public boolean prefire() throws IllegalActionException {
		try {
			if (that.isEndOfResultset()) {
				return false;
			} else {
				return true;
			}
		} catch (SQLException e) {
			throw new IllegalActionException(
					"Unable to determine end of result set");
		}

	}

	/**
	 * If it is end of result return false
	 */
	public boolean postfire() throws IllegalActionException {
		try {
			if (that.isEndOfResultset()) {
				return false;
			} else {
				return true;
			}
		} catch (SQLException e) {
			throw new IllegalActionException(
					"Unable to determine end of result set");
		}

	}

}
