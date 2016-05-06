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

package com.numericsolutions.geomodeltools;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.DoubleToken;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * Density Grid Actor
 */
public class DensityGridActor extends TypedAtomicActor {
	// input ports
	public TypedIOPort inDataFile = new TypedIOPort(this, "inDataFile", true,
			false);
	public TypedIOPort radius = new TypedIOPort(this, "radius", true, false);
	public TypedIOPort xmin = new TypedIOPort(this, "xmin", true, false);
	public TypedIOPort xmax = new TypedIOPort(this, "xmax", true, false);
	public TypedIOPort ymin = new TypedIOPort(this, "ymin", true, false);
	public TypedIOPort ymax = new TypedIOPort(this, "ymax", true, false);
	public TypedIOPort dx = new TypedIOPort(this, "dx", true, false);
	public TypedIOPort dy = new TypedIOPort(this, "dy", true, false);
	public TypedIOPort pval = new TypedIOPort(this, "pval", true, false);
	// output ports
	public TypedIOPort gridContents = new TypedIOPort(this, "gridContents",
			false, true);

	/**
	 * Constructor
	 */
	public DensityGridActor(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		inDataFile.setTypeEquals(BaseType.STRING);
		radius.setTypeEquals(BaseType.DOUBLE);
		xmin.setTypeEquals(BaseType.DOUBLE);
		xmax.setTypeEquals(BaseType.DOUBLE);
		ymin.setTypeEquals(BaseType.DOUBLE);
		ymax.setTypeEquals(BaseType.DOUBLE);
		dx.setTypeEquals(BaseType.DOUBLE);
		dy.setTypeEquals(BaseType.DOUBLE);
		pval.setTypeEquals(BaseType.DOUBLE);
		gridContents.setTypeEquals(BaseType.STRING);
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
		System.out.println("firing BackProp");
		super.fire();

		StringToken inDataFileToken = (StringToken) inDataFile.get(0);
		String inDataFileStr = inDataFileToken.stringValue();

		DoubleToken radToken = (DoubleToken) radius.get(0);
		double radDouble = radToken.doubleValue();

		DoubleToken xMin = (DoubleToken) xmin.get(0);
		double xMinDouble = xMin.doubleValue();

		DoubleToken xMax = (DoubleToken) xmax.get(0);
		double xMaxDouble = xMax.doubleValue();

		DoubleToken yMin = (DoubleToken) ymin.get(0);
		double yMinDouble = yMin.doubleValue();

		DoubleToken yMax = (DoubleToken) ymax.get(0);
		double yMaxDouble = yMax.doubleValue();

		DoubleToken dX = (DoubleToken) dx.get(0);
		double dXDouble = dX.doubleValue();

		DoubleToken dY = (DoubleToken) dy.get(0);
		double dYDouble = dY.doubleValue();

		DoubleToken pVal = (DoubleToken) pval.get(0);
		double pValDouble = pVal.doubleValue();

		System.out.println("Calling Density Gridder JNI Code");

		String output = (new GeomodelGlue()).runGridDensityByArrayDimension(
				inDataFileStr, radDouble, xMinDouble, xMaxDouble, yMinDouble,
				yMaxDouble, dXDouble, dYDouble, pValDouble);
		gridContents.broadcast(new StringToken(output));
		System.out.println("done");
	}
}