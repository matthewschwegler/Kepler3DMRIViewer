/*
 *
 *  Copyright (C) 2001 Numeric Solutions.  All Rights Reserved.
 *
 *  Permission is hereby granted, without written agreement and without license 
 *  or royalty fees, to use, copy, modify, and distribute this software and its 
 *  documentation for any purpose, provided that the above copyright notice 
 *  appears in all copies of this software.
 *  
 *  Contact information: Numeric Solutions support@numericsolutions.com, or:
 *
 *  http://www.numericsolutions.com
 *
 */

package com.numericsolutions.geomodeltools;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.StringTokenizer;
import java.util.Vector;

public class invdist_power_isosearch2d {

	private int NX = 0;
	private int NY = 0;

	/**
	 * Method that takes as input a name of a file and writes the file contents
	 * to a vector and then makes the vector and number of vector elements
	 * access to the public
	 * 
	 * @param fileName
	 *            name of the file that whose contents should be written to a
	 *            vector
	 * @return -- the contents of the file as a Vector
	 */
	private Vector StringVectorizer(String data) {
		Vector localVector = new Vector();
		try {
			BufferedReader in = new BufferedReader(new StringReader(data));
			String s;
			while ((s = in.readLine()) != null) {
				localVector.addElement(s);
			}
		} catch (Exception e) {
			System.out.println("failed: " + e.getMessage());
		}
		return (localVector);
	}

	/**
	 * Creates a Raster grid in memory with the following structure:
	 * 
	 * x, y, p, i, j
	 * 
	 * @param xmin
	 *            float -- the minumum x value
	 * @param xmax
	 *            float -- the maximum x value
	 * @param ymin
	 *            float -- the minimum y value
	 * @param ymax
	 *            float -- the maximum y value
	 * @param dx
	 *            float -- the distance between cells in the x direction
	 * @param dy
	 *            float -- the distance between cells in the y direction
	 * @param p
	 *            float -- the default grid value - this will be replaced by the
	 *            interpolation process
	 * @return Vector -- the grid vector, with the afore mentioned structure
	 */
	private Vector getTemplateGrid(float xmin, float xmax, float ymin,
			float ymax, float dx, float dy, float p) {

		int nx = (int) ((xmax - xmin) / dx); // gonna loose precision here
		int ny = (int) ((ymax - ymin) / dy);

		// set the instance vars
		this.NX = nx;
		this.NY = ny;

		Vector out = new Vector();

		for (int i = 1; i < ny + 1; i++) {
			float y = ymin + (dy * i);
			for (int ii = 1; ii < nx + 1; ii++) {
				float x = xmin + (dx * ii);
				String row = x + " " + y + " " + p + " " + ii + " " + i;
				out.addElement(row);
			}
		}
		return (out);
	}

