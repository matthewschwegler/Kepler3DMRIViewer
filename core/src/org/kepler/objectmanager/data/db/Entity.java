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

package org.kepler.objectmanager.data.db;

import java.util.Vector;

import org.kepler.objectmanager.cache.DataCacheObject;
import org.kepler.objectmanager.data.DataObjectDescription;
import org.kepler.objectmanager.data.text.TextComplexDataFormat;

/**
 * This object represents an TableEntity. A TableEntity stores information about
 * a table of Attributes that is used in a Step in the pipeline.
 */
public class Entity extends DataObjectDescription implements DSTableIFace {
	/** static variable for ROWMAJOR tables **/
	public static String ROWMAJOR = "ROWMAJOR";
	/** static variable for COLUMNMAJOR tables **/
	public static String COLUMNMAJOR = "COLUMNMAJOR";
	public static String ZIP = "zip";
	public static String TAR = "application/x-tar";
	public static String GZIP = "gzip";

	/** static variable for table type **/
	public static final String TABLEENTITY = "TABLEENTITY";
	public static final String SPATIALRASTERENTITY = "SPATIALRASTERENTITY";
	public static final String SPATIALVECTORENTITY = "SPATIALVECTORENTITY";
	public static final String STOREDPROCEDUREENTITY = "STOREDPROCEDUREENTITY";
	public static final String VIEWENTITY = "VIEWENTITY";
	public static final String OTHERENTITY = "OTHERENTITY";

	private AttributeList attributeList = new AttributeList();
	private Boolean caseSensitive;
	private String orientation;
	private int numRecords = 0;
	private int numHeaderLines = -1;
	private int numFooterLines = -1;
	private String delimiter = null;
	private String recordDelimiter = null;
	private boolean multiple = false; // if true, multiple inputs can be mapped
										// to one table

	private String fileName; // filename where TableEntity data is stored
	private String url; // distribution url for this entity
	private String DBtableName; // the unique table name will be stored in DB
	private String compressionMethod = null;
	private boolean isImageEntity = false;
	private boolean hasGZipDataFile = false;
	private boolean hasZipDataFile = false;
	private boolean hasTarDataFile = false;
	private DataCacheObject dataCacheObject = null;
	private boolean simpleDelimited = true;
	private TextComplexDataFormat[] dataFormatArray = null;
	private String physicalLineDelimiter = null;
	private boolean collapseDelimiter = false;
	private boolean isDownloadble = true;

	/**
	 * construct this object with some extra parameters
	 * 
	 * @param name
	 *            the name of the tableEntity
	 * @param description
	 *            the description of the tableEntity
	 * @param caseSensitive
	 *            indicates whether this tableEntity is caseSensitive or not.
	 * @param orientation
	 *            indicates whether this tableEntity is column or row major
	 * @param numRecords
	 *            the number of records in this tableEntity
	 */
	public Entity(String id, String name, String description,
			Boolean caseSensitive, String orientation, int numRecords) {
		this(id, name, description, null);
		attributeList = new AttributeList();
		if (caseSensitive != null) {
			this.caseSensitive = caseSensitive;
		}
		if (orientation != null) {
			this.orientation = orientation;
		}
		this.numRecords = numRecords;
	}

	/**
	 * Construct a TableEntity, setting the list of attributes.
	 */
	public Entity(String id, String name, String description,
			AttributeList attributeList) {
		super(id, name, TABLEENTITY, description);
		// attributeList = new AttributeList();
		fileName = "";
		this.attributeList = attributeList;
		/*
		 * if (attributeList != null) { for (int i=0; i<attributeList.length;
		 * i++) { this.add(attributeList[i]); } }
		 */
		this.caseSensitive = false;
		this.orientation = "";
	}

	/**
	 * Add an Attribute to this table.
	 */
	public void add(Attribute a) {
		this.attributeList.add(a);

		// a.setParent(this);
	}

	/**
	 * Return the unit for this TableEntity
	 */
	public Attribute[] getAttributes() {
		Vector attrVector = attributeList.getAttributes();
		Attribute[] atts = new Attribute[attrVector.size()];
		return (Attribute[]) attrVector.toArray(atts);
	}

	/**
	 * indicates whether the tableEntity is caseSensitive or not
	 */
	public Boolean getCaseSensitive() {
		return caseSensitive;
	}

	/**
	 * gets the orientation of the table entity
	 */
	public String getOrientation() {
		return orientation;
	}

	/**
	 * gets the number of records in the table entity
	 */
	public int getNumRecords() {
		return numRecords;
	}

