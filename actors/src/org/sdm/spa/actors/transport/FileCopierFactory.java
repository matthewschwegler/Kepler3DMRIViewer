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


/**
 * Factory class to return a FileCopier class specific to the protocol passed
 * @author Chandrika Sivaramakrishnan
 *
 */
public class FileCopierFactory {

	
	/**
	 * Returns the appropriate derived class of FileCopier based on the protocol
	 * passed. Defaults to LocalFileCopier, if the protocol passed is unknown
	 * @param protocol - the protocol to be used to copy files
	 * @return instance of appropriate FileCopier class. Defaults to LocalFileCopier
	 */
	public static FileCopierBase getFileCopier(String protocol){

		if("scp".equals(protocol)){
			return new ScpCopier();
		} else if("sftp".equals(protocol)){
			return new SftpCopier();
		}else if("bbcp".equals(protocol)){
			return new BbcpCopier();
		}else if("srmlite".equals(protocol)){
			return new SrmliteCopier();
		}else {
			return new LocalFileCopier();
		}
		
	}
}
