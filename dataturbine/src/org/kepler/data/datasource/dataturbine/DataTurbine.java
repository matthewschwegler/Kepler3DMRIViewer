/*
 * Copyright (c) 1998-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2012-04-27 13:16:27 -0700 (Fri, 27 Apr 2012) $' 
 * '$Revision: 29789 $'
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ecoinformatics.seek.datasource.DataSourceIcon;

import ptolemy.actor.TypedIOPort;
import ptolemy.actor.lib.LimitedFiringSource;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleMatrixToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.FloatToken;
import ptolemy.data.IntMatrixToken;
import ptolemy.data.IntToken;
import ptolemy.data.LongToken;
import ptolemy.data.RecordToken;
import ptolemy.data.ShortToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.UnsignedByteToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.RecordType;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.util.MessageHandler;

import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Sink;

/**
 * The DataTurbine actor retrieves and outputs data from an RBNB DataTurbine server. 
 * Sink mode Request has been tested beneath SDF,
 * modes Monitor and Subscribe briefly in PN.
 * 
 * @author Derik Barseghian
 * @version $Id: DataTurbine.java 29789 2012-04-27 20:16:27Z barseghian $
 */

public class DataTurbine extends LimitedFiringSource {

	public static final String ZEROTIME = "0";
	private static Log log = LogFactory.getLog("org.kepler.data.datasource.datasource.dataturbine.DataTurbine");
	static {
	  System.setProperty("ZEROTIME", ZEROTIME);
	}
	private static final String ARRAY_OF_X_RECORDS = "Array of x records";
	private static final String RECORD_OF_2_ARRAYS = "Record of 2 arrays";
	public static final String SINKMODE_MONITOR = "Monitor";
	public static final String SINKMODE_REQUEST = "Request";
	public static final String SINKMODE_SUBSCRIBE = "Subscribe";

	/** The URL to the DataTurbine Server */
	public PortParameter dataTurbineAddressInputParam;

	/** Actor mode */
	public Parameter actorModeInputParam;
	
	/** The name of the channel to output through the specifiedChannel output port */
	public PortParameter outputChannelPortParam;
	
	/**
	 * The amount of time (ms) to wait for data to become available. Use 0 for
	 * no delay or any negative number for an infinite delay.
	 */
	public Parameter blockTimeoutParam;

	/**
	 * Sink mode.
	 * <ul>
	 * <li>Request: Initiates a request for a specific time slice of data.</li>
	 * <li>Subscribe: Starts a continuous feed of data on the specified channels
	 * to this sink, for retrieval. Each block retrieved will be duration time
	 * units in length.</li>
	 * <li>Monitor: Similar to Subscribe, but allows for continuous frames of
	 * data without gaps.</li>
	 * </ul>
	 */
	public Parameter sinkModeInputParam;

	/** Start time for Request or Subscribe modes. seconds or Date: yyyy-MM-dd HH:mm:ss */
	public PortParameter startTimePortParam;

	/** The duration of the request. Unit is seconds unless fetchByFrame is set. */
	public PortParameter durationPortParam;
	
	/** channelNames - This output port outputs all of the filtered (non-metric)
	 * channel names. 
	 */
	public static final String CHANNEL_NAMES_OUTPUT_PORT = "channelNames";

	/**
	 * For Subscribe mode: Any of "newest", "oldest", "absolute", "next", or
	 * "previous". <br />
	 * For Request mode:
	 * <ul>
	 * <li>"absolute" -- The start parameter is absolute time from midnight, Jan
	 * 1st, 1970 UTC.</li>
	 * <li>"newest" -- The start parameter is measured from the most recent data
	 * available in the server at the time this request is received. Note that
	 * for this case, the start parameter actually represents the end of the
	 * duration, and positive times proceed toward oldest data.</li>
	 * <li>"oldest" -- As "newest", but relative to the oldest data.</li>
	 * <li>"aligned" -- As "newest", but rather than per channel, this is
	 * relative to the newest for all of the channels.</li>
	 * <li>"after" -- A combination between "absolute" and "newest", this flag
	 * causes the server to return the newest data available after the specified
	 * start time. Unlike "newest", you do not have to request the data to find
	 * out that you already have it. Unlike "absolute", a gap may be inserted in
	 * the data to provide you with the freshest data.</li>
	 * <li>"modified" -- Similar to "after", but attempts to return a duration's
	 * worth of data in a contiguous block. If the data is not available after
	 * the start time, it will be taken from before the start time.</li>
	 * <li>"next" - gets the data that immediately follows the time range
	 * specified. This will skip over gaps.</li>
	 * <li>"previous" - get the data that immediately preceeds the time range
	 * specified. This will skip over gaps.</li>
	 * </ul>
	 * 
	 * */
	public Parameter referenceInputParam;

	/**
	 * Format of output datapoint and timestamp pairs: Record of 2 Arrays, or an
	 * Array of X Records.
	 */
	public Parameter outputRecordOfArrays;

	/**
	 * Will attempt to identify and pad gappy data with pairs of timestamps and
	 * nils. Need at least 2 samples to be able to pad.
	 */
	public Parameter tryToPad;

	private Sink _sink = null;
	private ChannelMap _map = null;
	private ChannelMap _registrationMap = null;

	
	private final static String DEFAULT_RBNB_CLIENT_NAME = "KeplerClient";
	private String _rbnbClientName = DEFAULT_RBNB_CLIENT_NAME;

	private String _url = "";
	private String _specifiedOutputChannel = "";
	/** specificChannel*/
	private final static String SPECIFIC_CHANNEL = "specificChannel";
	/** Subscribe, Monitor, or Request */
	private String _sinkMode = SINKMODE_REQUEST;
	/** for Request or Subscribe mode */
	private String _startTime = "0";
	/** for Request or Subscribe mode */
	private String _duration = "0";
	/** for Request mode: absolute,
	* newest, oldest, aligned, after,
	* modified, next, previous 
	*/
	private String _reference = "absolute";
	/** for Request mode */
	private String[] _connectedOutputPortNames = null;
	/** for Request mode - time (ms) to wait
	* for data to become available. 0 is no
	* delay. negative number for infinite
	* delay.
	*/
	private int _blockTimeout = 15000; 
	private double _startTimeDouble = 0.0;
	private double _durationDouble = 0.0;
	// TODO: eventually allow use of different date patterns:
	private String pattern = "yyyy-MM-dd HH:mm:ss";
	private SimpleDateFormat format = new SimpleDateFormat(pattern);
	private Date startDate = null;
	private boolean reload = false;

	private int _numChans = 0;
	private int _numChans2 = 0;
	private String[] _chanNames = null;
	private String[] _chanTypes = null;
	private double[] _chanDurations = null;
	private String[] _chanMimeTypes = null;
	private String[] _filteredChanNames = null;
	private String[] _filteredChanTypes = null;
	private double[] _filteredChanDurations = null;
	private String[] _filteredChanMimeTypes = null;
	private double[] _filteredChanStartTimes = null;
	private Map<String,String> _userInfoTypesMap = new HashMap<String,String>();
	
	public String outputRecOfArrays = RECORD_OF_2_ARRAYS;

	String[] labels = { "timestamps", "data" };
	Token[] values = new Token[labels.length];
	public boolean paddingOn = false;
	/** padding is not always possible. */
	public boolean willPad = paddingOn; 

	private DataSourceIcon _icon;

	/** for use with Monitor mode, presumably value
	* doesn't matter since gapControl logic is not
	* yet implemented in DT
	*/
	private int gapControl = 0;
	
    /** If true, director told us to stop firing. */
    private AtomicBoolean _stopRequested = new AtomicBoolean(false);

