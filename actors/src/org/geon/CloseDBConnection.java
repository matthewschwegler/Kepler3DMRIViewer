/*
 * Copyright (c) 2002-2010 The Regents of the University of California.
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

package org.geon;

import java.sql.Connection;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// CloseDBConnection
/**
 * This actor disconnect from a database. The connection refernce is given by
 * the dbcon input port.
 * 
 * @UserLevelDocumentation This actor is used to release a database connection.
 *                         The connection refernce is given by the dbcon input
 *                         port.
 * @author Efrat Jaeger
 * @version $Id: CloseDBConnection.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 3.0.2
 */
public class CloseDBConnection extends TypedAtomicActor {

	/**
	 * Construct an actor with the given container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the actor cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */

	public CloseDBConnection(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {

		super(container, name);

		dbcon = new TypedIOPort(this, "dbcon", true, false);
		trigger = new TypedIOPort(this, "trigger", true, false);
		trigger.setMultiport(true);

		// Set the type constraints.
		dbcon.setTypeEquals(DBConnectionToken.DBCONNECTION);

		_attachText("_iconDescription", "<svg>\n"
				+ "<ellipse cx=\"0\" cy=\"-30\" " + "rx=\"20\" ry=\"10\"/>\n"
				+ "<line x1=\"20\" y1=\"0\" " + "x2=\"20\" y2=\"-30\"/>\n"
				+ "<line x1=\"-20\" y1=\"0\" " + "x2=\"-20\" y2=\"-30\"/>\n"
				+ "<line x1=\"-20\" y1=\"0\" " + "x2=\"20\" y2=\"0\"/>\n"
				+ "</svg>\n");

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * A reference to the database connection
	 * 
	 * @UserLevelDocumentation A reference to the database connection
	 */
	public TypedIOPort dbcon;

	/**
	 * A trigger for closing the db connection
	 * 
	 * @UserLevelDocumentation This port is used to trigger the actor.
	 */
	public TypedIOPort trigger;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Closes the input database connection when triggered.
	 */

	public void fire() throws IllegalActionException {
		try {

			// super.fire();

			// Trigger the actor.
			for (int i = 0; i < trigger.getWidth(); i++) {
				if (trigger.hasToken(i)) {
					trigger.get(i);
				}
			}
			System.out.println("in close db con - after trigger");
			// Get the db connection.
			DBConnectionToken _dbcon = (DBConnectionToken) dbcon.get(0);
			Connection _con = null;
			try {
				_con = _dbcon.getValue();
				System.out.println(_con.toString());
				_con.close();
				System.out
						.println("after closing the connection. Con is closed?"
								+ _con.isClosed());
			} catch (Exception ex) {
				if (_con != null) {
					// if the connection is still alive try to close it again.
					if (!_con.isClosed()) {
						_con.close();
					}
				}
			}
		} catch (Exception ex) {
		}
	}
}