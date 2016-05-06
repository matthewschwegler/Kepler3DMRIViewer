/*
 * Copyright (c) 2010-2011 The Regents of the University of California.
 * All rights reserved.
 *
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

package org.kepler.kar.karxml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.kar.KARFile;
import org.kepler.moml.CompositeClassEntity;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ptolemy.actor.TypedCompositeActor;

/**
 * Created by IntelliJ IDEA.
 * User: sean
 * Date: Feb 16, 2010
 * Time: 1:53:33 PM
 */
public class KarXml {

	private Document document;
	private String name;
	private long size;
	private String karVersion;
	private String manifestVersion;
	private List<String> moduleDependencies;
	private String lsid;
	private List<KarEntry> karEntries;
	
	private boolean valid = false;
	
	
	
	public static final String COMPOSITEACTORTYPE = TypedCompositeActor.class.getName();
	public static final String COMPOSITECLASSENTITYTYPE = CompositeClassEntity.class.getName();
	private static final String DIRECTOR = "director";
	public static final String REPORTLAYOUT = "ReportLayout";
	private static XPathExpression GET_KAR_VERSION_EXPRESSION = null;
	private static XPathExpression GET_NAME_EXPRESSION = null;
	private static XPathExpression GET_SIZE_EXPRESSION = null;
	private static XPathExpression GET_MANIFEST_VERSION_EXPRESSION = null;
	private static XPathExpression GET_MODULE_DEPENDENCIES_EXPRESSION = null;
	private static XPathExpression GET_LSID_EXPRESSION = null;
	private static XPathExpression GET_KAR_ENTRIES = null;
	private static XPathExpression GKE_NAME_EXPRESSION = null;
	private static XPathExpression GKE_DEPENDS_ON_EXPRESSION = null;
	private static XPathExpression GKE_TYPE_EXPRESSION = null;
	private static XPathExpression GKE_LSID_EXPRESSION = null;
	private static XPathExpression GKE_HANDLER_EXPRESSION = null;
	//only used by 2.0.0:
	private static XPathExpression GKE_DEPENDS_ON_MODULE_EXPRESSION = null;
	private static XPathExpression GKE_XML_EXPRESSION = null;
	private static XPathExpression GKE_GET_SEMANTIC_TYPES = null;
	private static XPathExpression GKE_GET_WORKFLOW_NAME = null;
	private static XPathExpression GKE_GET_DIRECTOR = null;
	private static XPathExpression GKE_GET_DERIVEDFROM = null;
	private String repositoryName;
	private boolean hasReportLayoutInKarEntry = false;
	
	// accessors
	public String getKarVersion() {
		return karVersion;
	}

	public String getManifestVersion() {
		return manifestVersion;
	}

	public List<String> getModuleDependencies() {
		return moduleDependencies;
	}

	public String getLsid() {
		return lsid;
	}
	

	public Document getDocument() {
		return document;
	}

	public List<KarEntry> getKarEntries() {
		return karEntries;
	}
	// accessors end	
	
	private KarXml() {
		if (GET_KAR_VERSION_EXPRESSION == null) {
			setupXPathExpressions();
		}
	}

