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

import java.util.Collection;
import java.util.Collections;
import java.util.Vector;

import org.kepler.objectmanager.data.db.DSTableFieldIFace;

import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.OrderedRecordToken;
import ptolemy.data.RecordToken;
import ptolemy.data.Token;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.RecordType;
import ptolemy.kernel.util.IllegalActionException;

class Eml200DataOutputFormatColumnRecord extends
		Eml200DataOutputFormatColumnVector {

	private RecordToken _recordToken = null;

	private static final String RECORDPORTNAME = "Record";
	private RecordType recordType;

	private static Collection portList;
	static {
		portList = new Vector(1);
		portList.add(RECORDPORTNAME);
		portList = Collections.unmodifiableCollection(portList);
	}

	Eml200DataOutputFormatColumnRecord(Eml200DataSource t) {
		super(t);
	}

	/*
	 * Initialize as column based record for fire.
	 */
	void initialize() throws IllegalActionException {
		// get columnDataArray
		super.initialize();
		isFired = false;
		// the bulk of what used to be here is now called
		// from within fire() using a private method

		// reset the data - selected entity and/or selection criteria may have
		// changed!
		_recordToken = null;

	}

	private void generateRecord() throws IllegalActionException {

		// make sure there's some data to work with first
		if (_columnVectorArray == null) {
			super.populateVector();
		}

		// generate record
		int size = that.getColumnLabels().length;
		int dataSize = _columnVectorArray.length;
		if (size != dataSize) {
			throw new IllegalActionException(
					"The data column didn't match head column");
		}
		Token[] data = new ArrayToken[size];
		Vector attributes = that.getColumns();
		for (int i = 0; i < size; i++) {
			Vector columnData = _columnVectorArray[i];
			DSTableFieldIFace colDef = (DSTableFieldIFace) attributes
					.elementAt(i);
			Vector missingValue = colDef.getMissingValueCode();
			Token[] columnTokens = Eml200DataSource
					.transformStringVectorToTokenArray(columnData, that
							.getColumnTypes()[i], missingValue);
			// transform columnTokens to ArrayToken
			ArrayToken arrayToken = new ArrayToken(columnTokens);
			data[i] = arrayToken;
		}
		_recordToken = new OrderedRecordToken(that.getColumnLabels(), data);
	}

	/*
	 * This method will configure the actor output as a column based record. It
	 * will has one output port and name will be "Record".
	 */
	void reconfigurePorts() throws IllegalActionException {
		// the record is an ArrayToken, we should transfer from base type to
		// array type
		if (that.getColumnTypes() == null) {
			Eml200DataSource.log
					.debug("The columns info is null and coudldn't conigure ports as column record");
			return;
		}
		int size = that.getColumnTypes().length;
		ArrayType[] arrayTypeList = new ArrayType[size];
		for (int i = 0; i < size; i++) {
			arrayTypeList[i] = new ArrayType(that.getColumnTypes()[i]);
		}
		RecordType recordType = new RecordType(that.getColumnLabels(),
				arrayTypeList);

		that.removeOtherOutputPorts(portList);
		that.initializePort(RECORDPORTNAME, recordType);
	}

	/*
	 * This method will fire as column based record. Column based record is
	 * already prepared in initialize
	 */
	void fire() throws IllegalActionException {
		if (_recordToken == null) {
			generateRecord();
		}
		TypedIOPort pp = (TypedIOPort) that.getPort(RECORDPORTNAME);
		pp.send(0, _recordToken);
		isFired = true;
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

}
