/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-02-21 11:20:05 -0800 (Thu, 21 Feb 2013) $' 
 * '$Revision: 31474 $'
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

package util;

import gnu.regexp.RE;
import gnu.regexp.REException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.util.DotKeplerManager;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This is a utility class. Any static methods that may be needed by multiple
 * classes should be put here.
 */
public class StaticUtil {
	// this place is for hsql data file and chache data object file
	private static final String idDelimiter = "@";
	private static String pipelineLocation = null;

	static public String getIdDelimiter() {
		return idDelimiter;
	}

	/**
	 * creates an object of a type className. this is used for instantiating
	 * plugins.
	 */
	public static Object createObject(String className)
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException {
		Object object = null;
		try {
			Class classDefinition = Class.forName(className);
			object = classDefinition.newInstance();
		} catch (InstantiationException e) {
			throw new InstantiationException("Error instantiating class.");
		} catch (IllegalAccessException e) {
			throw new IllegalAccessException("Error accessing class.");
		} catch (ClassNotFoundException e) {
			throw new ClassNotFoundException("Class " + className
					+ " not found.");
		}
		return object;
	}

	/**
	 * returns true if the inputstream's first 6 bytes = "&lt;? xml"
	 */
	public static boolean hasXMLHeader(InputStream is) throws IOException {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < 6; i++) {
			sb.append((char) is.read());
		}

		String s = sb.toString();
		if (s.indexOf("<?xml") != -1) {
			return true;
		}

