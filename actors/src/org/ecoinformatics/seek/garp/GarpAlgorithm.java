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

package org.ecoinformatics.seek.garp;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.StringToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * @actor.name GarpAlgorithm
 * @actor.lsid urn:lsid:ecoinformatics.org:kepler.44.1
 * @actor.ontology <owl:Ontology rdf:about="http://seek.ecoinformatics.org/annotations"/>
 * @actor.ontology http://seek.ecoinformatics.org/ontology#
 * @actor.annotation <rdf:type rdf:resource="http://seek.ecoinformatics.org/ontology#ArithmeticMathOperationActor"/>
 * @actor.annotation <rdf:type rdf:resource="http://seek.ecoinformatics.org/ontology#ExternalExecutionEnvironmentActor"/>
 *
 * @port.name cellSetFileName
 * @port.type STRING
 * @port.name ruleSetFilename
 * @port.type STRING
 * @port.name ruleSetFilenameOutput
 * @port.type STRING
 *
 * @depends.linux libgarp.so
 * @depends.windows garp.dll
 * @depends.windows libexpat.dll
 * @depends.osx libgarp.jnilib
 *
 *
 */

/**
 * <p>
 * GARP is a computer program for predicting species locations based on various
 * spatial data sets of environment variables and known species locations. GARP
 * is an acronym for Genetic Algorithm for Rule Set Production. GARP was
 * originally ceated by David Stockwell. The version in Kepler is based on
 * 'Desktop GARP', http://www.lifemapper.org/desktopgarp/. The GarpAlgorithm
 * actor takes information from the GarpPresampleLayers actor (A randomly
 * generated set of spatial locations and associated environment data.) and
 * calculates a 'RuleSet' using a genetic algorithm. The RuleSet can then be
 * used to determine whether the environmental data for some location is similar
 * enough to the environment at known location positions to predict
 * presence/absence.
 * </p>
 * <p>
 * This is a JNI-based actor. It requires the following: linux: libgarp.so
 * windows: garp.dll, libexpat.dll MacOSX - currently not available for the Mac
 * (3/16/2006)
 * </p>
 * 
 * @author Chad Berkeley, Dan Higgins, NCEAS, UC Santa Barbara
 */
public class GarpAlgorithm extends TypedAtomicActor {
	// FileParameters
	/**
	 * This is the name of the file containing the cellSet information. This is
	 * usually the output of the GarpPresampleLayers actor. This filename can
	 * also be specified on an input port.
	 */
	public FileParameter cellSetFileNameParameter = new FileParameter(this,
			"cellSetFileNameParameter");
	/**
	 * This is the name to be given to the file containing the RuleSet
	 * information. This filename can also be specified on an input port.
	 */
	public FileParameter ruleSetFilenameParameter = new FileParameter(this,
			"ruleSetFilenameParameter");

	// input ports
	/**
	 * This is the name of the file containing the cellSet information. This is
	 * usually the output of the GarpPresampleLayers actor.
	 */
	public TypedIOPort cellSetFileName = new TypedIOPort(this,
			"cellSetFileName", true, false);
	/**
	 * This is the name to be given to the file containing the RuleSet
	 * information.
	 */
	public TypedIOPort ruleSetFilename = new TypedIOPort(this,
			"ruleSetFilename", true, false);
	// output ports
	/**
	 * This is the name of the file containing the RuleSet information. The port
	 * is only fired once the ruleset has been created. It is usually used as a
	 * trigger.
	 */
	public TypedIOPort ruleSetFilenameOutput = new TypedIOPort(this,
			"ruleSetFilenameOutput", false, true);

	/**
	 * GarpAlgorithm Actor
	 */
	public GarpAlgorithm(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
		cellSetFileName.setTypeEquals(BaseType.STRING);
		ruleSetFilename.setTypeEquals(BaseType.STRING);
		ruleSetFilenameOutput.setTypeEquals(BaseType.STRING);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"66\" height=\"42\" " + "style=\"fill:white\"/>\n"
				+ "<text x=\"12\" y=\"16\" "
				+ "style=\"font-size:14; fill:blue; font-family:SansSerif\">"
				+ "GARP</text>\n" + "<text x=\"5\" y=\"34\" "
				+ "style=\"font-size:12; fill:blue; font-family:SansSerif\">"
				+ "Algorithm</text>\n" + "</svg>\n");

	}

	/**
   *
   */
	public void initialize() throws IllegalActionException {
	}

	/**
   *
   */
	public boolean prefire() throws IllegalActionException {
		return super.prefire();
	}

	/**
   *
   */
	public void fire() throws IllegalActionException {
		System.out.println("firing GarpAlgorithm");
		super.fire();
		String cellSetFileNameStr = "";
		String ruleSetFilenameStr = "";

		if (cellSetFileName.numberOfSources() > 0) {
			// get the cellset filename from the port
			if (!cellSetFileName.hasToken(0))
				return;
			StringToken cellsetfnToken = (StringToken) cellSetFileName.get(0);
			cellSetFileNameStr = cellsetfnToken.stringValue();
			cellSetFileNameParameter.setExpression(cellSetFileNameStr);
		} else {
			cellSetFileNameStr = cellSetFileNameParameter.asFile().getPath();
		}

		if (ruleSetFilename.numberOfSources() > 0) {
			// get the ruleset filename from the port
			if (!ruleSetFilename.hasToken(0))
				return;
			StringToken rulesetfnToken = (StringToken) ruleSetFilename.get(0);
			ruleSetFilenameStr = rulesetfnToken.stringValue();
			ruleSetFilenameParameter.setExpression(ruleSetFilenameStr);
		} else {
			ruleSetFilenameStr = ruleSetFilenameParameter.asFile().getPath();
		}
		// make the jni call to the c++ code
		System.out.println("Starting GarpAlgorithm JNI code");
		new GarpJniGlue().RunGarpAlgorithm(ruleSetFilenameStr,
				cellSetFileNameStr);

		ruleSetFilenameOutput.broadcast(new StringToken(ruleSetFilenameStr));

		System.out.println("Finished with GarpAlgorithm JNI code");
	}
}