/* An actor that displays XML tokens in a nice format.

 Copyright (c) 2012 The Regents of the University of California.
 All rights reserved.

 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the
 above copyright notice and the following two paragraphs appear in all
 copies of this software.

 IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.
 
 */
package org.kepler.actor.gui;

import org.jdom.input.DOMBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.w3c.dom.Document;

import ptolemy.actor.lib.gui.Display;
import ptolemy.data.XMLToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/** An actor that displays XML tokens in a nice format.
 *
 *  @author Daniel Crawl
 *  @version $Id: XMLViewer.java 29809 2012-05-05 01:41:02Z crawl $
 */
public class XMLViewer extends Display {

    /** Construct a new XMLViewer in a container with a specific name.
     *  @param container The container.
     *  @param name The name of this actor.
     *  @exception IllegalActionException If the entity cannot be contained
     *   by the proposed container.
     *  @exception NameDuplicationException If the container already has an
     *   actor with this name.
     */
    public XMLViewer(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
        
        input.setTypeEquals(BaseType.XMLTOKEN);
        
        _builder = new DOMBuilder();
        _outputter = new XMLOutputter();
        _outputter.setFormat(Format.getPrettyFormat());
    }


    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Return a nicely formatted string describing XML input on channel i.
     *  @param i The channel
     *  @return A string representation of the input, or null
     *   if there is nothing to display.
     *  @throws IllegalActionException If reading the input fails.
     */
    protected String _getInputString(int i) throws IllegalActionException {
        if (input.hasToken(i)) {
            final XMLToken token = (XMLToken) input.get(i);
            final Document document = token.getDomTree();
            synchronized(document) {
                final org.jdom.Document jdomDoc = _builder.build(document);
                return _outputter.outputString(jdomDoc);
            }
        }
        return null;
    }

    /** An object to convert org.w3c.dom.Document objects to org.jdom.Document objects. */
    private DOMBuilder _builder;
    
    /** An object to serialize org.jdom.Documents into nicely formatted strings. */
    private XMLOutputter _outputter;

}
