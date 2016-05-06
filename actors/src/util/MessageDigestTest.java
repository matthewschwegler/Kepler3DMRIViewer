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

package util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.security.MessageDigest;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * MessageDigestTest.java The purpose of this actor is calculate the message
 * digest for the contents of a file. The MD5 algorithm is used and the result
 * is compared to a previously saved value. This provides a test of whether the
 * contents of the file has changed.
 * 
 * input is a file name; output is a boolean
 * 
 *@author Dan Higgins NCEAS UC Santa Barbara
 * 
 */

public class MessageDigestTest extends TypedAtomicActor {

	/**
	 * The name of the file to be tested
	 */
	public TypedIOPort testFileName = new TypedIOPort(this, "testFileName",
			true, false);

	/**
	 * The result of the test (true or false)
	 */
	public TypedIOPort testResult = new TypedIOPort(this, "testResult", false,
			true);

	/**
	 * A MD5 value to test against (in string format)
	 */
	public StringParameter MD5_MessageDigest;

	/**
	 * if the learningMode parameter is true, the calculated MD5 value is just
	 * placed in the MD5_MessageDigest field as a hex string and no comparison
	 * is made; a false is alway returned for this case (to indicate actor is in
	 * learning mode). If true, a comparison is made; result of comparison is
	 * sent to the testResult output port
	 * 
	 */
	public Parameter learningMode;

	public MessageDigestTest(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		testFileName.setTypeEquals(BaseType.STRING);
		testResult.setTypeEquals(BaseType.BOOLEAN);

		learningMode = new Parameter(this, "learningMode");
		learningMode.setTypeEquals(BaseType.BOOLEAN);
		learningMode.setToken(BooleanToken.FALSE);

		MD5_MessageDigest = new StringParameter(this, "MD5_MessageDigest");

	}

	/**
   *
   */
	public boolean prefire() throws IllegalActionException {
		return super.prefire();
	}

	/**
   *
   */
	public void fire() throws IllegalActionException {
		String fileNameStr = "";
		super.fire();

		boolean learningModeValue = ((BooleanToken) learningMode.getToken())
				.booleanValue();
		boolean comparison = false;
		try {
			if (testFileName.getWidth() > 0) {
				fileNameStr = ((StringToken) testFileName.get(0)).stringValue();
			} else {
				System.out.println("No Input file name!");
				return;
			}
			String res = md5_file(fileNameStr);
			if (learningModeValue) {
				// just put the calculated value in the parameter\
				if (res != null) {
					MD5_MessageDigest.setExpression(res);
					MD5_MessageDigest.setPersistent(true);
				}
			} else {
				// compare the calculated and saved values
				comparison = false;
				String oldval = MD5_MessageDigest.getExpression();
				if (oldval.equals(res))
					comparison = true;
			}
		} catch (Exception w) {
		}

		testResult.broadcast(new BooleanToken(comparison));
	}

	/**
	 * Post fire the actor. Return false to indicate that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 * 
	 *	 */
	public boolean postfire() throws IllegalActionException {
		return super.postfire();
	}

	/** private classes */
	private String hex(byte[] array) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < array.length; ++i) {
			sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100)
					.toUpperCase().substring(1, 3));
		}
		return sb.toString();
	}

	private String md5_file(String filename) {
		try {
			FileInputStream fis = new FileInputStream(filename);
			BufferedInputStream bis = new BufferedInputStream(fis);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int ch;
			while ((ch = bis.read()) != -1) {
				baos.write(ch);
			}
			byte[] buffer = baos.toByteArray();
			MessageDigest algorithm = MessageDigest.getInstance("MD5");
			algorithm.update(buffer);
			byte[] digest = algorithm.digest();
			return hex(algorithm.digest(buffer));
		} catch (Exception w) {
		}
		return null;

	}

}