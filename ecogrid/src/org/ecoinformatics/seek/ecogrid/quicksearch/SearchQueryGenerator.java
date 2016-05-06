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

import java.io.StringReader;
import java.util.Hashtable;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.axis.types.URI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ecoinformatics.ecogrid.queryservice.query.QueryType;
import org.ecoinformatics.ecogrid.queryservice.util.EcogridQueryParser;
import org.ecoinformatics.seek.ecogrid.exception.InvalidEcogridQueryException;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;

/**
 * This class will read kepler configure file and get the query part from it
 * base on search name space.
 * 
 * @author Jing Tao
 * 
 */

public class SearchQueryGenerator {
	private String _queryId = null;
	private Hashtable _replacementMap = null;
	private QueryType _query = new QueryType();

	private final static String QUERYPATH = "//ecogridService/queryList/query[@queryId='";
	private final static String CONDITION = "condition";

	protected final static Log log;
	static {
		log = LogFactory
				.getLog("org.ecoinformatics.seek.ecogrid.SearchQueryGenerator");
	}

	/**
	 * Constructor of SearchQueryGenerator
	 * 
	 * @param queryId
	 *            String the queryId which will be found in config
	 * @param replacementMap
	 *            Hashtable the hash table which contain the key - be replaced
	 *            value value - replacement For example, if key is "#value#",
	 *            and value is "soil". This means any element and attribute in
	 *            xml has value "#value#" will be replaced by soil
	 */
	public SearchQueryGenerator(String queryId, Hashtable replacementMap)
			throws InvalidEcogridQueryException

	{
		_queryId = queryId;
		_replacementMap = replacementMap;
		try {
			generateQuery();
		} catch (Exception e) {
      e.printStackTrace();
			throw new InvalidEcogridQueryException(e.getMessage());
		}
	}// SearchQueryGenerator

	/**
	 * Method to get query which generate by this class
	 * 
	 * @return QueryType
	 */
	public QueryType getQuery() {
		return _query;
	}// getQuery

	/**
	 * Recursively walks the tree looking for Condition values inorder to
	 * subsitute in the search value
	 * 
	 * @param aNode
	 *            the parent node
	 * @param aIsChildCond
	 *            indicates whether the current parent node is a Condition node
	 */
	private void mapInValue(SearchQuery searchQuery, Hashtable aMap) 
  {
    searchQuery.replaceValues(aMap);
    
		/*NodeList childList = aNode.getChildNodes();
		if (childList == null) 
    {
			return;
		}

		// go through every child element
		int length = childList.getLength();
		for (int i = 0; i < length; i++) 
    {
			Node kid = childList.item(i);
      System.out.println("kid name: " + kid.getNodeName());
			if (kid.getNodeName().equals(CONDITION) || aIsChildCond) 
      {
				String value = kid.getNodeValue();
				// replace the value by search value if this value in
				// replacementMap
				if (value != null && aMap.containsKey(value)) 
        {  
          System.out.println("replacing " + value + " with " + aMap.get(value));
					log.debug("Replacing [" + value + "] with ["
							+ aMap.get(value) + "]");
					kid.setNodeValue((String) aMap.get(value));
				} 
        else 
        {
					mapInValue(kid, true, aMap);
				}
			} 
      else 
      {
				mapInValue(kid, false, aMap);
			}
		}*/
	}

	/*
	 * Method to read config file and generate a query(It will chose the first
	 * one if it has more than one in configure file)
	 */
	private void generateQuery() throws URI.MalformedURIException,
			TransformerException, InvalidEcogridQueryException {
		String xpath = QUERYPATH + _queryId + "']";
    
    ConfigurationProperty ecogridProperty = ConfigurationManager.getInstance()
      .getProperty(ConfigurationManager.getModule("ecogrid"));
    ConfigurationProperty queryPathProp = ecogridProperty.getProperty("queryList");
    List queryList = queryPathProp.findProperties("queryId", _queryId, true);
    if(queryList == null || queryList.size() == 0)
    {
      return;
    }
    
    ConfigurationProperty queryProp = (ConfigurationProperty)queryList.get(0);
    
    SearchQuery searchQuery = new SearchQuery(queryProp);
    
    mapInValue(searchQuery, _replacementMap);

		try {
			EcogridQueryParser queryParser = new EcogridQueryParser(new StringReader(searchQuery.toString()));
			queryParser.parseXML();
			_query = queryParser.getEcogridQuery();
		} catch (Exception e) {
      System.out.println("Error parsing query: " + e.getMessage());
      e.printStackTrace();
			log.error("Exception", e);
		}

	} // generateQuery

} // SearchQueryGenerator