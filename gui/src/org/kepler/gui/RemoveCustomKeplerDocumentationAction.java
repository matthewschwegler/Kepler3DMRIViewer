/*
 * Copyright (c) 2007-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2010-11-09 17:36:37 -0800 (Tue, 09 Nov 2010) $' 
 * '$Revision: 26278 $'
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

package org.kepler.gui;

import java.awt.event.ActionEvent;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.moml.NamedObjId;
import org.kepler.moml.NamedObjIdReferralList;
import org.kepler.objectmanager.ActorMetadata;
import org.kepler.objectmanager.ObjectManager;
import org.kepler.objectmanager.cache.ActorCacheObject;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.lsid.KeplerLSID;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.NamedObj;
import ptolemy.moml.MoMLChangeRequest;
import ptolemy.vergil.basic.KeplerDocumentationAttribute;
import ptolemy.vergil.basic.RemoveCustomDocumentationAction;
import ptolemy.vergil.toolbox.FigureAction;

/**
 * This action removes KeplerDocumentationAttribute, and DocAttribute (by
 * calling RemoveCustomDocumentationAction) from a NamedObj and then attempts to
 * reset the KeplerDocumentationAttribute to its "original" state using the
 * first LSID in the NamedObj's referral list. The "original LSID" is used to
 * get the ActorMetadata on the associated ActorCacheObject; this will obviously
 * not work if there's no ActorCacheObject for the original LSID.
 * 
 * The NamedObj is gotten via FigureAction.getTarget() unless a specific LSID
 * has been set with setLSID (currently unused).
 */
public class RemoveCustomKeplerDocumentationAction extends FigureAction {

	private static final Log log = LogFactory
			.getLog(RemoveCustomKeplerDocumentationAction.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	private TableauFrame parent = null;
	private KeplerLSID _lsid = null;

	public RemoveCustomKeplerDocumentationAction(TableauFrame parent) {
		super("Remove Custom Kepler Documentation");
		if (parent == null) {
			IllegalArgumentException iae = new IllegalArgumentException(
					"RemoveCustomKeplerDocumentationAction constructor received NULL argument for TableauFrame");
			iae.fillInStackTrace();
			throw iae;
		}
		this.parent = parent;
	}

	public void setLSID(KeplerLSID lsid) {
		_lsid = lsid;
	}

	public KeplerLSID getLSID() {
		return _lsid;
	}

	/**
	 * Invoked when an action occurs.
	 * 
	 *@param e
	 *            ActionEvent
	 */
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		NamedObj target = null;
		KeplerLSID lsidToView = getLSID();

		try {

			if (lsidToView != null) {
				target = ObjectManager.getInstance().getObjectRevision(
						lsidToView);
			} else {
				target = getTarget();
			}

			if (target != null) {

				// remove the KeplerDocumentationAttribute if it exists
				List keplerDocAttributeList = target
						.attributeList(KeplerDocumentationAttribute.class);

				if (keplerDocAttributeList.size() != 0) {
					String moml = "<deleteProperty name=\""
							+ ((KeplerDocumentationAttribute) keplerDocAttributeList
									.get(0)).getName() + "\"/>";
					MoMLChangeRequest request = new MoMLChangeRequest(this,
							target, moml);
					target.requestChange(request);
				}

				// now remove the DocAttribute
				RemoveCustomDocumentationAction rcda = new RemoveCustomDocumentationAction();
				rcda.actionPerformed(e);

				// now attempt to reset KeplerDocumentationAttribute to original
				// documentation
				KeplerLSID lsid = NamedObjId.getIdFor(target);
				NamedObjIdReferralList norl = NamedObjId
						.getIDListAttributeFor(target);
				KeplerLSID origLSID = lsid;
				if (norl.getReferrals().size() > 0) {
					origLSID = norl.getReferrals().get(
							norl.getReferrals().size() - 1);
				}
				if (isDebugging) {
					log.debug("Going to try to reset " + target.getName()
							+ " to original documentation,"
							+ "the documentation for " + origLSID.toString());
				}
				CacheManager cacheMan = CacheManager.getInstance();
				ActorCacheObject aco = (ActorCacheObject) cacheMan
						.getObject(origLSID);
				if (aco == null) {
					if (isDebugging) {
						log.warn("Couldn't find ActorCacheObject for "
								+ origLSID + ". Can't reset documentation.");
					}
					return;
				}
				ActorMetadata am = aco.getMetadata();
				if (am == null) {
					if (isDebugging) {
						log.warn("No ActorMetadata for ActorCacheObject:"
								+ origLSID + ". Can't reset documentation.");
					}
					return;
				}

				KeplerDocumentationAttribute kda = am
						.getDocumentationAttribute();
				KeplerDocumentationAttribute keplerDocumentation = new KeplerDocumentationAttribute(
						target, "KeplerDocumentation");
				keplerDocumentation.createInstanceFromExisting(kda);
				keplerDocumentation.setContainer(target);
			}

		} catch (Exception ee) {
			ee.printStackTrace();
		}
	}

}