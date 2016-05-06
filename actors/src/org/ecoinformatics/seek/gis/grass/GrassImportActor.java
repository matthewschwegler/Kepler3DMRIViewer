/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
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

package org.ecoinformatics.seek.gis.grass;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * This actor imports an ARC file into the GRASS database. it does not produce
 * any tangible output, rather it just writes the info the the GRASS DB and
 * fires a trigger to say that it's done.
 */

public class GrassImportActor extends TypedAtomicActor {
	// input ports
	public TypedIOPort inFileName = new TypedIOPort(this, "inFileName", true,
			false);
	public TypedIOPort dataType = new TypedIOPort(this, "dataType", true, false);
	public TypedIOPort objectName = new TypedIOPort(this, "objectName", true,
			false);
	public TypedIOPort trigger = new TypedIOPort(this, "trigger", false, true);

	public Parameter grass_gisbase = new Parameter(this, "GRASS_GISBASE");
	public Parameter location_name = new Parameter(this, "LOCATION_NAME");
	public Parameter gisdbase = new Parameter(this, "GISDBASE");
	public Parameter gisrc = new Parameter(this, "GISRC");

	private String GRASS_GISBASE;
	private String LOCATION_NAME;
	private String GISDBASE;
	private String GISRC;

	/**
	 * Grass Import Actor. imports an arc file into the grass database.
	 */
	public GrassImportActor(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
		inFileName.setTypeEquals(BaseType.STRING);
		dataType.setTypeEquals(BaseType.STRING);
		objectName.setTypeEquals(BaseType.STRING);
		trigger.setTypeEquals(BaseType.STRING);
	}

	/**
   *
   */
	public void initialize() throws IllegalActionException {
		if (grass_gisbase.getToken() != null) {
			GRASS_GISBASE = ((StringToken) grass_gisbase.getToken()).toString();
			// get rid of the quotes
			GRASS_GISBASE = GRASS_GISBASE.substring(1,
					GRASS_GISBASE.length() - 1);
			System.out.println("GRASS_GISBASE: " + GRASS_GISBASE);
		}
		if (location_name.getToken() != null) {
			LOCATION_NAME = ((StringToken) location_name.getToken()).toString();
			// get rid of the quotes
			LOCATION_NAME = LOCATION_NAME.substring(1,
					LOCATION_NAME.length() - 1);
			System.out.println("LOCATION_NAME: " + LOCATION_NAME);
		}
		if (gisdbase.getToken() != null) {
			GISDBASE = ((StringToken) gisdbase.getToken()).toString();
			// get rid of the quotes
			GISDBASE = GISDBASE.substring(1, GISDBASE.length() - 1);
			System.out.println("GISDBASE: " + GISDBASE);
		}
		if (gisrc.getToken() != null) {
			GISRC = ((StringToken) gisrc.getToken()).toString();
			// get rid of the quotes
			GISRC = GISRC.substring(1, GISRC.length() - 1);
			System.out.println("GISRC: " + GISRC);
		}

		if (GISRC == null || GISDBASE == null || LOCATION_NAME == null
				|| GRASS_GISBASE == null) {
			throw new IllegalActionException(
					"The parameters GISRC, GISBASE, "
							+ "LOCATION_NAME and GRASS_GISBASE must have valid values.  GISRC is "
							+ "the path to your .grassrc file.  GISBASE is the path to your grass5"
							+ "installation directory.  LOCATION_NAME is the name of the database "
							+ "location within GRASS.  GRASS_GISBASE is the base directory for "
							+ "your GRASS database.  Please provide these paths and re-execute.");
		}
	}

	/**
   *
   */
	public boolean prefire() throws IllegalActionException {
		return super.prefire();
	}

	/**
   *
   */
	public void fire() throws IllegalActionException {
		System.out.println("firing GrassImportActor");
		super.fire();

		StringToken inFileToken = (StringToken) inFileName.get(0);
		String inFileStr = inFileToken.stringValue();

		StringToken dataTypeToken = (StringToken) dataType.get(0);
		String dataTypeStr = dataTypeToken.stringValue();

		StringToken objectNameToken = (StringToken) objectName.get(0);
		String objectNameTypeStr = objectNameToken.stringValue();

		String errorout = "";
		// /////////////////////////// IMPL CODE
		// //////////////////////////////////

		try {
			final Process listener;
			String importCommand = null;

			if (dataTypeStr.equalsIgnoreCase("asc")) {
				importCommand = "r.in.arc";
			} else if (dataTypeStr.equalsIgnoreCase("shp")) {
				importCommand = "r.in.shape";
			} else {
				importCommand = "r.in.arc"; // default
			}

			String args[] = { this.GRASS_GISBASE + "/bin/" + importCommand,
					"input=" + inFileStr, "output=" + objectNameTypeStr };

			String envvars[] = { "GISBASE=" + this.GRASS_GISBASE,
					"LOCATION_NAME=" + this.LOCATION_NAME,
					"GISDBASE=" + this.GISDBASE, "GISRC=" + this.GISRC };

			listener = Runtime.getRuntime().exec(args, envvars);

			/*
			 * new Thread(new Runnable() { public void run() { try {
			 * BufferedReader br_in = new BufferedReader( new
			 * InputStreamReader(listener.getInputStream())); BufferedReader
			 * br_err_in = new BufferedReader( new
			 * InputStreamReader(listener.getErrorStream())); String buff =
			 * null; while ((br_in != null && (buff = br_in.readLine()) !=
			 * null)) { System.out.println("Process out: " + buff); try
			 * {Thread.sleep(1); } catch(Exception e) {} } br_in.close(); while
			 * ((br_err_in != null && (buff = br_err_in.readLine()) != null)) {
			 * System.out.println("Process error out: " + buff); try
			 * {Thread.sleep(1); } catch(Exception e) {} } br_err_in.close(); }
			 * catch (IOException ioe) {
			 * System.out.println("Exception caught printing result");
			 * ioe.printStackTrace(); } } }).start();
			 */

			BufferedReader br_err_in = new BufferedReader(
					new InputStreamReader(listener.getErrorStream()));
			String buff = null;
			while ((br_err_in != null && (buff = br_err_in.readLine()) != null)) {
				errorout += buff + "\n";
			}
			br_err_in.close();

			System.out.println("****" + errorout + "****");

			// thread to wait for grass process to terminate
			new Thread(new Runnable() {
				public void run() {
					try {
						System.out
								.println("*Thread waiting for Process to exit");
						listener.waitFor();
						System.out.println("*Thread detected Process exiting");
					} catch (InterruptedException ie) {
						System.out
								.println("InterruptedException caught whilst waiting "
										+ "for Mozilla Process to exit: " + ie);
						ie.printStackTrace();
					}
				}
			}).start();
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalActionException("Unexpected error: "
					+ e.getMessage());
		}

		// ////////////////////////////////////////////////////////////////////////

		if (errorout.indexOf("CREATING SUPPORT FILES FOR") != -1) {
			trigger.broadcast(new StringToken("SUCCESS"));
			System.out.println("Grass Import Action done");
		} else {
			throw new IllegalActionException("There was a problem running the "
					+ "GRASS script: " + errorout);
		}
	}
}