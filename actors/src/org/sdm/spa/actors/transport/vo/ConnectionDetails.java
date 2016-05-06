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

package org.sdm.spa.actors.transport.vo;

public class ConnectionDetails {
	
	private String user;
	private String host;
	private int port = -1;
	//Chandrika Addition -- Starts
	 private boolean connectionOrigin = false;
	//Chandrika Addition -- Ends
	public String getUser() {
		return user;
	}

	public boolean isLocal() {
		if (host == null || host.trim().equals("localhost")
				|| host.trim().equals("local")) {
			return true;
		}
		return false;
	}

	public void setUser(String user) {
		if(user==null || user.trim().equals("")){
			user = System.getProperty("user.name");
		}
		this.user = user;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		
		if (host == null || host.trim().equals("")
				|| host.trim().equals("localhost")
				|| host.trim().equals("local")) {
			host = "localhost";
		}
		this.host = host.trim();
		
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	@Override
	public String toString(){
		StringBuffer buff = new StringBuffer(50);
		buff.append(this.user);
		buff.append("@");
		buff.append(this.host);
		if(port!=-1){
			buff.append(":");
			buff.append(this.port);
		}
		return buff.toString();	   
      //Chandrika Addition -- Starts
	}

	public boolean isConnectionOrigin() {
		return connectionOrigin;
	}
	
	public void setConnectionOrigin(boolean connectionOrigin) {
		
		
		this.connectionOrigin = connectionOrigin;
    //Chandrika Addition -- Ends
	}
}
