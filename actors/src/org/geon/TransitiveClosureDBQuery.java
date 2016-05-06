/*
 * Copyright (c) 1998-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jianwu $'
 * '$Date: 2010-12-09 16:09:01 -0800 (Thu, 09 Dec 2010) $' 
 * '$Revision: 26473 $'
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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// TransitiveClosureDBQuery
/**
 * Receives a string query and a database connection reference. Produces the
 * transitive closure of the query.
 * 
 * @author Efrat Jaeger
 * @version $Id: TransitiveClosureDBQuery.java 12876 2006-05-18 23:17:52Z mangal
 *          $
 * @since Ptolemy II 3.0.2
 */
public class TransitiveClosureDBQuery extends TypedAtomicActor {

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
	public TransitiveClosureDBQuery(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// Ports
		dbcon = new TypedIOPort(this, "dbcon", true, false);
		dbcon.setTypeEquals(DBConnectionToken.DBCONNECTION);

		initialSet = new TypedIOPort(this, "initialSet", true, false);
		initialSet.setDisplayName("initial set");
		initialSet.setTypeEquals(new ArrayType(BaseType.GENERAL));

		result = new TypedIOPort(this, "result set", false, true);

		outputEachRowSeparately = new Parameter(this,
				"outputEachRowSeparately", new BooleanToken(false));
		outputEachRowSeparately.setTypeEquals(BaseType.BOOLEAN);

		fieldInSet = new StringParameter(this, "fieldInSet");
		fieldInSet.setDisplayName("field in set");

		query = new PortParameter(this, "query");
		query.setStringMode(true);

		_attachText("_iconDescription", "<svg>\n"
				+ "<ellipse cx=\"0\" cy=\"-30\" " + "rx=\"20\" ry=\"10\"/>\n"
				+ "<line x1=\"20\" y1=\"0\" " + "x2=\"20\" y2=\"-30\"/>\n"
				+ "<line x1=\"-20\" y1=\"0\" " + "x2=\"-20\" y2=\"-30\"/>\n"
				+ "<line x1=\"-20\" y1=\"0\" " + "x2=\"20\" y2=\"0\"/>\n"
				+ "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/** Initial input set */
	public TypedIOPort initialSet;

	/** Result set */
	public TypedIOPort result;

	/**
	 * Specify whether to display the complete result at once or each row
	 * separately.
	 */
	public Parameter outputEachRowSeparately;

	/**
	 * A reference to the database connection.
	 */
	public TypedIOPort dbcon;

	/**
	 * A query string.
	 */
	public PortParameter query;

	/** Field in set condition field */
	public StringParameter fieldInSet;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Set the output type
	 * 
	 * @param attribute
	 *            The attribute that changed.
	 * @exception IllegalActionException
	 *                If the output type is not recognized.
	 */
	public void preinitialize() throws IllegalActionException {
		super.preinitialize();

		// Set the output type.
		_separate = ((BooleanToken) outputEachRowSeparately.getToken())
				.booleanValue();
		if (_separate) {
			result.setTypeEquals(BaseType.STRING);
		} else {
			result.setTypeEquals(new ArrayType(BaseType.STRING));
		}
	}

	/**
	 * Consumes a query and a database connection reference. Compute the query
	 * result according to the specified output format.
	 * 
	 * @exception IllegalActionException
	 *                If there is no director.
	 */
	public void fire() throws IllegalActionException {

		if (dbcon.hasToken(0) && initialSet.hasToken(0)) {
			DBConnectionToken _dbcon = (DBConnectionToken) dbcon.get(0);
			Connection _con;
			try {
				_con = _dbcon.getValue();
			} catch (Exception e) {
				throw new IllegalActionException(this, e, "CONNECTION FAILURE");
			}

			// Initial set.
			ArrayToken initSetArray = (ArrayToken) initialSet.get(0);
			Set s1 = new TreeSet();
			// Set s2 = new TreeSet();

			String set = "(";
			for (int i = 0; i < initSetArray.length(); i++) {
				String input = ((StringToken) initSetArray.getElement(i))
						.stringValue();
				s1.add(input);
				set += input + ",";
			}
			set = set.trim().substring(0, set.length() - 1) + ")";

			// the select query has to be of a single attribute that has to be
			// specified
			query.update();
			String _query = ((StringToken) query.getToken()).stringValue();

			// Leave just the first column.
			// int selectInd = _query.toLowerCase().indexOf("select");
			int fromInd = _query.toLowerCase().indexOf("from");
			int commaInd = _query.indexOf(",");
			if (commaInd > -1 && commaInd < fromInd) {
				_query = _query.substring(0, commaInd)
						+ _query.substring(fromInd - 1);
			}
			// String _field = _query.substring(selectInd+7, fromInd-1).trim();

			String originalQuery = _query;
			String _fieldInSet = ((StringToken) fieldInSet.getToken())
					.stringValue();
			// _query = addFieldInSetCondition(originalQuery, _fieldInSet, set);

			try {
				Statement st = _con.createStatement();
				ResultSet rs;

				boolean contained = false;
				if (s1.isEmpty()) {
					contained = true;
				}

				while (!contained) {
					try {
						contained = true;
						_query = addFieldInSetCondition(originalQuery,
								_fieldInSet, set);
						rs = st.executeQuery(_query);
						set = "(";
					} catch (Exception e1) {
						throw new IllegalActionException(this, e1,
								"SQL executeQuery exception");
					}
					while (rs.next()) {
						String val = rs.getString(1);
						if (val == null) // is this necessary?
							val = "";
						if (!s1.contains(val)) {
							s1.add(val);
							set += val + ",";
							contained = false;
						}
					}
					set = set.substring(0, set.length() - 1) + ")";
					rs.close();
					st.close();
				}
				int i = 0;
				Token resultSet[] = new Token[s1.size()];
				Iterator sIt = s1.iterator();
				while (sIt.hasNext()) {
					String val = (String) sIt.next();
					if (_separate)
						result.broadcast(new StringToken(val));
					else {
						resultSet[i++] = new StringToken(val);
					}
				}
				if (!_separate) {
					result.broadcast(new ArrayToken(resultSet));
				}
			} catch (Exception ex) {
				throw new IllegalActionException(this, ex, "exception in SQL");
			}
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // private variables ////

	/**
	 * Add field in set condition to the query
	 */

	String addFieldInSetCondition(String query, String field, String set) {
		String cond = field + " in " + set;
		int whereInd = query.toLowerCase().indexOf("where");
		if (whereInd > -1) { // NOTE! Ignoring inline queries.
			query = query.substring(0, whereInd + 6) + cond + " and "
					+ query.substring(whereInd + 6);
		} else { // NOTE! for the mean time, ignoring other operations such as
					// order by, group by, limit.
			query = query + " where " + cond;
		}
		return query;
	}

	// /////////////////////////////////////////////////////////////////
	// // private variables ////

	/**
	 * Output indicator parameter.
	 */
	private boolean _separate;

}