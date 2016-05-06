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

import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import opendap.dap.DAP2Exception;
import opendap.dap.DAS;
import opendap.dap.DArray;
import opendap.dap.DConnect2;
import opendap.dap.DConstructor;
import opendap.dap.DDS;
import opendap.dap.parser.ParseException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ecoinformatics.seek.datasource.DataSourceIcon;

import ptolemy.actor.TypedIOPort;
import ptolemy.actor.lib.LimitedFiringSource;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.StringToken;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * The OPeNDAP actor reads data from OPeNDAP data sources (i.e. servers).
 * <p/>
 * <h1>OPeNDAP Actor Overview</h1>
 * <p/>
 * The OPeNDAP actor provides access to data served by any Data Access Protocol
 * (DAP) 2.0 compatible data source. The actor takes as configuration parameters
 * the URL to the data source and an optional constraint expression (CE). Based
 * on the URL and optional CE, the actor configures its output ports to match
 * the variables to be read from the data source.
 * <p/>
 * <h2>More information about the OPeNDAP actor</h2>
 * <p/>
 * The OPeNDAP actor reads data from a single DAP data server and provides that
 * data as either a vector.matrix or array for processing by downstream elements
 * in a Kepler workflow. Each DAP server provides (serves) many data sources and
 * each of those data sources can be uniquely identified using a URL in a way
 * that's similar to how pages are provided by a web server. For more
 * information on the DAP and on OPeNDAP's software, see www.opendap.org.
 * <p/>
 * <h3>Characterization of Data Sources</h3>
 * <p/>
 * Data sources accessible using DAP 2.0 are characterized by a URL that
 * references a both a specific data server and a data granule available from
 * that server and a Constraint Expression that describes which variables to
 * read from within the data granule. In addition to reading data from a
 * granule, a DAP 2.0 server can provide two pieces of information about the
 * granule: a description of all of its variables, their names and their data
 * types; and a collection of 'attributes' which are bound to those variables.
 * <p/>
 * <h3>Operation of the Actor</h3>
 * <p/>
 * The actor must have a valid URL before it can provide any information (just
 * as a file reader actor need to point toward a file to provide data). Given a
 * URL and the optional CE, the OPeNDAP actor will interrogate that data source
 * and configure its output ports.
 * <p/>
 * <h3>Data Types Returned by the Actor</h3>
 * <p/>
 * There are two broad classes of data types returned by the actor. First there
 * are vectors, matrices and arrays. These correspond to one, two and N (&gt; 2)
 * dimensional arrays. The distinction between the vector and matrix types and
 * the N-dimensional array is that Kepler can operate on the vector and matrix
 * types far more efficiently than the N-dimensional arrays. Many variables
 * present in DAP data sources are of the N-dimensional array class and one way
 * to work with these efficiently is to use the constraint expression to reduce
 * the order of these data to one or two, thus causing the actor to store them
 * in a vector or matrix.
 * <p/>
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
 * <p/>
 * <p>
 * The second data type returned by the actor is a record. In reality, all DAP
 * data sources are records but the actor automatically 'disassembles' the top
 * most record since we know that's what the vast majority of users will want.
 * However, some data sources contains nested hierarchies of records many levels
 * deep. When dealing with those data sources you will need to use the Kepler
 * record disassembler in your workflow.
 * </p>
 * 
 * @author Nathan Potter
 * @version $Id: OpendapDataSource.java 30948 2012-10-24 00:46:56Z barseghian $
 * @date Jul 17, 2007
 * @since Kepler 1.0RC1
 */
public class OpendapDataSource extends LimitedFiringSource {

	static Log log;

	static {
		log = LogFactory
				.getLog("org.kepler.dataproxy.datasource.opendap.OpendapDataSource");
	}

	/**
	 * The OPeNDAP URL that identifies a (possibly constrained) dataset.
	 */
	public PortParameter opendapURLParameter;
	private String opendapURL;

	/**
	 * The OPeNDAP Constraint Expression used to sub sample the dataset.
	 */
	public PortParameter opendapCEParameter;
	private String opendapCE;

	/**
	 * Controls if and how the DAP2 metadata is incorporated into the Actors
	 * output.
	 * 
	 */
	public StringParameter metadataOptionsParameter;
	public static int NO_METADATA = 0;
	public static int EMBEDDED_METADATA = 1;
	public static int SEPARATE_METADATA_PORT = 2;
	private String[] metadataChoices = { "No Metadata", "Embedded Metadata",
			"Separate Metadata Port" };

