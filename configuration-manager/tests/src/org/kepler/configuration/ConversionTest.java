/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: welker $'
 * '$Date: 2010-12-07 15:39:30 -0800 (Tue, 07 Dec 2010) $' 
 * '$Revision: 26437 $'
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

package org.kepler.configuration;

import java.util.Iterator;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.kepler.build.modules.Module;

/**
 * Tests for the conversion from configxml to the configuration manager
 * 
 * @author Chad Berkley
 */
public class ConversionTest extends TestCase {
	ConfigurationManager config;
  
	public ConversionTest(String name) 
  {
		super(name);
    try
    {
		  config = ConfigurationManager.getInstance();
    }
    catch(Exception e)
    {
      e.printStackTrace();
      fail("could not instantiate the configurationManager: " + e.getMessage());
    }
	}

	/**
	 * Establish a testing framework by initializing appropriate objects
	 */
	public void setUp() 
  {
	}

	/**
	 * Release any objects after tests are complete
	 */
	public void tearDown() 
  {
	}

	/**
	 * Create a suite of tests to be run together
	 */
	public static Test suite() 
  {
		TestSuite suite = new TestSuite();
		// note that the order of these tests DOES matter. don't change the
		// order!
		suite.addTest(new ConversionTest("initialize"));
    suite.addTest(new ConversionTest("EML2MetadataSpecificationTest"));
    suite.addTest(new ConversionTest("ADNMetadataSpecificationTest"));
    suite.addTest(new ConversionTest("DarwinCoreSchemaTest"));
    suite.addTest(new ConversionTest("testFilterUI"));
    suite.addTest(new ConversionTest("testGEONDatabaseResource"));
    suite.addTest(new ConversionTest("testDataTypeResolver"));
    suite.addTest(new ConversionTest("testAuthNamespace"));
    suite.addTest(new ConversionTest("testDBDataTypeResolver"));
    suite.addTest(new ConversionTest("testDBTablesGenerator"));
    suite.addTest(new ConversionTest("testDelimiterResolver"));
    suite.addTest(new ConversionTest("testEcogridDataCacheItem"));
    suite.addTest(new ConversionTest("testDocumentType"));
    suite.addTest(new ConversionTest("testEcoGridServicesController"));
    suite.addTest(new ConversionTest("testSearchRegistryAction"));
    suite.addTest(new ConversionTest("testLDAPLoginGUI"));
    suite.addTest(new ConversionTest("testKeplerApplication"));
    suite.addTest(new ConversionTest("testStaticUtil"));
    suite.addTest(new ConversionTest("testDBConnectionFactory"));
    //this test only works if you're in the WRP suite
    //suite.addTest(new ConversionTest("testReportingInstanceKAREntryHandler"));
    suite.addTest(new ConversionTest("testDomainList"));
    suite.addTest(new ConversionTest("testGenericJobLauncher"));
    suite.addTest(new ConversionTest("testRepositoryManager"));
    suite.addTest(new ConversionTest("testKeplerContextMenuFactory"));
    suite.addTest(new ConversionTest("testMenuMapper"));
    //this test requires static resources which normally can't be imported from this module
    //suite.addTest(new ConversionTest("testStaticResources"));
    
    //suite.addTest(new ConversionTest(""));
    //suite.addTest(new ConversionTest(""));
    
    /*
     * There should be a test here for ecogrid/src/org/ecoinformatics/seek/ecogrid/quicksearch/SearchQueryGenerator.java
     * but since there was already a test for that class in the ecogrid module, 
     * I added the test there instead.
     */
    return suite;
  }
  
  public void initialize()
  {
    assertTrue(1 == 1);
  }
  
  /**
   * many lines in util/src/org/kepler/util/StaticResources.java
   *
  public void testStaticResources()
  {
    boolean b = StaticResources.getBoolean("KEPLER_MENUS", false);
    assertTrue(b);
    String s = StaticResources.getDisplayString("general.HELP", "x");
    assertTrue(s.equals("Help"));
    s = StaticResources.getSettingsString("dialogs.defaultHelpURL", "y");
    assertTrue(s.equals("ptolemy/configs/doc/basicHelp.htm"));
    int i = StaticResources.getSize("dialogs.labels.id.width", 0);
    assertTrue(i == 60);
    
  }*/
  
  /**
   * line 382 of gui/src/org/kepler/gui/MenuMapper.java
   */
  public void testMenuMapper()
  {
    Iterator it;
    ConfigurationProperty prop = ConfigurationManager.getInstance()
        .getProperty(ConfigurationManager.getModule("gui"), 
        new ConfigurationNamespace("uiMenuMappings"));
    List reposList = prop.getProperties("name", true);
    it = reposList.iterator();
    
    ConfigurationProperty cp = (ConfigurationProperty)it.next();
    String nextKey = cp.getValue();
    String nextVal = cp.getParent().getProperty("value").getValue();
    //System.out.println("nextKey: " + nextKey);
    //System.out.println("nextVal: " + nextVal);
    assertTrue(nextKey.equals("~File->Open ~File..."));
    assertTrue(nextVal.equals("File->Open File"));
  }
  
