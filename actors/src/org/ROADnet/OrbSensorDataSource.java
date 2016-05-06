/*
 * Copyright (c) 2010 The Regents of the University of California.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.StringTokenizer;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ObjectToken;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.brtt.antelope.Orb;

/**
 * This actor connects to an Antelope ORB and collects packets matching the
 * given sourcename. Packets are reaped from the Orb and unpacked assuming that
 * they're OrbImagePackets. The actor outputs ImageTokens to Ptolemy.
 * 
 * @author Tobin Fricke (tobin@splorg.org), University of California
 * @version $Id: OrbSensorDataSource.java 24234 2010-05-06 05:21:26Z welker $
 * @Pt.ProposedRating Red (tobin)
 */

public class OrbSensorDataSource extends TypedAtomicActor {
	/**
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the actor cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public OrbSensorDataSource(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		output = new TypedIOPort(this, "output", false, true);
		output.setMultiport(false);
		output.setTypeEquals(BaseType.OBJECT);

		orbname = new StringParameter(this, "orbname");
		srcname = new StringParameter(this, "srcname");
		location = new StringParameter(this, "location");

		orbname.setTypeEquals(BaseType.STRING);
		srcname.setTypeEquals(BaseType.STRING);
		location.setTypeEquals(BaseType.STRING);
		addAvailableLocationChoices();
	}

	/**
	 * Initialize the component and connect to the ORB.
	 * 
	 * public void initialize() throws IllegalActionException {
	 * 
	 * 
	 * try {
	 * 
	 * // _orb = new
	 * Orb(StringToken.convert(orbname.getToken()).stringValue(),"r");
	 * //_orb.select(StringToken.convert(srcname.getToken()).stringValue());
	 * //_orb.after(0);
	 * 
	 * 
	 * 
	 * } catch (Exception e) { throw new IllegalActionException(this,
	 * "Couldn't connect to Orb."+ " (" + e.getMessage() + ")"); }
	 * 
	 * }
	 */
	/**
	 * Reap one packet from the ORB, unstuff it as an OrbImagePacket, and output
	 * the resulting ImageToken (containing a java.awt.Image object). Note that
	 * this whole actor can be implemented as a composite actor utilizing
	 * ObjectToRecord, RecordDisassembler, and something that forms ImageTokens
	 * from java.awt.Image.
	 */

	public void fire() throws IllegalActionException {

		// get status.txt file now from the above ORB.
		// get sites.txt file now from the ORB also (?)
		status = new File(System.getProperty("KEPLER")
				+ "//lib//testdata//ROADNet//status.txt");
		sites = new File(System.getProperty("KEPLER")
				+ "//lib//testdata//ROADNet//sites.txt");

		output.broadcast(new ObjectToken(computeSparseMatrices()));
		/*
		 * try { OrbRawPacket pkt = (OrbRawPacket)(_orb.reap(false));
		 * OrbImagePacket imgPkt =
		 * (OrbImagePacket)(OrbImagePacket.unstuff(pkt)); output.broadcast(new
		 * AWTImageToken(imgPkt.image));
		 * 
		 * } catch (Exception e) { throw new IllegalActionException(this,
		 * e.getMessage()); }
		 */
	}

	// /////////////////////////////////////////////////////////
	// /// private methods ////

	private void addAvailableLocationChoices() {
		// get available location choices
		location.addChoice("San Diego");

	}