	public static String separateMetadataPortName = "DAP2 Metadata";
	public static String globalMetadataPortName = "Global Metadata";

	private boolean imbedMetadata;
	private boolean useSeparateMetadataPort;

	private DConnect2 dapConnection;
	private DataSourceIcon icon;

	public OpendapDataSource(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// hide the parent class's output port since we cannot delete it
		new ptolemy.kernel.util.Attribute(output, "_hide");

		opendapURLParameter = new PortParameter(this, "DAP2 URL");
		opendapURLParameter.setStringMode(true);
		opendapURLParameter.getPort().setTypeEquals(BaseType.STRING);
		opendapURL = "";

		opendapCEParameter = new PortParameter(this,
				"DAP2 Constraint Expression");
		opendapCEParameter.setStringMode(true);
		opendapCEParameter.getPort().setTypeEquals(BaseType.STRING);
		opendapCE = "";

		metadataOptionsParameter = new StringParameter(this, "Metadata Options");
		metadataOptionsParameter.setTypeEquals(BaseType.STRING);
		metadataOptionsParameter.setToken(new StringToken(metadataChoices[0]));
		for (String choice : metadataChoices) {
			metadataOptionsParameter.addChoice(choice);
		}
		imbedMetadata = false;
		useSeparateMetadataPort = false;

		dapConnection = null;
        try{
        	icon = new DataSourceIcon(this);
        }catch(Throwable ex){
        	log.error(ex.getMessage());
        }
	}

	/**
	 * @param attribute
	 *            The changed Attribute.
	 * @throws ptolemy.kernel.util.IllegalActionException
	 *             When bad things happen.
	 */
	public void attributeChanged(ptolemy.kernel.util.Attribute attribute)
			throws ptolemy.kernel.util.IllegalActionException {

		if (attribute == opendapURLParameter || attribute == opendapCEParameter
				|| attribute == metadataOptionsParameter) {
			updateParameters();
		} else {
			super.attributeChanged(attribute);
		}
	}

	/**
	 * Update values in URL and CE parameters.
	 * 
	 * @throws IllegalActionException
	 *             When the bad things happen.
	 */
	private void updateParameters() throws IllegalActionException {

		boolean reload = false;

		String url = ((StringToken) opendapURLParameter.getToken())
				.stringValue();
		if (!opendapURL.equals(url)) {
			opendapURL = url;

			if (opendapURL.contains("?"))
				throw new IllegalActionException(this,
						"The DAP2 URL must NOT contain a constraint expression or fragment thereof.");

			// only reload if not empty.
			if (!url.equals("")) {
				reload = true;
			}

		}

		String ce = ((StringToken) opendapCEParameter.getToken()).stringValue();
		if (!opendapCE.equals(ce)) {
			opendapCE = ce;

			if (!opendapURL.equals("")) // Only REload if it looks like they
										// have a URL too.
				reload = true;
		}

		String mdo = metadataOptionsParameter.stringValue();
		boolean goodChoice = false;
		for (String choice : metadataChoices) {
			if (mdo.equals(choice))
				goodChoice = true;
		}
		if (goodChoice) {

			if (mdo.equals(metadataChoices[NO_METADATA])) {

				if (imbedMetadata || useSeparateMetadataPort)
					reload = true;

				imbedMetadata = false;
				useSeparateMetadataPort = false;
			} else if (mdo.equals(metadataChoices[EMBEDDED_METADATA])) {

				if (!imbedMetadata || useSeparateMetadataPort)
					reload = true;

				imbedMetadata = true;
				useSeparateMetadataPort = false;
			} else if (mdo.equals(metadataChoices[SEPARATE_METADATA_PORT])) {

				if (imbedMetadata || !useSeparateMetadataPort)
					reload = true;

				imbedMetadata = false;
				useSeparateMetadataPort = true;
			}

		} else {
			String msg = "You may not edit the metadata options. "
					+ "You must chose one of: ";
			for (String choice : this.metadataChoices) {
				msg += "[" + choice + "]   ";
			}
			throw new IllegalActionException(this, msg);
		}

		if (reload) {

			try {

				log.debug("OPeNDAP URL: " + opendapURL);
				dapConnection = new DConnect2(opendapURL);

				DDS dds = getDDS(ce, false);

				// log.debug("Before ports configured.");
				// dds.print(System.out);

				log.debug("Configuring ports.");
				configureOutputPorts(dds);

				// log.debug("After ports configured.");
				// dds.print(System.out);

			} catch (Exception e) {
				e.printStackTrace();
				throw new IllegalActionException(this, "Problem accessing "
						+ "OPeNDAP Data Source: " + e.getMessage());
			}

		}
	}

