/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:21:34 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31119 $'
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

package org.kepler.moml;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.kepler.objectmanager.lsid.LSIDGenerator;

import ptolemy.actor.gui.RenameConfigurer;
import ptolemy.kernel.undo.RedoChangeRequest;
import ptolemy.kernel.undo.UndoChangeRequest;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.ChangeListener;
import ptolemy.kernel.util.ChangeRequest;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Settable;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.kernel.util.ValueListener;
import ptolemy.kernel.util.Workspace;
import ptolemy.moml.MoMLChangeRequest;
import ptolemy.util.MessageHandler;
import ptolemy.vergil.actor.ActorGraphModel;
import ptolemy.vergil.basic.LocatableNodeDragInteractor;

/**
 * The NamedObjId is a StringAttribute to be used for identifying a NamedObj by
 * a KeplerLSID. A NamedObj that contains this Attribute is considered to be
 * uniquely identified by the KeplerLSID that this NamedObjId stores. This class
 * also handles the updating of KeplerLSIDs based on the ptolemy change listener
 * system. Static methods in org.kepler.objectmanager.ObjectManager can be used
 * to access NamedObjId attributes contained by NamedObjs.
 * 
 *@author berkley, schultz
 *@created March 1, 2005
 */
public class NamedObjId extends StringAttribute implements ChangeListener {
	private static final long serialVersionUID = 3665319359405551002L;
	private static final Log log = LogFactory
			.getLog(NamedObjId.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();
	
	/** If true, new NamedObjId objects will increment the LSID when changes
	 *  to the workflow occur.
	 */
    private static boolean _incrementLSIDOnWorkflowChange = true;

	/** container for valueListeners assigned to this attribute */
	private Vector<ValueListener> valueListeners = new Vector<ValueListener>();

	private KeplerLSID _id = null;

	public static final String NAME = "entityId";
	
	/* A parent NamedObjId for this NamedObjId
	 * The parent will always be updated when this one is.
	 */
	private NamedObjId _parentId = null;

	public NamedObjId getParentId() {
		return _parentId;
	}

	public void setParentId(NamedObjId parent) {
		_parentId = parent;
	}

	/** Constructor */
	public NamedObjId() {
		super();
		if(_incrementLSIDOnWorkflowChange) {
		    addChangeListener(this);
		}
	}

	/**
	 * Constructor
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
	public NamedObjId(NamedObj container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
        if(_incrementLSIDOnWorkflowChange) {
            addChangeListener(this);
        }
	}

	/**
	 * Constructor
	 * 
	 *@param workspace
	 *            Description of the Parameter
	 */
	public NamedObjId(Workspace workspace) {
		super(workspace);
        if(_incrementLSIDOnWorkflowChange) {
            addChangeListener(this);
        }
	}
	
	/**
	 * Generate a new KeplerLSID and assign it to this NamedObj.
	 * 
	 * @param no
	 *            the NamedObj.
	 * @throws Exception
	 */
	public static void assignIdTo(NamedObj no) throws Exception {
		assignIdTo(no, LSIDGenerator.getInstance().getNewLSID(), true);
	}

	/**
	 * Assign the given KeplerLSID to this NamedObj. If the NamedObj already has
	 * this LSID assigned to it do nothing. If the NamedObj has a different
	 * LSID, assign the new LSID to the NamedObj and move the old LSID into the
	 * referral list.
	 * 
	 * @param no
	 * @param lsid
	 * @throws Exception
	 */
	public static void assignIdTo(NamedObj no, KeplerLSID lsid)
			throws Exception {
		assignIdTo(no, lsid, true);
	}

	/**
	 * Assign the given KeplerLSID to this NamedObj. If the NamedObj already has
	 * this LSID assigned to it do nothing. If the NamedObj has a different
	 * LSID, assign the new LSID to the NamedObj and optionally move the old
	 * LSID into the referral list.
	 * 
	 * @param no
	 *            the NamedObj
	 * @param lsid
	 *            the new LSID to assign.
	 * @param updateReferrals
	 *            if true, update the referrals list.
	 * @throws Exception
	 */
	public static void assignIdTo(NamedObj no, KeplerLSID lsid,
			boolean updateReferrals) throws Exception {
		if (isDebugging)
			log.debug("assignIdTo(" + no.getName() + ", " + lsid + ")");

		NamedObjIdReferralList referrals = null;

		if (updateReferrals) {
			// make sure there is a referral list
			Attribute referralListAttribute = no
					.getAttribute(NamedObjIdReferralList.NAME);
			// if it does not exist, create one
			if (referralListAttribute == null) {
				referrals = new NamedObjIdReferralList(no,
						NamedObjIdReferralList.NAME);
			// make sure the attribute is a NamedObjIdReferralList
			} else if(referralListAttribute instanceof NamedObjIdReferralList) {
				referrals = (NamedObjIdReferralList) referralListAttribute;
			} else if(referralListAttribute instanceof Settable) {
				// the attribute is not a NamedObjIdReferralList
				// this can happen if a Kepler workflow is opened in Ptolemy, saved,
				// then opened in Kepler. (Ptolemy does not have NamedObjIdReferralList,
				// so converts to StringAttribute).
				
				// copy the contents, remove it, and replace with a NamedObjIdReferralList.
				String expression = ((Settable) referralListAttribute).getExpression();
				no.removeAttribute(referralListAttribute);
				referrals = new NamedObjIdReferralList(no,
						NamedObjIdReferralList.NAME);
				referrals.setExpression(expression);
			}
		}

		// find the entityId attribute
		NamedObjId lsidAtt = null;
                try {
                    // If the user opens a Kepler-2.2 model in Ptolemy and then
                    // saves it, the org.kepler.moml.NamedObjId
                    // will be replaced with a StringAttribute.  Then, if the user
                    // tries to reopen the model in Kepler, there will be a cast
                    // exception.  So, we get rid of the offending attribute and
                    // create another.
                    lsidAtt = (NamedObjId) no.getAttribute(NamedObjId.NAME);
                } catch (ClassCastException ex) {
                    no.getAttribute(NamedObjId.NAME).setContainer(null);
                    lsidAtt = null;
                }
		if (lsidAtt == null) {
			if (isDebugging)
				log.debug("lsidAtt is null");

			lsidAtt = new NamedObjId(no, NamedObjId.NAME);
			lsidAtt.setExpression(lsid.toString());

			// look for the ID in the Annotation attribute. (for backwards
			// compatibility)
			Attribute a = (Attribute) no.getAttribute("Annotation");
			if (a != null) {
				if (isDebugging)
					log.debug("a not null");
				NamedObjId annoLsid = (NamedObjId) a
						.getAttribute(NamedObjId.NAME);
				if (annoLsid != null) {
					if (isDebugging)
						log.debug("found lsidAtt in Annotation");
					lsidAtt.setExpression(annoLsid.getExpression());
					annoLsid.setContainer(null);
				}
			}
		}

		// Add the old lsid to the derivedFrom list if it is a different
		// object, namespace, or authority
		if (lsidAtt.getId() != null) {
			if (!lsidAtt.getId().equalsWithoutRevision(lsid) && updateReferrals) {
				if (isDebugging)
					log.debug("add referral");
				referrals.addReferral(lsidAtt.getId());
			}
		}
		lsidAtt.setExpression(lsid.toString());
	}

	/**
	 * returns the default expression which is null
	 * 
	 *@return The defaultExpression value
	 */
	public String getDefaultExpression() {
		return null;
	}

	public KeplerLSID getId() {
		return _id;
	}
	
	/**
	 * Return the LSID for the given NamedObj. If an LSID has not already been
	 * assigned then assign the NamedObj a new LSID.
	 * 
	 * @param no
	 * */
	public static KeplerLSID getIdFor(NamedObj no) {
		NamedObjId lsidAtt = NamedObjId.getIdAttributeFor(no);
		if (lsidAtt == null) {
			try {
				NamedObjId.assignIdTo(no);
				lsidAtt = NamedObjId.getIdAttributeFor(no);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		String lsidStr = lsidAtt.getExpression();
		if (lsidStr == null) {
			return null;
		}
		try {
			return new KeplerLSID(lsidStr);
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Return the KeplerIDListAttribute associated with the given NamedObj or
	 * null if no KeplerIDListAttribute exists.
	 * 
	 * @param no
	 *            * @throws NameDuplicationException
	 * @throws IllegalActionException
	 */
	public static NamedObjIdReferralList getIDListAttributeFor(NamedObj no)
			throws IllegalActionException, NameDuplicationException {

		NamedObjIdReferralList theAtt = null;

		// check the attributes of this obj
		// FIXME: why iterate over all the attributes instead of
		// getting the attribute named NamedObjIdReferralList.NAME?
		List<Attribute> theAtts = no.attributeList();
		for (Iterator<Attribute> atit = theAtts.iterator(); atit.hasNext();) {
			Attribute anAtt = (Attribute) atit.next();
			if (isDebugging)
				log.debug(anAtt.getName() + " " + anAtt);
			log.debug(anAtt.getClass().getName());
			if (anAtt instanceof NamedObjIdReferralList) {
				theAtt = (NamedObjIdReferralList) anAtt;
				if (isDebugging)
					log.debug("FOUND IT");
				return theAtt;
			}
		}

		if (theAtt == null) {
			theAtt = new NamedObjIdReferralList(no, NamedObjIdReferralList.NAME);
		}

		return theAtt;

	}
	
	/**
	 * Return the NamedObjId associated with the given NamedObj or null if no
	 * associated NamedObjId.
	 * 
	 * @param no
	 * */
	public static NamedObjId getIdAttributeFor(NamedObj no) {
		if (no == null) return null;
		
		Attribute idAtt = no.getAttribute(NAME);
		if (idAtt == null) {
			return null;
		}
		if (idAtt instanceof NamedObjId) {
			NamedObjId theAtt = (NamedObjId) idAtt;
			return theAtt;
		}
		return null;
	}

	/**
	 * set the value of this id.
	 * 
	 * If you want a NamedObjIdChangeRequest sent out in response to this
	 * setExpression you must call requestNamedObjIdChange(). updateRevision
	 * does this for you.
	 * 
	 *  @see #requestNamedObjIdChange()
	 * 
	 *@param expression
	 *            The new expression value
	 */
	public void setExpression(String expression) throws IllegalActionException {
		super.setExpression(expression);

		try {
			_id = new KeplerLSID(expression);
		} catch (Exception e) {
			_id = null;
			throw new IllegalActionException("KeplerID format is incorrect. "
					+ e.toString());
		}
		
		/*
		 * It seems like requestNamedObjIdChange() should be private and
		 * called here, but unfortunately this kicks off an infinite 
		 * during startup.
		 * NamedObj => MoMLParser => NamedObj => ChangeRequest => NamedObj
		 * => StringAttribute => NamedObjId.
		 *
		 *requestNamedObjIdChange();
		*/
		
		for (int i = 0; i < valueListeners.size(); i++) {
			// notify any listeners of the change
			ValueListener listener = valueListeners
					.elementAt(i);
			listener.valueChanged(this);
		}
		
		// Update the parent
		NamedObjId parentId = getParentId();
		if (parentId != null) {
			NamedObj parentsContainer = parentId.getContainer();
			if (parentsContainer != null) {
				try {
					NamedObjId.assignIdTo(parentsContainer,getId());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
	}

	/**
	 * Helper method for setting the expression directly with a KeplerLSID
	 * object.
	 * 
	 * @param lsid
	 * @throws IllegalActionException
	 */
	public void setExpression(KeplerLSID lsid) throws IllegalActionException {
		this.setExpression(lsid.toString());
	}

	/**
	 * add a valueListener
	 * 
	 *@param listener
	 *            The feature to be added to the ValueListener attribute
	 */
	public void addValueListener(ValueListener listener) {
		valueListeners.add(listener);
	}

	/**
	 * NamedObjIds should be invisible to the user
	 * 
	 *@return The visibility value
	 */
	public Settable.Visibility getVisibility() {
		return NONE;
	}

	/**
	 * this method does not change the visibility. NamedObjId should only be
	 * invisible
	 * 
	 *@param visibility
	 *            The new visibility value
	 */
	public void setVisibility(Settable.Visibility visibility) {
		// do nothing....we don't want the visibility getting changed
	}

	/**
	 * remove the indicated listener
	 * 
	 *@param listener
	 *            Description of the Parameter
	 */
	public void removeValueListener(ValueListener listener) {
		valueListeners.remove(listener);
	}

	/**
	 * @param change
	 * @throws Exception
	 */
	private void handleNamedObjIdChangeRequest(NamedObjIdChangeRequest change)
			throws Exception {

		Object source = change.getSource();

		if (source instanceof NamedObjId) {

			NamedObj context = change.getContext();
			NamedObj container = getContainer();
			if (container == null)
				throw new Exception(
						"NamedObjId.getContainer() is null on NamedObjIdChangeRequest");

			if (container.deepContains(context)) {
				updateRevision();
			}

		}
	}

	/**
	 * @param change
	 * @throws Exception
	 */
	private void handleMoMLChangeRequest(MoMLChangeRequest change)
			throws Exception {

		Object source = change.getSource();
		NamedObj context = change.getContext();
		NamedObj container = this.getContainer();

		if (isDebugging) {
			if (context != null) {
				log.debug("MoMLChangeRequest.context " + context.getName()
						+ " " + context.getClass().getName());
			} else {
				log.debug("MoMLChangeRequest.context is null");
			}
			log.debug("context: " + context);
			log.debug("container: " + container);
		}

		if (source instanceof LocatableNodeDragInteractor) {
			if (isDebugging)
				log.debug("Handle LocatableNodeDragInteractor");

			NamedObj containerContainer = container.getContainer();
			if (containerContainer == null) {
				// This is a change in the location of a component on the
				// canvas and we are in the NamedObjId for the top level object
				// So roll the revision on the top level object since it's
				// xml has changed
				updateRevision();
			}

		} else if (source instanceof ActorGraphModel) {
			if (isDebugging) {
				log.debug("Handle ActorGraphModel");
				log.debug(change.getClass().getName());
			}
			if (context == container) {

				// Because we cannot tell if the relation is being
				// started or ended here the lsid revision gets updated
				// both when the user starts the relation and when they
				// end it which is not ideal. Would prefer to only update
				// the revision once after a relation has been created.
				// perhaps there is a way to do this by inspecting the
				// ActorGraphModel?
				// ActorGraphModel agm = (ActorGraphModel) source;
				// agm.getLinkModel().tellMeIfThisIsTheBeginningOrEndOfARelationCreation()
				// For Now...

				// Set the id on the object directly
				KeplerLSID lsid = getId();
				if (isDebugging)
					log.debug(lsid.toString());
				LSIDGenerator gen = LSIDGenerator.getInstance();
				KeplerLSID newLSID = gen.updateLsidRevision(lsid);
				setExpression(newLSID);
				if (isDebugging)
					log.debug(newLSID.toString());

				// NOTE
				// Would prefer to update the revision by using a ChangeRequest
				// as is done in the updateRevision() method but this fouls up
				// the LinkModel for some reason and causes the linking to fail
				// completely
				// so the kludge fix for now is to not use a ChangeRequest to
				// update the lsid
				// updateRevision();
			}
		} else if (source instanceof RenameConfigurer) {
			RenameConfigurer rc = (RenameConfigurer) source;
			NamedObj sourceObj = rc.getObject();
			if (isDebugging) 
				log.debug("RenameConfigurer object is " + sourceObj.getName() );
			if (container == sourceObj) {
				updateRevision();
			}
		} else {
			if (isDebugging)
				log.debug("Handle MoMLChangeRequest from "
						+ source.getClass().getName());

			if (context == container) {
				updateRevision();
				
			} else {
				if (isDebugging)
					log.debug("Ignore: MoMLChangeRequest outside of context");
			}
		}

	}

	/**
	 * @param change
	 * @throws Exception
	 */
	private void handleUndoChangeRequest(UndoChangeRequest change)
			throws Exception {

		NamedObj context = change.getContext();
		NamedObj container = this.getContainer();

		if (isDebugging) {
			if (context != null) {
				log.debug("UndoChangeRequest.context " + context.getName()
						+ " " + context.getClass().getName());
			} else {
				log.debug("UndoChangeRequest.context is null");
			}
			log.debug("context: " + context);
			log.debug("container: " + container);
		}

		if (context == container) {
			updateRevision();

		} else {
			if (isDebugging)
				log.debug("Ignore: UndoChangeRequest outside of context");
		}

	}

	/**
	 * @param change
	 * @throws Exception
	 */
	private void handleRedoChangeRequest(RedoChangeRequest change)
			throws Exception {

		NamedObj context = change.getContext();
		NamedObj container = this.getContainer();

		if (isDebugging) {
			if (context != null) {
				log.debug("RedoChangeRequest.context " + context.getName()
						+ " " + context.getClass().getName());
			} else {
				log.debug("RedoChangeRequest.context is null");
			}
			log.debug("context: " + context);
			log.debug("container: " + container);
		}

		if (context == container) {
			updateRevision();

		} else {
			if (isDebugging)
				log.debug("Ignore: RedoChangeRequest outside of context");
		}
	}

	/**
	 * Check the given NamedObj to see if it matches the search ID.
	 * 
	 * @param no
	 * @return true if the given NamedObj has an attached NamedObjId that
	 *         matches the KeplerLSID that we are searching for
	 * @throws Exception
	 */
	public static boolean idMatches(KeplerLSID lsid, NamedObj no,
			boolean matchRevision) {
		if (isDebugging)
			log.debug("idMatches(" + no.getName() + ") "
					+ no.getClass().getName());

		NamedObjId theID = NamedObjId.getIdAttributeFor(no);
		if (theID != null) {
			if (matchRevision) {
				if (theID.getId().equals(lsid)) {
					return true;
				}
			} else {
				if (theID.getId().equalsWithoutRevision(lsid)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ptolemy.kernel.util.ChangeListener#changeExecuted(ptolemy.kernel.util
	 * .ChangeRequest)
	 */
	public void changeExecuted(ChangeRequest change) {

		if (isDebugging) {
			System.out.println();
			log.debug("********   NamedObjId.changeExecuted("
					+ change.getClass().getName() + ")");
			log.debug("ID: " + getId().toString());
			log.debug("change executed being run in " + this.getName() + " "
					+ this.getClass().getName());
			NamedObj c1 = this.getContainer();
			if (c1 != null) {
				log.debug("    with container " + c1.getName() + " "
						+ c1.getClass().getName());
				NamedObj c2 = c1.getContainer();
				if (c2 != null) {
					log.debug("    which is inside of " + c2.getName() + " "
							+ c2.getClass().getName());
				}
			}
			if (change.getSource() instanceof NamedObj) {
				log.debug("Source: "
						+ ((NamedObj) change.getSource()).getName() + " "
						+ change.getSource().getClass().getName());
			} else {
				log.debug("Source is not a named object but is a "
						+ change.getSource().getClass().getName());
			}
		}

		try {

			// The container of a NamedObjId should never be null
			NamedObj container = this.getContainer();
			if (container == null) {
				throw new Exception(
						"NamedObjId.getContainer() is null on ChangeRequest");
			}
			if (change instanceof NamedObjIdChangeRequest) {
				handleNamedObjIdChangeRequest((NamedObjIdChangeRequest) change);

			} else if (change instanceof MoMLChangeRequest) {
				handleMoMLChangeRequest((MoMLChangeRequest) change);

			} else if (change instanceof UndoChangeRequest) {
				handleUndoChangeRequest((UndoChangeRequest) change);

			} else if (change instanceof RedoChangeRequest) {
				handleRedoChangeRequest((RedoChangeRequest) change);

			}

		} catch (Exception e) {
			log.error("Failed to update revision: " + e.getMessage());
		}
	}

	/**
	 * The updateRevision() method is used to retrieve the next available
	 * revision from the LSIDGenerator and updates the revision accordingly.
	 * 
	 * @throws Exception
	 */
	public void updateRevision() throws Exception {
		if (isDebugging)
			log.debug("updateRevision()");
        
		final NamedObj container = getContainer();
		
		// update the LSID
		if (getId().isLocalToInstance()) {

			KeplerLSID lsidCheck = LSIDGenerator.getInstance()
					.updateLsidRevision(getId());
			if (!lsidCheck.equals(getId())) {
				try {
                    setExpression(lsidCheck.toString());
                } catch (IllegalActionException e) {
                    MessageHandler.error("Error setting LSID.", e);
                    return;
                }
			}

		} else {
			
			NamedObjIdReferralList noirl;
            try {
                noirl = NamedObjId
                		.getIDListAttributeFor(container);
            } catch (Exception e) {
                MessageHandler.error("Error get referral list.", e);
                return;
            }
			try {
				noirl.addReferral(getId());
			} catch (Exception e) {
			    MessageHandler.error("Error adding referral.", e);
			    return;
			}
			String updateReferralsMoml = "<property name=\""
					+ NamedObjIdReferralList.NAME + "\" class=\""
					+ noirl.getClass().getName() + "\" value=\""
					+ noirl.getExpression() + "\"/>";
			if (isDebugging)
				log.debug(updateReferralsMoml);
			NamedObjIdChangeRequest updateReferralRequest = new NamedObjIdChangeRequest(
					this, getContainer(), updateReferralsMoml);
			getContainer().requestChange(updateReferralRequest);

			try {
				_id = LSIDGenerator.getInstance().getNewLSID();
			} catch (Exception e) {
			    MessageHandler.error("Error getting new LSID.", e);
			    return;
			}

		}
		
		if (isDebugging)
			log.debug("The new revision: " + getId().toString());

		requestNamedObjIdChange();
		
		for (int i = 0; i < valueListeners.size(); i++) {
			// notify any listeners of the change
			ValueListener listener = valueListeners
					.elementAt(i);
			listener.valueChanged(this);
		}


	}

	
	public void requestNamedObjIdChange(){
		
		String updateEntityIdMoml = "<property name=\"" + NamedObjId.NAME
				+ "\" class=\"" + getClass().getName() + "\" value=\""
				+ getId() + "\"/>";
		if (isDebugging)
			log.debug(updateEntityIdMoml);
		NamedObjIdChangeRequest updateIdRequest = new NamedObjIdChangeRequest(
				this, getContainer(), updateEntityIdMoml);
		getContainer().requestChange(updateIdRequest);
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeptolemy.kernel.util.ChangeListener#changeFailed(ptolemy.kernel.util.
	 * ChangeRequest, java.lang.Exception)
	 */
	public void changeFailed(ChangeRequest change, Exception exception) {
		if (isDebugging)
			log.debug("changeFailed()");

	}

	/** validate the expression. */
	public Collection validate() {
		// don't need to do anything here
		return null;
	}
	
	/**
	 * Override setContainer method to handle adding and removing the listener
	 * based on whether or not there is a container.
	 */
	public void setContainer(NamedObj container) throws IllegalActionException,
		NameDuplicationException {
		if (container == null) {
			removeChangeListener(this);
		} else {
			addChangeListener(this);
		}
		super.setContainer(container);
	}
	
	/**
	 * Add a change listener, if the container is null the listener
	 * is not added.
	 */
	public void addChangeListener(ChangeListener listener) {
		NamedObj container = this.getContainer();
		if (container != null) {
			super.addChangeListener(listener);
		}
	}

	/**
	 * Description of the Method
	 * 
	 *@param obj
	 *            Description of the Parameter
	 *@return Description of the Return Value
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof NamedObjId)) {
			return false;
		}
		NamedObjId objId = (NamedObjId) obj;
		String str = objId.getExpression();
		if (this.getExpression() == null) {
			if (str != null) {
				return false;
			}
			return true;
		}
		return this.getExpression().equals(objId.getExpression());
	}

	/** Set if workflow changes should increment LSIDs. NOTE: this setting
	 *  only affects NamedObjId objects created after this method is called;
	 *  it does not change existing NamedObjIds.
	 */
    public static void incrementLSIDOnWorkflowChange(boolean listen) {
        _incrementLSIDOnWorkflowChange = listen;
    }
}
