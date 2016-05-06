/*
 * Copyright (c) 1998-2012 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2012-04-27 13:16:27 -0700 (Fri, 27 Apr 2012) $' 
 * '$Revision: 29789 $'
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
package org.kepler.metadata;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.ecoinformatics.seek.datasource.eml.eml2.Eml200Parser;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationNamespace;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.objectmanager.data.db.Attribute;
import org.kepler.objectmanager.data.db.Entity;
import org.xml.sax.InputSource;

import ptolemy.actor.TypedIOPort;
import ptolemy.actor.lib.LimitedFiringSource;
import ptolemy.data.ArrayToken;
import ptolemy.data.OrderedRecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * @author Derik Barseghian
 * @version $Id: MetadataParser.java 29789 2012-04-27 20:16:27Z barseghian $
 */
public class MetadataParser extends LimitedFiringSource {

	private static final Logger logger = Logger.getLogger(MetadataParser.class);
	private static final String CONFIGURATIONNAMESPACE = "metadataParser";
	private static final String MODULENAME = "actors";
	private static final String MAPPINGLIST = "mappingList";
	private static final String MAPPING = "mapping";
	private static final String TYPE = "type";
	private static final String CLASS = "class";
	private TypedIOPort metadataInputPort;
	private TypedIOPort metadataTypeInputPort;
	private Vector<MetadataTypeParserMap> metadataTypeClassMapList = null;
	
	public MetadataParser(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
		
		metadataInputPort = new TypedIOPort(this, "metadataInputPort", true, false);
		metadataTypeInputPort = new TypedIOPort(this, "metadataTypeInputPort", true, false);

    output.setTypeEquals(new ArrayType(BaseType.RECORD));
    metadataTypeClassMapList = getMetadataTypeParserMap();
	}
	
