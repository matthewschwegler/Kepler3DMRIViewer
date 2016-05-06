/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
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

package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * This code is for converting an IPCC climate file into an ASC file for use as
 * a layer in GARP. IPCC data is stored in a file which has data organized by
 * months. This actor processes that data to return file(s) that are seasonal
 * (fall, winter, summer, or spring) and/or annual The 'outputPeriod' parameter
 * sets the season.
 * 
 * Minimum, maximum, or average values can be placed in the output file. The
 * type of values is set with the 'outputType' parameter
 * 
 * If the 'baseOutputFileName' parameter is left empty, the resulting output
 * file is placed in the same directory as the input IPCC file, with some text
 * added to the filename to indicate its type and period. Otherwise, the text in
 * 'baseOutputFileName' is assumed to be the base for the output file path (and
 * text indicating type and period is added). This allows the output to be sent
 * to arbitrary local locations.
 * 
 * In any case, the resulting output is an ASC grid filename.
 * 
 * @author Dan Higgins NCEAS UC Santa Barbara
 */
public class ClimateFileProcessor extends TypedAtomicActor {

	/**
	 * A string determining the output 'type' which has a value of 'minimum',
	 * 'maximum', or 'average'. These types refer to the value selected over the
	 * outputPeriod parameter
	 */
	public StringParameter outputType;

	/**
	 * A string determining the output 'period' which has a value of annual,
	 * fall, winter, summer, or spring
	 */
	public StringParameter outputPeriod;

	/**
	 * If the 'baseOutputFileName' parameter is left empty, the resulting output
	 * file is placed in the same directory as the input IPCC file, with some
	 * text added to the filename to indicate its type and period. Otherwise,
	 * the text in 'baseOutputFileName' is assumed to be the base for the output
	 * file path (and text indicating type and period is added). This allows the
	 * output to be sent to arbitrary local locations.
	 */
	public Parameter baseOutputFileName;

	private int months = 12, rows = 360, cols = 720, NODATA_value = -9999;

	/**
	 * The input port is given the file name of the IPCC data source for climate
	 * data.
	 */
	public TypedIOPort input = new TypedIOPort(this, "input", true, false);

	/**
	 * The output port is an ASC grid filename.
	 */
	public TypedIOPort output = new TypedIOPort(this, "output", false, true);

	/**
	 * constructor
	 * 
	 *@param container
	 *            The container.
	 *@param name
	 *            The name of this actor.
	 *@exception IllegalActionException
	 *                If the actor cannot be contained by the proposed
	 *                container.
	 *@exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public ClimateFileProcessor(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		outputType = new StringParameter(this, "outputType");
		outputType.setExpression("average");
		outputType.addChoice("average");
		outputType.addChoice("minimum");
		outputType.addChoice("maximum");

		outputPeriod = new StringParameter(this, "outputPeriod");
		outputPeriod.setExpression("annual");
		outputPeriod.addChoice("annual");
		outputPeriod.addChoice("winter");
		outputPeriod.addChoice("spring");
		outputPeriod.addChoice("summer");
		outputPeriod.addChoice("fall");
		outputPeriod.addChoice("jan");
		outputPeriod.addChoice("feb");
		outputPeriod.addChoice("mar");
		outputPeriod.addChoice("apr");
		outputPeriod.addChoice("may");
		outputPeriod.addChoice("jun");
		outputPeriod.addChoice("jul");
		outputPeriod.addChoice("aug");
		outputPeriod.addChoice("sep");
		outputPeriod.addChoice("oct");
		outputPeriod.addChoice("nov");
		outputPeriod.addChoice("dev");

		baseOutputFileName = new Parameter(this, "baseOutputFileName");

	}

	/**
	 * 
	 *@exception IllegalActionException
	 *                If there is no director.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		if (!input.hasToken(0))
			return;
		try {
			String inputfilename = null;
			String outputfilename = null;
			String baseoutputfilename = null;
			String temp = "";
			int width = input.getWidth();
			for (int iii = 0; iii < width; iii++) {
				inputfilename = ((StringToken) input.get(iii)).stringValue();
				// System.out.println("inputfilename: "+inputfilename);

				temp = inputfilename;
				int dotloc = temp.lastIndexOf(".");
				temp = temp.substring(0, dotloc);
				outputfilename = temp;
				// System.out.println("outputfilename: " + outputfilename);

				File file = new File(inputfilename);
				// System.out.println("file: " + file);

				if (file.exists()) {
					if (baseOutputFileName.getExpression().length() > 0) {
						baseoutputfilename = ((StringToken) baseOutputFileName
								.getToken()).stringValue().trim();
					}
					// System.out.println("baseoutputfilename: "+baseoutputfilename);
					// assume that string exists and is nonempty to use
					if ((baseoutputfilename != null)
							&& (baseoutputfilename.length() > 0)) {
						outputfilename = baseoutputfilename;
					}
					outputfilename = writeOutputFile(inputfilename,
							outputfilename);

					output.broadcast(new StringToken(outputfilename));
				} else {
					throw new IllegalActionException("Input file "
							+ inputfilename + " does not exist.");
				}
			}
		} catch (Exception eee) {
			throw new IllegalActionException("Problem Reading File" + eee);
		}
	}

	/**
	 * Post fire the actor. Return false to indicate that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 * 
	 *	 */
	public boolean postfire() throws IllegalActionException {
		return super.postfire();
	}

