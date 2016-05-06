/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
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

package org.kepler.actor.rest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.MultipartPostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ptolemy.actor.IOPort;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;


//////////////////////////////////////////////////////////////////////////
////RESTService
/**
 * <p>
 * The RESTService actor provides the user with a plug-in interface to execute
 * any REST web service. Given a URL for the web service and type of the service
 * GET or POST. It works with both type of servers implementing REST standards
 * or not.
 * </p>
 * <p>
 * User shall provide all the parameters that server is expecting either through
 * user configured ports or through actor defined paramInputPort. For the user
 * defined port(s), port name is the parameter name and its value is parameter
 * value. Parameters provided through actor defined paramInputPort should have
 * name & value separated by = and each name value pair should be separated by a
 * comma. Same thing holds true for actor defined fileInputPort, parameter name
 * and the file path are separated by a = and name value pairs are separated by
 * a user defined delimiter(default value is comma. This port is not read for
 * the GET service.
 * </p>
 * <p>
 * <li>Please double-click on the actor to start customization.</li>
 * <li>To enter a URL for the service URLs, select from "methodtype" dropdown
 * and select GET or POST as appropriate.</li>
 * <li>After you select the URL and methodType, "Commit" .</li></ul> </i>
 * </p>
 *
 * @author Madhu, SDSC
 * @version $Id: RESTService.java 31113 2012-11-26 22:19:36Z crawl $
 */
public class RESTService extends TypedAtomicActor {


	private static final long serialVersionUID = 1L;
	private static Log log = LogFactory.getLog(RESTService.class);

	private static final String GET = "Get";
	private static final String POST = "Post";

	private String parDelimiter = null;
	private StringBuilder messageBldr = null;

	/** Output of REST service. */
	public TypedIOPort outputPort;

	/** Input file. */
	public TypedIOPort fileInputPort;

	/** Input parameters to REST service. */
	public TypedIOPort paramInputPort;

	/** The REST service URL. */
	public PortParameter serviceSiteURL;


	/** The REST service method to use, either GET or POST. */
	public Parameter methodType;

	/** The REST service method to use, either GET or POST. */
	public Parameter delimiter;

	/**
	 * Construct an actor with the given container and name. In addition to
	 * invoking the base class constructors, construct the <i>serviceSiteURL</i>
	 * and <i>methodType</i> parameter. Initialize <i>serviceSiteURL</i> to the
	 * URL of the site offering the web services. It is needed and must start
	 * with "http://". <i>methodType</i> is a drop down for the type of the
	 * request this service is accepting, which could be Get or Post.
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
	public RESTService(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {


		super(container, name);

		// serviceSiteURL = new Parameter(this, "serviceSiteURL");
		serviceSiteURL = new PortParameter(this, "serviceSiteURL");
		serviceSiteURL.setExpression("");
		serviceSiteURL.setStringMode(true);

		methodType = new Parameter(this, "methodType");
		methodType.setExpression("");
		methodType.setStringMode(true);
		methodType.addChoice(GET);
		methodType.addChoice(POST);

		delimiter = new Parameter(this, "Provide delimiter");
		delimiter.setExpression(ServiceUtils.PARAMDELIMITER);
		delimiter.setStringMode(true);

		outputPort = new TypedIOPort(this, "outputPort", false, true);
		outputPort.setTypeEquals(BaseType.STRING);

		paramInputPort = new TypedIOPort(this, "paramInputPort", true, false);
		paramInputPort.setTypeEquals(BaseType.STRING);

		fileInputPort = new TypedIOPort(this, "fileInputPort", true, false);
		fileInputPort.setTypeEquals(BaseType.STRING);

	}

	/**
	 * It returns the URL provided as a value of <i> serviceSiteURL </i>
	 * Parameter for RESTService actor as a String.
	 *
	 * @return ServiceSiteURL as String
	 */
	private String getServiceSiteURL() throws IllegalActionException {

		return ((StringToken) serviceSiteURL.getToken()).stringValue()
					.trim();

	}