	/**
	 * Construct a DataTurbine source with the given container and name.
	 * 
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public DataTurbine(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		_icon = new DataSourceIcon(this);

		dataTurbineAddressInputParam = new PortParameter(this, "DataTurbine Address");
		dataTurbineAddressInputParam.setStringMode(true);
		dataTurbineAddressInputParam.setTypeEquals(BaseType.STRING);
		dataTurbineAddressInputParam.getPort().setTypeEquals(BaseType.STRING);
		
		outputChannelPortParam = new PortParameter(this, 
				"specificChannel Name");
		outputChannelPortParam.setStringMode(true);
		outputChannelPortParam.setTypeEquals(BaseType.STRING);
		outputChannelPortParam.getPort().setTypeEquals(BaseType.STRING);
		
		sinkModeInputParam = new Parameter(this, "Sink Mode");
		sinkModeInputParam.setStringMode(true);
		sinkModeInputParam.setTypeEquals(BaseType.STRING);
		sinkModeInputParam.addChoice(SINKMODE_REQUEST);
		sinkModeInputParam.addChoice(SINKMODE_MONITOR);
		sinkModeInputParam.addChoice(SINKMODE_SUBSCRIBE);
		sinkModeInputParam.setExpression(SINKMODE_REQUEST);
		
		startTimePortParam = new PortParameter(this,
				"Start Time (for Request or Subscribe modes)", 
				new StringToken(""));
		startTimePortParam.setStringMode(true);
		startTimePortParam.setTypeEquals(BaseType.STRING);
		startTimePortParam.getPort().setTypeEquals(BaseType.STRING);
		
		durationPortParam = new PortParameter(this,
				"Duration (for Request or Subscribe modes)",
				new StringToken(""));
		durationPortParam.setStringMode(true);
		durationPortParam.setTypeEquals(BaseType.STRING);
		durationPortParam.getPort().setTypeEquals(BaseType.STRING);
		
		referenceInputParam = new Parameter(this,
				"Reference (for Request or Subscribe modes)");
		referenceInputParam.setStringMode(true);
		referenceInputParam.setTypeEquals(BaseType.STRING);
		referenceInputParam.addChoice("absolute");
		referenceInputParam.addChoice("newest");
		referenceInputParam.addChoice("oldest");
		referenceInputParam.addChoice("aligned   (Request mode only)");
		referenceInputParam.addChoice("after   (Request mode only)");
		referenceInputParam.addChoice("modified   (Request mode only)");
		referenceInputParam.addChoice("next");
		referenceInputParam.addChoice("previous");
		referenceInputParam.setExpression("absolute");
		blockTimeoutParam = new Parameter(this,
				"Block Timeout (ms) (for Fetch)", new IntToken(15000));

		// note: keep this outputRecordOfArrays param initialization after
		// RBNBurlInputParam - this will avoid relation-breaking problem 
		// during reload events.
		// outputRecordOfArrays:
		//    RECORD_OF_2_ARRAYS - each output port will output a record of 
		// 						   2 arrays (data and timestamps)
		//    ARRAY_OF_X_RECORDS - each output port will output an array of
		//						   records (each record a data and timestamp)
		outputRecordOfArrays = new Parameter(this, "Output Data Type");
		outputRecordOfArrays.setStringMode(true);
		outputRecordOfArrays.setTypeEquals(BaseType.STRING);
		outputRecordOfArrays.addChoice(RECORD_OF_2_ARRAYS);
		outputRecordOfArrays.addChoice(ARRAY_OF_X_RECORDS);
		outputRecordOfArrays.setExpression(RECORD_OF_2_ARRAYS);

		tryToPad = new Parameter(this, "Pad data gaps with nils");
		tryToPad.setTypeEquals(BaseType.BOOLEAN);
		tryToPad.setExpression("false");

		_sink = new Sink();
		_map = new ChannelMap();
		_registrationMap = new ChannelMap();

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");
	}
	

	/**
	 * Send the token in the value parameter to the output.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		outputRecOfArrays = ((StringToken)outputRecordOfArrays.getToken()).stringValue();

		paddingOn = ((BooleanToken) tryToPad.getToken()).booleanValue();
		willPad = paddingOn;

		_url = ((StringToken)dataTurbineAddressInputParam.getToken()).stringValue();
		_url = _url.replaceAll("\"", "");

		_blockTimeout = Integer.parseInt(blockTimeoutParam.getToken()
				.toString());

		_sinkMode = ((StringToken)sinkModeInputParam.getToken()).stringValue();

		if (!(_sinkMode.equals(SINKMODE_MONITOR) || _sinkMode.equals(SINKMODE_REQUEST) || _sinkMode
				.equals(SINKMODE_SUBSCRIBE))) {
			throw new IllegalActionException(this,
					"Error. sinkMode must be Monitor, Request or Subscribe.");
		}

		if ((_sinkMode.equals(SINKMODE_SUBSCRIBE) || _sinkMode
				.equals(SINKMODE_REQUEST))) {
			startTimePortParam.update();
			_startTime = ((StringToken)startTimePortParam.getToken()).stringValue();
			_startTime = _startTime.replaceAll("\"", "");
			
			durationPortParam.update();
			_duration = ((StringToken)durationPortParam.getToken()).stringValue();
			_duration = _duration.replaceAll("\"", "");
			_reference = ((StringToken)referenceInputParam.getToken()).stringValue();
			_reference = _reference.replaceAll("\\s*\\(Request mode only\\)", "");
			
			if (_startTime == null || _startTime.equals("")) {
				throw new IllegalActionException(this,
						"DataTurbine actor must specify a Start Time for "+ _sinkMode + " Sink Mode.");
			}
			if (_duration == null || _duration.equals("")) {
				throw new IllegalActionException(this,
						"DataTurbine actor must specify a Duration for "+ _sinkMode + " Sink Mode.");
			}
			if (_reference == null || _reference.equals("")) {
				throw new IllegalActionException(this,
						"DataTurbine actor must specify a Reference for "+ _sinkMode + " Sink Mode.");
			}
			
			try {
				startDate = format.parse(_startTime);
				_startTimeDouble = startDate.getTime() / 1000;
			} catch (ParseException pe) {
				//throw new IllegalActionException(this, "ParseException " + pe);
				// allow users to also specify startTime as seconds, since this
				// is more natural when using "newest"
				try{
					_startTimeDouble = new Double(_startTime);
				}
				catch(NumberFormatException nfe){
					throw new IllegalActionException(this, "Start Time must be number (seconds) or Date formatted: "+pattern);
				}
			}
		}
		
		outputChannelPortParam.update();

		// count connected output ports
		int itr = 0;
		Iterator<?> q = this.outputPortList().iterator();
		while (q.hasNext()) {
			TypedIOPort port = (TypedIOPort) q.next();
			if (port.numberOfSinks() > 0) {
				itr++;
			}
		}
		_connectedOutputPortNames = new String[itr];

		// gather connected output port names
		itr = 0;
		q = this.outputPortList().iterator();
		while (q.hasNext()) {
			TypedIOPort port = (TypedIOPort) q.next();
			if (port.numberOfSinks() > 0) {
				_connectedOutputPortNames[itr] = port.getName();
				itr++;
			}
		}

		if (_sinkMode.equals(SINKMODE_REQUEST) || _sinkMode.equals(SINKMODE_SUBSCRIBE)) {
			_durationDouble = Double.parseDouble(_duration);
		}

		_icon.setBusy();

		// connect to dataturbine.
		try {
			openDataTurbine();
		} catch (Exception e) {
			throw new IllegalActionException(this,
					"ERROR opening DataTurbine connection from fire()");
		}

		// get dataturbine metadata.
		getDataTurbineInfo();

		try {
			
			// output the channel names
			outputChannelNames();
			
			// add requested channels to userMap.
			ChannelMap userChanMap = getUserChannelMap(_connectedOutputPortNames);
			if (userChanMap.NumberOfChannels() > 0) {
				if (_sinkMode.equals(SINKMODE_REQUEST)) {
					// request data in userMap.
					// System.out.println("----- about to sink.Request(userChanMap, "
					// + _startTimeDouble + ", " + _durationDouble + ", "
					// + _reference + ") -----");
					_sink.Request(userChanMap, _startTimeDouble,
							_durationDouble, _reference);

					// fetch data in userMap.
					// System.out.println("----- fetching data, using timeout:"
					// + _blockTimeout + " -----");
					_sink.Fetch(_blockTimeout, userChanMap);

					// send data out output ports.
					// System.out.println("----- about to call outputData -----");
					outputData(userChanMap);
				} else if (_sinkMode.equals(SINKMODE_MONITOR)) {
					// setup Monitor
					// System.out.println("----- about to sink.Monitor(userChanMap, "
					// + gapControl + ") -----");
					_sink.Monitor(userChanMap, gapControl);
					do {
						// fetch data in userMap.
						// System.out.println("----- fetching data, using timeout:"
						// + _blockTimeout + " -----");
						_sink.Fetch(_blockTimeout, userChanMap);

						// send data out output ports.
						// System.out.println("----- about to call outputData -----");
						outputData(userChanMap);
					} while (!_stopRequested.get());
				} else if (_sinkMode.equals(SINKMODE_SUBSCRIBE)) {
					// setup Subscribe
					// System.out
					// .println("----- about to sink.Subscribe(userChanMap, "
					// + _startTimeDouble + ", " + _durationDouble
					// + ", " + _reference + ") -----");
					_sink.Subscribe(userChanMap, _startTimeDouble,
							_durationDouble, _reference);
					do {
						// fetch data in userMap.
						// System.out.println("----- fetching data, using timeout:"
						// + _blockTimeout + " -----");
						_sink.Fetch(_blockTimeout, userChanMap);

						// send data out output ports.
						// System.out.println("----- about to call outputData -----");
						outputData(userChanMap);
					} while (!_stopRequested.get());
				}
			}

			_icon.setReady();
		} catch (SAPIException sapie) {
		    // ignore errors if we've stopped.
		    if(! _stopRequested.get())
  		    {
    			log.error("DataTurbine actor Error: during fire:" + sapie);
    			_icon.setReady();
    			log.debug("disconnect from DataTurbine");
    			_sink.CloseRBNBConnection();
    			throw new IllegalActionException(this, sapie, "Error during fire()");
  		    }
		} catch (Exception e){
			log.error("DataTurbine actor Error: during fire:" + e);
			_icon.setReady();
			log.debug("disconnect from DataTurbine");
			_sink.CloseRBNBConnection();
			throw new IllegalActionException(this, e, "Error during fire()");
		} 

		// disconnect from DataTurbine in wrapup() so same connection can be shared
		// across a workflow execution.
	}// fire
		
	/** Reset the stop requested boolean. */
	public void preinitialize() throws IllegalActionException {
	    super.preinitialize();
	    _stopRequested.set(false);
	}
	
