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

package org.kepler.gui;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;


/**
 * A utilities class for creating Swing UI widgets
 * 
 * @author Matthew Brooke
 * @since 27 February 2006
 */
public class WidgetFactory {

	private WidgetFactory() {
	}

	/**
	 * create a non-opaque JLabel containing the given displayText, with the
	 * minimumSize and preferredSize set to the PrefMinDims Dimension
	 * 
	 * @param displayText
	 *            String
	 * @param PrefMinDims
	 *            Dimension
	 * @return JLabel
	 */
	protected static JLabel makeJLabel(String displayText, Dimension PrefMinDims) {

		JLabel lbl = new JLabel(displayText != null ? displayText : "");
		lbl.setOpaque(false);
		setPrefMinSizes(lbl, PrefMinDims);
		return lbl;
	}

	/**
	 * create a JTextField containing the given initialValue, with the
	 * minimumSize and preferredSize set to the PrefMinDims Dimension
	 * 
	 * @param initialValue
	 *            String
	 * @param PrefMinDims
	 *            Dimension
	 * @return JLabel
	 */
	protected static JTextField makeJTextField(String initialValue,
			Dimension PrefMinDims) {
		initialValue = (initialValue != null ? initialValue : "");
		JTextField tf = new JTextField();
		setPrefMinSizes(tf, PrefMinDims);
		tf.setText(initialValue);
		return tf;
	}

	/**
	 * create a non-opaque JTextField containing the given initialValue, with
	 * the minimumSize and preferredSize set to the PrefMinDims Dimension
	 * 
	 * @param initialValue
	 *            String
	 * @param PrefMinDims
	 *            Dimension
	 * @return JLabel
	 */
	protected static JTextArea makeJTextArea(String initialValue) {
		initialValue = (initialValue != null ? initialValue : "");
		JTextArea ta = new JTextArea(initialValue);
		ta.setEditable(true);
		ta.setLineWrap(true);
		ta.setWrapStyleWord(true);

		return ta;
	}

	/**
	 * create a non-opaque JLabel containing the given displayText, with the
	 * minimumSize and preferredSize set to the PrefMinDims Dimension
	 * 
	 * @param displayText
	 *            String
	 * @param PrefMinDims
	 *            Dimension
	 * @return JLabel
	 */
	protected static Component getDefaultSpacer() {

		return Box.createRigidArea(defaultSpacerDims);
	}

	/**
	 * sets the minimum and preferred sizes to the passed Dimension
	 * 
	 * @param component
	 *            JComponent
	 * @param dims
	 *            Dimension
	 */
	protected static void setPrefMinSizes(JComponent component, Dimension dims) {
		component.setSize(dims);
		component.setMinimumSize(dims);
		component.setPreferredSize(dims);
	}

	/**
	 * sets the minimum, maximum and preferred sizes to the passed Dimension
	 * 
	 * @param component
	 *            JComponent
	 * @param dims
	 *            Dimension
	 */
	protected static void setPrefMinMaxSizes(JComponent component,
			Dimension dims) {
		setPrefMinSizes(component, dims);
		component.setMaximumSize(dims);
	}

	private static final Dimension defaultSpacerDims = StaticGUIResources
			.getDimension("general.defaultSpacer.width",
					"general.defaultSpacer.height", 5, 5);
}