  /**
   * line 161 in gui/src/org/kepler/gui/KeplerContextMenuFactory.java
   */
  public void testKeplerContextMenuFactory()
  {
    Iterator it;
    
    List l = ConfigurationManager.getInstance().getProperties();
    //ConfigurationProperty.simplePrintList(l);
    
    ConfigurationProperty prop = ConfigurationManager.getInstance()
        .getProperty(ConfigurationManager.getModule("gui"), 
        new ConfigurationNamespace("uiContextMenuMappings"));
    //prop.prettyPrint();
    List reposList = prop.getProperties("name", true);
    //it = getContextMenuMappingsResBundle().getKeys();
    it = reposList.iterator();

    /*while (it.hasNext()) {
      //String nextKey = (String) (it.next());
      ConfigurationProperty cp = (ConfigurationProperty)it.next();
      String nextKey = cp.getValue();
      String nextVal = cp.getParent().getProperty("value").getValue();
      //System.out.println("key: " + nextKey);
      //System.out.println("val: " + nextVal);
    }*/
    
    ConfigurationProperty cp = (ConfigurationProperty)it.next();
    String nextKey = cp.getValue();
    String nextVal = cp.getParent().getProperty("value").getValue();
    assertTrue(nextKey.equals("DIRECTOR->Configure Director"));
    assertTrue(nextVal.equals("DIRECTOR->Configure"));
  }
  
  /**
   * line 119 in repository/src/org/kepler/objectmanager/repository/RepositoryManager.java
   */
  public void testRepositoryManager()
  {
    ConfigurationProperty prop = ConfigurationManager.getInstance()
      .getProperty(ConfigurationManager.getModule("repository"));
		List reposList = prop.getProperties("repository");
    //for(int i=0; i<reposList.size(); i++)
    int i = 0;
    {
      ConfigurationProperty cp = (ConfigurationProperty)reposList.get(i);
			String name = cp.getProperty("name").getValue();
			String repository = cp.getProperty("repository").getValue();
			String putPath = cp.getProperty("putpath").getValue();
			String authProtocol = cp.getProperty("authprotocol").getValue();
			String authDomain = cp.getProperty("authdomain").getValue();
			String lsidPath = cp.getProperty("lsidpath").getValue();
			String queryPath = cp.getProperty("querypath").getValue();
			String registry = cp.getProperty("registrypath").getValue();
			String registryauth = cp.getProperty("registryauth").getValue();
			String username = cp.getProperty("username").getValue();
			String password = cp.getProperty("password").getValue();
			String repClass = cp.getProperty("class").getValue();
			String lsidAuthority = cp.getProperty("lsidAuthority").getValue();
      
      assertTrue(name.equals("localRepository"));
      assertTrue(lsidAuthority.equals("kepler-project.org"));
    }
    assertTrue(reposList.size() == 4);
  }
  
  /**
   * line 492 in actors/src/org/kepler/actor/job/GenericJobLauncher.java
   */
  public void testGenericJobLauncher()
  {
    String strScheduler = "condor";
    
    ConfigurationProperty cp = ConfigurationManager.getInstance().
        getProperty(ConfigurationManager.getModule("actors"), 
          new ConfigurationNamespace("JobLauncher"));
    ConfigurationProperty prop = cp.findProperties("name", strScheduler.toLowerCase(), true).get(0);
    String jobsSupported = prop.getProperty("value").getValue();
    
    assertTrue(jobsSupported.equals("Condor"));
  }
  
  /**
   * line 69 in authentication/src/org/kepler/authentication/DomainList.java
   */
  public void testDomainList()
  {
    List propList = ConfigurationManager.getInstance().getProperties(
      ConfigurationManager.getModule("authentication"), "config.service");
    /*for(int i=0; i<propList.size(); i++)
    {
      ConfigurationProperty serviceProp = (ConfigurationProperty)propList.get(i);
      Domain d = new Domain();
      d.setDomain(serviceProp.getProperty("domain").getValue());
      d.setServiceOperation(serviceProp.getPoperty("serviceOperation").getValue());
      d.setServiceURL(serviceProp.getProperty("serviceURL").getValue());
      d.setServiceClass(serviceProp.getProperty("serviceClass").getValue());
      domainVector.add(d);
      
    }*/
    
    //ConfigurationProperty.simplePrintList(propList);
    /*ConfigurationProperty serviceProp = (ConfigurationProperty)propList.get(1);
    assertTrue(propList.size() == 3);
    assertTrue(serviceProp.getProperty("domain").getValue().equals("SEEK"));
    assertTrue(serviceProp.getProperty("serviceOperation").getValue().equals("ldap"));
    assertTrue(serviceProp.getProperty("serviceURL").getValue().equals("http://library.kepler-project.org/kepler/services/AuthenticationService"));
    assertTrue(serviceProp.getProperty("serviceClass").getValue().equals("org.kepler.authentication.LDAPAuthenticationService"));*/
  }
  
