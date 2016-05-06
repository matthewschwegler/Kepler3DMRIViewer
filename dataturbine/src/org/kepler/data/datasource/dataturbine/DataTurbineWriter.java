/* An actor that writes data to a DataTurbine server.
 * 
 * Copyright (c) 2011 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2011-07-05 10:53:41 -0700 (Tue, 05 Jul 2011) $' 
 * '$Revision: 27792 $'
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
package org.kepler.data.datasource.dataturbine;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ecoinformatics.seek.datasource.DataSourceIcon;
import org.kepler.util.DotKeplerManager;

import ptolemy.actor.IOPort;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.Token;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.MatrixType;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.util.MessageHandler;

import com.rbnb.api.Server;
import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Sink;
import com.rbnb.sapi.Source;

/** An actor that writes data to a DataTurbine server.
 * 
 *  @author Daniel Crawl
 *  @version $Id: DataTurbineWriter.java 27792 2011-07-05 17:53:41Z crawl $
 * 
 * TODO
 * 
 * - add input port _timestamp, date token, use as timestamp for data
 * - add input ports _specificChannelName, _specificChannelValue for writing to
 *      a specific channel (analogous to dt reader actor)
 * - create kar
 * 
 * 
 */
public class DataTurbineWriter extends TypedAtomicActor
{
    /** Construct a new DataTurbineWriter with a container and name. */
    public DataTurbineWriter(CompositeEntity container, String name)
        throws IllegalActionException, NameDuplicationException
    {

        super(container, name);
        
        _icon = new DataSourceIcon(this);
        
        persistDataAfterWorkflowEnd = new Parameter(this, "persistDataAfterWorkflowEnd");
        persistDataAfterWorkflowEnd.setTypeEquals(BaseType.BOOLEAN);
        persistDataAfterWorkflowEnd.setToken(BooleanToken.TRUE);
        
        channelContainer = new StringParameter(this, "channelContainer");
        
        flushAfterSeconds = new Parameter(this, "flushAfterSeconds");
        flushAfterSeconds.setTypeEquals(BaseType.INT);
        flushAfterSeconds.setToken("60");
        
        flushAfterNumData = new Parameter(this, "flushAfterNumData");
        flushAfterNumData.setTypeEquals(BaseType.INT);
        flushAfterNumData.setToken("100");
        
        serverAddress = new StringParameter(this, "serverAddress");
        serverAddress.setToken("localhost:3333");
        
        showChannels = new Parameter(this, "showChannels");
        showChannels.setTypeEquals(BaseType.BOOLEAN);
        showChannels.setToken(BooleanToken.TRUE);
        
        startServer = new Parameter(this, "startServer");
        startServer.setTypeEquals(BaseType.BOOLEAN);
        startServer.setToken(BooleanToken.FALSE);
        
        archiveDirectory = new FileParameter(this, "archiveDirectory");
        String dir = DotKeplerManager.getInstance().getPersistentUserDataDirString() + "dataturbine";
        archiveDirectory.setToken(dir);

    }
    
