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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.kepler.objectmanager.data.db.Attribute;
import org.kepler.objectmanager.data.db.Entity;

import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.util.IllegalActionException;

class Eml200DataOutputFormatAllFileNames extends Eml200DataOutputFormatBase {

	private List<String> _fileNames = null;
	private List<String> _resourceNames = null;
	private List<String> _delimiters = null;
	private List<Integer> _headerLines = null;
	private List<Vector<String>> _fieldNames = null;

	private static final String _CACHELOCALFILENAMEPORT = "CacheLocalFileNames";
	private static final String _CACHERESOURCENAMEPORT = "CacheResourceNames";
	private static final String _DELIMITERS_PORT = "Delimiters";
	private static final String _HEADER_LINES_PORT = "HeaderLines";
	private static final String _FIELD_NAMES_PORT = "FieldNames";
	
	private static Collection portList;
	static {
		portList = new Vector(2);
		portList.add(_CACHELOCALFILENAMEPORT);
		portList.add(_CACHERESOURCENAMEPORT);
		portList.add(_DELIMITERS_PORT);
		portList.add(_HEADER_LINES_PORT);
		portList.add(_FIELD_NAMES_PORT);
		portList = Collections.unmodifiableCollection(portList);
	}

	Eml200DataOutputFormatAllFileNames(Eml200DataSource t) {
		super(t);
	}

	/*
	 * InitializeAsCacheFileName. It will read the local cache file name
	 */
	void initialize() throws IllegalActionException {
		_fileNames = new ArrayList<String>();
		_resourceNames = new ArrayList<String>();
		_delimiters = new ArrayList<String>();
		_headerLines = new ArrayList<Integer>();
		_fieldNames = new ArrayList<Vector<String>>();
		
		int size  = that.getEntityList().size();
		for (int i = 0; i < size; i++) {
			Entity entity = that.getEntityList().get(i);
			// cache filename
			String fileName = entity.getDataCacheObject().getAbsoluteFileName();
			_fileNames.add(fileName);
			// entity name
			String tableName = entity.getName();
			_resourceNames.add(tableName);
			// delimiter
			String delimiter = entity.getDelimiter();
			_delimiters.add(delimiter);
			// headerlines
			int headerLines = entity.getNumHeaderLines();
			_headerLines.add(headerLines);
			// get the names
			 Vector<Attribute> fields = entity.getFields();
			 Vector<String> names = new Vector<String>();
			 for (int j = 0; j < fields.size(); j++) {
				 Attribute attribute = fields.get(j);
				 String name = attribute.getName();
				 names.add(name);
			 }
			 _fieldNames.add(names);

		}
	}

	/**
	 * This method will reconfig ports for people want to get a cache local file
	 * name. There are two ports for this output type. One port is for cache
	 * local file name. This is an absolute path of cache file name. The other
	 * port is for the resource location( it is a url in eml distribution).
	 * 
	 * @throws IllegalActionException
	 */
	void reconfigurePorts() throws IllegalActionException {
		that.removeOtherOutputPorts(portList);
		that.initializePort(_CACHELOCALFILENAMEPORT, new ArrayType(BaseType.STRING) );
		that.initializePort(_CACHERESOURCENAMEPORT, new ArrayType(BaseType.STRING) );
		that.initializePort(_DELIMITERS_PORT, new ArrayType(BaseType.STRING) );
		that.initializePort(_HEADER_LINES_PORT, new ArrayType(BaseType.STRING) );
		that.initializePort(_FIELD_NAMES_PORT, new ArrayType(new ArrayType(BaseType.STRING)) );
		
	}

	/*
	 * This method will send out a cache local file path and resource location
	 * of this data object
	 */
	void fire() throws IllegalActionException {

		
		int size  = _fileNames.size();
		Token[] fileTokens = new Token[size];
		Token[] nameTokens = new Token[size];
		Token[] delimiterTokens = new Token[size];
		Token[] headerLineTokens = new Token[size];
		Token[] fieldNameTokens = new Token[size];
		for (int i = 0; i < size; i++) {
			fileTokens[i] = new StringToken(_fileNames.get(i));
			nameTokens[i] = new StringToken(_resourceNames.get(i));
			delimiterTokens[i] = new StringToken(_delimiters.get(i));
			headerLineTokens[i] = new IntToken(_headerLines.get(i));
			
			// make a vector of vectors!
			Vector<String> names = _fieldNames.get(i);
			Token[] fieldNames = new Token[names.size()];
			for (int j = 0; j < names.size(); j++) {
				fieldNames[j] = new StringToken(names.get(j));
			}
			fieldNameTokens[i] = new ArrayToken(fieldNames);

		}
		TypedIOPort pp = (TypedIOPort) that.getPort(_CACHELOCALFILENAMEPORT);
		pp.send(0, new ArrayToken(fileTokens));
		TypedIOPort pp1 = (TypedIOPort) that.getPort(_CACHERESOURCENAMEPORT);
		pp1.send(0, new ArrayToken(nameTokens));
		TypedIOPort pp2 = (TypedIOPort) that.getPort(_DELIMITERS_PORT);
		pp2.send(0, new ArrayToken(delimiterTokens));
		TypedIOPort pp3 = (TypedIOPort) that.getPort(_HEADER_LINES_PORT);
		pp3.send(0, new ArrayToken(headerLineTokens));
		TypedIOPort pp4 = (TypedIOPort) that.getPort(_FIELD_NAMES_PORT);
		pp4.send(0, new ArrayToken(fieldNameTokens));
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
	 * Only fire once
	 */
	boolean postfire() throws IllegalActionException {
		return false;
	}

}
