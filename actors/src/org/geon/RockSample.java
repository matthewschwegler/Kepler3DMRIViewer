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

package org.geon;

import java.awt.Point;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

//////////////////////////////////////////////////////////////////////////
////RockSample
/**
 * @author Efrat Jaeger
 * @version $Id: RockSample.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 3.0.2
 */
public class RockSample {

	private static Set minerals = new TreeSet();

	static {

		minerals.add("quartz");
		minerals.add("k-feldspar");
		minerals.add("microcline");
		minerals.add("plagioclase");
		minerals.add("biotite");
		minerals.add("muscovite");
		minerals.add("amphibole");
		minerals.add("olivine");
		minerals.add("epidote");
		minerals.add("allanite");
		minerals.add("garnet");
		minerals.add("clinopyroxene");
		minerals.add("orthopyroxene");
		minerals.add("ilmenite");
		minerals.add("magnetite");
		minerals.add("opaques");
		minerals.add("zircon");
		minerals.add("apatite");
		minerals.add("carbonate");
		minerals.add("hornblende");
		minerals.add("magnesio-hornblende");
		minerals.add("paragasitic-hornblende");
	}

	private String id;
	private Map data = new TreeMap();

	// add a mineral into this record
	public void add(String att, String val) {
		att = att.toLowerCase().replaceAll("_", "-");
		if (att.equals("ssid")) {
			id = val;
			data.put(att, val);
		} else if (minerals.contains(att)) {
			if (val.equals("") || val.equals("tr")) {
				data.put(att, new Float(0));
			} else {
				data.put(att, new Float(Float.parseFloat(val)));
			}
		}
	}

	public float get(String att) {
		att = att.toLowerCase().replaceAll("_", "-");
		if (att.equals("ssid")) {
			return Float.parseFloat(id);
		} else if (!minerals.contains(att)) {
			return 0;
		} else {
			return ((Float) data.get(att)).floatValue();
		}
	}

	private float getQ() {
		return ((Float) data.get("quartz")).floatValue();
	}

	private float getA() {
		return ((Float) data.get("k-feldspar")).floatValue()
				+ ((Float) data.get("microcline")).floatValue();
	}

	private float getP() {

		return ((Float) data.get("plagioclase")).floatValue();
	}

	private float getM() {
		return ((Float) data.get("biotite")).floatValue()
				+ ((Float) data.get("muscovite")).floatValue()
				+ ((Float) data.get("amphibole")).floatValue()
				+ ((Float) data.get("olivine")).floatValue()
				+ ((Float) data.get("epidote")).floatValue()
				+ ((Float) data.get("allanite")).floatValue()
				+ ((Float) data.get("garnet")).floatValue()
				+ ((Float) data.get("clinopyroxene")).floatValue()
				+ ((Float) data.get("orthopyroxene")).floatValue()
				+ ((Float) data.get("ilmenite")).floatValue()
				+ ((Float) data.get("magnetite")).floatValue()
				+ ((Float) data.get("opaques")).floatValue()
				+ ((Float) data.get("zircon")).floatValue()
				+ ((Float) data.get("apatite")).floatValue()
				+ ((Float) data.get("carbonate")).floatValue();
	}

	public boolean isFelsic() {

		float p = getP();
		float a = getA();
		float q = getQ();
		float m = getM();

		if (m / (p + a + q + m) < 0.9) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isUltramafic() {
		return !isFelsic();
	}

	public String getId() {
		return id;
	}

	public Point getPointForGabbroOlivine(int leftPadding, int topPadding,
			int diagramWidth, int diagramHeight, int layer) {

		float X = 0, Y = 0;

		if (layer == 1) {
			float plagValue = getP();
			float quartzValue = getQ();
			float alakaliValue = getA();

			float sum = plagValue + quartzValue + alakaliValue;

			X = plagValue / sum;
			Y = quartzValue / sum;
			// alakaliValue = alakaliValue / sum;
		} else if (layer == 2) {
			// System.out.println("HERE!!!");
			float plagValue = ((Float) data.get("plagioclase")).floatValue();
			float olValue = ((Float) data.get("olivine")).floatValue();
			float cpxValue = ((Float) data.get("clinopyroxene")).floatValue();
			float opxValue = ((Float) data.get("orthopyroxene")).floatValue();

			/*
			 * System.out.println("diagramWidth: " + diagramWidth);
			 * System.out.println("plagValue: " + plagValue);
			 * System.out.println("olValue: " + olValue);
			 * System.out.println("cpxValue: " + cpxValue);
			 * System.out.println("opxValue: " + opxValue);
			 */
			float pxValue = cpxValue + opxValue;
			float sum = plagValue + olValue + pxValue;

			// System.out.println("sum: " + sum);

			Y = plagValue / sum;
			X = olValue / sum;

			// System.out.println("X: " + X + "  ,  Y:" + Y);

			// pxValue = pxValue /sum;
		}
		// calculate x and y coordinates
		int x1 = leftPadding + (int) (X * diagramWidth);
		int y1 = topPadding + diagramHeight;

		int y0 = topPadding + (int) ((1 - Y) * diagramHeight);
		int x0 = x1 + (y1 - y0) / 2;

		// System.out.println("x1 = " + x1 + " , y1 = " + y1 + " , x0 = " + x0 +
		// " , y0 = " + y0);

		return new Point(x0, y0);

	}

}