	/**
	 * Pre fire the actor. Calls the super class's prefire in case something is
	 * set there.
	 * 
	 *	 *@exception IllegalActionException
	 */
	public boolean prefire() throws IllegalActionException {
		return super.prefire();
	}

	private short[][][] readFile(String fn) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(fn));
		in.readLine();
		in.readLine();
		short[][][] result = new short[months][rows][cols];
		short temp;
		for (int m = 0; m < months; m++)
			for (int r = 0; r < rows; r++) {
				String line = in.readLine();
				for (int c = 0; c < cols; c++) {
					result[m][r][c] = Short.parseShort(line.substring(c * 5,
							(c + 1) * 5).trim());
				}
				// swap column data so that England is in center - DFH
				for (int c1 = 0; c1 < cols / 2; c1++) {
					temp = result[m][r][c1];
					result[m][r][c1] = result[m][r][c1 + cols / 2];
					result[m][r][c1 + cols / 2] = temp;
				}
			}
		return result;
	}

	private PrintWriter header(String fn) {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileOutputStream(fn));
		} catch (FileNotFoundException e) {
			System.out.println("Problem opening output file " + fn + ": "
					+ e.toString());
			System.exit(1);
		}
		out.println("ncols         720");
		out.println("nrows         360");
		out.println("xllcorner     -180.0");
		out.println("yllcorner     -90");
		out.println("cellsize      0.5");
		out.println("NODATA_value  -9999");
		return out;
	}

	private String writeOutputFile(String infilename, String outfilename) {
		int m, r, c;
		short[][][] data = null;
		System.out.println("Starting " + infilename);
		try {
			data = readFile(infilename);
		} catch (IOException e) {
		}
		int[][] annual = new int[rows][cols];
		int[][] winter = new int[rows][cols]; // dec-feb
		int[][] spring = new int[rows][cols]; // mar-may
		int[][] summer = new int[rows][cols]; // jun-aug
		int[][] fall = new int[rows][cols]; // sep-nov
		boolean[][] missingData = new boolean[rows][cols];
		PrintWriter out = null;
		String fileName = "";
		// output for each month in the year
		/*
		 * for (m=0; m<months; m++) { String fileName = vars[v] + "6190_l" +
		 * (m+1) + ".asc"; if (args.length>0 && !contains(args, fileName))
		 * continue; PrintWriter out = header("processed/" + fileName); for
		 * (r=0; r<rows; r++) { for (c=0; c<cols; c++) out.print(data[m][r][c] +
		 * " "); out.println(); } out.close(); }
		 */
		String outs = outputType.getExpression();
		int type = 'a';
		if (outs.equals("minumum"))
			type = 'm';
		if (outs.equals("maxumum"))
			type = 'M';
		for (m = 0; m < months; m++) {
			for (r = 0; r < rows; r++) {
				for (c = 0; c < cols; c++) {
					if (data[m][r][c] == NODATA_value)
						missingData[r][c] = true;
					switch (type) {
					case 'm':
						if (m == 0 | data[m][r][c] < annual[r][c])
							annual[r][c] = data[m][r][c];
						break;
					case 'M':
						if (m == 0 | data[m][r][c] > annual[r][c])
							annual[r][c] = data[m][r][c];
						break;
					default:
						annual[r][c] += data[m][r][c];
						break;
					}
				}
			}
		}

		// spring - mar-may
		for (m = 3; m < 6; m++) {
			for (r = 0; r < rows; r++) {
				for (c = 0; c < cols; c++) {
					if (data[m][r][c] == NODATA_value)
						missingData[r][c] = true;
					switch (type) {
					case 'm':
						if (m == 3 | data[m][r][c] < spring[r][c])
							spring[r][c] = data[m][r][c];
						break;
					case 'M':
						if (m == 3 | data[m][r][c] > spring[r][c])
							spring[r][c] = data[m][r][c];
						break;
					default:
						spring[r][c] += data[m][r][c];
						break;
					}
				}
			}
		}

		// summer - jun-aug
		for (m = 6; m < 9; m++) {
			for (r = 0; r < rows; r++) {
				for (c = 0; c < cols; c++) {
					if (data[m][r][c] == NODATA_value)
						missingData[r][c] = true;
					switch (type) {
					case 'm':
						if (m == 6 | data[m][r][c] < summer[r][c])
							summer[r][c] = data[m][r][c];
						break;
					case 'M':
						if (m == 6 | data[m][r][c] > summer[r][c])
							summer[r][c] = data[m][r][c];
						break;
					default:
						summer[r][c] += data[m][r][c];
						break;
					}
				}
			}
		}

		// fall - sep-nov
		for (m = 9; m < 11; m++) {
			for (r = 0; r < rows; r++) {
				for (c = 0; c < cols; c++) {
					if (data[m][r][c] == NODATA_value)
						missingData[r][c] = true;
					switch (type) {
					case 'm':
						if (m == 9 | data[m][r][c] < fall[r][c])
							fall[r][c] = data[m][r][c];
						break;
					case 'M':
						if (m == 9 | data[m][r][c] > fall[r][c])
							fall[r][c] = data[m][r][c];
						break;
					default:
						fall[r][c] += data[m][r][c];
						break;
					}
				}
			}
		}

		// winter - dec-feb
		for (r = 0; r < rows; r++) {
			for (c = 0; c < cols; c++) {
				winter[r][c] = data[11][r][c]; // start with values for dec
			}
		}
		for (m = 0; m < 2; m++) {
			for (r = 0; r < rows; r++) {
				for (c = 0; c < cols; c++) {
					if (data[m][r][c] == NODATA_value)
						missingData[r][c] = true;
					switch (type) {
					case 'm':
						if (data[m][r][c] < winter[r][c])
							winter[r][c] = data[m][r][c];
						break;
					case 'M':
						if (data[m][r][c] > winter[r][c])
							winter[r][c] = data[m][r][c];
						break;
					default:
						winter[r][c] += data[m][r][c];
						break;
					}
				}
			}
		}

		String outperiod = outputPeriod.getExpression();
		if (outperiod.equals("annual")) {
			fileName = outfilename + "_" + outs + "_ann.asc";
			out = header(fileName);
			for (r = 0; r < rows; r++) {
				for (c = 0; c < cols; c++) {
					annual[r][c] = (int) Math.round(annual[r][c] / 12.0);
					if (missingData[r][c])
						annual[r][c] = NODATA_value;
					out.print(annual[r][c] + " ");
				}
				out.println();
			}
			out.close();
		}

		if (outperiod.equals("winter")) {
			fileName = outfilename + "_" + outs + "_win.asc";
			out = header(fileName);
			for (r = 0; r < rows; r++) {
				for (c = 0; c < cols; c++) {
					winter[r][c] = (int) Math.round(winter[r][c] / 3.0);
					if (missingData[r][c])
						winter[r][c] = NODATA_value;
					out.print(winter[r][c] + " ");
				}
				out.println();
			}
			out.close();
		}

		if (outperiod.equals("spring")) {
			fileName = outfilename + "_" + outs + "_spr.asc";
			out = header(fileName);
			for (r = 0; r < rows; r++) {
				for (c = 0; c < cols; c++) {
					spring[r][c] = (int) Math.round(spring[r][c] / 3.0);
					if (missingData[r][c])
						spring[r][c] = NODATA_value;
					out.print(spring[r][c] + " ");
				}
				out.println();
			}
			out.close();
		}

		if (outperiod.equals("summer")) {
			fileName = outfilename + "_" + outs + "_sum.asc";
			out = header(fileName);
			for (r = 0; r < rows; r++) {
				for (c = 0; c < cols; c++) {
					summer[r][c] = (int) Math.round(summer[r][c] / 3.0);
					if (missingData[r][c])
						summer[r][c] = NODATA_value;
					out.print(summer[r][c] + " ");
				}
				out.println();
			}
			out.close();
		}

		if (outperiod.equals("fall")) {
			fileName = outfilename + "_" + outs + "_fall.asc";
			out = header(fileName);
			for (r = 0; r < rows; r++) {
				for (c = 0; c < cols; c++) {
					fall[r][c] = (int) Math.round(fall[r][c] / 12.0);
					if (missingData[r][c])
						fall[r][c] = NODATA_value;
					out.print(fall[r][c] + " ");
				}
				out.println();
			}
			out.close();
		}

		if (outperiod.equals("jan")) {
			fileName = outfilename + "_" + outs + "_jan.asc";
			out = header(fileName);
			for (r = 0; r < rows; r++) {
				for (c = 0; c < cols; c++) {
					out.print(data[0][r][c] + " ");
				}
				out.println();
			}
			out.close();
		}

		if (outperiod.equals("feb")) {
			fileName = outfilename + "_" + outs + "_feb.asc";
			out = header(fileName);
			for (r = 0; r < rows; r++) {
				for (c = 0; c < cols; c++) {
					out.print(data[1][r][c] + " ");
				}
				out.println();
			}
			out.close();
		}

		if (outperiod.equals("mar")) {
			fileName = outfilename + "_" + outs + "_mar.asc";
			out = header(fileName);
			for (r = 0; r < rows; r++) {
				for (c = 0; c < cols; c++) {
					out.print(data[2][r][c] + " ");
				}
				out.println();
			}
			out.close();
		}

		if (outperiod.equals("apr")) {
			fileName = outfilename + "_" + outs + "_apr.asc";
			out = header(fileName);
			for (r = 0; r < rows; r++) {
				for (c = 0; c < cols; c++) {
					out.print(data[3][r][c] + " ");
				}
				out.println();
			}
			out.close();
		}

		if (outperiod.equals("may")) {
			fileName = outfilename + "_" + outs + "_may.asc";
			out = header(fileName);
			for (r = 0; r < rows; r++) {
				for (c = 0; c < cols; c++) {
					out.print(data[4][r][c] + " ");
				}
				out.println();
			}
			out.close();
		}

		if (outperiod.equals("jun")) {
			fileName = outfilename + "_" + outs + "_jun.asc";
			out = header(fileName);
			for (r = 0; r < rows; r++) {
				for (c = 0; c < cols; c++) {
					out.print(data[5][r][c] + " ");
				}
				out.println();
			}
			out.close();
		}

		if (outperiod.equals("jul")) {
			fileName = outfilename + "_" + outs + "_jul.asc";
			out = header(fileName);
			for (r = 0; r < rows; r++) {
				for (c = 0; c < cols; c++) {
					out.print(data[6][r][c] + " ");
				}
				out.println();
			}
			out.close();
		}

		if (outperiod.equals("aug")) {
			fileName = outfilename + "_" + outs + "_aug.asc";
			out = header(fileName);
			for (r = 0; r < rows; r++) {
				for (c = 0; c < cols; c++) {
					out.print(data[7][r][c] + " ");
				}
				out.println();
			}
			out.close();
		}

		if (outperiod.equals("sep")) {
			fileName = outfilename + "_" + outs + "_sep.asc";
			out = header(fileName);
			for (r = 0; r < rows; r++) {
				for (c = 0; c < cols; c++) {
					out.print(data[8][r][c] + " ");
				}
				out.println();
			}
			out.close();
		}

		if (outperiod.equals("oct")) {
			fileName = outfilename + "_" + outs + "_oct.asc";
			out = header(fileName);
			for (r = 0; r < rows; r++) {
				for (c = 0; c < cols; c++) {
					out.print(data[9][r][c] + " ");
				}
				out.println();
			}
			out.close();
		}

		if (outperiod.equals("nov")) {
			fileName = outfilename + "_" + outs + "_nov.asc";
			out = header(fileName);
			for (r = 0; r < rows; r++) {
				for (c = 0; c < cols; c++) {
					out.print(data[10][r][c] + " ");
				}
				out.println();
			}
			out.close();
		}

		if (outperiod.equals("dec")) {
			fileName = outfilename + "_" + outs + "_dec.asc";
			out = header(fileName);
			for (r = 0; r < rows; r++) {
				for (c = 0; c < cols; c++) {
					out.print(data[11][r][c] + " ");
				}
				out.println();
			}
			out.close();
		}

		return fileName;
	}
}