	/** The director told us to stop firing immediately. */
	public void stop() {
		super.stop();
        _stopRequested.set(true);
		log.debug("disconnect from DataTurbine");
        _sink.CloseRBNBConnection();
	}
	
	/**
	 * Sets all the object variables by gathering info from the RBNB server.
	 * (fire() and attributeChanged() should probably be the only methods to
	 * call getDataTurbineInfo.)
	 * 
	 * @throws IllegalActionException
	 */
	private boolean getDataTurbineInfo() throws IllegalActionException {

		int chanType = 0;
		// filtered lists will exclude channels with duration 0
		// and those with names that begin with an underscore
		int filteredListLength = 0;

		try{
			openDataTurbine();

			_map.Clear();
			_registrationMap.Clear();

			_sink.RequestRegistration(_registrationMap);
			_sink.Fetch(_blockTimeout, _registrationMap);
			_numChans = _registrationMap.NumberOfChannels();
			_chanNames = new String[_numChans];
			_chanTypes = new String[_numChans];
			_chanDurations = new double[_numChans];
			_chanMimeTypes = new String[_numChans];
			
			_userInfoTypesMap.clear();
			
			// Determine length of the filtered list. Add all chans to _map. Set
			// _chanNames[]
			for (int i = 0; i < _numChans; i++) {
				_chanNames[i] = _registrationMap.GetName(i);
				_chanDurations[i] = _registrationMap.GetTimeDuration(i);
				chanType = _registrationMap.GetType(i);
				_chanTypes[i] = _registrationMap.TypeName(chanType);
				_chanMimeTypes[i] = _registrationMap.GetMime(i);
				
				String userInfo = _registrationMap.GetUserInfo(i);
				if(! userInfo.isEmpty()) {
	                Matcher matcher = DataTurbineWriter.PTOLEMY_TYPE_PATTERN.matcher(userInfo);
	                if(matcher.matches()) {
	                    _userInfoTypesMap.put(_chanNames[i], matcher.group(1));
	                }
				}
				    
				if (_chanDurations[i] < 0 || _chanNames[i].matches("_.*")) {
					// System.out.println("WARNING: chan " + i + " named: " +
					// _chanNames[i] + " duration: " + _chanDurations[i] +
					// " will not be added");
				} else {
					_map.Add(_chanNames[i]);
					// System.out.println("added chan " + i + ": " +
					// _chanNames[i] + " _chanType: " + _chanTypes[i] +
					// "_chanDuration: " + _chanDurations[i]);
					filteredListLength++;
				}
			}

			_filteredChanNames = new String[filteredListLength];
			_filteredChanTypes = new String[filteredListLength];
			_filteredChanDurations = new double[filteredListLength];
			_filteredChanMimeTypes = new String[filteredListLength];
			_filteredChanStartTimes = new double[filteredListLength];

			// Just get one small chunk of data so we are able to check the data
			// types. If server contains no non-metrics channels, do not make
			// (illegal) empty request.
			if (filteredListLength > 0) {
				_sink.Request(_map, 0, 1, "oldest");

				// Fetch data. Channels with duration 0 will not be returned.
				_sink.Fetch(_blockTimeout, _map);
				_numChans2 = _map.NumberOfChannels();

				boolean didfetchtimeout = _map.GetIfFetchTimedOut();
				if (didfetchtimeout) {
					log
							.warn("WARNING: fetch timed out. Try increasing the blockTimeOut value");
					if (filteredListLength != _numChans2) {
						log.error("DataTurbine actor Error: only " + _numChans2
								+ " channels were returned");
						// throw error
					}
				}

				Double smallestDuration = null;
				Double earliestStartTime = null;
				for (int i = 0; i < _numChans2; i++) {
					chanType = _map.GetType(i);
					_filteredChanTypes[i] = _map.TypeName(chanType);
					_filteredChanNames[i] = _map.GetName(i);
					_filteredChanDurations[i] = _map.GetTimeDuration(i);
					// TODO: durations apparently change after a fetch as well?
					// do they really change to sample duration?
					_filteredChanMimeTypes[i] = _map.GetMime(i);
					_filteredChanStartTimes[i] = _map.GetTimeStart(i);
					
					if (i == 0){
						smallestDuration = _filteredChanDurations[i];
						earliestStartTime = _filteredChanStartTimes[i];
					}
					else{
						if (_filteredChanDurations[i] < smallestDuration){
							smallestDuration = _filteredChanDurations[i];
						}
						if (_filteredChanStartTimes[i] < earliestStartTime){
							earliestStartTime = _filteredChanStartTimes[i];
						}
					}
					
					//System.out.println("_filteredChanNames: " +
					//_filteredChanNames[i] + " _filteredChanType: " +
					//_filteredChanTypes[i] + " _filteredChanDuration: " +
					//_filteredChanDurations[i] + " _filteredChanStartTime: " + 
					//_filteredChanStartTimes[i]);
				}
				
				// try to be helpful, if blank, set duration & startTime to smallest & earliest
				if (smallestDuration != null){
					if (((StringToken)durationPortParam.getToken()).stringValue().equals("")){
						durationPortParam.setToken(smallestDuration.toString());
						_duration = smallestDuration.toString();
					}
				}
				if (earliestStartTime != null){
					if (((StringToken)startTimePortParam.getToken()).stringValue().equals("")){
						long l = (new Double(earliestStartTime)).longValue();
						Date earliestDate = new Date(l*1000);
						String startDateString = format.format(earliestDate);
						startTimePortParam.setToken(startDateString);
						_startTime = earliestStartTime.toString();
					}
				}
						
			}

			return true;
		} catch (Exception e) {
			throw new IllegalActionException(this, e,
					"Problem opening DataTurbine connection in getDataTurbineInfo()");
		}

	}

	/**
	 * Return a channel map that contains only channels with duration >=0 and
	 * that were requested.
	 * 
	 * @return chanMap The ChannelMap.
	 */
	public ChannelMap getUserChannelMap(String[] requestedChannels)
			throws SAPIException {

		ChannelMap chanMap = new ChannelMap();

		Iterator<?> i = this.outputPortList().iterator();
		while (i.hasNext()) {
			TypedIOPort port = (TypedIOPort) i.next();
			String outputPortName = port.getName();
			for (int j = 0; j < requestedChannels.length; j++) {
				if (outputPortName.equals(requestedChannels[j])) {
					if (outputPortName.equals(SPECIFIC_CHANNEL)
							&& _specifiedOutputChannel != null) {
						chanMap.Add(_specifiedOutputChannel);
					} else {
						// if channel is already present, its current index is
						// returned, and no other action is taken.
						chanMap.Add(outputPortName);
					}
					break;
				}
			}
		}

		return chanMap;
	}