	private static void setupXPathExpressions() {
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		try {
			GET_KAR_VERSION_EXPRESSION = xpath.compile("/*[local-name() = 'kar']/mainAttributes/KAR-Version/text()");
			GET_NAME_EXPRESSION = xpath.compile("/*[local-name() = 'kar']/karFileName/text()");
			GET_SIZE_EXPRESSION = xpath.compile("/*[local-name() = 'kar']/karFileSize/text()");
			GET_MANIFEST_VERSION_EXPRESSION = xpath.compile("/*[local-name() = 'kar']/mainAttributes/Manifest-Version/text()");
			GET_MODULE_DEPENDENCIES_EXPRESSION = xpath.compile("/*[local-name() = 'kar']/mainAttributes/module-dependencies/text()");
			GET_LSID_EXPRESSION = xpath.compile("/*[local-name() = 'kar']/mainAttributes/lsid/text()");
			
			GET_KAR_ENTRIES = xpath.compile("/*[local-name() = 'kar']/karEntry");
			GKE_NAME_EXPRESSION = xpath.compile("karEntryAttributes/Name/text()");
			GKE_DEPENDS_ON_EXPRESSION = xpath.compile("karEntryAttributes/dependsOn/text()");
			GKE_TYPE_EXPRESSION = xpath.compile("karEntryAttributes/type/text()");
			GKE_LSID_EXPRESSION = xpath.compile("karEntryAttributes/lsid/text()");
			GKE_HANDLER_EXPRESSION = xpath.compile("karEntryAttributes/handler/text()");
			//only used by 2.0.0:
			GKE_DEPENDS_ON_MODULE_EXPRESSION = xpath.compile("karEntryAttributes/dependsOnModule/text()");
			GKE_XML_EXPRESSION = xpath.compile("karEntryXML");
			GKE_GET_SEMANTIC_TYPES = xpath.compile("karEntryXML/entity/property[@class=\"org.kepler.sms.SemanticType\"]/@value");
			GKE_GET_WORKFLOW_NAME = xpath.compile("karEntryXML/entity/@name");
			GKE_GET_DIRECTOR = xpath.compile("karEntryXML/entity/property/property[@name=\"entityId\"]/@value");
			GKE_GET_DERIVEDFROM =xpath.compile("karEntryXML/entity/property/property[@name=\"derivedFrom\"]/@value");
		} catch(XPathExpressionException ex) {
			log.error("Exception", ex);
			// Make sure all expression are in a mutually consistent state
			GET_KAR_VERSION_EXPRESSION = null;
			GET_NAME_EXPRESSION = null;
			GET_SIZE_EXPRESSION = null;
			GET_MANIFEST_VERSION_EXPRESSION = null;
			GET_MODULE_DEPENDENCIES_EXPRESSION = null;
			GET_LSID_EXPRESSION = null;

			GET_KAR_ENTRIES = null;
			GKE_DEPENDS_ON_EXPRESSION = null;
			GKE_TYPE_EXPRESSION = null;
			GKE_LSID_EXPRESSION = null;
			GKE_HANDLER_EXPRESSION = null;
			//only used by 2.0.0:
			GKE_DEPENDS_ON_MODULE_EXPRESSION = null;
			GKE_XML_EXPRESSION = null;
			GKE_GET_SEMANTIC_TYPES = null;
			GKE_GET_WORKFLOW_NAME = null;
			GKE_GET_DIRECTOR = null;
			GKE_GET_DERIVEDFROM = null;
		}
	}

	public static KarXml of(File file) {
		try {
			return of(new FileInputStream(file));
		}
		catch(FileNotFoundException ex) {
			return null;
		}
	}
	public static KarXml of(InputStream is) {
		Document document = parseXmlStream(is);
		if (document == null) {
			return null;
		}
		// Validate document
		boolean valid = validateDocument(document);
		if (!valid) {
			System.out.println("Invalid document.");
			return null;
		}
		KarXml kx = new KarXml();
		kx.document = document;
		kx.parse();
		kx.fixSemanticTypes();
		return kx;
	}

