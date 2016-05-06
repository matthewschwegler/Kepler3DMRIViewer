/* An actor that reads NetCDF files.
 * 
 * Copyright (c) 2011 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-09-18 11:39:51 -0700 (Tue, 18 Sep 2012) $' 
 * '$Revision: 30701 $'
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
package org.kepler.data.netcdf;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ptolemy.actor.TypedIOPort;
import ptolemy.actor.lib.LimitedFiringSource;
import ptolemy.actor.parameters.FilePortParameter;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleMatrixToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.FloatToken;
import ptolemy.data.IntMatrixToken;
import ptolemy.data.IntToken;
import ptolemy.data.LongToken;
import ptolemy.data.ShortToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;
import ptolemy.math.DoubleMatrixMath;
import ptolemy.math.IntegerMatrixMath;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * This actor reads values from a NetCDF file. The <i>constraint</i> parameter
 * specifies the variables to read and optionally how to subset them. For each
 * variable, an output port with the same name is created. The type of the
 * output port depends on how many dimensions are left unconstrained in the
 * variable: scalar tokens for zero, array tokens for one, and matrix tokens for
 * two. Unconstrained dimensions of greater than two are not supported.
 * <p>
 * The syntax for the <i>constraint</i> parameter is a space-separated list of
 * variables. Each variable may optionally have a set of dimensional constraints
 * in the form of [start:end:stride], where start is the starting index, end is
 * the ending index, and stride is the increment. A dimension may be left
 * unconstrained by specifying [:]. For example, suppose the variable is a
 * two-dimensional matrix z[x,y]. To read the entire matrix, use z. To read all
 * the values where y = 3, use z[:][3].
 * <p>
 * 
 * @author Daniel Crawl
 * @version $Id: NetCDFReader.java 30701 2012-09-18 18:39:51Z crawl $
 * 
 *          TODO
 * 
 *          test reading hdf5, other types
 * 
 */
public class NetCDFReader extends LimitedFiringSource {

    public NetCDFReader(CompositeEntity container, String name)
            throws NameDuplicationException, IllegalActionException {
        super(container, name);
        
        filename = new FilePortParameter(this, "filename");
        
        constraint = new PortParameter(this, "constraint");
        constraint.setStringMode(true);
        constraint.getPort().setTypeEquals(BaseType.STRING);
   
        // hide output port name
        new Attribute(output, "_hide");
    }
    
    /** React to a change in an attribute. */
    public void attributeChanged(Attribute attribute) throws IllegalActionException
    {
        if(attribute == filename)
        {
            _updateFileName();
            _updateOutputPorts();
        }
        else if(attribute == constraint)
        {
            _parseConstraintExpression();
            _updateOutputPorts();
        }
        else
        {
            super.attributeChanged(attribute);
        }
    }
    
    /** Clone this actor into the specified workspace.
     *  @param workspace The workspace for the cloned object.
     *  @exception CloneNotSupportedException If cloned ports cannot have
     *   as their container the cloned entity (this should not occur), or
     *   if one of the attributes cannot be cloned.
     *  @return A new NetCDFReader.
     */
    public Object clone(Workspace workspace) throws CloneNotSupportedException
    {
        NetCDFReader newObject = (NetCDFReader) super.clone(workspace);
        newObject._constraint = null;
        newObject._constraintMap = new HashMap<String,Section>();
        newObject._filenameStr = null;
        newObject._ncFile = null;
        return newObject;
    }
    
    public void fire() throws IllegalActionException
    {
        // read tokens in port parameters
        constraint.update();
        filename.update();
                
        // output the variables for each connected output port.
        for(Object object : outputPortList())
        {
            TypedIOPort port = (TypedIOPort)object;
            if(port.numberOfSinks() > 0)
            {
                _outputData(port);
            }
        }
    }
      
    public void initialize() throws IllegalActionException
    {
        super.initialize();
        _amFiring = true;
        
        if(_ncFile == null) {
            _openFile();
        }
    }
    
