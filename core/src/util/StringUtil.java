/*
 * Copyright (c) 2002-2010 The Regents of the University of California.
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

package util;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * @author Daniel Crawl
 * @version $Id: StringUtil.java 31119 2012-11-26 22:21:34Z crawl $
 */

public class StringUtil {
	/** This class cannot be instantiated. */
	private StringUtil() {
	}

	/** Join an array of strings with a delimiter. */
	public static String join(String str[], String delim) {
		StringBuilder retval = null;
		if (str.length > 0) {
			retval = new StringBuilder();

			/*
			 * System.out.println("len = " + str.length); for(int i = 0; i <
			 * str.length; i++) System.out.print(i + " " + str[i]);
			 * System.out.println("");
			 */

			for (int i = 0; i < str.length - 1; i++) {
				retval.append(str[i]);
				retval.append(delim);

				// System.out.println("retval " + retval);
			}
			retval.append(str[str.length - 1]);
		}
		// System.out.println("done " + retval);
		return retval.toString();
	}

	/**
	 * Clean up nasty things like spaces, quotes, etc from strings when using
	 * them to generate names (for files, say)
	 * 
	 * @param input
	 *            String with possible problematic characters
	 * @return another string with likely problematic characters replaced or
	 *         removed
	 */
	public static String stripProblematicCharacters(String input) {
		String retVal = input;
		retVal = retVal.replaceAll(" ", "_"); // spaces
		retVal = retVal.replaceAll("'", ""); // single quotes
		retVal = retVal.replaceAll("\"", ""); // double quotes

		// more as they come up...

		return retVal;
	}
    
    /** Convert a list to a comma-separated string. */
    public static String listToString(List<?> list) {
        return listToString(list, ", ");
    }

    /** Convert a list to a string with a delimiter. */
    public static String listToString(List<?> list, String delimiter) {
        StringBuilder buf = new StringBuilder();
        Iterator<?> iter = list.iterator();
        while (iter.hasNext()) {
            buf.append(iter.next());
            if (iter.hasNext()) {
                buf.append(delimiter);
            }
        }
        return buf.toString();
    }
    
    /** Nicely format an XML string.
     *  @return the formatted string. If an error occurs, returns
     *  the original string.
     */
    public static String prettifyXML(String xmlStr, int indent) {
        try {
            Source xmlInput = new StreamSource(new StringReader(xmlStr));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer(); 
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch(Exception e) {
            System.err.println("Could not format XML: " + e.getMessage());
            return xmlStr;
        }
    }

}