	private HashMap computeSparseMatrices() {

		HashMap dataMatrices = new HashMap();

		try {

			FileOutputStream out;
			PrintStream p;
			out = new FileOutputStream(System.getProperty("KEPLER")
					+ "//lib//testdata//ROADNet//WeatherSensors.data");
			p = new PrintStream(out);

			int weatherChannels = countSiteDataVariables() - 4; // (e.g
																// temperature,Pressure,humidity
																// ..)
			System.err.println(weatherChannels);

			for (int channelCounter = 0; channelCounter < weatherChannels; channelCounter++) {
				String[][] outputMatrix = new String[countSites()][countSiteDataVariables()];
				int rowCounter = 0;

				// using sites,status files make a string array containing
				// sites,

				BufferedReader sitesInput = new BufferedReader(new FileReader(
						sites));
				String strSites;
				while ((strSites = sitesInput.readLine()) != null) {
					int i = 0;
					// System.err.println(strSites);
					String[] resultSites = new String[countSiteDataVariables()];
					StringTokenizer lineTok = new StringTokenizer(strSites);
					while (lineTok.hasMoreTokens()) {
						String word = lineTok.nextToken();
						resultSites[i] = word;
						i++;
					}

					String net = resultSites[0];
					String sta = resultSites[1];
					/*
					 * String chan = result[2]; String chan2 = result[3];
					 */
					String lat = resultSites[countSiteDataVariables() - 2];
					String longitude = resultSites[countSiteDataVariables() - 1];
					String currentChannel = resultSites[channelCounter + 2];

					// now search through status for entries with the same name
					// as net,sta,chan
					BufferedReader statusInput = new BufferedReader(
							new FileReader(status));
					String strStatus;
					while ((strStatus = statusInput.readLine()) != null) {
						i = 0;
						String[] resultStatus = new String[30]; // current
																// status file
																// 10 data
																// variables.
						StringTokenizer lineTok2 = new StringTokenizer(
								strStatus);
						while (lineTok2.hasMoreTokens()) {
							String word2 = lineTok2.nextToken();
							resultStatus[i] = word2;
							i++;
						}

						if (resultStatus[0].equals(net)
								&& resultStatus[1].equals(sta)
								&& resultStatus[2].equals(currentChannel)) {
							System.out.println("****" + strStatus + "****\n\n");
							String sample = resultStatus[8];

							outputMatrix[rowCounter][0] = lat;
							outputMatrix[rowCounter][1] = longitude;
							outputMatrix[rowCounter][2] = sample;
							outputMatrix[rowCounter][3] = net;
							outputMatrix[rowCounter][4] = sta;
							outputMatrix[rowCounter][5] = currentChannel;

							System.err.println(outputMatrix[rowCounter][0]
									+ " " + outputMatrix[rowCounter][1] + " "
									+ outputMatrix[rowCounter][2] + " "
									+ outputMatrix[rowCounter][3] + " "
									+ outputMatrix[rowCounter][4] + " "
									+ outputMatrix[rowCounter][5]);
							p.println(new String(outputMatrix[rowCounter][0]
									+ " " + outputMatrix[rowCounter][1] + " "
									+ outputMatrix[rowCounter][2] + " "
									+ outputMatrix[rowCounter][3] + " "
									+ outputMatrix[rowCounter][4] + " "
									+ outputMatrix[rowCounter][5]));
							rowCounter++;
						}

					}// end of status while

				}// end of sites while

				p.close();
				String keySparseMatrix = "";
				if (channelCounter == 0)
					keySparseMatrix = "temperature";
				else if (channelCounter == 1)
					keySparseMatrix = "humidity";
				else if (channelCounter == 2)
					keySparseMatrix = "pressure";
				else if (channelCounter == 3)
					keySparseMatrix = "wind";

				dataMatrices.put(keySparseMatrix, outputMatrix);

			}// end of for

		} catch (Exception e) {
			System.err.println(e.toString());
		}

		return dataMatrices;

	}

	public int countSites() throws Exception {
		int siteCounter = 0;
		BufferedReader sitesInput = new BufferedReader(new FileReader(sites));
		while ((sitesInput.readLine()) != null)
			siteCounter++;

		return siteCounter;
	}

	public int countSiteDataVariables() throws Exception {
		BufferedReader sitesInput = new BufferedReader(new FileReader(sites));
		String siteStr = sitesInput.readLine();

		int dataVariablesCounter = 0;
		String[] result2 = new String[30];

		StringTokenizer lineTok2 = new StringTokenizer(siteStr);
		while (lineTok2.hasMoreTokens()) {
			String word2 = lineTok2.nextToken();
			dataVariablesCounter++;
		}
		return dataVariablesCounter;
	}

	/**
	 * The name of the orb to connect to, in the format "hostname:port". Note
	 * that orbnames.pf-style names are not supported -- you have to use a valid
	 * IP address or resolvable DNS name, and you have to use a numeric port
	 * number.
	 */

	public StringParameter orbname;

	/**
	 * The source name to request from the Orb. When this actor is initialized,
	 * orb.select() is called with the value of this parameter.
	 */

	public StringParameter srcname;

	/** Samples from incoming waveform packets appear on this port. */

	public TypedIOPort output;

	/** The location to be mapped */

	public StringParameter location;

	/**
	 * This is our orb handle. Maybe one day it will also come on an input
	 * channel.
	 */

	private Orb _orb;
	private File status;
	private File sites;

}