	private void fixSemanticTypes() {
		
		List<String> semanticTypes = null;
		
		// Go through each of the KAR XML entries in this object
		
		// Find a TypedCompositeActor with at least one semantic type on it
		for (KarEntry entry : this.getKarEntries()) {
			if (entry.getType().endsWith(".TypedCompositeActor") && !entry.getSemanticTypes().isEmpty()) {
				semanticTypes = entry.getSemanticTypes();
				break;
			}
		}
		if (semanticTypes == null) {
			// Nothing has a semantic type. So nothing is going to be visible, sorry.
			log.warn("This KAR has no semantically-tagged composites, cannot determine semantic types to assign");
			return;
		}
		
		// Assign that/those semantic type(s) to all entries without a semantic type already
		for (KarEntry entry : this.getKarEntries()) {
			if (entry.getSemanticTypes().isEmpty()) {
				entry.semanticTypes = new ArrayList<String>(semanticTypes);
			}
		}
	}

	
	/**
	 * Validate the document against the schema. Currently we only validate against
	 * kar xml 2.0.0 and 2.1.0. If it is not a kar xml 2.0.0 or 2.1.0 xml, this method will return true.
	 * @param document  the document need to be validate
	 * @return true if it is a valid document
	 */
	public static boolean validateDocument(Document document) {
	  if(document == null){
	    return false;
	  }
		try {
		  Node docElement = document.getDocumentElement();
		  String nameSpace = docElement.getNamespaceURI();
		  log.debug("The name space is ===== "+nameSpace);
		  
		  if(nameSpace == null  || !nameSpace.equals(KARFile.KAR_VERSION_200_NAMESPACE_DEFAULT)|| 
				  !nameSpace.equals(KARFile.KAR_VERSION_210_NAMESPACE_DEFAULT)){
		    return true;
		  }
		  SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		  String resourceDir = KARFile.getResourceDir(nameSpace);
		  String resourceFileName = KARFile.getResourceFileName(nameSpace);
		  // ClassLoader.getResource javadoc says: 
		  // The name of a resource is a '/'-separated path name that identifies the resource.
		  // so I am using a hardcode / in this path:
		  Source schemaFile = new StreamSource(KarXml.class.getClassLoader().getResourceAsStream(resourceDir +
				  "/" + resourceFileName));
		  Schema schema = factory.newSchema(schemaFile);
		  Validator validator = schema.newValidator();
		  validator.validate(new DOMSource(document));
		}
		catch(SAXException ex) {
			ex.printStackTrace();
			return false;
		}
		catch(IOException ex) {
			ex.printStackTrace();
			return false;
		}
		log.debug("return true");
		return true;
	}

	private void parse() {
		try {
			NodeList nodes = (NodeList) GET_KAR_VERSION_EXPRESSION.evaluate(document, XPathConstants.NODESET);
			if (nodes.getLength() == 0) {
				log.warn("No KAR version found");
			}
			else {
				valid = true;	// NOTE: At present, this is what is used to
								// determine if the file is a valid KAR XML
								// file.
				karVersion = nodes.item(0).getNodeValue().trim();
			}
			
			nodes = (NodeList) GET_NAME_EXPRESSION.evaluate(document, XPathConstants.NODESET);
			if (nodes.getLength() == 0) {
				log.warn("No KAR name found");
			}
			else {
				name = nodes.item(0).getNodeValue().trim();
			}
			
      //System.out.println("The kar name is ================ "+name);
			
			nodes = (NodeList) GET_SIZE_EXPRESSION.evaluate(document, XPathConstants.NODESET);
			if (nodes.getLength() == 0) {
				log.warn("No KAR size found");
				size = -1;
			}
			else {
				size = Long.valueOf(nodes.item(0).getNodeValue().trim());
			}
			
			nodes = (NodeList) GET_MANIFEST_VERSION_EXPRESSION.evaluate(document, XPathConstants.NODESET);
			if (nodes.getLength() == 0) {
				log.warn("No manifest version found");
			}
			else {
				manifestVersion = nodes.item(0).getNodeValue().trim();
			}
			
			nodes = (NodeList) GET_LSID_EXPRESSION.evaluate(document, XPathConstants.NODESET);
			if (nodes.getLength() == 0) {
				log.warn("No LSID found");
			}
			else {
				lsid = nodes.item(0).getNodeValue().trim();
			}
			//System.out.println("The kar file lisd is ============== "+lsid);
			
			nodes = (NodeList) GET_MODULE_DEPENDENCIES_EXPRESSION.evaluate(document, XPathConstants.NODESET);
			if (nodes.getLength() == 0) {
				log.warn("No module dependencies found");
			}
			else {
				String moduleDependenciesString = nodes.item(0).getNodeValue().trim();
				String[] dependencies = moduleDependenciesString.split(";");
				moduleDependencies = new ArrayList<String>(Arrays.asList((String[]) dependencies));
			}
			
			nodes = (NodeList) GET_KAR_ENTRIES.evaluate(document, XPathConstants.NODESET);
			if (nodes.getLength() == 0) {
				log.warn("No KAR entries found");
			}
			else {
				List<KarEntry> entries = new ArrayList<KarEntry>();
				for (int i = 0; i < nodes.getLength(); i++) {
					Node node = nodes.item(i);
					KarEntry entry = KarEntry.of(node, karVersion);
					//System.out.println("kar entry name ==================="+entry.getName());
					entry.setParent(this);
					entries.add(entry);
					if(entry.hasReportingLayout())
					{
					  hasReportLayoutInKarEntry = true;
					}
				}
				karEntries = entries;
			}
		}
		catch(XPathExpressionException ex) {
			log.error("Exception", ex);
		}
		
		// Get the karEntry nodes
		
	}

