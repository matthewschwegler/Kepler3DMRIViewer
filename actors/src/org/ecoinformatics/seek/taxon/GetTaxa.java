/*
 * Copyright (c) 2010 The Regents of the University of California.
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

package org.ecoinformatics.seek.taxon;

import java.net.MalformedURLException;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import org.ecoinformatics.taxon.client.CSOAPClient;
import org.ecoinformatics.taxon.soap.holders.FloatArrayHolder;
import org.ecoinformatics.taxon.soap.holders.StringArrayHolder;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * The GetTaxa actor provides access to taxonomic data via the Taxonomic Object
 * Service. (TOS).
 * <p>
 * The Taxonomic Object Service is a web services enabled product that allows
 * user to query taxonomic data in a variety of ways, for example, name to
 * concept resolution, walking a classification tree and finding concepts
 * related to one another.
 * </p>
 * <p>
 * This actor uses several of the API methods defined within the TOS WSDL (@todo
 * URL to WSDL) to find all syunonymous names defined within a particular
 * authority at a user specified rank where each of those names are descendants
 * of a specified concept. For example, this actor can be used to find all names
 * at the species level associated with the ITIS, 2006 concept of Mammalia.
 * </p>
 * <p>
 * The user of the actor must provide the authority, root concept's name and the
 * target level. After execution, the return will be a two-dimensional array
 * containing with the following structure: <code>
 * [ ['guid1', 'name1', name2', ...], ['guid2', 'name1', 'name2', ...]...]
 * </code> The first element of each row
 * contains the GUID of the concept to which the following names are associated.
 * This data will presented to downstream actors on the
 * <code>synonymousNames</code> port.
 * </p>
 * <p>
 * Any errors that take place during configuration or execution of the actor
 * will be concatenated into a single string and presented on the
 * <code>clientExecErrors</code> port.
 * </p>
 */
public class GetTaxa extends TypedAtomicActor {
	/* constants */
	private static final String URL = "http://seek.nhm.ku.edu/TaxObjServ/services/TaxonomicObjectServicePort";

	/* dialog parameters */
	public StringParameter taxaName = null;
	public StringParameter authority = null;
	public StringParameter targetLevel = null;
	public Parameter checkForOverlap = null;

	/* ports */
	public TypedIOPort synonymousNames = null;
	public TypedIOPort clientExecErrors = null;

	/* private members */
	private StringBuffer errBuf = new StringBuffer();