	/**
	 * Get the server name and port string.
	 * 
	 * @return server URL
	 */
	public String getServer() {
		return _url;
	}

	/**
	 * Get the name of this DataTurbine client.
	 * 
	 * @return client name
	 */
	public String getRBNBClientName() {
		return _rbnbClientName;
	}

	/**
	 * Output the channel names through the CHANNEL_NAMES_OUTPUT_PORT
	 * as an ArrayToken.
	 */
	public void outputChannelNames(){
		Iterator<?> j = this.outputPortList().iterator();
		while (j.hasNext()) {
			TypedIOPort port = (TypedIOPort) j.next();
			String currPortName = port.getName();
			if (currPortName.equals(CHANNEL_NAMES_OUTPUT_PORT)){
				for (int i=0; i< _connectedOutputPortNames.length; i++){
					if (_connectedOutputPortNames[i].equals(CHANNEL_NAMES_OUTPUT_PORT)){
						String chanNames = "{";
						for (int h=0; h < _filteredChanNames.length; h++){
							if (h != _filteredChanNames.length-1){
								chanNames = chanNames.concat("\""+ _filteredChanNames[h] + "\"" + ",");
							}
							else{
								chanNames = chanNames.concat("\""+ _filteredChanNames[h] + "\"");
							}
						}
						chanNames = chanNames.concat("}");
						ArrayToken at;
						try {
							at = new ArrayToken(chanNames);
							port.send(0, at);
						} catch (IllegalActionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}
		
	}
	
	/**
	 * Push the data out the output ports.
	 * 
	 * @param ChannelMap
	 */
	public void outputData(ChannelMap cmap) throws IllegalActionException {

		// TODO: check if we should be accessing the map in outputData, or
		// instead referencing eg _chanNames

		// The RBNB data types:
		// UNKNOWN = 0.
		// TYPE_FROM_INPUT = 1. Take the data type from the input data payload.
		// The resulting data type value will be one of the other types.
		// TYPE_BOOLEAN = 2. Boolean (8-bit, one byte)
		// TYPE_INT8 = 3. 8-bit (one byte) signed integer data type
		// TYPE_INT16 = 4. 16-bit (two byte) signed integer data type.
		// TYPE_INT32 = 5; 32-bit (four byte) signed integer data type.
		// TYPE_INT64 = 6. 64-bit (eight byte) signed integer data type
		// TYPE_FLOAT32 = 7. 32-bit (four byte) floating point data type
		// TYPE_FLOAT64 = 8. 64-bit (eight byte) floating point data type.
		// TYPE_STRING = 9. Character string data type.
		// TYPE_BYTEARRAY = 10. Byte array data type.
		// TYPE_USER = 11. User registration type.

		int outputPortListSize = this.outputPortList().size();

		// When RBNB returns no channels, output NIL tokens out every port
		if (cmap.NumberOfChannels() == 0) {
			// System.out
			// .println("* outputting nils * - Your request returned no channels");
			Iterator<?> j = this.outputPortList().iterator();
			while (j.hasNext()) {
				TypedIOPort port = (TypedIOPort) j.next();
				//String currPortName = port.getName();
				// System.out.println("* outputting nils * - Your request returned no data for channel: "
				// + currPortName);
				outputNils(port);
			}
		} else {
			// When only some channels have data, output NILs on those
			// channels that have no data.
			boolean[] portHasData = new boolean[outputPortListSize];
			Iterator<?> z = this.outputPortList().iterator();
			int h = -1;
			while (z.hasNext()) {
				h++;
				TypedIOPort port = (TypedIOPort) z.next();
				String outputPortName = port.getName();
				portHasData[h] = false; // initialize false
				
				int index = -1;
				//for the specificChannel output port we use the index of the specified channel
				if (outputPortName.equals(SPECIFIC_CHANNEL) && _specifiedOutputChannel != null){
					index = cmap.GetIndex(_specifiedOutputChannel);
				}
				else{
					index = cmap.GetIndex(outputPortName);
				}
				if (index != -1){
					portHasData[h] = true;
					int chanType = cmap.GetType(index);
					String chanTypeName = cmap.TypeName(chanType);

					double[] someTimes = cmap.GetTimes(index);
					Token someTimeTokens[] = new Token[someTimes.length];

					for (int j = 0; j < someTimeTokens.length; j++) {
						someTimeTokens[j] = new DoubleToken(someTimes[j]);
					}

					Token someDataTokens[] = new Token[someTimes.length];
					Token paddedTimeTokens[] = null;
					Token paddedDataTokens[] = null;

					Vector<Object> timeAndDataVector = new Vector<Object>();
					Vector<Object> vectorOfArrays = new Vector<Object>();

					if (chanTypeName.equals("Float32")) {
						float[] someData = cmap.GetDataAsFloat32(index);
						checkLengths(someData.length, someTimes.length);

						if (paddingOn && someData.length == 1) {
							willPad = false;
							log
									.warn("DataTurbine actor - Warning: cannot pad. Need at least 2 samples, your request returned 1.");
						}

						if (willPad) {
							for (int j = 0; j < someData.length; j++) {
								timeAndDataVector.add(someTimes[j]);
								timeAndDataVector.add(someData[j]);
							}
							vectorOfArrays = doPadding(timeAndDataVector,
									chanTypeName);
							paddedTimeTokens = (Token[]) vectorOfArrays
									.get(0);
							paddedDataTokens = (Token[]) vectorOfArrays
									.get(1);
						} else {
							for (int j = 0; j < someData.length; j++) {
								someDataTokens[j] = new FloatToken(
										someData[j]);
							}
						}

					} else if (chanTypeName.equals("Float64")) {
						double[] someData = cmap.GetDataAsFloat64(index);
						checkLengths(someData.length, someTimes.length);
						if (paddingOn && someData.length == 1) {
							willPad = false;
							log
									.warn("DataTurbine actor - Warning: cannot pad. Need at least 2 samples, your request returned 1.");
						}

						if (willPad) {
							for (int j = 0; j < someData.length; j++) {
								timeAndDataVector.add(someTimes[j]);
								timeAndDataVector.add(someData[j]);
							}
							vectorOfArrays = doPadding(timeAndDataVector,
									chanTypeName);
							paddedTimeTokens = (Token[]) vectorOfArrays
									.get(0);
							paddedDataTokens = (Token[]) vectorOfArrays
									.get(1);
						} else {
							for (int j = 0; j < someData.length; j++) {
								someDataTokens[j] = new DoubleToken(
										someData[j]);
							}
						}
					} else if (chanTypeName.equals("String")) {
						String[] someData = cmap.GetDataAsString(index);
						checkLengths(someData.length, someTimes.length);
						if (paddingOn && someData.length == 1) {
							willPad = false;
							log
									.warn("DataTurbine actor - Warning: cannot pad. Need at least 2 samples, your request returned 1.");
						}

						if (willPad) {
							for (int j = 0; j < someData.length; j++) {
								timeAndDataVector.add(someTimes[j]);
								timeAndDataVector.add(someData[j]);
							}
							vectorOfArrays = doPadding(timeAndDataVector,
									chanTypeName);
							paddedTimeTokens = (Token[]) vectorOfArrays
									.get(0);
							paddedDataTokens = (Token[]) vectorOfArrays
									.get(1);
						} else {
						    String userInfoType = _userInfoTypesMap.get(outputPortName);
							for (int j = 0; j < someData.length; j++) {
							    // see if the user info field contained a ptolemy type
							    // if not, then put into string token
							    if(userInfoType == null) {
							        someDataTokens[j] = new StringToken(
							                someData[j]);
							    } else if(userInfoType.equals("[int]")) {
							        someDataTokens[j] = new IntMatrixToken(someData[j]);
                                } else if(userInfoType.equals("[double]")) {
                                    someDataTokens[j] = new DoubleMatrixToken(someData[j]);
							    } else {
							        MessageHandler.error("Channel " + outputPortName + 
							            " has unsupported ptolemy type: " + userInfoType);
							    }
							}
						}
					} else if (chanTypeName.equals("Int8")) {
						byte[] someData = cmap.GetDataAsInt8(index);
						checkLengths(someData.length, someTimes.length);
						if (paddingOn && someData.length == 1) {
							willPad = false;
							log
									.warn("DataTurbine actor - Warning: cannot pad. Need at least 2 samples, your request returned 1.");
						}

						if (willPad) {
							for (int j = 0; j < someData.length; j++) {
								timeAndDataVector.add(someTimes[j]);
								timeAndDataVector.add(someData[j]);
							}
							vectorOfArrays = doPadding(timeAndDataVector,
									chanTypeName);
							paddedTimeTokens = (Token[]) vectorOfArrays
									.get(0);
							paddedDataTokens = (Token[]) vectorOfArrays
									.get(1);
						} else {
							for (int j = 0; j < someData.length; j++) {
								someDataTokens[j] = new UnsignedByteToken(
										someData[j]);
							}
						}
					} else if (chanTypeName.equals("Int16")) {
						short[] someData = cmap.GetDataAsInt16(index);
						checkLengths(someData.length, someTimes.length);
						if (paddingOn && someData.length == 1) {
							willPad = false;
							log
									.warn("DataTurbine actor - Warning: cannot pad. Need at least 2 samples, your request returned 1.");
						}

						if (willPad) {
							for (int j = 0; j < someData.length; j++) {
								timeAndDataVector.add(someTimes[j]);
								timeAndDataVector.add(someData[j]);
							}
							vectorOfArrays = doPadding(timeAndDataVector,
									chanTypeName);
							paddedTimeTokens = (Token[]) vectorOfArrays
									.get(0);
							paddedDataTokens = (Token[]) vectorOfArrays
									.get(1);
						} else {
							for (int j = 0; j < someData.length; j++) {
								someDataTokens[j] = new ShortToken(
										someData[j]);
							}
						}
					} else if (chanTypeName.equals("Int32")) {
						int[] someData = cmap.GetDataAsInt32(index);
						checkLengths(someData.length, someTimes.length);
						if (paddingOn && someData.length == 1) {
							willPad = false;
							log
									.warn("DataTurbine actor - Warning: cannot pad. Need at least 2 samples, your request returned 1.");
						}

						if (willPad) {
							for (int j = 0; j < someData.length; j++) {
								timeAndDataVector.add(someTimes[j]);
								timeAndDataVector.add(someData[j]);
							}
							vectorOfArrays = doPadding(timeAndDataVector,
									chanTypeName);
							paddedTimeTokens = (Token[]) vectorOfArrays
									.get(0);
							paddedDataTokens = (Token[]) vectorOfArrays
									.get(1);
						} else {
							for (int j = 0; j < someData.length; j++) {
								someDataTokens[j] = new IntToken(
										someData[j]);
							}
						}
					} else if (chanTypeName.equals("Int64")) {
						long[] someData = cmap.GetDataAsInt64(index);
						checkLengths(someData.length, someTimes.length);
						if (paddingOn && someData.length == 1) {
							willPad = false;
							log
									.warn("DataTurbine actor - Warning: cannot pad. Need at least 2 samples, your request returned 1.");
						}

						if (willPad) {
							for (int j = 0; j < someData.length; j++) {
								timeAndDataVector.add(someTimes[j]);
								timeAndDataVector.add(someData[j]);
							}
							vectorOfArrays = doPadding(timeAndDataVector,
									chanTypeName);
							paddedTimeTokens = (Token[]) vectorOfArrays
									.get(0);
							paddedDataTokens = (Token[]) vectorOfArrays
									.get(1);
						} else {
							for (int j = 0; j < someData.length; j++) {
								someDataTokens[j] = new LongToken(
										someData[j]);
							}
						}
					}
					if (!chanTypeName.equals("ByteArray")) {
						// System.out
						// .println("about to send dataTokens out of port: "
						// + currPortName);
						try {
							if (outputRecOfArrays
									.equals(RECORD_OF_2_ARRAYS)) {

								if (willPad) {
									ArrayToken atSomeTimeTokens = new ArrayToken(
											paddedTimeTokens);
									ArrayToken atSomeDataTokens = new ArrayToken(
											paddedDataTokens);
									Token[] dataValues = {
											atSomeTimeTokens,
											atSomeDataTokens };
									RecordToken recToken = new RecordToken(
											labels, dataValues);
									port.send(0, recToken);
								} else {
									ArrayToken atSomeTimeTokens = new ArrayToken(
											someTimeTokens);
									ArrayToken atSomeDataTokens = new ArrayToken(
											someDataTokens);
									Token[] dataValues = {
											atSomeTimeTokens,
											atSomeDataTokens };
									RecordToken recToken = new RecordToken(
											labels, dataValues);
									port.send(0, recToken);
								}
							} else {

								if (willPad) {
									RecordToken[] recTokens = new RecordToken[paddedTimeTokens.length];
									for (int k = 0; k < paddedTimeTokens.length; k++) {
										values[0] = paddedTimeTokens[k];
										values[1] = paddedDataTokens[k];
										recTokens[k] = new RecordToken(
												labels, values);
									}
									ArrayToken at = new ArrayToken(
											recTokens);
									port.send(0, at);
								} else {
									RecordToken[] recTokens = new RecordToken[someTimeTokens.length];
									for (int k = 0; k < someTimeTokens.length; k++) {
										values[0] = someTimeTokens[k];
										values[1] = someDataTokens[k];
										recTokens[k] = new RecordToken(
												labels, values);
									}
									ArrayToken at = new ArrayToken(
											recTokens);
									port.send(0, at);
								}
							}
						} catch (Exception e) {
							throw new IllegalActionException(this, e,
									"Exception trying to send dataTokens"
											+ " out of port: "
											+ outputPortName);

						}
					} else if (chanTypeName.equals("ByteArray")) { 
						// TODO: padding for byte channel
						byte[][] someData = cmap.GetDataAsByteArray(index);
						Token[][] someByteDataTokens = new Token[someData.length][];
						checkLengths(someData.length, someTimes.length);

						for (int j = 0; j < someData.length; j++) {
							someByteDataTokens[j] = new Token[someData[j].length];
							// System.out.println("debug Tokens: someData["
							// + j + "]: " + someData[j] + " someTimes[" + j
							// + "]: " +someTimes[j]);
							for (int k = 0; k < someData[j].length; k++) {
								try {
									someByteDataTokens[j][k] = new UnsignedByteToken(
											someData[j][k]);
								} catch (Exception e) {
									throw new IllegalActionException(this,
											e,
											"Exception trying to create and assign an UnsignedByteToken.");
								}
							}
						}
						try {
							if (outputRecOfArrays
									.equals(RECORD_OF_2_ARRAYS)) {
								ArrayToken[] atSomeDataTokens = new ArrayToken[someTimeTokens.length];
								for (int k = 0; k < someTimeTokens.length; k++) {
									atSomeDataTokens[k] = new ArrayToken(
											someByteDataTokens[k]);
								}
								ArrayToken atSomeTimeTokens = new ArrayToken(
										someTimeTokens);
								ArrayToken atatSomeDataTokens = new ArrayToken(
										atSomeDataTokens);
								Token[] dataValues = { atSomeTimeTokens,
										atatSomeDataTokens };
								RecordToken recToken = new RecordToken(
										labels, dataValues);
								port.send(0, recToken);
							} else {
								RecordToken[] recTokens = new RecordToken[someTimeTokens.length];
								for (int k = 0; k < someTimeTokens.length; k++) {
									values[0] = someTimeTokens[k];
									values[1] = new ArrayToken(
											someByteDataTokens[k]);
									recTokens[k] = new RecordToken(labels,
											values);
								}
								ArrayToken at = new ArrayToken(recTokens);
								port.send(0, at);
							}
						} catch (Exception e) {
							throw new IllegalActionException(this, e,
									"Exception trying to send out port 0.");
						}
					}
				}
				if (!portHasData[h]) {
					if (!outputPortName.equals(CHANNEL_NAMES_OUTPUT_PORT)){
							//System.out.println("* outputting nils * - Your request returned no data for channel: "
							//		+ currPortName);
							outputNils(port);
					}
				}
			} // end while z

		}

	} // outputData()

	/**
	 * 
	 * @param timeAndDataVector
	 * @param chanTypeName
	 * @return
	 */
	public Vector<Object> doPadding(Vector<Object> timeAndDataVector,
			String chanTypeName) {

		// System.out.println("Will try to pad any gaps...");

		Token paddedTimeTokens[] = null;
		Token paddedDataTokens[] = null;

		timeAndDataVector = fillGaps(timeAndDataVector);
		paddedTimeTokens = new Token[timeAndDataVector.size() / 2];
		paddedDataTokens = new Token[timeAndDataVector.size() / 2];

		int itr = -1;
		for (int j = 0; j < timeAndDataVector.size() - 1; j += 2) {
			itr++;
			paddedTimeTokens[itr] = new DoubleToken((Double) timeAndDataVector
					.get(j));

			// TODO: surely there's a better way of doing this:
			String t = timeAndDataVector.get(j + 1).toString();
			if (t.equals("nil")) {
				paddedDataTokens[itr] = Token.NIL;
			} else {
				if (chanTypeName.equals("Float32")) {
					paddedDataTokens[itr] = new FloatToken(
							(Float) timeAndDataVector.get(j + 1));
				} else if (chanTypeName.equals("Float64")) {
					paddedDataTokens[itr] = new DoubleToken(
							(Double) timeAndDataVector.get(j + 1));
				} else if (chanTypeName.equals("String")) {
					paddedDataTokens[itr] = new StringToken(
							(String) timeAndDataVector.get(j + 1));
				} else if (chanTypeName.equals("Int8")) {
					paddedDataTokens[itr] = new UnsignedByteToken(
							(Byte) timeAndDataVector.get(j + 1));
				} else if (chanTypeName.equals("Int16")) {
					paddedDataTokens[itr] = new ShortToken(
							(Short) timeAndDataVector.get(j + 1));
				} else if (chanTypeName.equals("Int32")) {
					paddedDataTokens[itr] = new IntToken(
							(Integer) timeAndDataVector.get(j + 1));
				} else if (chanTypeName.equals("Int64")) {
					paddedDataTokens[itr] = new LongToken(
							(Long) timeAndDataVector.get(j + 1));
				} else {
					log
							.error("DataTurbine Actor: ERROR. padding for this datatype not yet implemented!");
				}
			}
		}

		Vector<Object> vectorOfArrays = new Vector<Object>();
		vectorOfArrays.add(paddedTimeTokens);
		vectorOfArrays.add(paddedDataTokens);

		return vectorOfArrays;
	}

	public Vector<Object> fillGaps(Vector<Object> timeAndDataVector) {

		long sampleInterval = guessSamplingRate(timeAndDataVector);

		// TODO: track down adcp binary chan bug (unless fixed, will require an
		// if here on the +1)

		// note: duration is number of seconds that might contain data, not
		// number of data points.

		double durationDouble = (double) _durationDouble;
		double sampleIntervalDouble = (double) sampleInterval;
		// double firstTimeSample = (Double) timeAndDataVector.get(0);
		// double missingFront = firstTimeSample - _startTimeDouble;

		double dnumSamples = durationDouble / sampleIntervalDouble;
		// TODO: double check this round
		int numSamples = (int) Math.round(dnumSamples);
		if (sampleIntervalDouble > durationDouble) {
			log
					.error("DataTurbine Actor: ERROR: sampleIntervalDouble > durationDouble, something went wrong");
		}

		// System.out.println("durationDouble: " + durationDouble +
		// " sampleIntervalDouble: " + sampleIntervalDouble + " numSamples:"+
		// numSamples);

		Vector<Object> paddedTimeAndDataVector = null;

		if (numSamples != timeAndDataVector.size() / 2) {
			// System.out.println(numSamples + " != " +
			// timeAndDataVector.size()/2);
			int numMissing = numSamples - (timeAndDataVector.size() / 2);
			log
					.debug("DataTurbine Actor: Padding - requested timeslice missing "
							+ numMissing
							+ " samples, padding using sampleInterval of majority:"
							+ sampleInterval + "sec");

			// it will get larger
			paddedTimeAndDataVector = new Vector<Object>(numSamples * 2);
			paddedTimeAndDataVector = padGaps(sampleInterval, numMissing,
					timeAndDataVector);

			return paddedTimeAndDataVector;
		}

		log.debug("DataTurbine Actor: No need to pad");
		return timeAndDataVector;
	}

	// Guess Sampling Rate
	//
	// TODO: fix math - this will not work for faster than 1sps sampling!
	//
	public long guessSamplingRate(Vector<Object> timeAndDataVector) {

		Map hashmap = new HashMap(); // hash table
		double timesDifference;
		long roundedTimesDiff; // used for rounded value
		boolean keyExists;
		for (int j = 0; j < timeAndDataVector.size() - 2; j += 2) {
			timesDifference = (Double) timeAndDataVector.get(j + 2)
					- (Double) timeAndDataVector.get(j);
			roundedTimesDiff = Math.round(timesDifference);
			// System.out.println("timesDifference:" + timesDifference +
			// "roundedTimesDiff:" + roundedTimesDiff);
			keyExists = hashmap.containsKey(roundedTimesDiff);
			if (keyExists) {
				int hashValue = (Integer) hashmap.get(roundedTimesDiff);
				hashmap.put(Long.valueOf(roundedTimesDiff), Integer
						.valueOf(++hashValue));
				hashValue = (Integer) hashmap.get(roundedTimesDiff);
			} else {
				hashmap.put(Long.valueOf(roundedTimesDiff), Integer.valueOf(1));
				int hashValue = (Integer) hashmap.get(roundedTimesDiff);
			}
		}

		int maxHashValue = 0;
		long keyOfMax = 0l;
		Iterator hashItr = hashmap.keySet().iterator();
		while (hashItr.hasNext()) {
			long hashKey = (Long) hashItr.next();
			int hashValue = (Integer) hashmap.get(hashKey);
			if (hashValue > maxHashValue) {
				maxHashValue = hashValue;
				keyOfMax = hashKey;
			}
			// System.out.println(""+hashKey+ " => "+hashValue + "times");
		}
		long sampleInterval = keyOfMax;

		return sampleInterval;
	}

	public Vector<Object> padGaps(long sampleInterval, int numMissing,
			Vector<Object> timeAndDataVector) {
		// int newLength = timeAndDataVector.size() + numMissing * 2;

		double time, fakeTimeStamp;

		double firstTimeStamp = (Double) timeAndDataVector.get(0);
		double lastTimeStamp = (Double) timeAndDataVector.get(timeAndDataVector
				.size() - 2);
		// double firstPossibleTimeStamp = _startTimeDouble;
		double lastPossibleTimeStamp = _startTimeDouble + _durationDouble;

		// System.out.println("firstPossibleTimeStamp" + firstPossibleTimeStamp
		// + " firstTimeStamp " + firstTimeStamp +
		// " lastTimeStamp: " + lastTimeStamp + " lastPossibleTimeStamp: " +
		// lastPossibleTimeStamp);

		// insert - fill any gaps between first and last obtained samples
		int itr = -1;
		log.debug("padGaps numMissing:"+numMissing);
		for (int i = 0; i < timeAndDataVector.size(); i += 2) { // dynamic end
			// condition
			itr++;
			fakeTimeStamp = firstTimeStamp + sampleInterval * itr;
			time = (Double) timeAndDataVector.get(i);
			if (fakeTimeStamp != time) {
				// System.out.println("inserting fakeTimeStamp: " +
				// fakeTimeStamp);
				timeAndDataVector.add(i, Token.NIL);
				timeAndDataVector.add(i, fakeTimeStamp);
				numMissing--;
				if (numMissing < 0) {
					log
							.error("DataTurbine Actor Error: tried to insert too many samples");
				}
			}
		}

		// append - if needed, fill between last obtained sample and last
		// possible time
		if (numMissing != 0) {
			itr = 1;
			fakeTimeStamp = lastTimeStamp + sampleInterval * itr;
			while (fakeTimeStamp <= lastPossibleTimeStamp) {
				// System.out.println("appending fakeTimeStamp: " +
				// fakeTimeStamp);
				timeAndDataVector.add(fakeTimeStamp);
				timeAndDataVector.add(Token.NIL);
				numMissing--;

				itr++;
				fakeTimeStamp = lastTimeStamp + sampleInterval * itr;
			}
		}

		// prepend - if needed, fill between first possible time to first
		// obtained time
		itr = 0;
		while (numMissing > 0) {
			itr++;
			fakeTimeStamp = firstTimeStamp - sampleInterval * itr;
			if (fakeTimeStamp < _startTimeDouble) { // sanity-check
				log
						.error("ERROR! Tried to prepend before requested start time, something wrong!");
				// throw exception
				numMissing = 0;
			} else {
				// System.out.println("prepending fakeTimeStamp: " +
				// fakeTimeStamp);
				timeAndDataVector.add(0, Token.NIL);
				timeAndDataVector.add(0, fakeTimeStamp);
				numMissing--;
				// sanity check:
				if (numMissing == 0) {
					double tmp = fakeTimeStamp - sampleInterval;
					// System.out.println("fakeTimeStamp - sampleInterval " +
					// tmp + " _startTimeDouble " + _startTimeDouble);
				}
			}
		}

		return timeAndDataVector;
	}

	/**
	 * Output nils for a given port.
	 * 
	 * @param TypedIOPort
	 *            port
	 */
	public void outputNils(TypedIOPort port) {

		Type currPortType = port.getType();
		// String currPortName = port.getName();
		Token[] nilTokenArray = new Token[1];
		nilTokenArray[0] = Token.NIL;

		RecordToken[] recTokens = new RecordToken[1];

		try {

			ArrayToken atSomeTimeTokens = new ArrayToken(nilTokenArray);

			// hardcode:
			if ((currPortType.toString().contains(" = arrayType(unsignedByte)") && outputRecOfArrays
					.equals(ARRAY_OF_X_RECORDS))
					|| (currPortType.toString().contains(
							" = arrayType(arrayType(unsignedByte))") && outputRecOfArrays
							.equals(RECORD_OF_2_ARRAYS))) {

				// System.out.println("about to send * NIL dataTokens * out of BYTE port: "+currPortName);
				if (outputRecOfArrays.equals(RECORD_OF_2_ARRAYS)) {
					ArrayToken[] atSomeDataTokens = new ArrayToken[1];
					atSomeDataTokens[0] = new ArrayToken(nilTokenArray);
					ArrayToken atatSomeDataTokens = new ArrayToken(
							atSomeDataTokens);
					values[0] = atSomeTimeTokens;
					values[1] = atatSomeDataTokens;
					RecordToken recToken = new RecordToken(labels, values);
					port.send(0, recToken);
				} else {
					values[0] = Token.NIL;
					values[1] = new ArrayToken(nilTokenArray);
					recTokens[0] = new RecordToken(labels, values);
					ArrayToken at = new ArrayToken(recTokens);
					port.send(0, at);
				}
			} else {

				// System.out.println("about to send * NIL dataTokens * out of port: "+currPortName);
				if (outputRecOfArrays.equals(RECORD_OF_2_ARRAYS)) {
					ArrayToken atSomeDataTokens = new ArrayToken(nilTokenArray);
					values[0] = atSomeTimeTokens;
					values[1] = atSomeDataTokens;
					RecordToken recToken = new RecordToken(labels, values);
					port.send(0, recToken);
				} else {
					values[0] = Token.NIL;
					values[1] = Token.NIL;
					recTokens[0] = new RecordToken(labels, values);
					ArrayToken at = new ArrayToken(recTokens);
					port.send(0, at);
				}
			}
		} catch (Exception e) {
			log.error("DataTurbine actor: exception trying to sendout port 0"
					+ e);
		}
	}

	/**
	 * Compare two array lengths.
	 * 
	 * @param int arraylength1, int arraylength2
	 * @throws IllegalActionException
	 */
	void checkLengths(int dataLength, int timesLength)
			throws IllegalActionException {
		if (dataLength != timesLength) {
			throw new IllegalActionException(
					this,
					"Error. someData.length != someTimes.length, This shouldn't be. Check your DataTurbine server data");
		}
	}

	/**
	 * Post fire the actor. Return false to indicated that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 * public boolean postfire() throws IllegalActionException { if
	 * (_sinkMode.equals(SINKMODE_MONITOR) ){ return true; } else { return false; } }
	 */

	/**
	 * Open connection to DataTurbine server.
	 * 
	 * @throws SAPIException
	 * @throws IllegalActionException 
	 * 
	 */
	public boolean openDataTurbine() throws SAPIException, IllegalActionException {
		try {
			if (getServer() != null && !getServer().equals("") 
					&& getRBNBClientName() != null && !getRBNBClientName().equals("")){
				if (_sink.VerifyConnection()) {
					return true;
				}
				log.debug("open DataTurbine connection. OpenRBNBConnection(" + 
						getServer() + "," + getRBNBClientName() + ")");
				_sink.OpenRBNBConnection(getServer(), getRBNBClientName());
				if (!_sink.VerifyConnection()) {
					log.error("Error trying to open DataTurbine connection");
					return false;
				}
				return true;
			}
		} catch (SAPIException sap) {
			log.warn("\nFailed to connect to "+getServer()+"\n"+
				"Verify server URL. Enter empty string for none.\n");
			throw new SAPIException("\nFailed to connect to "+getServer()+"\n"+
				"Verify server URL. Enter empty string for none.\n");
		}
		return false;
	}

	/**
	 * Reconfigure actor when certain attributes change.
	 * 
	 * @param attribute
	 *            The changed Attribute.
	 * @throws ptolemy.kernel.util.IllegalActionException
	 * 
	 */
	public void attributeChanged(ptolemy.kernel.util.Attribute attribute)
			throws ptolemy.kernel.util.IllegalActionException {
		
	    boolean outputChannelChanged = false;
	    
		if (attribute == dataTurbineAddressInputParam) {
			if (dataTurbineAddressInputParam != null && dataTurbineAddressInputParam.getToken() != null){
				String tmpUrl = ((StringToken)dataTurbineAddressInputParam.getToken()).stringValue();
				tmpUrl = tmpUrl.replaceAll("\"", "");
			
				if (!_url.equals(tmpUrl)) {
					_url = tmpUrl;
					reload = true;
				}
			}
		}
		else if (attribute == outputChannelPortParam){
			if (outputChannelPortParam != null && outputChannelPortParam.getToken() != null) {

				String tmpSpecifiedOutputChannel = ((StringToken) outputChannelPortParam
						.getToken()).stringValue();
				if ((tmpSpecifiedOutputChannel != null && 
						!_specifiedOutputChannel.equals(tmpSpecifiedOutputChannel))) {
					_specifiedOutputChannel = tmpSpecifiedOutputChannel;
					outputChannelChanged = true;

				}
			}
		} else if (attribute == outputRecordOfArrays) {
			String tmpoutputRecOfArrays = ((StringToken)outputRecordOfArrays.getToken()).stringValue();
			if (tmpoutputRecOfArrays != null && !outputRecOfArrays.equals(tmpoutputRecOfArrays)) {
				reload = true;
				outputRecOfArrays = tmpoutputRecOfArrays;

			}
		} else if (attribute == tryToPad) {
			boolean tmpPaddingOn = ((BooleanToken) tryToPad.getToken())
					.booleanValue();

			if (paddingOn != tmpPaddingOn) {
				paddingOn = tmpPaddingOn;
				willPad = paddingOn;
			}
		} else{
			super.attributeChanged(attribute);
		}

		if(outputChannelChanged) {
		    getDataTurbineInfo();
	    }
		else if (reload) {
			try {
				reload = false;
				log.debug("Fetching info from DataTurbine: " + _url);
				// first make sure we can connect
				// if already connected to a server, disconnect first
    			log.debug("disconnect from DataTurbine");
				_sink.CloseRBNBConnection();
				boolean opened = openDataTurbine();
				if (opened){
					boolean gotInfo = getDataTurbineInfo();
					if (gotInfo){
						configureOutputPorts(null);
					}
				}
				else{
					// should only occur when the user manually changes to blank
					// (and not also during instantiation).
					if (attribute == dataTurbineAddressInputParam){
						removeAllOutputPorts();
					}
				}
			}
			catch (SAPIException e) {
				e.printStackTrace();
				throw new IllegalActionException("\nFailed to connect to "+getServer()+"\n"+
						"Verify server URL. Enter empty string for none.\n");
			}
			catch (Exception e) {
				e.printStackTrace();
				throw new IllegalActionException(this, e,
						"Error opening DataTurbine connection.");
			}
		}
	}

	/**
	 * Remove all existing output ports.
	 * 
	 * @throws ptolemy.kernel.util.IllegalActionException
	 */
	private void removeAllOutputPorts() throws IllegalActionException {
		Iterator<?> i = this.outputPortList().iterator();
		while (i.hasNext()) {
			TypedIOPort port = (TypedIOPort) i.next();
			String currPortName = port.getName();
			try {
				port.setContainer(null);
			} catch (Exception ex) {
				throw new IllegalActionException(this, ex,
						"Error removing port: " + currPortName);
			}
		}
	}

	/**
	 * Remove all ports with names not in the selected vector.
	 * @param nonRemovePortName
	 * @throws IllegalActionException
	 */
	void removeOtherOutputPorts(Collection<?> nonRemovePortName)
			throws IllegalActionException {
		// Use toArray() to make a deep copy of this.portList().
		// Do this to prevent ConcurrentModificationExceptions.

		TypedIOPort[] l = new TypedIOPort[0];
		l = (TypedIOPort[]) this.portList().toArray(l);

		for (int i = 0; i < l.length; i++) {
			TypedIOPort port = l[i];
			if (port == null || port.isInput()) {
				continue;
			}
			String currPortName = port.getName();
			if (!nonRemovePortName.contains(currPortName)) {
				try {
					port.setContainer(null);
				} catch (Exception ex) {
					throw new IllegalActionException(this, ex,
							"Error removing port: " + currPortName);
				}
			}
		}
	}

	/**
	 * Reconfigure output ports.
	 * 
	 * Some channels may have a period in their name. Ptolemy does not allow
	 * ports to have periods in their name, so replace . with PERIOD
	 * 
	 * @param requestedChanNames
	 *            A list of channels from which the actor will request data, and
	 *            will therefore need output ports for. If null, use all
	 *            filteredChanNames.
	 * @throws ptolemy.kernel.util.IllegalActionException
	 * 
	 */
	private void configureOutputPorts(String[] requestedChanNames)
			throws IllegalActionException {

		Vector<String> portsToKeep = new Vector<String>();
		
		// add the output port that simply outputs all filtered
		// channel names
		addPort(CHANNEL_NAMES_OUTPUT_PORT, "String");
		portsToKeep.add(CHANNEL_NAMES_OUTPUT_PORT);
		
		// add the specific channel output port
		addPort(SPECIFIC_CHANNEL,"String");
		portsToKeep.add(SPECIFIC_CHANNEL);

		if (_filteredChanNames.length != _filteredChanTypes.length) {
			throw new IllegalActionException(this,
					"ERROR filteredNames.length:" + _filteredChanNames.length
							+ " and filteredTypes.length:"
							+ _filteredChanTypes.length + " need to match!");
		}

		for (int z = 0; z < _filteredChanNames.length; z++) {
			if (_filteredChanNames[z] == null) {
				continue;
			}
			if (requestedChanNames == null) {
				addPort(_filteredChanNames[z].replaceAll("\\.", "PERIOD"),
						_filteredChanTypes[z]);
				portsToKeep.add(_filteredChanNames[z].replaceAll("\\.", "PERIOD"));
			} else {
				for (int i = 0; i < requestedChanNames.length; i++) {
					if (_filteredChanNames[z].equals(requestedChanNames[i])) {
						addPort(_filteredChanNames[z].replaceAll("\\.", "PERIOD"),
								_filteredChanTypes[z]);
						portsToKeep.add(_filteredChanNames[z].replaceAll("\\.",
								"PERIOD"));
						break;
					}
				}
			}
		}
		
		removeOtherOutputPorts(portsToKeep);
	}

	/**
	 * Add an output port to DataTurbine actor if it does not already exist and
	 * set the type appropriately.
	 * 
	 * @param aPortName
	 *            The name of the port.
	 * @param aPortType
	 *            The type of the port.
	 * 
	 */
	private void addPort(String aPortName, String rbnbType) {
		try {
			TypedIOPort port = (TypedIOPort) this.getPort(aPortName);
			boolean aIsNew = (port == null);
			if (aIsNew) {
				port = new TypedIOPort(this, aPortName, false, true);
			}
			setPortType(aPortName, rbnbType, port);
		} catch (Exception e) {
			log.error("DataTurbine Error. Trouble making port: " + aPortName
					+ " with type: " + rbnbType);
		}
	}

	/**
	 * Set the output port type based on outputRecOfArrays boolean
	 * 
	 * @param aPortName
	 *            The name of the port.
	 * @param aPortType
	 *            The type of the port.
	 * 
	 */
	private void setPortType(String aPortName, String rbnbType, TypedIOPort port) {

		Type[] types = new Type[labels.length];

		String rbnbUserInfoType = _userInfoTypesMap.get(aPortName);
		
		//special case for output port that simply outputs channel names
		if (aPortName.equals(CHANNEL_NAMES_OUTPUT_PORT)){
			ArrayType at = new ArrayType(BaseType.STRING);
			port.setTypeEquals(at);
		}
		else if (outputRecOfArrays.equals(RECORD_OF_2_ARRAYS)) {
			// System.out.println("Setting port " + aPortName +
			// " to send out a Record of arrays");
		    
            types[0] = new ArrayType(BaseType.DOUBLE);

		    if(rbnbUserInfoType != null) {
		        types[1] = new ArrayType(BaseType.forName(rbnbUserInfoType)); 
		    } else if (rbnbType.equals("Float32")) {
				types[1] = new ArrayType(BaseType.FLOAT);
			} else if (rbnbType.equals("Float64")) {
				types[1] = new ArrayType(BaseType.DOUBLE);
			} else if (rbnbType.equals("String")) {
				types[1] = new ArrayType(BaseType.STRING);
			} else if (rbnbType.equals("Int8")) {
				types[1] = new ArrayType(BaseType.UNSIGNED_BYTE);
			} else if (rbnbType.equals("Int16")) {
				types[1] = new ArrayType(BaseType.SHORT);
			} else if (rbnbType.equals("Int32")) {
				types[1] = new ArrayType(BaseType.INT);
			} else if (rbnbType.equals("Int64")) {
				types[1] = new ArrayType(BaseType.LONG);
			} else if (rbnbType.equals("Unknown")) {
				types[1] = new ArrayType(BaseType.UNKNOWN);
			} else if (rbnbType.equals("ByteArray")) {
				types[1] = new ArrayType(new ArrayType(BaseType.UNSIGNED_BYTE));
			}
			else if (rbnbType.equals("User")) {
			    System.out.println("type is User for " + aPortName);
			} else {
				log.error("DataTurbine actor Error: trouble making port: "
						+ aPortName + " unhandled DataTurbine type");
			}
			RecordType rt = new RecordType(labels, types);
			port.setTypeEquals(rt);
		} else {
		    
            types[0] = BaseType.DOUBLE;

			// System.out.println("Setting " + aPortName +
			// " to send out an Array of records");
            if(rbnbUserInfoType != null) {
                types[1] = BaseType.forName(rbnbUserInfoType); 
            } else if (rbnbType.equals("Float32")) {
				types[1] = BaseType.FLOAT;
			} else if (rbnbType.equals("Float64")) {
				types[1] = BaseType.DOUBLE;
			} else if (rbnbType.equals("String")) {
				types[1] = BaseType.STRING;
			} else if (rbnbType.equals("Int8")) {
				types[1] = BaseType.UNSIGNED_BYTE;
			} else if (rbnbType.equals("Int16")) {
				types[1] = BaseType.SHORT;
			} else if (rbnbType.equals("Int32")) {
				types[1] = BaseType.INT;
			} else if (rbnbType.equals("Int64")) {
				types[1] = BaseType.LONG;
			} else if (rbnbType.equals("Unknown")) {
				types[1] = BaseType.UNKNOWN;
			} else if (rbnbType.equals("ByteArray")) {
				types[1] = new ArrayType(BaseType.UNSIGNED_BYTE);
			}
			// else if (rbnbType.equals("User")){
			// ??
			// }
			else {
				log.error("DataTurbine actor Error: trouble making port: "
						+ aPortName + " unhandled DataTurbine type");
			}

			RecordType rt = new RecordType(labels, types);
			ArrayType at = new ArrayType(rt);
			port.setTypeEquals(at);
		}

	}

	public void wrapup() throws IllegalActionException {
		super.wrapup();
		log.debug("disconnect from DataTurbine");
		_sink.CloseRBNBConnection();
	}


}