    /** React to a change in an attribute. */
    public void attributeChanged(Attribute attribute) throws IllegalActionException
    {
        if(attribute == flushAfterNumData)
        {
            Token token = flushAfterNumData.getToken();
            if(token != null)
            {
                _flushAfterNumData = ((IntToken)token).intValue();
                if(_flushAfterNumData < 0)
                {
                    throw new IllegalActionException(this, "flushAfterNumData must be >= 0.");
                }
            }
        }
        else if(attribute == flushAfterSeconds)
        {
            Token token = flushAfterSeconds.getToken();
            if(token != null)
            {
                _flushAfterSeconds = ((IntToken)token).intValue();
                if(_flushAfterSeconds < 0)
                {
                    throw new IllegalActionException(this, "flushAfterSeconds must be >= 0.");
                }
            }
        }
        else if(attribute == serverAddress)
        {
            String addr = serverAddress.stringValue();
            if(addr.length() == 0)
            {
                throw new IllegalActionException(this, "Must provide serverAddress.");
            }
            
            if(_serverAddress == null || !_serverAddress.equals(addr))
            {
                _serverAddress = addr;
                if(_showChannels)
                {
                  _reconfigureInputPorts();  
                }
            }
        }
        else if(attribute == channelContainer)
        {
            String container = channelContainer.stringValue();
            
            if(container.isEmpty() || container.contains("/"))
            {
                throw new IllegalActionException(this, "channelContainer cannot be empty or contain slashes.");
            }

            if(_channelContainer == null || !_channelContainer.equals(container))
            {            
                _channelContainer = container;
                if(_showChannels)
                {
                    _reconfigureInputPorts();
                }
            }
        }
        else if(attribute == showChannels)
        {
            boolean val = ((BooleanToken)showChannels.getToken()).booleanValue();
            if(val != _showChannels)
            {
                _showChannels = val;
                
                if(_showChannels)
                {
                    _reconfigureInputPorts();
                }
                else
                {
                    // remove all unconnected ports
                    List<?> ports = inputPortList();
                    for(Object obj : ports)
                    {
                        IOPort port = (IOPort)obj;
                        if(port.numberOfSources() == 0)
                        {
                            try {
                                port.setContainer(null);
                            } catch (NameDuplicationException e) {
                                throw new IllegalActionException(this, e, "Unable to remove port.");
                            }
                        }
                    }
                }
            }
        }
        else
        {
            super.attributeChanged(attribute);
        }
    }
    
    /** Fire the actor. */
    public void fire() throws IllegalActionException
    {
        super.fire();
        
        boolean wroteData = false;
        
        List<?> inputList = inputPortList();
        for(Object obj : inputList)
        {
            IOPort port = (IOPort)obj;
            
            String portName = port.getName();
            
            if(portName.startsWith("_"))
            {
                continue;
            }
            
            // see if port is connected
            if(port.numberOfSources() > 0)
            {
                int index = _channelMap.GetIndex(portName);
                if(index == -1)
                {
                    throw new IllegalActionException(this, "Channel " + portName + " not found in channel map");
                }
                
                Token token = port.get(0);
                Type tokenType = token.getType();
                
                try
                {
                    if(tokenType == BaseType.DOUBLE)
                    {
                        _channelMap.PutDataAsFloat64(index, 
                                new double [] { ((DoubleToken)token).doubleValue() });
                    }
                    else if(tokenType == BaseType.INT)
                    {
                        _channelMap.PutDataAsInt32(index, 
                                new int [] { ((IntToken)token).intValue() });                        
                    }
                    else if(tokenType instanceof MatrixType)
                    {                        
                        String tokenStr = token.toString();
                        _channelMap.PutDataAsString(index, tokenStr);
                        //_channelMap.PutDataAsByteArray(index, tokenStr.getBytes());
                        //System.out.println(portName + " is a " + typeStr);
                    }
                    else
                    {
                        throw new IllegalActionException(this, "Unsupported data type: " + tokenType);
                    }
                    
                    wroteData = true;
                    
                }
                catch(SAPIException e)
                {
                    throw new IllegalActionException(this, e, "Error writing data.");
                }
            }
        }
        
        if(wroteData)
        {
            _numDataWriten++;
         
            if(_numDataWriten >= _flushAfterNumData)
            {
                _flush();
            }
        }
    }
    
    /** Initialize the actor. */
    public void initialize() throws IllegalActionException
    {   
        // for any port types that were unknown during preinitialize,
        // see if they were determined during type resolution and put the
        // (ptolemy) type string in the channel map user info field.
        //
        for(String name : _unknownChannelTypeSet)
        {
            TypedIOPort port = (TypedIOPort) getPort(name);
            if(port == null)
            {
                throw new IllegalActionException(this, "No port called " + name);
            }
     
            // see if the port type is supported by dataturbine
            Type portType = port.getType();
            
            if(portType == BaseType.DOUBLE ||
                portType == BaseType.INT)
            {
                // this type is supported
                continue;
            }
            
            int index = _channelMap.GetIndex(name);
            //XXX check for index = -1
            
            // set the ptolemy port type as user info
            try 
            {
                _channelMap.PutUserInfo(index, "ptolemyType=" + port.getType().toString());
                //System.out.println("set user info for " + name + " = " + port.getType());
            }
            catch (SAPIException e)
            {
                throw new IllegalActionException(this, e, "Error setting user info.");
            }
        }
        
        // register the channel map to save the user info fields on the server.
        try 
        {
            _source.Register(_channelMap);
        }
        catch (SAPIException e)
        {
            throw new IllegalActionException(this, e, "Error registering channel map.");
        }
    }
    