	/* @todo document me */
	public GetTaxa(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {

		super(container, name);

		/* configure parameters */
		taxaName = new StringParameter(this, "taxaName");
		authority = new StringParameter(this, "authority");
		targetLevel = new StringParameter(this, "targetLevel");
		checkForOverlap = new Parameter(this, "checkForOverlap");
		checkForOverlap.setTypeEquals(BaseType.BOOLEAN);
		checkForOverlap.setToken(BooleanToken.FALSE);

		try {
			String[] authorities = callGetAuthorityNames();
			for (int i = 0; i < authorities.length; i++) {
				authority.addChoice(authorities[i]);
			}
		} catch (Exception e) {
			_debug("<EXCEPTION> There was an error trying to retrieve the authorities "
					+ "from the TOS.  Using a plain text field for authority.  "
					+ e + ". </EXCEPTION>");
		}

		/* configure ports */
		synonymousNames = new TypedIOPort(this, "synonymousNames", false, true);
		synonymousNames.setTypeEquals(new ArrayType(new ArrayType(
				BaseType.STRING)));
		clientExecErrors = new TypedIOPort(this, "clientExecErrors", false,
				true);
		clientExecErrors.setTypeEquals(BaseType.STRING);

		/* icon */
		_attachText(
				"_iconDescription",
				"<svg>\n<rect x=\"0\" y=\"0\" width=\"60\" height=\"30\" style=\"fill:white\"/>\n</svg>\n");
	}

	/* @todo document me */
	public void fire() throws IllegalActionException {
		super.fire();

		String name = ((StringToken) taxaName.getToken()).stringValue();
		String auth = ((StringToken) authority.getToken()).stringValue();
		String level = ((StringToken) targetLevel.getToken()).stringValue();
		boolean checkOverlaps = ((BooleanToken) checkForOverlap.getToken())
				.booleanValue();

		/* @todo error if no results from get best concept */
		try {
			String[] guids = this.callGetBestConcept(name, auth);

			if (guids == null || guids.length == 0) {
				this.errBuf.append("No concepts could be found for '").append(
						name).append("' within authority '").append(auth)
						.append("'.");
			}

			int indexSelected = 0;

			if (guids.length > 1) {
				/* @todo interact with user */
			}

			String[][] synNames = this.callGetSynsForAuthoritativeList(
					guids[indexSelected], auth, level);

			if (checkOverlaps) {
				/* @todo check for overlaps and interact with user */
			}

			synonymousNames.broadcast(this.resultToArrayToken(synNames));
		} catch (RemoteException re) {
			_debug("<EXCEPTION> There was an error executing a remote query. "
					+ re + ". </EXCEPTION>");
			this.errBuf
					.append(
							"The Taxonomic Object Service reported an error executing ")
					.append(
							"the query.  Please report the following information to ")
					.append(
							"the TOS administrators (astewart@ku.edu or rgales@ku.edu) ")
					.append("\nAuthority:  ").append(auth).append(
							"\nRoot Name:  ").append(name).append(
							"\nTarget Level:  ").append(level).append(
							"\nCheck Overlaps:  ").append(checkOverlaps)
					.append("\n").append(re.toString()).append("\n\n");
		} catch (ServiceException se) {
			_debug("<EXCEPTION> There was an error contacting the TOS.  " + se
					+ ". </EXCEPTION>");
			this.errBuf
					.append(
							"The Taxonomic Object Service appears to be unavailable.  ")
					.append("Please report this to the TOS administrators ")
					.append("(astewart@ku.edu or rgales@ku.edu).\n\n");
		} catch (MalformedURLException mue) {
			_debug("<EXCEPTION> The URL for the TOS is invalid.  " + mue
					+ ". </EXCEPTION>");
		} finally {
			clientExecErrors.broadcast(new StringToken(this.errBuf.toString()));
		}
	}

	/** @todo document me */
	private String[] callGetAuthorityNames() throws RemoteException,
			ServiceException, MalformedURLException {
		_debug("Calling getAuthortiyNames.");
		CSOAPClient client = new CSOAPClient(URL);
		return client.getAuthorityNames();
	}

	/** @todo document me */
	private String[] callGetBestConcept(String name, String auth)
			throws RemoteException, ServiceException, MalformedURLException {
		_debug("Calling getBestConcept with name='" + name
				+ "' and authority='" + auth + "'.");
		CSOAPClient client = new CSOAPClient(URL);
		StringArrayHolder guidHolder = new StringArrayHolder();
		FloatArrayHolder wgtHolder = new FloatArrayHolder();
		client.getBestConcept(name, auth, guidHolder, wgtHolder);
		return guidHolder.value;
	}

	/** @todo document me */
	private String[][] callGetSynsForAuthoritativeList(String guid,
			String auth, String level) throws RemoteException,
			ServiceException, MalformedURLException {
		_debug("Calling getSynsForAuthoritativeList with guid='" + guid
				+ "' authority='" + auth + "' level='" + level + "'.");
		CSOAPClient client = new CSOAPClient(URL);
		return client.getSynsOfAuthoritativeList(guid, auth, level);
	}

	/** @todo document me */
	private ArrayToken resultToArrayToken(String[][] result)
			throws IllegalActionException {
		StringBuffer buf = new StringBuffer("{");

		for (int i = 0; i < result.length; i++) {
			buf.append("{");
			for (int j = 0; j < result[i].length; j++) {
				buf.append("\"").append(result[i][j]).append("\"");

				if (j < (result[i].length - 1)) {
					buf.append(", ");
				}
			}
			buf.append("}");

			if (i < (result.length - 1)) {
				buf.append(", ");
			}
		}

		buf.append("}");

		return new ArrayToken(buf.toString());
	}
}