	/**
	 * sets the number of header lines in the entity
	 */
	public void setNumHeaderLines(int numHeaderLines) {
		this.numHeaderLines = numHeaderLines;
	}

	/**
	 * set the number of footer lines in the entity
	 * 
	 * @param numFooterLines
	 */
	public void setNumFooterLines(int numFooterLines) {
		this.numFooterLines = numFooterLines;
	}

	/**
	 * get the number of header lines in the entity
	 */
	public int getNumHeaderLines() {
		return this.numHeaderLines;
	}

	/**
	 * get the number of footer lines in the entity
	 * 
	 * 	 */
	public int getNumFooterLines() {
		return this.numFooterLines;
	}

	/**
	 * set the delimiter used with this entity
	 */
	public void setDelimiter(String delim) {
		this.delimiter = delim;
	}

	/**
	 * get the delimiter used with this entity
	 */
	public String getDelimiter() {
		return this.delimiter;
	}

	/**
	 * set the record delimiter used with this entity
	 */
	public void setRecordDelimiter(String delim) {
		this.recordDelimiter = delim;
	}

	/**
	 * get the recordDelimiter used with this entity
	 */
	public String getRecordDelimiter() {
		return this.recordDelimiter;
	}

	public void setURL(String url) {
		this.url = url;
	}

	public String getURL() {
		return this.url;
	}

	public void setDBTableName(String DBtableName) {
		this.DBtableName = DBtableName;
	}

	public String getDBTableName() {
		return this.DBtableName;
	}

	/**
	 * Method to get if this entity can collapse consecutive delimiter
	 * 
	 * 	 */
	public boolean getCollapseDelimiter() {
		return this.collapseDelimiter;
	}

	/**
	 * Method to set collapse delimiter
	 * 
	 * @param collapseDelimiter
	 */
	public void setCollaplseDelimiter(boolean collapseDelimiter) {
		this.collapseDelimiter = collapseDelimiter;
	}

	/*
	 * public String toString() { StringBuffer sb = new StringBuffer();
	 * sb.append("name: ").append(name).append("\n");
	 * sb.append("dataType: ").append(dataType).append("\n");
	 * sb.append("description: ").append(description).append("\n");
	 * sb.append("numRecords: ").append(numRecords).append("\n");
	 * sb.append("caseSensitive: ").append(caseSensitive).append("\n");
	 * sb.append("orientation: ").append(orientation).append("\n");
	 * sb.append("numHeaderLines: ").append(numHeaderLines).append("\n");
	 * sb.append("delimiter: ").append(delimiter).append("\n");
	 * sb.append("attributes: {"); for(int i=0; i<attributes.size(); i++) {
	 * sb.append(((Attribute)attributes.elementAt(i)).toString()); }
	 * sb.append("\n}"); return sb.toString(); }
	 */

	/**
	 * Returns the fileName.
	 * 
	 * @return String
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * Sets the fileName.
	 * 
	 * @param fileName
	 *            The fileName to set
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Serialize the data item in XML format.
	 */
	public String toXml() {
		StringBuffer x = new StringBuffer();
		x.append("<table-entity id=\"");
		x.append(getId());
		x.append("\"");
		if (multiple == true) {
			x.append(" multiple=\"true\"");
		}
		x.append(">\n");
		appendElement(x, "entityName", getName());
		appendElement(x, "entityType", getDataType());
		appendElement(x, "entityDescription", getDefinition());
		Attribute[] atts = getAttributes();
		for (int i = 0; i < atts.length; i++) {
			x.append(atts[i].toXml());
		}
		x.append("</table-entity>\n");

		return x.toString();
	}

	/**
	 * Sets the multiple to true.
	 */
	public void setMultiple() {
		this.multiple = true;
	}

	/**
	 * Returns multiple.
	 * 
	 * @return boolean
	 */
	public boolean isMultiple() {
		return multiple;
	}

	// -----------------------------------------------------------------
	// -- DSTableIFace
	// -----------------------------------------------------------------

	/**
	 * Returns the name of the table
	 * 
	 * @return name as string
	 */
	// This is imlemented in the base class
	// public String getName();
	/**
	 * Return the name for this data item.
	 */
	public String getMappedName() {
		return this.DBtableName;
	}

	/**
	 * Returns a Vector of the fields in the table
	 * 
	 * @return vector
	 */
	public Vector getFields() {
		return attributeList.getAttributes();
	}

	/**
	 * Returns a the Primary Key Definition for the table
	 * 
	 * @return object
	 */
	public DSTableKeyIFace getPrimaryKey() {
		return null;
	}

