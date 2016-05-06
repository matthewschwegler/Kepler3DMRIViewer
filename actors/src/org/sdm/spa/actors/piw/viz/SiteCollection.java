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

import java.awt.Color;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/**
 * @author xiaowen
 */
public class SiteCollection {

	private final SequenceCollection _sequenceCollection;
	private Site[] _overallSites;
	private Site[][] _individualSites;

	public SiteCollection(SequenceCollection sequenceCollection) {
		this._sequenceCollection = sequenceCollection;
		calculateSites();
	}

	public int size() {
		return _overallSites.length;
	}

	public Site getSite(int index) {
		return _overallSites[index];
	}

	public Site getSite(String name) {
		// TODO: Inefficient.
		for (int i = 0; i < _overallSites.length; i++) {
			Site site = _overallSites[i];
			if (name.equals(site.name)) {
				return site;
			}
		}
		return null;
	}

	public double getMaxFrequencyOverall() {
		double max = 0;
		for (int i = 0; i < _overallSites.length; i++) {
			Site site = _overallSites[i];
			if (site.frequency > max) {
				max = site.frequency;
			}
		}
		return max;
	}

	public double getMaxFrequencyPerSequence() {
		// FIXME: Slightly wrong, but will work for now.
		double max = 0;
		HashMap siteToMinFreq = this.getSiteToMinPerSeqFreq();
		for (int i = 0; i < _overallSites.length; i++) {
			Site site = _overallSites[i];
			double freq = ((Double) siteToMinFreq.get(site.name)).doubleValue();
			if (freq > max) {
				max = freq;
			}
		}
		return max;
	}

	public void selectAll() {
		for (int i = 0; i < _overallSites.length; i++) {
			Site site = _overallSites[i];
			site.selected = true;
		}
	}

	public void deselectSitesWithOverallFrequencyUnder(double frequency) {
		for (int i = 0; i < _overallSites.length; i++) {
			Site site = _overallSites[i];
			if (frequency > site.frequency) {
				site.selected = false;
			}
		}
	}

	public void deselectSitesWithPerSequenceFrequencyUnder(double frequency) {
		HashMap siteToMinFreq = this.getSiteToMinPerSeqFreq();
		for (int i = 0; i < _overallSites.length; i++) {
			Site site = _overallSites[i];
			if (frequency > ((Double) siteToMinFreq.get(site.name))
					.doubleValue()) {
				site.selected = false;
			}
		}
	}

	private HashMap getSiteToMinPerSeqFreq() {
		HashMap siteToMinFreq = new HashMap();
		for (int i = 0; i < _individualSites.length; i++) {
			for (int j = 0; j < _individualSites[i].length; j++) {
				Site site = _individualSites[i][j];
				if (siteToMinFreq.containsKey(site.name)) {
					Double freq = (Double) siteToMinFreq.get(site.name);
					if (freq.intValue() > site.frequency) {
						siteToMinFreq
								.put(site.name, new Double(site.frequency));
					}
				} else {
					siteToMinFreq.put(site.name, new Double(site.frequency));
				}
			}
		}

		return siteToMinFreq;
	}

	private void calculateSites() {
		HashMap siteToFreqOverall = new HashMap();
		int totalSitesOverall = 0;

		this._individualSites = new Site[_sequenceCollection.size()][];

		for (int i = 0; i < _sequenceCollection.size(); i++) {
			Sequence sequence = _sequenceCollection.getSequence(i);

			HashMap siteToFreq = new HashMap();
			int totalSites = 0;

			for (int j = 0; j < sequence.arrTFBSs.length; j++) {
				TranscriptionFactorBindingSite tfbs = sequence.arrTFBSs[j];
				addToHashMap(siteToFreq, tfbs.name);
				addToHashMap(siteToFreqOverall, tfbs.name);
				totalSites++;
				totalSitesOverall++;
			}

			_individualSites[i] = mapToSiteArray(siteToFreq, totalSites);
		}

		_overallSites = mapToSiteArray(siteToFreqOverall, totalSitesOverall);
	}

	private Site[] mapToSiteArray(HashMap map, int total) {
		Vector vecEntries = new Vector(map.entrySet());

		// Sort by frequency.
		Collections.sort(vecEntries, new Comparator() {
			public int compare(Object o1, Object o2) {
				Map.Entry e1 = (Map.Entry) o1;
				Map.Entry e2 = (Map.Entry) o2;

				int f1 = ((Integer) e1.getValue()).intValue();
				int f2 = ((Integer) e2.getValue()).intValue();
				if (f1 != f2) {
					return f2 - f1;
				}

				String s1 = (String) e1.getKey();
				String s2 = (String) e2.getKey();

				return (s1.compareTo(s2));
			}
		});

		Site[] sites = new Site[vecEntries.size()];
		int step = (int) Math.ceil(255 / Math.ceil(Math.pow(vecEntries.size(),
				1f / 3f)));
		int r = 0;
		int g = 0;
		int b = 0;
		int i = 0;

		Iterator it = vecEntries.iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();

			// Update color.
			r += step;
			if (255 < r) {
				r -= 255;
				g += step;
			}
			if (255 < g) {
				g -= 255;
				b += step;
			}

			sites[i] = new Site((String) entry.getKey(), new Color(r, g, b),
					((Integer) entry.getValue()).intValue() / (double) total,
					true);
			i++;
		}
		return sites;
	}

	private void addToHashMap(HashMap map, String name) {
		if (map.containsKey(name)) {
			Integer i = (Integer) map.get(name);
			map.put(name, new Integer(i.intValue() + 1));
		} else {
			map.put(name, new Integer(1));
		}
	}

	class Site {
		public final String name;
		public final Color color;
		public final double frequency;
		public boolean selected;

		public Site(String name, Color color, double frequency, boolean selected) {

			this.name = name;
			this.color = color;
			this.frequency = frequency;
			this.selected = selected;
		}
	}
}