		return false;
	}

	/**
	 * looks through the child nodes of n and returns the first child with name
	 * 
	 * @param name
	 *            the name of the child node to search for
	 * @param n
	 *            the node whose children we are searching
	 * @return the found node or null if the node is not found.
	 */
	public static Node getChildNodeWithName(String name, Node n) {
		NodeList nl = n.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node c = nl.item(i);
			if (c.getNodeName().equals(name)) {
				return c;
			}
		}
		return null;
	}

	/**
	 * searches for a child of n called childNodeName. If it is found, the
	 * nodeValue() of the child is returned. If it is not found, null is
	 * returned. See the Xerces documentation for class Node for return values.
	 * 
	 * @param childNodeName
	 *            the child node to search for
	 * @param n
	 *            the node to search through.
	 */
	public static String getChildNodeValue(String childNodeName, Node n) {
		Node child = getChildNodeWithName(childNodeName, n);
		return child.getNodeValue();
	}

	/**
	 * strips all of the path information off of a package file just leaving the
	 * file id. ex. /home/berkley/package/berkley.1.1.package/berkley.1.1 become
	 * berkley.1.1
	 */
	public static String getIdFromFileName(String filename) {
		return filename.substring(filename.lastIndexOf("/") + 1, filename
				.length());
	}

	/**
	 * substitutes subval for val in text
	 */
	public static String substitute(String text, String val, String subval)
			throws REException {
		RE regexp = new RE(val);
		text = regexp.substituteAll(text, subval);
		return text;
	}

	/**
	 * Set up a DOM parser for reading an XML document
	 * 
	 * @return a DOM parser object for parsing
	 */
	public static DocumentBuilder createDomParser() throws Exception {
		DocumentBuilder parser = null;

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setNamespaceAware(true);
			parser = factory.newDocumentBuilder();
			if (parser == null) {
				throw new Exception("Could not create Document parser in "
						+ "Util.createDomParser");
			}
		} catch (ParserConfigurationException pce) {
			throw new Exception("Could not create Document parser in "
					+ "Util.createDomParser.");
		}

		return parser;
	}

	/**
	 * returns the total size in bytes of all files in a directory and its
	 * subdirectories
	 * 
	 * @param directoryPath
	 *            the directory to find the size of
	 */
	public static long getDirectorySize(String directoryPath) throws Exception {
		return getDirectorySize(directoryPath, 0);
	}

	/**
	 * returns the total size in bytes of all files in a directory and its
	 * subdirectories
	 * 
	 * @param directoryPath
	 *            the directory to find the size of
	 * @param size
	 *            the size to start out with. Usually this should be 0
	 */
	private static long getDirectorySize(String directoryPath, long size)
			throws Exception {
		long length = 0;
		File root = new File(directoryPath);
		if (!root.isDirectory()) {
			throw new Exception("The path " + root.getAbsolutePath()
					+ " specified is not a directory");
		}
		if (!root.exists()) {
			throw new Exception("The path " + root.getAbsolutePath()
					+ " does not exist.");
		}
		if (!root.canRead()) {
			throw new Exception("Invalid read permissions on the path "
					+ root.getAbsolutePath());
		}

		String[] listing = root.list();
		Vector dirs = new Vector();
		// add up the file sizes
		for (int i = 0; i < listing.length; i++) {
			File f = new File(directoryPath + "/" + listing[i]);
			if (f.isFile()) {
				length += f.length();
			} else if (f.isDirectory()) {
				dirs.addElement(f);
			}
		}

		// recurse into each directory
		for (int i = 0; i < dirs.size(); i++) {
			File f = (File) dirs.elementAt(i);
			length = getDirectorySize(f.getAbsolutePath(), length);
		}
		return length + size;
	}

	/**
	 * deletes a directory tree, not including the child of directoryPath. if
	 * directoryPath == "/temp/cache" all of the contents of 'cache' will be
	 * deleted but not 'cache' itself
	 * 
	 * @param directoryPath
	 *            the directory to delete the contents of
	 */
	public static void deleteDirectoryTree(String directoryPath)
			throws Exception {
		long length = 0;
		File root = new File(directoryPath);
		if (!root.isDirectory()) {
			throw new Exception("The path " + root.getAbsolutePath()
					+ " specified is not a directory");
		}
		if (!root.exists()) {
			throw new Exception("The path " + root.getAbsolutePath()
					+ " does not exist.");
		}
		if (!root.canWrite()) {
			throw new Exception("Invalid write permissions on the path "
					+ root.getAbsolutePath());
		}

		String[] listing = root.list();
		Vector dirs = new Vector();
		// delete the files
		for (int i = 0; i < listing.length; i++) {
			File f = new File(directoryPath + "/" + listing[i]);
			if (f.isFile()) {
				if (!f.delete()) {
					throw new Exception("Could not delete "
							+ f.getAbsolutePath());
				}
			} else if (f.isDirectory()) {
				dirs.addElement(f);
			}
		}

		// recurse into each directory
		for (int i = 0; i < dirs.size(); i++) {
			File f = (File) dirs.elementAt(i);
			deleteDirectoryTree(f.getAbsolutePath());
			if (!f.delete()) { // delete this directory
				throw new Exception("Could not delete " + f.getAbsolutePath());
			}
		}
	}

	/**
	 * returns true if a document has "&lt;?xml" at the beginning of it
	 */
	public static boolean isXMLDoc(File f) throws FileNotFoundException,
			IOException {
		FileReader fr = null;
		try {
			fr = new FileReader(f);
			char[] c = new char[200];
			fr.read(c, 0, 200);
			String s = new String(c);
			if (s.indexOf("<?xml") != -1) {
				return true;
			}	
			return false;
		} finally {
			if(fr != null) {
				fr.close();
			}
		}
	}

	/**
	 * returns a DOM Document from an xml string
	 */
	public static Document getDoc(String s)
			throws javax.xml.parsers.ParserConfigurationException,
			SAXException, IOException {
		StringReader sr = new StringReader(s);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document d = db.parse(new InputSource(sr));
		return d;
	}

	/**
	 * transforms an xml document using the specified xsl stylesheet
	 * 
	 * @param xml
	 *            the document to transform
	 * @param xsl
	 *            the stylesheet to use
	 */
	public static String transformXML(Reader xml, Reader xsl)
			throws TransformerException {
		if (xsl != null) {
			TransformerFactory tFactory = TransformerFactory.newInstance();
			// Get the XML input document and the stylesheet, both in the
			// servlet
			// engine document directory.
			Source xmlSource = new StreamSource(xml);
			Source xslSource = new StreamSource(xsl);
			// Generate the transformer.
			Transformer transformer = tFactory.newTransformer(xslSource);
			// Perform the transformation, sending the output to the response.
			StringWriter outputWriter;
			transformer.transform(xmlSource, new StreamResult(
					outputWriter = new StringWriter()));
			return outputWriter.getBuffer().toString();
		}

		throw new TransformerException("XSL stylesheet is null.");
	}

	/**
	 * returns a string representation of a stack trace from the exception e
	 * 
	 * @param e
	 *            the exception to return the stack trace from
	 */
	static public String getStackTrace(Exception e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}

	/**
	 * Normalizes the given string.
	 * 
	 * @param s
	 *            string to normize
	 * @return Description of the Returned Value
	 */
	public static String normalize(String s) {
		StringBuffer str = new StringBuffer();
		char oxc = (char) Integer.decode("0xC").intValue();

		int len = (s != null) ? s.length() : 0;
		for (int i = 0; i < len; i++) {
			char ch = s.charAt(i);
			switch (ch) {
				case '<' : {
					str.append("&lt;");
					break;
				}
				case '>' : {
					str.append("&gt;");
					break;
				}
				case '&' : {
					str.append("&amp;");
					break;
				}
				case '"' : {
					str.append("&quot;");
					break;
				}
				case '\u000c' : {
					str.append("");
					break;
				}
					/*
					 * case '\r': case '\t': case '\n': { if (false) {
					 * str.append("&#"); str.append(Integer.toString(ch));
					 * str.append(';'); break; } // else, default append char break;
					 * }
					 */
				default : {
					str.append(ch);
				}
			}
		}

		return str.toString();
	}

	/**
	 * This method will transfer a xml metadata stream into a html file. The
	 * html file will be returned.
	 * 
	 * @param source
	 *            Reader the orignal xml metadata stream
	 * @param namespace
	 *            String the xml metadata namespace
	 * @param htmlFileName
	 *            String the file name will assign the html file
	 * @return URL
	 */
	public static URL getMetadataHTMLurl(Reader source, String namespace,
			String htmlFileName) throws Exception {
		// Set up the cache directory for html files if needed
		URL htmlUrl = null;
		File htmlDir = new File(DotKeplerManager.getInstance().getCacheDir()
				+ "html");
		htmlDir.mkdirs();

		File outputFile = new File(htmlDir, htmlFileName);
		ConfigurationProperty commonProperty = ConfigurationManager
				.getInstance().getProperty(
						ConfigurationManager.getModule("common"));
		String qformat = commonProperty.getProperty("qformat").getValue();
		String stylePath = commonProperty.getProperty("stylePath").getValue();

		ConfigurationProperty stylesheetsProp = commonProperty
				.getProperty("stylesheets");
		ConfigurationProperty stylesheetProp = (ConfigurationProperty) stylesheetsProp
				.findProperties("namespace", namespace, true).get(0);
		String stylesheet = stylesheetProp.getProperty("systemid").getValue();

		// TODO: determine this documents DOCTYPE/NAMESPACE and use that to
		// choose a stylesheet    
		File styleDir = new File(htmlDir, stylePath + "/" + qformat);

		// Extract the css file to the local filesystem cache directory
		if (!styleDir.exists()) {

			styleDir.mkdirs();
			String css = stylePath + "/" + qformat + "/" + qformat + ".css";
			File localCssFile = new File(styleDir, qformat + ".css");
			BufferedOutputStream outputStream = new BufferedOutputStream(
					new FileOutputStream(localCssFile));
			StaticUtil util = new StaticUtil();
			InputStream styleData = util.getClass().getClassLoader()
					.getResourceAsStream(css);
			copyInputStream(styleData, outputStream);

		}

		// Get the metadata XML document and transform it to html
		if (source != null) {

			FileOutputStream os = new FileOutputStream(outputFile);
			Hashtable parameters = new Hashtable();
			parameters.put("qformat", qformat);
			parameters.put("stylePath", stylePath);
			parameters.put("insertTemplate", "0");
			parameters.put("displaymodule", "printall");
			parameters.put("withEntityLinks", "0");
			parameters.put("withOriginalXMLLink", "0");
			parameters.put("withHTMLLinks", "0");
			xslTransform(source, os, stylesheet, parameters);
			htmlUrl = outputFile.toURI().toURL();

		}
		return htmlUrl;
	}

	/**
	 * Copy the content of an input stream into an output stream.
	 * 
	 * @param in
	 *            the InputStream to be copied
	 * @param out
	 *            the OutputStream to write data into
	 * @throws IOException
	 *             if either stream produces an error
	 */
	private static void copyInputStream(InputStream in, OutputStream out)
			throws IOException {
		byte[] buffer = new byte[1024];
		int len;

		while ((len = in.read(buffer)) >= 0)
			out.write(buffer, 0, len);

		in.close();
		out.close();
	}

	/**
	 * Transform an XML document to an output stream using the XSLT stylesheet
	 * found at the xslSystemId location and the given parameters.
	 * 
	 * @param inputXml
	 *            the XML stream to be transformed
	 * @param resultOutput
	 *            the output stream to which results are written
	 * @param xslSystemId
	 *            the location of the style sheet
	 * @param parameters
	 *            name/value pair parameters to pass to the transformer
	 */
	private static void xslTransform(Reader inputXml,
			OutputStream resultOutput, String xslSystemId, Hashtable parameters)
			throws TransformerException {
		if (xslSystemId != null) {

			TransformerFactory tFactory = TransformerFactory.newInstance();
			StaticUtil util = new StaticUtil();
			URL styleURL = util.getClass().getClassLoader().getResource(
					xslSystemId);
			InputStream stylesheet = util.getClass().getClassLoader()
					.getResourceAsStream(xslSystemId);
			StreamSource xsl = new StreamSource(stylesheet);
			xsl.setSystemId(styleURL.toExternalForm());
			Transformer transformer = tFactory.newTransformer(xsl);
			// Notify the stylesheet about each of the parameters
			if (parameters != null) {
				Enumeration en = parameters.keys();
				while (en.hasMoreElements()) {
					String key = (String) en.nextElement();
					String value = (String) parameters.get(key);
					transformer.setParameter(key, value);
				}
			}
			StreamSource input = new StreamSource(inputXml);
			StreamResult output = new StreamResult(resultOutput);
			transformer.transform(input, output);

		}
	}
	
	/**
   * Get the bytes array from a file
   * 
   *@param  fileName  
   *              the file to get the bytes array
   *@return  he bytes array
   */
  public static byte[] getBytesArrayFromFile(String fileName) throws Exception
  {
    byte[] readBytes = new byte[2000];
    int n = 0;
    int i =0;

    byte[] totalBytes = null;
    byte[] tmpBytes = null;
    int curByteCnt = 0;
    try
    {
      FileInputStream file = new FileInputStream(fileName);
      n = file.read(readBytes, 0, 2000);
      curByteCnt = 0;
      while(n > 0)
      {
        if(totalBytes == null)
        {
          totalBytes = new byte[n];
          for(i = 0; i < n; i++)
          {
            totalBytes[i] = readBytes[i];
          }
        }
        else
        {
          tmpBytes = new byte[totalBytes.length];
          for(i = 0; i < totalBytes.length; i++)
          {
            tmpBytes[i] = totalBytes[i];
          }

          totalBytes = null;
          totalBytes = new byte[tmpBytes.length + n];
          for(i = 0; i < tmpBytes.length; i++)
          {
            totalBytes[i] = tmpBytes[i];
          }
          for(i = 0; i < n; i++)
          {
            totalBytes[tmpBytes.length + i] = readBytes[i];
          }

          tmpBytes = null;
        }

        n = file.read(readBytes, 0, 2000);
      }

      file.close();
    }
    catch(Exception e)
    {
      throw e;
    }

    return totalBytes;
  }

}