	/**
	 * It works only on user defined/configured ports and excludes actor defined
	 * ports.
	 *
	 * @return List of Name, Value pairs of port names and values
	 */
	private List<NameValuePair> getPortValuePairs()throws IllegalActionException {

		List<NameValuePair> nvList = new ArrayList<NameValuePair>();
		List inPortList = this.inputPortList();
		Iterator ports = inPortList.iterator();

		while (ports.hasNext()) {
			IOPort p = (IOPort) ports.next();
			if (p != fileInputPort && p != paramInputPort
					&& !p.getName().equals(serviceSiteURL.getName())) {

				String pValue = getPortValue(p);
				if (pValue != null || pValue.trim().length() != 0) {
					nvList.add(new NameValuePair(p.getName(), pValue));
					System.out.println("NAME: " + p.getName() + " VALUE: "
					+ pValue);
				}

			}
		}

		return nvList;

	}

	/**
	 *
	 * @param iop
	 * @return the value associated with a particular port
	 * @throws IllegalActionException
	 */

	private String getPortValue(IOPort iop) throws IllegalActionException{

		String result = null;
		if(iop.getWidth() > 0){
			Token tk = iop.get(0);
			Type tp = tk.getType();
			if(tp == BaseType.INT || tp == BaseType.LONG ||
					tp == BaseType.FLOAT || tp == BaseType.DOUBLE){

				result = tk.toString().trim();
				log.debug("String value of the Token: " + result);
			}else{
				result =((StringToken)tk).stringValue().trim();
			}
			if(_debugging){
				_debug("In getPortValue method RESULT: " + result);
			}
		}

		return result;
	}

	/**
	 *
	 * @param inputValues
	 *            is a comma separated values fed as input to the actor from a
	 *            String Constant actor.
	 * @return a List after splitting the inputValues with PARAMDELIMITER (,)
	 *         defined in the ServiceUtils class
	 */
	private List<String> generateInputParameterList(String inputValues) {

		String[] paramArray = inputValues.split(parDelimiter);

		List<String> pList = new ArrayList<String>();

		if (paramArray.length == 0) {
			return null;
		}
		for (String param : paramArray) {
			pList.add(param);
		}
		return pList;
	}

	/**
	 * It converts List<String> to List<NameValuePair> after splitting each
	 * String with EQUALDELIMITER and adding to the NameValuePair List if Split
	 * String array length is 2 else reject it.
	 *
	 * @param valuesList
	 * @return a List<NameValuePair>
	 */
	private List<NameValuePair> generatePairList(List<String> valuesList) {
		List<NameValuePair> pairList = new ArrayList<NameValuePair>();
		if (valuesList.size() > 0) {

			for (String value : valuesList) {
				String[] splitString = value.split(ServiceUtils.EQUALDELIMITER);
				if (splitString.length == 2) {
					if (!ServiceUtils.checkEmptyString(splitString[0])
							&& !ServiceUtils.checkEmptyString(splitString[1])) {
						pairList.add(new NameValuePair(ServiceUtils
								.trimString(splitString[0]), ServiceUtils
								.trimString(splitString[1])));
					}
				}
			}

			return pairList;
		}
		return null;
	}


	private void setParDelimiter() throws IllegalActionException {

		String tmp = ((StringToken) delimiter.getToken()).stringValue().trim();

		if (!ServiceUtils.checkEmptyString(tmp)) {

			parDelimiter = tmp;
		} else {
			parDelimiter = ServiceUtils.PARAMDELIMITER;
		}
	}


	/**
	 * @return
	 */
	private String getParDelimiter() {
		return parDelimiter;
	}

