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

import org.ecoinformatics.seek.datasource.EcogridCompressedDataCacheItem;
import org.kepler.objectmanager.data.db.Entity;

import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.StringToken;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.util.IllegalActionException;

class Eml200DataOutputFormatUnzippedFileName extends Eml200DataOutputFormatBase {

	private String[] _targetFilePathInZip = null;

	private static final String _UNZIPPEDFILENAMEPORT = "UnzippedFileName";
	private static Collection portList;
	static {
		portList = new Vector(1);
		portList.add(_UNZIPPEDFILENAMEPORT);
		portList = Collections.unmodifiableCollection(portList);
	}

	Eml200DataOutputFormatUnzippedFileName(Eml200DataSource t) {
		super(t);
	}

	public String[] getTargetFilePathInZip() {
		return this._targetFilePathInZip;
	}

	/*
	 * InitializeAsUnCompressFileName. It will read the file path from
	 * uncompressed directory
	 */
	void initialize() throws IllegalActionException {
		Entity selectedTable = that.getSelectedTableEntity();
		if (selectedTable.getHasZipDataFile()
				|| selectedTable.getHasGZipDataFile()
				|| selectedTable.getHasGZipDataFile()) {
			String targetFileExtension = that.getFileExtensionInZip();
			that.log.debug("The file extension will send out is "
					+ targetFileExtension);
			EcogridCompressedDataCacheItem compressedItem = (EcogridCompressedDataCacheItem) that
					.getSelectedCachedDataItem();
			_targetFilePathInZip = compressedItem
					.getUnzippedFilePath(targetFileExtension);
		} else {
			throw new IllegalActionException(
					"The selected entity is not a compressed data file");
		}
	}

	/*
	 * This method will configure the ports for output the unzip file path.
	 * There is only one ports for this and the type is string token
	 */
	void reconfigurePorts() throws IllegalActionException {
		that.removeOtherOutputPorts(portList);
		that.initializePort(_UNZIPPEDFILENAMEPORT, new ArrayType(
				BaseType.STRING));
	}

	/*
	 * A zipped cache data item will be unzipped into a directory where it
	 * stores a file list. Given a file extension,( which will be read from
	 * string attribute _fileExtensionInZip) we will get array of file path. So
	 * each fire, a string (file name )will be send out.
	 */
	void fire() throws IllegalActionException {

		if (_targetFilePathInZip != null) {
			// put file name into a string token array
			int length = _targetFilePathInZip.length;
			StringToken[] fileNameArray = new StringToken[length];
			for (int i = 0; i < length; i++) {

				fileNameArray[i] = new StringToken(_targetFilePathInZip[i]);
			}
			TypedIOPort pp = (TypedIOPort) that.getPort(_UNZIPPEDFILENAMEPORT);
			pp.send(0, new ArrayToken(fileNameArray));
			_targetFilePathInZip = null;

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
}
