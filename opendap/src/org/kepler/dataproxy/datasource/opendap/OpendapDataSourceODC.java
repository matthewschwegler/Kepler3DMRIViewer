/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2009 OPeNDAP, Inc.
// All rights reserved.
// Permission is hereby granted, without written agreement and without
// license or royalty fees, to use, copy, modify, and distribute this
// software and its documentation for any purpose, provided that the above
// copyright notice and the following two paragraphs appear in all copies
// of this software.
//
// IN NO EVENT SHALL OPeNDAP BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
// SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OF
// THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF OPeNDAP HAS BEEN ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.
//
// OPeNDAP SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE. THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS"
// BASIS, AND OPeNDAP HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
// UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
//
// Author: Nathan David Potter  <ndp@opendap.org>
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
//
/////////////////////////////////////////////////////////////////////////////

package org.kepler.dataproxy.datasource.opendap;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.DodsURL;
import opendap.dap.DArray;
import opendap.dap.DConnect2;
import opendap.dap.DConstructor;
import opendap.dap.DDS;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ptolemy.actor.TypedIOPort;
import ptolemy.actor.lib.Source;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.moml.MoMLChangeRequest;

/**
 * The OPeNDAP actor reads data from OPeNDAP data sources (i.e. servers).
 * 
 * <h1>OPeNDAP Actor Overview</h1>
 * 
 * The OPeNDAP actor provides access to data served by any Data Access Protocol
 * (DAP) 2.0 compatible data source. The actor takes as configuration parameters
 * the URL to the data source and an optional constraint expression (CE). Based
 * on the URL and optional CE, the actor configures its output ports to match
 * the variables to be read from the data source.
 * 
 * <h2>More information about the OPeNDAP actor</h2>
 * 
 * The OPeNDAP actor reads data from a single DAP data server and provides that
 * data as either a vector.matrix or array for processing by downstream elements
 * in a Kepler workflow. Each DAP server provides (serves) many data sources and
 * each of those data sources can be uniquely identified using a URL in a way
 * that's similar to how pages are provided by a web server. For more
 * information on the DAP and on OPeNDAP's software, see www.opendap.org.
 * 
 * <h3>Characterization of Data Sources</h3>
 * 
 * Data sources accessible using DAP 2.0 are characterized by a URL that
 * references a both a specific data server and a data granule available from
 * that server and a Constraint Expression that describes which variables to
 * read from within the data granule. In addition to reading data from a
 * granule, a DAP 2.0 server can provide two pieces of information about the
 * granule: a description of all of its variables, their names and their data
 * types; and a collection of 'attributes' which are bound to those variables.
 * 
 * <h3>Operation of the Actor</h3>
 * 
 * The actor must have a valid URL before it can provide any information (just
 * as a file reader actor need to point toward a file to provide data). Given a
 * URL and the optional CE, the OPeNDAP actor will interrogate that data source
 * and configure its output ports.
 * 
 * <h3>Data Types Returned by the Actor</h3>
 * 
 * There are two broad classes of data types returned by the actor. First there
 * are vectors, matrices and arrays. These correspond to one, two and N (&gt; 2)
 * dimensional arrays. The distinction between the vector and matrix types and
 * the N-dimensional array is that Kepler can operate on the vector and matrix
 * types far more efficiently than the N-dimensional arrays. Many variables
 * present in DAP data sources are of the N-dimensional array class and one way
 * to work with these efficiently is to use the constraint expression to reduce
 * the order of these data to one or two, thus causing the actor to store them
 * in a vector or matrix.
 * 
 * <p>
 * As an example, consider the FNOC1 data source available at test.opendap.org.
 * The full URL for this is http://test.opendap.org/opendap/data/nc/fnoc1.nc. It
 * contains a variable 'u' which has three dimensions. We can constrain 'u' so
 * that it has only two dimensions when read into Kepler using the CE
 * 'u[0][0:16][0:20]' which selects only the first element (index 0) for the
 * first dimension while requesting all of the remaining elements for the second
 * and third dimensions. The www.opendap.org has documentation about the CE
 * syntax.
 * </p>
 * 
 * <p>
 * The second data type returned by the actor is a record. In reality, all DAP
 * data sources are records but the actor automatically 'disassembles' the top
 * most record since we know that's what the vast majority of users will want.
 * However, some data sources contains nested hierarchies of records many levels
 * deep. When dealing with those data sources you will need to use the Kepler
 * record disassembler in your work flow.
 * </p>
 * 
 * @author Nathan Potter
 * @version $Id: OpendapDataSourceODC.java 24234 2010-05-06 05:21:26Z welker $
 * @since Kepler 1.0RC1
 * @date Jul 17, 2007
 */
