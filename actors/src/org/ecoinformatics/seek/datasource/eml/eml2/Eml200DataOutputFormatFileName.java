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

import ptolemy.actor.TypedIOPort;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.util.IllegalActionException;

class Eml200DataOutputFormatFileName extends Eml200DataOutputFormatBase {

	private String _selectedDataCacheItemLocalFileName = null;
	private String _selectedDataCacheItemResourceName = null;

	private static final String _CACHELOCALFILENAMEPORT = "CacheLocalFileName";
	private static final String _CACHERESOURCENAMEPORT = "CacheResourceName";
	private static Collection portList;
	static {
		portList = new Vector(2);
		portList.add(_CACHELOCALFILENAMEPORT);
		portList.add(_CACHERESOURCENAMEPORT);
		portList = Collections.unmodifiableCollection(portList);
	}

	Eml200DataOutputFormatFileName(Eml200DataSource t) {
		super(t);
	}

	/*
	 * InitializeAsCacheFileName. It will read the local cache file name
	 */
	void initialize() throws IllegalActionException {
		if (that.getSelectedCachedDataItem() != null) {
			_selectedDataCacheItemLocalFileName = that
					.getSelectedCachedDataItem().getAbsoluteFileName();
			Eml200DataSource.log
					.debug("The locale file name for data cache is "
							+ _selectedDataCacheItemLocalFileName);
			_selectedDataCacheItemResourceName = that
					.getSelectedCachedDataItem().getResourceName();
			Eml200DataSource.log
					.debug("The resource name for the data cache is "
							+ _selectedDataCacheItemResourceName);
		} else {
			throw new IllegalActionException(
					"The selected entity is null and couldn't find data cache for it");
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
		that.initializePort(_CACHELOCALFILENAMEPORT, BaseType.STRING);
		that.initializePort(_CACHERESOURCENAMEPORT, BaseType.STRING);
	}

	/*
	 * This method will send out a cache local file path and resource location
	 * of this data object
	 */
	void fire() throws IllegalActionException {

		TypedIOPort pp = (TypedIOPort) that.getPort(_CACHELOCALFILENAMEPORT);
		pp.send(0, new StringToken(_selectedDataCacheItemLocalFileName));
		TypedIOPort pp1 = (TypedIOPort) that.getPort(_CACHERESOURCENAMEPORT);
		pp1.send(0, new StringToken(_selectedDataCacheItemResourceName));
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
