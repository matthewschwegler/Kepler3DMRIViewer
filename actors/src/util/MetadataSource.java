/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: tao $'
 * '$Date: 2012-08-14 11:55:29 -0700 (Tue, 14 Aug 2012) $' 
 * '$Revision: 30431 $'
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

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.gui.style.TextStyle;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.StringAttribute;

/**
 * This actor acts as a simple 'Metadata' source for a datafile It purpose is to
 * feed metadata to an ecogrid writer for submission to the Ecogrid. Metadata
 * (e.g. EML) is entered as a string along with the name of a datafile that the
 * metadata characterizes. There are 2 optional input ports that can be used to
 * input strings that replace the substings '_PARAM1_' and '_PARAM2' in the
 * metadata. This allows things like package title or id to be dynamically
 * changed in a workflow.
 * 
 * @author Dan Higgins NCEAS UC Santa Barbara
 */
public class MetadataSource extends TypedAtomicActor {
	/**
	 * The metadata as a string (typically eml)
	 */
	public StringAttribute metadata;

	/**
	 * String metadata passed on output port
	 */
	public TypedIOPort metadataOut;

	/**
	 * Datafile name passed to input
	 */
	public TypedIOPort dataFilenameIn;

	/**
	 * First parameter passed to input. The string '_PARAM1_' in the metadata
	 * will be replaced by this value if there is a token on this port.
	 */
	public TypedIOPort parameter1In;

	/**
	 * Second parameter passed to input. The string '_PARAM2_' in the metadata
	 * will be replaced by this value if there is a token on this port.
	 */
	public TypedIOPort parameter2In;

	/**
	 * Datafile name passed on output port
	 */
	public TypedIOPort dataFilenameOut;

	/**
	 *@param container
	 *            The container.
	 *@param name
	 *            The name of this actor.
	 *@exception IllegalActionException
	 *                If the container is incompatible with this actor.
	 *@exception NameDuplicationException
	 *                If the name coincides with an actor already in the
	 *                container.
	 */
	public MetadataSource(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
		metadata = new StringAttribute(this, "XML Metadata");
		TextStyle metadataTS = new TextStyle(metadata, "XML Metadata");
		metadata.setExpression(getDefaultXMLMetadata());
		dataFilenameIn = new TypedIOPort(this, "dataFilenameIn", true, false);
		parameter1In = new TypedIOPort(this, "parameter1In", true, false);
		parameter2In = new TypedIOPort(this, "parameter2In", true, false);
		metadataOut = new TypedIOPort(this, "metadataOut", false, true);
		metadataOut.setTypeEquals(BaseType.STRING); // xml in string format
		dataFilenameOut = new TypedIOPort(this, "dataFilenameOut", false, true);
		dataFilenameOut.setTypeEquals(BaseType.STRING); // xml in string format
	}

	/**
	 * Output the data read from the port
	 * 
	 *@exception IllegalActionException
	 *                If there is no director.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		try {
			String xmlmetadata = metadata.getExpression();

			String _dfilename = ((StringToken) dataFilenameIn.get(0))
					.stringValue();

			String param1 = "";
			String param2 = "";
			if (parameter1In.getWidth() > 0) {
				if (parameter1In.hasToken(0)) {
					param1 = ((StringToken) parameter1In.get(0)).stringValue();
					xmlmetadata = xmlmetadata.replaceAll("_PARAM1_", param1);
				}
			}
			if (parameter2In.getWidth() > 0) {
				if (parameter2In.hasToken(0)) {
					param2 = ((StringToken) parameter2In.get(0)).stringValue();
					xmlmetadata = xmlmetadata.replaceAll("_PARAM2_", param2);
				}
			}

			dataFilenameOut.send(0, new StringToken(_dfilename));
			metadataOut.send(0, new StringToken(xmlmetadata));

		} catch (Exception ex) {
			throw new IllegalActionException(this, ex.getMessage());
		}
	}

	/**
	 *@return boolean
	 *@exception IllegalActionException
	 *                If the superclass throws it.
	 */
	public boolean prefire() throws IllegalActionException {
		return true;
	}

	private String getDefaultXMLMetadata() {
		String xml = "";
		xml = "<?xml version=\"1.0\"?>"
				+ "\n"
				+ "<eml:eml packageId=\"asdf.4.1\" system=\"knb\" xmlns:eml=\"eml://ecoinformatics.org/eml-2.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"eml://ecoinformatics.org/eml-2.0.0 eml.xsd\">"
				+ "\n"
				+ "<dataset>"
				+ "\n"
				+ "<title>Minimal Package with Data</title>"
				+ "\n"
				+ "<creator> <individualName><surName>Higgins</surName></individualName></creator>"
				+ "\n"
				+ "<abstract><para>This is an abstract of the package</para></abstract>"
				+ "\n"
				+ "<contact><individualName><surName>Higgins</surName></individualName></contact>"
				+ "\n"
				+ "<access authSystem=\"knb\" order=\"allowFirst\"><allow><principal>public</principal><permission>read</permission></allow></access>"
				+ "\n"
				+ "<dataTable id=\"1117749528859\">"
				+ "\n"
				+ "<entityName>TestTable</entityName>"
				+ "\n"
				+ "<physical><objectName>~deleteme28552.tmp</objectName>"
				+ "\n"
				+ "<dataFormat> <textFormat><attributeOrientation>column</attributeOrientation>"
				+ "\n"
				+ "<simpleDelimited><fieldDelimiter>#x09</fieldDelimiter>"
				+ "\n"
				+ "</simpleDelimited> </textFormat> </dataFormat>"
				+ "\n"
				+ "<distribution><online><url>ecogrid://knb/asdf.3.1</url></online></distribution>"
				+ "\n"
				+ "</physical>"
				+ "\n"
				+ "<attributeList><attribute id=\"1117749528890\"><attributeName>Atribute</attributeName>"
				+ "\n"
				+ "<attributeDefinition>Attibute definition</attributeDefinition>"
				+ "\n"
				+ "<measurementScale><ratio><unit><standardUnit>number</standardUnit>"
				+ "\n" + "</unit>" + "\n" + "<precision>1</precision>" + "\n"
				+ "<numericDomain><numberType>real</numberType>" + "\n"
				+ "</numericDomain>" + "\n" + "</ratio>" + "\n"
				+ "</measurementScale>" + "\n" + "</attribute>" + "\n"
				+ "</attributeList>" + "\n"
				+ "</dataTable> </dataset></eml:eml>";

		return xml;
	}
}