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

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;

import javax.swing.JFrame;

import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.DriverProperties;
import com.vividsolutions.jump.io.GMLInputTemplate;
import com.vividsolutions.jump.io.GMLReader;
import com.vividsolutions.jump.io.ShapefileReader;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.ui.ErrorHandler;
import com.vividsolutions.jump.workbench.ui.GUIUtil;

/**
 * Name: JumpFrame.java Purpose:Given a GML string or an ESRI shapefile name, it
 * obtains the geometric data and send them to a JUMP MapTab for display.
 * Author: Jianting Zhang Date: August, 2005
 */
public class JumpFrame extends JFrame implements ErrorHandler {

	private JumpMapTab mapTab = new JumpMapTab(this);
	static GMLReader reader = new GMLReader();

	/**
	 * Constructor for the JumpFrame object
	 * 
	 *@param title
	 *            Description of the Parameter
	 *@exception Exception
	 *                Description of the Exception
	 */
	public JumpFrame(String title) throws Exception {

		setTitle(title);
		setSize(700, 700);

		try {
			jbInit();
		} catch (Exception e) {
			e.printStackTrace();
		}

		addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				try {
					mapTab.initialize();
				} catch (Throwable t) {
					handleThrowable(t);
				}
			}
		});
	}

	/**
	 * Description of the Method
	 * 
	 *@exception Exception
	 *                Description of the Exception
	 */
	private void jbInit() throws Exception {
		/*
		 * addWindowListener(new WindowAdapter() { public void
		 * windowClosing(WindowEvent e) { System.exit(0); } });
		 */
		this.getContentPane().add(mapTab, BorderLayout.CENTER);
	}

	/**
	 * Description of the Method
	 * 
	 *@param t
	 *            Description of the Parameter
	 */
	public void handleThrowable(final Throwable t) {
		GUIUtil.handleThrowable(t, JumpFrame.this);
	}

	// All the attribute data are skipped (assuming schema is null or emtpy)
	// To allow attribute data, schema has to be in the format defined by
	// VividSolution.com.
	/**
	 * Adds a feature to the GMLLayer attribute of the JumpFrame object
	 * 
	 *@param gml
	 *            The feature to be added to the GMLLayer attribute
	 *@param schema
	 *            The feature to be added to the GMLLayer attribute
	 *@param layerName
	 *            The feature to be added to the GMLLayer attribute
	 *@exception Exception
	 *                Description of the Exception
	 */
	public void addGMLLayer(String gml, String schema, String layerName)
			throws Exception {
		WorkbenchContext context = mapTab.getWorkbenchContext();
		String s = "";
		String collectionElement = "gml:FeatureCollection";
		String featureElement = "gml:Feature";
		String geometryElement = "gml:Geometry";
		s += "<?xml version='1.0' encoding='UTF-8'?>";
		s += "<JCSGMLInputTemplate>";
		s += ("<CollectionElement>" + collectionElement + "</CollectionElement>");
		s += ("<FeatureElement>" + featureElement + "</FeatureElement>");
		s += ("<GeometryElement>" + geometryElement + "</GeometryElement>");
		if ((schema == null) || ((schema != null) && (schema.equals("")))) {
			s += "<ColumnDefinitions></ColumnDefinitions>";
		}
		// no attributes read
		else {
			s += "<ColumnDefinitions>" + schema + "</ColumnDefinitions>";
		}
		s += "</JCSGMLInputTemplate>";
		GMLInputTemplate template = new GMLInputTemplate();
		StringReader sr = new StringReader(s);
		template.load(sr);
		reader.setInputTemplate(template);
		sr = new StringReader(gml);
		FeatureCollection fc = reader.read(sr);
		// System.out.println(fc.size());
		Layer layer = context.createPlugInContext().addLayer(
				StandardCategoryNames.WORKING, layerName, fc);
		// layer.setDescription("ABCDE");
	}

	/**
	 * Adds a feature to the SHPLayer attribute of the JumpFrame object
	 * 
	 *@param fileName
	 *            The feature to be added to the SHPLayer attribute
	 *@param layerName
	 *            The feature to be added to the SHPLayer attribute
	 *@exception Exception
	 *                Description of the Exception
	 */
	public void addSHPLayer(String fileName, String layerName) throws Exception {
		WorkbenchContext context = mapTab.getWorkbenchContext();
		ShapefileReader reader = new ShapefileReader();
		FeatureCollection fc = reader.read(new DriverProperties(fileName));
		// System.out.println(fc.size());
		Layer layer = context.createPlugInContext().addLayer(
				StandardCategoryNames.WORKING, layerName, fc);
		// layer.setDescription("ABCDE");
	}

	/*
	 * private void removeAllCategories() { LayerManager layerManager=
	 * mapTab.getWorkbenchContext().getLayerManager(); for (Iterator i =
	 * layerManager.getCategories().iterator(); i.hasNext();) { Category cat =
	 * (Category) i.next(); layerManager.removeIfEmpty(cat); } }
	 */
	/**
	 * The main program for the JumpFrame class
	 * 
	 *@param args
	 *            The command line arguments
	 *@exception Exception
	 *                Description of the Exception
	 */
	public static void main(String[] args) throws Exception {
		JumpFrame fm = new JumpFrame("test");
		fm.setVisible(true);

		File f = new File("z:/ctws/miscs/gml2.txt");
		char[] s = new char[(int) f.length()];
		FileReader fr = new FileReader(f);
		fr.read(s);
		String gml = new String(s);
		// System.out.println(gml);
		fm.addGMLLayer(gml, "", "test1");
		fm.addSHPLayer("c:/temp/test.shp", "test2");
	}

}