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

import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * This test connects to a host and executes a command. You should be asked for
 * password at most once, if the @user.dir@/.ssh/id_dsa private key does not
 * exist or is not valid for the selected host. Timeout is set to 1 minute, so
 * if your command is longer, it will be killed.
 * 
 * Arguments: user@host command1 string (enclose in "" if contains space)
 */
public class TestExec {

	private static SshExec ssh;
	private static String user;
	private static String host;

	public static void main(String[] arg) throws SshException,
			InterruptedException {

		System.out.println("arg length = " + arg.length);
		String target = arg.length > 0 ? arg[0] : "pnorbert@localhost:22";
		String command = arg.length > 1 ? arg[1] : "ls";

		System.out.println("remote machine = " + target + "\ncommand = "
				+ command);

		/*
		 * int atPos = target.indexOf('@'); if ( atPos >= 0) user =
		 * target.substring(0, target.indexOf('@')); else user =
		 * System.getProperty("user.name");
		 * 
		 * host=target.substring(atPos+1);
		 * 
		 * ssh = new SshExec(user, host);
		 */

		ssh = new SshExec(target);
		File iddsa = new File(System.getProperty("user.home") + File.separator
				+ ".ssh" + File.separator + "id_dsa");
		if (iddsa.exists())
			ssh.addIdentity(iddsa);
		File idrsa = new File(System.getProperty("user.home") + File.separator
				+ ".ssh" + File.separator + "id_rsa");
		if (idrsa.exists())
			ssh.addIdentity(idrsa);
		ssh.openConnection();

		ssh.setTimeout(60, false, false);
		ssh.setForcedCleanUp(true);

		ByteArrayOutputStream streamOut = new ByteArrayOutputStream();
		ByteArrayOutputStream streamErr = new ByteArrayOutputStream();

		try {
			int exitCode = ssh.executeCmd(command, streamOut, streamErr);

			if (exitCode != 0)
				System.out.println("Error when making connection to " +
				// user + "@" + host + "   exit code = " + exitCode);
						target + "   exit code = " + exitCode);

			System.out.println(" exit code = " + exitCode
					+ "\n ---- output stream -----\n" + streamOut
					+ "      ----- error stream ------\n" + streamErr
					+ "      ---------------------------\n");
		} catch (ExecException e) {
			System.out.println("Error: " + e);
		}
		ssh.closeConnection();
		System.exit(0);

	}

}