/*
 * Copyright (c) 2002-2010 The Regents of the University of California.
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

package org.srb;

import java.io.IOException;

import ptolemy.actor.lib.Source;
import ptolemy.data.ObjectToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.StringAttribute;
import edu.sdsc.grid.io.srb.SRBAccount;
import edu.sdsc.grid.io.srb.SRBFileSystem;

//////////////////////////////////////////////////////////////////////////
//// SRBConnect
/**
 * <p>
 * The SRBConnection actor provides users with valid accounts an ability to
 * connect to the SDSC Storage Resoure Broker from within the Kepler
 * computational environment. SRB actors enable access to a Data Grid Management
 * System (DGMS) that provides a hierarchical logical namespace to manage the
 * organization of data (usually files).
 * </p>
 * <p>
 * This actor connects to the SRB and returns a reference to the SRB file
 * system. This reference can be further shared by other SRB Actors which
 * perform various SCommands like functionality from within the Kepler Worfklows
 * system. The connection reference can be propagated to all actors accessing
 * the SRB workspace. This actor will create a different connection object to
 * each connected channel to allow paralel operations.
 * </p>
 * <p>
 * To access the Distributed Logical File System and other SRB features each
 * user is assigned an SRB account which includes the following information:
 * <ul>
 * <li>srbHost</li>
 * <li>srbPort</li>
 * <li>srbUserName</li>
 * <li>srbPasswd</li>
 * <li>srbHomeCollection</li>
 * <li>srbDomainName</li>
 * <li>srbDefaultResource</li>
 * </ul>
 * </p>
 * <p>
 * <B>Required User input: </B>The SRBConnection actor asks the user to specify
 * the above connection parameters and in return creates an SRB Account and
 * outputs the created SRB file system. The user needs to specify the following
 * connection parameters: srbHost, srbPort, srbUserName, srbPasswd,
 * srbHomeCollection, srbMdasDomainHome and srbDefaultResource by double-
 * clicking the actor and entering the above information in the Edit Parameters
 * Dialog Box.
 * </p>
 * <p>
 * <B>Actor Output:</B> The SRB connection reference system.
 * </p>
 * <p>
 * The following actor creates SRB Account and SRB file reference system with
 * the SRB Jargon API provided. The JARGON is a pure API for developing programs
 * with a data grid interface and I/O for SRB file systems.
 * </p>
 * <A href="http://www.sdsc.edu/srb"><I>Further information on SRB</I> </A>
 * 
 * @author Bing Zhu and Efrat Jaeger
 * @version $Id: SRBConnect.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 3.0.2
 */
public class SRBConnect extends Source {

	/**
	 * Construct an actor with the given container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the actor cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public SRBConnect(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {

		super(container, name);

		srbHost = new StringAttribute(this, "srbHost");
		srbPort = new StringAttribute(this, "srbPort");
		srbUserName = new StringAttribute(this, "srbUserName");
		srbPasswd = new StringAttribute(this, "srbPasswd");
		srbHomeCollection = new StringAttribute(this, "srbHomeCollection");
		srbMdasDomainHome = new StringAttribute(this, "srbMdasDomainHome");
		srbDefaultResource = new StringAttribute(this, "srbDefaultResource");

		// Set the type constraint.
		output.setName("SRBFileSystem");
		output.setTypeEquals(BaseType.GENERAL);
		output.setMultiport(true);
		new Attribute(output, "_showName");

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"128\" height=\"30\" " + "style=\"fill:white\"/>\n"
				+ "<text x=\"7\" y=\"24\" "
				+ "style=\"font-size:12; fill:black; font-family:SansSerif\">"
				+ "SRB$</text>\n" + "<text x=\"41\" y=\"25\" "
				+ "style=\"font-size:16; fill:blue; font-family:SansSerif\">"
				+ "CONNECT</text>\n" + "</svg>\n");

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * srbHost : represents available srb server hosts
	 * 
	 */
	public StringAttribute srbHost;

	/**
	 * the port number required to connect to the srb server
	 * 
	 */
	public StringAttribute srbPort;

