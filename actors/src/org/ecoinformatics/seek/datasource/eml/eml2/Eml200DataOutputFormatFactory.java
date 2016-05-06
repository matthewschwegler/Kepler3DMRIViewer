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

class Eml200DataOutputFormatFactory {
	static final String _AsField = "As Field";
	static final String _AsTable = "As Table";
	static final String _AsRow = "As Row";
	static final String _AsByteArray = "As Byte Array";
	static final String _AsUnzippedFileName = "As UnCompressed File Name";
	static final String _AsFileName = "As Cache File Name";
	static final String _AsAllFileNames = "As All Cache File Names";
	static final String _AsColumnVector = "As Column Vector";
	static final String _AsColumnRecord = "As ColumnBased Record";

	private Eml200DataOutputFormatFactory() {
	}

	static Eml200DataOutputFormatBase newInstance(String dataOutputFormat,
			Eml200DataSource that) {
		if (_AsTable.equals(dataOutputFormat)) {
			return new Eml200DataOutputFormatTable(that);
		}
		if (_AsRow.equals(dataOutputFormat)) {
			return new Eml200DataOutputFormatRow(that);
		}
		if (_AsField.equals(dataOutputFormat)) {
			return new Eml200DataOutputFormatField(that);
		}
		if (_AsByteArray.equals(dataOutputFormat)) {
			return new Eml200DataOutputFormatByteArray(that);
		}
		if (_AsUnzippedFileName.equals(dataOutputFormat)) {
			return new Eml200DataOutputFormatUnzippedFileName(that);
		}
		if (_AsFileName.equals(dataOutputFormat)) {
			return new Eml200DataOutputFormatFileName(that);
		}
		if (_AsAllFileNames.equals(dataOutputFormat)) {
			return new Eml200DataOutputFormatAllFileNames(that);
		}
		if (_AsColumnVector.equals(dataOutputFormat)) {
			return new Eml200DataOutputFormatColumnVector(that);
		}
		if (_AsColumnRecord.equals(dataOutputFormat)) {
			return new Eml200DataOutputFormatColumnRecord(that);
		}
		return new Eml200DataOutputFormatField(that);
	}

	static Eml200DataOutputFormatBase newInstance(Eml200DataSource that) {
		return new Eml200DataOutputFormatInitialize(that);
	}
}
