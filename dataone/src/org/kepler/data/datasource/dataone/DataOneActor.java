/*
 * Copyright (c) 1998-2012 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2012-04-27 13:16:27 -0700 (Fri, 27 Apr 2012) $' 
 * '$Revision: 29789 $'
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
package org.kepler.data.datasource.dataone;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectLocation;
import org.dataone.service.types.v1.ObjectLocationList;
import org.dataone.service.types.v1.SystemMetadata;
import org.ecoinformatics.seek.datasource.DataSourceIcon;

import ptolemy.actor.TypedIOPort;
import ptolemy.actor.lib.LimitedFiringSource;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.ArrayToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.UnsignedByteToken;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * @author Derik Barseghian
 * @version $Id: DataOneActor.java 29789 2012-04-27 20:16:27Z barseghian $
 */
public class DataOneActor extends LimitedFiringSource {

	private static final Logger logger = Logger.getLogger(DataOneActor.class);
	private DataSourceIcon _icon;
	private StringParameter nodeIdStringParameter;
	private PortParameter pidPortParam;

	/**
	 * Output sensorNameOutputPort
	 * 
	 * @UserLevelDocumentation The sensor name after parsing sensorId
	 */
	public TypedIOPort formatIdOutputPort;

	public DataOneActor(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		_icon = new DataSourceIcon(this);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");

		pidPortParam = new PortParameter(this, "PID");
		pidPortParam.setStringMode(true);
		pidPortParam.setTypeEquals(BaseType.STRING);
		pidPortParam.getPort().setTypeEquals(BaseType.STRING);

		// output.setDisplayName("output");
		output.setTypeEquals(new ArrayType(BaseType.UNSIGNED_BYTE));
		formatIdOutputPort = new TypedIOPort(this, "Format Id", false, true);
		formatIdOutputPort.setTypeEquals(BaseType.STRING);
	}

	public void fire() throws IllegalActionException {
		super.fire();
		_icon.setBusy();

		try {
			CNode cNode = D1Client.getCN();

			Identifier pid = new Identifier();
			pidPortParam.update();
			if (pidPortParam.getToken() != null) {
				pid.setValue(((StringToken) pidPortParam.getToken())
						.stringValue());
			}

			// Only attempt to fetch pid if necessary
			if (output.numberOfSinks() > 0 && pid.getValue() != null
					&& !pid.getValue().equals("")) {

				ObjectLocationList objectLocationList = cNode.resolve(pid);

				// XXX Instead of first, use a specific node based on some
				// criteria?
				ObjectLocation objectLocation = objectLocationList
						.getObjectLocation(0);
				MNode mnNode = D1Client.getMN(objectLocation
						.getNodeIdentifier());
				InputStream inputStream = mnNode.get(pid);
				byte[] bytes = IOUtils.toByteArray(inputStream);
				logger.debug(" for pid:" + pid.getValue() + " got "
						+ bytes.length + " bytes.");
				String formatId = "unknown";
				SystemMetadata sysMeta = mnNode.getSystemMetadata(pid);
				
				if (sysMeta != null) {
					ObjectFormatIdentifier objectFormat = sysMeta.getFormatId();
					if (objectFormat != null) {
						formatId = objectFormat.getValue();
					}
				}
				// System.out.println("=====the format identifier is "+formatId);
				// XXX ByteArrayToken offers less overhead
				// but currently in ptolemy-excludes
				// ByteArrayToken token = new ByteArrayToken();
				Token byteTokens[] = new Token[bytes.length];
				for (int i = 0; i < bytes.length; i++) {
					byteTokens[i] = new UnsignedByteToken(bytes[i]);
				}
				output.broadcast(new ArrayToken(byteTokens));
				formatIdOutputPort.send(0, new StringToken(formatId));
			}

		} catch (InvalidToken e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IllegalActionException("InvalidToken: " + e.getDescription());
		} catch (NotAuthorized e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IllegalActionException("NotAuthorized: " + e.getDescription());
		} catch (NotImplemented e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IllegalActionException("NotImplemented: "	+ e.getDescription());
		} catch (ServiceFailure e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IllegalActionException("ServiceFailure: " + e.getDescription());
		} catch (NotFound e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IllegalActionException("NotFound: " + e.getDescription());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IllegalActionException("IOException: " + e.toString());
		} catch (InsufficientResources e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IllegalActionException("InsufficientResources: " + e.toString());
		} finally {
			_icon.setReady();
		}

		_icon.setReady();
	}

	public void preinitialize() throws IllegalActionException {
		super.preinitialize();
	}

	/** The director told us to stop firing immediately. */
	public void stop() {
		super.stop();
		_icon.setReady();
	}

	public void wrapup() throws IllegalActionException {
		super.wrapup();
	}

}