  /**
   * in the wrp suite only.
   * line 252 in reporting/src/org/kepler/kar/handlers/ReportInstanceKAREntryHandler.java
   */
  public void testReportingInstanceKAREntryHandler()
  {
    ConfigurationProperty wrpProperty = ConfigurationManager.getInstance()
      .getProperty(ConfigurationManager.getModule("wrp"));
    Iterator reportingPropertyIt = wrpProperty.getProperties("reporting.reportMapping").iterator();
    
		//while (reportingPropertyIt.hasNext()) 
    {
      ConfigurationProperty reportingProp = (ConfigurationProperty)reportingPropertyIt.next();
			String itemName = reportingProp.getProperty("itemName").getValue(); //(String) mappingIter.next();
			String attrName = reportingProp.getProperty("metadataName").getValue(); //(String) reportMapping.get(itemName);
      
      assertTrue(itemName.equals("status.input"));
      assertTrue(attrName.equals("tpcStatus"));
    }
  }
  
  /**
   * line 80 in core/src/org/kepler/util/DBConnectionFactory.java
   */
  public void testDBConnectionFactory()
  {
    String username;
    String password;
    
    ConfigurationProperty commonProperty = ConfigurationManager.getInstance()
      .getProperty(ConfigurationManager.getModule("common"));
    List hsqlList = commonProperty.findProperties("sqlEngineName", "hsql", true);
    //ConfigurationProperty.prettyPrintList(hsqlList);
    ConfigurationProperty hsqlProp = (ConfigurationProperty)hsqlList.get(0);
    
		//username = Config.getValue(USERNAMEPATH);
		//password = Config.getValue(PASSWORDPATH);
    username = hsqlProp.getProperty("userName").getValue();
    password = hsqlProp.getProperty("password").getValue();
    assertTrue(username.equals("sa"));
    
  }
  
  /**
   * line 455 in util/src/util/StaticUtil.java
   */
  public void testStaticUtil()
  {
    String namespace = "eml://ecoinformatics.org/eml-2.0.0";
    ConfigurationProperty commonProperty = ConfigurationManager.getInstance()
      .getProperty(ConfigurationManager.getModule("common"));
    //commonProperty.prettyPrint();
    String qformat = commonProperty.getProperty("qformat").getValue();
    String stylePath = commonProperty.getProperty("stylePath").getValue();
		//String qformat = Config.getValue("//qformat");
		//String stylePath = Config.getValue("//stylePath");
		//Map stylesheets = Config.getMap("//stylesheet", "./namespace",
		//		"./systemid");
    
    ConfigurationProperty stylesheetsProp = commonProperty.getProperty("stylesheets");
    //stylesheetsProp.prettyPrint();
    ConfigurationProperty stylesheetProp = (ConfigurationProperty)
      stylesheetsProp.findProperties("namespace", namespace, true).get(0);
    String stylesheet = stylesheetProp.getProperty("systemid").getValue();
    assertTrue(stylesheet.equals("style/eml-2.0.1/eml.xsl"));
  }
  
  /**
   * line 80 in gui/src/org/kepler/gui/KeplerApplication.java
   */
  public void testKeplerApplication()
  {
    ConfigurationProperty commonProperty = ConfigurationManager.getInstance()
      .getProperty(ConfigurationManager.getModule("common"));
    ConfigurationProperty splashscreenProp = commonProperty.getProperty("splash.image");
    String splashname = splashscreenProp.getValue();
    assertTrue(splashname.equals("images/kepler-splash.png"));
  }
  
  /**
   * line 130 in ecogrid/src/org/kepler/authentication/gui/LDAPLoginGUI.java
   */ 
  public void testLDAPLoginGUI()
  {
    ConfigurationProperty ecogridProperty = ConfigurationManager.getInstance()
      .getProperty(ConfigurationManager.getModule("ecogrid"));
    ConfigurationProperty ldapOrgProp = ecogridProperty.getProperty("ldapOrganizations");
    Iterator orgs = ConfigurationProperty.getValueList(ldapOrgProp.getProperties(), "organization", false).iterator();
    
    assertTrue(((String)orgs.next()).equals("KU"));
  }
  
