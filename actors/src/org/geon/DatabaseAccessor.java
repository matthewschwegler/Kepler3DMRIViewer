/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-02-22 13:44:09 -0800 (Wed, 22 Feb 2012) $' 
 * '$Revision: 29445 $'
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

package org.geon;

import java.sql.Connection;
import java.sql.SQLException;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.IntToken;
import ptolemy.data.RecordToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * A base class for actors that access JDBC databases.
 * 
 * @author Daniel Crawl
 * @version $Id: DatabaseAccessor.java 29445 2012-02-22 21:44:09Z crawl $
 */

public class DatabaseAccessor extends TypedAtomicActor {
	public DatabaseAccessor(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		dbParams = new PortParameter(this, "dbParams");
		Type type = OpenDBConnection.getDBParamsType();
		dbParams.setTypeEquals(type);
		dbParams.getPort().setTypeEquals(type);

		dbcon = new TypedIOPort(this, "dbcon", true, false);
		dbcon.setTypeEquals(DBConnectionToken.DBCONNECTION);

		dbconTokenConsumptionRate = new Parameter(dbcon,
				"tokenConsumptionRate", IntToken.ONE);
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/** A record token containing parameters to create a database connection. */
	public PortParameter dbParams;

	/** A reference to the database connection. */
	public TypedIOPort dbcon;

	/** Parameter to change token consumption rate on dbcon. */
	public Parameter dbconTokenConsumptionRate;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/** Close the connection if open. */
	public void initialize() throws IllegalActionException {
		super.initialize();
		_closeConnection();
	}

	/** If connection is closed, open a new one. */
	public void fire() throws IllegalActionException {
	    dbParams.update();
	    if (_db == null) {
			getConnection();
		}
	}

	/** Close the connection if open. */
	public void wrapup() throws IllegalActionException {
		super.wrapup();
		_closeConnection();
	}

	// /////////////////////////////////////////////////////////////////
	// // protected methods ////

	/** Get the database connection. */
	protected void getConnection() throws IllegalActionException {
		// first check dbParams
		RecordToken params = (RecordToken) dbParams.getToken();
		if (params != null) {
			_db = OpenDBConnection.getConnection(params);
		} else if(_db == null && dbcon.getWidth() > 0) {
			DBConnectionToken dbToken = (DBConnectionToken) dbcon.get(0);
			try {
				_db = dbToken.getValue();
			} catch (Exception e) {
				throw new IllegalActionException(this, e, "CONNECTION FAILURE");
			}
		}
		
		if(_db == null) {
		    throw new IllegalActionException(this, "Must provide either" +
	    		" database connection parameters in dbParams or database" +
	    		" connection in dbcon.");
		}

		// we no longer need to read from dbcon
		dbconTokenConsumptionRate.setToken(IntToken.ZERO);
	}

	// /////////////////////////////////////////////////////////////////
	// // protected variables ////

	/** A JDBC database connection. */
	protected Connection _db = null;

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	/** Close the connection if open. */
	private void _closeConnection() throws IllegalActionException {
		try {
			if (_db != null) {
				_db.close();
			}
			_db = null;

			// reset the token consumption rate on dbcon
			dbconTokenConsumptionRate.setToken(IntToken.ONE);
		} catch (SQLException e) {
			throw new IllegalActionException(this, "SQLException: "
					+ e.getMessage());
		}
	}
}