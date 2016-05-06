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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * <p>
 * This code is for converting an ascii raster file into a binary *.raw file.
 * The raw file is just a sequence of row x col bytes, scaled appropriately. The
 * ascii raster has several header lines, followed by one row of ascii
 * space-delimited values per line
 * </p>
 * Typical header lines:<br/>
 * ncols 720<br/>
 * nrows 360<br/>
 * xllcorner -180.0<br/>
 * yllcorner -90<br/>
 * cellsize 0.5<br/>
 * NODATA_value -9999<br/>
 * 
 * @author Dan Higgins NCEAS UC Santa Barbara
 */
public class AscToRaw extends TypedAtomicActor {

	/**
	 * An array of file names of *.ASC files that are to be used as GARP spatial
	 * layer inputs
	 */
	public TypedIOPort inputAscFilenameArrayPort;

	/**
	 * A single ASC grid file name to be converted to RAW format
	 */
	public TypedIOPort singleFilenamePort;

	/**
	 * Either an XML (*.dxl) file name (if array input) or a single RAW file
	 * name (if single ASC file input)
	 */
	public TypedIOPort outputValuesPort;

	/** used to specify the output RAW filename */
	public FileParameter outputRawFilename;

	/** used to specify the output dxl filename */
	public StringParameter dxlFilename;

	/** used to specify an EnvironmentSetId in the dxl file */
	public StringParameter EnvLayerSetIdParameter;

	/** used to specify an Environment Title in the dxl file */
	public StringParameter EnvLayerSetTitleParameter;

	/**
	 * In some cases it is not desirable to scale the input ASC file (e.g. when
	 * the ASC file already represents data that is in the range of 0-255) A
	 * 'scaleRaw' parameter is thus defined with a default value of 'true' Note
	 * that the setting of this parameter is ignored if the range of values is
	 * outside range 0.0 to 255.0
	 */
	public Parameter scaleRaw;

	private int minByte = 0;
	private int maxByte = 253;

	private boolean finished;
	private String prevAscFilename = "";
	private String prevRawFilename = "";
	private String cachedLine = "";
	private Hashtable header = null;

	// Each envLayer might have different extents and cell sizes
	// However, these must all be the same for a GARP EnvLayerSet dxl file
	// Thus we assume they all match and use the current (last) set of data
	private double xll;
	private int nr;
	private double yll;
	private int nc;
	private double csize;
	private double xmax;
	private double ymax;
	private String EnvLayerSetId = "Env Layer Set Id";
	private String EnvLayerSetTitle = "Env Layer Set Title";
	private boolean scaleFlag = true;
	private Vector layerVector;

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
	public AscToRaw(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		outputRawFilename = new FileParameter(this, "outputRawFilename");

		dxlFilename = new StringParameter(this, "dxlFilename");

		EnvLayerSetIdParameter = new StringParameter(this,
				"EnvLayerSetIdParameter");
		EnvLayerSetTitleParameter = new StringParameter(this,
				"EnvLayerSetTitleParameter");

		inputAscFilenameArrayPort = new TypedIOPort(this,
				"inputAscFilenameArrayPort", true, false);
		inputAscFilenameArrayPort.setTypeEquals(new ArrayType(BaseType.STRING));

		singleFilenamePort = new TypedIOPort(this, "singleFilenamePort", true,
				false);

		outputValuesPort = new TypedIOPort(this, "outputValuesPort", false,
				true);

		outputValuesPort.setTypeEquals(BaseType.STRING);

		scaleRaw = new Parameter(this, "scaleRaw");
		scaleRaw.setTypeEquals(BaseType.BOOLEAN);
		scaleRaw.setToken(BooleanToken.TRUE);

		layerVector = new Vector();
	}