  /**
   * lines 90, 187 in ecogrid/src/org/ecoinformatics/seek/ecogrid/SearchRegistryAction.java
   */
  public void testSearchRegistryAction()
  {
    String registryEndPoint;
    ConfigurationProperty ecogridProperty = ConfigurationManager.getInstance()
      .getProperty(ConfigurationManager.getModule("ecogrid"));
    ConfigurationProperty endpointProperty = ecogridProperty.getProperty("registry.endPoint");
    registryEndPoint = endpointProperty.getValue();
    assertTrue(registryEndPoint.equals("http://knb.ecoinformatics.org/registry/services/RegistryService"));
  }
  
  /**
   * lines 147, 685, 791 in ecogrid/src/org/ecoinformatics/seek/ecogrid/EcoGridServicesController.java
   */
  public void testEcoGridServicesController()
  {
    //in lookupAuthenticationDomainMapping()
    String serviceType = "http://ecoinformatics.org/authenticationservice-1.0.0";
    ConfigurationProperty ecogridProperty = ConfigurationManager.getInstance()
      .getProperty(ConfigurationManager.getModule("ecogrid"));
    ConfigurationProperty authMapping = ecogridProperty.findProperties("serviceType", serviceType, true).get(0);
    assertTrue(authMapping.getProperty("serviceClass").getValue().equals("org.kepler.authentication.LDAPAuthenticationService"));
    
    //in generateServiceFromServiceNode()
    List servicesList = ecogridProperty.getProperties("servicesList.service");
    //ConfigurationProperty.prettyPrintList(servicesList);
    ConfigurationProperty cp = (ConfigurationProperty)servicesList.get(3);
    //cp.prettyPrint();
    String serviceName = cp.getProperty("serviceName").getValue();
    serviceType = cp.getProperty("serviceType").getValue();
    String endPoint = cp.getProperty("endPoint").getValue();
    String selection = cp.getProperty("selected").getValue();
    String serviceGroup = cp.getProperty("serviceGroup").getValue();
    assertTrue(serviceName.equals("KNB Metacat Query Interface"));
    assertTrue(serviceType.equals("http://ecoinformatics.org/queryservice-1.0.0"));
    assertTrue(endPoint.equals("http://ecogrid.ecoinformatics.org/knb/services/QueryService"));
    assertTrue(selection.equals("true"));
    assertTrue(serviceGroup.equals("KNB"));
    
    //in getDocumentList()
    List documentTypeList = cp.getProperties("documentType");
    ConfigurationProperty documentTypeProp = (ConfigurationProperty)documentTypeList.get(1);
    String namespace = documentTypeProp.getProperty("namespace").getValue();
    String label = documentTypeProp.getProperty("label").getValue();
    String selectedStr = documentTypeProp.getProperty("selected").getValue();
    assertTrue(namespace.equals("eml://ecoinformatics.org/eml-2.0.1"));
    assertTrue(label.equals("Ecological Metadata Language 2.0.1"));
    assertTrue(selectedStr.equals("true"));
  }
  
  /**
   * line 124 of ecogrid/src/org/ecoinformatics/seek/ecogrid/DocumentType.java
   */
  public void testDocumentType()
  {
    String namespace = "eml://ecoinformatics.org/eml-2.1.0";
    String metadataSpecificationClassName;
    
    ConfigurationProperty ecogridProperty = ConfigurationManager
        .getInstance().getProperty(ConfigurationManager.getModule("ecogrid"));
    ConfigurationProperty cp  = ecogridProperty.getProperty("metadataSpecificationClassList");
    ConfigurationProperty specProp = (ConfigurationProperty)
      cp.findProperties("namespace", namespace, true).get(0);
    //specProp.prettyPrint();
		// check if the return vector is null or not
		if (specProp == null) {
      fail("specProp should not be null");
			//throw new UnrecognizedDocumentTypeException(error);
		}
		//metadataSpecificationClassName = (String) metadataSpecificationClassNameList
		//		.get(0);
    metadataSpecificationClassName = specProp.getProperty("value").getValue();
    assertTrue(metadataSpecificationClassName.equals("org.ecoinformatics.seek.datasource.eml.eml2.EML210MetadataSpecification"));
  }
  
  /**
   * lines 170, 242 in ecogrid/src/org/ecoinformatics/seek/datasource/EcogridDataCacheItem.java
   */
  public void testEcogridDataCacheItem()
  {
    String mEndPoint;
    ConfigurationProperty commonProperty = ConfigurationManager
      .getInstance().getProperty(ConfigurationManager.getModule("ecogrid"));
    mEndPoint = commonProperty.getProperty("srb.endPoint").getValue();
    assertTrue(mEndPoint.equals("http://srb.ecoinformatics.org:8080/SRBImpl/services/SRBQueryService"));
    
    String user = commonProperty.getProperty("srb.user").getValue();
		//String passwd = Config.getValue("//ecogridService/srb/passwd");
    String passwd = commonProperty.getProperty("srb.passwd").getValue();
		//String machineName = Config.getValue("//ecogridService/srb/machineName");
    String machineName = commonProperty.getProperty("srb.machineName").getValue();
    
    assertTrue(user.equals("testuser.sdsc"));
    assertTrue(passwd.equals("TESTUSER"));
    assertTrue(machineName.equals("srb-mcat.sdsc.edu"));
  }
  