public class OpendapDataSourceODC extends Source {

	static Log log;
	static {
		log = LogFactory
				.getLog("org.kepler.dataproxy.datasource.opendap.OpendapDataSource");
	}

	private static final String OPENDAP_CONFIG_DIR = "/configs/ptolemy/configs/kepler/opendap";

	/**
	 * The OPeNDAP URL that identifies a (possibly constrained) dataset.
	 */
	public FileParameter opendapURLParameter = null;

	/**
	 * The OPeNDAP Constraint Expression used to sub sample the dataset.
	 */
	public FileParameter opendapCEParameter = null;

	// *** Remove. jhrg
	public Parameter runODC = null;

	private String opendapURL;
	private String opendapCE;
	private DConnect2 dapConnection;

	public OpendapDataSourceODC(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		opendapURLParameter = new FileParameter(this, "opendapURLParameter");
		opendapCEParameter = new FileParameter(this, "opendapCEParameter");

		opendapURL = "";
		opendapCE = "";
		dapConnection = null;

		runODC = new Parameter(this, "runODC");
		runODC.setTypeEquals(ptolemy.data.type.BaseType.BOOLEAN);
		runODC.setExpression("false");
	}

	/**
	 * 
	 * @param attribute
	 *            The changed Attribute.
	 * @throws ptolemy.kernel.util.IllegalActionException
	 *             When bad things happen.
	 */
	public void attributeChanged(ptolemy.kernel.util.Attribute attribute)
			throws ptolemy.kernel.util.IllegalActionException {

		log.debug("attributeChanged() start.");

		if (attribute == opendapURLParameter || attribute == opendapCEParameter) {

			String url = opendapURLParameter.getExpression();
			String ce = opendapCEParameter.getExpression();

			if (attribute == opendapURLParameter)
				log.debug("--- attributeChanged() url: " + url
						+ " Current URL: " + opendapURL);
			if (attribute == opendapCEParameter)
				log.debug("--- attributeChanged()  ce: \"" + ce
						+ "\" Current CE: \"" + opendapCE + "\"");

			boolean reload = false;
			if (!opendapURL.equals(url)) {
				opendapURL = url;

				// only reload if not empty.
				if (!url.equals("")) {
					reload = true;
				}
			}

			if (!opendapCE.equals(ce)) {
				opendapCE = ce;
				// *** I think this should test if url.equals(""). jhrg
				reload = true;
			}

			if (reload) {

				try {

					log.debug("OPeNDAP URL: " + opendapURL);
					dapConnection = new DConnect2(opendapURL);

					DDS dds = dapConnection.getDDS(opendapCE);

					log.debug("Got DDS.");
					// dds.print(System.out);

					log.debug("Squeezing arrays.");
					squeezeArrays(dds);

					// log.debug("Before ports configured.");
					// dds.print(System.out);

					log.debug("Configuring ports.");
					configureOutputPorts(dds);

					// log.debug("After ports configured.");
					// dds.print(System.out);

				} catch (Exception e) {
					e.printStackTrace();
					throw new IllegalActionException("Problem accessing "
							+ "OPeNDAP Data Source: " + e.getMessage());
				}

			}

		}
		// *** Remove the ODC option. jhrg
		else if (attribute == runODC) {
			BooleanToken token = (BooleanToken) runODC.getToken();
			if (token.booleanValue()) {
				// start ODC in separate thread
				ODCThread tr = new ODCThread();
				tr.start();
				runODC.setExpression("false");
			}
		}
	}