	/**
	 * This is the interpolation function for this class, it does an Inverse
	 * Distance Weighted (IDW) Interpolation, using a user-defined weight
	 * coefficient, and serach radius.
	 * 
	 * @param gridVec
	 *            Vector -- a raster grid with an x, y, p, i, j structure
	 * 
	 * @param data
	 *            String -- the datafile
	 * 
	 * @param outFile
	 *            String -- the output file
	 * 
	 * @param weight
	 *            float -- the weight to be used for the interpolation, the
	 *            smaller this value (or closer to 1) the smoother the grid, the
	 *            larger the value the better the fit to the data. Typically,
	 *            for geological data a value of 2 is used and for biological
	 *            data 1.2 is commonly used.
	 * 
	 * @param searchRadius
	 *            float -- the lateral search direction -- if a grid cell has no
	 *            corresponding data within the search no interpolation on that
	 *            cell is done
	 * 
	 * @return Vector -- the raster grid as a vector with the structure: x, y,
	 *         value, i, j
	 */
	public Vector getInverseDistance2dGrid(Vector gridVec, String data,
			String outFile, float weight, float searchRadius) {

		// System.err.println("weight --> " + weight +
		// "\nsearch radius --> " + searchRadius);

		Vector outVec = new Vector();

		try {
			Vector dataVec = this.StringVectorizer(data);
			PrintStream out = new PrintStream(new FileOutputStream(outFile));
			float power = (weight);
			double searchradius = searchRadius;
			double point_estimate;
			int datalines = dataVec.size();
			int gridlines = gridVec.size();
			String datavals[] = new String[datalines];
			String gridvals[] = new String[gridlines];

			dataVec.toArray(datavals);
			gridVec.toArray(gridvals);

			double distvals[] = new double[datalines];

			int cnt = 0;
			while (cnt < gridlines) {
				StringTokenizer t = new StringTokenizer(gridvals[cnt], " \t");
				double cur_x = (new Double(t.nextToken())).doubleValue();
				double cur_y = (new Double(t.nextToken())).doubleValue();
				double cur_p = (new Double(t.nextToken())).doubleValue();
				int i = Integer.parseInt(t.nextToken());
				int j = Integer.parseInt(t.nextToken());

				int data_cnt = 0;
				while (data_cnt < datalines) {
					StringTokenizer tdata = new StringTokenizer(
							datavals[data_cnt], " \t");
					double cur_datax = (new Double(tdata.nextToken()))
							.doubleValue();
					double cur_datay = (new Double(tdata.nextToken()))
							.doubleValue();
					double cur_datap = (new Double(tdata.nextToken()))
							.doubleValue();

					double dy = cur_datay - cur_y;
					double dx = cur_datax - cur_x;
					if (dx == 0.0) {
						dx = 0.000001;
					}
					/*
					 * if dx, the numerator is zero make it some very small
					 * number
					 */
					if (dy == 0.0) {
						dy = 0.000001;
					}
					/* this is to keep dy from equal zero at 90 degrees */
					double dy_dx = dy / dx;
					double angle_radians = Math.atan(dy_dx);
					double angle_degrees = angle_radians / (Math.PI / 180);
					/*
					 * convert zero angle to some small number - this should /
					 * not ever happen because of the above logic if statements
					 */

					double dist = dy / (Math.sin(angle_radians));
					/* load the array with the distnces and angles */
					distvals[data_cnt] = dist;
					data_cnt++;
				}

				/*
				 * next block will calculate the weights to be used for the
				 * averaging
				 */
				int array_cnt = 0;
				double invdist_sum = 0.0;
				double weights[] = new double[datalines]; /*
															 * this is the weights
															 * array
															 */
				while (array_cnt < datalines) {
					if (distvals[array_cnt] < 0) {
						distvals[array_cnt] = distvals[array_cnt] * (-1);
					}
					if (distvals[array_cnt] <= searchradius) {
						invdist_sum = invdist_sum
								+ (1 / Math.pow(distvals[array_cnt], power));
					}
					array_cnt++;
				}

				/*
				 * next bit divides each inverse distance by the sum of the
				 * inverses
				 */
				array_cnt = 0;
				while (array_cnt < datalines) {
					/*
					 * if statemant checks to see that the values are less than
					 * the serach
					 */
					if (distvals[array_cnt] <= searchradius) {
						weights[array_cnt] = (1 / Math.pow(distvals[array_cnt],
								power))
								/ invdist_sum;
					}
					if (distvals[array_cnt] > searchradius) {
						weights[array_cnt] = 0;
					}
					array_cnt++;
				}

				/*
				 * next block multiplies the weights by the values to calculate
				 * the estimates
				 */
				array_cnt = 0;
				point_estimate = 0;
				while (array_cnt < datalines) {
					StringTokenizer datapoints = new StringTokenizer(
							datavals[array_cnt], " \t");
					String buf1 = datapoints.nextToken();
					buf1 = datapoints.nextToken();
					double cur_dataz = (new Double(datapoints.nextToken()))
							.doubleValue();
					point_estimate = point_estimate
							+ (weights[array_cnt] * cur_dataz);
					array_cnt++;
				}

				outVec.addElement(cur_x + " " + cur_y + " " + point_estimate
						+ " " + i + " " + j);
				out.println(cur_x + " " + cur_y + " " + point_estimate + " "
						+ i + " " + j);
				cnt++;
			}
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return outVec;
	}

	/**
	 * this is the main method that is used to create the arc grid file from the
	 * fileName
	 * 
	 * @param xyzFile
	 *            -- this is the file that contains the xyz values delimeted by
	 *            a space
	 */
	private StringBuffer writeArcGrid(Vector gridVec, double xmin, double xmax,
			double ymin, double ymax, int rows, int cols, double resolution,
			double nullValue) {

		StringBuffer sb = new StringBuffer();
		try {

			double x = xmin;
			double y = ymax;
			Vector z_vec = new Vector();
			Vector row_vec = new Vector();
			Vector col_vec = new Vector();
			Vector matrix = new Vector();

			// poopulate the vectors that are to be used for searching
			for (int k = 0; k < gridVec.size(); k++) {

				z_vec.addElement(getColumnToken((String) gridVec.elementAt(k),
						3));
				row_vec.addElement(getColumnToken(
						(String) gridVec.elementAt(k), 4));
				col_vec.addElement(getColumnToken(
						(String) gridVec.elementAt(k), 5));

			}

			// the header
			/**
			 * System.out.println("ncols        " + rows); // notice that these
			 * are switched System.out.println("nrows        " + cols);
			 * System.out.println("xllcorner    " + xmin);
			 * System.out.println("yllcorner    " + ymin);
			 * System.out.println("cellsize     " + resolution);
			 * System.out.println("NODATA_value	" + nullValue);
			 **/

			sb.append("ncols        " + rows + "\n"); // notice that these are
			// switched
			sb.append("nrows        " + cols + "\n");
			sb.append("xllcorner    " + xmin + "\n");
			sb.append("yllcorner    " + ymin + "\n");
			sb.append("cellsize     " + resolution + "\n");
			sb.append("NODATA_value	" + nullValue + "\n");

			String longLine = "";
			for (int i = 1; i <= cols; i++) {
				for (int j = 1; j <= rows; j++) {
					for (int k = 0; k < gridVec.size(); k++) {
						// if this is the node that we're searching for
						if (i == Integer.parseInt(((String) col_vec
								.elementAt(k)))
								&& j == Integer.parseInt(((String) row_vec
										.elementAt(k)))) {
							String z = (String) z_vec.elementAt(k);
							if (j == 1) {
								longLine = z;
							} else {
								longLine = longLine + " " + z;
							}
							break;
						}
					}
				}
				// System.out.println(longLine);
				matrix.addElement(longLine);
			}
			for (int i = 1; i <= matrix.size(); i++) {
				sb.append(matrix.elementAt(matrix.size() - i) + "\n");
				// System.out.println(matrix.elementAt(matrix.size() - i));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sb;
	}

	/**
	 * utility method that returns a column value from a string that contains
	 * multiple tokens
	 * 
	 * @param string
	 *            -- the string to tokenize
	 * @param tokenPosition
	 *            -- the location in the string to get
	 */
	public String getColumnToken(String s, int tokenPosition) {
		String token = "null";
		if (s != null) {
			// System.out.println( "not null");
			StringTokenizer t = new StringTokenizer(s.trim(), " \r\n\t");
			int i = 1;
			while (i <= tokenPosition) {
				if (t.hasMoreTokens()) {
					token = t.nextToken();
					i++;
				} else {
					token = "null";
					i++;
				}
			}
			return (token);
		} else {
			return (token);
		}
	}

	/**
	 * returns the application usage as a String
	 * 
	 * @return String -- the string describing how to use the application
	 */
	private static String getUsage() {
		StringBuffer sb = new StringBuffer();
		sb.append("\n\nusage: $java invdist_power_isosearch2d params \n");
		sb.append(" where params: ");
		sb.append(" xmin, xmax, ymin, ymax, dx, dy, null value, datafile, "
				+ "output file, weight coeficient, search radius\n\n");

		sb
				.append("[xmin] --> the minimum x location describing the output grid\n");
		sb
				.append("[xmax] --> the maximum x location describing the output grid\n");
		sb
				.append("[ymin] --> the minimum y location describing the output grid\n");
		sb
				.append("[ymax] --> the maximum y location describing the output grid\n");
		sb
				.append("[dx] --> the spacing between the grid cells in the x direction\n");
		sb
				.append("[dy] --> the spacing between the grid cells in the y direction\n");
		sb.append("[nullvalue] --> the default null value\n");

		sb
				.append("[datafile] --> the data file used for the interpolation -- this \n");
		sb.append("file needs to have a structure of: x y property like: \n\n");
		sb.append("34.94365              -119.7953333          772.8 \n"
				+ "34.94365              -119.2928333          769.8 \n"
				+ "34.9438333            -119.9943333          918.1 \n"
				+ "34.9438333            -119.6651667          671.1 \n"
				+ "34.9438333            -119.459              873.1 \n"
				+ "34.944                -119.4591667          873.2 \n"
				+ "34.9441667            -119.4101667          1064.5\n\n");

		sb
				.append("[output file] --> the outputfile, which by default has a structure \n "
						+ "of mimicing the following pattern: \n");

		sb
				.append("  >>> x, y, interpolated value, row level, column level <<<\n\n");

		sb
				.append("[weight coefficient] -->  the weight to be used for the \n"
						+ "interpolation, the smaller this value (or closer to 1) the \n"
						+ "smoother the grid, the larger the value the better the fit \n"
						+ "to the data.  Typically, for geological data a value of 2 \n"
						+ "is used and for biological data 1.2 is commonly used.\n");

		sb.append("[search radius] --> the search radius ");

		sb.append("\n\n\nNOTES\n");
		sb
				.append("** if the output file extension is either .asc or .ASC an ESRI \n "
						+ "ASCII raster will be written instead of the default grid structure");

		sb.append("\n\n\nEXAMPLES\n");

		sb
				.append("java invdist_power_isosearch2d 34.90033 35 -119.9958 -119.0037 .01 .01 -999 ../data/latLongElevation.txt.dat out.dat 1 100");
		sb
				.append("java invdist_power_isosearch2d 34.90033 35 -119.9958 -119.0037 .01 .01 -999 ../data/latLongElevation.txt.dat out.asc 1 100");

		sb.append("\n\n\nFEEDBACK\n");
		sb.append("John Harris -- harris@numericsolutions.com");

		return sb.toString();
	}

	public static void main(String[] args) {

		try {

			if (args.length != 11) {
				System.out.println(getUsage());
			} else {

				float xmin = (float) Double.parseDouble(args[0]);
				float xmax = (float) Double.parseDouble(args[1]);
				float ymin = (float) Double.parseDouble(args[2]);
				float ymax = (float) Double.parseDouble(args[3]);

				float dx = (float) Double.parseDouble(args[4]);
				float dy = (float) Double.parseDouble(args[5]);
				float nullval = (float) Double.parseDouble(args[6]);

				String datafile = args[7];
				String outFile = args[8];

				float weight = (float) Double.parseDouble(args[9]);
				float searchRadius = (float) Double.parseDouble(args[10]);

				invdist_power_isosearch2d gridder = new invdist_power_isosearch2d();

				Vector template = gridder.getTemplateGrid(xmin, xmax, ymin,
						ymax, dx, dy, nullval);
				Vector outGrid = gridder.getInverseDistance2dGrid(template,
						datafile, outFile, weight, searchRadius);

				if (outFile.endsWith(".asc") || outFile.endsWith(".ASC")) {
					StringBuffer out = gridder.writeArcGrid(outGrid, xmin,
							xmax, ymin, ymax, gridder.NX, gridder.NY, dx,
							nullval);

					PrintStream outfile = new PrintStream(new FileOutputStream(
							outFile));
					outfile.print(out.toString());

				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// execute(dataFile, gridFile, xmin, xmax, ymin, ymax, dx, dy, nullval,
	// weight, searchradius);

	public void execute(String datafile, String outFile, float xmin,
			float xmax, float ymin, float ymax, float dx, float dy,
			float nullval, float weight, float searchRadius) {

		try {

			Vector template = getTemplateGrid(xmin, xmax, ymin, ymax, dx, dy,
					nullval);
			Vector outGrid = getInverseDistance2dGrid(template, datafile,
					outFile, weight, searchRadius);

			if (outFile.endsWith(".asc") || outFile.endsWith(".ASC")) {
				StringBuffer out = writeArcGrid(outGrid, xmin, xmax, ymin,
						ymax, NX, NY, dx, nullval);

				PrintStream outfile = new PrintStream(new FileOutputStream(
						outFile));
				outfile.print(out.toString());
				outfile.close(); // closing the file after finished.
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