	public void preinitialize() throws IllegalActionException {
		super.preinitialize();

		String ce;
		ce = ((StringToken) opendapCEParameter.getToken()).stringValue();
		log.debug("opendapCEParameter: " + ce);

		if (ce.equals("")) {
			ce = createCEfromWiredPorts();
			log.debug("Created CE from wired ports. CE: " + ce);
			opendapCEParameter.setToken(new StringToken(ce));
			updateParameters();
		}
		log.debug("metadataOptionsParameter.stringValue(): "
				+ metadataOptionsParameter.stringValue());
		log.debug("ce: "
				+ ((StringToken) opendapCEParameter.getToken()).stringValue());

	}

	public void fire() throws IllegalActionException {
		super.fire();
		log.debug("\n\n\n--- fire");

		opendapURLParameter.update();
		opendapCEParameter.update();
		updateParameters();

		try {
			if (icon != null)
				icon.setBusy();

			String ce;
			ce = ((StringToken) opendapCEParameter.getToken()).stringValue();

			log.debug("ConstraintExpression: " + ce);

			DDS dds = getDDS(ce, true);

			// log.debug("fire(): dapConnection.getData(ce) returned DataDDS:");

			log.debug("Broadcasting DAP data arrays.");
			broadcastDapData(dds);
			
			if (icon != null)
				icon.setReady();

			// log.debug("fire(): After data broadcast:");
			// dds.print(System.out);

		} catch (Exception e) {
			log.error("fire() Failed: ", e);

		}
	}

	/**
	 * Build up the projection part of the constraint expression (CE) in order
	 * to minimize the amount of data retrieved. If the CE is empty, then this
	 * will build a list of projected variables based on which output ports are
	 * wired. If the CE is not empty then it will not be modified.
	 * 
	 * @return A new CE if the passed one is not empty, a new one corresponding
	 *         to the wired output ports otherwise.
	 * @exception IllegalActionException
	 *                If thrown will getting the width of the ports.
	 */
	private String createCEfromWiredPorts() throws IllegalActionException {

		String ce;

		// Get the port list
		Iterator i = this.outputPortList().iterator();

		String projection = "";
		int pcount = 0;
		while (i.hasNext()) {
			TypedIOPort port = (TypedIOPort) i.next();

			if (port.getWidth() > 0
					&& !port.getName().equals(separateMetadataPortName)) {

				log.debug("Added " + port.getName() + " to projection.");
				if (pcount > 0)
					projection += ",";
				projection += port.getName();
				pcount++;
			}
		}
		ce = projection;

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

		TypedIOPort port;

		// log.debug("Broadcasting Dap Data for DDS: ");
		// dds.print(System.out);

		if (useSeparateMetadataPort) {
			log.debug("Sending " + separateMetadataPortName + " data.");
			port = (TypedIOPort) this.getPort(separateMetadataPortName);
			port.broadcast(AttTypeMapper.buildMetaDataTokens(dds));
			log.debug("Sent " + separateMetadataPortName);
		}

		if (imbedMetadata) {
			log.debug("Sending " + globalMetadataPortName + " port data.");
			port = (TypedIOPort) this.getPort(globalMetadataPortName);
			port.broadcast(AttTypeMapper.convertAttributeToToken(dds
					.getAttribute()));
			log.debug("Sent " + globalMetadataPortName);
		}

		Enumeration e = dds.getVariables();
		while (e.hasMoreElements()) {
			opendap.dap.BaseType bt = (opendap.dap.BaseType) e.nextElement();

			String columnName = TypeMapper.replacePeriods(bt.getName().trim());
			// Get the port associated with this DDS variable.
			port = (TypedIOPort) this.getPort(columnName);
			if (port == null) {
				throw new IllegalActionException(this,
						"Request Output Port Missing: " + columnName);
			}

			log.debug("Translating data.");
			// bt.printDecl(System.out);

			// Map the DAP data for this variable into the ptII Token model.
			ptolemy.data.Token token = TokenMapper.mapDapObjectToToken(bt,
					imbedMetadata);
			log.debug("Data Translated.");
			// bt.printDecl(System.out);

			// Send the data.
			log.debug("Sending data.");
			port.broadcast(token);
			log.debug("Sent data.");

		}

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

		if (useSeparateMetadataPort) {
			log.debug("Adding " + separateMetadataPortName
					+ " port to port list.");
			names.add(separateMetadataPortName);
			types.add(AttTypeMapper.buildMetaDataTypes(dds));
			log.debug("Added " + separateMetadataPortName
					+ " port to port list.");
		}

		if (imbedMetadata) {
			log.debug("Adding " + globalMetadataPortName
					+ " port to port list.");
			names.add(globalMetadataPortName);
			types.add(AttTypeMapper.convertAttributeToType(dds.getAttribute()));
			log.debug("Added " + globalMetadataPortName + " port.");
		}

		Enumeration e = dds.getVariables();
		while (e.hasMoreElements()) {
			opendap.dap.BaseType bt = (opendap.dap.BaseType) e.nextElement();
			types.add(TypeMapper.mapDapObjectToType(bt, imbedMetadata));
			names.add(TypeMapper.replacePeriods(bt.getName()));
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

			// FIXME: we cannot set the port type during fire() since this
			// requires write access to the workspace, which could lead to
			// a deadlock. For now, we check to see if the port type is
			// different; it usually is the same when we're in fire().

			// See if the port types are different.
			if (!port.getType().equals(aPortType)) {
				port.setTypeEquals(aPortType);
			}

		} catch (ptolemy.kernel.util.NameDuplicationException nde) {
			throw new IllegalActionException(this,
					"One or more attributes has the same name.  Please correct this and try again.");
		}

	}

