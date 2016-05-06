/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2012-11-09 19:45:47 -0800 (Fri, 09 Nov 2012) $' 
 * '$Revision: 31064 $'
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

import java.util.Collection;
import java.util.Enumeration;
import java.util.Vector;

import org.kepler.objectmanager.data.db.DSTableFieldIFace;

import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.Token;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.Type;
import ptolemy.kernel.util.IllegalActionException;

class Eml200DataOutputFormatColumnVector extends Eml200DataOutputFormatBase {

	protected Vector[] _columnVectorArray = null;
	protected boolean isFired = false;

	Eml200DataOutputFormatColumnVector(Eml200DataSource t) {
		super(t);
	}

	/*
	 * Initialize as column vector for fire. This method will transfer a
	 * resultset into column array or transfer a delimiter reader to column
	 * array
	 */
	void initialize() throws IllegalActionException {

		Eml200DataSource.log.debug("********** INITIALIZING************ "
				+ this.getClass().getName());
		isFired = false;
		// moved the meat of this method into the private method
		// that is called upon firing

		// rest the data, selected entity and/or selection criteria may have
		// changed
		_columnVectorArray = null;

	}

	/**
	 * Because this format consumes the resultset when creating the vector it
	 * should be called upon firing rather than initializing. This allows the
	 * initialize() method in superclass to correctly set up the actor but not
	 * consume the resultset prematurely. See bug #3125 for some symptoms of not
	 * protecting the resultset
	 * 
	 * @throws IllegalActionException
	 */
	protected void populateVector() throws IllegalActionException {
		Vector rowVector = new Vector();
		try {
			rowVector = that.gotRowVectorFromSource();
		} catch (Exception e) {
			throw new IllegalActionException(e.getMessage());
		}
		int size = rowVector.size();
		// initial the columnVector array
		_columnVectorArray = new Vector[size];
		for (int i = 0; i < size; i++) {
			Vector column = new Vector();
			_columnVectorArray[i] = column;

		}

		while (!rowVector.isEmpty()) {
			for (int i = 0; i < size; i++) {
				Vector column = _columnVectorArray[i];
				String data = (String) rowVector.elementAt(i);
				Eml200DataSource.log.trace("Add data " + data
						+ " into column vector " + i);
				column.add(data);
			}
			try {
				rowVector = that.gotRowVectorFromSource();
			} catch (Exception e) {
				throw new IllegalActionException(e.getMessage());
			}

		}
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
			Type type = new ArrayType(that.getBaseType(column.getDataType()));
			that.initializePort(name, type);
		}
	}

	/*
	 * Transform a column vector to array token, and send the array token to
	 * port in one fire
	 * 
	 * @exception IllegalActionException If there is no director.
	 */
	void fire() throws IllegalActionException {
		// fill in the values upon firing
		if (_columnVectorArray == null) {
			this.populateVector();
		}

		// log.debug("Processing record: " + i);
		if (_columnVectorArray != null) {
			Enumeration colEnum = that.getColumns().elements();
			int rowSize = _columnVectorArray.length;
			// Enumeration typeEnum = _colTypes.elements();
			int rowIndex = 0;
			while (colEnum.hasMoreElements() && rowIndex < rowSize) {
				DSTableFieldIFace colDef = (DSTableFieldIFace) colEnum
						.nextElement();
				Vector columnVector = _columnVectorArray[rowIndex];
				rowIndex++;
				Type type = (Type) that.getBaseType(colDef.getDataType());
				TypedIOPort port = (TypedIOPort) that.getPort(colDef.getName()
						.trim());
				if (port != null) {

					Token[] columnToken = Eml200DataSource
							.transformStringVectorToTokenArray(columnVector,
									type, colDef.getMissingValueCode());
					// send the data on the port
					port.send(0, new ArrayToken(columnToken));
				}
			}
			isFired = true;
		} else {
			throw new IllegalActionException("Couldn't get source data");
		}

	}

	/**
	 * If is already fired, return false
	 */
	public boolean prefire() throws IllegalActionException {

		if (isFired) {
			return false;
		} else {
			return true;
		}

	}

	public boolean postfire() throws IllegalActionException {
		return !isFired;
	}

}
