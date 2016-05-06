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

package org.ecoinformatics.seek.sms;

/**
 *@author    Shawn Bowers
 *
 *  A quick hack at a start to a local Kepler LSID suite of services
 *  for kepler.  This ultimately is an implementation of each
 *  LSIDxyzService, but is local to Kepler. Not sure how all of this
 *  will eventually work ...
 *
 *  This class locally maintains the default actor library. The actor
 *  library is passed at start up; and a hash table is created of all
 *  named objects in the library.  New objects can be added, etc.
 *  Each new addition or change should update default actor library,
 *  persisting the change locally.
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.kepler.moml.NamedObjId;

import ptolemy.actor.lib.Const;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.NamedObj;
import ptolemy.moml.EntityLibrary;
import ptolemy.vergil.tree.EntityTreeModel;
import ptolemy.vergil.tree.VisibleTreeModel;

/*
 *  KeplerLocalLSIDService Interface
 *  {
 *  // singleton
 *
 *  KeplerLocalLSIDService instance();
 *
 *  // initialization hacks, called from TabbedLibraryPane at setup
 *
 *  void setBasicActorLibrary(EntityTreeModel _libraryModel);
 *  EntityTreeModel getBasicActorLibrary();
 *
 *  // lsid ops
 *
 *  void assignLSID(String lsid, NamedObj obj);
 *  void updateLSID(String lsid, NamedObj obj);
 *  NamedObj getData(String lsid);
 *  Iterator<String> getLSIDFor(NamedObj obj)
 *
 *  // helper ops
 *
 *  String createLSID(String namespace, String id);
 *  String createLocallyUniqueLSID(String namespace);
 *  boolean isAssignedLSID(String lsid);
 *  boolean isWellFormedLSID(String lsid)
 *  Iterator<String> assignedLSIDs();
 *  void commitChanges()
 *  }
 */

/**
 * Description of the Class
 * 
 *@author berkley
 *@created February 17, 2005
 */
public class KeplerLocalLSIDService {

	/*
	 * PRIVATE ATTRIBUTES
	 */
	// singleton instance
	private static KeplerLocalLSIDService _service = null;

	// indexed, main-memory storage and management
	private Hashtable _assignedObjects = new Hashtable();
	// maps lsid -> namedObj
	private EntityTreeModel _libraryModel = null;
	// orig. lib. of actors
	private Vector _managedLsids = new Vector();
	// list of managed lsids; that have been assigned
	private Hashtable _lastDomainId = new Hashtable();
	// for finding new, unqiue domains

	// references to the database of actors
	private static String KEPLER = System.getProperty("KEPLER");
	private static String KEPLERACTORLIB = KEPLER
			+ "/configs/ptolemy/configs/kepler/basicKeplerActorLibrary.xml";

	// prefix for identifier
	private static String URN = "urn";
	private static String LSID = "lsid";
	private static String DOMAIN = "localhost";

	/*
	 * PROTECTED CONSTRUCTOR
	 */
	/** Constructor for the KeplerLocalLSIDService object */
	protected KeplerLocalLSIDService() {
		// construct a default library, prior to setBasicActorLibrary
		_libraryModel = new VisibleTreeModel(new CompositeEntity());
	}

	/*
	 * SINGLETON CONSTRUCTOR
	 */

	/**
	 *@return The unique instance of this class This must be called to
	 *         create/obtain an instance of the service
	 */
	public static KeplerLocalLSIDService instance() {
		if (_service == null) {
			_service = new KeplerLocalLSIDService();
		}
		return _service;
	}

	/*
	 * INITIALIZATION
	 */

