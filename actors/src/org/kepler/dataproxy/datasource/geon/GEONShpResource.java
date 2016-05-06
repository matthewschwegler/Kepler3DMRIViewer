/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
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

package org.kepler.dataproxy.datasource.geon;

import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.dataproxy.datasource.DataSourceInterface;
import org.kepler.dataproxy.metadata.ADN.ADNMetadataSpecification;
import org.kepler.objectmanager.cache.DataCacheListener;
import org.kepler.objectmanager.cache.DataCacheObject;
import org.kepler.objectmanager.data.DataSourceControllerFactory;
import org.sdm.spa.WebService;

import ptolemy.actor.IOPort;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.gui.style.TextStyle;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.SingletonParameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;
import ptolemy.kernel.util.StringAttribute;

/**
 * A GEON shapefile resource.
 */
public class GEONShpResource extends WebService implements DataCacheListener,
		DataSourceInterface {

	private DataSourceControllerFactory _nodeController = null;

	// Constants used for more efficient execution.

	private static final int _VIEWMD = 0;
	private static final int _DOWNLOADDATA = 1;
	private static final int _VIEWDATA = 2;
	private static final int _FORWARDID = 3;

	private static final String _VIEWMDSTR = "view metadata";
	private static final String _DOWNLOADDATASTR = "download data";
	private static final String _VIEWDATASTR = "view data";
	private static final String _FORWARDIDSTR = "forward geon id";

	int _procType = 0;

	private final static Map processMap = new TreeMap();

	static {
		processMap.put(_VIEWMDSTR, new Integer(_VIEWMD));
		processMap.put(_DOWNLOADDATASTR, new Integer(_DOWNLOADDATA));
		processMap.put(_VIEWDATASTR, new Integer(_VIEWDATA));
		processMap.put(_FORWARDIDSTR, new Integer(_FORWARDID));
	}

	protected final static Log log;
	static {
		log = LogFactory
				.getLog("org.kepler.dataproxy.datasource.geon.GEONShpResource");
	}

	// private static final String ENDPOINT = "endpoint";
	// private static final String RECORDID = "recordid";
	// private static final String NAMESPACE = "namespace";

	// protected static final String YELLOW = "{1.0, 1.0, 0.0, 1.0}";
	// protected static final String RED = "{1.0, 0.0, 0.0, 1.0}";
	// protected static final String BLACK = "{0.0, 0.0, 0.0, 1.0}";
	// protected static final String MAGENTA = "{1.0, 0.0, 1.0, 1.0}";
	// protected static final String TITLE_BINARY = "0101";
	// protected static final String TITLE_BUSY = "BUSY";
	// protected static final String TITLE_ERROR = "ERROR";

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	public StringParameter _process = null;

	public StringAttribute _idAtt = null;

	public StringAttribute _endpointAtt = null;

	public StringAttribute _namespaceAtt = null;

	public StringAttribute _descriptionAtt = null;

	public SingletonParameter _hideOutput;

	public SingletonParameter _hideErrorPort;

	public TypedIOPort _forwardGEONId;

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
	 * @since
	 */
	public GEONShpResource(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		_forwardGEONId = new TypedIOPort(this, "forwardGEONId", false, true);
		_forwardGEONId.setTypeEquals(BaseType.STRING);

		_process = new StringParameter(this, "process");
		// _process.setExpression(_VIEWMDSTR);
		_process.setExpression(_FORWARDIDSTR);
		// _process.addChoice(_VIEWMDSTR);
		// _process.addChoice(_DOWNLOADDATASTR);
		// _process.addChoice(_VIEWDATASTR);
		_process.addChoice(_FORWARDIDSTR); // requires component to integrate
											// several services.

		_idAtt = new StringAttribute(this, RECORDID);
		_idAtt.setVisibility(Settable.NOT_EDITABLE);

		_endpointAtt = new StringAttribute(this, ENDPOINT);
		_endpointAtt.setVisibility(Settable.NOT_EDITABLE);

		_namespaceAtt = new StringAttribute(this, NAMESPACE);
		_namespaceAtt.setVisibility(Settable.NONE);

		_descriptionAtt = new StringAttribute(this, "description");
		TextStyle descTSatt = new TextStyle(_descriptionAtt, "descriptionTS");

		_hideOutput = new SingletonParameter(_forwardGEONId, "_hide");
		_hideOutput.setToken(BooleanToken.FALSE);

		_hideErrorPort = new SingletonParameter(clientExecErrors, "_hide");
		// _hideErrorPort.setToken(BooleanToken.TRUE);

		wsdlUrl.setVisibility(Settable.NONE);
		methodName.setVisibility(Settable.NONE);
		userName.setVisibility(Settable.NONE);
		password.setVisibility(Settable.NONE);

		hasTrigger.moveToLast();

		// Create a node controller to control the context menu
		_nodeController = new DataSourceControllerFactory(this,
				"_controllerFactory");

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"85\" height=\"30\" " + "style=\"fill:white\"/>\n"
				+ "<text x=\"9\" y=\"22\""
				+ "style=\"font-size:16; fill:blue; font-family:SansSerif\">"
				+ "Shapefile</text>\n" + "</svg>\n");

	}

	/**
	 * Callback for changes in attribute values.
	 */
	public void attributeChanged(ptolemy.kernel.util.Attribute attribute)
			throws ptolemy.kernel.util.IllegalActionException {

		if (attribute.getName().equals("process")) {
			if (_process != null) {
				String proc = ((StringParameter) attribute).getExpression();
				if (!proc.equals("")) {
					_procType = ((Integer) processMap.get(proc)).intValue();

					if (proc.equals(_VIEWMDSTR)
							|| proc.equals(_DOWNLOADDATASTR)
							|| proc.equals(_FORWARDIDSTR)) {

						// Reset wsdlUrl and methodName
						wsdlUrl.setExpression("");
						methodName.setExpression("");

						// delete all other service ports
						_deletePorts();

						if (proc.equals(_FORWARDIDSTR)) {
							// expose output port
							_hideOutput.setToken(BooleanToken.FALSE);
							_forwardGEONId.moveToFirst();
						} else {
							// hide output port
							_hideOutput.setToken(BooleanToken.TRUE);
							_forwardGEONId.moveToLast();
						}
						// hide clientErrorPort
						_hideErrorPort.setToken(BooleanToken.TRUE);
					}

					else if (proc.equals(_VIEWDATASTR)) {

						// hide clientErrorPort and expose output port
						_hideErrorPort.setToken(BooleanToken.FALSE);
						// _hideOutput.setToken(BooleanToken.TRUE);

						// Point wsdlUrl and methodName to the mapping service
						// URL.
						// This should call the web service actor's
						// attributeChanged method!!!
						// wsdlUrl.setExpression("??"); // TODO: ADD URL +
						// modify config file
						// methodName.setExpression("??");
					}
				}
			}
		} else
			super.attributeChanged(attribute);
	}

	/**
	 * Get the identifier of this record.
	 * 
	 * @return the String that uniquely identifies the record
	 */
	public String getRecordId() {
		String value = null;
		try {
			StringAttribute attribute = (StringAttribute) this
					.getAttribute(RECORDID);
			value = attribute.getExpression();
		} catch (Exception e) {
			System.err.println("getRecordId - RECORDID attr is null.");
		}
		return value;
	}

	/**
	 * Get the endpoint of this record. The endpoint indicates where the service
	 * generating the record can be accessed.
	 * 
	 * @return endpoint the URL of the service that contains the record
	 */
	public String getEndpoint() {
		String value = null;
		try {
			StringAttribute attribute = (StringAttribute) this
					.getAttribute(ENDPOINT);
			value = attribute.getExpression();
		} catch (Exception e) {
			System.err.println("getEndpoint - ENDPOINT attr is null.");
		}
		return value;
	}

	/**
	 * Get the namespace of this record.
	 * 
	 * @return namespace the URL of the service that contains the record
	 */
	public String getNamespace() {
		String value = null;
		try {
			StringAttribute attribute = (StringAttribute) this
					.getAttribute(NAMESPACE);
			value = attribute.getExpression();
		} catch (Exception e) {
			System.err.println("getNamespace - NAMESPACE attr is null.");
		}
		return value;
	}

	/**
	 * Get a URL pointer to the ADN documentation for this data source.
	 * 
	 * @return URL the URL of the HTML file containing the documentation
	 */
	public URL getDocumentation() {
		try {
			URL htmlDoc = ADNMetadataSpecification
					.getDocumentation(getRecordId());
			return htmlDoc;
		} catch (Exception ex) {
			return null;
		}
	}

	public void fire() throws IllegalActionException {
		switch (_procType) {
		case _VIEWMD:
			viewMD();
			break;
		case _DOWNLOADDATA:
			downloadData();
			break;
		case _VIEWDATA:
			super.fire();
			break;
		case _FORWARDID:
			_forwardGEONId.broadcast(new StringToken(getRecordId()));
			break;
		default:
			throw new IllegalActionException(this,
					"Unrecognized process type: " + _procType);

		}
	}

	/**
	 * postfire the actor. Return false if the command is not invoking the web
	 * service client and there is no trigger.
	 */
	public boolean postfire() throws IllegalActionException {
		if (_procType != _VIEWDATA && startTrigger.getWidth() == 0) {
			return false;
		}
		return super.postfire();
	}

	/** Deletes all the ports of this actor. */
	protected void _deletePorts() throws IllegalActionException {
		List inPortList = this.inputPortList();
		Iterator ports = inPortList.iterator();
		while (ports.hasNext()) {
			IOPort p = (IOPort) ports.next();
			if (p.isInput()) {
				try {
					if (!(p.getName().equals("startTrigger"))) {
						p.setContainer(null);
					}
				} catch (NameDuplicationException e) {
					throw new IllegalActionException(this, e,
					        "Could not delete the input port: " + p.getName());
				}
			}
		}

		List outPortList = this.outputPortList();
		Iterator oports = outPortList.iterator();
		while (oports.hasNext()) {
			IOPort outp = (IOPort) oports.next();
			if (outp.isOutput()) {
				try {
					if (!(outp.getName().equals("clientExecErrors"))
							&& !(outp.getName().equals("forwardGEONId"))) {
						outp.setContainer(null);
					}
				} catch (NameDuplicationException e) {
					throw new IllegalActionException(this, e,
					        "Could not delete the output port:" + outp.getName());
				}
			}
		}
	} // end of deletePorts

	/**
	 * download shapefile from the GEON portal to the client machine.
	 */
	private void downloadData() {
		// add implementation here..
	}

	/**
	 * View ADN metadata (could be also exposed by getMetadata in the actor's
	 * context menu).
	 */
	private void viewMD() {
		// add implementation here..
	}

	// ------------------------------------------------------------------------
	// -- DataCacheListener
	// ------------------------------------------------------------------------

	public void complete(DataCacheObject aItem) {
		log.debug("complete: " + this);

		aItem.removeListener(this);

		/*
		 * if (aItem.isReady()) { // }
		 */}

}