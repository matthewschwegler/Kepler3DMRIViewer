/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
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

package org.kepler.objectmanager.data.text;

import org.kepler.objectmanager.data.DataType;
import org.kepler.objectmanager.data.DataTypeResolver;
import org.kepler.objectmanager.data.Domain;
import org.kepler.objectmanager.data.UnresolvableTypeException;

/**
 * @author tao This class will store info for text domain type
 */
public class TextDomain implements Domain {
	private String numberType;
	private DataType dataType;
	private DataTypeResolver resolver = DataTypeResolver.instanceOf();
	private String definition;
	private String[] pattern;
	private String source;

	/**
	 * Constructor for text domain
	 * 
	 * @throws UnresolvableTypeException
	 */
	public TextDomain() throws UnresolvableTypeException {
		numberType = DataType.STRINGTYPE;
		dataType = resolver.resolveDataType(numberType, null, null);
	}

	/**
	 * Method to get DataType
	 */
	public DataType getDataType() {
		return dataType;
	}

	/**
	 * @return Returns the definition.
	 */
	public String getDefinition() {
		return definition;
	}

	/**
	 * @param definition
	 *            The definition to set.
	 */
	public void setDefinition(String definition) {
		this.definition = definition;
	}

	/**
	 * @return Returns the pattern.
	 */
	public String[] getPattern() {
		return pattern;
	}

	/**
	 * @param pattern
	 *            The pattern to set.
	 */
	public void setPattern(String[] pattern) {
		this.pattern = pattern;
	}

	/**
	 * @return Returns the source.
	 */
	public String getSource() {
		return source;
	}

	/**
	 * @param source
	 *            The source to set.
	 */
	public void setSource(String source) {
		this.source = source;
	}
}