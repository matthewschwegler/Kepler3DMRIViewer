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

import java.util.List;

import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.objectmanager.data.UnresolvableTypeException;

/**
 * This class will resolve a data type in HSQL for a given data type in
 * Attribute object
 * 
 * @author Jing Tao
 * 
 */

public class DBDataTypeResolver {

  private ConfigurationProperty sqlEngineProperty = null;
	private static final String DBDEFAULTYPEPATH = "//sqlEngine[sqlEngineName=\"hsql\"]/DBdataTypeMapping/defaultDBdataType";
	private static final String JAVADEFAULTPATH = "//sqlEngine[sqlEngineName=\"hsql\"]/DBdataTypeMapping/defaultJavaDataType";
	private static final String DATATYPEMAPPARENTPATH = "//sqlEngine[sqlEngineName=\"hsql\"]/DBdataTypeMapping/mapping";
	private static final String METADATATYPEPATH = "./metadataDataType";
	private static final String DBDATATYPEPATH = "./DBdataType";
	private static final String JAVADATATYPEPATH = "./javaDataType";

  private static String DEFAULTTYPE;
  private static String JAVADEFAULT;

	/**
	 * Constructor get two mapping from lib/config.xml file. Mapping is hash
	 * table, key is attribute data type and value is db data type Another
	 * mapping is : key - attribute data type, value - java data type
	 */
	public DBDataTypeResolver() {
			
      ConfigurationManager confMan = ConfigurationManager.getInstance();
      ConfigurationProperty commonProperty = confMan.getProperty(ConfigurationManager.getModule("common"));
      sqlEngineProperty = (ConfigurationProperty)commonProperty
        .findProperties("sqlEngineName", "hsql", true).get(0);
      DEFAULTTYPE = sqlEngineProperty.getProperty("DBdataTypeMapping").getProperty("defaultDBdataType").getValue();
      JAVADEFAULT = sqlEngineProperty.getProperty("DBdataTypeMapping").getProperty("defaultJavaDataType").getValue();
	}// DBDataTypeResolver

	/**
	 * Method to find mapping db data type to a given attribute data type
	 * 
	 * @param attributeType
	 *            String
	 * @throws UnresolvableTypeException
	 * @return String
	 */
	public String resolveDBType(String attributeType)
			throws UnresolvableTypeException {
		String dbType = null;
		if (attributeType == null || attributeType.trim().equals("")) {
			dbType = DEFAULTTYPE;
		} else {
      ConfigurationProperty dbDatatypeMappingProp = sqlEngineProperty.getProperty("DBdataTypeMapping");
      List mappingsList = dbDatatypeMappingProp.findProperties("metadataDataType", attributeType, true);
      ConfigurationProperty mappingProp = (ConfigurationProperty)mappingsList.get(0);
      dbType = mappingProp.getProperty("DBdataType").getValue();
		}

		if (dbType == null) {
			throw new UnresolvableTypeException(
					"Unable to resolve db data type "
							+ "for attribute data type " + attributeType);
		}
		return dbType;
	}// resolveDBType

	/**
	 * Method to find a mapping between java type and a given metadata data type
	 * 
	 * @param attributeType
	 *            String
	 * @throws UnresolvableTypeException
	 * @return String
	 */
	public String resolveJavaType(String attributeType)
			throws UnresolvableTypeException {
		String javaType = null;
		if (attributeType == null || attributeType.trim().equals("")) {
			javaType = JAVADEFAULT;
		} else {
      ConfigurationProperty dbDatatypeMappingProp = sqlEngineProperty.getProperty("DBdataTypeMapping");
      List mappingsList = dbDatatypeMappingProp.findProperties("metadataDataType", attributeType, true);
      ConfigurationProperty mappingProp = (ConfigurationProperty)mappingsList.get(0);
      javaType = mappingProp.getProperty("javaDataType").getValue();
		}

		if (javaType == null) {
			throw new UnresolvableTypeException(
					"Unable to resolve java data type "
							+ "for attribute data type " + attributeType);
		}
		return javaType;

	}// resolveJavaType

}// DBDataTypeResolver