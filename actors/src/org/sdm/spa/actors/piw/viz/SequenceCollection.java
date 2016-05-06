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

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

/**
 * @author xiaowen
 */
public class SequenceCollection {

	private Sequence[] _sequences;
	private int _lengthMinimumConsensus;

	public SequenceCollection(Sequence[] sequences) {
		this._sequences = sequences;
		this._lengthMinimumConsensus = 1;
	}

	public Sequence getSequence(int index) {
		return _sequences[index];
	}

	public int size() {
		return _sequences.length;
	}

	public void setOffset(int index, int offset) {
		Sequence seq = _sequences[index];
		_sequences[index] = new Sequence(seq.accessionNumberOriginal,
				seq.geneID, seq.alignedSequence, seq.arrTFBSs, offset);
	}

	public int getMaximumSequenceLength() {
		int intLength = 0;
		for (int i = 0; i < _sequences.length; i++) {
			Sequence seq = _sequences[i];
			int intCurrentLength = seq.alignedSequence.length() + seq.offset;
			if (intLength < intCurrentLength) {
				intLength = intCurrentLength;
			}
		}

		return intLength;
	}

	public void setMinimumConsensusLength(int length) {
		this._lengthMinimumConsensus = length;
	}

	public int getMinimumConsensusLength() {
		return _lengthMinimumConsensus;
	}

	/** Uses the sequences to construct the consensus sequence. */
	public String getConsensus() {

		// Get the minimum length of all the sequences.
		// The length of them should all be the same,
		// but this will allow differences.
		int intMinLength = -1;
		for (int i = 0; i < _sequences.length; i++) {
			Sequence seq = _sequences[i];
			int intCurrentLength = seq.alignedSequence.length() + seq.offset;
			if (-1 == intMinLength || intMinLength > intCurrentLength) {
				intMinLength = intCurrentLength;
			}
		}

		// String buffer for the consensus sequence.
		StringBuffer strConsensus = new StringBuffer();

		// Loop over the length of the sequences.
		for (int index = 0; index < intMinLength; index++) {
			Vector vecBases = new Vector();
			final int bin[] = new int[] { 0, 0, 0, 0, 0 };
			char trans[] = new char[] { 'A', 'G', 'C', 'T', '-' };
			char type[] = new char[] { 'R', 'R', 'Y', 'Y', '-' };
			int total = _sequences.length;
			char ch;

			for (int i = 0; i < _sequences.length; i++) {
				Sequence seq = _sequences[i];

				char c = '-';
				if (index >= seq.offset) {
					c = seq.alignedSequence.charAt(index - seq.offset);
				}

				Character chr = new Character(Character.toUpperCase(c));

				int id = chr.equals(new Character('A')) ? 0 : chr
						.equals(new Character('G')) ? 1 : chr
						.equals(new Character('C')) ? 2 : chr
						.equals(new Character('T')) ? 3 : 4;

				bin[id]++;
			}

			// Sort the indices by the number each contains.
			Vector vecIndices = new Vector();
			for (int i = 0; i < 5; i++) {
				vecIndices.add(new Integer(i));
			}

			Collections.sort(vecIndices, new Comparator() {
				public int compare(Object o1, Object o2) {
					Integer i1 = (Integer) o1;
					Integer i2 = (Integer) o2;
					return bin[i2.intValue()] - bin[i1.intValue()];
				}
			});

			// Convert vector to array.
			int ind[] = new int[5];
			for (int i = 0; i < 5; i++) {
				ind[i] = ((Integer) vecIndices.elementAt(i)).intValue();
			}

			// Determine the letter to reach the consensus sequence.
			if (bin[ind[0]] * 4 >= total * 3) {
				// 75% or more are all one letter.
				ch = trans[ind[0]];
			} else if (type[ind[0]] == type[ind[1]]
					&& (bin[ind[0]] + bin[ind[1]]) * 4 >= total * 3) {
				// 75% or more are the same type.
				ch = type[ind[0]];
			} else {
				ch = '-';
			}

			strConsensus.append(ch);
		}

		return strConsensus.toString();
	}
}