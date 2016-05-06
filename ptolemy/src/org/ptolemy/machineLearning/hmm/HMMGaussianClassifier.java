
/* A sequence classifier for Gaussian emission HMMs

Copyright (c) 1998-2013 The Regents of the University of California.
All rights reserved.
Permission is hereby granted, without written agreement and without
license or royalty fees, to use, copy, modify, and distribute this
software and its documentation for any purpose, provided that the above
copyright notice and the following two paragraphs appear in all copies
of this software.

IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
ENHANCEMENTS, OR MODIFICATIONS.

PT_COPYRIGHT_VERSION_2
COPYRIGHTENDKEY

*/
package org.ptolemy.machineLearning.hmm;

import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.ArrayToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.MatrixToken;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.kernel.util.Workspace;

/**
<p>This actor performs Maximum-Likelihood classification of the partially-observed
Bayesian Network models. ClassifyObservations is designed to work with <i>
ExpectationMaximization</i>, which provides the Maximum-Likelihood model parameters
from which the observations are assumed to be drawn. The output is an integer array
of labels, representing the maximum-likelihood hidden state sequence of the given
model.

<p>
The user provides a set of parameter estimates as inputs to the model, and
The <i>mean</i>  is a double array input containing the mean estimate and
<i>sigma</i> is a double array input containing standard deviation estimate of
each mixture component. If the <i>modelType</i> is HMM, then an additional input,
<i>transitionMatrix</i> is provided, which is an estimate of the transition matrix
governing the Markovian process representing the hidden state evolution. The <i>prior
</i> input is an estimate of the prior state distribution.

 @author Ilge Akkaya
 @version $Id: HMMGaussianClassifier.java 68631 2014-03-16 10:14:10Z ilgea $
 @since Ptolemy II 10.0
 @Pt.ProposedRating Red (ilgea)
 @Pt.AcceptedRating
 */
public class HMMGaussianClassifier extends ObservationClassifier {
    /** Construct an actor with the given container and name.
     *  @param container The container.
     *  @param name The name of this actor
     *  @exception IllegalActionException If the actor cannot be contained
     *   by the proposed container.
     *  @exception NameDuplicationException If the container already has an
     *   actor with this name.
     */
    public HMMGaussianClassifier(CompositeEntity container, String name)
            throws NameDuplicationException, IllegalActionException {
        super(container, name);

        mean = new PortParameter(this, "mean");
        mean.setExpression("{0.0,3.0}");
        mean.setTypeEquals(new ArrayType(BaseType.DOUBLE));
        StringAttribute cardinality = new StringAttribute(mean.getPort(),
                "_cardinal");
        cardinality.setExpression("SOUTH");

        standardDeviation = new PortParameter(this, "standardDeviation");
        standardDeviation.setExpression("{10E-3,50E-3}");
        standardDeviation.setTypeEquals(new ArrayType(BaseType.DOUBLE));
        cardinality = new StringAttribute(standardDeviation.getPort(),
                "_cardinal");
        cardinality.setExpression("SOUTH");

        //_nStates = ((ArrayToken) meanToken).length();
        _nStates = ((ArrayToken) mean.getToken()).length();
        _mu = new double[_nStates];
        _sigma = new double[_nStates];
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public variables                  ////

    public PortParameter mean;

    public PortParameter standardDeviation;

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    public Object clone(Workspace workspace) throws CloneNotSupportedException {
        HMMGaussianClassifier newObject = (HMMGaussianClassifier) super
                .clone(workspace);
        newObject._mu = new double[_nStates];
        newObject._sigma = new double[_nStates];
        return newObject;
    }

    /** Consume the inputs and produce the outputs of the FFT filter.
     *  @exception IllegalActionException If a runtime type error occurs.
     */
    public void fire() throws IllegalActionException {
        super.fire();

        mean.update();
        standardDeviation.update();
        transitionMatrix.update();
        prior.update();

        // update array values and lengths
        _nStates = ((ArrayToken) mean.getToken()).length();
        _mu = new double[_nStates];
        _sigma = new double[_nStates];
        _priors = new double[_nStates];
        _transitionMatrixEstimate = new double[_nStates][_nStates];

        for (int i = 0; i < _nStates; i++) {
            _sigma[i] = ((DoubleToken) ((ArrayToken) standardDeviation
                    .getToken()).getElement(i)).doubleValue();
            _priors[i] = ((DoubleToken) ((ArrayToken) prior.getToken())
                    .getElement(i)).doubleValue();
            _mu[i] = ((DoubleToken) ((ArrayToken) mean.getToken())
                    .getElement(i)).doubleValue();
            for (int j = 0; j < _nStates; j++) {
                _transitionMatrixEstimate[i][j] = ((DoubleToken) ((MatrixToken) transitionMatrix
                        .getToken()).getElementAsToken(i, j)).doubleValue();
            }
        }
        if ((_nStates != _sigma.length)
                || (_nStates != _transitionMatrixEstimate.length)) {
            throw new IllegalActionException(this,
                    "Parameter guess vectors need to have the same length.");
        }
        if (_observations != null) {
            int[] classifyStates = new int[_observations.length];

            classifyStates = classifyHMM(_observations, _priors,
                    _transitionMatrixEstimate);

            IntToken[] _outTokenArray = new IntToken[classifyStates.length];
            for (int i = 0; i < classifyStates.length; i++) {
                _outTokenArray[i] = new IntToken(classifyStates[i]);
            }

            output.broadcast(new ArrayToken(BaseType.INT, _outTokenArray));
        }
    }

    protected double emissionProbability(double y, int hiddenState) {
        double s = _sigma[hiddenState];
        double m = _mu[hiddenState];
        return 1.0 / (Math.sqrt(2 * Math.PI) * s)
                * Math.exp(-0.5 * Math.pow((y - m) / s, 2));
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    private double[] _mu;

    private double[] _sigma;
}
