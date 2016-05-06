/*
 * Copyright (c) 2002-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:19:36 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31113 $'
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.net.ftp.FTP;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.StringAttribute;

//////////////////////////////////////////////////////////////////////////
//// FTPClient
/**
 * This actor serves as an interface for FTP operations (currently only upload
 * and download are supported).
 */

public class FTPClient extends TypedAtomicActor {

	public FTPClient(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		arguments = new TypedIOPort(this, "arguments", true, false);
		arguments.setMultiport(true);
		arguments.setTypeEquals(BaseType.STRING);

		// trigger = new TypedIOPort(this, "trigger", false, true);
		// trigger.setTypeEquals(BaseType.STRING);

		url = new TypedIOPort(this, "url", false, true);
		url.setTypeEquals(BaseType.STRING);

		operation = new StringParameter(this, "operation");
		operation.setExpression("GET");
		operation.addChoice("PUT");
		mode = new StringParameter(this, "mode");
		mode.setExpression("ASC");
		mode.addChoice("BIN");

		host = new StringAttribute(this, "host");
		remotePath = new StringAttribute(this, "remote path");
		username = new StringAttribute(this, "username");
		password = new StringAttribute(this, "password");
		localPath = new StringParameter(this, "localPath");
		localPath.setExpression(System.getProperty("user.dir"));

		_attachText("_iconDescription", "<svg>\n"
				+ "<rect x=\"-25\" y=\"-20\" " + "width=\"50\" height=\"40\" "
				+ "style=\"fill:white\"/>\n"
				+ "<polygon points=\"-15,-10 -12,-10 -8,-14 -1,-14 3,-10"
				+ " 15,-10 15,10, -15,10\" " + "style=\"fill:red\"/>\n"
				+ "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////
	/**
	 * The input port, which is a multiport.
	 */
	// public TypedIOPort trigger;
	/** Source or destination files to be uploaded/downloaded. */
	public TypedIOPort arguments;

	/** URL of the uploaded/downloaded file. */
	public TypedIOPort url;

	/** Operation performed: put/get */
	public StringParameter operation;

	/** Transfer mode: asc/bin */
	public StringParameter mode;

	/** host server name. */
	public StringAttribute host;

	/** path to remote file (begins and ends with '/'). */
	public StringAttribute remotePath;

	/** Authentication username */
	public StringAttribute username;

	/** Authentication password */
	public StringAttribute password;
	
	/** Local directory */
	public StringParameter localPath;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * If the specified attribute is <i>fileOrURL</i> and there is an open file
	 * being read, then close that file and open the new one; if the attribute
	 * is <i>numberOfLinesToSkip</i> and its value is negative, then throw an
	 * exception. In the case of <i>fileOrURL</i>, do nothing if the file name
	 * is the same as the previous value of this attribute.
	 * 
	 * @param attribute
	 *            The attribute that has changed.
	 * @exception IllegalActionException
	 *                If the specified attribute is <i>fileOrURL</i> and the
	 *                file cannot be opened, or the previously opened file
	 *                cannot be closed; or if the attribute is
	 *                <i>numberOfLinesToSkip</i> and its value is negative.
	 */

	/**
	 * Determine the output format
	 * 
	 * @param attribute
	 *            The attribute that changed.
	 * @exception IllegalActionException
	 *                If the output type is not recognized.
	 */
	public void attributeChanged(Attribute attribute)
			throws IllegalActionException {
		try {
			if (attribute == operation) {
				String strOperation = operation.getExpression();
				if (strOperation.equals("GET")) {
					_operation = _GET;
				} else if (strOperation.equals("PUT")) {
					_operation = _PUT;
				} else {
					throw new IllegalActionException(this,
							"Unrecognized operation function: " + strOperation);
				}
			} else if (attribute == mode) {
				String strMode = mode.getExpression();
				if (strMode.equals("ASC")) {
					_mode = _ASC;
				} else if (strMode.equals("BIN")) {
					_mode = _BIN;
				} else {
					throw new IllegalActionException(this,
							"Unrecognized mode function: " + strMode);
				}
			} else {
				super.attributeChanged(attribute);
			}
		} catch (Exception nameDuplication) {
			/*
			 * throw new InternalErrorException(this, nameDuplication,
			 * "Unexpected name duplication");
			 */
		}
	}

	/**
	 * Output the data lines into an array.
	 * 
	 * @exception IllegalActionException
	 *                If there's no director.
	 */

	public void fire() throws IllegalActionException {
		super.fire();

		String _host = host.getExpression();
		String _remotePath = remotePath.getExpression();
		String _username = username.getExpression();
		String _password = password.getExpression();
		
		String localPathStr = System.getProperty("user.dir");
		Token token = localPath.getToken();
		if(token != null) {
		    localPathStr = ((StringToken)token).stringValue();
		}

		String failMsg = "FTP transfer failed because of: ";
		try {
			org.apache.commons.net.ftp.FTPClient f = new org.apache.commons.net.ftp.FTPClient();
			f.connect(_host);
			f.login(_username, _password);
			f.changeWorkingDirectory(_remotePath);
		    f.cwd(localPathStr);
			if (_mode == _BIN) {
				f.setFileType(FTP.BINARY_FILE_TYPE);
			}

			// for all channels get input...
			int argsWidth = arguments.getWidth();
			for (int i = 0; i < argsWidth; i++) {
				String _arg = ((StringToken) arguments.get(i)).stringValue();
				_debug("argument(" + i + ") = " + _arg);
				File argFile = new File(localPathStr, _arg);
				_debug("file exist?  " + argFile.exists());
				// extract filename
				_debug("_remotePath = " + _remotePath + argFile.getName());
				if (_operation == _GET) {
					FileOutputStream os = new FileOutputStream(argFile);
					f.retrieveFile(argFile.getName(), os);
					os.close();
				} else if (_operation == _PUT && argFile.exists()) {
					// TODO: add if get fails then put.
					FileInputStream is = new FileInputStream(argFile);
					f.storeFile(argFile.getName(), is);
					is.close();
				} else {
					_url = "invalid command";
				}
			}
			f.disconnect();
		} catch (IOException ioe) {
			throw new IllegalActionException(failMsg + ioe.toString());
		}
		url.broadcast(new StringToken(_url));
	}

	/**
	 * Post fire the actor. Return false to indicated that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 */

	public boolean postfire() {
		return false;
	}

	// /////////////////////////////////////////////////////////////////
	// // private variables ////
	/**
	 * Output indicator parameter.
	 */
	private int _operation;
	private int _mode;

	// Constants used for more efficient execution.
	private static final int _GET = 0;
	private static final int _PUT = 1;

	private static final int _ASC = 0;
	private static final int _BIN = 1;

	/** Result string Variable. */
	private String _url = new String("");

}