  /**
   *  lines 84, 127, 164 in data-handling/src/org/ecoinformatics/seek/dataquery/DelimiterResolver.java
   */
  public void testDelimiterResolver()
  {
    String dbDelimiter;
    String metadataDelimiter = ";";
    ConfigurationManager confMan = ConfigurationManager.getInstance();
    ConfigurationProperty commonProperty = confMan.getProperty(ConfigurationManager.getModule("common"));
    ConfigurationProperty sqlEngineProperty = (ConfigurationProperty)commonProperty
      .findProperties("sqlEngineName", "hsql", true).get(0);
    //sqlEngineProperty.prettyPrint();
    //List l = sqlEngineProperty.findProperties("metadataDelimiter", metadataDelimiter, true);
    //ConfigurationProperty.prettyPrintList(l);
    ConfigurationProperty mappingProperty = (ConfigurationProperty)
        sqlEngineProperty.findProperties("metadataDelimiter", metadataDelimiter, true).get(0);
    dbDelimiter = mappingProperty.getProperty("DBdelimiter").getValue();
    assertTrue(dbDelimiter.equals("\\semi"));
    
    
    String replaceMent;
    String givenDelimiter = "0x";
    List prefixes = sqlEngineProperty.getProperties("delimiterMapping.prefixmapping"); 
    // go through the hash table to if the givenDelimiter start with a
    // prefix
    Iterator enm = prefixes.iterator();
    while (enm.hasNext()) {
      //String prefix = (String) enm.next();
      ConfigurationProperty prefixMapping = (ConfigurationProperty)enm.next();
      //System.out.println("prefixMapping: ");
      //prefixMapping.prettyPrint();
      String prefix = prefixMapping.getProperty("metadataDelimiter").getValue();
      if (prefix != null && givenDelimiter.startsWith(prefix)) {
        //inHash = true;
        //originalString = prefix;
        //replaceMent = (String) specialPrefixMapping.get(prefix);
        replaceMent = prefixMapping.getProperty("DBdelimiter").getValue();
        assertTrue(replaceMent.equals("\\u"));
        break;
      }
    }
  }
  
  /**
   * line 121 in data-handling/src/org/ecoinformatics/seek/dataquery/DBTablesGenerator.java
   */
  public void testDBTablesGenerator()
  {
    ConfigurationManager confMan = ConfigurationManager.getInstance();
    ConfigurationProperty commonProperty = confMan.getProperty(ConfigurationManager.getModule("common"));
    ConfigurationProperty sqlEngineProperty = (ConfigurationProperty)commonProperty
      .findProperties("sqlEngineName", "hsql", true).get(0);
    //sqlEngineProperty.prettyPrint();
      
    String CREATETEXTTABLE = sqlEngineProperty.getProperty("SQLDictionary.textTable.createTextTable").getValue();
    String CREATETABLE = sqlEngineProperty.getProperty("SQLDictionary.createTable").getValue();
    String IFEXISTS = sqlEngineProperty.getProperty("SQLDictionary.dropSuffix").getValue();
    String SEMICOLON = sqlEngineProperty.getProperty("SQLDictionary.semicolon").getValue();
    String FIELDSEPATATOR = sqlEngineProperty.getProperty("SQLDictionary.textTable.fieldSeperator").getValue();
    String SETTABLE = sqlEngineProperty.getProperty("SQLDictionary.textTable.setTable").getValue();
    String SOURCE = sqlEngineProperty.getProperty("SQLDictionary.textTable.source").getValue();
    String IGNOREFIRST = sqlEngineProperty.getProperty("SQLDictionary.textTable.ignoreFirst").getValue();
    
    assertTrue(CREATETEXTTABLE.equals("CREATE TEXT TABLE"));
    assertTrue(CREATETABLE.equals("CREATE CACHED TABLE"));
    assertTrue(IFEXISTS.equals("IF EXISTS"));
    assertTrue(SEMICOLON.equals(";"));
    assertTrue(FIELDSEPATATOR.equals("fs="));
    assertTrue(SETTABLE.equals("SET TABLE"));
    assertTrue(SOURCE.equals("SOURCE"));
    assertTrue(IGNOREFIRST.equals("ignore_first=true"));
  }
  