	/**
	 * Sends the results as a String back after executing the appropriate
	 * service.
	 */
	@Override
	public void fire() throws IllegalActionException {

		super.fire();
		serviceSiteURL.update();

		setParDelimiter();

		if(_debugging) {
		    _debug("DELIMITER IS: " + parDelimiter);
		}

		String ssURL = getServiceSiteURL();

		if (ServiceUtils.checkEmptyString(ssURL)) {
			outputPort.send(0, new StringToken("Please provide URL"));
			return;
		} else if (!ssURL.startsWith("http://")) {
			outputPort.send(0, new StringToken("URL must start with http://"));
			return;
		}

		String inputP = null;
		// In case no port is connected to the paramInputPort
		if (paramInputPort.getWidth() > 0) {
			Token tkP = paramInputPort.get(0);
			if (tkP != null) {
				inputP = ((StringToken) tkP).stringValue().trim();
			}
			if (_debugging) {
				_debug("INPUT parameter: " + inputP);
			}
		}

		List<String> paramList = null;
		List<NameValuePair> paramPairList = null;

		if (!ServiceUtils.checkEmptyString(inputP)) {
			paramList = generateInputParameterList(inputP);
			if (paramList.size() > 0) {
				paramPairList = generatePairList(paramList);
			}
		}

		String type = ((StringToken) methodType.getToken()).stringValue();
		if (_debugging) {
			_debug("METHOD TYPE VALUE: " + type);
		}

		String returnedResults = null;

		if (type.equals(GET)) {
			log.debug("CALLING GET METHOD");
			returnedResults = executeGetMethod(paramPairList, ssURL);
		} else if (type.equals(POST)) {
			log.debug("CALLING POST METHOD");

			String fileParam = null;
			if (fileInputPort.getWidth() == 0) {
				returnedResults = executePostMethod(paramPairList, null, ssURL);
			} else {

				Token tkF = fileInputPort.get(0);
				fileParam = ((StringToken) tkF).stringValue().trim();

				if (_debugging) {
					_debug("INPUT FILE parameters: " + fileParam);
				}

				List<String> fileList = null;
				List<NameValuePair> filePairList = null;

				if (!ServiceUtils.checkEmptyString(fileParam)) {
					fileList = generateInputParameterList(fileParam);
					if (fileList.size() > 0) {
						filePairList = generatePairList(fileList);
					}
				}

				returnedResults = executePostMethod(paramPairList,
						filePairList, ssURL);
			}
		} else {
			returnedResults = "NO WEB SERVICE CALL MADE";
			log.debug("NEITHER GET NOR POST");
		}

		messageBldr = null; // messageBldr is set to null again
		outputPort.send(0, new StringToken(returnedResults));

	}

	/**
	 *
	 * @param pmPairList
	 *            List of the name and value parameters that user has provided
	 *            through paramInputPort. However in method this list is
	 *            combined with the user configured ports and the combined list
	 *            name value pair parameters are added to the service URL
	 *            separated by ampersand.
	 * @param nvPairList
	 *            List of the name and value parameters that user has provided
	 * @return the results after executing the Get service.
	 */
	public String executeGetMethod(List<NameValuePair> nvPairList,
			String serSiteURL)throws IllegalActionException {

		if (_debugging) {
			_debug("I AM IN GET METHOD");
		}
		//log.debug("I AM IN GET METHOD");

		HttpClient client = new HttpClient();

		StringBuilder results = new StringBuilder();
		results.append(serSiteURL);
		List<NameValuePair> fullPairList = getCombinedPairList(nvPairList);

		if (fullPairList.size() > 0) {

			results.append("?");

			int pairListSize = fullPairList.size();
			for (int j = 0; j < pairListSize; j++) {
				NameValuePair nvPair = fullPairList.get(j);
				results.append(nvPair.getName()).append(
						ServiceUtils.EQUALDELIMITER).append(nvPair.getValue());
				if (j < pairListSize - 1) {
					results.append("&");
				}

			}
		}
		if (_debugging) {
			_debug("RESULTS :" + results.toString());
		}

		// Create a method instance.
		GetMethod method = new GetMethod(results.toString());
		InputStream rstream = null;
		StringBuilder resultsForDisplay = new StringBuilder();

		try {

			messageBldr = new StringBuilder();
			messageBldr.append("In excuteGetMethod, communicating with service: ").
			append(serSiteURL).append("   STATUS Code: ");
			
			int statusCode = client.executeMethod(method);
			messageBldr.append(statusCode);
			
			log.debug(messageBldr.toString());
			if(_debugging) {
			    _debug(messageBldr.toString());
			}
			

			// if(statusCode == 201){
			// System.out.println("Success -- " + statusCode +
			// ServiceUtils.ANEMPTYSPACE + method.getResponseBodyAsString());
			// }else{
			// System.out.println("Failure -- " + statusCode +
			// ServiceUtils.ANEMPTYSPACE + method.getResponseBodyAsString());
			// }

			rstream = method.getResponseBodyAsStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(
					rstream));

			String s;
			while ((s = br.readLine()) != null) {
				resultsForDisplay.append(s).append(ServiceUtils.LINESEP);
			}

		} catch (HttpException httpe) {
			if (_debugging) {
				_debug("Fatal protocol violation: " + httpe.getMessage());
			}
			httpe.printStackTrace();
			throw new IllegalActionException(this, httpe, "Fatal Protocol Violation: "
					+ httpe.getMessage());
		} catch (ConnectException conExp) {
			if (_debugging) {
				_debug("Perhaps service '"+ serSiteURL + "' is not reachable: " + conExp.getMessage());
			}
			conExp.printStackTrace();
			throw new IllegalActionException(this, conExp, "Perhaps service '"+ serSiteURL + "' is not reachable: "
					+ conExp.getMessage());
		} catch (IOException ioe) {
			if (_debugging) {
				_debug("IOException: " + ioe.getMessage());
			}
			// System.err.println("Fatal transport error: " + e.getMessage());
			ioe.printStackTrace();
			throw new IllegalActionException(this, ioe, "IOException: " + ioe.getMessage());
		} catch (Exception e) {
			if (_debugging) {
				_debug("Fatal transport error: " + e.getMessage());
			}
			// System.err.println("Fatal transport error: " + e.getMessage());
			e.printStackTrace();
			throw new IllegalActionException(this, e, "Error: " + e.getMessage());
		} finally {
			// Release the connection.
			method.releaseConnection();
			client = null;
			// close InputStream;
			if (rstream != null)
				try {
					rstream.close();
				} catch (IOException e) {
					e.printStackTrace();
					throw new IllegalActionException(this, e, "InputStream Close Exception: " + e.getMessage());
				}
		}

