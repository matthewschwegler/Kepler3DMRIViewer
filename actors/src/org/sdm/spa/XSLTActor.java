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

package org.sdm.spa;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.XMLToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * <p>
 * Given an XSL transformation file, performs the specified transformation on
 * the input XML doc.
 * 
 * </p>
 * <p>
 * XSLT is designed for use as part of XSL, which is a stylesheet language for
 * XML. In addition to XSLT, XSL includes an XML vocabulary for specifying
 * formatting. XSL specifies the styling of an XML document by using XSLT to
 * describe how the document is transformed into another XML document that uses
 * the formatting vocabulary. (ref: http://www.w3.org/TR/xslt)
 * 
 * </p>
 * <p>
 * Given an xml stream as input, XSLTTransformer is used for for linking (almost
 * but not quite fitting) output port and input port data formats together. The
 * actor produces an html stream that can be viewed or queried using the
 * BrowserUI actor.
 * 
 * </p>
 * <p>
 * The configuration window for the XSLTTransformer actor can be viewed by
 * double-clicking on the actor or by selecting 'Configure' from the right-click
 * context menu. The window displays a GUI for browsing the XSL script which
 * will be used to perform the transformation.
 * </p>
 * 
 * @author Ilkay Altintas
 * @version $Id: XSLTActor.java 24234 2010-05-06 05:21:26Z welker $
 *
 */

public class XSLTActor extends TypedAtomicActor {

	/**
	 * This parameter is to provide the path for the xsl file in user's
	 * computer.
	 * <P>
	 * This parameter can be filled in by specifying the file using the browse
	 * interface or by typing in the file. <i>The actor has <b>NO</b> default
	 * value. </i> Please double click on the actor to set it.
	 */
	public FileParameter xslFileName;

	/**
	 * String representation of the XML or HTMl input stream that needs to be
	 * transformed.
	 */
	public TypedIOPort xmlIn;

	/**
	 * String representation of the output of the transformation.
	 */
	public TypedIOPort htmlOut;

	public XSLTActor(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {

		super(container, name);

		xmlIn = new TypedIOPort(this, "xmlIn", true, false);
		htmlOut = new TypedIOPort(this, "htmlOut", false, true);

		htmlOut.setTypeEquals(BaseType.STRING);
        
        xslFileName = new FileParameter(this, "XSLT File Path");

		new Attribute(xmlIn, "_showName");
		new Attribute(htmlOut, "_showName");

        _factory = TransformerFactory.newInstance();;

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"30\" " + "style=\"fill:white\"/>\n"
				+ "<text x=\"10\" y=\"25\" "
				+ "style=\"font-size:16; fill:blue; font-family:SansSerif\">"
				+ "XSLT</text>\n" + "</svg>\n");

	}

	public void fire() throws IllegalActionException {
		super.fire();

        Source xmlSource;
        StringReader xmlReader = null;
        
        Token token = xmlIn.get(0);

        // see what kind of input
        if(token instanceof StringToken)
        {
            String xmlStr = ((StringToken)token).stringValue();    
            System.out.println(xmlStr);
            xmlReader = new StringReader(xmlStr);
            xmlSource = new StreamSource(xmlReader);
        }
        else if(token instanceof XMLToken)
        {
            Document doc = ((XMLToken)token).getDomTree();
            xmlSource = new DOMSource(doc);
        }
        else
        {
            throw new IllegalActionException(this, "Unsupported type of " +
                "input token: " + token.getType());
        }

        StringWriter writer = new StringWriter();
        Result result = new StreamResult(writer);

        // read the xsl file
        File xslFile = xslFileName.asFile();
        if(xslFile == null)
        {
            throw new IllegalActionException(this, "XSL file required. ");
        }

        Source xslSource = new StreamSource(xslFile);

        // perform the transformation
        try
        {
            Transformer transformer = _factory.newTransformer(xslSource);
            transformer.transform(xmlSource, result);
        }
        catch(TransformerException e)
        {
            throw new IllegalActionException(this, "Error transforming: " + 
                e.getMessage());
        }

        // output the result
		htmlOut.broadcast(new StringToken(writer.toString()));

        // close resources
        try
        {
            if(xmlReader != null)
            {
                xmlReader.close();
            }
		    writer.close();
        }
        catch(IOException e)
        {
            throw new IllegalActionException(this, "Error closing " +
                " string writer: " + e.getMessage());
        }
	}

    /////////////////////////////////////////////////////////////////////////
    //// private variables                                               ////

    /** Factory for xsl transformers. */
    private TransformerFactory _factory;
}