	// a thread to run ODC
	// *** Remove. jhrg
	private class ODCThread extends Thread {
		ODCThread() {
			super();
		}

		public void run() {

			String keplerprop = System.getProperty("KEPLER");
			if (keplerprop == null) {
				keplerprop = ".";
			}

			String configDir = keplerprop + OPENDAP_CONFIG_DIR;

			DodsURL[] urls = ApplicationController
					.blockingMain(new String[] { configDir });

			if (urls != null) {
				if (urls.length > 1) {
					// XXX how is this case possible?
					log.warn("More than one URL returned from ODC: "
							+ urls.length);
				}

				String odcUrl = urls[0].getFullURL();

				String urlStr = null;
				String ceStr = null;
				int index = odcUrl.indexOf("?");
				if (index == -1) {
					urlStr = odcUrl;
				} else {
					urlStr = odcUrl.substring(0, index);
					ceStr = odcUrl.substring(index + 1);
				}

				log.debug("ODC sent url = " + urlStr);

				try {
					// first clear the old URL and CE
					// NOTE: setting a new URL before clearing old CE
					// will cause a reload, which may not work since
					// the CE could refer to fields not present in the
					// new URL.
					opendapURLParameter.setToken(new StringToken(""));
					opendapCEParameter.setToken(new StringToken(""));

					opendapURLParameter.setToken(new StringToken(urlStr));
					if (ceStr != null) {
						opendapCEParameter.setToken(new StringToken(ceStr));
						log.debug("ODC sent ce = " + ceStr);
					}
				} catch (IllegalActionException e) {
					log.error(e);
				}

				// queue a dummy change request that will cause
				// the gui to show the new output ports.
				String buffer = "<group>\n</group>";
				MoMLChangeRequest request = new MoMLChangeRequest(this,
						getContainer(), buffer);
				request.setPersistent(false);
				requestChange(request);
			}
		}
	}

	public void preinitialize() throws IllegalActionException {

		super.preinitialize();
		log.debug("--- preintitialize");

	}

	public void initialize() throws IllegalActionException {

		super.initialize();
		log.debug("--- intitialize");

	}

	public boolean prefire() throws IllegalActionException {
		super.prefire();

		log.debug("--- prefire");

		try {

			if (dapConnection == null) {
				log.debug("OPeNDAP URL: " + opendapURL);
				dapConnection = new DConnect2(opendapURL);
			}

		} catch (Exception e) {
			log.error("prefire Failed: ", e);
		}

		return true;
	}

	public void fire() throws IllegalActionException {
		super.fire();
		log.debug("\n\n\n--- fire");

		try {
			String ce = opendapCEParameter.getExpression();
			log.debug("Constraint Expression: " + ce);

			ce = createCEfromWiredPorts(ce);

			log.debug("Using CE: " + ce);

			DDS dds = dapConnection.getData(ce);
			// log.debug("fire(): dapConnection.getData(ce) returned DataDDS:");
			// dds.print(System.out);

			log.debug("Squeezing arrays.");
			squeezeArrays(dds);

			log.debug("Broadcasting DAP data arrays.");
			broadcastDapData(dds);

			// log.debug("fire(): After data broadcast:");
			// dds.print(System.out);

		} catch (Exception e) {
			log.error("fire() Failed: ", e);

		}
	}

	public boolean postfire() throws IllegalActionException {

		super.postfire();
		log.debug("--- postfire");

		return false;

	}

