/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
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

package org.sdm.spa;

import static java.net.HttpURLConnection.HTTP_OK;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

//////////////////////////////////////////////////////////////////////////
////BrowserUIServer
/*
 * <p>BrowserUIServer is to provide web server support for BrowserUI actor
 * </p>
 * <p> This class use com.sun.net.httpserver package in java 1.6 
 * which can easily create a httpserver. The server will be created when 
 * initiating the actor and will be destroyed in its postfire() function. 
 * Also only local user can visit it and only a certain url, 
 * e.g. localhost:port/browserUI.
 * Since the com.sun.net.httpserver package is only available in java 1.6, 
 * $Kepler/modules/actors/lib/jar/http.jar is added to make it work with java 1.5. 
 * </p>
 *
 * @author Jianwu Wang
 * @version $Id: BrowserUIServer.java 31113 2012-11-26 22:19:36Z crawl $
 *
 */

public final class BrowserUIServer {

	private static final Log log = LogFactory.getLog(BrowserUIServer.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	InetSocketAddress addr;
	HttpServer server;

	protected void startServer(String context, int port) throws IOException {
		try{
			addr = new InetSocketAddress("localhost", port);
			server = HttpServer.create(addr, 11);
			server.createContext("/" + context, new BrowserUIHandler());
			server.start();
			if (isDebugging)
				log.debug("server " + server.getAddress());
		} catch (java.net.BindException e){
			if (e.getMessage().equalsIgnoreCase("Address already in use"))
				System.out.println("the server for BrowserUI is already started. Reuse it...");
		}
	}

	protected void stopServer() throws IOException {

		server.stop(1);
	}

	class BrowserUIHandler implements HttpHandler {
		HashMap repository = new HashMap();

		public void handle(HttpExchange t) throws IOException {
			final InputStream is;
			OutputStream os;
			StringBuilder buf;
			StringBuilder output;
			int b;
			String ptid = null;
			HashMap map;
			String request, response;

			buf = new StringBuilder();
			output = new StringBuilder();
			String hostname = t.getRemoteAddress().getHostName();
			if (!hostname.equalsIgnoreCase("127.0.0.1")
					&& !hostname.equalsIgnoreCase("localhost")) {

				response = "Error: BrowserUIServer only accept requests from hocalhost";
				t.sendResponseHeaders(HTTP_OK, response.length());
				os = t.getResponseBody();
				os.write(response.getBytes());
				os.close();
				t.close();
				return;
			}

			String requestMethod = t.getRequestMethod();

			if (requestMethod.equalsIgnoreCase("POST")) {
				is = t.getRequestBody();
				while ((b = is.read()) != -1) {
					buf.append((char) b);
				}
				is.close();
				if (buf.length() > 0) {
					request = URLDecoder.decode(buf.toString(), "UTF-8");
				} else {
					request = null;
				}
				// the request url does not have ptid parameter, then save data
				// into
				// repository.
				if (request != null) {
					// the first POST request for a POST form, fetch data
					saveInfo(request, t);
				}

			} else if (requestMethod.equalsIgnoreCase("GET"))// if request is
			// null, the
			// form may use
			// 'get'. So
			// read info
			// from requestURI.
			{
				URI requests = t.getRequestURI();
				String requestQuery = requests.getQuery();
				if (requestQuery.indexOf("&") != -1) {
					// the first GET request for a GET form, fetch data
					saveInfo(requestQuery, t);
				}
				// the request url has ptid parameter, try to fetch data from
				// repository.
				else {
					// the second GET request for a POST/GET form, fetch data
					getInfo(requestQuery, t);
				}
			} else {
				response = "Error: BrowserUIServer only accept GET and POST requests";
				t.sendResponseHeaders(HTTP_OK, response.length());
				os = t.getResponseBody();
				os.write(response.getBytes());
				os.close();
				t.close();
				return;
			}
		}

		void saveInfo(String request, HttpExchange t) throws IOException {
			HashMap map = new HashMap();
			String ptid = null, response;
			StringBuilder output = new StringBuilder();
			OutputStream os;
			String[] requestArray = request.split("&");
			for (int i = 0; i < requestArray.length; i++) {
				String[] oneRequestArray = requestArray[i].split("=");
				for (int j = 0; j < oneRequestArray.length; j++) {
					if (oneRequestArray[0] != null) {
						if (oneRequestArray[0].equalsIgnoreCase("ptid")) {
							ptid = oneRequestArray[1];
						} else {
							if (oneRequestArray.length > 1)
								map.put(oneRequestArray[0], oneRequestArray[1]);
							else
								map.put(oneRequestArray[0], null);
						}
					}
				}
			}
			if (ptid == null) {
				output.append("sorry, no ptid was found.");
			} else if (repository.containsKey(ptid)) {
				output
						.append("sorry, an item with the same ptid was found in the repository.");
			} else {
				output.append("done.");
				repository.put(ptid, map);
				if (isDebugging)
					log
							.debug("new item in repository at BrowserUIHandler. Key="
									+ ptid + ", value=" + map);
			}
			response = output.toString();
			t.sendResponseHeaders(HTTP_OK, response.length());
			os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();
			t.close();
		}

		void getInfo(String request, HttpExchange t) throws IOException {
			HashMap map = new HashMap();
			String ptid = null, response;
			StringBuilder output = new StringBuilder();
			OutputStream os;
			String[] requestQueryArray = request.split("=");
			ptid = requestQueryArray[1];
			if (ptid == null) {
				output.append("sorry, no ptid was found.");
			} else {
				map = (HashMap) repository.get(ptid);
				if (map == null) {
					output.append("sorry, no item was saved with id " + ptid
							+ ".");
				} else {
					repository.remove(ptid);
					output.append("<xmp>\n");
					Iterator keyIt = map.keySet().iterator();
					while (keyIt.hasNext()) {
						String key = (String) keyIt.next();
						String value = (String) map.get(key);
						output
								.append("    <name>" + key
										+ "</name>\n    <value>" + value
										+ "</value>\n");
					}
					output.append("</xmp>");
				}

				response = output.toString();
				t.sendResponseHeaders(HTTP_OK, response.length());
				os = t.getResponseBody();
				os.write(response.getBytes());
				os.close();
				t.close();
			}
		}

	}

}
