/* An actor that writes NetCDF files.
 * 
 * Copyright (c) 2011 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:19:36 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31113 $'
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ptolemy.actor.IOPort;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.FloatToken;
import ptolemy.data.IntToken;
import ptolemy.data.LongToken;
import ptolemy.data.ShortToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.Port;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;
import ucar.ma2.Array;
import ucar.ma2.ArrayBoolean;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayLong;
import ucar.ma2.ArrayShort;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

/** This actor writes a single variable in a new NetCDF file. There
 * are input ports for each dimension and the variable. For example,
 * if the variable is z[x,y], then there are input ports called x,
 * y, and z. The input ports are automatically created when the
 * <i>variable</i> and <i>dimensions</i> parameters are set.
 * <p>
 * The actor reads a token on each input port every time it executes.
 * A token read by a port for a dimension is used as the index, and
 * a token read by a port for the variable is used as the value. For
 * example, if the variable is z[x,y], and the values read by ports
 * x, y, and z, are 1, 2, and 10, respectively, then the value 
 * written to z[1,2] = 10. 
 * <p>
 * 
 * @author Daniel Crawl
 * @version $Id: NetCDFWriter.java 31113 2012-11-26 22:19:36Z crawl $
 * 
 * TODO
 * 
 *  deletes/recreates variable port on wf load
 *  add parameter to overwrite file
 *  write each element to disk instead of keeping in memory until wrapup
 */

public class NetCDFWriter extends TypedAtomicActor
{
    /** Construct a new NetCDFWriter in a container with a name. */
    public NetCDFWriter(CompositeEntity container, String name)
        throws IllegalActionException, NameDuplicationException
    {
        super(container, name);

        filename = new FileParameter(this, "filename");
        _filenameStr = "";
        
        variable = new StringParameter(this, "inputVariable");
        dimensions = new StringParameter(this, "dimensions");
        
        writeOnFinish = new Parameter(this, "writeOnFinish");
        writeOnFinish.setTypeEquals(BaseType.BOOLEAN);
        writeOnFinish.setToken(BooleanToken.FALSE);
    }