	public void fire() throws IllegalActionException {
		super.fire();
	
		if (metadataInputPort.numberOfSources() <= 0){
			return;
		}
		if (metadataTypeInputPort.numberOfSources() <= 0){
      return;
    }
		
		StringToken metadataTypeStringToken = (StringToken) metadataTypeInputPort.get(0);
		String metadataType = metadataTypeStringToken.stringValue();
		
		ParserInterface parser = createParser(metadataType);
		if(parser == null) {
		  throw new IllegalActionException("Kepler can't find a metadata parser for the type "+metadataType+". Please check the metadataParser.xml file.");
		}
    StringToken metadataStringToken = (StringToken) metadataInputPort.get(0);
    
    //logger.debug("received on input port:\n"+metadataStringToken.stringValue());
        
		try {
	        // FIXME character encoding
			InputStreamReader inputStreamReader = new InputStreamReader(IOUtils.toInputStream(metadataStringToken.stringValue(), "UTF-16"), "UTF-16");
			ArrayList<OrderedRecordToken> orderedRecordTokens = parsePackage(inputStreamReader, parser);
			if (output.numberOfSinks() > 0){
				OrderedRecordToken[] ors = orderedRecordTokens.toArray(new OrderedRecordToken[orderedRecordTokens.size()]);
				output.broadcast(new ArrayToken(ors));
				//for (int i=0; i<ors.length; i++){
				//	output.send(0, ors[0]);
				//}
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        

	}
	
	public void preinitialize() throws IllegalActionException {
	    super.preinitialize();
	}
	
	/** The director told us to stop firing immediately. */
	public void stop() {
		super.stop();
	}
	
	public void wrapup() throws IllegalActionException {
		super.wrapup();
	}
	
	
	/*
	 *Create a parser object from the metadataParser.xml configuration according to the metadata type.
	 *The null will be returned if no parser class can be found. 
	 */
	private ParserInterface createParser(String metadataType) {
	  ParserInterface parser = null;
	  if(metadataType != null ) {
	    if(metadataTypeClassMapList != null ) {
	      for(MetadataTypeParserMap map : metadataTypeClassMapList) {
	        if(map != null) {
	          if(metadataType.equals(map.getMetadataType())) {
	            String className = map.getClassName();
	            if(className != null) {
	              try {
	                Class classDefinition = Class.forName(className);
	                parser = (ParserInterface)classDefinition.newInstance();
	                break;
	              } catch (InstantiationException e) {
	                logger.warn("MetadataPaser.createParser - can't get the parser object since "+e.getMessage());
	                continue;
	              } catch (IllegalAccessException e) {
	                logger.warn("MetadataPaser.createParser - can't get the parser object since "+e.getMessage());
                  continue;
	              } catch (ClassNotFoundException e) {
	                logger.warn("MetadataPaser.createParser - can't get the parser object since "+e.getMessage());
                  continue;
	              }
	            }
	          }
	        }
	      }
	    }
	  }
	  return parser;
	}
	
	/*
	 * Get the the mapping between the parser class name and metadata type from the configuration file - metadataParser.xml
	 */
	private Vector<MetadataTypeParserMap> getMetadataTypeParserMap() {
	  Vector<MetadataTypeParserMap> mapping = new Vector <MetadataTypeParserMap>();
	  ConfigurationManager manager = ConfigurationManager.getInstance();
	  if(manager != null) {
	    ConfigurationProperty cp = manager.getProperty(ConfigurationManager.getModule(MODULENAME),
          new ConfigurationNamespace(CONFIGURATIONNAMESPACE));
	    if(cp != null) {
	      //System.out.println("==== the property file is not null");
	      ConfigurationProperty mappingListProperty = cp.getProperty(MAPPINGLIST);
	      if (mappingListProperty != null) {
	        //System.out.println("==== the mapping list  is not null");
	        List<ConfigurationProperty> mappingProperties = mappingListProperty.getProperties(MAPPING);
	        if(mappingProperties != null) {
	          for(ConfigurationProperty mappingProperty : mappingProperties) {
	            //System.out.println("==== the mapping  is not null");
	            ConfigurationProperty metadataTypeProperty = mappingProperty.getProperty(TYPE);
	            ConfigurationProperty classProperty = mappingProperty.getProperty(CLASS);
	            if(metadataTypeProperty != null && classProperty != null ) {
	              MetadataTypeParserMap map = new MetadataTypeParserMap();
	              //System.out.println("==== add class name "+classProperty.getValue());
	              //System.out.println("==== add metadata type "+metadataTypeProperty.getValue());
	              map.setClassName(classProperty.getValue());
	              map.setMetadataType(metadataTypeProperty.getValue());
	              mapping.add(map);
	            }
	          }
	          
	        }
	        
	      }
	      
	    }
	  }
	  return mapping;
	  
	}


	private ArrayList<OrderedRecordToken> parsePackage(Reader reader, ParserInterface parser) throws IllegalActionException {
		Eml200Parser eml200Parser;

		ArrayList<OrderedRecordToken> orderedRecordTokens = new ArrayList<OrderedRecordToken>();


		try { // parse the package for the names and types of the atts
			//eml200Parser = new Eml200Parser();
			parser.parse(new InputSource(reader));
			//String namespace = eml200Parser.getNameSpace();
			
			logger.debug("entity count:"+parser.getEntityCount());
			
			//TODO check if eml parser be refactored to use generics / if this typing is safe:
			List<Entity> entities = parser.getEntities();
			if(entities != null ) {
			  for (Entity entity: entities){
	        ArrayList<String> labels = new ArrayList<String>();
	        ArrayList<Token> tokens = new ArrayList<Token>();
	        logger.debug("name:" + entity.getName() + " getAttributes().length:" + entity.getAttributes().length);
	        //logger.debug("key:"+key + " name:" + entity.getName() + " num attributes:" + entity.getAttributes().length + " entity:\n"+entity.toXml());
	        
	        labels.add("entityName");
	        tokens.add(new StringToken(entity.getName()));
	        int numAttributes = entity.getAttributes().length;
	        
	        if (numAttributes > 0){
	          labels.add("attributeName");
	          labels.add("attributeDataType");
	        
	          Token[] attributeNames = new Token[numAttributes];
	          Token[] attributeDataTypes = new Token[numAttributes];
	          
	          for (int i=0; i< numAttributes; i++){
	            Attribute a = entity.getAttributes()[i];
	            logger.debug("attributeName:" + a.getName() +  
	                " attributeDataType:" + a.getDataType());
	                //" unit:" + a.getUnit() +
	                //" unitType:" + a.getUnitType() + 
	                //" measurementScale:" + a.getMeasurementScale());
	            attributeNames[i] = new StringToken(a.getName());
	            attributeDataTypes[i] = new StringToken(a.getDataType());

	          }
	          
	          tokens.add(new ArrayToken(attributeNames));
	          tokens.add(new ArrayToken(attributeDataTypes));
	        }
	        
	        String[] labelArray = labels.toArray(new String[labels.size()]);
	        Token[] tokenArray = tokens.toArray(new Token[tokens.size()]);
	        orderedRecordTokens.add(new OrderedRecordToken(labelArray, tokenArray));
	      }
			}
			
			
			return orderedRecordTokens;
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalActionException("Error parsing the eml package: "
					+ e.getMessage());
		}

	}
	
	/*
	 * A class represents the map between the metadata type and parser from the configuration file.
	 */
	private class MetadataTypeParserMap {
	  private String metadataType = null;
	  private String className = null;
	  
	  /**
	   * Constructor
	   */
	  public MetadataTypeParserMap() {
	    
	  }
	  
	  /**
	   * Set the metadata type.
	   * @param metadataType - the type of the metadata.
	   */
	  public void setMetadataType(String metadataType) {
	    this.metadataType = metadataType;
	  }
	  
	  /**
	   * Get the metadata type. It can be null.
	   * @return the type of the metadata.
	   */
	  public String getMetadataType() {
	    return metadataType;
	  }
	  
	  /**
	   * Set the class name.
	   * @param className - the name of the class.
	   */
	  public void setClassName(String className) {
	    this.className = className;
	  }
	  
	  /**
	   * Get the class name mapping the metadata type. It can be null.
	   * @return the class name.
	   */
	  public String getClassName() {
	    return this.className;
	  }
	}

}
