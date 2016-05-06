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

package org.ecoinformatics.seek.querybuilder;

import java.awt.BorderLayout;
import java.net.URL;

import javax.swing.JApplet;

/**
 * Applet for housing the QBApp (JPanel)
 */

public class QBApplet extends JApplet {

	/**
	 * Initializes the applet with the QBApp
	 */
	public void init() {
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(new QBApp(this), BorderLayout.CENTER);
	}

	/**
	 * Returns an URL from the codebase
	 * 
	 * @param filename
	 *            the file to be used in the URL
	 * @return the url
	 */
	public URL getURL(String filename) {
		URL codeBase = this.getCodeBase();
		URL url = null;
		try {
			url = new URL(codeBase, filename);
			System.out.println(url);
		} catch (java.net.MalformedURLException e) {
			System.out.println("Error: badly specified URL");
			return null;
		}
		return url;
	}

}