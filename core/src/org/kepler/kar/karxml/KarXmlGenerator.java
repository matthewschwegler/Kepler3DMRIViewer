/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:21:34 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31119 $'
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Iterator;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.kar.KAREntry;
import org.kepler.kar.KARFile;
import org.kepler.util.FileUtil;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * The KarXmlGenerator class reads in a KARFile and generates an xml string that
 * can be used to upload the KAR file to Metacat.
 * 
 * @author Aaron Schultz
 */
public class KarXmlGenerator {

	private static final Log log = LogFactory.getLog(KarXmlGenerator.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();
	private String _schemaUrl = null;
	private String _XSNamespace = "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"";
	private String _namespace = null;
	private String _schemaLocation = null;
	private Name _manifestVersion = new Name("Manifest-Version");
	
	private KARFile _karFile;
	private StringBuffer karxml;
	private boolean hasReportLayout = false;

	public static final String nl = System.getProperty("line.separator");
	public static final String tab = "\t";

	/**
	 * Empty Constructor
	 */
	public KarXmlGenerator() {
	}

	/**
	 * Constructor that takes a KARFile as a parameter. This will be the KARFile
	 * read in during the call to getKarXml()
	 * 
	 * @param karFile
	 */
	public KarXmlGenerator(KARFile karFile) {
		setKarFile(karFile);
	}

	/**
	 * Set the KARFile object to generate the karxml from.
	 * 
	 * @param karFile
	 * @return
	 */
	public void setKarFile(KARFile karFile) {
		_karFile = karFile;
		setKARVersionSpecificInfo(_karFile.getVersion());	
	}
	
	/**
	 * Set _namespace, _schemaUrl, and _schemaLocation for KAR version
	 * @param karFileVersion
	 */
	public void setKARVersionSpecificInfo(String karFileVersion) {

		ConfigurationProperty KARVersionsProp = KARFile
				.getKARVersionsConfigProperty();

		Iterator<ConfigurationProperty> karVersionsItr = KARVersionsProp
				.getProperties().iterator();
		while (karVersionsItr.hasNext()) {
			ConfigurationProperty prop = karVersionsItr.next();
			if (prop.getName().equals(KARFile.KAR_VERSION_PROPERTY_NAME)) {
				ConfigurationProperty version = prop.getProperty(KARFile.KAR_VERSION_VERSION_PROPERTY_NAME);
				if (version.getValue().equals(karFileVersion)) {
					ConfigurationProperty namespaceConfigProp = prop
							.getProperty(KARFile.KAR_VERSION_NAMESPACE_PROPERTY_NAME);
					ConfigurationProperty schemaUrlConfigProp = prop
							.getProperty(KARFile.KAR_VERSION_SCHEMAURL_PROPERTY_NAME);
					_namespace = namespaceConfigProp.getValue();
					_schemaUrl = schemaUrlConfigProp.getValue();
					_schemaLocation = "xsi:schemaLocation=\"" + _namespace
							+ " " + _schemaUrl + "\"";
				}
			}
		}

	}

	/**
	 * Get an XML file representation for a KARFile. Pass in the KARFile to be
	 * used while generating the karxml.
	 * 
	 * @param karFile
	 * @return
	 * @throws IOException
	 */
	public String getKarXml(KARFile karFile) throws IOException, SAXException{
		setKarFile(karFile);
		String xml = getKarXml();
		return xml;
	}

	/**
	 * Get an XML file representation for a KARFile. Use the setKarFile(KARFile)
	 * method before calling this method.
	 * 
	 * @return
	 * @throws IOException
	 */
	public String getKarXml() throws IOException, SAXException {
		karxml = new StringBuffer();
		karxml.append("<?xml version=\"1.0\"?>" + nl);
		karxml.append("<kar:kar xmlns:kar=\""+_namespace+"\""+" "+_XSNamespace+" "+_schemaLocation+">" 
		    + nl);

		// Adding this for Sean Riddle to facilitate using the kar filename
		// in remote search results
		File f = _karFile.getFileLocation();
		String filename = f.getName();
		karxml.append(tab + "<karFileName>" + nl);
		karxml.append(tab + tab + filename + nl);
		karxml.append(tab + "</karFileName>" + nl);
		
		long fileSize = f.length();
		karxml.append(tab + "<karFileSize>" + nl);
		karxml.append(tab + tab + fileSize + nl);
		karxml.append(tab + "</karFileSize>" + nl);

		appendXmlForMainAttributes();

		for (KAREntry entry : _karFile.karEntries()) {
		  if(entry != null && entry.isReportLayout()){
		    hasReportLayout = true;
		  }
			karxml.append(tab + "<karEntry>" + nl);
			appendXmlForEntryAttributes(entry);
			appendXmlFor(entry);
			karxml.append(tab + "</karEntry>" + nl);
		}

		karxml.append("</kar:kar>" + nl);
		String karXmlStr = karxml.toString();
		validateKarXml(karXmlStr);
		return karXmlStr;
	}
	
	/**
	 * Determine if the generated kar xml has a report layout.
	 * This method can only be called after calling getKarXml()
	 * @return
	 */
	public boolean hasReportLayout(){
	  return hasReportLayout;
	}
	
	/*
	 * validate the generated kar xml
	 */
	private void validateKarXml(String xml) throws SAXException
	{
	  if(xml != null)
	  {
	    Document document = KarXml.parseXml(new StringReader(xml));
	    if(document == null)
	    {
	      throw new SAXException("Kepler couldn't transform the generated kar xml to a DOM tree model. So the generated kar xml is not valid");
	    }
	    else
	    {
	      boolean isValid = KarXml.validateDocument(document);
	      if(!isValid)
	      {
	        throw new SAXException("The generated kar xml is not valid");
	      }
	    }
	  }
	  else
	  {
	    throw new SAXException("The generated kar xml is null and it is invalid");
	  }
	}

	/**
	 * Read in the main attributes of the KAR and append them to the xml.
	 * 
	 * @throws IOException
	 */
	private void appendXmlForMainAttributes() throws IOException {

		karxml.append(tab + "<mainAttributes>" + nl);

		// gets the manifest
		Manifest mf = _karFile.getManifest();

		// gets the main attributes in the manifest
		Attributes atts = mf.getMainAttributes();
		if(atts != null){
  		String lsid = atts.getValue(KARFile.LSID);
  		if(lsid != null){
  		  karxml.append(tab + tab + "<" + KARFile.LSID + ">" + nl);
        karxml.append(tab + tab + tab + lsid + nl);
        karxml.append(tab + tab + "</" + KARFile.LSID + ">" + nl);
  		}
  		String moduleDependencies = atts.getValue(KARFile.MOD_DEPEND);
      if(moduleDependencies != null){
        karxml.append(tab + tab + "<" + KARFile.MOD_DEPEND + ">" + nl);
        karxml.append(tab + tab + tab + moduleDependencies + nl);
        karxml.append(tab + tab + "</" + KARFile.MOD_DEPEND + ">" + nl);
      }
      String karVersion = atts.getValue(KARFile.KAR_VERSION);
      if(karVersion != null){
        karxml.append(tab + tab + "<" + KARFile.KAR_VERSION + ">" + nl);
        karxml.append(tab + tab + tab + karVersion + nl);
        karxml.append(tab + tab + "</" + KARFile.KAR_VERSION + ">" + nl);
      }
      String manifestVersion = atts.getValue(_manifestVersion);
      if(manifestVersion != null){
        karxml.append(tab + tab + "<" + _manifestVersion + ">" + nl);
        karxml.append(tab + tab + tab + manifestVersion + nl);
        karxml.append(tab + tab + "</" + _manifestVersion + ">" + nl);
      }
		}
		// Loop through the attributes
		/*for (Object att : atts.keySet()) {

			if (att instanceof Name) {
				Name attrName = (Name) att;
				String attrValue = atts.getValue(attrName);

				karxml.append(tab + tab + "<" + attrName + ">" + nl);
				karxml.append(tab + tab + tab + attrValue + nl);
				karxml.append(tab + tab + "</" + attrName + ">" + nl);
			} else {
				throw new IOException("Unrecognized Main Attribute");
			}
		}*/

		karxml.append(tab + "</mainAttributes>" + nl);
    //System.out.println("karxml is ==========\n "+karxml.toString());
	}

	/**
	 * Read in all attributes for the entry and append them to the xml.
	 * 
	 * @param entry
	 */
	private void appendXmlForEntryAttributes(KAREntry entry) {

		karxml.append(tab + tab + "<karEntryAttributes>" + nl);

		// the name of a KAREntry is not found in it's attributes
		// must get it directly
		String entryName = entry.getName();
		karxml.append(tab + tab + tab + "<Name>" + nl);
		karxml.append(tab + tab + tab + tab + entryName + nl);
		karxml.append(tab + tab + tab + "</Name>" + nl);

		Attributes atts = entry.getAttributes();

		for (Object att : atts.keySet()) {
			// System.out.println( att.toString() );
			if (att instanceof Name) {

				Name attrName = (Name) att;
				String value = atts.getValue(attrName);

				karxml.append(tab + tab + tab + "<" + attrName + ">" + nl);
				karxml.append(tab + tab + tab + tab + value + nl);
				karxml.append(tab + tab + tab + "</" + attrName + ">" + nl);

			}
		}

		karxml.append(tab + tab + "</karEntryAttributes>" + nl);

	}

	/**
	 * Decide what to do depending on what kind of entry this is.
	 * 
	 * @param entry
	 * @throws IOException
	 */
	private void appendXmlFor(KAREntry entry) throws IOException {

		if (isXmlEntry(entry)) {
			appendXmlEntryContents(entry);
		} else {
			// maybe at some point we'll want to include this
			// for now, only include the kar entry attributes
			// for generic (aka non-xml) files
			// appendGenericEntryXml(entry);
		}

	}

	/**
	 * Checks to see if the entry ends with the xml extension. This method is
	 * case insensitive.
	 * 
	 * @param entry
	 * @return
	 */
	private boolean isXmlEntry(KAREntry entry) {

		String entryName = entry.getName();
		String lowerCaseEntryName = entryName.toLowerCase();
		if (lowerCaseEntryName.endsWith(".xml"))
			return true;

		return false;
	}

	/**
	 * For an XML file we convert the contents to a string and append it.
	 * 
	 * @param entry
	 * @throws IOException
	 */
	private void appendXmlEntryContents(KAREntry entry) throws IOException {
		karxml.append(tab + tab + "<karEntryXML>" + nl);

		InputStream is = _karFile.getInputStream((ZipEntry) entry);

		String contents = FileUtil.convertStreamToString(is);
		contents = processXmlEntryContents(contents);

		karxml.append(contents + nl);

		karxml.append(tab + tab + "</karEntryXML>" + nl);
	}

	/**
	 * Strip out the "<?xml" tag from the xml. Also removes a header DOCTYPE, if present.
	 * 
	 * @param xml
	 * @return
	 */
	public String processXmlEntryContents(String xml) {

		xml = xml.replaceAll("<\\?xml.*?>", "");
		xml = Pattern.compile("<!DOCTYPE.*?>", Pattern.DOTALL).matcher(xml).replaceAll("");
		return xml;
	}

	/**
	 * For a file that is not XML maybe we want to add something here. Or maybe
	 * the attributes for the entry are enough.
	 * 
	 * @param entry
	 * 
	 *            private void appendGenericEntryXml(KAREntry entry) {
	 * 
	 *            karxml.append(tab + tab + "<genericKarEntry>" + nl);
	 * 
	 *            karxml.append(tab + tab + "</genericKarEntry>" + nl); }
	 */

}
