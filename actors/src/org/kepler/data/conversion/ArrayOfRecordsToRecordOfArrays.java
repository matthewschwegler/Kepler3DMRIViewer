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

package org.kepler.data.conversion;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.RecordToken;
import ptolemy.data.Token;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.MonotonicFunction;
import ptolemy.data.type.RecordType;
import ptolemy.data.type.Type;
import ptolemy.graph.InequalityTerm;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**

   Convert an array of records to a record of arrays.
   
   @author Daniel Crawl
   @version $Id: ArrayOfRecordsToRecordOfArrays.java 24234 2010-05-06 05:21:26Z welker $

 */
public class ArrayOfRecordsToRecordOfArrays extends TypedAtomicActor
{
    /** Construct an ArrayOfRecordsToRecordOfArrays with the given container
     *  and name.  
     */
    public ArrayOfRecordsToRecordOfArrays(CompositeEntity container, String name)
        throws NameDuplicationException, IllegalActionException
    {
        super(container, name);

        input = new TypedIOPort(this, "input", true, false);
        input.setTypeAtMost(new ArrayType(RecordType.EMPTY_RECORD));

        output = new TypedIOPort(this, "output", false, true);
        output.setTypeAtLeast(new FunctionTerm());

        _attachText("_iconDescription",
            "<svg>\n" + "<rect x=\"0\" y=\"0\" " +
            "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n" +
            "</svg>\n");
    }

    ///////////////////////////////////////////////////////////////////
    ////                     ports and parameters                  ////

    /** The input array of records. */
    public TypedIOPort input;
    
    /** The output record of arrays. */
    public TypedIOPort output;

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Convert an input array of records.  */
    public void fire() throws IllegalActionException
    {
        super.fire();

        ArrayToken array = (ArrayToken)input.get(0);
        int length = array.length();
        
        Map<String,Token[]> map = new HashMap<String,Token[]>();

        RecordType elementType = (RecordType)array.getElementType();
        Set<String> labels = elementType.labelSet();
        for(String label : labels)
        {
            map.put(label, new Token[length]);
        }

        for(int i = 0; i < length; i++)
        {
            RecordToken element = (RecordToken)array.getElement(i);
            for(String label : labels)
            {
                Token[] outArray = map.get(label);
                //XXX need to clone the token
                outArray[i] = element.get(label);
            }
        }

        // replace Token[] with (Array)Token
        Map<String,Token> outMap = new HashMap<String,Token>();
        for(String label : labels)
        {
            Token[] outArray = map.get(label); 
            outMap.put(label, new ArrayToken(outArray));
        }
        
        output.broadcast(new RecordToken(outMap));
    }

    /** A class to determine the type of the output port. */
    private class FunctionTerm extends MonotonicFunction
    {
        /** Get the type. Return a type representing the output record of
         *  arrays.
         */
        public Object getValue()
        {
            Type retval = BaseType.UNKNOWN;
            
            Type inType = input.getType();
            if(inType instanceof ArrayType)
            {
                Type inRecType = ((ArrayType)inType).getElementType();
                if(inRecType instanceof RecordType)
                {
                    Set<String> labels = ((RecordType)inRecType).labelSet();
                    Type[] outTypes = new Type[labels.size()];
                    int i = 0;
                    for(String label : labels)
                    {
                        outTypes[i] = new ArrayType(((RecordType)inRecType).get(label));
                        i++;
                    }
                    retval = new RecordType(labels.toArray(new String[0]), outTypes);
                }
            }

            //System.out.println("returning type = " + retval);
            return retval;
        }

        /** Return the variables. Return the input port term. */
        public InequalityTerm[] getVariables()
        {
            return new InequalityTerm[] { input.getTypeTerm() };
        }
    }
}