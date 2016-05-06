/*
 * Copyright (c) 2009-2012 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jianwu $'
 * '$Date: 2012-10-09 17:45:41 -0700 (Tue, 09 Oct 2012) $' 
 * '$Revision: 30849 $'
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

package org.sdm.spa.actors.transport;

import org.kepler.ssh.ExecException;
import org.sdm.spa.actors.transport.vo.ConnectionDetails;


/**
 * Default class for copying files within the localhost.This class doesn't have 
 * have any specific implementation. All methods in turn call the Parent class'
 * copyLocal method.
 * @author D3X140
 *
 */
public class LocalFileCopier extends FileCopierBase {

	@Override
	protected CopyResult copyFrom(ConnectionDetails srcDetails, String srcFile, 
			String destFile, boolean recursive) throws ExecException {
		return copyLocal(srcFile, destFile, recursive);
	}

	@Override
	protected CopyResult copyTo(String srcFile,
			ConnectionDetails destDetails,String destFile, boolean recursive) throws ExecException {
		return copyLocal(srcFile,destFile,recursive);
	}

	@Override
	protected CopyResult copyRemote(ConnectionDetails srcDetails, String srcFile,
			ConnectionDetails destDetails, String destFile, boolean recursive)
			throws ExecException {
		return copyLocal(srcFile,destFile, recursive);
	}

	@Override
	protected int getDefaultPort() {
		return -1;
	}

}
