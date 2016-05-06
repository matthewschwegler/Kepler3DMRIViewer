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

package org.kepler.sms.gui;

/**
 * A semantic-type browser selection event in which a class was
 * selected.
 */

import java.util.EventObject;

import org.kepler.sms.NamedOntClass;

public class SemanticTypeBrowserSelectionEvent extends EventObject {

	/**
	 * creates a new event
	 * 
	 * @param source
	 *            The source of the event
	 * @param cls
	 *            A NamedOntClasses ont
	 */
	public SemanticTypeBrowserSelectionEvent(Object source, NamedOntClass cls) {
		super(source);
		_source = source;
		_cls = cls;
	}

	/**
	 * @return A list of NamedOntClasses
	 */
	public NamedOntClass getOntClass() {
		return _cls;
	}

	public String toString() {
		return "[ontClass = " + _cls + "; source = " + _source + "]";
	}

	public Object getSource() {
		return _source;
	}

	/* private members */

	private Object _source;
	private NamedOntClass _cls;

} // SemanticTypeBrowserSelectionEvent