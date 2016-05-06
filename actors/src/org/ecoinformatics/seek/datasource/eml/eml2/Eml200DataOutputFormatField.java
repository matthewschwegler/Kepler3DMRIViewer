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
import java.util.Enumeration;
import java.util.Vector;

import org.kepler.objectmanager.data.db.DSTableFieldIFace;

import ptolemy.actor.TypedIOPort;
import ptolemy.data.Token;
import ptolemy.data.type.Type;
import ptolemy.kernel.util.IllegalActionException;

class Eml200DataOutputFormatField extends Eml200DataOutputFormatBase {

	Eml200DataOutputFormatField(Eml200DataSource t) {
		super(t);
	}

	/*
	 * Intialize for output as field, row or Table. It will generate query from
	 * sqlDef attribute(if sqlDef is empty, the generate query as select from
	 * selectedEntity. Then run this query action and get resultset
	 */
	void initialize() throws IllegalActionException {
	}

	/*
	 * This method will configure ports for every selected fields(attributes) If
	 * the output is for a value as a token, isArrayToken = false. If the ouput
	 * is a vector a column data, isArrayToken = true;
	 */
	void reconfigurePorts() throws IllegalActionException {
		if (that.getColumns() == null || that.getColumns().isEmpty()) {
			Eml200DataSource.log
					.debug("The columns info is null and coudldn't conigure ports as field");
			return;
		}
		// Get List of new column names
		Collection portList = new Vector();
		for (Enumeration e = that.getColumns().elements(); e.hasMoreElements();) {
			portList.add(((DSTableFieldIFace) e.nextElement()).getName());
		}

		that.removeOtherOutputPorts(portList);

		// Now add all the new columns
		for (Enumeration e = that.getColumns().elements(); e.hasMoreElements();) {
			DSTableFieldIFace column = (DSTableFieldIFace) e.nextElement();
			String name = column.getName();
			Type type = that.getBaseType(column.getDataType());
			that.initializePort(name, type);
		}
	}

	/**
	 * Send a record's tokens over the ports on each fire event.
	 * 
	 * @exception IllegalActionException
	 *                If there is no director.
	 */
	void fire() throws IllegalActionException {
		// log.debug("Processing record: " + i);
		Enumeration colEnum = that.getColumns().elements();
		// Enumeration dataEnum = _dataVectors[_recordCount].elements();
		Vector rowData = null;
		try {
			rowData = that.gotRowVectorFromSource();
		} catch (Exception e) {
			throw new IllegalActionException(e.getMessage());
		}
		// Enumeration typeEnum = _colTypes.elements();
		int vectorIndex = 0;
		while (colEnum.hasMoreElements() && vectorIndex < rowData.size()) {

			DSTableFieldIFace colDef = (DSTableFieldIFace) colEnum
					.nextElement();
			// String eleStr = (String) dataEnum.nextElement();
			String eleStr = (String) rowData.elementAt(vectorIndex);
			Vector missingValue = colDef.getMissingValueCode();
			vectorIndex++;
			Type type = (Type) that.getBaseType(colDef.getDataType());
			TypedIOPort port = (TypedIOPort) that.getPort(colDef.getName()
					.trim());
			if (port != null) {
				Token val = Eml200DataSource.transformStringToToken(eleStr,
						type, missingValue, colDef.getName());
				// send the data on the port
				port.send(0, val);
			}
		}
		// _recordCount++;
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

}
