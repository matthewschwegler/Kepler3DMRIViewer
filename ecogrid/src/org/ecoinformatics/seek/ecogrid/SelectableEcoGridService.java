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

import java.util.Vector;

/**
 * This class extends from EcoGridService, it contains selected status from user
 * interface
 * 
 * @author Jing Tao
 * 
 */

public class SelectableEcoGridService extends EcoGridService {

	private SelectableServiceName selectableServiceName = null;
	private SelectableDocumentType[] selectableDcoumentTypeList = null;// inlcude
	// selected and unselected
	private SelectableDocumentType[] selectedDocumentTypeList = null;// only

	// selected part
	/**
	 * Default constructor and inits selected service name and document type
	 * array
	 */
	public SelectableEcoGridService() {
		super();
		initSeletableServiceName();
		initSelectableDocumentTypeList();
	}// SelectedEcoGridService

	/**
	 * Copy Constructor, copy a EcoGridService and inited selected service name
	 * and selected document type array
	 * 
	 * @param myService
	 *            EcoGridService
	 */
	public SelectableEcoGridService(EcoGridService myService) {
		super(myService);
		initSeletableServiceName();
		initSelectableDocumentTypeList();
	}

	/*
	 * Method to initial Seletect service name
	 */
	private void initSeletableServiceName() {
		selectableServiceName = new SelectableServiceName();
		selectableServiceName.setServiceName(super.getServiceName());
		selectableServiceName
				.setIsSelected(SelectableObjectInterface.DEFAULTSELECTIONSTATUS);
	}// initSelectedServiceName

	/*
	 * Method to initial select document type array
	 */
	private void initSelectableDocumentTypeList() {
		DocumentType[] documentTypeList = super.getDocumentTypeList();
		if (documentTypeList != null) {
			int length = documentTypeList.length;
			selectableDcoumentTypeList = new SelectableDocumentType[length];
			for (int i = 0; i < length; i++) {
				DocumentType documentType = documentTypeList[i];
				SelectableDocumentType selectedDcoumetType = new SelectableDocumentType(
						documentType, this.getSelectableServiceName()
								.getIsSelected()); // inherit from service
													// selection
				// SelectableObjectInterface.DEFAULTSELECTIONSTATUS);
				selectableDcoumentTypeList[i] = selectedDcoumetType;
			}
		}// if
	}// initSeletedCoumentTypeList

	/**
	 * Method to get selected service name
	 * 
	 * @return SelectedServiceName
	 */
	public SelectableServiceName getSelectableServiceName() {
		return this.selectableServiceName;
	}// getSelectedServieName

	/**
	 * Method to set selectable service name(both selected and unselected)
	 * 
	 * @param selectedServiceName
	 *            SelectedServiceName
	 */
	public void setSelectableServiceName(
			SelectableServiceName selectedServiceName) {
		this.selectableServiceName = selectedServiceName;
	}// setSelectedServiceName

	/**
	 * Method to get selectable DocumentType list(both selected and unselected)
	 * 
	 * @return SelectedDocumentType[]
	 */
	public SelectableDocumentType[] getSelectableDocumentTypeList() {
		return this.selectableDcoumentTypeList;
	}// getSelectedDocumentTypeList

	/**
	 * Method to set selected docuemnt type list(
	 * 
	 * @param selectedDocumentTypeList
	 *            SelectedDocumentType[]
	 */
	public void setSelectedDocumentTypeList(
			SelectableDocumentType[] selectedDocumentTypeList) {
		this.selectedDocumentTypeList = selectedDocumentTypeList;
	}// setSelectedDocumentTypeList

	/**
	 * Method to get selectable DocumentType list(only selected)
	 * 
	 * @return SelectedDocumentType[]
	 */
	public SelectableDocumentType[] getSelectedDocumentTypeList() {
		if (selectableDcoumentTypeList != null) {
			int length = selectableDcoumentTypeList.length;
			Vector docList = new Vector();
			for (int i = 0; i < length; i++) {
				SelectableDocumentType type = selectableDcoumentTypeList[i];
				if (type.getIsSelected()) {
					docList.add(type);
				}
			}
			int size = docList.size();
			selectedDocumentTypeList = new SelectableDocumentType[size];
			for (int j = 0; j < size; j++) {
				selectedDocumentTypeList[j] = (SelectableDocumentType) docList
						.elementAt(j);
			}
		}
		return this.selectedDocumentTypeList;
	}// getSelectedDocumentTypeList

