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

package org.ecoinformatics.seek.ecogrid;

import org.ecoinformatics.seek.ecogrid.exception.UnrecognizedDocumentTypeException;

/**
 * This class represents a selected document type
 * 
 * @author Jing Tao
 * 
 */
public class SelectableDocumentType extends DocumentType implements
		SelectableObjectInterface {

	private boolean isSelected = SelectableObjectInterface.DEFAULTSELECTIONSTATUS;
	private boolean isEnable = SelectableObjectInterface.ENABLE;

	/**
	 * Default constructor
	 */
	public SelectableDocumentType(String namespace, String label,
			boolean isSelected) throws UnrecognizedDocumentTypeException {
		super(namespace, label);
		this.isSelected = isSelected;
	}// SelectedDocumentType

	/**
	 * Copy constructor
	 * 
	 * @param documentType
	 *            DocumentType
	 * @param isSelected
	 *            boolean
	 */
	public SelectableDocumentType(DocumentType documentType, boolean isSelected) {
		super(documentType);
		this.isSelected = isSelected;
	}

	/**
	 * Method to get selected status
	 * 
	 * @return boolean
	 */
	public boolean getIsSelected() {
		return this.isSelected;
	}// getIsSelected

	/**
	 * Method to set selected status
	 * 
	 * @param isSelected
	 *            boolean
	 */
	public void setIsSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}// setIsSelected

	/**
	 * This method come from SelecteObjectInterface. In this case it will return
	 * label. If label is null, return namesapce itself
	 * 
	 * @return String
	 */
	public String getSelectableObjectLabel() {
		if (super.getLabel() != null) {
			return super.getLabel();
		} else {
			return super.getNamespace();
		}
	}// getSelectedObjectLabel

	/**
	 * Method to get icon enable status
	 * 
	 * @return boolean
	 */
	public boolean getEnabled() {
		return this.isEnable;
	}

	/**
	 * Method to set icon enable status
	 * 
	 * @param isEnable
	 *            boolean
	 */
	public void setEnabled(boolean isEnable) {
		this.isEnable = isEnable;
	}

}// SelectedDocumentType