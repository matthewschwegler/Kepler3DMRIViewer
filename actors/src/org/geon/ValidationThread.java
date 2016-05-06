/*
 * Copyright (c) 2010 The Regents of the University of California.
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

/* A thread for validating the number of processing points. 
 */

package org.geon;

//////////////////////////////////////////////////////////////////////////
//// ValidationThread
/**
 * Thread for validating the number of Lidar points with a user selected query
 * attributes.
 * 
 * @author Efrat Jaeger
 */
public class ValidationThread extends Thread {

	public ValidationThread(StringBuffer threadResp, String configFile,
			String MinX, String MinY, String MaxX, String MaxY,
			String[] classification) {
		super();
		this.threadResp = threadResp;
		this.configFile = configFile;
		this.MinX = MinX;
		this.MinY = MinY;
		this.MaxX = MaxX;
		this.MaxY = MaxY;
		this.classification = classification;
		header = "";
		footer = "";
	}

	private String configFile;
	private String header;
	private String footer;
	private String MinX;
	private String MinY;
	private String MaxX;
	private String MaxY;
	private String[] classification;
	public long count;
	public StringBuffer threadResp;

	public void run() {

		LidarUtilities lutil = new LidarUtilities(threadResp, header, footer,
				"1005");
		lutil.setProperties(configFile);

		count = -1;
		count = lutil.calculateNumRows(MinX, MinY, MaxX, MaxY, classification,
				"0");

	}
}
