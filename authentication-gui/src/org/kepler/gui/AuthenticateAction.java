/*
 * Copyright (c) 2007-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2013-01-16 15:42:20 -0800 (Wed, 16 Jan 2013) $' 
 * '$Revision: 31342 $'
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

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.authentication.AuthenticationException;
import org.kepler.authentication.AuthenticationListener;
import org.kepler.authentication.AuthenticationManager;
import org.kepler.authentication.ProxyEntity;
import org.kepler.authentication.gui.DomainSelectionGUI;
import org.kepler.util.StaticResources;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.util.MessageHandler;
import ptolemy.vergil.toolbox.FigureAction;
import diva.gui.GUIUtilities;

/**
 * This action displays the dialog to login using the
 * <code>AuthenticationManager</code> First the user is prompted for the
 * appropriate domain name to use for authentication. Then the
 * AuthenticationManager is used to validate the user in that domain.
 * 
 * @author Ben Leinfelder
 * @since 11/19/2007
 */
public class AuthenticateAction extends FigureAction {

	private static final Log log = LogFactory.getLog(AuthenticateAction.class);

	// ////////////////////////////////////////////////////////////////////////////
	// LOCALIZABLE RESOURCES - NOTE that these default values are later
	// overridden by values from the uiDisplayText resourcebundle file
	// ////////////////////////////////////////////////////////////////////////////

	private static String DISPLAY_NAME = StaticResources.getDisplayString(
			"actions.actor.displayName", "Login");
	private static String TOOLTIP = StaticResources.getDisplayString(
			"actions.actor.tooltip", "Use Authentication Manager to login.");
	private static ImageIcon LARGE_ICON = null;
	private static KeyStroke ACCELERATOR_KEY = null;

	private TableauFrame parent;
	private TabbedDialog actorDialog;

	protected String domainName = null;

	private int maxLoginAttempt = 1;
	private AuthenticationListener authListener = null;

	/**
	 * Constructor
	 * 
	 * @param parent
	 *            the "frame" (derived from ptolemy.gui.Top) where the menu is
	 *            being added.
	 */
	public AuthenticateAction(TableauFrame parent) {
		super("");
		if (parent == null) {
			IllegalArgumentException iae = new IllegalArgumentException(
					"AuthenticateAction constructor received NULL argument for TableauFrame");
			iae.fillInStackTrace();
			throw iae;
		}
		this.parent = parent;

		this.putValue(Action.NAME, DISPLAY_NAME);
		this.putValue(GUIUtilities.LARGE_ICON, LARGE_ICON);
		this.putValue("tooltip", TOOLTIP);
		this.putValue(GUIUtilities.ACCELERATOR_KEY, ACCELERATOR_KEY);
	}

	/**
	 * Constructor
	 * 
	 * @param parent
	 *            the "frame" (derived from ptolemy.gui.Top) where the menu is
	 *            being added.
	 * @param authListener
	 *            listener of the authentication process
	 */
	public AuthenticateAction(TableauFrame parent,
			AuthenticationListener authListener) {
		this(parent);
		this.authListener = authListener;
	}

	/**
	 * Invoked when an action occurs.
	 * 
	 * @param e
	 *            ActionEvent
	 */
	public void actionPerformed(ActionEvent e) {
		// must call this first...
		super.actionPerformed(e);
		doAction();
	}
	
        /*
         * Do the real action. Every subclass should implement this method.
         */
        protected void doAction() {
            log.debug("starting domain prompt worker");
            SwingWorker domainWorker = new SwingWorker<Void, Void>() {
                /**
                 * Collect the domain, which will then prompt for username/password.
                 */
                public Void doInBackground() {
                    // Display the prompt window.
                    promptForDomain();
                    // Check if we get a selected domain.
                    if (waitForDomain()) {
                        // Try the authentication.
                        fireAuthentication();
                    }
                    cancel(true);
                    return null;
                }
            };

            domainWorker.execute();
        }

	/**
	 * Get the selected domain name.
	 * 
	 * @return the name of domain
	 */
	public String getDomainName() {
		return domainName;
	}

	/** ************* DOMAIN ********** */
	private boolean promptForDomain() {
		log.debug("getting domain info - user input...");
		// DomainSelectionGUI.fire();
		DomainSelectionGUI.createAndShowGUI();
		return true;
	}

	private boolean waitForDomain() {

		log.debug("waiting for gui...");
		while (DomainSelectionGUI.getDomain() == null) {
			// log.debug("waiting for gui...");
			// wait for the input to be made and the 'ok' button to be pressed
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}

		log.debug("done waiting for gui...domain="
				+ DomainSelectionGUI.getDomain());
		if (DomainSelectionGUI.getDomain().equals(DomainSelectionGUI.DOMAIN_BREAK)) {
			// user canceled the action
			if (authListener != null) {
				authListener.authenticationComplete(
						AuthenticationListener.CANCEL, null, null);
			}
			return false;
		}
		// set the domain name so it is available for the worker
		this.domainName = DomainSelectionGUI.getDomain();

		// reset
		DomainSelectionGUI.resetFields();

		return true;
	}

	/** ************* AUTHENTICATION ********** */
	private String login() {
		String credential = null;
		ProxyEntity proxy = null;
		try {
			// first peek at it to see if we are already authenticated
			proxy = AuthenticationManager.getManager().peekProxy(
					this.domainName);

			if (proxy != null) {
				credential = proxy.getCredential();
				String userName = proxy.getUserName();
				log.debug("authentication userName=" + userName);
				log.debug("authentication credential=" + credential);
				log.debug("user is already authenticated for domain: "
						+ this.domainName);

				// prompt to logout
				boolean logoutFirst = MessageHandler.yesNoQuestion(userName
						+ "  is already authenticated in domain: "
						+ this.domainName + "\nLogout?");

				if (logoutFirst) {
					log.info("revoking authenticated proxy for credential="
							+ credential);
					AuthenticationManager.getManager().revokeProxy(proxy);
					// return null;
				} else {
					log.info("continuing using existing authentication, credential="
							+ credential);
					// return credential;
				}
			}
		} catch (AuthenticationException ae) {
			MessageHandler.error("Authentication error encountered", ae);
			log.error("The authentication exception is ", ae);
			ae.printStackTrace();
		}

		// give them a few chances to do it right
		for (int i = 0; i < maxLoginAttempt; i++) {
			try {
				// now try the authentication - no more peeking
				proxy = AuthenticationManager.getManager().getProxy(
						this.domainName);
				credential = proxy.getCredential();
				if (authListener != null) {
					authListener.authenticationComplete(
							AuthenticationListener.SUCCESS, credential,
							this.domainName);
				}
				break; // good!
			} catch (AuthenticationException ae) {
				if (ae.getType() == AuthenticationException.USER_CANCEL) {
					log.info("user cancelled the authentication");
					if (authListener != null) {
						authListener.authenticationComplete(
								AuthenticationListener.CANCEL, null, null);
					}
					break; // we're done here, now
				} else {
					if (authListener != null) {
						authListener.authenticationComplete(
								AuthenticationListener.FAILURE, credential,
								this.domainName);
					}
					MessageHandler.error("Error authenticating", ae);
					log.error("The authentication exception is ", ae);
					ae.printStackTrace();
					// loop will continue for a few tries
				}
			}
		}
		return credential;
	}

	protected void fireAuthentication() {
		log.debug("starting to authenticate with domain=" + this.domainName);
		SwingWorker authWorker = new SwingWorker<Void, Void>() {
			
			// Display the login window.
			public Void doInBackground() {
				login();
				cancel(true);
				return null;
			}
		};

		authWorker.execute();
	}

}
