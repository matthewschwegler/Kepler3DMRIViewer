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

package org.ecoinformatics.seek.ecogrid.quicksearch;

import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ecoinformatics.seek.ecogrid.MetadataSpecificationInterface;
import org.ecoinformatics.seek.ecogrid.exception.NULLEcogridConfigurationException;
import org.ecoinformatics.seek.ecogrid.exception.NULLSearchNamespaceException;
import org.ecoinformatics.seek.ecogrid.exception.NoMetadataSpecificationClassException;
import org.ecoinformatics.seek.ecogrid.exception.NoSearchEndPointException;

import util.StaticUtil;

/**
 * <p>
 * Title: SearchScope
 * </p>
 * .
 * <p>
 * Description: This class represents a search scope: namespace - the
 * documents's namespace need to be searched metadataSpecificationClass - the
 * metadata class correspond to the namespace endPoints - the url list which
 * have those documents
 * </p>
 * <p>
 * Copyright: Copyright (c) 2004
 * </p>
 * <p>
 * Company: UCSB
 * </p>
 * 
 * @author tao
 * @version 1.0
 */

class SearchScope {
	private String namespace = null;
	private String metadataSpecificationClassName = null;
	private MetadataSpecificationInterface metadataSpecification = null;
	private Vector endPointsVector = null;

	protected final static Log log;
	static {
		log = LogFactory.getLog("org.ecoinformatics.seek.ecogrid.SearchScope");
	}

	/**
	 * Constructor
	 * 
	 * @param nameSpace
	 *            String the namesapce need to be searched
	 * @param metadataSpecificationClassName
	 *            String the metadata specification class name for the namespace
	 * @param endPoints
	 *            Vector url need to go searching
	 * @throws NULLSearchNamespaceException
	 * @throws NoMetadataSpecificationClassException
	 */
	public SearchScope(String nameSpace, String metadataSpecificationClassName,
			Vector endPoints) throws NULLSearchNamespaceException,
			NULLEcogridConfigurationException,
			NoMetadataSpecificationClassException, NoSearchEndPointException {
		if (nameSpace == null || nameSpace.equals("")) {
			throw new NULLSearchNamespaceException("Search namespace is null");
		}
		this.namespace = nameSpace;
		log.debug("The namespace in SearchScope is " + namespace);
		if (metadataSpecificationClassName == null
				|| metadataSpecificationClassName.trim().equals("")) {
			throw new NoMetadataSpecificationClassException("No "
					+ "metadataSpecificationClassName is given");
		}
		this.metadataSpecificationClassName = metadataSpecificationClassName;
		metadataSpecification = generateMetadataSpecification();

		if (endPoints == null || endPoints.isEmpty()) {
			throw new NoSearchEndPointException("No end points in Search Scope");
		}
		endPointsVector = endPoints;
	}// SearchScope

	/*
	 * This method will create a concrete class object for
	 * MetadataSpecificationInterface base on the given class name
	 */
	private MetadataSpecificationInterface generateMetadataSpecification()
			throws NoMetadataSpecificationClassException {

		MetadataSpecificationInterface metadataSpec = null;
		try {
			metadataSpec = (MetadataSpecificationInterface) StaticUtil
					.createObject(metadataSpecificationClassName);
		} catch (Exception ee) {
			throw new NoMetadataSpecificationClassException("Couldn't get the "
					+ "object for this class " + metadataSpecificationClassName);
		}
		return metadataSpec;
	}

	/**
	 * Method to get namespace
	 * 
	 * @return String the namespace
	 */
	public String getNamespace() {
		return namespace;
	}

	/**
	 * Method to get metadata specification class
	 * 
	 * @return MetadataSpecificationInterface
	 */
	public MetadataSpecificationInterface getMetadataSpecification() {
		return this.metadataSpecification;
	}

	/**
	 * Method go get endponts (url)
	 * 
	 * @return Vector
	 */
	public Vector getEndPoints() {
		return endPointsVector;
	}

	/**
	 * Method to add an endpoint to vector(if endpoint is already there, it will
	 * be ignored
	 * 
	 * @param String
	 *            endPoint the end point will be added
	 */
	public void addSearchEndPoint(String endPoint) {
		if (endPointsVector != null) {
			if (!endPointsVector.contains(endPoint)) {
				endPointsVector.add(endPoint);
			}
		}
	}// addSearchEndPoint

}// SearchScope