    /** Preinitialize the actor. */
    public void preinitialize() throws IllegalActionException
    { 
        super.preinitialize();
        
        _numDataWriten = 0;
        _lastFlushTime = System.currentTimeMillis() / 1000;
        
        // see if we should start a server
        if(((BooleanToken)startServer.getToken()).booleanValue())
        {
            _startServer(_serverAddress);
        }
        
        try
        {
            // the sink may be connected in start server
            if(_sink == null)
            {
                _sink = new Sink();
                try {
                    _sink.OpenRBNBConnection(_serverAddress, getName());
                } catch (SAPIException e) {
                    throw new IllegalActionException(this, e, "Error opening connection to server.");
                }
            }
            
            _source = new Source(100, "append", 10000);
            try
            {
                _source.OpenRBNBConnection(_serverAddress, _channelContainer);
            }
            catch (SAPIException e)
            {
                throw new IllegalActionException(this, e, "Error opening connection to server.");
            }
                   
            // initially add all input ports to unknown type set
            _unknownChannelTypeSet = new HashSet<String>();
            List<?> inputList = inputPortList();
            for(Object obj : inputList)
            {
                IOPort port = (IOPort)obj;
                _unknownChannelTypeSet.add(port.getName());
            }
       
            // get a channel map based on the names of input ports
            _channelMap = _getMapFromInputPorts();
            
            // get the user info fields
            ChannelMap serverChannelMap = _getMapAndUserInfo(_sink);
            
            // get the type fields by doing a fetch
            ChannelMap serverDataChannelMap = _getMapFromFetch(_sink, serverChannelMap.GetChannelList());

            // set input port types based on channel types        
            String[] channelList = serverChannelMap.GetChannelList();
            for(int i = 0; i < channelList.length; i++) 
            {
                String name = channelList[i].substring(channelList[i].indexOf("/") + 1);
                TypedIOPort port = (TypedIOPort) getPort(name);
    
                if(name.startsWith("_") || port == null || port.numberOfSources() == 0)
                {
                    continue;
                }
    
                int typeId = serverChannelMap.GetType(i);
                int serverDataChannelMapIndex = serverDataChannelMap.GetIndex(_channelContainer + "/" + name);
                if(serverDataChannelMapIndex != -1)
                {
                    typeId = serverDataChannelMap.GetType(serverDataChannelMapIndex);
                }
                
                String userInfo = serverChannelMap.GetUserInfo(i);
                
                // set the user info in the source channel map, otherwise
                // when Register() is called in initialize(), we'll delete
                // the user info.
                if(!userInfo.isEmpty())
                {
                    int sourceChannelMapIndex = _channelMap.GetIndex(name);
                    if(sourceChannelMapIndex != -1)
                    {
                        try
                        {
                            _channelMap.PutUserInfo(sourceChannelMapIndex, userInfo);
                        }
                        catch (SAPIException e)
                        {
                            throw new IllegalActionException(this, e, "Error setting user info.");
                        }
                    }
                }
                
                // get the ptolemy type either from type field in the channel
                // map or the user info field.
                Type ptolemyType = _getTypeFromChannelMap(_channelContainer + "/" + name, serverDataChannelMap);
                if(ptolemyType == null)
                {
                    ptolemyType = _getTypeFromUserInfo(_channelContainer + "/" + name, serverChannelMap);
                }

                // see if it's a valid ptolemy type
                if(ptolemyType != null)
                {
                    port.setTypeEquals(ptolemyType);
                    _unknownChannelTypeSet.remove(name);
                }
                else
                {
                    System.out.println("WARNING: Channel " + name + 
                        " has unsupported or unknown DataTurbine type: " +
                        serverChannelMap.TypeName(typeId));
                }
            }
            
            _quit.set(false);
            _flushThread = new PeriodicFlushThread();
            _flushThread.start();
        }
        catch(IllegalActionException e)
        {
            // stop the server ignoring any exceptions
            _stopServerIgnoreException();
            throw e;
        }
    }
        