  /**
   * lines 78, 85, 100, 129 in data-handling/src/org/ecoinformatics/seek/dataquery/DBDataTypeResolver.java
   */
  public void testDBDataTypeResolver()
  {
    ConfigurationManager confMan = ConfigurationManager.getInstance();
    ConfigurationProperty commonProperty = confMan.getProperty(ConfigurationManager.getModule("common"));
    ConfigurationProperty sqlEngineProperty = (ConfigurationProperty)commonProperty
      .findProperties("sqlEngineName", "hsql", true).get(0);
    //sqlEngineProperty.prettyPrint();
    
    String attributeType = "STRING";
    ConfigurationProperty dbDatatypeMappingProp = sqlEngineProperty.getProperty("DBdataTypeMapping");
    List mappingsList = dbDatatypeMappingProp.findProperties("metadataDataType", attributeType, true);
    ConfigurationProperty mappingProp = (ConfigurationProperty)mappingsList.get(0);
    //return mappingProp.getProperty("DBdataType").getValue();
    assertTrue(mappingProp.getProperty("DBdataType").getValue().equals("LONGVARCHAR"));
    
    dbDatatypeMappingProp = sqlEngineProperty.getProperty("DBdataTypeMapping");
    mappingsList = dbDatatypeMappingProp.findProperties("metadataDataType", attributeType, true);
    mappingProp = (ConfigurationProperty)mappingsList.get(0);
    assertTrue(mappingProp.getProperty("javaDataType").getValue().equals("String"));
    
    String DEFAULTTYPE = sqlEngineProperty.getProperty("DBdataTypeMapping").getProperty("defaultDBdataType").getValue();
    String JAVADEFAULT = sqlEngineProperty.getProperty("DBdataTypeMapping").getProperty("defaultJavaDataType").getValue();
    assertTrue(DEFAULTTYPE.equals("LONGVARCHAR"));
    assertTrue(JAVADEFAULT.equals("String"));
  }
  
  /**
   * line 213 of core/src/org/kepler/util/AuthNamespace.java
   */
  public void testAuthNamespace()
  {
    ConfigurationManager confMan = ConfigurationManager.getInstance();
    ConfigurationProperty commonProperty = confMan.getProperty(ConfigurationManager.getModule("common"));
		try {
      List authNamespaceServiceList = commonProperty.getProperties("authNamespaceServices.authNamespaceService");
			//NodeList ans = c
			//		.getNodeListFromPath("//authNamespaceServices/authNamespaceService");
      
			if (authNamespaceServiceList == null || authNamespaceServiceList.size() == 0)
				return;
      
      assertTrue(authNamespaceServiceList.size() == 1);
			for (int i = 0; i < authNamespaceServiceList.size(); i++) {
				//Node service = ans.item(i); // <authNamespaceService url="">
        ConfigurationProperty authNamespaceService = (ConfigurationProperty)authNamespaceServiceList.get(i);
				if (authNamespaceService == null)
			  {
					continue;
				}
				
				//String serviceURL = service.getAttributes().getNamedItem("url")
				//		.getNodeValue();
        String serviceURL = authNamespaceService.getProperty("url").getValue();
				//_configuredAuthorities.add(serviceURL);
        assertTrue(serviceURL.equals("http://gamma.msi.ucsb.edu/OpenAuth/"));
			}

		} catch (Exception e) {
			System.out.println(e.getStackTrace());
			System.out.println(e);
			System.out.println("Unable to readAuthNamespaceServicesConfiguration");
      fail("no exception should have been thrown in testAuthNamespace()");
		}
  }
  
