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

package org.sdm.spa;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// MatchDisplay
/**
 * Show the Match Results in a tabulated form.
 * 
 * @author Zhengang Cheng, Xiaowen Xin
 * @version $Revision: 24234 $
 */

public class MatchDisplay extends TypedAtomicActor {
	TfDisplay frame = null;

	int m_intCount = 0;
	HashMap m_mapFreq = new HashMap();

	SequenceSet m_sequenceSet = new SequenceSet();

	/**
	 * Construct an actor with the given container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the actor cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public MatchDisplay(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// default tokenConsumptionRate is 1.
		input = new TypedIOPort(this, "GI", true, false);
		input.setTypeEquals(BaseType.STRING);

		context = new TypedIOPort(this, "Context", true, false);
		context.setTypeEquals(BaseType.STRING);

		// Set the icon.
		_attachText("_iconDescription", "<svg>\n"
				+ "<polygon points=\"-15,-15 15,15 15,-15 -15,15\" "
				+ "style=\"fill:white\"/>\n" + "</svg>\n");
	}

	// -- Part of the Actor

	public TypedIOPort input;
	public TypedIOPort context;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	public void initialize() throws IllegalActionException {
		super.initialize();
		// if (frame == null)
		frame = new TfDisplay();
		m_intCount = 0;
		m_mapFreq = new HashMap();
	} // end of initialize

	/**
	 * It receives GI number and the Match result in a string.
	 * 
	 * */
	public void fire() throws IllegalActionException {
		super.fire();

		String in = ((StringToken) (input.get(0))).stringValue();
		// Get the context of the result
		final String ctx = ((StringToken) (context.get(0))).stringValue();
		_debug("Process:" + in);

		// The results are in several lines.
		// Each field in a line is separated by \t
		String temp;
		StringTokenizer st1, st2;
		final Vector mn, pom, cs, ms, seq, ln, tfm, lnt, freq;
		mn = new Vector();
		pom = new Vector();
		cs = new Vector();
		ms = new Vector();
		seq = new Vector();
		ln = new Vector();
		tfm = new Vector();
		lnt = new Vector();

		st1 = new StringTokenizer(in, "\n");
		while (st1.hasMoreTokens()) {
			String line = st1.nextToken();
			// Each line will look like:
			// M00108 V$NRF2_01 980 (+) 1.000 0.922 gacGGAAGtg NRF-2
			st2 = new StringTokenizer(line, "\t");
			temp = st2.nextToken();
			ln.add(temp); // Link
			mn.add(st2.nextToken());
			pom.add(st2.nextToken());
			cs.add(st2.nextToken());
			ms.add(st2.nextToken());
			seq.add(st2.nextToken());
			tfm.add(st2.nextToken());
		}

		freq = processTfbs(mn);

		// Modify link vector to add information:
		for (int i = 0; i < ln.size(); i++) {
			lnt
					.addElement("http://www.gene-regulation.com/cgi-bin/pub/databases/transfac/getTF.cgi?AC="
							+ (String) ln.elementAt(i));
		}

		final DefaultTableModel dtm = new DefaultTableModel();

		// Create a new table instance
		dtm.addColumn("matrixName", mn);
		dtm.addColumn("link", lnt);
		dtm.addColumn("positionOfMatrix", pom);
		dtm.addColumn("coreSimilarity", cs);
		dtm.addColumn("matrixSimilarity", ms);
		dtm.addColumn("sequence", seq);
		dtm.addColumn("frequencies", freq);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				// add tables showing Transfac results and frequencies
				frame.addResult(ctx, dtm);
				frame.addResult("Frequencies", getFrequencyPerRun(mn));

				// update table showing overall frequencies
				frame.addOverallFreq("Overall", getFrequencyOverall(mn));

				// update table showing overall counts
				frame
						.addOverallCounts("Counts", m_sequenceSet.addData(mn,
								ctx));

				// play with frame size--make sure it's not too big
				if (!frame.isShowing()) {
					frame.pack();

					double width = java.awt.Toolkit.getDefaultToolkit()
							.getScreenSize().getWidth();
					double height = java.awt.Toolkit.getDefaultToolkit()
							.getScreenSize().getHeight();

					if (frame.getWidth() < width) {
						width = frame.getWidth();
					}

					if (frame.getHeight() < height) {
						height = frame.getHeight();
					}

					frame.setSize(new Dimension((int) width, (int) height));
				}
				frame.show();

			}
		});

	}

	class TfDisplay extends JFrame {
		JPanel outpanel;

		JPanel panelOverallFreq;

		JPanel panelOverallCounts;

		public JPanel addResult(String ctx, DefaultTableModel dtm) {
			JPanel cellpanel;
			JTable table;
			JScrollPane scrollPane;

			// Create a panel to hold all other components
			cellpanel = new JPanel();
			cellpanel.setLayout(new BorderLayout());
			table = new JTable(dtm);
			table.setCellSelectionEnabled(true);
			table.setAutoResizeMode(1);
			table.setColumnSelectionAllowed(true);
			table.setRowSelectionAllowed(true);
			JScrollPane cellsp = new JScrollPane(table);
			cellpanel.setLayout(new BorderLayout());
			cellpanel.add(new JLabel("Context:" + ctx), BorderLayout.NORTH);
			cellpanel.add(cellsp, BorderLayout.CENTER);
			outpanel.add(cellpanel);

			return cellpanel;
		}

		public void addOverallFreq(String ctx, DefaultTableModel dtm) {
			if (null != panelOverallFreq) {
				outpanel.remove(panelOverallFreq);
			}
			panelOverallFreq = addResult(ctx, dtm);
		}

		public void addOverallCounts(String ctx, DefaultTableModel dtm) {
			if (null != panelOverallCounts) {
				outpanel.remove(panelOverallCounts);
			}
			panelOverallCounts = addResult(ctx, dtm);
		}

		TfDisplay() {
			setTitle("Match(Transfac) Result Display");
			setBackground(Color.gray);
			outpanel = new JPanel();
			outpanel.setLayout(new BoxLayout(outpanel, BoxLayout.Y_AXIS));
			JScrollPane outsp = new JScrollPane(outpanel);
			getContentPane().add(outsp);

		}
	}

	private Vector processTfbs(Vector mn) {
		String str, str1;
		double tfCount;
		double mnCount = mn.size();
		double frequency;
		Vector uniqueTF = new Vector();
		Vector tfFrequency = new Vector();
		Vector frequencies = new Vector();
		for (int i = 0; i < mn.size(); i++) {
			int index = ((String) mn.elementAt(i)).indexOf("_");
			str = ((String) mn.elementAt(i)).substring(0, index);
			if (uniqueTF.contains(str)) {
			} else {
				uniqueTF.addElement(str);
			}
		}

		NumberFormat formatter = new DecimalFormat("0.000");

		for (int i = 0; i < uniqueTF.size(); i++) {
			tfCount = 0;
			for (int j = 0; j < mn.size(); j++) {
				if (((String) mn.elementAt(j)).startsWith(((String) uniqueTF
						.elementAt(i))
						+ "_")) {
					tfCount++;
				}
			}
			// We allow only 3 digits
			frequency = (tfCount / mnCount) * 100;

			tfFrequency.add(i, formatter.format(frequency));

		}

		for (int i = 0; i < mn.size(); i++) {
			int index = ((String) mn.elementAt(i)).indexOf("_");
			str = ((String) mn.elementAt(i)).substring(0, index);
			int freqIndex = uniqueTF.indexOf(str);
			frequencies.addElement(tfFrequency.elementAt(freqIndex) + "%");
		}

		return frequencies;
	}

	private DefaultTableModel getFrequencyPerRun(Vector tfSites) {
		// create hashmap mapping from tf binding site to frequency
		HashMap mapFrequencies = new HashMap();

		Iterator itFreq = tfSites.iterator();
		while (itFreq.hasNext()) {
			String strSite = (String) itFreq.next();
			strSite = strSite.split("_")[0];

			if (mapFrequencies.containsKey(strSite)) {
				Integer count = (Integer) mapFrequencies.get(strSite);
				mapFrequencies.put(strSite, new Integer(count.intValue() + 1));
			} else {
				mapFrequencies.put(strSite, new Integer(1));
			}
		}
		return createTableModel(mapFrequencies, tfSites.size());
	}

	private DefaultTableModel getFrequencyOverall(Vector tfSites) {
		// create a set with only unique tf binding sites
		HashSet setSites = new HashSet();
		Iterator itSites = tfSites.iterator();
		while (itSites.hasNext()) {
			String strSite = (String) itSites.next();
			setSites.add(strSite.split("_")[0]);
		}

		// update m_mapFreq
		itSites = setSites.iterator();
		while (itSites.hasNext()) {
			String strSite = (String) itSites.next();

			if (m_mapFreq.containsKey(strSite)) {
				Integer count = (Integer) m_mapFreq.get(strSite);
				m_mapFreq.put(strSite, new Integer(count.intValue() + 1));
			} else {
				m_mapFreq.put(strSite, new Integer(1));
			}
		}

		// update m_intCount
		m_intCount++;

		return createTableModel(m_mapFreq, m_intCount);
	}

	/**
	 * Create a DefaultTableModel representing the HashMap
	 * 
	 * @param map
	 *            a map from transcription factor binding sites to the number of
	 *            times they appear
	 * @param count
	 *            total number of transcription factor binding sites
	 * @return an object fit to be displayed sorted by frequency
	 */
	private DefaultTableModel createTableModel(HashMap map, int count) {
		// put the entries of the HashMap in a vector
		Vector vec = new Vector(map.entrySet());

		// sort the vector by frequency
		Collections.sort(vec, new Comparator() {
			public int compare(Object o1, Object o2) {
				if (!((o1 instanceof Map.Entry && o2 instanceof Map.Entry))) {
					throw new ClassCastException(
							"Comparator called with arguments not of Map.Entry type.");
				}

				Map.Entry entry1 = (Map.Entry) o1;
				Map.Entry entry2 = (Map.Entry) o2;

				if (!((entry1.getKey() instanceof String) && (entry2.getKey() instanceof String))) {
					throw new ClassCastException(
							"Inputs to Comparator don't have keys of String type.");
				}
				if (!((entry1.getValue() instanceof Integer) && (entry2
						.getValue() instanceof Integer))) {
					throw new ClassCastException(
							"Inputs to Comparator don't have values of Integer type.");
				}

				String key1 = (String) entry1.getKey();
				String key2 = (String) entry2.getKey();

				Integer value1 = (Integer) entry1.getValue();
				Integer value2 = (Integer) entry2.getValue();

				int comp = value1.compareTo(value2);
				if (0 != comp) {
					return -comp;
				} else {
					return key1.compareTo(key2);
				}
			}
		});

		// populate table model
		DefaultTableModel tableModel = new DefaultTableModel();
		tableModel.addColumn("MatrixName");
		tableModel.addColumn("Frequency");
		tableModel.addColumn("MatrixName");
		tableModel.addColumn("Frequency");
		tableModel.addColumn("MatrixName");
		tableModel.addColumn("Frequency");

		NumberFormat formatter = new DecimalFormat("0.000");
		int size = vec.size();
		Vector vecRow = new Vector();
		for (int i = 0; i < size; i++) {
			Map.Entry entry = (Map.Entry) vec.elementAt(i);
			String strSite = (String) entry.getKey();
			String strFreq = formatter.format(((Integer) entry.getValue())
					.intValue()
					* 100.0 / count);

			vecRow.add(strSite);
			vecRow.add(strFreq + "% (" + entry.getValue() + "/" + count + ")");

			if (i % 3 == 2) {
				tableModel.addRow(vecRow);
				vecRow = new Vector();
			}
		}
		if (vecRow.size() > 0) {
			tableModel.addRow(vecRow);
		}

		return tableModel;
	}

	/**
	 * Class to keep track of counts This class remembers the counts of tfbs'
	 * for all sequences it has processed so far.
	 */
	class SequenceSet {
		// set of strings representing transcription factor binding sites
		HashSet setTfs = new HashSet();

		// vector of Sequence objects
		Vector vecSequences = new Vector();

		/**
		 * Class to store data related to one specific sequence
		 */
		class Sequence {
			// name of the sequence
			String strName;

			// maps name of tfbs to number of times it occurred
			HashMap mapCount;
		}

		/**
		 * Create a DefaultTableModel representing all data so far
		 * 
		 * @param vecTfs
		 *            vector of tfbs'
		 * @param name
		 *            name of the sequence
		 * @return an object fit to be displayed sorted by tfbs name
		 */
		public DefaultTableModel addData(Vector vecTfs, String name) {
			// create sequence
			Sequence sequence = createSequence(vecTfs, name);

			// update member variables of this class
			vecSequences.add(sequence);
			setTfs.addAll(sequence.mapCount.keySet());

			// initialize the DefaultTableModel to be returned
			DefaultTableModel dtm = new DefaultTableModel();

			// create the left-most column
			vecTfs = new Vector(setTfs);
			Collections.sort(vecTfs);
			dtm.addColumn("TFBS", vecTfs);

			// loop over sequences
			Iterator it = vecSequences.iterator();
			while (it.hasNext()) {
				sequence = (Sequence) it.next();

				Vector vecCounts = new Vector();

				// loop over each TF
				Iterator itTf = vecTfs.iterator();
				while (itTf.hasNext()) {
					String strTf = (String) itTf.next();

					if (sequence.mapCount.containsKey(strTf)) {
						vecCounts.add(sequence.mapCount.get(strTf));
					} else {
						vecCounts.add(new Integer(0));
					}
				}

				// add a column corresponding to this sequence to dtm
				dtm.addColumn(sequence.strName, vecCounts);
			}

			return dtm;
		}

		/**
		 * Create a Sequence object
		 * 
		 * @param vecTfs
		 *            vector of tfbs'
		 * @param name
		 *            name of the sequence
		 * @return Sequence object containing representing input data
		 */
		public Sequence createSequence(Vector vecTfs, String name) {
			// create hashmap mapping from tf binding site to frequency
			HashMap mapFrequencies = new HashMap();

			// loop over tfbs'
			Iterator itFreq = vecTfs.iterator();
			while (itFreq.hasNext()) {
				String strSite = (String) itFreq.next();
				strSite = strSite.split("_")[0];

				if (mapFrequencies.containsKey(strSite)) {
					Integer count = (Integer) mapFrequencies.get(strSite);
					mapFrequencies.put(strSite, new Integer(
							count.intValue() + 1));
				} else {
					mapFrequencies.put(strSite, new Integer(1));
				}
			}

			// create sequence object and return it
			Sequence sequence = new Sequence();
			sequence.strName = name;
			sequence.mapCount = mapFrequencies;

			return sequence;
		}

	}

}

// vim: noet ts=4 sw=4