	/**
	 * Build up the projection part of the constraint expression (CE) in order
	 * to minimize the amount of data retrieved. If the CE is empty, then this
	 * will build a list of projected variables base on which output ports are
	 * wired. If the CE is not empty then it will not be modified.
	 * 
	 * @param ce
	 *            The current CE
	 * @return A new CE if the passed one is not empty, a new one corresponding
	 *         to the wired output ports otherwise.
	 * @exception IllegalActionException
	 *                If the width of the wired ports cannot be calculated.
	 */
	private String createCEfromWiredPorts(String ce)
			throws IllegalActionException {

		if (ce.equals("")) {

			// Get the port list
			Iterator i = this.outputPortList().iterator();

			String projection = "";
			int pcount = 0;
			while (i.hasNext()) {
				TypedIOPort port = (TypedIOPort) i.next();
				if (port.getWidth() > 0) {
					log.debug("Added " + port.getName() + " to projection.");
					if (pcount > 0)
						projection += ",";
					projection += port.getName();
					pcount++;
				}
			}
			ce = projection;
		}

		return ce;

	}

	/**
	 * Walks through the DDS, converts DAP data to ptII data, and broadcasts the
	 * data onto the appropriate ports.
	 * 
	 * @param dds
	 *            The DDS from which to get the data to send
	 * @throws IllegalActionException
	 *             When bad things happen.
	 */
	private void broadcastDapData(DDS dds) throws IllegalActionException {

		// log.debug("broadcastDapData(): DataDDS prior to broadcast:");
		// dds.print(System.out);

		Enumeration e = dds.getVariables();
		while (e.hasMoreElements()) {
			opendap.dap.BaseType bt = (opendap.dap.BaseType) e.nextElement();

			String columnName = bt.getLongName().trim();
			// Get the port associated with this DDS variable.
			TypedIOPort port = (TypedIOPort) this.getPort(columnName);
			if (port == null) {
				throw new IllegalActionException(
						"Request Output Port Missing: " + columnName);
			}

			log.debug("Translating data.");
			// bt.printDecl(System.out);

			// Map the DAP data for this variable into the ptII Token model.
			ptolemy.data.Token token = TokenMapper.mapDapObjectToToken(bt,
					false);
			log.debug("Data Translated :");
			// bt.printDecl(System.out);

			// Send the data.
			log.debug("Sending data.");
			port.broadcast(token);
			log.debug("Sent data.");

		}

	}

	/**
	 * Probe a port
	 * 
	 * @param port
	 *            The port to probe.
	 * @return The probe report.
	 */
	public static String portInfo(TypedIOPort port) {

		String width = "";
		try {
			width = Integer.valueOf(port.getWidth()).toString();
		} catch (IllegalActionException ex) {
			width = "Failed to get width of port " + port.getFullName() + ex;
		}

		String description = "";
		try {
			description = port.description();
		} catch (IllegalActionException ex) {
			description = "Failed to get the description of port "
					+ port.getFullName() + ": " + ex;
		}
		String msg = "Port Info: \n";

		msg += "    getName():         " + port.getName() + "\n";
		msg += "    getWidth():        " + width + "\n";
		msg += "    isInput():         " + port.isInput() + "\n";
		msg += "    isOutput():        " + port.isOutput() + "\n";
		msg += "    isMultiport():     " + port.isMultiport() + "\n";
		msg += "    className():       " + port.getClassName() + "\n";
		msg += "    getDisplayName():  " + port.getDisplayName() + "\n";
		msg += "    getElementName():  " + port.getElementName() + "\n";
		msg += "    getFullName():     " + port.getFullName() + "\n";
		msg += "    getSource():       " + port.getSource() + "\n";
		msg += "    description():     " + description + "\n";
		msg += "    toString():        " + port + "\n";

		return msg;

	}

	/**
	 * Configure the output ports to expose all of the variables at the top
	 * level of the (potentially constrained) DDS.
	 * 
	 * @param dds
	 *            The DDS
	 * @throws IllegalActionException
	 *             When bad things happen.
	 */
	private void configureOutputPorts(DDS dds) throws IllegalActionException {

		Vector<Type> types = new Vector<Type>();
		Vector<String> names = new Vector<String>();

		Enumeration e = dds.getVariables();
		while (e.hasMoreElements()) {
			opendap.dap.BaseType bt = (opendap.dap.BaseType) e.nextElement();
			types.add(TypeMapper.mapDapObjectToType(bt, false));
			names.add(bt.getLongName());
		}

		removeOtherOutputPorts(names);

		Iterator ti = types.iterator();
		Iterator ni = names.iterator();

		while (ti.hasNext() && ni.hasNext()) {
			Type type = (Type) ti.next();
			String name = (String) ni.next();
			initializePort(name, type);
		}

	}

