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

package org.kepler.actor.io;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.geon.FileWrite;

import ptolemy.data.ArrayToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.RecordType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**

 Write an array of records to a table.

 @author Daniel Crawl
 @version $Id: ArrayOfRecordsWriter.java 24234 2010-05-06 05:21:26Z welker $

 */

public class ArrayOfRecordsWriter extends FileWrite
{
    /** Construct an ArrayOfRecordsWriter.  */
    public ArrayOfRecordsWriter(CompositeEntity container, String name)
        throws NameDuplicationException, IllegalActionException
    {
        super(container, name);

        tableType = new StringParameter(this, "table type");
        tableType.setExpression("text");
        tableType.addChoice("text");
        tableType.addChoice("HTML");

        columns = new StringParameter(this, "columns");
        columnNames = new StringParameter(this, "columnNames");

        format = new StringParameter(this, "format");
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////


    /** Read the formatting parameter. */
    public void preinitialize() throws IllegalActionException
    {
        super.preinitialize();

        _formatStr = format.stringValue();
    }

    /** The type of table output. */
    public StringParameter tableType;

    /** Comma-separated list of name and order of columns to output. */
    public StringParameter columns;

    /** Comma-separated list of column names to write in header of
     *  table. Note: if not empty, the number of names must be the
     *  same as the number of names in <i>columns</i>.
     */
    public StringParameter columnNames;

    /** Formatting string for numeric data. */
    public StringParameter format;

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Set the type constraints and multiport property of the input
     *  port. This is done here so that derived classes can override it.
     */
    protected void _setInputConstraints() throws IllegalActionException
    {
        input.setTypeAtMost(new ArrayType(RecordType.EMPTY_RECORD));
    }
    
    /** Write the specified token to the current writer.
     *  This is protected so that derived classes can modify the
     *  format in which the token is written.
     *  @param token The token to write.
     */
    protected void _writeToken(Token token) throws IllegalActionException 
    {
        ArrayToken arrayToken = (ArrayToken)token;

        // determine the columns to output
        String columnStr = ((StringToken)columns.getToken()).stringValue();
        List<String> columnsList = null;
        List<String> headersList = null;

        // see if anything in columns parameter
        if(columnStr.length() > 0)
        {
            // put the names in an ordered list
            String[] columnsArray = columnStr.split("\\s*,\\s*");
            columnsList = Arrays.asList(columnsArray);

            String columnNamesStr = ((StringToken)columnNames.getToken()).stringValue();
            
            if(columnNamesStr.length() > 0)
            {
                String[] headersArray = columnNamesStr.split("\\s*,\\s*"); 

                // make sure sizes match
                if(headersArray.length != columnsArray.length)
                {
                    throw new IllegalActionException(this, "Different number " +
                        " of columns and column names.");
                }

                headersList = Arrays.asList(headersArray);
            }
            else
            {
                headersList = columnsList;
            }

        }
        else
        {
            // get the first record and put the labels in an
            // ordered list
            Set<String> labels = ((RecordToken)arrayToken.getElement(0)).labelSet();
            columnsList = new LinkedList<String>(labels);
        }

        String tableTypeStr = ((StringToken)tableType.getToken()).stringValue();

        if(tableTypeStr.equals("text"))
        {
            // output the header
            for(String header: headersList)    
            {
                _writer.print(header + " ");
            }
            _writer.println();

            // output the data
            for(int i = 0; i < arrayToken.length(); i++)
            {
                RecordToken recordToken = 
                    (RecordToken)arrayToken.getElement(i);
                for(String str : columnsList)
                {
                    String output = _formatValue(recordToken.get(str));
                    _writer.print(output + " ");
                }
                _writer.println();
            }
        }
        else if(tableTypeStr.equals("HTML"))
        {
            _writer.println("<TABLE BORDER=1>");

            // output the column names
            _writer.print("<TR>");
            for(String header : headersList)
            {
                _writer.print("<TD><B>" + header + "</B></TD>");
            }
            _writer.println("</TR>");

            // output the data
            for(int i = 0; i < arrayToken.length(); i++)
            {
                _writer.print("<TR>");
                RecordToken recordToken =
                    (RecordToken)arrayToken.getElement(i);
                for(String str : columnsList) 
                {
                    String output = _formatValue(recordToken.get(str));
                    _writer.print("<TD>" + output + "</TD>");
                }
                _writer.println("</TR>");
            }

            _writer.println("</TABLE>");
        }
        else
        {
            throw new IllegalActionException(this, "Unsupported table type: " +
                tableTypeStr);
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private methods                   ////

    /** Format a value based on the format parameter. */
    private String _formatValue(Token token)
    {
        String retval = token.toString();
        // XXX what about other types of tokens?
        if(_formatStr.length() > 0 && (token instanceof DoubleToken))
        {
            double val = ((DoubleToken)token).doubleValue();
            retval = String.format(_formatStr, val);
        }
        return retval;
    }

    /** Value of format parameter. */
    private String _formatStr;
}