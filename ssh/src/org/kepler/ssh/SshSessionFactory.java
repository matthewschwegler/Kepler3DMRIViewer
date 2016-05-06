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

package org.kepler.ssh;

import java.util.Hashtable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class provides a factory to store SSH sessions. The reference to a
 * session is with "user@host". It should be used by the SshExec class only
 * 
 * <p>
 * 
 * @author Norbert Podhorszki
 */

class SshSessionFactory {

	/* Singleton object */
	protected final static SshSessionFactory instance = new SshSessionFactory();

	/* Private variables */
	private static Hashtable sessionsTable = new Hashtable();
	private static final Log log = LogFactory.getLog(SshSessionFactory.class
			.getName());
	private SshSessionFactory() {
	}

	// protected synchronized static SshSession getSession( String user, String
	// host ) {
	// return getSession(user, host, 22); // default port 22
	// }

	protected synchronized static SshSession getSession(String user,
			String host, int port) {
		SshSession session;
		if (port <= 0)
			port = 22;
		// System.out.println(" ++ SshSessionFactory.getSession() called for " +
		// user + "@" + host + ":" + port);
		session = (SshSession) sessionsTable
				.get(user + "@" + host + ":" + port);
		if (session == null) {
			log.debug("Session DOES NOT exists in hashtable for "
					+user + "@" + host + ":" + port);
			session = new SshSession(user, host, port);
			sessionsTable.put(user + "@" + host + ":" + port, session);
		}else{
			log.debug("Session EXISTS in hashtable for "
					+user + "@" + host + ":" + port);
		}
		return session;
	}

}