	/**
	 * Method to add a selected document type into list
	 * 
	 * @param selectedDocumentType
	 */
	public void addSelectedDocumentType(SelectableDocumentType type) {
		int size = 0;
		if (selectedDocumentTypeList != null) {
			size = selectedDocumentTypeList.length;
		}
		SelectableDocumentType[] list = new SelectableDocumentType[size + 1];
		for (int i = 0; i < size; i++) {
			list[i] = selectedDocumentTypeList[i];
		}
		list[size] = type;
		selectedDocumentTypeList = list;
	}

	/**
	 * Method to set selectable docuemnt type list
	 * 
	 * @param selectedDocumentTypeList
	 *            SelectedDocumentType[]
	 */
	public void setSelectableDocumentTypeList(
			SelectableDocumentType[] selectedDocumentTypeList) {
		this.selectableDcoumentTypeList = selectedDocumentTypeList;
	}// setSelectedDocumentTypeList

	/**
	 * Method to transfer a vecotr of ecogrid service to default selected
	 * ecogrid service vector. This is a utility method
	 * 
	 * @param serviceVector
	 *            Vector
	 * @return Vector
	 */
	public static Vector transferServiceVectToDefaultSelectedServiceVect(
			Vector serviceVector) {
		Vector selectedServiceVector = new Vector();
		if (serviceVector == null) {
			return selectedServiceVector;
		}// if
		int size = serviceVector.size();
		for (int i = 0; i < size; i++) {
			EcoGridService service = (EcoGridService) serviceVector
					.elementAt(i);
			SelectableEcoGridService selectedService = new SelectableEcoGridService(
					service);
			DocumentType[] docList = service.getDocumentTypeList();
			if (docList != null) {
				int length = docList.length;
				SelectableDocumentType[] selectableDocList = new SelectableDocumentType[length];
				for (int j = 0; j < length; j++) {
					DocumentType type = docList[j];
					String namespace = type.getNamespace();
					String label = type.getLabel();
					boolean isSelected = SelectableObjectInterface.DEFAULTSELECTIONSTATUS;
					try {
						SelectableDocumentType newDoc = new SelectableDocumentType(
								namespace, label, isSelected);
						selectableDocList[j] = newDoc;
					} catch (Exception e) {
						continue;
					}

				}
				selectedService
						.setSelectableDocumentTypeList(selectableDocList);
				selectedService.setSelectedDocumentTypeList(selectableDocList);
			}
			selectedServiceVector.add(selectedService);
		}// for
		return selectedServiceVector;
	}// transferServiceVectToDefaultSelectedServiceVect

	/**
	 * This method will copy a ecogrid service to another one. The difference to
	 * the copy constructor is, it creates a new array for document type
	 * 
	 * @param oldService
	 *            EcoGridService
	 * @return EcoGridService
	 */
	public static SelectableEcoGridService copySelectableEcoGridService(
			SelectableEcoGridService oldService) throws Exception {

		SelectableEcoGridService newService = new SelectableEcoGridService();
		SelectableServiceName selectableServiceName = oldService
				.getSelectableServiceName();
		String serviceName = selectableServiceName.getServiceName();
		boolean selected = selectableServiceName.getIsSelected();
		SelectableServiceName newSelectableServiceName = new SelectableServiceName();
		newSelectableServiceName.setServiceName(serviceName);
		newSelectableServiceName.setIsSelected(selected);
		newService.setSelectableServiceName(newSelectableServiceName);
		String serviceType = oldService.getServiceType();
		newService.setServiceType(serviceType);
		newService.setServiceGroup(oldService.getServiceGroup());
		String endpoint = oldService.getEndPoint();
		newService.setEndPoint(endpoint);
		SelectableDocumentType[] oldArray = oldService
				.getSelectableDocumentTypeList();
		if (oldArray != null) {
			int length = oldArray.length;
			SelectableDocumentType[] newArray = new SelectableDocumentType[length];
			for (int i = 0; i < length; i++) {
				SelectableDocumentType oldDoc = oldArray[i];
				String namespace = oldDoc.getNamespace();
				String label = oldDoc.getLabel();
				boolean isSelected = oldDoc.getIsSelected();
				SelectableDocumentType newDoc = new SelectableDocumentType(
						namespace, label, isSelected);
				newArray[i] = newDoc;
			}// for
			newService.setSelectableDocumentTypeList(newArray);
		}// if
		return newService;
	}// copyEcoGridService

}// SelectedEcoGridService