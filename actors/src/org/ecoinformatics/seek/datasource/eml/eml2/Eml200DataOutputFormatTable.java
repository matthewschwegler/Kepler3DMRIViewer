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

import org.kepler.objectmanager.cache.CacheException;

import ptolemy.actor.TypedIOPort;
import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.util.IllegalActionException;

class Eml200DataOutputFormatTable extends Eml200DataOutputFormatBase {

	private static final String DATATABLEPORTNAME = "DataTable";
	private static final String DELIMITERPORTNAME = "Delimiter";
	private static final String NUMCOLUMNSPORTNAME = "numColumns";
	private static Collection portList;

	static {
		portList = new Vector(3);
		portList.add(DATATABLEPORTNAME);
		portList.add(DELIMITERPORTNAME);
		portList.add(NUMCOLUMNSPORTNAME);
		portList = Collections.unmodifiableCollection(portList);
	}

	Eml200DataOutputFormatTable(Eml200DataSource t) {
		super(t);
	}

	/*
	 * Intialize for output as field, row or Table. It will generate query from
	 * sqlDef attribute(if sqlDef is empty, the generate query as select from
	 * selectedEntity. Then run this query action and get resultset
	 */
	void initialize() throws IllegalActionException {
	}

	boolean isAlreadyConfigured() {
		return that.getPort("DataTable") != null
				&& that.getPort("Delimiter") != null
				&& that.getPort("numColumns") != null;
	}

	void setPortTypes() throws IllegalActionException {
		TypedIOPort pp1 = (TypedIOPort) that.getPort("DataTable");
		pp1.setTypeEquals(BaseType.STRING);
		TypedIOPort pp = (TypedIOPort) that.getPort("Delimiter");
		pp.setTypeEquals(BaseType.STRING);
		TypedIOPort pp2 = (TypedIOPort) that.getPort("numColumns");
		pp2.setTypeEquals(BaseType.INT);
		return;
	}

	/**
	 * It reconfigures all the Ports to represent passing data back a row at a
	 * time.
	 * 
	 * @throws ptolemy.kernel.util.IllegalActionException
	 */
	void reconfigurePorts() throws IllegalActionException {
		that.removeOtherOutputPorts(portList);
		that.initializePort(DATATABLEPORTNAME, BaseType.STRING);
		that.initializePort(DELIMITERPORTNAME, BaseType.STRING);
		that.initializePort(NUMCOLUMNSPORTNAME, BaseType.INT);
	}

	/*
	 * Send an entire tableas a single token over the port on each fire event.
	 * 
	 * @exception IllegalActionException If there is no director.
	 */
	void fire() throws IllegalActionException {
		TypedIOPort pp = (TypedIOPort) that.getPort("DataTable");
		try {
			pp.send(0, new StringToken(new String(that
					.getSelectedCachedDataItem().getData())));
		} catch (CacheException e) {
			throw new IllegalActionException(that, e,
					"Unable to retrieve data from Cache");
		}

		pp = (TypedIOPort) that.getPort("Delimiter");
		String delim = that.getSelectedTableEntity().getDelimiter();
		pp.send(0, new StringToken(delim));

		pp = (TypedIOPort) that.getPort("numColumns");
		pp.send(0, new IntToken(
				that.getSelectedTableEntity().getAttributes().length));

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