	/**
	 * Assigns the current kepler actor library. Clears current set of lsid
	 * objects. This operation is here because there isn't a great way in
	 * Kepler/Ptolemy to access "global" objects. Here, we grab the library
	 * model that is initially given to Kepler when the graph editor starts up.
	 * 
	 *@param _libraryModel
	 *            The current kepler actor library
	 */
	public void setBasicActorLibrary(EntityTreeModel _libraryModel) {
		// clear the hashtable
		_assignedObjects = new Hashtable();
		// set the library model
		this._libraryModel = _libraryModel;
		// load up the hashtable
		getNamedObjIds(_libraryModel.getRoot());

	}

	/**
	 * This operation provides access to the assigned library model.
	 * 
	 *@return The current kepler actor library
	 */
	public EntityTreeModel getBasicActorLibrary() {
		return _libraryModel;
	}

	// .........
	// recursive helper function to traverse and pull out the named
	// objects with ids
	// .........
	/**
	 * Gets the namedObjIds attribute of the KeplerLocalLSIDService object
	 * 
	 *@param parent
	 *            Description of the Parameter
	 */
	private void getNamedObjIds(Object parent) {
		// check if the parent has an id, and if so add to hashtable
		if (parent instanceof NamedObj) {
			_addNamedObject((NamedObj) parent);
		}
		// iterator over children
		for (int i = 0; i < _libraryModel.getChildCount(parent); i++) {
			getNamedObjIds(_libraryModel.getChild(parent, i));
		}
	}

	/*
	 * MAIN LSID OPERATIONS
	 */

	/**
	 * Adds a named object to an lsid that hasn't been previously assigned. The
	 * lsid cannot be previously assigned to an object. To create a well-formed
	 * lsid, use createLSID.
	 * 
	 *@param obj
	 *            The object to assign an lsid to
	 *@param lsid
	 *            Description of the Parameter
	 *@exception IllegalLSIDAssignmentException
	 *                Description of the Exception
	 *@return true if the object was assigned to the lsid correctly, false
	 *          otherwise.
	 */
	public void assignLSID(String lsid, NamedObj obj)
			throws IllegalLSIDAssignmentException {
		// the lsid must not be assigned
		if (_assignedObjects.containsKey(lsid)) {
			throw new IllegalLSIDAssignmentException(lsid
					+ " already assigned to an object");
		}

		// make sure there isn't already an obj with the same entity name
		// to be assigned
		Enumeration e = _assignedObjects.elements();
		while (e.hasMoreElements()) {
			NamedObj assignedObj = (NamedObj) e.nextElement();
			String name = assignedObj.getName();
			if (name.equals(obj.getName())) {
				String msg = "Object name '" + obj.getName()
						+ "' already exists in library";
				throw new IllegalLSIDAssignmentException(msg);
			}
		}

		try {
			// add the id
			NamedObjId objId = new NamedObjId(obj, NamedObjId.NAME);
			objId.setContainer(obj);
			objId.setExpression(lsid);
			// update the assigned objects
			_assignedObjects.put(lsid, obj);
			if (!_managedLsids.contains(lsid)) {
				_managedLsids.add(lsid);
			}
		} catch (ptolemy.kernel.util.NameDuplicationException nde) {
			// don't add the entity id, but do add the lsid to the DB.
			_assignedObjects.put(lsid, obj);
			if (!_managedLsids.contains(lsid)) {
				_managedLsids.add(lsid);
			}
		} catch (Exception ex) {
			throw new IllegalLSIDAssignmentException(ex.toString());
		}
	}

	/**
	 * Updates a previously assigned lsid to be assigned the given object.
	 * Removes the old object from the actor library, and adds the given object.
	 * 
	 *@param lsid
	 *            the lsid to update
	 *@param obj
	 *            the new object to connect to the lsid
	 *@exception IllegalLSIDAssignmentException
	 *                Description of the Exception
	 */
	public void updateLSID(String lsid, NamedObj obj)
			throws IllegalLSIDAssignmentException {
		// the lsid must be assigned
		if (!_assignedObjects.containsKey(lsid)) {
			throw new IllegalLSIDAssignmentException(
					lsid
							+ " cannot be updated because it is not assigned to an object");
		}
		try {
			NamedObj oldObj = (NamedObj) _assignedObjects.get(lsid);
			if (oldObj instanceof Attribute) {
				((Attribute) oldObj).setContainer(null);
			} else if (oldObj instanceof ComponentEntity) {
				((ComponentEntity) oldObj).setContainer(null);
			}
			_assignedObjects.remove(lsid);
			// need to remove the object from the actor library!!!
			assignLSID(lsid, obj);
		} catch (Exception e) {
			throw new IllegalLSIDAssignmentException(e.toString());
		}
	}

