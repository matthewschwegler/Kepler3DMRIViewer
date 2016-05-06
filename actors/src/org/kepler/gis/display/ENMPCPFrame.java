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

package org.kepler.gis.display;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.util.Random;
import java.util.StringTokenizer;

import javax.swing.JFrame;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import edu.psu.geovista.datamining.data.HD.BasicCell;
import edu.psu.geovista.datamining.data.HD.BasicInstance;
import edu.psu.geovista.datamining.vis.hd.HDClusterViewer_New;

/**
 * Name: ENMPCPFrame.java Purpose:Given an XML string that representing GARP
 * presaming result,it retrieves lat/lon info and the environmental data for
 * each sample and display a PCP panel for interactive explore high-dimensional
 * data. Knwon problem(s): (1) Other than x/y, the rest attributes are numbered
 * sequentially (stratring from 0). The attribute names could be obtained from
 * the workflow and passed to this file. But I found the attibute names from the
 * workflow does not aggree with the presampling results. I may need to look
 * into GARP C++ source codes. (2)The GeoVista components, on which this package
 * is built on, are still under development. The functionality of the four
 * selection modes are not clear to me. But the basic functonality is clear.
 * Author: Jianting Zhang Date: August, 2005
 */
public class ENMPCPFrame extends JFrame {
	Color[] colors = new Color[] { Color.red, Color.green, Color.blue };
	HDClusterViewer_New pcp = new HDClusterViewer_New();
	Random rd = new Random();

	/**
	 * Constructor for the ENMPCPFrame object
	 * 
	 *@param title
	 *            Description of the Parameter
	 *@exception Exception
	 *                Description of the Exception
	 */
	public ENMPCPFrame(String title) throws Exception {
		setTitle(title);
		setSize(700, 700);
		this.getContentPane().add(pcp);

	}

	/**
	 * Sets the data attribute of the ENMPCPFrame object
	 * 
	 *@param sampleString
	 *            The new data value
	 *@exception Exception
	 *                Description of the Exception
	 */
	public void setData(String sampleString) throws Exception {

		DOMParser parser = new DOMParser();
		InputStream is = new ByteArrayInputStream(sampleString.getBytes());
		parser.parse(new InputSource(is));
		Document d = parser.getDocument();
		NodeList rnl = d.getElementsByTagName("EnvCellSet");
		NodeList fnl = rnl.item(0).getChildNodes();
		int len = fnl.getLength();
		int numCells = 0;
		int num_attr = -1;
		for (int i = 0; i < len; i++) {
			Node n = fnl.item(i);
			/*
			 * System.out.println(pn.getClass().getName());
			 * System.out.println("name="+pn.getNodeName());
			 * System.out.println("attr="+pn.getAttributes());
			 */
			if (!n.getNodeName().equalsIgnoreCase("EnvCell")) {
				continue;
			}
			Element e = (Element) n;
			String x = e.getAttribute("X");
			String y = e.getAttribute("Y");
			String vals = e.getFirstChild().getNodeValue();
			StringTokenizer st = new StringTokenizer(vals, " ,");
			if (num_attr < 0) {
				num_attr = st.countTokens();
			} else {
				assert (num_attr == st.countTokens());
			}
			// System.out.println(x+" "+y+" "+vals);
			numCells++;
		}
		System.out.println(numCells + "  " + num_attr);

		int dims[] = new int[num_attr + 2];
		dims[0] = 0;
		dims[1] = 1;
		String[] names = new String[num_attr + 2];
		names[0] = "X";
		names[1] = "Y";
		for (int i = 0; i < num_attr; i++) {
			dims[i + 2] = i + 2;
			names[i + 2] = "a" + i;
		}

		int k = 0;
		int num_groups = 50;
		int observ_pos = 0;
		BasicCell[] dataBasicCells = new BasicCell[num_groups];
		for (int i = 0; i < num_groups; i++) {
			dataBasicCells[i] = new BasicCell(i, dims, null, true);
		}
		for (int i = 0; i < len; i++) {
			Node n = fnl.item(i);
			if (!n.getNodeName().equalsIgnoreCase("EnvCell")) {
				continue;
			}
			float[] values = new float[num_attr + 2];
			Element e = (Element) n;
			String x = e.getAttribute("X");
			values[0] = (new Float(x)).floatValue();
			String y = e.getAttribute("Y");
			values[1] = (new Float(y)).floatValue();
			String vals = e.getFirstChild().getNodeValue();
			StringTokenizer st = new StringTokenizer(vals, " ,");
			for (int j = 0; j < num_attr; j++) {
				values[j + 2] = (new Float(st.nextToken())).floatValue();
				// +rd.nextInt(100)*0.02f;
			}
			/*
			 * for(int j=0;j<num_attr+1;j++) System.out.print(values[j]+" ");
			 * System.out.println();
			 */
			BasicInstance dcell = new BasicInstance(numCells - 1, k, values,
					values);
			int group_no = (int) (values[observ_pos + 2]) * 25 + k % 25;
			// System.out.println(k+"  "+p+"  "+group_no);
			dataBasicCells[group_no].addInstance(dcell);
			k++;
			assert (k <= numCells);
		}
		for (int i = 0; i < 25; i++) {
			dataBasicCells[i].setHDColor(colors[0]);
			dataBasicCells[25 + i].setHDColor(colors[1]);
		}

		pcp.setAttributeNames(names);
		pcp.setCells(dataBasicCells);
	}

	/**
	 * The main program for the ENMPCPFrame class
	 * 
	 *@param args
	 *            The command line arguments
	 *@exception Exception
	 *                Description of the Exception
	 */
	public static void main(String[] args) throws Exception {
		ENMPCPFrame fm = new ENMPCPFrame("test");
		fm.setVisible(true);

		File f = new File("z:/GISVis/miscs/CellSet.xml");
		char[] s = new char[(int) f.length()];
		FileReader fr = new FileReader(f);
		fr.read(s);
		String sampleString = new String(s);
		// System.out.println(sampleString);
		fm.setData(sampleString);
	}
}