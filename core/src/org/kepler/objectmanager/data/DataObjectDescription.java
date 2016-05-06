/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
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

package org.kepler.objectmanager.data;

import java.util.Vector;

import org.kepler.objectmanager.data.db.DSTableFieldIFace;

/**
 * This object represents an DataObjectDescription. A DataObjectDescription
 * stores information about a DataItem that is used in a Step in the pipeline.
 */
public class DataObjectDescription implements DSTableFieldIFace {
	protected String id;
	protected String name;
	protected String dataType;
	protected String definition;
	protected Vector missingValueCode = new Vector();

	/**
	 * Construct a DataObjectDescription.
	 */
	public DataObjectDescription(String id, String name, String dataType) {
		this(id, name, dataType, null);
	}

	/**
	 * Construct a DataObjectDescription with a description.
	 */
	public DataObjectDescription(String id, String name, String dataType,
			String definition) {
		if (id == null) {
			this.id = "";
		} else {
			this.id = id;
		}
		if (name == null) {
			this.name = "";
		} else {
			this.name = name.trim();
		}
		if (dataType == null) {
			this.dataType = "";
		} else {
			this.dataType = dataType;
		}
		if (definition == null) {
			this.definition = "";
		} else {
			this.definition = definition;
		}
	}

	/**
	 * Return the unique ID for this data item. It is unique within the scope of
	 * the Step in which it is described.
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Return the name for this data item.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the data type for this data item.
	 */
	public String getDataType() {
		return this.dataType;
	}

	/**
	 * Return the definition for this data item.
	 */
	public String getDefinition() {
		return this.definition;
	}

	/**
	 * returns true if all of the fields of didesc are equal to the fields of
	 * this object.
	 */
	public boolean equals(DataObjectDescription didesc) {
		if (didesc.getId().trim().equals(this.id.trim())
				&& didesc.getName().trim().equals(this.name.trim())
				&& didesc.getDataType().trim().equals(this.dataType.trim())
				&& didesc.getDefinition().trim().equals(this.definition.trim())) {
			return true;
		}

		return false;
	}

	/**
	 * Set the identifier for this data item.
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Set the name for this data item.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Set the type for this data item.
	 */
	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	/**
	 * Set the definition for this data item.
	 */
	public void setDefinition(String definition) {
		this.definition = definition;
	}

	/**
	 * Produce a string view of the item, just the name.
	 */
	public String toString() {
		return name;
	}

	/**
	 * Method to add missing value code into a vector. This method will be used
	 * to store the missing value code in metadata
	 * 
	 * @param code
	 */
	public void addMissingValueCode(String code) {
		if (code != null) {
			missingValueCode.add(code);
		}
	}

	/**
	 * Method to return the vector which store the missing value code. If this
	 * attribute doesn't has the missing value code, empty vector will be
	 * returned.
	 * 
	 * 	 */
	public Vector getMissingValueCode() {
		return missingValueCode;
	}

	/**
	 * Method to return the vector which store the missing value code. If this
	 * attribute doesn't has the missing value code, empty vector will be
	 * returned.
	 * 
	 * 	 */
	public void setMissingValueCode(Vector missingValueVector) {
		missingValueCode = missingValueVector;
	}

	/**
	 * Utility for writing out XML elements
	 * 
	 * @param StringBuffer
	 *            the buffer to write to
	 * @param the
	 *            name of the element to create
	 * @param the
	 *            value for the element
	 */
	protected static void appendElement(StringBuffer x, String name,
			String value) {
		x.append("<");
		x.append(name);
		x.append(">");
		x.append(value);
		x.append("</");
		x.append(name);
		x.append(">\n");
	}

}