	/**
	 * 
	 *@exception IllegalActionException
	 *                If there is no director.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		if (layerVector == null)
			layerVector = new Vector();
		String ascfilename = null;
		String ascfileshortname = null;
		String rawfilename = null;
		String rawfileparent = null;
		String res = null;

		if (inputAscFilenameArrayPort.getWidth() > 0) {
			ArrayToken token = (ArrayToken) inputAscFilenameArrayPort.get(0);
			// now iterate over all the filenames in the array
			for (int i = 0; i < token.length(); i++) {
				StringToken s_token = (StringToken) token.getElement(i);
				ascfilename = s_token.stringValue();
				if (ascfilename.indexOf("/") > -1) {
					ascfileshortname = ascfilename.substring(ascfilename
							.lastIndexOf("/") + 1, ascfilename.length());
				} else if (ascfilename.indexOf("\\") > -1) {
					ascfileshortname = ascfilename.substring(ascfilename
							.lastIndexOf("\\") + 1, ascfilename.length());
				} else {
					ascfileshortname = ascfilename;
				}
				System.out.println("ascfileshortname: " + ascfileshortname);
				String temp = outputRawFilename.stringValue();
				// automatically name the output if empty value to input +
				// '.raw'
				if (temp.trim().length() == 0) {
					temp = ascfilename;
					int dotloc = temp.lastIndexOf(".");
					temp = temp.substring(0, dotloc);
					temp = temp + ".raw";
					rawfilename = (new File(temp)).getPath();
				} else { // check to see if the output file is a dir
					File of = outputRawFilename.asFile();
					if (of.isDirectory()) {
						temp = ascfileshortname;
						int dotloc = temp.lastIndexOf(".");
						temp = temp.substring(0, dotloc);
						temp = temp + ".raw";
						rawfilename = of.getPath() + "/" + temp;
					} else {
						rawfilename = outputRawFilename.asFile().getPath();
					}
				}

				System.out.println("ascfilename: " + ascfilename);
				System.out.println("rawfilename: " + rawfilename);
				File file = new File(ascfilename);
				File rawfile = new File(rawfilename);
				rawfileparent = rawfile.getParent();
				if (file.exists()) {
					res = readScaleAndWrite(ascfilename, rawfilename);
					layerVector.addElement(res);
				}

			} // end of i-loop over tokens in array
			String temp = "";
			String dxlname = rawfileparent;
			for (int i = 0; i < layerVector.size(); i++) {
				temp = temp + "\n" + (String) layerVector.elementAt(i);
			}
			// temp = buildEnvLayerSetTag() + temp + "\n" + res +
			// "\n</EnvLayerSet>";
			// For some unknown reason, I added the last 'res' value; this
			// results in a duplicaton
			// of the last layer! DFH Aug 2005
			temp = buildEnvLayerSetTag() + temp + "\n</EnvLayerSet>";
			// temp is the content of the *.dxl file
			try {
				String fn = dxlFilename.stringValue();
				if (fn.trim().length() == 0)
					fn = "world.dxl";
				File dxlfile = new File(dxlname + "/" + fn);
				dxlname = dxlfile.getPath();
				FileWriter dxlwriter = new FileWriter(dxlfile);
				BufferedWriter bdxlwriter = new BufferedWriter(dxlwriter);
				bdxlwriter.write(temp, 0, temp.length());
				bdxlwriter.flush();
				dxlwriter.close();
			} catch (Exception ee) {
				System.out.println("Problem saving 'dxl' file ! ");
			}
			dxlname = dxlname.replace('\\', '/');
			System.out.println("dxlname: " + dxlname);
			outputValuesPort.broadcast(new StringToken(dxlname));

			layerVector = null;
		} // end of hasToken
		else { // no array connection
			if (singleFilenamePort.getWidth() > 0) {
				ascfilename = ((StringToken) singleFilenamePort.get(0))
						.stringValue();

				if (ascfilename.indexOf("/") > -1) {
					ascfileshortname = ascfilename.substring(ascfilename
							.lastIndexOf("/") + 1, ascfilename.length());
				} else if (ascfilename.indexOf("\\") > -1) {
					ascfileshortname = ascfilename.substring(ascfilename
							.lastIndexOf("\\") + 1, ascfilename.length());
				} else {
					ascfileshortname = ascfilename;
				}
				System.out.println("ascfileshortname: " + ascfileshortname);
				String temp = outputRawFilename.stringValue();
				// automatically name the output if empty value to input +
				// '.raw'
				if (temp.trim().length() == 0) {
					temp = ascfilename;
					int dotloc = temp.lastIndexOf(".");
					temp = temp.substring(0, dotloc);
					temp = temp + ".raw";
					outputRawFilename.setExpression(temp);
					rawfilename = outputRawFilename.asFile().getPath();
					rawfilename = rawfilename.replace('\\', '/');
				} else { // check to see if the output file is a dir
					File of = outputRawFilename.asFile();
					if (of.isDirectory()) {
						temp = ascfileshortname;
						int dotloc = temp.lastIndexOf(".");
						temp = temp.substring(0, dotloc);
						temp = temp + ".raw";
						rawfilename = of.getPath() + "/" + temp;
					} else {
						rawfilename = outputRawFilename.asFile().getPath();
					}
				}
			}
			System.out.println("single ascfilename: " + ascfilename);
			System.out.println("single rawfilename: " + rawfilename);
			File file = new File(ascfilename);
			File rawfile = new File(rawfilename);
			if (file.exists()) {
				res = readScaleAndWrite(ascfilename, rawfilename);
				outputValuesPort.broadcast(new StringToken(rawfilename));
			}

		}
	}

	/**
	 * Pre fire the actor. Calls the super class's prefire in case something is
	 * set there.
	 * 
	 *	 *@exception IllegalActionException
	 */
	public boolean prefire() throws IllegalActionException {
		prevAscFilename = "";
		String temp = EnvLayerSetIdParameter.stringValue();
		if (!temp.trim().equals("")) {
			EnvLayerSetId = temp;
		}
		temp = EnvLayerSetTitleParameter.stringValue();
		if (!temp.trim().equals("")) {
			EnvLayerSetTitle = temp;
		}
		return super.prefire();
	}

