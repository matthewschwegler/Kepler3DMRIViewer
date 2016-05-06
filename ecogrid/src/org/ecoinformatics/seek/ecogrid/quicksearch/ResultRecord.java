/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: tao $'
 * '$Date: 2012-05-08 16:52:13 -0700 (Tue, 08 May 2012) $' 
 * '$Revision: 29820 $'
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

package org.ecoinformatics.seek.ecogrid.quicksearch;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Vector;

import org.ecoinformatics.seek.datasource.EcogridMetaDataCacheItem;
import org.kepler.dataproxy.datasource.DataSourceInterface;
import org.kepler.objectmanager.cache.DataCacheManager;

import ptolemy.actor.lib.Source;
import ptolemy.data.BooleanToken;
import ptolemy.data.expr.SingletonParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.StringAttribute;

/**
 * A class that represents a single record from a query result. This class is
 * used to display the records in a tree view in the tree view on the left of
 * the Kepler interface.
 * 
 * @see ResultPane
 * @author Matt Jones and Jing Tao
 */
public class ResultRecord extends Source implements DataSourceInterface {
	public SingletonParameter hide;
	private Vector recordDetailList = new Vector();

	// private static final String ENDPOINT = "endpoint";
	// private static final String RECORDID = "recordid";
	// private static final String NAMESPACE = "namespace";

	// /////////////////////////////////////////////////////////////////
	// // private variables ////

	/**
	 * Create a new ResultRecord with the given name. The name is used to
	 * display the record in various user interface views.
	 * 
	 * @param container
	 *            The model that contains this entity.
	 * @param name
	 *            The name of this result set resord.
	 * @exception IllegalActionException
	 *                If the actor cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public ResultRecord(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
		// _attachText("_iconDescription", getBusyIcon());

		hide = new SingletonParameter(output, "_hide"); // DFH
		hide.setToken(BooleanToken.TRUE); // DFH

		// get rid of the output port since we don't need it. this port is
		// created automatically by the Source interface
		// try {
		// output.setContainer(null);
		// } catch (Exception e) {//nothing to do here. the only exception
		// thrown
		// // is a duplicateNameException
		// }
	}

	/*
	 * protected String getReadyIcon() { return
	 * "<svg xmlns=\"http://www.w3.org/2000/svg\"" +
	 * "     width=\"22.438\" height=\"23.125\" viewBox=\"0 0 22.438 23.125\"" +
	 * "     style=\"overflow:visible;enable-background:new 0 0 22.438 23.125\""
	 * + "     xml:space=\"preserve\">\n" +
	 * "    <polygon  style=\"fill:#F7F999;stroke:#D7AA2A;stroke-linejoin:round;\" "
	 * +
	 * "          points=\"6.5,-26.5 6.5,-7.875 27.917,-10.167 27.917,-27 16,-27 15,-28.563 7.063,-28.563\"/>\n"
	 * +
	 * "    <polyline style=\"fill:none;stroke:#D7AA2A;stroke-linejoin:round;\" "
	 * +
	 * "          points=\"6.5,-19.833 8.333,-22.5 14.417,-22.5 16.417,-24.5 20.417,-24.5 21.417,-25.625 27.917,-25.625\"/>\n"
	 * + "    <text x=\"10\" y=\"-12\" " +
	 * "          style=\"fill:#108531; font-family:'Courier-Bold'; font-size:8;\">0101</text>\n"
	 * + "</svg>\n"; }
	 */

	/**
	 * Set the identifier of this record.
	 * 
	 * @param id
	 *            the String that uniquely identifies the record
	 */
	public void setRecordId(String id) {
		try {
			StringAttribute att = (StringAttribute) this.getAttribute(RECORDID);
			if (att == null) {
				att = new StringAttribute(this, RECORDID);
			}
			att.setExpression(id);
		} catch (IllegalActionException iae) {
			System.err.println("Could not add the record id.");
		} catch (NameDuplicationException nde) {
			System.err.println("Could not add the record id.");
		}
	}

	/**
	 * Get the identifier of this record.
	 * 
	 * @return the String that uniquely identifies the record
	 */
	public String getRecordId() {
		String value = null;
		try {
			StringAttribute attribute = (StringAttribute) this
					.getAttribute(RECORDID);
			value = attribute.getExpression();
		} catch (Exception e) {
			// System.err.println("getRecordId - RECORDID attr is null.");
		}
		return value;
	}