  /**
   * line 62 in core/src/org/kepler/objectmanager/data/DataTypeResolver.java
   */
  public void testDataTypeResolver()
  { 
    ConfigurationManager confMan = ConfigurationManager.getInstance();
    ConfigurationProperty commonProperty = confMan.getProperty(ConfigurationManager.getModule("common"));
    ConfigurationProperty dtdProperty = commonProperty.getProperty("dataTypeDictionary");
    List types = dtdProperty.getProperties();
    
    //dataTypes = new DataType[nl.getLength()];
    //for (int i = 0; i < nl.getLength(); i++)
    assertTrue(types.size() == 10);
    assertTrue(((ConfigurationProperty)types.get(0)).getProperty("name").getValue().equals("INTEGER"));
    assertTrue(((ConfigurationProperty)types.get(9)).getProperty("name").getValue().equals("DATETIME"));
    
    //for(int i=0; i<types.size(); i++)
    int i = 5;
    while(i < 10)
    {
      //CachedXPathAPI xpathapi = new CachedXPathAPI();
      //Node n = nl.item(i);
      ConfigurationProperty type = (ConfigurationProperty)types.get(i);
      //type.prettyPrint();
      //Node nameNode = xpathapi.selectSingleNode(n, "./name");
      ConfigurationProperty nameProp = type.getProperty("name");
      ConfigurationProperty numberTypeProp = type.getProperty("numberType");
      //Node numberTypeNode = xpathapi.selectSingleNode(n,
      //		"./numberType");
      if (nameProp== null || numberTypeProp == null) {
        throw new RuntimeException(
            "Expecting a name and numberType "
                + "child of dataType in " + "configuration.xml.");
      }
  
      //String name = nameNode.getFirstChild().getNodeValue();
      //String numberType = numberTypeNode.getFirstChild()
      //		.getNodeValue();
      String name = nameProp.getValue();
      String numberType = numberTypeProp.getValue();
      
      if(i == 5)
        assertTrue(name.equals("LONG"));
      if(i == 5)
        assertTrue(numberType.equals("integer"));
  
      //NodeList aliasNL = xpathapi.selectNodeList(n, "./alias");
      List aliasList = type.getProperties("alias");
      String[] aliases = new String[aliasList.size()];
      for (int j = 0; j < aliasList.size(); j++) {
        //Node aliasNode = aliasNL.item(j);
        //aliases[j] = aliasNode.getFirstChild().getNodeValue();
        aliases[j] = ((ConfigurationProperty)aliasList.get(j)).getValue();
      }
      if(i == 5)
        assertTrue(aliases[0].equals("xs:long"));
      if(i == 5)
        assertTrue(aliases[1].equals("long"));
  
      //Node numericTypeNode = xpathapi.selectSingleNode(n,
      //		"./numericType");
      ConfigurationProperty numericTypeProp = type.getProperty("numericType");
      String maxval = null;
      String minval = null;
      String textencoding = null;
      if (numericTypeProp != null) {
        //Node minValNode = xpathapi.selectSingleNode(
        //		numericTypeNode, "./minValue");
        //Node maxValNode = xpathapi.selectSingleNode(
        //		numericTypeNode, "./maxValue");
        ConfigurationProperty minValProp = numericTypeProp.getProperty("minValue");
        ConfigurationProperty maxValProp = numericTypeProp.getProperty("maxValue");
        if(i == 5)
          assertTrue(minValProp.getValue().equals("-9.22337e18"));
        if(i == 5)
          assertTrue(maxValProp.getValue().equals("9.22337e18"));
        if (minValProp == null || maxValProp == null) {
          throw new RuntimeException(
              "Expecting a minValue and "
                  + "maxValue children of numericType in configuration.xml.");
        }
  
        //minval = minValNode.getFirstChild().getNodeValue();
        //maxval = maxValNode.getFirstChild().getNodeValue();
        minval = minValProp.getValue();
        maxval = maxValProp.getValue();
      } else {
        //Node textTypeNode = xpathapi.selectSingleNode(n,
        //		"./textType");
        ConfigurationProperty textTypeProp = type.getProperty("textType");
        if (textTypeProp == null) {
          throw new RuntimeException(
              "Each dataType defined in the "
                  + "config.xml file must have either a textType or a numericType "
                  + "defined to be valid.");
        }
  
        //Node textEncodingNode = xpathapi.selectSingleNode(
        //		textTypeNode, "./encoding");
        ConfigurationProperty textEncodingProp = textTypeProp.getProperty("encoding");
        if(i == 8)
          assertTrue(textEncodingProp.getValue().equals("ASCII"));
        //System.out.println("text encoding: " + textEncodingProp.getValue());
        if (textEncodingProp == null) {
          throw new RuntimeException(
              "The textType node must have an "
                  + "encoding node as a child in config.xml.");
        }
        //textencoding = textEncodingNode.getFirstChild()
        //		.getNodeValue();
        textencoding = textEncodingProp.getValue();
      }
      if(i == 5)
      {
        i = 8;
      }
      else
      {
        i = 100;
      }
    }
  }
  
  /**
   * line 263 actors/src/org/kepler/dataproxy/datasource/geon/GEONDatabaseResource.java
   */
  public void testGEONDatabaseResource()
  {
    ConfigurationManager confMan = ConfigurationManager.getInstance();
    ConfigurationProperty commonProperty = confMan.getProperty(ConfigurationManager.getModule("common"));
    ConfigurationProperty geonSqlEngine = commonProperty.getProperty("sqlEngines").findProperties("sqlEngineName", "geon", true).get(0);
    //geonSqlEngine.prettyPrint();
    assertTrue(geonSqlEngine.getProperty("jdbcConnect").getValue().equals("jdbc:geon://geon01.sdsc.edu:25322"));
    assertTrue(geonSqlEngine.getProperty("dbDriver").getValue().equals("org.geongrid.jdbc.driver.Driver"));
    assertTrue(geonSqlEngine.getProperty("email").getValue().equals("klin@ucsd.edu"));
    assertTrue(geonSqlEngine.getProperty("userName").getValue().equals("geongrid"));
  }
  
