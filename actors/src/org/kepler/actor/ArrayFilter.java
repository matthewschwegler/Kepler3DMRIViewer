/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
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

package org.kepler.actor;

import java.util.LinkedList;
import java.util.List;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.IntToken;
import ptolemy.data.ScalarToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**

   Filter elements from an array.

   @author Daniel Crawl
   @version $Id: ArrayFilter.java 24234 2010-05-06 05:21:26Z welker $

 */

public class ArrayFilter extends TypedAtomicActor 
{

   /** Construct an ArrayFilter with the given container and name. */
    public ArrayFilter(CompositeEntity container, String name)
        throws NameDuplicationException, IllegalActionException  
    {
        super(container, name);
    
        input = new TypedIOPort(this, "input", true, false);
        input.setTypeAtLeast(ArrayType.ARRAY_UNSIZED_BOTTOM);

        // XXX i'd prefer to have min or max be optional and
        // not have defaults. however, this would require more
        // complicated methods to set the type.

        min = new PortParameter(this, "min");
        min.setTypeAtMost(BaseType.SCALAR);
        min.getPort().setTypeAtMost(BaseType.SCALAR);
        min.setExpression("-9999999");

        max = new PortParameter(this, "max");
        max.setTypeAtMost(BaseType.SCALAR);
        max.getPort().setTypeAtMost(BaseType.SCALAR);
        max.setExpression("9999999");

        getIndices = new Parameter(this, "getIndices");
        getIndices.setToken(BooleanToken.FALSE);
        getIndices.setTypeEquals(BaseType.BOOLEAN);

        output = new TypedIOPort(this, "output", false, true);

        _attachText("_iconDescription", "<svg>\n" +
                "<rect x=\"0\" y=\"0\" "
                + "width=\"60\" height=\"20\" "
                + "style=\"fill:white\"/>\n" +
                "</svg>\n");
    }

    ///////////////////////////////////////////////////////////////////
    ////                     ports and parameters                  ////

    /** The array to filter. */
    public TypedIOPort input;

    /** Remove elements below this value. */
    public PortParameter max;
    
    /** Remove elements above this value. */
    public PortParameter min;

    /** If true, return the <b>indices</b> of the array values within
     *  (or equals) min and max. If false (default), return the
     *  <b>values</b> of the array within (or equals) min and max.
     */
    public Parameter getIndices;

    /** The filtered array. */
    public TypedIOPort output;

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Read getIndices and set the output type. */
    public void preinitialize() throws IllegalActionException
    {
        super.preinitialize();

        _wantIndices = ((BooleanToken)getIndices.getToken()).booleanValue();

        // if we output indices, output type is array of integers.
        if(_wantIndices)
        {
            output.setTypeEquals(new ArrayType(BaseType.INT));
        }
        // otherwise output type is array of input type.
        else
        {
            output.setTypeSameAs(input);
        }
    }

    /** Filter elements in the input array.  */
    public void fire() throws IllegalActionException 
    {
        super.fire();
   
        ArrayToken array = (ArrayToken)input.get(0);
        
        max.update();
        Token token = max.getToken();

        if(! (token instanceof ScalarToken))
        {
            throw new IllegalActionException(this, "max must have scalar " +
                "value; given value: " + token); 
        }

        ScalarToken maxToken = (ScalarToken)token;
        
        min.update();
        token = min.getToken();

        if(! (token instanceof ScalarToken))
        {
            throw new IllegalActionException(this, "min must have scalar " +
                "value; given value: " + token);
        }

        ScalarToken minToken = (ScalarToken)min.getToken();
        
        List<Token> outputList = new LinkedList<Token>();

        for(int i = 0; i < array.length(); i++)
        {
            ScalarToken curToken = (ScalarToken)array.getElement(i);
           
            // see if current element is within thresholds
            if((curToken.isLessThan(maxToken) == BooleanToken.TRUE ||
                curToken.isEqualTo(maxToken) == BooleanToken.TRUE) &&
                (curToken.isGreaterThan(minToken) == BooleanToken.TRUE ||
                curToken.isEqualTo(minToken) == BooleanToken.TRUE))
            {
                // see if output should be values or indices
                if(_wantIndices)
                {
                    outputList.add(new IntToken(i));
                }
                else
                {
                    outputList.add(curToken); 
                }
            }
        }

        if(outputList.size() == 0)
        {
            output.broadcast(new ArrayToken(input.getType())); 
        }
        else
        {
            output.broadcast(new ArrayToken(outputList.toArray(new Token[0])));
        }
    }

    /** The value of the getIndices parameter. */
    private boolean _wantIndices;
}