	/**
	 * Method to get compression method for distribution file
	 * 
	 * @return String
	 */
	public String getCompressionMethod() {
		return this.compressionMethod;
	}

	/**
	 * Method to set compression method for distribution file
	 * 
	 * @param compressionMethod
	 *            String
	 */
	public void setCompressionMethod(String compressionMethod) {
		this.compressionMethod = compressionMethod;
	}

	/**
	 * If this entity for SpatialRaster or SpatialVector
	 * 
	 * @return boolean
	 */
	public boolean getIsImageEntity() {
		return this.isImageEntity;
	}

	/**
	 * Set if this is a Image entity
	 * 
	 * @param isImageEntity
	 *            boolean
	 */
	public void setIsImageEntity(boolean isImageEntity) {
		this.isImageEntity = isImageEntity;
	}

	/**
	 * Method get if the data file is zip file
	 * 
	 * @return boolean
	 */
	public boolean getHasZipDataFile() {
		return this.hasZipDataFile;
	}

	/**
	 * Method to set if the data file is zip file
	 * 
	 * @param isZipDataFile
	 *            boolean
	 */
	public void setHasZipDataFile(boolean isZipDataFile) {
		this.hasZipDataFile = isZipDataFile;
	}

	/**
	 * Method to get if the data file is gzip
	 * 
	 * @return boolean
	 */
	public boolean getHasGZipDataFile() {
		return this.hasGZipDataFile;
	}

	/**
	 * Method to set if the data file is gzip
	 * 
	 * @param hasGZipDataFile
	 *            boolean
	 */
	public void setHasGZipDataFile(boolean hasGZipDataFile) {
		this.hasGZipDataFile = hasGZipDataFile;
	}

	/**
	 * Method to get if this has a tar data file
	 * 
	 * @return boolean
	 */
	public boolean getHasTarDataFile() {
		return this.hasTarDataFile;
	}

	/**
	 * Method to set if this has a tar data file
	 * 
	 * @param hasTarDataFile
	 *            boolean
	 */
	public void setHasTarDataFile(boolean hasTarDataFile) {
		this.hasTarDataFile = hasTarDataFile;
	}

	/**
	 * Method to get data cache item associated with this entity
	 * 
	 * @return DataCacheObject
	 */
	public DataCacheObject getDataCacheObject() {
		return this.dataCacheObject;
	}

	/**
	 * Method to set a cache data item associate with this entity
	 * 
	 * @param dataCacheObject
	 *            DataCachObject
	 */
	public void setDataCacheObject(DataCacheObject dataCacheObject) {
		this.dataCacheObject = dataCacheObject;
	}

	/**
	 * If data file in this entity is simple delimited
	 * 
	 * @return Returns the simpleDelimited.
	 */
	public boolean isSimpleDelimited() {
		return simpleDelimited;
	}

	/**
	 * @param simpleDelimited
	 *            The simpleDelimited to set.
	 */
	public void setSimpleDelimited(boolean simpleDelimited) {
		this.simpleDelimited = simpleDelimited;
	}

	/**
	 * Get the complex data format array
	 * 
	 * @return Returns the dataFormatArray.
	 */
	public TextComplexDataFormat[] getDataFormatArray() {
		return dataFormatArray;
	}

	/**
	 * Set DataFormatArray
	 * 
	 * @param dataFormatArray
	 *            The dataFormatArray to set.
	 */
	public void setDataFormatArray(TextComplexDataFormat[] dataFormatArray) {
		this.dataFormatArray = dataFormatArray;
	}

	/**
	 * @return Returns the physicalLineDelimiter.
	 */
	public String getPhysicalLineDelimiter() {
		return physicalLineDelimiter;
	}

	/**
	 * @param physicalLineDelimiter
	 *            The physicalLineDelimiter to set.
	 */
	public void setPhysicalLineDelimiter(String physicalLineDelimiter) {
		this.physicalLineDelimiter = physicalLineDelimiter;
	}

	/**
	 * Method to set attribute list
	 * 
	 * @param list
	 */
	public void setAttributeList(AttributeList list) {
		this.attributeList = list;
	}

	/**
	 * Return this entity is downloadable or not. If the value of function
	 * attribute in url is "information", this entity wouldn't be downloaded
	 * 
	 * 	 */
	public boolean isDownloadable() {
		return isDownloadble;
	}

	/**
	 *Set this entity downloadable or not. f the value of function attribute in
	 * url is "information", this entity wouldn't be downloaded
	 * 
	 * @param isDownloadable
	 */
	public void setDownloadable(boolean isDownloadable) {
		this.isDownloadble = isDownloadable;
	}

}