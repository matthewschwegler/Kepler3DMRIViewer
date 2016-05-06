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
import java.io.FileReader;
import java.io.PrintStream;
import java.util.Vector;

public class GeomodelGlue {

	static {
		try {
			System.out.println("LIBPATH:  {"
					+ System.getProperty("java.library.path") + "}");
			System.loadLibrary("nsgeotoolsjniglue");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * function in the nsgrid.cpp code
	 * 
	 * @param grid
	 *            String[]
	 * @param data
	 *            String[]
	 * @param radius
	 *            float
	 * @return String[]
	 */
	public static native String[] gridDensityByArray(String grid[],
			String data[], float radius);

	public static native String[] gridDensityByArrayDimension(String data[],
			float radius, float xmin, float xmax, float ymin, float ymax,
			float dx, float dy, float pval);

	public String runGridDensityByArrayDimension(String inDataFile, double rad,
			double xmin, double xmax, double ymin, double ymax, double dx,
			double dy, double pVal) {
		try {
			BufferedReader indata = new BufferedReader(new FileReader(
					inDataFile));
			Vector testGridVec = new Vector();
			Vector testDataVec = new Vector();
			String s;
			while ((s = indata.readLine()) != null) {
				testDataVec.addElement(s);
			}

			int size = testDataVec.size();
			String d[] = new String[size];
			for (int i = 0; i < size; i++) {
				d[i] = (String) testDataVec.elementAt(i);
			}

			String out[] = GeomodelGlue.gridDensityByArrayDimension(d,
					(float) rad, (float) xmin, (float) xmax, (float) ymin,
					(float) ymax, (float) dx, (float) dy, (float) pVal);
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < out.length; i++) {
				sb.append(out[i]);
				sb.append("\n");
			}

			indata.close();
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void runGridDensityByArray(String inGridFile, String inDataFile,
			double rad) {
		try {
			BufferedReader ingrid = new BufferedReader(new FileReader(
					inGridFile));
			BufferedReader indata = new BufferedReader(new FileReader(
					inDataFile));
			Vector testGridVec = new Vector();
			Vector testDataVec = new Vector();
			String s;

			while ((s = ingrid.readLine()) != null) {
				testGridVec.addElement(s);
			}
			s = null;
			while ((s = indata.readLine()) != null) {
				testDataVec.addElement(s);
			}

			int size = testGridVec.size();
			String g[] = new String[size];
			for (int i = 0; i < size; i++) {
				g[i] = (String) testGridVec.elementAt(i);
			}

			size = testDataVec.size();
			String d[] = new String[size];
			for (int i = 0; i < size; i++) {
				d[i] = (String) testDataVec.elementAt(i);
			}

			String out[] = GeomodelGlue.gridDensityByArray(g, d, (float) rad);
			// ** TEMPORARILR WRITE THE RESULTS TO STD OUT **//
			PrintStream outfile = new PrintStream(new FileOutputStream(
					"out.ns2grid"));
			for (int i = 0; i < out.length; i++) {
				outfile.println(out[i]);
			}
			outfile.close();
			ingrid.close();
			indata.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
