/*
 * Copyright (c) 1998-2010 The Regents of the University of California.
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

package org.kepler.data.conversion.binary;

import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.TimeZone;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.FloatToken;
import ptolemy.data.IntToken;
import ptolemy.data.RecordToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.RecordType;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;
import edu.hawaii.soest.kilonalu.adcp.Ensemble;
import edu.hawaii.soest.kilonalu.adcp.EnsembleDataType;

/**
 * @author Derik Barseghian Takes in a byte array representing an ensemble,
 *         converts it to an ensemble object, prints info from all the get
 *         methods the ensemble class offers, and then outputs the decoded
 *         numeric data (currently hardcoded to temperature, salinity, and
 *         pressure). using Chris Jones Ensemble code.
 */

public class EnsembleActor extends TypedAtomicActor {

	/**
	 * Construct a RBNBToKepler2 source with the given container and name.
	 * 
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public EnsembleActor(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// Set parameters.
		numInputTokens = new Parameter(this, "numInputTokens");
		numInputTokens.setExpression("numStartTimes");

		arrayIO = new TypedIOPort(this, "data", true, false);
		arrayIO.setTypeEquals(new ArrayType(new ArrayType(
				BaseType.UNSIGNED_BYTE)));
		arrayIO_tokenConsumptionRate = new Parameter(arrayIO,
				"tokenConsumptionRate");
		arrayIO_tokenConsumptionRate.setExpression("numInputTokens");

		accept2dArrays = new Parameter(this, "accept2dArrays");
		accept2dArrays.setTypeEquals(BaseType.BOOLEAN);
		accept2dArrays.setExpression("true");

		// hardcoded - not sure having an ensemble always means having temp,
		// pressure and salinity:
		tempIO = new TypedIOPort(this, "temperature", false, true);
		pressureIO = new TypedIOPort(this, "pressure", false, true);
		salinityIO = new TypedIOPort(this, "salinity", false, true);
		systemPowerIO = new TypedIOPort(this, "systemPower", false, true);

		outputArrayLength = new Parameter(this, "outputArrayLength");
		outputArrayLength.setExpression("(requestDuration+1)*numStartTimes"); // todo:
																				// this
																				// default
																				// is
																				// not
																				// generic
																				// obviously
		outputArrayLength.setVisibility(Settable.FULL);

		try {
			Type[] types = new Type[labels.length];

			types[0] = new ArrayType(BaseType.DOUBLE);
			types[1] = new ArrayType(BaseType.FLOAT);
			RecordType rt = new RecordType(labels, types);

			tempIO.setTypeEquals(rt);
			pressureIO.setTypeEquals(rt);

			types[1] = new ArrayType(BaseType.INT);
			rt = new RecordType(labels, types);
			salinityIO.setTypeEquals(rt);
			rt = new RecordType(labels, types);
			systemPowerIO.setTypeEquals(rt);
		} catch (Exception e) {
			System.out.println("" + e);
		}

		// RBNBurlInputParam = new Parameter(this, "RBNBurl", new
		// StringToken(_server));

		// _sink = new Sink();

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	public TypedIOPort arrayIO = null;
	public TypedIOPort tempIO = null;
	public TypedIOPort pressureIO = null;
	public TypedIOPort salinityIO = null;
	public TypedIOPort systemPowerIO = null;

	// the (max) size of output arrays (some datasource will contain gaps):
	public Parameter outputArrayLength;
	public Parameter numInputTokens;
	public Parameter arrayIO_tokenConsumptionRate;

	public Parameter accept2dArrays;

	private byte[] byteArray = null;
	private ByteBuffer ensembleBuffer = null;
	public int itr = 0;
	String[] labels = { "timestamps", "data" };
	public int maxArrayLength = 0;
	private boolean take2dArrays = true;

	// **************begin ensemble vars*****************
	int ADCChannelFive;
	int ADCChannelFour;
	int ADCChannelOne;
	int ADCChannelSeven;
	int ADCChannelSix;
	int ADCChannelThree;
	int ADCChannelTwo;
	int ADCChannelZero;
	int baseFrequencyIndex;
	int beamAngle;
	int beamDirection;
	int beamPattern;
	int binOneDistance;
	int blankAfterTransmit;
	int builtInTestResult;
	//
	int checksum;
	int coordinateTransformParams;
	int cpuFirmwareRevision;
	int cpuFirmwareVersion;
	int dataTypeID = 0x7f7f; // EnsembleDataType.HEADER
	EnsembleDataType ensembleDataType;
	// prob:
	int dataTypeNumber;
	float dataTypeOffset;
	int depthCellLength;
	float depthOfTransducer;
	int ensembleNumber;
	int ensembleNumberIncrement;
	int errorStatusWord;
	int errorVelocityThreshold;
	int falseTargetThreshold;
	int fixedLeaderID;
	int fixedLeaderSpare;

	// prob:
	// int headerID;

	int headerSpare;
	float heading;
	// prob:
	// float headingAlignment;
	// prob:
	// float headingBias;
	float headingStandardDeviation;
	int lagLength;
	int lowCorrelationThreshold;
	int minPrePingWaitHundredths;
	int minPrePingWaitMinutes;
	int minPrePingWaitSeconds;
	int numberOfBeams;
	int numberOfBytesInEnsemble;
	int numberOfCells;
	int numberOfCodeRepetitions;
	int numberOfDataTypes;
	int pdRealOrSimulatedFlag;
	int percentGoodMinimum;
	int pingHundredths;
	int pingMinutes;
	int pingSeconds;
	int pingsPerEnsemble;
	float pitch;
	float pitchStandardDeviation;
	float pressure;
	float pressureVariance;
	int profilingMode;
	int realTimeClockDay;
	int realTimeClockHour;
	int realTimeClockHundredths;
	int realTimeClockMinute;
	int realTimeClockMonth;
	int realTimeClockSecond;
	int realTimeClockYear;
	int realTimeY2KClockCentury;
	int realTimeY2KClockDay;
	int realTimeY2KClockHour;
	int realTimeY2KClockHundredths;
	int realTimeY2KClockMinute;
	int realTimeY2KClockMonth;
	int realTimeY2KClockSecond;
	int realTimeY2KClockYear;
	int referenceLayerEnd;
	int referenceLayerStart;
	int reservedBIT;
	float roll;
	float rollStandardDeviation;
	int salinity;
	int sensorAvailability;
	int sensorConfiguration;
	int sensorDepthSetting;
	int sensorHeadingSetting;
	int sensorPitchSetting;
	int sensorRollSetting;
	int sensorSalinitySetting;
	int sensorSource;
	int sensorSpeedOfSoundSetting;
	int sensorTemperatureSetting;
	int serialNumber;
	int signalProcessingMode;
	int spareFieldOne;
	int spareFieldTwo;
	int speedOfSound;
	int systemBandwidth;
	int systemConfiguration;
	int systemFrequency;
	int systemPower;
	float temperature;
	int transducerAttachment;
	int transformBinMappingSetting;
	int transformThreeBeamSetting;
	int transformTiltsSetting;
	int transmitLagDistance;
	int transmitPulseLength;
	// protected int VariableLeaderID;
	boolean isValid;

	// **************end ensemble vars*****************

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	public void fire() throws IllegalActionException {
		System.out
				.println("derik ensembleactor debug. arrayIO_tokenConsumptionRate:"
						+ arrayIO_tokenConsumptionRate.getValueAsString());
		super.fire();

		take2dArrays = ((BooleanToken) accept2dArrays.getToken())
				.booleanValue();

		ArrayToken atInput;
		ArrayToken token;
		int numValid = 0;
		int numInvalid = 0;
		Token[] dataValues = new Token[labels.length];
		RecordToken recToken = null;

		if (take2dArrays) {
			System.out.println("take2dArray mode");
			// this can be set to (requestDuration+1)*numStartTimes
			// (though there seems to be a bug at the moment where we're not
			// getting the +1 w/ the binary chan)
			maxArrayLength = ((IntToken) outputArrayLength.getToken())
					.intValue();
			System.out.println("maxArrayLength: " + maxArrayLength);

			DoubleToken timeTokens[] = new DoubleToken[maxArrayLength];
			Token tempTokens[] = new Token[maxArrayLength];
			Token pressureTokens[] = new Token[maxArrayLength];
			Token salinityTokens[] = new Token[maxArrayLength];
			Token systemPowerTokens[] = new Token[maxArrayLength];

			boolean allNils = true;

			boolean thisItrIsNil;
			while (arrayIO.hasToken(0)) {
				thisItrIsNil = true; // we'll check
				// numValid = 0;
				token = (ArrayToken) arrayIO.get(0);
				// System.out.println("EnsembleActor got a token on arrayIO. token.length: "
				// + token.length());

				// if *all* are nil, output nils
				for (int j = 0; j < token.length(); j++) {
					atInput = (ArrayToken) token.getElement(j);
					// System.out.print("derik tmp debug atInput.toString:" +
					// atInput.toString());

					if (atInput.isNil()
							|| atInput.toString().equalsIgnoreCase("nil")
							|| ((atInput.length() == 1) && (atInput.getElement(
									0).toString().equalsIgnoreCase("nil")))) {
						System.out.println("EnsembleActor got a nil token");
					} else {
						thisItrIsNil = false;
						allNils = false;
					}
				}

				if (!thisItrIsNil) {
					for (int j = 0; j < token.length(); j++) {
						atInput = (ArrayToken) token.getElement(j);

						// convert token to java type
						byteArray = ArrayToken
								.arrayTokenToUnsignedByteArray(atInput);

						ensembleBuffer = ByteBuffer.allocate(byteArray.length);
						ensembleBuffer.put(byteArray);

						Ensemble en = new Ensemble(ensembleBuffer);

						setVars(en);
						// printDebug();

						if (!isValid) {
							numInvalid++;
						} else {
							double time = getEnsembleDate(en);

							tempTokens[numValid] = new FloatToken(temperature);
							pressureTokens[numValid] = new FloatToken(pressure);
							salinityTokens[numValid] = new IntToken(salinity);
							systemPowerTokens[numValid] = new IntToken(
									systemPower);
							timeTokens[numValid] = new DoubleToken(time);

							numValid++;

							if (numValid >= maxArrayLength) {
								System.out
										.println("ERROR length exceeds hardcoded array max length in EnsembleActor.java");
							}
						}
					}
				}
				// }
			}// end while

			System.out
					.println("num valid ensembles (actual output array length):"
							+ numValid
							+ "\nnum Invalid ensembles:"
							+ numInvalid);

			if (allNils) {
				System.out
						.println("All tokens received were nil, outputtings nils");
				Token[] nilTokenArray = new Token[1];
				nilTokenArray[0] = Token.NIL;
				ArrayToken nilArrayToken = new ArrayToken(nilTokenArray);

				dataValues[0] = nilArrayToken;
				dataValues[1] = nilArrayToken;
				recToken = new RecordToken(labels, dataValues);
				System.out
						.println("EnsembleActor sending NILS out ports.\n***");
				tempIO.send(0, recToken);
				pressureIO.send(0, recToken);
				salinityIO.send(0, recToken);
				systemPowerIO.send(0, recToken);

			}

			if (numValid > 0) {
				Token actualLengthTempTokens[] = new Token[numValid];
				Token actualLengthPressureTokens[] = new Token[numValid];
				Token actualLengthSalinityTokens[] = new Token[numValid];
				Token actualLengthSystemPowerTokens[] = new Token[numValid];
				DoubleToken actualLengthTimeTokens[] = new DoubleToken[numValid];

				for (int i = 0; i < numValid; i++) {
					actualLengthTempTokens[i] = tempTokens[i];
					actualLengthPressureTokens[i] = pressureTokens[i];
					actualLengthSalinityTokens[i] = salinityTokens[i];
					actualLengthSystemPowerTokens[i] = systemPowerTokens[i];
					actualLengthTimeTokens[i] = timeTokens[i];
				}

				ArrayToken atTimeTokens = new ArrayToken(actualLengthTimeTokens);
				ArrayToken atTempTokens = new ArrayToken(actualLengthTempTokens);
				ArrayToken atPressureTokens = new ArrayToken(
						actualLengthPressureTokens);
				ArrayToken atSalinityTokens = new ArrayToken(
						actualLengthSalinityTokens);
				ArrayToken atSystemPowerTokens = new ArrayToken(
						actualLengthSystemPowerTokens);

				dataValues[0] = atTimeTokens;
				dataValues[1] = atTempTokens;
				recToken = new RecordToken(labels, dataValues);
				System.out.println("EnsembleActor sending out ports.\n***");
				tempIO.send(0, recToken);

				dataValues[1] = atPressureTokens;
				recToken = new RecordToken(labels, dataValues);
				pressureIO.send(0, recToken);

				dataValues[1] = atSalinityTokens;
				recToken = new RecordToken(labels, dataValues);
				salinityIO.send(0, recToken);

				dataValues[1] = atSystemPowerTokens;
				recToken = new RecordToken(labels, dataValues);
				systemPowerIO.send(0, recToken);
			}
		} else { // if not 2dArrays (ie 1 1d Ensemble at a time)
			System.out.println("per-Ensemble mode");

			while (arrayIO.hasToken(0)) {
				token = (ArrayToken) arrayIO.get(0);

				Token[] testTokens = token.arrayValue();
				for (int i = 0; i < testTokens.length; i++) {
					if (token.isNil()
							|| testTokens[i].toString().equalsIgnoreCase("nil")) {
						System.out
								.println("EnsembleActor got a nil token, outputting nil tokens");
						dataValues[0] = Token.NIL;
						dataValues[1] = Token.NIL;
						recToken = new RecordToken(labels, dataValues);
						tempIO.send(0, recToken);
						pressureIO.send(0, recToken);
						salinityIO.send(0, recToken);
						systemPowerIO.send(0, recToken);
					} else {

						// convert token to java type
						byteArray = ArrayToken
								.arrayTokenToUnsignedByteArray(token);

						ensembleBuffer = ByteBuffer.allocate(byteArray.length);
						ensembleBuffer.put(byteArray);
						Ensemble en = new Ensemble(ensembleBuffer);

						setVars(en);

						double time = getEnsembleDate(en);

						if (en.isValid()) {

							dataValues[0] = new DoubleToken(time);
							dataValues[1] = new FloatToken(temperature);
							recToken = new RecordToken(labels, dataValues);
							tempIO.send(0, recToken);

							dataValues[1] = new FloatToken(pressure);
							recToken = new RecordToken(labels, dataValues);
							pressureIO.send(0, recToken);

							dataValues[1] = new IntToken(salinity);
							recToken = new RecordToken(labels, dataValues);
							salinityIO.send(0, recToken);

							dataValues[1] = new IntToken(systemPower);
							recToken = new RecordToken(labels, dataValues);
							systemPowerIO.send(0, recToken);
						}
					}
				}
			}
		}

		// postfire();
	}// fire

	public double getEnsembleDate(Ensemble en) {

		double time = 0;

		int ensYear = (en.getRealTimeY2KClockCentury() * 100)
				+ en.getRealTimeY2KClockYear();
		int ensMonth = en.getRealTimeY2KClockMonth() - 1; // 0 start
		int ensDay = en.getRealTimeY2KClockDay();
		int ensHour = en.getRealTimeY2KClockHour();
		int ensMinute = en.getRealTimeY2KClockMinute();
		int ensSecond = en.getRealTimeY2KClockSecond();
		int ensHundredths = en.getRealTimeY2KClockHundredths();

		Calendar ensCalendar = Calendar
				.getInstance(TimeZone.getTimeZone("GMT"));
		ensCalendar.set(ensYear, ensMonth, ensDay, ensHour, ensMinute,
				ensSecond);
		ensCalendar.add(Calendar.MILLISECOND, ensHundredths * 10);
		time = (double) ensCalendar.getTime().getTime();

		// System.out.println("ensCalendar:" +ensCalendar.getTime().toString() +
		// " time:" + time);
		return time;
	}

	public void setVars(Ensemble en) {

		ADCChannelFive = en.getADCChannelFive();
		ADCChannelFour = en.getADCChannelFour();
		ADCChannelOne = en.getADCChannelOne();
		ADCChannelSeven = en.getADCChannelSeven();
		ADCChannelSix = en.getADCChannelSix();
		ADCChannelThree = en.getADCChannelThree();
		ADCChannelTwo = en.getADCChannelTwo();
		ADCChannelZero = en.getADCChannelZero();
		baseFrequencyIndex = en.getBaseFrequencyIndex();
		beamAngle = en.getBeamAngle();
		beamDirection = en.getBeamDirection();
		beamPattern = en.getBeamPattern();
		binOneDistance = en.getBinOneDistance();
		blankAfterTransmit = en.getBlankAfterTransmit();
		builtInTestResult = en.getBuiltInTestResult();
		//
		checksum = en.getChecksum();
		coordinateTransformParams = en.getCoordinateTransformParams();
		cpuFirmwareRevision = en.getCpuFirmwareRevision();
		cpuFirmwareVersion = en.getCpuFirmwareVersion();
		dataTypeID = 0x7f7f; // EnsembleDataType.HEADER
		ensembleDataType = en.getDataType(dataTypeID);
		// prob:
		// dataTypeNumber = en.getDataTypeNumber(ensembleDataType);
		// dataTypeOffset = en.getDataTypeOffset(dataTypeNumber);
		depthCellLength = en.getDepthCellLength();
		depthOfTransducer = en.getDepthOfTransducer();
		ensembleNumber = en.getEnsembleNumber();
		ensembleNumberIncrement = en.getEnsembleNumberIncrement();
		errorStatusWord = en.getErrorStatusWord();
		errorVelocityThreshold = en.getErrorVelocityThreshold();
		falseTargetThreshold = en.getFalseTargetThreshold();
		fixedLeaderID = en.getFixedLeaderID();
		fixedLeaderSpare = en.getFixedLeaderSpare();

		// prob:
		// int headerID = en.getHeaderID();

		headerSpare = en.getHeaderSpare();
		heading = en.getHeading();
		// prob:
		// float headingAlignment = en.getHeadingAlignment();
		// prob:
		// float headingBias = en.getHeadingBias();
		headingStandardDeviation = en.getHeadingStandardDeviation();
		lagLength = en.getLagLength();
		lowCorrelationThreshold = en.getLowCorrelationThreshold();
		minPrePingWaitHundredths = en.getMinPrePingWaitHundredths();
		minPrePingWaitMinutes = en.getMinPrePingWaitMinutes();
		minPrePingWaitSeconds = en.getMinPrePingWaitSeconds();
		numberOfBeams = en.getNumberOfBeams();
		numberOfBytesInEnsemble = en.getNumberOfBytesInEnsemble();
		numberOfCells = en.getNumberOfCells();
		numberOfCodeRepetitions = en.getNumberOfCodeRepetitions();
		numberOfDataTypes = en.getNumberOfDataTypes();
		pdRealOrSimulatedFlag = en.getPdRealOrSimulatedFlag();
		percentGoodMinimum = en.getPercentGoodMinimum();
		pingHundredths = en.getPingHundredths();
		pingMinutes = en.getPingMinutes();
		pingSeconds = en.getPingSeconds();
		pingsPerEnsemble = en.getPingsPerEnsemble();
		pitch = en.getPitch();
		pitchStandardDeviation = en.getPitchStandardDeviation();
		pressure = en.getPressure();
		pressureVariance = en.getPressureVariance();
		profilingMode = en.getProfilingMode();
		realTimeClockDay = en.getRealTimeClockDay();
		realTimeClockHour = en.getRealTimeClockHour();
		realTimeClockHundredths = en.getRealTimeClockHundredths();
		realTimeClockMinute = en.getRealTimeClockMinute();
		realTimeClockMonth = en.getRealTimeClockMonth();
		realTimeClockSecond = en.getRealTimeClockSecond();
		realTimeClockYear = en.getRealTimeClockYear();
		realTimeY2KClockCentury = en.getRealTimeY2KClockCentury();
		realTimeY2KClockDay = en.getRealTimeY2KClockDay();
		realTimeY2KClockHour = en.getRealTimeY2KClockHour();
		realTimeY2KClockHundredths = en.getRealTimeY2KClockHundredths();
		realTimeY2KClockMinute = en.getRealTimeY2KClockMinute();
		realTimeY2KClockMonth = en.getRealTimeY2KClockMonth();
		realTimeY2KClockSecond = en.getRealTimeY2KClockSecond();
		realTimeY2KClockYear = en.getRealTimeY2KClockYear();
		referenceLayerEnd = en.getReferenceLayerEnd();
		referenceLayerStart = en.getReferenceLayerStart();
		reservedBIT = en.getReservedBIT();
		roll = en.getRoll();
		rollStandardDeviation = en.getRollStandardDeviation();
		salinity = en.getSalinity();
		sensorAvailability = en.getSensorAvailability();
		sensorConfiguration = en.getSensorConfiguration();
		sensorDepthSetting = en.getSensorDepthSetting();
		sensorHeadingSetting = en.getSensorHeadingSetting();
		sensorPitchSetting = en.getSensorPitchSetting();
		sensorRollSetting = en.getSensorRollSetting();
		sensorSalinitySetting = en.getSensorSalinitySetting();
		sensorSource = en.getSensorSource();
		sensorSpeedOfSoundSetting = en.getSensorSpeedOfSoundSetting();
		sensorTemperatureSetting = en.getSensorTemperatureSetting();
		serialNumber = en.getSerialNumber();
		signalProcessingMode = en.getSignalProcessingMode();
		spareFieldOne = en.getSpareFieldOne();
		spareFieldTwo = en.getSpareFieldTwo();
		speedOfSound = en.getSpeedOfSound();
		systemBandwidth = en.getSystemBandwidth();
		systemConfiguration = en.getSystemConfiguration();
		systemFrequency = en.getSystemFrequency();
		systemPower = en.getSystemPower();
		temperature = en.getTemperature();
		transducerAttachment = en.getTransducerAttachment();
		transformBinMappingSetting = en.getTransformBinMappingSetting();
		transformThreeBeamSetting = en.getTransformThreeBeamSetting();
		transformTiltsSetting = en.getTransformTiltsSetting();
		transmitLagDistance = en.getTransmitLagDistance();
		transmitPulseLength = en.getTransmitPulseLength();
		// VariableLeaderID = en.getVariableLeaderID();
		isValid = en.isValid();

	}

	public void printDebug() {

		System.out.println("ADCChannelFive: " + ADCChannelFive);
		System.out.println("ADCChannelFour: " + ADCChannelFour);
		System.out.println("ADCChannelOne: " + ADCChannelOne);
		System.out.println("ADCChannelSeven: " + ADCChannelSeven);
		System.out.println("ADCChannelSix: " + ADCChannelSix);
		System.out.println("ADCChannelThree: " + ADCChannelThree);
		System.out.println("ADCChannelTwo: " + ADCChannelTwo);
		System.out.println("ADCChannelZero: " + ADCChannelZero);
		System.out.println("baseFrequencyIndex: " + baseFrequencyIndex);
		System.out.println("beamAngle: " + beamAngle);
		System.out.println("beamDirection: " + beamDirection);
		System.out.println("beamPattern: " + beamPattern);
		System.out.println("binOneDistance: " + binOneDistance);
		System.out.println("blankAfterTransmit: " + blankAfterTransmit);
		System.out.println("builtInTestResult: " + builtInTestResult);
		System.out.println("checksum: " + checksum);
		System.out.println("coordinateTransformParams: "
				+ coordinateTransformParams);
		System.out.println("cpuFirmwareRevision: " + cpuFirmwareRevision);
		System.out.println("cpuFirmwareVersion: " + cpuFirmwareVersion);
		// System.out.println("dataTypeNumber: " + dataTypeNumber);
		// System.out.println("dataTypeOffset: " + dataTypeOffset);
		System.out.println("depthCellLength: " + depthCellLength);
		System.out.println("depthOfTransducer: " + depthOfTransducer);
		System.out.println("ensembleNumber: " + ensembleNumber);
		System.out.println("ensembleNumberIncrement: "
				+ ensembleNumberIncrement);
		System.out.println("errorStatusWord: " + errorStatusWord);
		System.out.println("errorVelocityThreshold: " + errorVelocityThreshold);
		System.out.println("falseTargetThreshold: " + falseTargetThreshold);
		System.out.println("fixedLeaderID: " + fixedLeaderID);
		System.out.println("fixedLeaderSpare: " + fixedLeaderSpare);
		// //some error with this get?
		// System.out.println("headerID: " + headerID);
		// System.out.println("headingAlignment: " + headingAlignment);
		// System.out.println("headingBias: " + headingBias);
		System.out.println("headerSpare: " + headerSpare);
		System.out.println("heading: " + heading);
		System.out.println("headingStandardDeviation: "
				+ headingStandardDeviation);
		System.out.println("lagLength: " + lagLength);
		System.out.println("lowCorrelationThreshold: "
				+ lowCorrelationThreshold);
		System.out.println("minPrePingWaitHundredths: "
				+ minPrePingWaitHundredths);
		System.out.println("minPrePingWaitMinutes: " + minPrePingWaitMinutes);
		System.out.println("minPrePingWaitSeconds: " + minPrePingWaitSeconds);
		System.out.println("numberOfBeams: " + numberOfBeams);
		System.out.println("numberOfBytesInEnsemble: "
				+ numberOfBytesInEnsemble);
		System.out.println("numberOfCells: " + numberOfCells);
		System.out.println("numberOfCodeRepetitions: "
				+ numberOfCodeRepetitions);
		System.out.println("numberOfDataTypes: " + numberOfDataTypes);
		System.out.println("pdRealOrSimulatedFlag: " + pdRealOrSimulatedFlag);
		System.out.println("percentGoodMinimum: " + percentGoodMinimum);
		System.out.println("pingHundredths: " + pingHundredths);
		System.out.println("pingMinutes: " + pingMinutes);
		System.out.println("pingSeconds: " + pingSeconds);
		System.out.println("pingsPerEnsemble: " + pingsPerEnsemble);
		System.out.println("pitch: " + pitch);
		System.out.println("pitchStandardDeviation: " + pitchStandardDeviation);
		System.out.println("pressure: " + pressure);
		System.out.println("pressureVariance: " + pressureVariance);
		System.out.println("profilingMode: " + profilingMode);
		System.out.println("realTimeClockDay: " + realTimeClockDay);
		System.out.println("realTimeClockHour: " + realTimeClockHour);
		System.out.println("realTimeClockHundredths: "
				+ realTimeClockHundredths);
		System.out.println("realTimeClockMinute: " + realTimeClockMinute);
		System.out.println("realTimeClockMonth: " + realTimeClockMonth);
		System.out.println("realTimeClockSecond: " + realTimeClockSecond);
		System.out.println("realTimeClockYear: " + realTimeClockYear);
		System.out.println("realTimeY2KClockCentury: "
				+ realTimeY2KClockCentury);
		System.out.println("realTimeY2KClockDay: " + realTimeY2KClockDay);
		System.out.println("realTimeY2KClockHour: " + realTimeY2KClockHour);
		System.out.println("realTimeY2KClockHundredths: "
				+ realTimeY2KClockHundredths);
		System.out.println("realTimeY2KClockMinute: " + realTimeY2KClockMinute);
		System.out.println("realTimeY2KClockMonth: " + realTimeY2KClockMonth);
		System.out.println("realTimeY2KClockSecond: " + realTimeY2KClockSecond);
		System.out.println("realTimeY2KClockYear: " + realTimeY2KClockYear);
		System.out.println("referenceLayerEnd: " + referenceLayerEnd);
		System.out.println("referenceLayerStart: " + referenceLayerStart);
		System.out.println("reservedBIT: " + reservedBIT);
		System.out.println("roll: " + roll);
		System.out.println("rollStandardDeviation: " + rollStandardDeviation);
		System.out.println("salinity: " + salinity);
		System.out.println("sensorAvailability: " + sensorAvailability);
		System.out.println("sensorConfiguration: " + sensorConfiguration);
		System.out.println("sensorDepthSetting: " + sensorDepthSetting);
		System.out.println("sensorHeadingSetting: " + sensorHeadingSetting);
		System.out.println("sensorPitchSetting: " + sensorPitchSetting);
		System.out.println("sensorRollSetting: " + sensorRollSetting);
		System.out.println("sensorSalinitySetting: " + sensorSalinitySetting);
		System.out.println("sensorSource: " + sensorSource);
		System.out.println("sensorSpeedOfSoundSetting: "
				+ sensorSpeedOfSoundSetting);
		System.out.println("sensorTemperatureSetting: "
				+ sensorTemperatureSetting);
		System.out.println("serialNumber: " + serialNumber);
		System.out.println("signalProcessingMode: " + signalProcessingMode);
		System.out.println("spareFieldOne: " + spareFieldOne);
		System.out.println("spareFieldTwo: " + spareFieldTwo);
		System.out.println("speedOfSound: " + speedOfSound);
		System.out.println("systemBandwidth: " + systemBandwidth);
		System.out.println("systemConfiguration: " + systemConfiguration);
		System.out.println("systemFrequency: " + systemFrequency);
		System.out.println("systemPower: " + systemPower);
		System.out.println("temperature: " + temperature);
		System.out.println("transducerAttachment: " + transducerAttachment);
		System.out.println("transformBinMappingSetting: "
				+ transformBinMappingSetting);
		System.out.println("transformThreeBeamSetting: "
				+ transformThreeBeamSetting);
		System.out.println("transformTiltsSetting: " + transformTiltsSetting);
		System.out.println("transmitLagDistance: " + transmitLagDistance);
		System.out.println("transmitPulseLength: " + transmitPulseLength);
		System.out.println("isValid: " + isValid);
	}

	public void attributeChanged(ptolemy.kernel.util.Attribute attribute)
			throws ptolemy.kernel.util.IllegalActionException {

		if (attribute == accept2dArrays) {
			boolean tmpTake2dArrays = ((BooleanToken) accept2dArrays.getToken())
					.booleanValue();

			if (take2dArrays != tmpTake2dArrays) {
				take2dArrays = tmpTake2dArrays;
				setPortsFor2d(tmpTake2dArrays);

				if (take2dArrays) {
					numInputTokens.setExpression("numStartTimes");
				} else {
					numInputTokens.setExpression("1");
				}

			}
		}

	}

	public void setPortsFor2d(boolean twoD) {
		if (twoD) {

			// input
			arrayIO.setTypeEquals(new ArrayType(new ArrayType(
					BaseType.UNSIGNED_BYTE)));
			arrayIO_tokenConsumptionRate.setExpression("numInputTokens");
			arrayIO_tokenConsumptionRate.updateContent();

			// outputs
			Type[] types = new Type[labels.length];

			types[0] = new ArrayType(BaseType.DOUBLE);
			types[1] = new ArrayType(BaseType.FLOAT);
			RecordType rt = new RecordType(labels, types);

			tempIO.setTypeEquals(rt);
			pressureIO.setTypeEquals(rt);

			types[1] = new ArrayType(BaseType.INT);
			rt = new RecordType(labels, types);
			salinityIO.setTypeEquals(rt);
			systemPowerIO.setTypeEquals(rt);

			outputArrayLength
					.setExpression("(requestDuration+1)*numStartTimes"); // todo:
																			// this
																			// default
																			// is
																			// not
																			// generic
																			// obviously

		} else {

			// input
			arrayIO.setTypeEquals(new ArrayType(BaseType.UNSIGNED_BYTE));
			arrayIO_tokenConsumptionRate.setExpression("1");
			arrayIO_tokenConsumptionRate.updateContent();

			// outputs
			Type[] types = new Type[labels.length];

			types[0] = BaseType.DOUBLE;
			types[1] = BaseType.FLOAT;
			RecordType rt = new RecordType(labels, types);

			tempIO.setTypeEquals(rt);
			pressureIO.setTypeEquals(rt);

			types[1] = BaseType.INT;
			rt = new RecordType(labels, types);
			salinityIO.setTypeEquals(rt);
			systemPowerIO.setTypeEquals(rt);

			// this is just to avoid confusion, outputArrayLength is not used
			// for per-Ensemble mode.
			outputArrayLength.setExpression("0");

		}
	}

	/*
	 * Post fire the actor. Return false to indicated that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 */
	public boolean postfire() throws IllegalActionException {

		return false;
	}

}// EnsembleActor.java