    /** React to a change in an attribute. */
    public void attributeChanged(Attribute attribute) throws IllegalActionException
    {
        if(attribute == filename)
        {
            Token token = filename.getToken();
            if(token != null)
            {
                _filenameStr = ((StringToken)token).stringValue();
            }
        }
        else if(attribute == variable)
        {
            String varStr = variable.stringValue();
            if(!varStr.isEmpty() && (_variableName == null || !_variableName.equals(varStr)))
            {                
                // add the port if not already there
                Port port = getPort(varStr);
                if(port == null)
                {
                    try
                    {
                        port = new TypedIOPort(this, varStr, true, false);
                        new Attribute(port, "_showName");
                    }
                    catch (NameDuplicationException e)
                    {
                        throw new IllegalActionException(this, e, "Error adding port " + varStr);
                    }
                }
                _variableName = varStr;
                _removeOldInputPorts();
            }
        }
        else if(attribute == dimensions)
        {
            String dimStr = dimensions.stringValue();
            if(!dimStr.isEmpty() && (_dimensions == null || !_dimensions.equals(dimStr)))
            {
                _dimensions = dimStr;                
                _parseDimensions();
                _removeOldInputPorts();
            }
        }
        else if(attribute == writeOnFinish)
        {
            boolean val = ((BooleanToken)writeOnFinish.getToken()).booleanValue();
            if(val != _writeOnFinish)
            {
                _writeOnFinish = val;
                // if turned off, reparse dimensions to make sure each dimension has a length. 
                if(!_writeOnFinish && _dimensions != null)
                {
                    _parseDimensions();
                }
            }
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
     *  @return A new NetCDFWriter.
     */
    public Object clone(Workspace workspace) throws CloneNotSupportedException
    {        
        NetCDFWriter newObject = (NetCDFWriter) super.clone(workspace);
        newObject._array = null;
        newObject._datatype = null;
        newObject._dimensionMap = new HashMap<String,Integer>();
        newObject._dimensions = null;
        newObject._filenameStr = null;
        newObject._maxValue = new HashMap<String,Integer>();
        newObject._ncFile = null;
        newObject._savedIndexes = new LinkedList<Map<String,Integer>>();
        newObject._savedValues = new LinkedList<Token>();
        newObject._variableName = null;
        newObject._writeOnFinish = false;
        return newObject;
    }

    /** Read the value and indexes, and update the array. */
    public void fire() throws IllegalActionException
    {      
        // read value and add to array
        final IOPort valuePort = (IOPort) getPort(_variableName);
        final Token valueToken = valuePort.get(0);

        if(_writeOnFinish)
        {
            final Map<String,Integer> map = new HashMap<String,Integer>();
            for(String dimName : _dimensionMap.keySet())
            {
                final IOPort port = (IOPort) getPort(dimName);
                final int val = ((IntToken)port.get(0)).intValue();
                map.put(dimName, val);
                
                // see if we need to update max values.
                final Integer max = _maxValue.get(dimName);
                if(max == null || max < val)
                {
                    _maxValue.put(dimName, val);
                }
            }
            
            _savedValues.add(valueToken);
            _savedIndexes.add(map);
            
        }
        else
        {
            
            // read dimension indexes
            final Index index = _array.getIndex();
            int i = 0;
            for(Map.Entry<String, Integer> entry : _dimensionMap.entrySet())
            {            
                final String dimName = entry.getKey();
                final int length = entry.getValue();
                
                final IOPort port = (IOPort) getPort(dimName);
                final int val = ((IntToken)port.get(0)).intValue();
                if(val >= length)
                {
                    throw new IllegalActionException(this, "Invalid value " +
                        val + " for dimension " + dimName +
                        " whose length is " + length);
                }
                
                index.setDim(i, val);
                i++;
            }
            
            _writeOneValue(valueToken, index);
        }
        
    }
         
    /** Create the NetCDF file, add the header information, and initialize the array. */
    public void initialize() throws IllegalActionException
    {
        super.initialize();
        
        _datatype = null;
        
        _maxValue.clear();
        _savedValues.clear();
        _savedIndexes.clear();

        // sanity checks

        if(_variableName.isEmpty())
        {
            throw new IllegalActionException(this, "No variable specified.");
        }

        if(_dimensionMap.isEmpty())
        {
            throw new IllegalActionException(this, "No dimensions specified.");
        }

        if(!_writeOnFinish)
        {
            _openFile();
        }
    }        

    /** Write the array to the file and close it. */
    public void wrapup() throws IllegalActionException
    {
        
        if(_writeOnFinish)
        {
            _openFileAndWriteToArray();
        }
        
        // write array to file.
        if(_ncFile != null && _array != null)
        {
            try
            {
                _ncFile.write(_variableName, _array);
            }
            catch (Exception e)
            {
                throw new IllegalActionException(this, e, "Error writing array data to file.");
            }
        }
        
        _closeFile();
        
        super.wrapup();
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public fields                     ////

    /** The name of the NetCDF file. */
    public FileParameter filename;
    
    /** The name of the variable to write. */
    public StringParameter variable;
    
    /** A space-separated list of dimensions and their length, e.g., x[10] y[4]. */
    public StringParameter dimensions;
    
    /** If true, wait until the workflow is finished before writing data to
     *  the NetCDF file. Set this to true if the length of the dimensions
     *  are not known before the workflow starts. (A length number is still
     *  required for each dimension in the dimensions parameter, but the
     *  value is ignored).
     */
    public Parameter writeOnFinish;
    
    ///////////////////////////////////////////////////////////////////
    ////                         private methods                   ////

    /** Close the NetCDF file. */
    private void _closeFile() throws IllegalActionException
    {
        _array = null;
        if(_ncFile != null) {
            try {
                _ncFile.close();
            } catch (IOException e) {
                throw new IllegalActionException(this, e, "Error closing file.");
            }
        }
    }

    /** Close the NetCDF file ignoring any exception thrown. */
    private void _closeFileIgnoreException()
    {
        try {
            _closeFile();
        } catch(Throwable t) {
            System.err.println("Error closing file: " + t.getMessage());
        }
    }
    
    /** Open the NetCDF file and initialize the data array. */
    private void _openFile() throws IllegalActionException
    {
        try
        {   
            _ncFile = NetcdfFileWriteable.createNew(_filenameStr);
            
            // add the dimensions
            
            List<Dimension> dimensions = new ArrayList<Dimension>();
            for(Map.Entry<String, Integer> entry : _dimensionMap.entrySet())
            {
                dimensions.add(_ncFile.addDimension(entry.getKey(), entry.getValue()));
            }
            
            // add the variable
            
            TypedIOPort port = (TypedIOPort) getPort(_variableName);
            _datatype = _getNetCDFType(port.getType());
            _ncFile.addVariable(_variableName, _datatype, dimensions);
            
            // create the file and leave define mode
            _ncFile.create();
            
            // create the array            
            int[] dim = new int[_dimensionMap.size()];
            int i = 0;
            for(Integer length : _dimensionMap.values())
            {
                dim[i] = length;
                i++;
            }

            if(_datatype == DataType.DOUBLE) {
                _array = new ArrayDouble(dim);
            } else if(_datatype == DataType.FLOAT) {
                _array = new ArrayFloat(dim);
            } else if(_datatype == DataType.SHORT) {
                _array = new ArrayShort(dim);
            } else if(_datatype == DataType.INT) {
                _array = new ArrayInt(dim);
            } else if(_datatype == DataType.LONG) {
                _array = new ArrayLong(dim);
            } else if(_datatype == DataType.BOOLEAN) {
                _array = new ArrayBoolean(dim);
            }
        }
        catch(IOException e)
        {
            _closeFileIgnoreException();
            throw new IllegalActionException(this, e, "Error creating netcdf file.");
        }
    }
    
    /** Parse the dimensions parameter and change input ports accordingly. */
    private void _parseDimensions() throws IllegalActionException
    {              
       // use LinkedHashMap for predictable iteration order
       _dimensionMap.clear();
       
       String[] dimArray = _dimensions.split("\\s+");
       for(String dimStr : dimArray)
       {
           Matcher matcher = DIMENSION_PATTERN.matcher(dimStr);
       
           if(!matcher.matches())
           {
               throw new IllegalActionException(this, "Dimension not formatted correctly: " + dimStr);
           }
           
           String name = matcher.group(1);
           int length = Integer.valueOf(matcher.group(2));
           
           // length must be >= 1 unless we write the data when finishing
           if(length < 1 && !_writeOnFinish)
           {
               throw new IllegalActionException(this, "Dimension " + name + " must have length >= 1.");
           }
           
           _dimensionMap.put(name, length);
           
           // add input port if not there
           TypedIOPort port = (TypedIOPort) getPort(name);
           if(port == null)
           {
               try {
                   port = new TypedIOPort(this, name, true, false);
                   new Attribute(port, "_showName");
               } catch(NameDuplicationException e) {
                   throw new IllegalActionException(this, e, "Error creating port for dimension " + name);
               }
           }
           port.setTypeEquals(BaseType.INT);
           
       }
    }
    
    /** Remove ports that are not named after the variable or one
     *  of the dimensions.
     */
    private void _removeOldInputPorts() throws IllegalActionException
    {
       // remove output ports whose names are not dimensions
       List<?> inputPorts = inputPortList();
       for(Object object : inputPorts)
       {
           TypedIOPort port = (TypedIOPort)object;
           String name = port.getName();
           // make sure both variable and dimensions have been set before removing
           if(_variableName != null && !name.equals(_variableName) &&
               !_dimensionMap.isEmpty() && !_dimensionMap.containsKey(name))
           {
               try
               {
                   port.setContainer(null);
               } 
               catch (NameDuplicationException e)
               {
                   throw new IllegalActionException(this, e, "Error removing " + name);
               }
           }
       }
    }
        
    /** Get the corresponding NetCDF type for the Ptolemy type. */
    private DataType _getNetCDFType(Type type) throws IllegalActionException
    {
        if(type == BaseType.DOUBLE) {
            return DataType.DOUBLE;
        } else if(type == BaseType.FLOAT) {
            return DataType.FLOAT;
        } else if(type == BaseType.SHORT) {
            return DataType.SHORT;
        } else if(type == BaseType.INT) {
            return DataType.INT;
        } else if(type == BaseType.LONG) {
            return DataType.LONG;
        } else if(type == BaseType.BOOLEAN) {
            return DataType.BOOLEAN;
        } else {
            throw new IllegalActionException(this, "Unsupported conversion to NetCDF type : " + type);
        }
    }

    /** Write data collected during fire() to the array. */
    private void _openFileAndWriteToArray() throws IllegalActionException
    {
        
        // add length for each dimension
        Set<String> dimensionNames = new HashSet<String>(_dimensionMap.keySet());
        for(String name : dimensionNames)
        {
            Integer max = _maxValue.get(name);
            
            if(max == null)
            {
                throw new IllegalActionException("Could not find values for dimension " + name);
            }
            
            // add one since dimensions start at 1, not 0
            _dimensionMap.put(name, max + 1);
        }
        
        // open the file for writing
        _openFile();
                
        // write each value
        while(!_savedValues.isEmpty())
        {
            Token valueToken = _savedValues.remove(0);
            Map<String,Integer> dimMap = _savedIndexes.remove(0);
            
            final Index index = _array.getIndex();
            int i = 0;

            for(String name : _dimensionMap.keySet())
            {
                final Integer dimVal = dimMap.get(name);
                
                if(dimVal == null)
                {
                    throw new IllegalActionException("No value for dimension " + name);
                }
                
                index.setDim(i, dimVal);
                i++;
            }        
            _writeOneValue(valueToken, index);
        }
    }
    
    /** Write a single value in a token to the array. */
    private void _writeOneValue(Token valueToken, Index index)
    {
        //System.out.println(getName() + " index " + index);
        
        if(_datatype == DataType.DOUBLE) {
            _array.setDouble(index, ((DoubleToken)valueToken).doubleValue());
        } else if(_datatype == DataType.FLOAT) {
            _array.setFloat(index, ((FloatToken)valueToken).floatValue());
        } else if(_datatype == DataType.SHORT) {
            _array.setShort(index, ((ShortToken)valueToken).shortValue());
        } else if(_datatype == DataType.INT) {
            _array.setInt(index, ((IntToken)valueToken).intValue());
        } else if(_datatype == DataType.LONG) {
            _array.setLong(index, ((LongToken)valueToken).longValue());
        } else if(_datatype == DataType.BOOLEAN) {
            _array.setBoolean(index, ((BooleanToken)valueToken).booleanValue());
        }   
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private fields                    ////

    /** The name of the NetCDF file. */
    private String _filenameStr;
    
    /** The name of the variable. */
    private String _variableName;
    
    /** A space-separate list of dimensions and their lengths. */
    private String _dimensions;
    
    /** A map of dimension name to its length. Use LinkedHashMap 
     *  for predictable iteration order.
     */
    private Map<String,Integer> _dimensionMap = new LinkedHashMap<String,Integer>();

    /** The NetCDF file object. */
    private NetcdfFileWriteable _ncFile;
    
    /** The NetCDF type of the variable. */
    private DataType _datatype;
    
    /** The array containing the values of the variable. */
    private Array _array;

    /** Regular expression of a dimension. */
    private final static Pattern DIMENSION_PATTERN = Pattern.compile("(\\w+)\\[(\\d+)\\]");

    /** If true, write data to the NetCDF file when the workflow is finished. */
    private boolean _writeOnFinish;
    
    /** The maximum value seen for each dimension. */
    private Map<String,Integer> _maxValue = new HashMap<String,Integer>();

    /** A list of values collected during fire(). */
    private List<Token> _savedValues = new LinkedList<Token>();
    
    /** A list of index names and values collected during fire(). */
    private List<Map<String,Integer>> _savedIndexes = new LinkedList<Map<String,Integer>>();
    
}