	private String readScaleAndWrite(String infilename, String outfilename) {
		String resString = "";
		String shortOutfilename = "";
		File in = new File(infilename);
		FileReader inReader = null;
		BufferedReader bufReader = null;
		try {
			inReader = new FileReader(in);
			bufReader = new BufferedReader(inReader);
			header = getHeaderInformation(bufReader);
		} catch (Exception ee) {
			System.out.println("Exception at main!");
		}
		// first non-header line should be in cachedLine string
		// and header values should be in header hash
		// test
		// System.out.println("header hash: "+header);
		String ndval = null;
		if (header.containsKey("NODATA_value")) {
			ndval = (String) header.get("NODATA_value");
		}
		// first get the maximum and minimum values in the raster, ignoring
		// NODATAs
		double maxval = -1.0E-99;
		double minval = 1.0E99;
		while (cachedLine != null) {
			StringTokenizer st = new StringTokenizer(cachedLine);
			while (st.hasMoreTokens()) {
				String nextToken = st.nextToken().trim();
				if (!nextToken.equals(ndval)) {
					double val = Double.parseDouble(nextToken);
					if (val > maxval)
						maxval = val;
					if (val < minval)
						minval = val;
				}
			}
			try {
				cachedLine = bufReader.readLine();
			} catch (Exception eee) {
				cachedLine = null;
			}
		}
		if ((maxval <= 255.0) && (minval >= 0.0)) {
			// ignore the scaling flag if not in the range of 0-255
			try {
				scaleFlag = ((BooleanToken) scaleRaw.getToken()).booleanValue();
			} catch (IllegalActionException e) {
				// required; just do nothing
			}
		}
		// System.out.println("max: "+maxval);
		// System.out.println("min: "+minval);
		// have read the entire asc file, one line at a time, to get max and min
		// values have not been saved to avoid very large arrays
		// thus need to read it again to scale
		try {
			bufReader.close();
			File out = new File(outfilename);
			shortOutfilename = out.getName();
			FileOutputStream outS = new FileOutputStream(out);
			inReader = new FileReader(in);
			bufReader = new BufferedReader(inReader);
			// since we have already read the header, presumably we can just
			// skip those line
			for (int i = 0; i < header.size(); i++) {
				bufReader.readLine();
			}
			cachedLine = bufReader.readLine(); // first data line
			while (cachedLine != null) {
				StringTokenizer st = new StringTokenizer(cachedLine);
				while (st.hasMoreTokens()) {
					String nextToken = st.nextToken().trim();
					if (!nextToken.equals(ndval)) {
						double val = Double.parseDouble(nextToken);
						// scale to range 1 to 254
						byte sval = scaleVal(minval, maxval, val);
						outS.write(sval);
					} else { // save as 255
						outS.write(255);
					}
				}
				try {
					cachedLine = bufReader.readLine();
				} catch (Exception eee) {
					cachedLine = null;
				}
			}
			bufReader.close();
			outS.close();
			xll = (new Double((String) header.get("xllcorner"))).doubleValue();
			nr = (new Integer((String) header.get("nrows"))).intValue();
			yll = (new Double((String) header.get("yllcorner"))).doubleValue();
			nc = (new Integer((String) header.get("ncols"))).intValue();
			csize = (new Double((String) header.get("cellsize"))).doubleValue();
			xmax = xll + nc * csize;
			ymax = yll + nr * csize;
			int lastdot = shortOutfilename.lastIndexOf(".");
			String idname = shortOutfilename;
			if (lastdot > -1) {
				idname = shortOutfilename.substring(0, lastdot);
			}
			String LayerType = "Layer";
			if (idname.equalsIgnoreCase("Mask"))
				LayerType = "Mask";
			resString = "<EnvLayer Type=\"" + LayerType + "\" " + "Id=\""
					+ idname + "\" " + "Title=\"" + idname + "\" ";
			resString = resString
					+ "MatrixType=\"RawByteMatrixInDisk\" MatrixFileName=\""
					+ shortOutfilename
					+ "\" "
					+ "ValueUnits=\"\" MapUnits=\"\" CoordSys=\"\" LayerType=\"\" "
					+ "Rows=\"" + header.get("nrows") + "\" Columns=\""
					+ header.get("ncols") + "\" " + "XMin=\""
					+ header.get("xllcorner") + "\" XMax=\"" + xmax
					+ "\" YMin=\"" + header.get("yllcorner") + "\" YMax=\""
					+ ymax + "\" CellSize=\"" + header.get("cellsize") + "\" "
					+ "MinValue=\"" + minval + "\" MaxValue=\"" + maxval
					+ "\" ValueCoef=\"" + ((maxval - minval) / 253) + "\" />";
			// System.out.println("res: "+resString);
		} catch (Exception eeee) {
			System.out.println("Error in saving *.raw file!");
		}
		return resString;
	}