    /** Close the NetCDF file. */
    public void wrapup() throws IllegalActionException
    {
        _amFiring = false;
        _closeFile();
        super.wrapup();        
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public fields                     ////

    /** The name of the NetCDF file to read. */
    public FilePortParameter filename;
    
    /** Space-separated list of variables with an optional set of
     *  constraints. Each dimension may be constrained using the
     *  syntax [start:end:stride], or use [:] for no constraint.
     */
    public PortParameter constraint;
    
    ///////////////////////////////////////////////////////////////////
    ////                         private methods                   ////

    /** Close the NetCDF file. */
    private void _closeFile() throws IllegalActionException
    {
        if(_ncFile != null) {
            try {
                _ncFile.close();
                _ncFile = null;
            } catch (IOException e) {
                throw new IllegalActionException(this, e, "Error closing " + _filenameStr);
            }
        }
        
    }
        
    /** Open the NetCDF file. */
    private void _openFile() throws IllegalActionException
    {     
        if(_filenameStr != null && !_filenameStr.isEmpty())
        {
            try {
                _ncFile = NetcdfFile.open(_filenameStr);
            } catch (IOException e) {
                throw new IllegalActionException(this, e, "Error opening " + _filenameStr);
            }
        }
    }

    /** Read a token from the file name port parameter. */
    private void _updateFileName() throws IllegalActionException
    {
        Token token = filename.getToken();
        if(token != null)
        {
            String fileStr = ((StringToken)token).stringValue();
            if(_filenameStr == null || !_filenameStr.equals(fileStr))
            {
                _filenameStr = fileStr;
                
                // if the old file is open, close and open the new one
                if(_ncFile != null) {
                    _closeFile();
                }
                
                if(_ncFile == null) {
                    _openFile();
                }
            }
        }
    }
    
    private void _updateOutputPorts() throws IllegalActionException
    {
        if(!_amFiring && _filenameStr != null && !_filenameStr.isEmpty() && _constraint != null)
        {
        
            if(_ncFile == null) {
                _openFile();
            }

            Set<String> variableNames = new HashSet<String>();
            List<Variable> variables = null;
                        
            // see if there are constraints
            if(_constraintMap.size() > 0)
            {
                // add all the variables in the constraints
                variableNames.addAll(_constraintMap.keySet());
                variables = new LinkedList<Variable>();
                for(String name : variableNames)
                {
                    Variable variable = _ncFile.findVariable(name);
                    if(variable == null)
                    {
                        throw new IllegalActionException(this, "Variable " + name + " is in constraint " +
                            "expression, but not found in file " + _filenameStr);
                    }
                    variables.add(variable);
                }
            }
            else
            {
                // add all the variables in the file
                variables = _ncFile.getVariables();
                for(Variable variable : variables)
                {
                    variableNames.add(variable.getFullName());                
                }
            }
        
            // add ports and set types
            for(Variable variable : variables)
            {
                String name = variable.getFullName();

                // see if we need to add the port
                TypedIOPort port = (TypedIOPort) getPort(name);
                if(port == null)
                {
                    try
                    {
                        port = new TypedIOPort(this, name, false, true);
                    }
                    catch (NameDuplicationException e)
                    {
                        throw new IllegalActionException(this, e, "Error creating port " + name);
                    }
                }
                // see if variable is named "output". we already have port called output,
                // so unhide it if it is hidden.
                else if(name.equals("output"))
                {
                    Attribute attribute = port.getAttribute("_hide");
                    if(attribute != null)
                    {
                        try
                        {
                            attribute.setContainer(null);
                        }
                        catch (NameDuplicationException e)
                        {
                            throw new IllegalActionException(this, e, "Unable to show output port.");
                        }
                    }
                }

                Type oldType = port.getType();

                // set the port type based on the netcdf type and decimation
                Type newType = _getTokenTypeForVariable(variable);

                if(!oldType.equals(newType))
                {
                    port.setTypeEquals(newType);
                    //System.out.println("setting type for " + name);
                }
            }
            
            // delete ports for non-existing variables
            for(Object obj : outputPortList())
            {
                TypedIOPort port = (TypedIOPort) obj;
                String portName = port.getName();
                if(!variableNames.contains(portName))
                {
                    // can't delete output port since belongs to parent class
                    if(portName.equals("output"))
                    {
                        if(port.getAttribute("_hide") == null)
                        {
                            // hide output port
                            try
                            {
                                new Attribute(port, "_hide");
                            }
                            catch (NameDuplicationException e)
                            {
                                throw new IllegalActionException(this, e, "Unable to hide output port.");
                            }
                        }
                    }
                    else
                    {
                        // remove port
                        try
                        {
                            port.setContainer(null);
                        }
                        catch (NameDuplicationException e)
                        {
                            throw new IllegalActionException(this, e, "Error deleting " + port.getName());
                        }
                    }
                }
            }
        }
    }
    
    /** Get the number of dimensions of a variable after decimating. */
    private int _getDimensionsRemaining(Variable variable)
    {
        int decimationAmount = 0;
        
        // see if this variable was decimated in the constraint expression
        Section section = _constraintMap.get(variable.getFullName());
        
        if(section != null)
        {
            for(Range range : section.getRanges())
            {
                // NOTE: range can be null if decimation specified as [:]
                if(range != null && range.length() == 1)
                {
                    decimationAmount++;
                }
            }
        }
        
        return variable.getShape().length - decimationAmount;
    }
    
    private Type _getTokenTypeForVariable(Variable variable) throws IllegalActionException
    {
        
        Type retval = null;
        
        if(variable.isMetadata())
        {
            throw new IllegalActionException(this, "variable " + variable.getFullName() + " is metadata.");
        }
        
        String name = variable.getFullName();
                
        DataType dataType = variable.getDataType();
        
        int dimensionsRemaining = _getDimensionsRemaining(variable);
        
        //System.out.println("dim rem for " + name + " is " + dimensionsRemaining);
        
        if(dimensionsRemaining < 0)
        {
            throw new IllegalActionException(this, "Variable " + name + " has " +
                variable.getShape().length + " dimension(s), but more " +
                " dimensions have been constrained.");
        }
        else if(dimensionsRemaining < 2)
        {            
            switch(dataType)
            {
            case DOUBLE:
                retval = BaseType.DOUBLE;
                break;
            case FLOAT:
                retval = BaseType.FLOAT;
                break;
            case SHORT:
                retval = BaseType.SHORT;
                break;
            case INT:
                retval = BaseType.INT;
                break;
            case LONG:
                retval = BaseType.LONG;
                break;
            case BOOLEAN:
                retval = BaseType.BOOLEAN;
                break;
            default:
                throw new IllegalActionException(this, "Variable " + name +
                        "has unsupported data type: " + dataType);
            }
            
            if(dimensionsRemaining == 1)
            {
                retval = new ArrayType(retval); 
            }
        }
        else if(dimensionsRemaining == 2)
        {
            switch(dataType)
            {
            case DOUBLE:
            case FLOAT:
                retval = BaseType.DOUBLE_MATRIX;
                break;
            case INT:
                retval = BaseType.INT_MATRIX;
                break;
            case LONG:
                retval = BaseType.LONG_MATRIX;
                break;
            case BOOLEAN:
                retval = BaseType.BOOLEAN_MATRIX;
                break;
            default:
                throw new IllegalActionException(this, "Unsupported matrix " +
                    "type for variable " + name + "(" + dataType + ")");
            }
        }
        else if(dimensionsRemaining > 2)
        {
            throw new IllegalActionException(this, "Variable " + name + " has" +
                " been decimated to have more than two dimensions, which is" +
                " currently not supported.");
        }
        
        return retval;
    }
    
    /** Write the data from the file to an output port. */
    private void _outputData(TypedIOPort port) throws IllegalActionException
    {
        Token token = null;

        String name = port.getName();
        Variable variable = _ncFile.findVariable(name);
        if(variable == null)
        {
            throw new IllegalActionException("Could not find variable " + name + " in file " + _filenameStr);
        }
        
        int dimensionsRemaing = _getDimensionsRemaining(variable);

        Array array;
        try {
            Section section = _constraintMap.get(name);
            if(section != null) {
                array = variable.read(section);
            } else {
                array = variable.read(null, variable.getShape());
            }
        } catch (Exception e) {
            throw new IllegalActionException(this, e, "Unable to read variable " + name);
        }

        // get the shape from the read array
        int[] readShape = array.getShape();

        Object arrayStorage = array.getStorage();

        DataType dataType = variable.getDataType();

        if(dimensionsRemaing == 0)
        {
           switch(dataType)
           {
           case DOUBLE:
               token = new DoubleToken(((double[])arrayStorage)[0]);
               break;
           case FLOAT:
               token = new FloatToken(((float[])arrayStorage)[0]);
               break;
           case SHORT:
               token = new ShortToken(((short[])arrayStorage)[0]);
               break;
           case INT:
               token = new IntToken(((int[])arrayStorage)[0]);
               break;
           case LONG:
               token = new LongToken(((long[])arrayStorage)[0]);
               break;
           case BOOLEAN:
               token = new BooleanToken(((boolean[])arrayStorage)[0]);
               break;
           default:
               throw new IllegalActionException(this, "Variable " + name +
                   " has unsupported data type: " + dataType);
           }
        }
        else if(dimensionsRemaing == 1)
        {
            int length = java.lang.reflect.Array.getLength(arrayStorage);
            StringBuilder arrayStr = new StringBuilder("{");
            for(int i = 0; i < length - 1; i++)
            {
                arrayStr.append(java.lang.reflect.Array.get(arrayStorage, i));
                arrayStr.append(",");
            }
            arrayStr.append(java.lang.reflect.Array.get(arrayStorage, length - 1));
            arrayStr.append("}");
            token = new ArrayToken(arrayStr.toString());
        }
        else // dimensionsRemaing == 2
        {
            switch(dataType)
            {
            case DOUBLE:
                
                /*
                double[][] data = new double[shape[0]][shape[1]];
                for(int i = 0; i < shape[0]; i++)
                {
                    for(int j = 0; j < shape[1]; j++)
                    {
                        data[i][j] = arrayDouble.get(i,j);
                    }
                }
                */
                
                token = new DoubleMatrixToken((double[])array.getStorage(),
                        readShape[0], readShape[1]);

                // XXX take the transpose to get elements in correct position
                token = new DoubleMatrixToken(
                        DoubleMatrixMath.transpose(((DoubleMatrixToken)token).doubleMatrix()));
                
                
                //System.out.println(getName() + ": array 5000,73 = " + arrayDouble.get(5000, 73));
                //System.out.println("rows = " + ((DoubleMatrixToken)token).getRowCount());
                //System.out.println("cols = " + ((DoubleMatrixToken)token).getColumnCount());
                //System.out.println(getName() + ": token 5000,73 = " + 
                       //((DoubleMatrixToken)token).getElementAt(73, 5000));

                
                break;
                
            case INT:
                                                
                token = new IntMatrixToken((int[])array.getStorage(),
                        readShape[0], readShape[1]);

                // XXX take the transpose to get elements in correct position
                token = new IntMatrixToken(
                        IntegerMatrixMath.transpose(((IntMatrixToken)token).intMatrix()));
                
                break;
                
             default:
                 throw new IllegalActionException(this, "Variable " + name +
                         " has unsupported data type: " + dataType);
            }            
        }
        
        if(token != null)
        {
            //System.out.println(getFullName() + " output token type " + token.getType());
            port.broadcast(token);
        }           
    }
    
    private void _parseConstraintExpression() throws IllegalActionException
    {
        String origConstraintStr = ((StringToken)constraint.getToken()).stringValue();
        // see if constraint expression has changed
        if(_constraint == null || !_constraint.equals(origConstraintStr))
        {
            _constraint = origConstraintStr.trim();
            _constraintMap.clear();
            
            String constraintStr = _constraint;
            while(constraintStr.length() > 0)
            {
                Matcher matcher = _VARIABLE_PATTERN.matcher(constraintStr);
                if(!matcher.matches())
                {
                    throw new IllegalActionException(this, "Bad constraint: " + origConstraintStr);
                }
                String variableName = matcher.group(1);
                
                Section section = null;
                String sectionStr = "";
                if(matcher.groupCount() > 1)
                {
                    sectionStr = matcher.group(2);
                    String formattedSectionStr = 
                        sectionStr.replaceAll("\\[", "").replaceAll("\\]", ",").replaceAll(",$", "");
                    
                    try {
                        section = new Section(formattedSectionStr);
                    } catch (InvalidRangeException e) {
                        throw new IllegalActionException(this, e, "Invalid decimation " + sectionStr);
                    }
                }
                
                _constraintMap.put(variableName, section);
                
                constraintStr = constraintStr.substring(variableName.length() + sectionStr.length());
            }
        }
    }
        
    ///////////////////////////////////////////////////////////////////
    ////                         private fields                    ////

    /** Variable pattern: variable name optionally followed by dimension(s). */
    private static final Pattern _VARIABLE_PATTERN =
        Pattern.compile("(\\w+)([\\d\\:\\[\\]]*).*");
        
    /** The name of the NetCDF file. */
    private String _filenameStr;
    
    /** The constraint expression. */
    private String _constraint;
    
    /** A map of variable name to dimension decimation. */
    private Map<String,Section> _constraintMap = new HashMap<String,Section>();
    
    /** NetCDF file object. */
    private NetcdfFile _ncFile;
    
    /** If true, workflow is executing. */
    private boolean _amFiring;
}
