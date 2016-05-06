/*
 * Copyright (c) 2010 The Regents of the University of California.
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

package org.kepler.sms;

/**
 * Created by IntelliJ IDEA.
 * User: sean
 * Date: Jun 2, 2009
 * Time: 3:00:12 PM
 */
public class OntologyRow {
	
	public OntologyRow() {
		
	}
	
	public OntologyRow(NamedOntModel model) {
		this();
		setModel(model);
	}
	
	public OntologyRow(NamedOntModel model, boolean inLibrary, boolean inTagBar, Color color, boolean isLocal) {
		this();
		setModel(model);
		setInLibrary(inLibrary);
		setInTagBar(inTagBar);
		setColor(color);
		setLocal(isLocal);
	}

	public NamedOntModel getModel() {
		return model;
	}

	public void setModel(NamedOntModel model) {
		this.model = model;
	}

	private NamedOntModel model;

	public boolean isInLibrary() {
		return inLibrary;
	}

	public void setInLibrary(boolean inLibrary) {
		this.inLibrary = inLibrary;
	}

	public boolean isInTagBar() {
		return inTagBar;
	}
	
	public void setInTagBar(boolean inTagBar) {
		this.inTagBar = inTagBar;
	}
	
	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}	
	
	private boolean inLibrary;
	private boolean inTagBar;
	private Color color;
	private boolean local;

	public boolean isLocal() {
		return local;
	}

	public void setLocal(boolean local) {
		this.local = local;
	}
}
