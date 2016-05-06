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

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Vector;

import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.Token;
import ptolemy.data.UnsignedByteToken;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.util.IllegalActionException;

class Eml200DataOutputFormatByteArray extends Eml200DataOutputFormatBase {

	private InputStream _reader = null;
	private int _numberOfBytesRead = 0;
	private byte[] _byteArray = new byte[200000];
	private boolean _endOfStream = false;

	private static final String _BINARYDATAPORT = "BinaryData";
	private static final String _ENDOFSTREAMPORT = "EndOfStream";
	private static Collection portList;
	static {
		portList = new Vector(2);
		portList.add(_BINARYDATAPORT);
		portList.add(_ENDOFSTREAMPORT);
		portList = Collections.unmodifiableCollection(portList);
	}

	Eml200DataOutputFormatByteArray(Eml200DataSource t) {
		super(t);
	}

	/*
	 * Initialize as byteArray. It will read byte array from reader
	 */
	void initialize() throws IllegalActionException {
		_reader = that.getSelectedCachedDataItem().getDataInputStream();
		if (_reader != null) {
			try {
				// _reader.reset();
				_numberOfBytesRead = _reader.read(_byteArray);
			} catch (IOException ex) {
				throw new IllegalActionException(that, ex,
						"couldn't read inputstream for as byte array output.");
			}

		}
	}

	/*
	 * This method will reconfigure ports when user choose the output as byte
	 * array. There are two exports in the actor, one is the port for sending
	 * out the byte array, the other is send out a boolean indactor if the byte
	 * array is reach the end of input stream.
	 * 
	 * @throws ptolemy.kernel.util.IllegalActionException
	 */
	void reconfigurePorts() throws IllegalActionException {
		that.removeOtherOutputPorts(portList);
		that.initializePort(_BINARYDATAPORT, new ArrayType(
				BaseType.UNSIGNED_BYTE));
		that.initializePort(_ENDOFSTREAMPORT, BaseType.BOOLEAN);
	}

	/*
	 * This method will be called in fire method when user choose the output
	 * type is As Byte Array. It is handle large binary file. During the
	 * excution, input tream of from dataChacheItem will be first read into a
	 * byte array in initilize method(Only once for start during a excution).
	 * Then fire method will send out the byte array. Post fire method will
	 * continue read the input stream to byte array and fire method will send
	 * them out. This will stop util reach end of inputstream.(Output the data
	 * read in the initialize() or in the previous invocation of postfire(), if
	 * there is any.
	 */
	void fire() throws IllegalActionException {
		if (_numberOfBytesRead > 0) {
			Token _bytes[] = new Token[_numberOfBytesRead];
			for (int i = 0; i < _numberOfBytesRead; i++) {
				_bytes[i] = new UnsignedByteToken(_byteArray[i]);

			}
			TypedIOPort dataPort = (TypedIOPort) that.getPort(_BINARYDATAPORT);
			dataPort.send(0, new ArrayToken(_bytes));
		}

	}

	boolean postfire() throws IllegalActionException {
		if (_reader == null) {
			return false;
		}
		try {
			TypedIOPort endOfStream = (TypedIOPort) that
					.getPort(_ENDOFSTREAMPORT);
			_numberOfBytesRead = _reader.read(_byteArray);
			if (_numberOfBytesRead <= 0) {
				// it is reach the end of inputstream;
				endOfStream.broadcast(BooleanToken.TRUE);
				return false;
			}
			endOfStream.broadcast(BooleanToken.FALSE);
			return true;
		} catch (IOException ex) {
			throw new IllegalActionException(that, ex, "Postfire failed");
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