		return resultsForDisplay.toString();

	}

	/**
	 *
	 * @param nmvlPairList
	 *            NameValue pair List of the parameters user has provided
	 *            through paramInputPort as comma separated pairs. Pairs are
	 *            separated by '='.
	 * @return
	 */
	public List<NameValuePair> getCombinedPairList(
			List<NameValuePair> nmvlPairList) throws IllegalActionException{
		List<NameValuePair> combinedPairList = new ArrayList<NameValuePair>();
		List<NameValuePair> nameValuePairList = getPortValuePairs();
		if (nameValuePairList != null && nameValuePairList.size() > 0) {
			combinedPairList.addAll(nameValuePairList);
		}
		if (nmvlPairList != null && nmvlPairList.size() > 0) {
			combinedPairList.addAll(nmvlPairList);
		}
		return combinedPairList;

	}

	/**
	 * File & regular parameters are passed as two separate lists and they are
	 * treated little differently. If flPairList is not null or empty then this
	 * method uses Part Object else no.
	 *
	 * @param pmPairList
	 *            List of the name and value parameters that user has provided
	 * @param flPairList
	 *            List of the name and value (full file path)of file parameters.
	 *            It is essentially a list of files that user wishes to attach.
	 * @return the results after executing the Post service.
	 */
	public String executePostMethod(List<NameValuePair> pmPairList,
			List<NameValuePair> flPairList, String serSiteURL)
			throws IllegalActionException {

		if (_debugging) {
			_debug("I AM IN POST METHOD");
		}

		//log.debug("I AM IN POST METHOD");
		String postAddress = serSiteURL;

		HttpClient client = new HttpClient();
		MultipartPostMethod method = new MultipartPostMethod(postAddress);
		List<NameValuePair> fullNameValueList = getCombinedPairList(pmPairList);
		if (flPairList != null && flPairList.size() > 0) {
			fullNameValueList.addAll(flPairList);
			int totalSize = fullNameValueList.size();
			Part[] parts = new Part[totalSize];

			try {

				for (int i = 0; i < parts.length; i++) {

					String nm = fullNameValueList.get(i).getName();
					String vl = fullNameValueList.get(i).getValue();

					if (i > totalSize - flPairList.size() - 1) {
						System.out.println("FILE NAME: " + nm);
						File file = getFileObject(vl);
						// System.out.println("FILE NAME: " + file.getName());
						parts[i] = new FilePart(nm, file);
						method.addPart(parts[i]);
						System.out.println("PARTS: " + i + " "
								+ parts[i].getName());
						System.out.println("file Name: " + vl);

					} else {

						System.out.println("PARAMETER NAME: " + nm);
						System.out.println("PARAMETER Value: " + vl);
						parts[i] = new StringPart(nm, vl);
						method.addPart(parts[i]);

					}
					if (_debugging) {
						_debug("Value of i: " + i);
					}

				}

			} catch (FileNotFoundException fnfe) {
				if (_debugging) {
					_debug("File Not Exception: " + fnfe.getMessage());
				}
				fnfe.printStackTrace();
				throw new IllegalActionException(this, fnfe, "File Not Found: " +
						fnfe.getMessage());
			}
		} else {
			for (NameValuePair nmPair : fullNameValueList) {
				method.addParameter(nmPair.getName(), nmPair.getValue());
			}
		}

		InputStream rstream = null;
		StringBuilder results = new StringBuilder();
		try {

			messageBldr = new StringBuilder();
			messageBldr.append("In excutePostMethod, communicating with service: ").
			append(serSiteURL).append("   STATUS Code: ");
			
			int statusCode = client.executeMethod(method);
			messageBldr.append(statusCode);
			
			log.debug("DEBUG: " + messageBldr.toString());
			System.out.println(messageBldr.toString());

			// if(statusCode == 201){
			// System.out.println("Succuess -- " + statusCode +
			// ServiceUtils.ANEMPTYSPACE + method.getResponseBodyAsString());
			// }else{
			// System.out.println("Failure -- " + statusCode +
			// ServiceUtils.ANEMPTYSPACE + method.getResponseBodyAsString());
			// }
			rstream = method.getResponseBodyAsStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(
					rstream));

			log.debug("BEFORE WHILE LOOP");
			String s;
			while ((s = br.readLine()) != null) {
				results.append(s).append(ServiceUtils.LINESEP);
			}

		} catch (HttpException e) {
			if (_debugging) {
				_debug("Fatal protocol violation: " + e.getMessage());
			}
			e.printStackTrace();
			return "Protocol Violation";
		} catch (ConnectException conExp) {
			if (_debugging) {
				_debug("Perhaps service '"+ serSiteURL + "' is not reachable: " + conExp.getMessage());
			}
			conExp.printStackTrace();
			throw new IllegalActionException(this, conExp, "Perhaps service '"+ serSiteURL + "' is not reachable: "
					+ conExp.getMessage());
		} catch (IOException ioe) {
			if (_debugging) {
				_debug("Fatal transport error: " + ioe.getMessage());
			}
			// System.err.println("Fatal transport error: " + e.getMessage());
			ioe.printStackTrace();
			throw new IllegalActionException(this, ioe, "IOException: Perhaps could not" +
					" connect to the service '"+ serSiteURL + "'. " + ioe.getMessage());
		} catch (Exception e) {
			if (_debugging) {
				_debug("Error: " + e.getMessage());
			}
			// System.err.println("Fatal transport error: " + e.getMessage());
			e.printStackTrace();
			throw new IllegalActionException(this, e, "Error: " + e.getMessage());
		} finally {
			// Release the connection.
			method.releaseConnection();
			client = null;
			// close InputStream;
			if (rstream != null)
				try {
					rstream.close();
				} catch (IOException e) {
					e.printStackTrace();
					throw new IllegalActionException(this, e, "InputStream Close Exception: " + e.getMessage());
				}
		}
		return results.toString();
	}

	/**
	 *
	 * @param val
	 * @return the File object for the supplied String
	 */
	private File getFileObject(String val) throws IllegalActionException {
		if(val.startsWith("file:")){

			URI uri = null;
			try{
				uri = new URI(val);
			}catch(URISyntaxException synE){
				synE.printStackTrace();
				throw new IllegalActionException(this, synE, "Error: " + synE.getMessage());
			}
			return new File(uri);
		}else{
			return new File(val);
		}
	}


	// public boolean postfire() {
	// return false;
	// }

}