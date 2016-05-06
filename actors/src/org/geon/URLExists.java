/*
 * Copyright (c) 1998-2010 The Regents of the University of California.
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

package org.geon;

import java.net.HttpURLConnection;
import java.net.URL;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// URLExists
/**
 * Assert whether a given URL exists
 * 
 * @UserLevelDocumentation Assert whether a given URL exists.
 * @author Efrat Jaeger
 * @version $Id: URLExists.java 24234 2010-05-06 05:21:26Z welker $
 */

public class URLExists extends TypedAtomicActor {

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
	public URLExists(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		output = new TypedIOPort(this, "output", false, true);

		// Set parameters.
		url = new PortParameter(this, "url");

		// set type constraints.
		url.setTypeEquals(BaseType.STRING);
		output.setTypeEquals(BaseType.BOOLEAN);
	}

	// /////////////////////////////////////////////////////////////////
	// // parameters ////

	/**
	 * Boolean output specifying whether the element is contained in the array.
	 * 
	 * @UserLevelDocumentation Boolean output specifying whether the element is
	 *                         contained in the array.
	 */
	public TypedIOPort output;

	/**
	 * The verified URL.
	 * 
	 * @UserLevelDocumentation The search element.
	 */
	public PortParameter url;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Consume a URL and verifies its existence.
	 */
	public void fire() throws IllegalActionException {
		// NOTE: This has be outside the if because we need to ensure
		// that if an index token is provided that it is consumed even
		// if there is no input token.
		url.update();
		String urlString = ((StringToken) url.getToken()).stringValue();
		try {
			HttpURLConnection.setFollowRedirects(false);
			// note : you may also need
			// HttpURLConnection.setInstanceFollowRedirects(false)
			HttpURLConnection con = (HttpURLConnection) new URL(urlString)
					.openConnection();
			con.setRequestMethod("HEAD");
			if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
				output.broadcast(new BooleanToken(true));
			} else {
				output.broadcast(new BooleanToken(false));
			}
		} catch (Exception e) {
			System.out.println("unable to create httpurlconnection to "
					+ urlString);
			System.out.println(e.getMessage());
			output.broadcast(new BooleanToken(false));
		}
	}
}