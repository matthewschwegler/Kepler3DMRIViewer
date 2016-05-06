/*
 * Copyright (c) 2005-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-08-09 15:47:01 -0700 (Thu, 09 Aug 2012) $' 
 * '$Revision: 30395 $'
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

package org.kepler.sms;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Settable;

/**
 * A data structure to hold a semantic type as a concept id in a MoML file.
 * 
 *@author Shawn Bowers
 *@created May 9, 2005
 */

/**
 * This is a subtype of StringParameter. All the data is stored through the
 * StringParameter.setExpression() call, as a single string. This string is
 * interpreted as follows:
 * 
 * If form is A#B#C, then A is the ontology URI, B is the concept ID, and C is
 * the concept label
 * else if form is A#B, then A is the ontology URI, B is the concept ID
 * else if form is A, then A is the concept ID
 */

public class SemanticType extends StringParameter {
	// the main value of the property (the class name)

	public SemanticType() throws IllegalActionException,
			NameDuplicationException {
            // super(null, null);
            super(new NamedObj(), "semtype");
	}

	/**
	 * Constructor for the SemanticTypeParam object
	 * 
	 *@param container
	 *            Description of the Parameter
	 *@param name
	 *            Description of the Parameter
	 *@exception IllegalActionException
	 *                Description of the Exception
	 *@exception NameDuplicationException
	 *                Description of the Exception
	 */
	public SemanticType(NamedObj container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
		setExpression("urn:lsid:localhost:onto:2:1#Workflow");
        setVisibility(Settable.NONE);
	}
	
	@Override
	public void setExpression(String string) {
		super.setExpression(string);
		// NOTE: It is difficult to have this called directly and maintain
		// class invariants.
		log.debug("SemanticType.setExpression() should probably not be called directly. Use the appropriate setXXX() methods.");
//		writeOutStackTrace();
		_setFieldsFromRawExpression(string);
	}

	public void writeOutStackTrace() {
		try {
			tryWriteOutStackTrace();
		}
		catch(IOException ex) {
			log.error("Error writing out stack trace", ex);
		}
	}
	private void tryWriteOutStackTrace() throws IOException {
		File file;
		BufferedWriter writer;
		try {
			throw new Exception();
		}
		catch(Exception ex) {
			StackTraceElement[] stackTrace = ex.getStackTrace();
			if (!isGoodTrace(stackTrace)) {
				log.debug("Not writing out this trace");
				return;
			}
			file = File.createTempFile("exception", ".txt");
			writer = new BufferedWriter(new FileWriter(file));
			for (StackTraceElement element : ex.getStackTrace()) {
				writer.write(element.toString());
				writer.write("\n");
			}
		}
		writer.close();
		log.info("Wrote stack trace to: " + file.getAbsolutePath());
	}

	private static boolean isGoodTrace(StackTraceElement[] stackTrace) {
		for (StackTraceElement ste : stackTrace) {
			if ("org.kepler.sms.SemanticType".equals(ste.getClassName()) && ste.getLineNumber() == 75) {
				return false;
			}
		}
		return true;
	}

	private void _setFieldsFromRawExpression(String string) {
		String[] parts = string.split("#");
		if (parts.length == 1) {
			_conceptName = parts[0];
			_namespace = null;
			_label = null;
		}
		else {
			_namespace = parts[0];
			_conceptName = parts[1];
			try {
				_label = parts[2];
			}
			catch(ArrayIndexOutOfBoundsException ex) {
				_label = null;
			}
		}
	}
	
	private void _setExpressionFromFields() {
		StringBuilder builder = new StringBuilder();
		if (_namespace != null) {
			builder.append(_namespace).append("#");
		}
		if (_conceptName != null) {
			builder.append(_conceptName).append("#");
		}
		if (_label != null) {
			builder.append(_label);
		}
		super.setExpression(builder.toString());
	}

	/**
	 * Description of the Method
	 * 
	 *@param obj
	 *            Description of the Parameter
	 *@return Description of the Return Value
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof SemanticType)) {
			return false;
		}
		SemanticType semtypeId = (SemanticType) obj;
		String str = semtypeId.getExpression();
		if (this.getExpression() == null) {
			return str == null;
		}
		String[] parts = getExpression().split("#");
		if (parts.length < 3) {
			return this.getExpression().equals(semtypeId.getExpression());
		}
		String[] otherParts = semtypeId.getExpression().split("#");
		
		if (parts.length < 2 || otherParts.length < 2) {
			System.out.println("This should not happen!");
		}
		
		// Compare up to but not including the third section of these strings
		for (int i = 0; i < 2; i++) {
			if (!parts[i].equals(otherParts[i])) {
				return false;
			}
		}
		
		return true;		
 	}
	
	/**
	 * return the value of the semantic type
	 * 
	 *@return The concept id
	 */
	public String getConceptId() {
		return getExpression();
	}
	

	public String getConceptUri() {
		return getNamespace() + "#" + getConceptName();	
	}
	

	/**
	 * set the semantic type concept value
	 * 
	 *@param expr
	 *            The new conceptId value
	 *@exception ptolemy.kernel.util.IllegalActionException
	 *                Description of the Exception
	 */
	public void setConceptId(String expr)
			throws ptolemy.kernel.util.IllegalActionException {
		setExpression(expr);
	}

	public String getLabel() {
		if (_label != null) {
			return _label;
		}
		return _conceptName;
	}
	
	/**
	 * Gets the namespace attribute of the SemanticTypeParam object
	 * 
	 *@return The namespace value
	 */
	public String getNamespace() {
		return _namespace;
	}

	/**
	 * Gets the conceptName attribute of the SemanticTypeParam object
	 * 
	 *@return The conceptName value
	 */
	public String getConceptName() {
		return _conceptName;
	}
	
	public void setLabel(String label) {
		_label = label;
		_setExpressionFromFields();
	}
	
	public void setNamespace(String namespace) {
		_namespace = namespace;
		_setExpressionFromFields();
	}
	
	public void setConceptName(String conceptName) {
		_conceptName = conceptName;
		_setExpressionFromFields();
	}

//	public void setRemovable(boolean removable) {
//		this._removable = removable;
//	}
//	
//	public boolean isRemovable() {
//		return this._removable;
//	}
	
	private String _conceptName;
	private String _namespace;
	private String _label;
//	private boolean _removable = true;
	private static final Log log = LogFactory.getLog(SemanticType.class);
	
}