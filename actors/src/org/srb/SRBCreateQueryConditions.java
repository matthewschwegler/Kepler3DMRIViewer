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

package org.srb;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// SRBCreateQueryConditions
/**
 * <p>
 * Creates an array of query conditions from user selections. Thereby
 * translating user selcted conditions into an array of strings. Creates
 * conditions for querying the SRB metadata from a user xml string conditions,
 * returned by the BrowserUI actor.
 * </p>
 * <p>
 * The SRBCreateQueryInterface outputs an HTML document with the appropriate
 * conditions in HTML template form. The HTML document's file content can be
 * further viewed by the BrowserUI actor. The BrowserUI outputs the XML output
 * form of the above HTML document. This XML output can be further given to the
 * SRBCreateQueryConditions actor to create an array of string conditions.
 * </p>
 * 
 * @author Efrat Jaeger
 * @version $Id: SRBCreateQueryConditions.java 13429 2007-02-01 20:18:02Z
 *          berkley $
 * @category.name srb
 * @category.name put
 */

public class SRBCreateQueryConditions extends TypedAtomicActor {

	/**
	 * Construct a constant source with the given container and name. Create the
	 * <i>value</i> parameter, initialize its value to the default value of an
	 * IntToken with value 1.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public SRBCreateQueryConditions(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		xmlConditions = new TypedIOPort(this, "xmlConditions", true, false);
		xmlConditions.setTypeEquals(BaseType.STRING);
		new Attribute(xmlConditions, "_showName");

		conditions = new TypedIOPort(this, "conditions", false, true);
		conditions.setTypeEquals(new ArrayType(BaseType.STRING));
		new Attribute(conditions, "_showName");

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"30\" " + "style=\"fill:white\"/>\n"
				+ "<text x=\"4\" y=\"20\""
				+ "style=\"font-size:16; fill:blue; font-family:SansSerif\">"
				+ "[SRB]</text>\n" + "<text x=\"45\" y=\"22\""
				+ "style=\"font-size:20; fill:blue; font-family:SansSerif\">"
				+ "$</text>\n" + "</svg>\n");
	}

	/**
	 * The xml conditions obtained from user selection.
	 */
	public TypedIOPort xmlConditions;

	/**
	 * An array of conditions strings.
	 */
	public TypedIOPort conditions;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////
	/**
	 * Translate user selcted conditions into an array of strings.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown if the SRB file cannot be accessed or the
	 *                current directory cannot be broadcasted.
	 */
	public void fire() throws IllegalActionException {

		try {
			String xmlStr = ((StringToken) xmlConditions.get(0)).stringValue();
			Map nameVal = new HashMap();
			int d_numAtts = 0, c_numAtts = 0;
			while (!xmlStr.trim().equals("</xmp>")) {

				int indStart = xmlStr.toLowerCase().indexOf("<name>");
				int indEnd = xmlStr.toLowerCase().indexOf("</name>");
				String name = xmlStr.substring(indStart + 6, indEnd);
				xmlStr = xmlStr.substring(indEnd + 7);
				indStart = xmlStr.toLowerCase().indexOf("<value>");
				indEnd = xmlStr.toLowerCase().indexOf("</value>");
				String value = xmlStr.substring(indStart + 7, indEnd);
				xmlStr = xmlStr.substring(indEnd + 8);
				if (name.startsWith("d_att"))
					d_numAtts++;
				else if (name.startsWith("c_att"))
					c_numAtts++;
				nameVal.put(name, value);
			}
			Vector condsVec = new Vector();
			for (int i = 0; i < d_numAtts; i++) {
				String att = (String) nameVal.get("d_att" + i);
				String op = (String) nameVal.get("d_op" + i);
				String val = (String) nameVal.get("d_newmdval" + i);
				if (!val.equals("")) {
					if (op.equals("")) // op is empty - assume equals.
						op = "=";
					String condition = att + " " + op + " " + val;
					condsVec.add(new StringToken(condition));
				}
			}

			condsVec.add(new StringToken("|")); // separating between dataset
												// and collection metadata.

			for (int i = 0; i < c_numAtts; i++) {
				String att = (String) nameVal.get("c_att" + i);
				String op = (String) nameVal.get("c_op" + i);
				String val = (String) nameVal.get("c_newmdval" + i);
				if (!val.equals("")) {
					if (op.equals("")) // op is empty - assume equals.
						op = "=";
					String condition = att + " " + op + " " + val;
					condsVec.add(new StringToken(condition));
				}
			}

			condsVec.add(new StringToken("|")); // separating between user
												// defined metadata and
												// predefined.

			String mcatVal = (String) nameVal.get("Annotation");
			if (!mcatVal.equals("")) {
				String mcatOp = (String) nameVal.get("AnnotationOp");
				String condition = "Annotation " + mcatOp + " " + mcatVal;
				condsVec.add(new StringToken(condition));
			}
			mcatVal = (String) nameVal.get("Annotator");
			if (!mcatVal.equals("")) {
				String mcatOp = (String) nameVal.get("AnnotatorOp");
				String condition = "Annotator " + mcatOp + " " + mcatVal;
				condsVec.add(new StringToken(condition));
			}
			mcatVal = (String) nameVal.get("Owner");
			if (!mcatVal.equals("")) {
				String mcatOp = (String) nameVal.get("OwnerOp");
				String condition = "Owner " + mcatOp + " " + mcatVal;
				condsVec.add(new StringToken(condition));
			}
			mcatVal = (String) nameVal.get("dataName");
			if (!mcatVal.equals("")) {
				String mcatOp = (String) nameVal.get("dataNameOp");
				String condition = "dataName " + mcatOp + " " + mcatVal;
				condsVec.add(new StringToken(condition));
			}
			mcatVal = (String) nameVal.get("collName");
			if (!mcatVal.equals("")) {
				String mcatOp = (String) nameVal.get("collNameOp");
				String condition = "collName " + mcatOp + " " + mcatVal;
				condsVec.add(new StringToken(condition));
			}
			mcatVal = (String) nameVal.get("dataType");
			if (!mcatVal.equals("")) {
				String mcatOp = (String) nameVal.get("dataTypeOp");
				String condition = "dataType " + mcatOp + " " + mcatVal;
				condsVec.add(new StringToken(condition));
			}

			Token[] condTokens = new Token[condsVec.size()];
			condsVec.toArray(condTokens);
			conditions.broadcast(new ArrayToken(condTokens));
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new IllegalActionException(this,
					"Failed to create query condition: " + ex.getMessage()
							+ ".");
		}

	}
}