	/**
	 * Remove all ports which's name is not in the selected vector
	 * 
	 * @param nonRemovePortName
	 *            The ports to NOT remove. That means keep. Whatever...
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

			// Do not remove the output port since it belongs to a
			// parent class.
			if (!nonRemovePortName.contains(currPortName)
					&& !currPortName.equals("output")) {
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

	private DDS getDDS(String ce, boolean getData) throws IOException,
			ParseException, DAP2Exception {

		DDS dds, ddx;
		DAS das;

		/*
		 * ------------------------------------------------------------- Get the
		 * metadata - this is ugly because there is a bug in DConnect that
		 * causes calls to getDataDDX to return a DDS object that contains no
		 * data.
		 */

		try {
			log.debug("Attempting to get DDX.");

			ddx = dapConnection.getDDX(ce);

			log.debug("Got DDX.");
			// ddx.print(System.out);
		} catch (Exception e) {
			log.debug("Failed to get DataDDX. Msg: " + e.getMessage());

			log.debug("Attempting to get DDS. ce: " + ce);
			ddx = dapConnection.getDDS(ce);
			log.debug("Got DDS.");

			log.debug("Attempting to get DAS.");
			das = dapConnection.getDAS();
			log.debug("Got DAS.");

			log.debug("Calling DDS.ingestDAS().");
			ddx.ingestDAS(das);

		}
		log.debug("Squeezing DDX arrays.");
		squeezeArrays(ddx);

		if (getData) {
			// Get the data.
			log.debug("Attempting to get DataDDS.");
			dds = dapConnection.getData(ce);
			log.debug("Got DataDDS.");
			// dds.print(System.out);

			log.debug("Squeezing DDS arrays.");
			squeezeArrays(dds);

			// Extract the metadata we got from the ddx.
			log.debug("Retrieving DAS from DDX.");
			das = ddx.getDAS();

			// Tie the extracted metadata back into the DataDDS
			log.debug("Calling DDS.ingestDAS().");
			dds.ingestDAS(das);
			// dds.print(System.out);

			return dds;
		}
		/*
		 * End of the ugly bit. Once the bug in DConnect2 is fixed we can
		 * replace this with a call to getDataDDX().
		 * 
		 * -----------------------------------------------------------
		 */
		// dds.print(System.out);
		return ddx;

	}

	/**
	 * Eliminates array dimensions whose dimensions are 1 (and thus in practice
	 * don't exist)
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
