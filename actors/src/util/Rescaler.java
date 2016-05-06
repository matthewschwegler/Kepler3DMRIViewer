/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
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

package util;

import java.util.Vector;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.DoubleMatrixToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;

/**
 * This actor rescales an array of numbers. The actor takes an min, max and a
 * matrix of integers as inputs. It outputs a matrix of integers that have been
 * rescaled to be within the min and the max parameters.
 * 
 * @author Dan Higgins NCEAS UC Santa Barbara
 */
public class Rescaler extends TypedAtomicActor {
	/** the desired min in the rescale **/
	public PortParameter min;
	/** the desired max in the rescale **/
	public PortParameter max;
	/**
	 * the matrix of integers to rescale. note that only the first dimension of
	 * the matrix is used. The matrix will only be indexed as int[0][i]
	 **/
	public TypedIOPort input;
	/**
	 * the matrix of rescaled integers. Note that this is a 1D matrix, even
	 * though IntMatrixToken will return it as a 2D matrix. Only the first
	 * dimension is used. see note for input.
	 **/
	public TypedIOPort output;

	private double mini;
	private double maxi;
	private Vector vals;
	private boolean newval;
	private double minval;
	private double maxval;

	/**
	 *@param workspace
	 *@exception IllegalActionException
	 *@exception NameDuplicationException
	 */
	public Rescaler(Workspace workspace) throws IllegalActionException,
			NameDuplicationException {
		super(workspace);
		setup();
	}

	/**
	 *@param container
	 *            The container.
	 *@param name
	 *            The name of this actor.
	 *@exception IllegalActionException
	 *                If the container is incompatible with this actor.
	 *@exception NameDuplicationException
	 *                If the name coincides with an actor already in the
	 *                container.
	 */
	public Rescaler(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
		setup();
	}

	/**
	 * get the portparam values
	 */
	public void initialize() throws IllegalActionException {
		vals = new Vector();
		minval = 0;
		maxval = 0;
		newval = true;
		DoubleToken mintoken = (DoubleToken) min.getToken();
		DoubleToken maxtoken = (DoubleToken) max.getToken();
		mini = mintoken.doubleValue();
		maxi = maxtoken.doubleValue();
	}

	/**
	 * Output the data read from the port
	 * 
	 *@exception IllegalActionException
	 *                If there is no director.
	 */
	public void fire() throws IllegalActionException {
		// get the value from the input port
		double[][] val = ((DoubleMatrixToken) input.get(0)).doubleMatrix();
		// add the value to the vector and check to see if it's bigger than
		// maxval or smaller than minval
		for (int i = 0; i < val[0].length; i++) {
			double vali = val[0][i];
			vals.addElement(new Double(vali));
			if (newval) { // just do this when minval and maxval haven't been
							// inited yet
				minval = vali;
				maxval = vali;
				newval = false;
			} else { // do this the rest of the time
				if (vali < minval) {
					minval = vali;
				} else if (vali > maxval) {
					maxval = vali;
				}
			}
		}

		// do the scaling
		vals = scale(mini, maxi, minval, maxval, vals);

		double[][] valarr = new double[1][vals.size()];
		for (int i = 0; i < vals.size(); i++) {
			valarr[0][i] = ((Double) vals.elementAt(i)).doubleValue();
		}
		// output vals
		output.broadcast(new DoubleMatrixToken(valarr));
	}

	/**
	 *@return boolean
	 *@exception IllegalActionException
	 *                If the superclass throws it.
	 */
	public boolean prefire() throws IllegalActionException {
		if (min.getToken() != null && // make sure we have the necessary data
				max.getToken() != null && input.getWidth() > 0) {
			return true;
		}

		return false;
	}

	/**
	 * do the rescale based on the passed values. returns the scaled values in a
	 * vector
	 * 
	 * @param min
	 *            the desired minimum scale value
	 * @param max
	 *            the desired maximum scale value
	 * @param minval
	 *            the smalled value in the vals vector
	 * @param maxval
	 *            the greatest value in the vals vector
	 * @param vals
	 *            the vector of values to rescale
	 * @return vector of rescaled values
	 */
	private Vector scale(double min, double max, double minval, double maxval,
			Vector vals) {
		/*
		 * algorithm (Thank you John Harris and Dan Higgins): 1. subtract minval
		 * from each val in vals 2. divide each val in vals by maxval 3. divide
		 * (max-min) by max val in step 2 4. for each val in vals apply y=mx+b
		 * where m=output of 3 x=output of 2 b=min
		 * 
		 * y is the new rescaled value
		 */

		Vector out = new Vector();
		double delta = max - min;
		double stepmaxval = 0;
		for (int i = 0; i < vals.size(); i++) {
			double val = ((Double) vals.elementAt(i)).doubleValue();
			val -= minval; // step 1
			val /= maxval; // step 2
			if (i == 0) { // collect the largest value after step 2 so we can do
							// step 3
				stepmaxval = val;
			} else {
				if (val > stepmaxval) {
					stepmaxval = val;
				}
			}
			out.addElement(new Double(val));
		}
		// step 3
		double rescaleFactor = delta / stepmaxval;
		// step 4
		Vector done = new Vector();
		for (int i = 0; i < out.size(); i++) {
			double outval = ((Double) out.elementAt(i)).doubleValue();
			double finalval = (rescaleFactor * outval) + min; // y=mx+b
			done.addElement(new Double(finalval));
		}
		return done;
	}

	/**
	 * sets up the ports and params for the constructors.
	 */
	private void setup() throws IllegalActionException,
			NameDuplicationException {
		min = new PortParameter(this, "min");
		max = new PortParameter(this, "max");

		input = new TypedIOPort(this, "input", true, false);
		output = new TypedIOPort(this, "output", false, true);

		input.setTypeEquals(BaseType.DOUBLE_MATRIX);
		output.setTypeEquals(BaseType.DOUBLE_MATRIX);
	}
}