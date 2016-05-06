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

package org.sdm.spa.actors.piw.viz;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class VizApplication {

	private JFrame _mainFrame;
	private JPanel _mainPanel;
	private boolean _exitOnClose;

	private SequenceCollection _sequenceCollection;
	private SiteCollection _siteCollection;

	private OverviewPanel _overviewPanel;
	private DetailPanel _detailPanel;
	private SitesPanel _sitesPanel;

	private JSlider _sliderOverall;
	private JSlider _sliderPerSequence;

	public VizApplication(boolean exitOnClose) {
		_exitOnClose = exitOnClose;
	}

	public void show(Sequence[] sequences) {
		JFrame.setDefaultLookAndFeelDecorated(true);
		_mainFrame = new JFrame("PIW Visualization Frame");
		_mainPanel = new JPanel(new BorderLayout());
		_mainFrame.getContentPane().add(_mainPanel);
		if (_exitOnClose) {
			_mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}

		// Create sequence collection.
		this._sequenceCollection = new SequenceCollection(sequences);

		// Create site collection.
		this._siteCollection = new SiteCollection(this._sequenceCollection);

		// Create the overview panel.
		this._overviewPanel = new OverviewPanel(this._sequenceCollection,
				this._siteCollection, new ListenerViewPortMove(),
				new ListenerSequenceMove());

		// Create the detail panel.
		this._detailPanel = new DetailPanel(this._sequenceCollection,
				this._siteCollection, new ListenerViewPortSize());

		// Create the transcription factor binding sites panel.
		this._sitesPanel = new SitesPanel(this._sequenceCollection,
				this._siteCollection, new ListenerSiteChange());

		// Create the split panes.
		JSplitPane splitPaneRight = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				new JScrollPane(_overviewPanel), new JScrollPane(_detailPanel));

		JSplitPane splitPaneMain = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				new JScrollPane(_sitesPanel), splitPaneRight);

		// Create toolbar.
		JToolBar toolbar = new JToolBar();

		// Add to it the zoom combo box.
		String[] listResolutions = new String[] { "100%", "200%", "300%" };
		JComboBox comboBox = new JComboBox(listResolutions);
		comboBox.setEditable(true);
		comboBox.addActionListener(new ListenerComboBox());
		comboBox.setMaximumSize(new Dimension(50, 50));
		toolbar.add(comboBox);

		// Add spinner for minimum consensus length.
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(6, 1, 100, 1));
		_sequenceCollection.setMinimumConsensusLength(6);
		spinner.addChangeListener(new ListenerConsensusLength());
		spinner.setMaximumSize(new Dimension(100, 50));
		toolbar.add(spinner);

		// Add a separator.
		toolbar.add(new JToolBar.Separator());

		// Add the overall frequency slider.
		JLabel label = new JLabel("OF:");
		label
				.setToolTipText("Minimum frequency overall: total occurrence / total number of sites.");
		toolbar.add(label);

		_sliderOverall = new JSlider(0, (int) Math.ceil(_siteCollection
				.getMaxFrequencyOverall() * 100), 0);
		_sliderOverall.setMajorTickSpacing(5);
		_sliderOverall.setMinorTickSpacing(1);
		_sliderOverall.setSnapToTicks(true);
		_sliderOverall.setPaintTicks(true);
		_sliderOverall.setPaintTrack(true);
		_sliderOverall.setToolTipText("0");
		_sliderOverall.addChangeListener(new ListenerFrequency());
		toolbar.add(_sliderOverall);

		// Add a separator.
		toolbar.add(new JToolBar.Separator());

		// Add the per sequence frequency slider.
		label = new JLabel("SF:");
		label
				.setToolTipText("Minimum frequency per sequence: total occurrence per sequence / total number of sites per sequence.");
		toolbar.add(label);

		_sliderPerSequence = new JSlider(0, (int) Math.ceil(_siteCollection
				.getMaxFrequencyPerSequence() * 100), 0);
		_sliderPerSequence.setMajorTickSpacing(5);
		_sliderPerSequence.setMinorTickSpacing(1);
		_sliderPerSequence.setSnapToTicks(true);
		_sliderPerSequence.setPaintTicks(true);
		_sliderPerSequence.setPaintTrack(true);
		_sliderPerSequence.setToolTipText("0");
		_sliderPerSequence.addChangeListener(new ListenerFrequency());
		toolbar.add(_sliderPerSequence);

		// Add toolbar and main panel.
		_mainPanel.add(toolbar, BorderLayout.NORTH);
		_mainPanel.add(splitPaneMain, BorderLayout.CENTER);

		// Draw it all.
		this._overviewPanel.drawSequences();
		this._detailPanel.drawSequences(0);

		// Show.
		_mainFrame.setSize(600, 400);
		_mainFrame.setVisible(true);

		// Set divider sizes, which only have effect when the windows are shown.
		splitPaneRight.setDividerLocation(0.7f);
		splitPaneMain.setDividerLocation(0.3f);
	}

	private class ListenerConsensusLength implements ChangeListener {
		/**
		 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
		 */
		public void stateChanged(ChangeEvent e) {
			JSpinner spinner = (JSpinner) e.getSource();
			Integer value = (Integer) spinner.getValue();

			_sequenceCollection.setMinimumConsensusLength(value.intValue());
			_overviewPanel.drawSequences();
			_detailPanel.drawSequences();
		}
	}

	private class ListenerFrequency implements ChangeListener {
		public void stateChanged(ChangeEvent e) {
			JSlider slider = (JSlider) e.getSource();
			slider.setToolTipText("" + slider.getValue());

			_siteCollection.selectAll();
			_siteCollection
					.deselectSitesWithOverallFrequencyUnder(_sliderOverall
							.getValue() / 100.0);
			_siteCollection
					.deselectSitesWithPerSequenceFrequencyUnder(_sliderPerSequence
							.getValue() / 100.0);
			_overviewPanel.drawSequences();
			_detailPanel.drawSequences();
			_sitesPanel.repaint();
		}
	}

	private class ListenerSiteChange implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			_overviewPanel.drawSequences();
			_detailPanel.drawSequences();
		}
	}

	private class ListenerComboBox implements ActionListener {
		private int _zoomPrevious;

		public ListenerComboBox() {
			_zoomPrevious = 100;
		}

		public void actionPerformed(ActionEvent e) {
			JComboBox cb = (JComboBox) e.getSource();
			String newSelection = (String) cb.getSelectedItem();

			newSelection = newSelection.trim();

			if (newSelection.endsWith("%")) {
				newSelection = newSelection.substring(0,
						newSelection.length() - 1);
			}

			// Try to format this number.
			Integer zoom;
			try {
				zoom = new Integer(newSelection);
			} catch (NumberFormatException ex) {
				zoom = new Integer(_zoomPrevious);
			}

			newSelection += "%";
			cb.setSelectedItem(newSelection);

			// Zoom the canvas and set its preferred size.
			AffineTransform current = _overviewPanel.getCanvasPane()
					.getTransformContext().getTransform();
			current.setToIdentity();
			double factor = ((double) zoom.intValue()) / 100;
			current.scale(factor, factor);
			_overviewPanel.getCanvasPane().setTransform(current);

			// Ask the canvas to recalculate its preferred size.
			_overviewPanel.setPreferredSize();

			_mainPanel.repaint();
		}
	}

	private class ListenerViewPortSize implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			_overviewPanel.setViewPortSize(e.getID());
			_detailPanel.drawSequences();
		}
	}

	private class ListenerSequenceMove implements ActionListener {
		/**
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(ActionEvent e) {
			// Hacked up fields in ActionEvent.
			int offset = e.getID();
			int index = e.getModifiers();

			_sequenceCollection.setOffset(index, offset);
			_overviewPanel.drawSequences();
			_detailPanel.drawSequences();
		}
	}

	private class ListenerViewPortMove implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			_detailPanel.drawSequences(e.getID());
		}
	}

	/**
	 * Main function
	 */
	public static void main(String argv[]) {
		// Always invoke graphics code in the event thread.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				// Create an array of Sequence objects.
				Sequence[] sequences = new Sequence[] {
						new Sequence(
								"acc1",
								"gene1",
								"ctagggggggggggataaaaaaactactatatagagatctacccatcacc",
								new TranscriptionFactorBindingSite[] {
										new TranscriptionFactorBindingSite(
												"tfbs1", 25),
										new TranscriptionFactorBindingSite(
												"tfbs2", -5),
										new TranscriptionFactorBindingSite(
												"tfbs3", 5) }),
						new Sequence(
								"acc1",
								"gene1",
								"ctagggggggggggataaaagggggactatatagagatctacccatcacc",
								new TranscriptionFactorBindingSite[] {
										new TranscriptionFactorBindingSite(
												"tfbs1", 5),
										new TranscriptionFactorBindingSite(
												"tfbs2", -5),
										new TranscriptionFactorBindingSite(
												"tfbs3", 25) }),
						new Sequence(
								"acc1",
								"gene1",
								"ctagggggggggggataaaaaaactactatatagagatccccccatcacc",
								new TranscriptionFactorBindingSite[] {
										new TranscriptionFactorBindingSite(
												"tfbs1", 5),
										new TranscriptionFactorBindingSite(
												"tfbs2", -5),
										new TranscriptionFactorBindingSite(
												"tfbs3", 25) }) };

				VizApplication app = new VizApplication(true);
				app.show(sequences);
			}
		});
	}
}

// vim: et ts=4 sw=4