    /** Cleanup after execution or error. */
    public void wrapup() throws IllegalActionException
    {    
        super.wrapup();
        
        // flush any buffered data
        _flush();
        
        // close connection to server
        if(((BooleanToken)persistDataAfterWorkflowEnd.getToken()).booleanValue())
        {
            _source.Detach();
        }
        else
        {
            _source.CloseRBNBConnection();
        }
        
        _source = null;

        _sink.CloseRBNBConnection();
        _sink = null;
        
        // stop server if running.
        if(_server != null)
        {
            _stopServer();
            System.out.println("in wrapup; stopped server.");
        }
        
        _channelMap = null;
        
        _quit.set(true);
        synchronized(_flushThread)
        {
            _flushThread.notify();
        }
        
        try
        {
            _flushThread.join();
        }
        catch (InterruptedException e)
        {
            throw new IllegalActionException(this, e, "Error waiting for flush thread to stop.");
        }
        _flushThread = null;
        
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public fields                     ////

    /** If true, data written to DataTurbine server will be accessible
     *  after workflow ends.
     */
    public Parameter persistDataAfterWorkflowEnd;
    
    /** The number of seconds between writing data. */
    public Parameter flushAfterSeconds;
    
    /** The number of data point between writing data. */
    public Parameter flushAfterNumData;
    
    /** The hostname and port of the server. */
    public StringParameter serverAddress;
    
    /** If true, show an input port for each channel on server. */
    public Parameter showChannels;
    
    /** If true, start server if not already running (must provide archiveDirectory). */
    public Parameter startServer;
    
    /** The directory containing the DataTurbine archive. This parameter is
     *  ignored if startServer is false.
     */
    public FileParameter archiveDirectory;
    
    /** Name of channel container. Cannot have slashes. */
    public StringParameter channelContainer;
    
    /** Regex pattern of ptolemy type in user info field. */
    public final static Pattern PTOLEMY_TYPE_PATTERN = Pattern.compile(".*ptolemyType=([^,]+).*");

    ///////////////////////////////////////////////////////////////////
    ////                         private methods                   ////

    /** Flush any pending data. */
    private synchronized void _flush() throws IllegalActionException
    {
        if(_debugging)
        {
            _debug(getFullName() + " going to flush");
        }
        
        _icon.setBusy();

        synchronized(_numDataWriten)
        {            
            if(_numDataWriten > 0)
            {
                synchronized(_channelMap)
                {
                    try
                    {
                        _source.Flush(_channelMap);
                    }
                    catch (SAPIException e)
                    {
                        throw new IllegalActionException(this, e, "Error flushing data.");
                    }
                    
                    _numDataWriten = 0;
                    _lastFlushTime = System.currentTimeMillis() / 1000;
                }
            }
        }
        
        _icon.setReady();
    }
    
    /** Stop the server. */
    private void _stopServer() throws IllegalActionException
    {
        if(_server != null)
        {
            try {
                _server.stop();
            } catch (Exception e) {
                throw new IllegalActionException(this, e, "Error stopping DataTurbineServer.");
            } finally {
                _server = null;
            }
        }
    }
    
    /** Stop the server ignoring any exceptions. */
    private void _stopServerIgnoreException()
    {
        try {
            _stopServer();
        } catch(IllegalActionException e) {
            System.out.println("Error stopping server: " + e.getMessage());
        }
        
    }
    
    /** Get the channel names and corresponding ptolemy types. */
    private Map<String,Type> _getChannelsNameAndType() throws IllegalActionException
    {
        Map<String,Type> retval = new HashMap<String,Type>();
        
        // see if server address and channel container has been set yet
        if(_serverAddress != null && _channelContainer != null)
        {
            Sink sink = new Sink();
            try
            {
                sink.OpenRBNBConnection(_serverAddress, getName());
            }
            catch (SAPIException e)
            {
                throw new IllegalActionException(this, e, "Error connecting to DataTurbine.");
            }
            
            ChannelMap channelMap = _getMapAndUserInfo(sink);
            String[] channels = channelMap.GetChannelList();
            ChannelMap serverDataChannelMap = _getMapFromFetch(sink, channels);
            for(String name : channels)
            {
                if(name.startsWith("_") || !name.startsWith(_channelContainer + "/"))
                {
                    continue;
                }
                
                Type type = _getTypeFromChannelMap(name, serverDataChannelMap);
                if(type == null)
                {
                    type = _getTypeFromUserInfo(name, channelMap);
                }
                
                if(type == null)
                {
                    System.out.println("Unable to find type for channel " + name);
                    type = BaseType.UNKNOWN;
                }
                
                retval.put(name, type);
            }
            
            
            sink.CloseRBNBConnection();
        }
        
        return retval;
    }
    
    /** Get a ptolemy type for a channel name based on the type returned from the channel map.
     *  NOTE: the channel map must be retrieved with a Sink.Fetch().
     */
    private Type _getTypeFromChannelMap(String name, ChannelMap map) throws IllegalActionException
    {
        int index = map.GetIndex(name);
        if(index == -1)
        {
            throw new IllegalActionException(this, name + " not found in channel map");
        }

    
        int typeId = map.GetType(index);
        switch(typeId)
        {
        case ChannelMap.TYPE_FLOAT64:
            return BaseType.DOUBLE;
        case ChannelMap.TYPE_INT32:
            return BaseType.INT;
        default:
            return null;
        }
    }
    
    /** Get a ptolemy type based on information in the user info metadata. */
    private Type _getTypeFromUserInfo(String name, ChannelMap map) throws IllegalActionException
    {
        int index = map.GetIndex(name);
        if(index == -1)
        {
            throw new IllegalActionException(this, name + " not found in channel map");
        }
        
        String userInfo = map.GetUserInfo(index);
        if(!userInfo.isEmpty())
        {
            // extract the ptolemyType from the user info
            Matcher matcher = PTOLEMY_TYPE_PATTERN.matcher(userInfo);
            if(matcher.matches())
            {
                String userInfoType = matcher.group(1);
                return BaseType.forName(userInfoType);
            }
        }
        return null;
    }
    
    /** Reconfigure input ports based on DataTurbine channels. */
    private void _reconfigureInputPorts() throws IllegalActionException
    {
        Map<String,Type> nameTypeMap = _getChannelsNameAndType();
        for(Map.Entry<String, Type> entry : nameTypeMap.entrySet())
        {
            String name = entry.getKey();
            // remove the container prefix from the name
            name = name.substring(_channelContainer.length() + 1);
            
            // see if port already exists
            TypedIOPort port = (TypedIOPort)getPort(name);
            if(port == null)
            {
                // create the port
                
                
                
                try {
                    port = new TypedIOPort(this, name, true, false);
                    new Attribute(port, "_showName");
                } catch (NameDuplicationException e) {
                    throw new IllegalActionException(this, e, "Error creating port " + name);
                }
            }
            
            // set type
            port.setTypeEquals(entry.getValue());
        }
    }
    
    /** Get a ChannelMap by calling Sink.Request and Fetch. */
    private ChannelMap _getMapFromFetch(Sink sink, String[] names) throws IllegalActionException
    {
        boolean foundChannel = false;
        
        ChannelMap map = new ChannelMap();
        for(String name : names)
        {
            if(name.startsWith("_"))
            {
                continue;
            }
            
            try 
            {
                map.Add(name);
                foundChannel = true;
            }
            catch (SAPIException e) 
            {
                throw new IllegalActionException(this, e, "Error adding to channel map.");
            }
        }
        
        if(foundChannel)
        {
            try
            {
                sink.Request(map, 0, 0, "oldest");
                sink.Fetch(-1, map);
            }
            catch(SAPIException e)
            {
                throw new IllegalActionException(this, e, "Error determining types of channels.");
            }
        }
        
        return map;
    }
    
    /** Get a ChannelMap and user info for existing channels on the server. */
    private ChannelMap _getMapAndUserInfo(Sink sink) throws IllegalActionException
    {
        ChannelMap map;
        try
        {
            sink.RequestRegistration();
            map = sink.Fetch(-1);
        }
        catch(SAPIException e)
        {
            throw new IllegalActionException(this, e, "Error retrieving channels from server.");
        }        
        
        return map;
    }
    
    /** Add the names of inputs ports to a ChannelMap. */
    private ChannelMap _getMapFromInputPorts() throws IllegalActionException
    {
        ChannelMap map = new ChannelMap();
        List<?> inputList = inputPortList();
        for(Object obj : inputList)
        {
            IOPort port = (IOPort)obj;
            
            String portName = port.getName();
            
            if(portName.startsWith("_") || port.numberOfSources() == 0)
            {
                continue;
            }
            
            try
            {
                map.Add(portName);
            }
            catch (SAPIException e)
            {
                throw new IllegalActionException(this, e, "Error updating channel map.");
            }
        }
        
        return map;
    }
    
    /** Start a DT server if not already running. */
    private void _startServer(String addr) throws IllegalActionException
    {
        
        // first see if we can connect to existing server
        boolean alreadyRunning = true;
        
        _sink = new Sink();
        try {
            _sink.OpenRBNBConnection(addr, getName());
        } catch (SAPIException e) {
            alreadyRunning = false;
            _sink = null;
        }

        if(!alreadyRunning)
        {
                
            String dir = archiveDirectory.stringValue();
            if(dir.length() == 0) {
                throw new IllegalActionException(this, "Must provide archiveDirectory to start server.");
            }
            
            // make sure directory exists
            File file = new File(dir);
            if(!file.exists())
            {
                if(!file.mkdir())
                {
                    throw new IllegalActionException(this, "Unable to create directory " + dir);
                }
            }
            
            String[] args = new String[] { 
                    "-a ", addr,
                    "-F ", // loads existing archives
                    "-H ", dir };
            
            try {
                _server = Server.launchNewServer(args);
            } catch (Exception e) {
                throw new IllegalActionException(this, e, "Error starting server.");
            }

            // XXX if we don't wait, sometimes we cannot connect to server
            // should find a better way to wait until server is running
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /** A thread that flushes data. */
    private class PeriodicFlushThread extends Thread
    {
        public void run()
        {
            while(!_quit.get())
            {
                synchronized(this)
                {
                    try
                    {
                        wait(_flushAfterSeconds * 1000);
                    }
                    catch (InterruptedException e)
                    {
                        MessageHandler.error("Error while waiting in flush thread.", e);
                        return;
                    }
                }
                
                long elapsed = (System.currentTimeMillis() / 1000) - _lastFlushTime;
                System.out.println("elapsed time is " + elapsed);
                
                if(elapsed > _flushAfterSeconds)
                {
                    try
                    {
                        _flush();
                    }
                    catch (IllegalActionException e)
                    {
                        MessageHandler.error("Error while flushing in flush thread.", e);
                        return;
                    }
                }
            }
        }
    }
    
    ///////////////////////////////////////////////////////////////////
    ////                         private fields                    ////

    /** A DataTurbine server object. */
    private Server _server;
    
    /** An object to write to DT. */
    private Source _source;
    
    /** An sink object for get type information.*/
    private Sink _sink;
    
    /** Channel map to write data. */
    private ChannelMap _channelMap;
    
    /** The number of data written since the last flush. */
    private Integer _numDataWriten;
    
    /** The number of data to write before flushing. */
    private int _flushAfterNumData;
    
    /** The amount of time to wait before flushing. */
    private int _flushAfterSeconds;
    
    /** The name of the channel container. */
    private String _channelContainer;
    
    /** A set of port names with unknown types. */
    private Set<String> _unknownChannelTypeSet;
    
    /** The actor icon. */
    private DataSourceIcon _icon;
    
    /** A thread to periodically flush data. */
    private Thread _flushThread;
    
    /** If true, cleanup threads. */
    private AtomicBoolean _quit = new AtomicBoolean(false);
    
    /** The last time of the flush. */
    private long _lastFlushTime;
    
    /** If true, create input ports for each channel on server (within the container). */
    private boolean _showChannels = false;
    
    /** The host name and port of the server. */
    private String _serverAddress;
    
}