	public static Document parseXmlStream(InputStream is) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			return builder.parse(is);
			// TODO: Have the document parser validate against the KAR XML DTD
			// (in progress)
		}
		catch(ParserConfigurationException ex) {
			log.error("Exception", ex);
		}
		catch(SAXException ex) {
			log.error("Exception", ex);
		}
		catch(IOException ex) {
			log.error("Exception", ex);
		}
		return null;
	}
	
	public static Document parseXml(Reader reader) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		try {
			InputSource source = new InputSource(reader);
			DocumentBuilder builder = factory.newDocumentBuilder();
			return builder.parse(source);
			// TODO: Have the document parser validate against the KAR XML DTD
			// (in progress)
		}
		catch(ParserConfigurationException ex) {
			log.error("Exception", ex);
		}
		catch(SAXException ex) {
			log.error("Exception", ex);
		}
		catch(IOException ex) {
			log.error("Exception", ex);
		}
		return null;
	}
	
	// toString
	@Override
	public String toString() {
		return "KarXml{" +
				"karVersion='" + getKarVersion() + '\'' +
				", manifestVersion='" + getManifestVersion() + '\'' +
				", moduleDependencies=" + getModuleDependencies() +
				", lsid='" + getLsid() + '\'' +
				'}';
	}

	public boolean isValid() {
		return valid;
	}
	
	public String getId() {
		return this.getLsid();
	}
	
	public String getName() {
		if (name == null) {
			// On some searches, this really floods stdout.
//			log.warn("No explicit name, using LSID instead");
			return this.getId();
		}
		else {
			return name;
		}
	}

	/**
	 * @return The size of the KAR file in bytes, if available. -1, if not available.
	 */
	public long getSize() {
		return size;
	}

	public String getRepositoryName() {
		return repositoryName;
	}

	public void setRepositoryName(String repositoryName) {
		this.repositoryName = repositoryName;
	}
	
	public boolean hasReportLayout()
	{
	  return hasReportLayoutInKarEntry;
	}

	public static class KarEntry {
		// disallow the default constructor
		private KarEntry() {}
		
		private KarEntry(Node karEntryNode, String karVersion) {
			this();
			try {
				NodeList nodes = (NodeList) GKE_DEPENDS_ON_EXPRESSION.evaluate(karEntryNode, XPathConstants.NODESET);
				if (nodes.getLength() == 0) {
					log.warn("No dependsOn found");
				}
				else {
					dependsOn = nodes.item(0).getNodeValue().trim();
				}
				
				nodes = (NodeList) GKE_NAME_EXPRESSION.evaluate(karEntryNode, XPathConstants.NODESET);
				if (nodes.getLength() == 0) {
					log.warn("No name found");
				}
				else {
					name = nodes.item(0).getNodeValue().trim();
				}
				
				nodes = (NodeList) GKE_TYPE_EXPRESSION.evaluate(karEntryNode, XPathConstants.NODESET);
				if (nodes.getLength() == 0) {
					log.warn("No type found");
				}
				else {
					type = nodes.item(0).getNodeValue().trim();
					if(type != null)
					{
					  type = type.trim();
					  //System.out.println("type is ==================== "+type);
					  if(type.equals(COMPOSITEACTORTYPE) || type.equals(COMPOSITECLASSENTITYTYPE))
					  {
					    //System.out.println("================it is a composite actor");
					    isCompositeActor = true;
					    //System.out.println("is a compoiste Actor ================ ");
					  }
					  else if(type.contains(REPORTLAYOUT))
					  {
					    hasReportingLayout = true;
					  }
					}
				}
				
				nodes = (NodeList) GKE_LSID_EXPRESSION.evaluate(karEntryNode, XPathConstants.NODESET);
				if (nodes.getLength() == 0) {
					log.warn("No LSID found");
				}
				else {
					lsid = nodes.item(0).getNodeValue().trim();
				}
				
				nodes = (NodeList) GKE_HANDLER_EXPRESSION.evaluate(karEntryNode, XPathConstants.NODESET);
				if (nodes.getLength() == 0) {
					log.warn("No handler found");
				}
				else {
					handler = nodes.item(0).getNodeValue().trim();
				}
				
				//not used in 2.1:
				if (!karVersion.equals(KARFile.VERSION_2_1)){
					nodes = (NodeList) GKE_DEPENDS_ON_MODULE_EXPRESSION.evaluate(karEntryNode, XPathConstants.NODESET);
					if (nodes.getLength() == 0) {
						log.warn("No dependsOnModule found");
					}
					else {
						dependsOnModule = nodes.item(0).getNodeValue().trim();
					}
				}
				
				// if this composite actor type, we need to figure out if it has director
				if(isCompositeActor)
				{
				  nodes = (NodeList) GKE_GET_DIRECTOR.evaluate(karEntryNode, XPathConstants.NODESET);
				  if(nodes.getLength() >0)
				  {
				   
				    if(hasDirector(nodes))
				    {
				      //System.out.println("find a directory, this is workflow =============");
	            hasDirector = true;
	            //System.out.println("has director========");
	            isWorkflow = true;
				    }
				    else
				    {
				      //check derivedForm
				      if(isDerivedFromDirector(karEntryNode))
				      {
				        hasDirector = true;
	              //System.out.println("has derived director========");
	              isWorkflow = true;
				      }
				    }
				   
				  }
				}
				//if this workflow, we need to get workflow name.
				//(getName method will get the name of the workflow xml file.)
				if(isWorkflow)
				{
				  nodes = (NodeList)GKE_GET_WORKFLOW_NAME.evaluate(karEntryNode, XPathConstants.NODESET);
				  if(nodes.getLength() == 0)
				  {
				    //System.out.println("Couldn't find workflow name, use the name of kar entry to replace it");
				    workflowName = name;
				  }
				  else
				  {
				    
				    workflowName = nodes.item(0).getNodeValue().trim();
				    //System.out.println("get the workflow name "+workflowName);
				  }
				}
				
				
				nodes = (NodeList) GKE_XML_EXPRESSION.evaluate(karEntryNode, XPathConstants.NODESET);
				if (nodes.getLength() == 0) {
					log.warn("No XML found");
				}
				else {
					xml = nodes.item(0);
				}
				
				nodes = (NodeList) GKE_GET_SEMANTIC_TYPES.evaluate(karEntryNode, XPathConstants.NODESET);
				if (nodes.getLength() == 0) {
					log.warn("No semantic types found");
				}
				else {
					List<String> semanticTypes = new ArrayList<String>();
					for (int i = 0; i < nodes.getLength(); i++) {
						Node node = nodes.item(i);
						semanticTypes.add(node.getNodeValue());
					}
					this.semanticTypes = semanticTypes;
				}
			}
			catch(XPathExpressionException ex) {
				log.warn("Exception", ex);
			}			
		}
		
		
		/*
		 * Determine if the entity has a director
		 */
		private boolean hasDirector(NodeList directorNodeList)
		{
		  boolean hasDirector = false;
		  if(directorNodeList != null)
		  {
		    for(int i=0 ; i<directorNodeList.getLength(); i++)
		    {
		      Node node = directorNodeList.item(i);
		      String value = node.getNodeValue().trim();
		      if(value != null && value.indexOf(DIRECTOR) != -1)
		      {
		        hasDirector = true;
		        break;
		      }
		    }
		  }
		  return hasDirector;
		  
		}
		
		
		/*
		 * Determine if the entity is derived from a director
		 */
		private boolean isDerivedFromDirector(Node karEntryNode){
		  boolean fromDirector = false;
		  if(karEntryNode != null){
		    try{
		      NodeList nodeList = (NodeList)GKE_GET_DERIVEDFROM.evaluate(karEntryNode, XPathConstants.NODESET);
		      if(nodeList != null){
		        for(int i=0; i<nodeList.getLength(); i++){
		          Node node = nodeList.item(i);
		          if(node != null){
		            String value = node.getNodeValue();
		            if(value != null && value.contains(DIRECTOR)){
		              fromDirector = true;
		              break;
		            }
		          }
		        }
		      }
		    }
		    catch(Exception e){
		      log.warn("Exception", e);
		    }
		  }
		  return fromDirector;
		}
		
		public String getName() {
			return name;
		}
		
		public List<String> getSemanticTypes() {
			return semanticTypes == null ? getDefaultSemanticTypes() : semanticTypes;
		}
		
		private List<String> getDefaultSemanticTypes() {
			return Collections.emptyList();
		}

		public String getDependsOn() {
			return dependsOn;
		}

		public String getType() {
			return type;
		}

		public String getLsid() {
			return lsid;
		}

		public String getHandler() {
			return handler;
		}

		//only used by 2.0.0:
		public String getDependsOnModule() {
			return dependsOnModule;
		}

		public Node getXml() {
			return xml;
		}
		
		public String getWorkflowName(){
		  return workflowName;
		}
		
		public boolean isWorkflow(){
		  return isWorkflow;
		}
		
		public boolean hasReportingLayout(){
		  return hasReportingLayout;
		}
		
		private String _asString() {
			return nodeToString(xml);
		}
		
		
		// Used for debugging
		@SuppressWarnings({"UnusedDeclaration"})
		public File asLocalFile() {
			Writer writer = null;
			String data = this._asString();
			File file = null;
			try {
				file = File.createTempFile("dump", ".xml");
				writer = new FileWriter(file);
				writer.write(data);
				System.out.println("Wrote fragment: " + file.getAbsolutePath());
			}
			catch(IOException ex) {
				System.out.println("Error dumping fragment");
			}
			finally {
				if (writer != null) {
					try {
						writer.close();
					}
					catch(IOException ignored) {}
				}
			}
			
			return file;
		}
		
		public InputStream asInputStream() {
			return new ByteArrayInputStream(this._asString().getBytes());
		}

		// toString
		@Override
		public String toString() {
			
			String KARVersion = getParent().getKarVersion();
			
			if (!KARVersion.equals(KARFile.VERSION_2_1))
			{
				return "KarEntry{" +
					"name='" + getName() + '\'' +
					", dependsOn='" + getDependsOn() + '\'' +
					", type='" + getType() + '\'' +
					", lsid='" + getLsid() + '\'' +
					", handler='" + getHandler() + '\'' +
					", dependsOnModule='" + getDependsOnModule() + '\'' +
					", xml=" + (getXml() == null ? "null" : "non-null") +
					'}';
			}
			else{
				return "KarEntry{" +
					"name='" + getName() + '\'' +
					", dependsOn='" + getDependsOn() + '\'' +
					", type='" + getType() + '\'' +
					", lsid='" + getLsid() + '\'' +
					", handler='" + getHandler() + '\'' +
					//", dependsOnModule='" + getDependsOnModule() + '\'' +
					", xml=" + (getXml() == null ? "null" : "non-null") +
					'}';
			}
		}

		public static KarEntry of(Node karEntryNode, String karVersion) {
			return new KarEntry(karEntryNode, karVersion);
		}
		
		private String name = null;
		private List<String> semanticTypes = null;
		private String dependsOn = null;
		private String type = null;
		private String lsid = null;
		private String handler = null;
		//only used by 2.0.0:
		private String dependsOnModule = null;
		private Node xml = null;
		private KarXml parent;
		private boolean isCompositeActor = false;
		private boolean hasReportingLayout = false;
		private boolean hasDirector = false;
		private boolean isWorkflow = false;
		private String workflowName = null;

		public void setParent(KarXml parent) {
			this.parent = parent;
		}
		
		public KarXml getParent() {
			return parent;
		}
	}

	private static final Log log = LogFactory.getLog(KarXml.class);
	
	public static String nodeToString(Node node) {
		StringWriter sw = new StringWriter();
		try {
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			NodeList nodes = node.getChildNodes();
			for (int i = 0; i < nodes.getLength(); i++) {
				t.transform(new DOMSource(nodes.item(i)), new StreamResult(sw));
			}
		}
		catch(TransformerException ex) {
			System.out.println("Transformer exception");
		}
		return sw.toString();
	}
}