	private Hashtable getHeaderInformation(BufferedReader br) {
		Hashtable headerVals = new Hashtable();
		// unsure exactly how many header lines may occur
		// but each line should have only two string tokens with the first being
		// a name
		// assume the 'name' is NOT a number
		boolean eoh = false; // eoh -> end of header
		while (!eoh) {
			try {
				cachedLine = br.readLine();
			} catch (Exception w) {
				System.out
						.println("error reading next line in getHeaderInformation!");
				eoh = true;
			}
			StringTokenizer st = new StringTokenizer(cachedLine);
			int cnt = st.countTokens(); // should be only 2
			if (cnt != 2)
				eoh = true;
			String firstToken = st.nextToken().trim();
			String secondToken = st.nextToken().trim();
			eoh = true;
			try {
				Double.parseDouble(firstToken);
			} catch (Exception e) {
				eoh = false;
			}
			if (!eoh) {
				headerVals.put(firstToken, secondToken);
			}
			if (!headerVals.containsKey("NODATA_value")) {
				headerVals.put("NODATA_value", "-9999"); // set a default
			}
		}
		return headerVals;
	}

	private byte scaleVal(double minval, double maxval, double val) {
		// return a byte scaled using minval, maxval, and minByte, maxByte
		byte res = 0;
		if (scaleFlag) {
			double scaledval = minByte + val / (maxval - minval)
					* (maxByte - minByte);
			res = (byte) Math.round(scaledval);
		} else {
			res = (byte) Math.round(val);
		}
		return res;

	}

	private String buildEnvLayerSetTag() {
		String res = "";
		res = "<EnvLayerSet Id=\"" + EnvLayerSetId + "\" Title=\""
				+ EnvLayerSetTitle + "\" Filename=\"\"" + " Rows=\"" + nr
				+ "\" Columns=\"" + nc + "\" XMin=\"" + xll + "\" XMax=\""
				+ xmax + "\" Ymin=\"" + yll + "\" Ymax=\"" + ymax
				+ "\" CellSize=\"" + csize + "\""
				+ " MapUnits=\"\" CoordSys=\"\" >";
		return res;
	}
}