	/*
	 * LSID HELPER OPERATIONS
	 */
	/**
	 *@param namespace
	 *            Description of the Parameter
	 *@param id
	 *            Description of the Parameter
	 *@exception IllegalLSIDException
	 *                Description of the Exception
	 *@return an lsid with the default domain and the given namespace and id
	 */
	public String createLSID(String namespace, String id)
			throws IllegalLSIDException {
		String lsid = URN + ":" + LSID + ":" + DOMAIN + ":" + namespace + ":"
				+ id;
		if (!isWellFormedLSID(lsid)) {
			throw new IllegalLSIDException(lsid);
		}
		return lsid;
	}

	/**
	 * This is a simple algorithm that computes a new id for a given namespace
	 * in the default domain. Appends a LOCALID to the id, so shouldn't be
	 * duplicated in other ids in the same domain.
	 * 
	 *@param namespace
	 *            Description of the Parameter
	 *@return a unqiue lsid, relevant to assigned lsids, for the given
	 *          namespace.
	 */
	public String createLocallyUniqueLSID(String namespace) {
		try {
			boolean found = false;
			Integer id = (Integer) _lastDomainId.get(namespace);
			if (id == null)
				id = new Integer(-1);
			int newId = id.intValue();
			while (!found) {
				newId = newId + 1;
				String lsid = createLSID(namespace, "LOCALID" + newId);
				if (!isAssignedLSID(lsid)) {
					found = true;
					if (_lastDomainId.contains(namespace)) {
						_lastDomainId.remove(namespace);
					}
					_lastDomainId.put(namespace, new Integer(newId));
				}
			}
			return createLSID(namespace, "LOCALID" + newId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 *@param obj
	 *            Description of the Parameter
	 *@return The lSIDFor value
	 */
	public Iterator getLSIDFor(NamedObj obj) {
		Vector result = new Vector();

		if (_assignedObjects.containsValue(obj)) {
			Enumeration keys = _assignedObjects.keys();
			// for each key, check if the associated object equals
			// this object and if so, add to result
			while (keys.hasMoreElements()) {
				String lsid = (String) keys.nextElement();
				NamedObj tmp = (NamedObj) _assignedObjects.get(lsid);
				if (tmp.equals(obj)) {
					result.add(lsid);
				}
			}
		}
		return result.iterator();
	}

	/**
	 *@param lsid
	 *            the lsid to test
	 *@return true if the given lsid is known to the service
	 */
	public boolean isAssignedLSID(String lsid) {
		return _managedLsids.contains(lsid);
	}

	/**
	 *@param lsid
	 *            the lsid to test
	 *@return true if the lsid is well formed, false otherwise
	 */
	public boolean isWellFormedLSID(String lsid) {
		String delim = ":";
		StringTokenizer strTok = new StringTokenizer(lsid, delim, false);
		String str = "";
		try {
			// urn part
			str = strTok.nextToken();
			if (!str.equals(URN)) {
				return false;
			}
			// lsid
			str = strTok.nextToken();
			if (!str.equals(LSID)) {
				return false;
			}
			// the domain
			str = strTok.nextToken();
			if (containsWhiteSpace(str)) {
				return false;
			}
			// the namespace
			str = strTok.nextToken();
			if (containsWhiteSpace(str)) {
				return false;
			}
			// the id
			str = strTok.nextToken();
			if (containsWhiteSpace(str)) {
				return false;
			}
			// the optional version
			if (strTok.hasMoreTokens()
					&& containsWhiteSpace(strTok.nextToken())) {
				return false;
			}
			// no more tokens
			if (strTok.hasMoreTokens()) {
				return false;
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Description of the Method
	 * 
	 *@param str
	 *            Description of the Parameter
	 *@return Description of the Return Value
	 */
	private boolean containsWhiteSpace(String str) {
		StringTokenizer strTok = new StringTokenizer(str);
		return (strTok.countTokens() != 1);
	}

	/**
	 *@param lsid
	 *            Description of the Parameter
	 *@return null if not a resolvable lsid (i.e., unknown lsid)
	 */
	public NamedObj getData(String lsid) {
		return (NamedObj) _assignedObjects.get(lsid);
	}

	/**
	 * Adds a new NamedObj instance to the engine
	 * 
	 *@param obj
	 *            the instance to add, which must have an NamedObjId property
	 *@return true if the item was added successfully
	 */
	private boolean _addNamedObject(NamedObj obj) {
		List idAtts = obj.attributeList(NamedObjId.class);
		Iterator iter = idAtts.iterator();
		boolean result = iter.hasNext();
		while (iter.hasNext()) {
			NamedObjId id = (NamedObjId) iter.next();
			// check if it is already there ...
			if (!_assignedObjects.containsKey(id.getExpression())) {
				String lsid = id.getExpression();
				if (!_managedLsids.contains(lsid)) {
					_managedLsids.add(lsid);
				}
				_assignedObjects.put(lsid, obj);
			}
		}
		return result;
	}

	/** TODO: throw error */
	public void commitChanges() {

		try {
			// open the KEPLERACTORLIB file
			File file = new File(KEPLERACTORLIB);
			BufferedWriter output = new BufferedWriter(new FileWriter(file));

			// write out the header of the file
			output.write("<?xml version=\"1.0\" standalone=\"no\"?>\n");
			output
					.write("<!DOCTYPE plot PUBLIC \"-//UC Berkeley//DTD MoML 1//EN\"\n");
			output
					.write("\"http://ptolemy.eecs.berkeley.edu/xml/dtd/MoML_1.dtd\">\n");
			output.write("<group>\n");

			// add each unique item in the hashtable to root
			Vector objsToAdd = new Vector();
			Enumeration e = _assignedObjects.elements();
			while (e.hasMoreElements()) {
				NamedObj obj = (NamedObj) e.nextElement();
				if (!objsToAdd.contains(obj)) {
					objsToAdd.add(obj);
				}
			}

			for (Iterator i = objsToAdd.iterator(); i.hasNext();) {
				NamedObj obj = (NamedObj) i.next();
				output.write("\n" + obj.exportMoML() + "\n");
			}
			output.write("</group>\n");

			// close the file
			output.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Description of the Method
	 * 
	 *@return Description of the Return Value
	 */
	public Iterator assignedLSIDs() {
		return _managedLsids.iterator();
	}

	/*
	 * Testing
	 */

	// for testing
	/**
	 * The main program for the KeplerLocalLSIDService class
	 * 
	 *@param args
	 *            The command line arguments
	 */
	public static void main(String[] args) {
		KeplerLocalLSIDService serv = KeplerLocalLSIDService.instance();
		try {
			System.out.println(">>> Assigning const");
			String lsid = serv.createLSID("bowers", "myFavoriteService");
			Const c = new Const(new EntityLibrary(), "myConst");
			serv.assignLSID(lsid, c);

			System.out.println(">>> Assigning another const");
			String lsid0 = serv.createLSID("bowers", "myOtherFavoriteService");
			System.out.println(">>> Updating const");
			Const c0 = new Const(new EntityLibrary(), "myFufuConst");
			serv.updateLSID(lsid, c0);

			serv.commitChanges();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}