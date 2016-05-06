/*
 * Copyright (c) 2002-2010 The Regents of the University of California.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.gui.BrowserLauncher;
import ptolemy.data.ArrayToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.gui.GraphicalMessageHandler;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import util.UUIDGen;

//////////////////////////////////////////////////////////////////////////
//// FilterUI
/**
 * This actor displays its input on a browser window and allows the user to
 * select and forward the desired selections to the output The input is accepted
 * as an array of strings. The selected outputs are returned as an array of
 * strings.
 * 
 * @author Efrat Jaeger
 * @version $Id: FilterUI.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 4.0.1
 */
public class FilterUI extends TypedAtomicActor {

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
	public FilterUI(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		input = new TypedIOPort(this, "input", true, false);
		input.setTypeEquals(new ArrayType(BaseType.STRING));

		output = new TypedIOPort(this, "output", false, true);
		output.setTypeEquals(new ArrayType(BaseType.STRING));

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The input to be displayed for selection.
	 */
	public TypedIOPort input;

	/**
	 * The selected items.
	 */
	public TypedIOPort output;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Output the selected input data.
	 * 
	 * @exception IllegalActionException
	 *                If there's no director.
	 */

	public void fire() throws IllegalActionException {

		html = "";
    ConfigurationManager confMan = ConfigurationManager.getInstance();
    ConfigurationProperty commonProperty = confMan.getProperty(ConfigurationManager.getModule("common"));
    ConfigurationProperty serversProperty = commonProperty.getProperty("servers.server");
    ConfigurationProperty geonProperty = serversProperty.findProperties("name", "geon").get(0);
    serverPath = geonProperty.getProperty("url").getValue();
    
    
		Token[] inputTokens = ((ArrayToken) input.get(0)).arrayValue();
		_createHTMLHeader();
		_createHTMLBody(inputTokens);

		String fileToShow = "";
		File filterDisplay = null;
		// write html str to tmpFile.
		try {
			UUIDGen uuidgen = new UUIDGen();
			String fileName = uuidgen.generateUUID();
			filterDisplay = new File(fileName + ".html");

			Writer writer = new FileWriter(filterDisplay);
			writer.write(html);
			writer.close();

			fileToShow = filterDisplay.getAbsolutePath();
			BrowserLauncher.openURL(fileToShow);
		} catch (Exception ex) {
			if (filterDisplay.exists()) {
				filterDisplay.delete();
			}
			GraphicalMessageHandler.error("Failed to display files to filter",
					ex);
		}

		String fetchURL = serverPath + "pt2/jsp/ptf.jsp?ptid=" + procID;
		System.out.println("fetchURL:  " + fetchURL + "\n");
		try {
			URL url = new URL(fetchURL);
			while (true) {
				HttpURLConnection urlconn = (HttpURLConnection) url
						.openConnection();
				urlconn.connect();
				Thread.sleep(5000);
				BufferedReader br = new BufferedReader(new InputStreamReader(
						urlconn.getInputStream()));
				String line = br.readLine();
				String resp = "";
				while (line != null) {
					System.out.println(line); // REMOVE!!!
					resp += line + "\n";
					line = br.readLine();
				}
				br.close();
				urlconn.disconnect();
				Vector outputVec = new Vector();
				if (resp.trim().toLowerCase().indexOf("<xmp>") != -1) {
					while (!resp.trim().equals("</xmp>")) {
						int indStart = resp.toLowerCase().indexOf("<name>");
						int indEnd = resp.toLowerCase().indexOf("</name>");
						String respLine = resp.substring(indStart + 6, indEnd);
						String currentParamName = respLine;
						String respRest = resp.substring(indEnd + 7);
						resp = respRest;
						indStart = resp.toLowerCase().indexOf("<value>");
						indEnd = resp.toLowerCase().indexOf("</value>");
						respLine = resp.substring(indStart + 7, indEnd);
						String currentParamValue = respLine;
						outputVec.add(new StringToken(currentParamValue));
						respRest = resp.substring(indEnd + 8);
						resp = respRest;
					}
					Token[] outputTokens = new Token[outputVec.size()];
					outputVec.toArray(outputTokens);
					output.broadcast(new ArrayToken(outputTokens));
					break;
				}
			}
		} catch (Exception e) {
			if (filterDisplay.exists()) {
				filterDisplay.delete();
			}
			GraphicalMessageHandler.error("url connection error. ", e);
		}
		filterDisplay.delete();
	}

	/**
	 * Post fire the actor. Return false to indicate that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 */
	public boolean postfire() {
		return true;
	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	/**
	 * Creates the html header and selection scripts.
	 */
	private void _createHTMLBody(Token[] tokens) {

		html += "<body>\n<form action=\"" + serverPath + "pt2/jsp/pts.jsp\"";
		html += "method=\"post\" name=\"dataList\">\n<table border=\"1\">\n";
		html += "<tr bgcolor=\"#9acd32\"> <th/><th align=\"center\">Data to filter</th></tr> \n";
		for (int i = 0; i < tokens.length; i++) {
			String data = ((StringToken) tokens[i]).stringValue();
			html += "<tr><td><input onclick=\"Toggle(this)\" value=\"" + data;
			html += "\" name=\"" + i + "\" type=\"checkbox\"/></td>\n";
			html += "<td align=\"left\">" + data + "</td></tr>\n";
		}
		html += "</table>\n\n  <input value=\"filter\" type=\"submit\"/> \n";

		// generate a process id.
		UUIDGen uuidgen = new UUIDGen();
		procID = uuidgen.generateUUID();

		html += "<input type=\"hidden\" name=\"ptid\" value=\"" + procID
				+ "\"/>\n";
		html += "</form>\n</body>\n</html>";

	}

	/**
	 * Creates the html header and selection scripts.
	 */
	private void _createHTMLHeader() {

		html += "<html><head><title>Select data</title>\n";
		html += "<script type=\"text/javascript\">\n\n";
		html += "function Toggle(e) { \n  if (e.checked) { \n";
		html += "document.dataList.toggleAll.checked = AllChecked(); \n  } \n";
		html += "else { document.dataList.toggleAll.checked = false; } \n  } \n\n";
		html += "function ToggleAll(e) { \n  if (e.checked) { \n  CheckAll(); \n  } \n";
		html += "else { ClearAll(); } \n  } \n\n";
		html += "function Check(e) { \n  e.checked = true; \n  } \n\n";
		html += "function Clear(e) { \n  e.checked = false; \n  } \n\n";
		html += "function CheckAll() { \n  var ml = document.dataList; \n";
		html += "var len = ml.elements.length; \n  for (var i = 0; i &lt; len; i++) { \n";
		html += "var e = ml.elements[i]; \n  Check(e); \n  } \n ";
		html += "ml.toggleAll.checked = true; \n  } \n\n";
		html += "function ClearAll() { \n  var ml = document.dataList; \n";
		html += "var len = ml.elements.length; \n  for (var i = 0; i &lt; len; i++) { \n";
		html += "var e = ml.elements[i]; \n  Clear(e); \n  } \n";
		html += "ml.toggleAll.checked = false; \n  } \n\n";
		html += "function AllChecked() { \n  var ml = document.dataList; \n";
		html += "var len = ml.elements.length; \n  for(var i = 0 ; i &lt; len ; i++) { \n";
		html += "if (ml.elements[i].name == \"data\" &amp;&amp; !ml.elements[i].checked) { \n";
		html += "return false; \n  } \n  } \n  return true; \n  } \n\n";
		html += "</script></head>\n\n";

	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	/**
	 * holds the html content.
	 */
	private String html = "";

	private String procID;

	/**
	 * Path to the geon server url in the config file.
	 */
	private static final String SERVERPATH = "//servers/server[@name=\"geon\"]/url";

	/**
	 * URL to backend server
	 */
	private String serverPath = "";

}