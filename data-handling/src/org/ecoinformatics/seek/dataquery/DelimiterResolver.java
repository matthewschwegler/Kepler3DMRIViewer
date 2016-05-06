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

package org.ecoinformatics.seek.dataquery;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;

/**
 * This class will map delimter format in metadata (e.g eml documents) to the
 * format in db (e.g, hsql).
 * 
 * @author Jing Tao
 * 
 */

public class DelimiterResolver {
	
  private ConfigurationProperty sqlEngineProperty = null;
	private String originalString;
	private String replaceMent;
	private static final String DELIMITERPARENTPATH = "//sqlEngine[sqlEngineName=\"hsql\"]/delimiterMapping/mapping";
	private static final String DELIMITERPREFIXPARENTPATH = "//sqlEngine[sqlEngineName=\"hsql\"]/delimiterMapping/prefixmapping";
	private static final String METADATADELIMITERPATH = "./metadataDelimiter";
	private static final String DBDELIMITERPATH = "./DBdelimiter";

	private static Log log;
	private static boolean isDebugging;

	static {
		log = LogFactory.getLog("org.ecoinformatics.seek.dataquery");
		isDebugging = log.isDebugEnabled();
	}

	/**
	 * Constructor. It will read config file and find the sepciy delimiter
	 * mapping then put them into a hash table. metadata delimiter is a key and
	 * db delimter is a value. For example, ; maps \semi. It also will read
	 * prefix mapping and metadata delimiter is a key and db delimiter is a
	 * value. For example 0x maps \\u
	 */
	public DelimiterResolver() {

    ConfigurationManager confMan = ConfigurationManager.getInstance();
    ConfigurationProperty commonProperty = confMan.getProperty(ConfigurationManager.getModule("common"));
    sqlEngineProperty = (ConfigurationProperty)commonProperty
      .findProperties("sqlEngineName", "hsql", true).get(0);
    
	}// DelimiterResolver

	/**
	 * This method will figure out a db delimiter when given a metadata
	 * delimiter. If metadataDelimiter is null, null will be returned. Here is
	 * way: 1. If the given metadataDelimiter is in the special hash (read from
	 * config file), the mapping will return from the hash. 2. If the given
	 * metadataDelimter is start with a prefix mapping, the mapping value will
	 * replaced by mapping key. for example a hexademical number start with
	 * "0x", (e.g. 0x##), the mapping to "0x" is "\\u", so "\\u##" will be
	 * returned. 3. The others will return itself(the given metadataDelimiter)
	 * 
	 * @param metadataDelimiter
	 *            String
	 * @return String
	 */
	public String resolve(String metadataDelimiter) {
		String dbDelimiter = null;
		if (metadataDelimiter == null) {
			if (isDebugging) {
				log.debug("The dbDelimiter is " + dbDelimiter);
			}
			return dbDelimiter;
		}

		if (sqlEngineProperty != null) 
    {
			//dbDelimiter = (String) specialMapping.get(metadataDelimiter);
      if(metadataDelimiter.equals("\t"))
      {
        metadataDelimiter = "tab";
      }
      else if(metadataDelimiter.equals(" "))
      {
        metadataDelimiter = "space";
      }
      
      List l = sqlEngineProperty.findProperties("metadataDelimiter", metadataDelimiter, true);
      ConfigurationProperty mappingProperty;
      if(l.size() > 0)
      {
        mappingProperty = (ConfigurationProperty)l.get(0);
        dbDelimiter = mappingProperty.getProperty("DBdelimiter").getValue();
      }
      else
      {
        dbDelimiter = metadataDelimiter;
      }
      
		} else if (startWithPrefix(metadataDelimiter)) {
			if (replaceMent != null && originalString != null) {
				dbDelimiter = replaceMent
						+ metadataDelimiter.substring(originalString.length());
			} else {
				throw new RuntimeException(
						"The mapping values for delimiter prefix is null");
			}
		} else {
			dbDelimiter = metadataDelimiter;
		}
		if (isDebugging) {
			log.debug("The dbDelimiter is " + dbDelimiter);
		}
		return dbDelimiter;
	}// resolve

	/*
	 * For a given delimiter, this method will see if it start with a prefix
	 * replacement
	 */
	private boolean startWithPrefix(String givenDelimiter) {
		boolean inHash = false;
		if (sqlEngineProperty != null && givenDelimiter != null) {
			//Iterator enm = specialPrefixMapping.keySet().iterator();
      List prefixes = sqlEngineProperty.getProperties("delimiterMapping.prefixmapping"); 
			// go through the hash table to if the givenDelimiter start with a
			// prefix
      Iterator enm = prefixes.iterator();
			while (enm.hasNext()) {
				//String prefix = (String) enm.next();
        ConfigurationProperty prefixMapping = (ConfigurationProperty)enm.next();
        String prefix = prefixMapping.getProperty("metadataDelimiter").getValue();
				if (prefix != null && givenDelimiter.startsWith(prefix)) {
					inHash = true;
					originalString = prefix;
					//replaceMent = (String) specialPrefixMapping.get(prefix);
          replaceMent = prefixMapping.getProperty("DBdelimiter").getValue();
					break;
				}
			}
		}
		return inHash;
	}// startWithPrefix

}