	/**
	 * SRB Users are uniquely identified by their usernames combined with their
	 * domains. SRBadmin has the authority to create domains.
	 * 
	 */
	public StringAttribute srbUserName;

	/**
	 * SRB User's password
	 * 
	 */
	public StringAttribute srbPasswd;

	/**
	 * Each SRB-registered user is started with a 'home' collection. They are
	 * given read, write and create-sub collection and grant permitsin that
	 * collection.
	 * 
	 */
	public StringAttribute srbHomeCollection;

	/**
	 * A domainHome is used to identify a site or project
	 * 
	 */
	public StringAttribute srbMdasDomainHome;

	/**
	 * A SRB resource is a system that is capable of storing data objects and is
	 * accessible to the SRB
	 * 
	 */
	public StringAttribute srbDefaultResource;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Connects to SRB and returns a connection reference.
	 */
	public void fire() throws IllegalActionException {
		if (_first) {
			for (int i = 0; i < output.getWidth(); i++)
				output.send(i, new ObjectToken(srbConnections[i]));
			_first = false;
		} else {
			for (int i = 0; i < output.getWidth(); i++) {
				// making sure that each connection is still alive
				// if not create a new instance and send it.
				try {
					srbConnections[i].getHost();
				} catch (Exception ex) {
					try { // the connection was terminated - reconnect.
						srbConnections[i] = new SRBFileSystem(srbAccount);
						output.send(i, new ObjectToken(srbConnections[i]));
					} catch (IOException ioex) {
						// if cannot reconnect to srb throw the failure reason.
						throw new IllegalActionException(this,
								"SRB connection closed on channel " + i
										+ " due to " + ex.getMessage()
										+ ".\n Could not reconnect to SRB: "
										+ ioex.getMessage());
					}
				}
			}
		}
	}

	/**
	 * Connect to SRB account.
	 */
	public void initialize() throws IllegalActionException {
		super.initialize();

		_first = true;
		String _srbHost = srbHost.getExpression();
		String _srbPort = srbPort.getExpression();
		String _srbUserName = srbUserName.getExpression();
		String _srbPasswd = srbPasswd.getExpression();
		String _srbHomeCollection = srbHomeCollection.getExpression();
		String _srbMdasDomainHome = srbMdasDomainHome.getExpression();
		String _srbDefaultResource = srbDefaultResource.getExpression();

		// reset existing connections.
		if (srbConnections != null) {
			for (int i = 0; i < srbConnections.length; i++) {
				srbConnections[i] = null;
			}
		}

		try {
			srbAccount = new SRBAccount(_srbHost, Integer.parseInt(_srbPort),
					_srbUserName, _srbPasswd, _srbHomeCollection,
					_srbMdasDomainHome, _srbDefaultResource);

			int outWidth = output.getWidth();
			srbConnections = new SRBFileSystem[outWidth];
			for (int i = 0; i < outWidth; i++) {
				try {
					srbConnections[i] = new SRBFileSystem(srbAccount);
				} catch (Exception ex) {
					ex.printStackTrace();
					for (int j = 0; j <= i; j++) {
						srbConnections[j] = SRBUtil
								.closeConnection(srbConnections[j]);
					}
					throw new IllegalActionException(this,
							"Could not create an SRB connection for channel "
									+ i + ": " + ex.getMessage());
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new IllegalActionException(this, ex.getMessage());
		}
	}

	/**
	 * Disconnect from SRB.
	 */
	public void wrapup() {
		_first = true;
		System.out.println(this.getName() + ":");
		for (int i = 0; i < srbConnections.length; i++) {
			// System.out.println(srbConnections[i].toString());
			srbConnections[i] = SRBUtil.closeConnection(srbConnections[i]);
		}
		srbAccount = null;
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	/**
	 * SRB account
	 */
	private SRBAccount srbAccount = null;

	/**
	 * SRB connections array.
	 */
	private SRBFileSystem[] srbConnections = null;

	/**
	 * Indicates first iteration - send all connections
	 */
	private boolean _first = true;

}