	/**
	 * Set the endpoint of this record. The endpoint indicates where the service
	 * generating the record can be accessed.
	 * 
	 * @param endpoint
	 *            the URL of the service that contains the record
	 */
	public void setEndpoint(String endpoint) {
		try {
			StringAttribute att = new StringAttribute(this, ENDPOINT);
			att.setExpression(endpoint);
		} catch (IllegalActionException iae) {
			System.err.println("Could not add the endpoint.");
		} catch (NameDuplicationException nde) {
			System.err.println("Could not add the endpoint.");
		}
	}

	/**
	 * Get the endpoint of this record. The endpoint indicates where the service
	 * generating the record can be accessed.
	 * 
	 * @return endpoint the URL of the service that contains the record
	 */
	public String getEndpoint() {
		String value = null;
		try {
			StringAttribute attribute = (StringAttribute) this
					.getAttribute(ENDPOINT);
			value = attribute.getExpression();
		} catch (Exception e) {
			// System.err.println("getEndpoint - ENDPOINT attr is null.");
		}
		return value;
	}

	/**
	 * Set the namespace of this record. The namespace indicates the document
	 * type
	 * 
	 * @param endpoint
	 *            the URL of the service that contains the record
	 */
	public void setNamespace(String namespace) {
		try {
			StringAttribute att = new StringAttribute(this, NAMESPACE);
			att.setExpression(namespace);
		} catch (IllegalActionException iae) {
			System.err.println("Could not add the namespace.");
		} catch (NameDuplicationException nde) {
			System.err.println("Could not add the namespace");
		}
	}

	/**
	 * Get the namespace of this record.
	 * 
	 * @return namespace the URL of the service that contains the record
	 */
	public String getNamespace() {
		String value = null;
		try {
			StringAttribute attribute = (StringAttribute) this
					.getAttribute(NAMESPACE);
			value = attribute.getExpression();
		} catch (Exception e) {
			System.err.println("getNamespace - NAMESPACE attr is null.");
		}
		return value;
	}

	/**
	 * Add one or more detail attributes to the record. These detail attributes
	 * are displayed in the user interface below the record for additional
	 * information about the record.
	 * 
	 * @param detail
	 *            the additional information about the record
	 */
	public void addRecordDetail(String detail) {
		try {
			ResultRecordDetail att = new ResultRecordDetail(this, detail);
			recordDetailList.add(detail);
		} catch (IllegalActionException iae) {
			System.err.println("Could not add the detail.");
		} catch (NameDuplicationException nde) {
			System.err.println("Could not add the detail.");
		}
	}

	/**
	 * Determine if the recordId and endpoint attributes have valid values for
	 * use in retrieving the record for parsing.
	 * 
	 * @return boolean true if both recordid and endpoint are not null and are
	 *         not the empty string
	 */
	public boolean hasConnectionValues() {
		boolean hasValues = false;
		String recordId = this.getRecordId();
		String endpoint = this.getEndpoint();

		if (recordId != null && !recordId.equals("") && endpoint != null
				&& !endpoint.equals("")) {
			hasValues = true;
		}
		return hasValues;
	}

	public Reader getFullRecord() {
		Reader recordReader = null;
		EcogridMetaDataCacheItem item = (EcogridMetaDataCacheItem) DataCacheManager
				.getCacheItem(null, "MetaData " + this.getRecordId(), this
						.getEndpoint(), EcogridMetaDataCacheItem.class
						.getName());
		item.setEndPoint(this.getEndpoint());
		item.setRecordId(this.getRecordId());
		item.start();

		// make sure the item is finished
		while (!item.isError() && !item.isReady()) {
			// do nothing, just waiting
			// System.out.println("Waiting!!!!!!!!!!");
		}
		// when it is ready
		InputStream stream = item.getDataInputStream();
		if (stream != null) {
			recordReader = new InputStreamReader(stream);
		}

		return recordReader;
	}

	public URL getDocumentation() {
		return null;
	}

	/**
	 * Method to transfer a array of ResultRecord to a Vector
	 * 
	 * @param source
	 *            the source array of ResultRecord
	 * @param dest
	 *            the destation vector the element will be added.
	 */
	public static void transformResultRecordArrayToVector(
			ResultRecord[] source, Vector dest) {
		if (source == null || dest == null) {
			return;
		}
		int size = source.length;
		for (int i = 0; i < size; i++) {
			ResultRecord record = source[i];
			dest.add(record);
		}
	}

	/**
	 * Get the recordDetailsList in this entity
	 * 
	 * 	 */
	public Vector getRecordDetailList() {
		return recordDetailList;
	}

}