	/**
	 * Add a new port.
	 * 
	 * @param aPortName
	 *            name of new port
	 * @param aPortType
	 *            Type of new port
	 * @throws IllegalActionException
	 *             When bad things happen.
	 */
	void initializePort(String aPortName, Type aPortType)
			throws IllegalActionException {
		try {
			String columnName = aPortName.trim();
			// Create a new port for each Column in the resultset
			TypedIOPort port = (TypedIOPort) this.getPort(columnName);
			boolean aIsNew = (port == null);
			if (aIsNew) {
				// Create a new typed port and add it to this container
				port = new TypedIOPort(this, columnName, false, true);
				new ptolemy.kernel.util.Attribute(port, "_showName");
				log.debug("Creating port [" + columnName + "]" + this);
			}
			port.setTypeEquals(aPortType);

		} catch (ptolemy.kernel.util.NameDuplicationException nde) {
			throw new IllegalActionException(
					"One or more attributes has the same name.  Please correct this and try again.");
		}

	}

	/**
	 * Remove all ports which's name is not in the selected vector
	 * 
	 * @param nonRemovePortName
	 *            The ports to NOT remove.
	 * @throws IllegalActionException
	 *             When bad things happen.
	 */
	void removeOtherOutputPorts(Collection nonRemovePortName)
			throws IllegalActionException {
		// Use toArray() to make a deep copy of this.portList().
		// Do this to prevent ConcurrentModificationExceptions.
		TypedIOPort[] l = new TypedIOPort[0];
		l = (TypedIOPort[]) this.portList().toArray(l);

		for (TypedIOPort port : l) {
			if (port == null || port.isInput()) {
				continue;
			}
			String currPortName = port.getName();
			if (!nonRemovePortName.contains(currPortName)) {
				try {
					port.setContainer(null);
				} catch (Exception ex) {
					throw new IllegalActionException(this,
							"Error removing port: " + currPortName);
				}
			}
		}
	}

	/**
	 * Remove all ports.
	 * 
	 * @throws IllegalActionException
	 *             When bad things happen.
	 */
	void removeAllOutputPorts() throws IllegalActionException {
		// Use toArray() to make a deep copy of this.portList().
		// Do this to prevent ConcurrentModificationExceptions.
		TypedIOPort[] ports = new TypedIOPort[0];
		ports = (TypedIOPort[]) this.portList().toArray(ports);

		for (TypedIOPort port : ports) {
			if (port != null && port.isOutput()) {
				String currPortName = port.getName();
				try {
					port.setContainer(null);
				} catch (Exception ex) {
					throw new IllegalActionException(this,
							"Error removing port: " + currPortName);
				}
			}

		}
	}

	/**
	 * Eliminates array dimensions whose dimensions are 1 (and thus in practice
	 * don't exisit)
	 * 
	 * @param dds
	 *            The DDS to traverse and squeeze its member arrays.
	 */
	public static void squeezeArrays(DConstructor dds) {

		DArray a;

		Enumeration e = dds.getVariables();
		while (e.hasMoreElements()) {
			opendap.dap.BaseType bt = (opendap.dap.BaseType) e.nextElement();

			if (bt instanceof DArray) {
				a = (DArray) bt;
				log.debug("Squeezing array " + a.getTypeName() + " "
						+ a.getLongName() + ";");
				a.squeeze();
				// System.out.print("Post squeezing: ");
				// a.printDecl(System.out);
				bt = a.getPrimitiveVector().getTemplate();
				if (bt instanceof DConstructor)
					squeezeArrays((DConstructor) bt);
			} else if (bt instanceof DConstructor) {
				squeezeArrays((DConstructor) bt);
			}

		}
	}
}
