/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
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

package org.ecoinformatics.seek.ecogrid;

import java.io.IOException;
import java.util.Vector;

import org.ecoinformatics.ecogrid.queryservice.query.QueryType;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetType;
import org.ecoinformatics.seek.ecogrid.exception.EcoGridException;
import org.ecoinformatics.seek.ecogrid.exception.InvalidEcogridQueryException;
import org.xml.sax.SAXException;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * <p>
 * 
 * Title:PlugInQueryTransferInterface
 * </p>
 * <p>
 * 
 * Description: This is a plugin interface and it defines a all methods any
 * metadata type should implement into order to search ecogrid service and parse
 * the results
 * </p>
 * <p>
 * 
 * Copyright: Copyright (c) 2004
 * </p>
 * <p>
 * 
 * Company:
 * </p>
 * 
 *@author not attributable
 *@created February 17, 2005
 *@version 1.0
 */

public abstract class MetadataSpecificationInterface {
	// Constant
	/**
	 * Description of the Field
	 */
	public final static String RETURNFIELD = "returnField";
	/**
	 * Description of the Field
	 */
	public final static String RETURNFIELDTYPE = "type";
	/**
	 * Description of the Field
	 */
	public final static String RETURNFIELDTITLE = "title";
	/**
	 * Description of the Field
	 */
	public final static String RETURNFIELDENTITY = "entityName";
	/**
	 * Description of the Field
	 */
	public final static String NAMESPACE = "namespace";
	/**
	 * Description of the Field
	 */
	public final static String QUICKPATH = "quickSearchPath";
	/**
	 * Description of the Field
	 */
	public final static String QUERYTITLE = "queryTitle";
	/**
	 * Description of the Field
	 */
	public final static String ENDPOINT = "endPoint";
	/**
	 * Description of the Field
	 */
	public final static String METADATACLASS = "metadataSpecificationClass";
	/**
	 * Description of the Field
	 */
	public final static String ECOGRIDPATH = "ecogridService";
	/**
	 * Description of the Field
	 */
	public final static String SERVICESLIST = "servicesList";
	/**
	 * Description of the Field
	 */
	public final static String SERVICE = "service";
	/**
	 * Description of the Field
	 */
	public final static String SERVICENAME = "serviceName";
	/**
	 * Description of the Field
	 */
	public final static String SERVICETYPE = "serviceType";
	/**
	 * Description of the Field
	 */
	public final static String DOCUMENTTYPE = "documentType";
	/**
	 * Description of the Field
	 */
	public final static String LABEL = "label";
	/**
	 * Description of the Field
	 */
	public final static String RETURNFIELDTYPELIST = "returnFieldTypeList";
	/**
	 * Description of the Field
	 */
	public final static String METADATASPECIFICATIONCLASSLIST = "metadataSpecificationClassList";
	/**
	 * Description of the field
	 */
	public final static String SELECTION = "selected";

	/**
	 * field for service clustering/authentication
	 */
	public final static String SERVICEGROUP = "serviceGroup";
	public final static String SERVICECLASSIFICATION = "serviceClassification";
	public final static String DESCRIPTION = "description";

	public final static String AUTHENTICATION_MAPPING = "authenticationMapping";
	public final static String AUTHENTICATION_SERVICETYPE = "serviceType";
	public final static String AUTHENTICATION_SERVICECLASS = "serviceClass";

	/**
	 * This method will return a quick search query base on the given value
	 * 
	 *@param value
	 *            String
	 *@return QueryType
	 *@exception InvalidEcogridQueryException
	 *                Description of the Exception
	 */
	public abstract QueryType getQuickSearchEcoGridQuery(String value)
			throws InvalidEcogridQueryException;

	/**
	 * This method will transfer a query group into ecogrid query base on
	 * different namespace
	 * 
	 *@return QueryType
	 */
	public abstract QueryType getEcoGridQuery();

	/**
	 * The ResultSet is added directly to the container
	 * 
	 *@param results
	 *@param endpoint
	 *@param container
	 *@param aResultList
	 *            The feature to be added to the ResultsetRecordsToContainer
	 *            attribute
	 *	 *@throws SAXException
	 *@throws IOException
	 *@throws EcoGridException
	 *@throws NameDuplicationException
	 *@throws IllegalActionException
	 */
	public abstract boolean addResultsetRecordsToContainer(
			ResultsetType results, String endpoint, CompositeEntity container,
			Vector aResultList) throws SAXException, IOException,
			EcoGridException, NameDuplicationException, IllegalActionException;

	/**
	 *@return Returns a unique name that descrobes this class, often it is the
	 *         name of the class that implments the interface
	 */
	public abstract String getName();

	/**
	 *@return Returns the Data Source Type Name
	 */
	public abstract String getBriefName();

	/**
	 *@return returns the number of results that for this data.
	 */
	public abstract int getNumResults();

	/* A a method will replace a "." by "-" in a string */
	public static String replaceDotByDash(String originalString) {
		String withoutDotString = null;
		if (originalString == null) {
			return withoutDotString;
		}

		if (originalString.indexOf(".") != -1) {
			withoutDotString = originalString.replace('.', '-');

		} else {
			withoutDotString = originalString;
		}
		// debugger.print("The string without dot is " + withoutDotString, 2);
		return withoutDotString;
	}

}