  /**
   * line 125 in actors/src/org/geon/FilterUI.java
   * line 571 in actors/src/org/sdm/spa/BrowserUI.java
   * line 122 in actors/src/org/srb/SRBCreateQuetyInterface.java
   */
  public void testFilterUI()
  {
    //serverPath = Config.getValue(SERVERPATH);
    //String SERVERPATH = "//servers/server[@name=\"geon\"]/url"
    //get the configuration for this module
    ConfigurationManager confMan = ConfigurationManager.getInstance();
    //get the specific configuration we want
    ConfigurationProperty commonProperty = confMan.getProperty(ConfigurationManager.getModule("common"));
    ConfigurationProperty serversProperty = commonProperty.getProperty("servers.server");
    ConfigurationProperty geonProperty = serversProperty.findProperties("name", "geon").get(0);
    String serverPath = geonProperty.getProperty("url").getValue();
    assertTrue(serverPath.equals("http://geon01.sdsc.edu:8164/"));
  }
  
  /**
   * line 150 in actors/src/org/ecoinformatics/seek/datasource/darwincore/DarwinCoreSchema.java
   */
  public void DarwinCoreSchemaTest()
  {
    //String urlStr = Config.getValue(schemaXpath);
    //schemaXpath: //ecogridService/digir/schema
    String urlStr;
    //get the configuration for this module
    ConfigurationManager confMan = ConfigurationManager.getInstance();
    //get the specific configuration we want
    ConfigurationProperty ecogridProperty = confMan.getProperty(ConfigurationManager.getModule("ecogrid"));
    ConfigurationProperty schemaProperty = ecogridProperty.getProperty("digir.schema");
    urlStr = schemaProperty.getValue();
    assertTrue(urlStr.equals("http://bnhm.berkeley.edu/manis/DwC/darwin2jrw030315.xsd"));
  }
  
  /**
   * line 126 in actors/src/org/kepler/dataproxy/metadata/ADN/ADNMetadataSpecification.java
   */
  public void ADNMetadataSpecificationTest()
  {
    ConfigurationManager confMan = ConfigurationManager.getInstance();
    ConfigurationProperty documentationProperty = confMan.getProperty(
      ConfigurationManager.getModule("common"));
    String docURL =  documentationProperty.getProperty("documentation.url").getValue();
    String userName = documentationProperty.getProperty("documentation.username").getValue();
    assertTrue(docURL.equals("http://geon10.sdsc.edu:8080/GEONSearchPortlet/jsp/dataset-detail.jsp"));
    assertTrue(userName.equals("kepler"));
  }
  
  /**
   * line 174 in actors/src/org/ecoinformatics/seek/datasource/eml/eml2/EML2MetadataSpecification.java
   */
  public void EML2MetadataSpecificationTest()
  {
    try
    {
      String namespace = "eml://ecoinformatics.org/eml-2.0.0";
      String NAMESPACE = "namespace";
      String RETURNFIELDENTITY = "entityName";
      String RETURNFIELDTYPE = "type";
      String RETURNFIELDTITLE = "title";
      
      Module ecogrid = ConfigurationManager.getModule("ecogrid");
      ConfigurationProperty ecogridProperty = config.getProperty(ecogrid);
      
      ConfigurationProperty returnFieldTypeList = ecogridProperty.getProperty("returnFieldTypeList");
      //returnFieldTypeList.prettyPrint();
      List returnFieldTypeNamespaceList = returnFieldTypeList.findProperties(NAMESPACE, namespace, true);
      List titlePathList = ConfigurationProperty.findProperties(returnFieldTypeNamespaceList, RETURNFIELDTYPE, RETURNFIELDTITLE, false);
      assertTrue(titlePathList.size() == 1);
      //ConfigurationProperty.prettyPrintList(titlePathList);
      titlePathList = ConfigurationProperty.getValueList(titlePathList, "value", true);
      assertTrue(titlePathList.size() == 1);
      assertTrue(((String)titlePathList.get(0)).equals("dataset/title"));
      
      
      List entityPathList = ConfigurationProperty.findProperties(returnFieldTypeNamespaceList, RETURNFIELDTYPE, RETURNFIELDENTITY, false);
      entityPathList = ConfigurationProperty.getValueList(entityPathList, "value", true);
      assertTrue(entityPathList.size() == 1);
      assertTrue((((String)entityPathList.get(0)).equals("entityName")));
    }
    catch(Exception e)
    {
      e.printStackTrace();
      fail("No exception should have been thrown in EML2MetadataSpecificationTest(): " + e.getMessage());
    }
  }
}
