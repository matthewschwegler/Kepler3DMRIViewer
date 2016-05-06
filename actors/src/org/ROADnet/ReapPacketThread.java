/*
 * Copyright (c) 2002-2010 The Regents of the University of California.
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

package org.ROADnet;

//orb classes
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

import com.brtt.antelope.Orb;
import com.brtt.antelope.OrbPacketChannel;
import com.brtt.antelope.OrbWaveformPacket;

/**
 * This actor connects to an Antelope ORB and collects element values for
 * weather elements such as temperature,pressure,humidity,wind.
 * 
 * @see OrbLogger,OrbWaveformSource, OrbWaveformSink, OrbPacketSource,
 * @author Nandita Mangal, University of California
 * @version $Id: ReapPacketThread.java 24234 2010-05-06 05:21:26Z welker $
 * @UserLevelDocumentation This actor connects to the Antelope ORB and logs the
 *                         required sensor data values.
 */

class ReapPacketThread extends Thread {

	String packetName = "";
	String element = "";
	String orbsource = "";
	String sites = "";
	String valueRange = "";
	String filePath = "";
	Orb _orb;

	public ReapPacketThread(String orbname, String name, String element,
			String sitesFile, String ranges, String path) {
		try {
			// initialize orb
			_orb = new Orb(orbname, "r");

			// initialize other fields such as packet name,weather element,
			// location of sensor sites file
			// ranges for the current value and the path to save all produced
			// files
			packetName = name;
			this.element = element;
			sites = sitesFile;
			valueRange = ranges;
			filePath = path;

		} catch (Exception e) {
			writeErrorLog(e.toString());
		}

	}

	public void run() {
		unstuffPacketValues();
		return;
	}

	public void unstuffPacketValues() {

		try {

			// Write to files

			BufferedWriter bwSensorsNames = new BufferedWriter(new FileWriter(
					new String(filePath + "/" + element + "FileSensors"), true));
			BufferedWriter bwValues = new BufferedWriter(new FileWriter(
					new String(filePath + "/" + element + "FileValues"), true));

			String[] location = getSourceLocation(packetName);
			String[] values = getSourceValues(packetName);

			// System.err.println(packetName);

			if (location == null)
				writeErrorLog("\nNO SOURCE FOUND FOR:" + packetName);
			if (location != null && values != null) {
				bwValues.write(location[0] + "  " + location[1] + "  "
						+ values[0] + "  " + values[1]);
				bwValues.write("\n");
				bwSensorsNames.write(packetName);
				bwSensorsNames.write("\n");

			}

			bwSensorsNames.close();
			bwValues.close();

			BufferedWriter threadLog = new BufferedWriter(new FileWriter(
					"ThreadLog", true));
			threadLog.write("Done with Thread:" + this.toString() + "\nPacket:"
					+ packetName);
			threadLog.close();

			return;
		} catch (Exception e) {
			writeErrorLog(e.toString());
			return;
		}

	}

	/**
	 * This method determines the sensor values by unstuffing algorithms If the
	 * value obtained is within range, then value & timestamps are returned
	 * 
	 * @param: The Sensor SourceName to get value for
	 * @return: StringArray with value & timestamp, return null if no value/bad
	 *          value.
	 * 
	 */
	public String[] getSourceValues(String name) {

		try {
			_orb.select(name);
			_orb.after(0);

			// reap the packet from ORB
			OrbWaveformPacket pkt = (OrbWaveformPacket) (_orb.reap(true));
			for (int c = 0; c < pkt.channels.size(); c++) {
				OrbPacketChannel channel = (OrbPacketChannel) (pkt.channels
						.get(c));
				for (int n = 0; n < channel.nsamp; n++) {

					String[] updatedSensorValues = new String[2];

					updatedSensorValues[0] = channel.data[n] * channel.calib
							+ ""; // added value
					updatedSensorValues[1] = channel.time + channel.samprate
							+ "";// added timestamp

					String range = valueRange.substring(1,
							valueRange.length() - 1);
					String[] rangeArray = new String[2];
					rangeArray = range.split(",");

					// find the minimnum & max values in the range.
					double min = Double.valueOf(rangeArray[0]).doubleValue();
					double max = Double.valueOf(rangeArray[1]).doubleValue();

					if (channel.data[n] * channel.calib < min
							|| channel.data[n] * channel.calib > max)
						return null;

					return updatedSensorValues;
				}

			}

		} catch (Exception e) {
			writeErrorLog("When retrieving packet values for :" + packetName);
			return null;
		}
		return null;

	}

	/**
	 * This method determines the sensor location for the given source from a
	 * sensor locations file.
	 * 
	 * @param: The Sensor SourceName to get location for.
	 * @return: StringArray with lat,long value return null if no location for
	 *          that particular sourceName was found.
	 */

	private String[] getSourceLocation(String sourceName) {

		// could be made shorter process by pre-storing all _values..
		String[] sourceParts = sourceName.split("_");
		Vector locationParts = new Vector();

		try {
			// read the sites file
			BufferedReader in = new BufferedReader(new FileReader(sites));
			String str;
			while ((str = in.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(str);
				while (st.hasMoreTokens()) {
					locationParts.add(st.nextToken());
				}
				// sourceParts[1] is the part of source name after "_" e.g ML of
				// the sourceName "HPWREN_ML"
				// If we find ML in the sitesFile we get the lat,long values
				// (locationParts vector's first & second element)
				if (new String(sourceParts[1]).equals((String) locationParts
						.get(2))) {
					String[] result = new String[2];
					result[0] = (String) locationParts.get(0);
					result[1] = (String) locationParts.get(1);
					return result;
				}
				locationParts.removeAllElements();
			}
			in.close();
		} catch (IOException e) {
			writeErrorLog(e.toString());
		}

		// null if no location for that particular sourceName was found.
		return null;
	}

	public void writeErrorLog(String message) {

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(
					"ErrorsLogFile", true));
			bw.write("\n");
			bw.write(message);
			bw.close();

		} catch (Exception e) {
			System.err.println(e.toString());
		}

	}

}