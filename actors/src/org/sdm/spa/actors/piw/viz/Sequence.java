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

import java.util.Arrays;

public class Sequence {

	/**
	 * Construct an instance with offset 0.
	 * 
	 * @see Sequence#Sequence(String, String, String,
	 *      TranscriptionFactorBindingSite[], int)
	 */
	public Sequence(String accessionNumberOriginal, String geneID,
			String alignedSequence, TranscriptionFactorBindingSite[] arrTFBSs) {

		this(accessionNumberOriginal, geneID, alignedSequence, arrTFBSs, 0);
	}

	/**
	 * Construct an instance.
	 * 
	 * @param accessionNumberOriginal
	 *            Accession number of the original sequence we submitted to
	 *            BLAST.
	 * @param geneID
	 *            Gene ID of this sequence.
	 * @param alignedSequence
	 *            Sequence returned by ClustalW with gaps.
	 * @param arrTFBSs
	 *            Transcription factor binding sites relative to this sequence
	 *            with no gaps.
	 * @param offset
	 *            How much this sequence is shifted.
	 */
	public Sequence(String accessionNumberOriginal, String geneID,
			String alignedSequence, TranscriptionFactorBindingSite[] arrTFBSs,
			int offset) {

		// Remove useless trailing hyphens.
		alignedSequence = alignedSequence.replaceFirst("-*$", "");

		this.accessionNumberOriginal = accessionNumberOriginal;
		this.geneID = geneID;
		this.alignedSequence = alignedSequence.replaceFirst("-*", "");
		this.arrTFBSs = arrTFBSs;
		this.offset = alignedSequence.length() - this.alignedSequence.length()
				+ offset;
	}

	/**
	 * @param index
	 *            Index on a sequence without gaps.
	 * @return Corresponding index on this sequence.
	 */
	public int getActualIndex(int index) {

		int countLetters = 0;

		int i = 0;
		for (; i < this.alignedSequence.length(); i++) {
			if ('-' != this.alignedSequence.charAt(i)) {
				countLetters++;
				if (index + 1 == countLetters) {
					break;
				}
			}
		}

		return i + offset;
	}

	/**
	 * Find the subsequence, filling in hyphens for gaps.
	 * 
	 * @param start
	 *            Initial index.
	 * @param length
	 *            Length of sequence to return.
	 * @return subsequence.
	 */
	public String subsequence(int start, int length) {
		System.out.println(length);
		int overlap1 = start > offset ? start : offset;
		int overlap2 = this.alignedSequence.length() + offset > start + length ? start
				+ length
				: this.alignedSequence.length() + offset;

		String str = "";
		if (overlap2 <= overlap1) {
			for (int i = 0; i < length; i++) {
				str += "-";
			}
		} else {
			for (int i = start; i < overlap1; i++) {
				str += "-";
			}
			str += this.alignedSequence.substring(overlap1 - offset, overlap2
					- offset);
			for (int i = overlap2; i < start + length; i++) {
				str += "-";
			}
		}
		System.out.println(str);
		return str;
	}

	/**
	 * Returns the number of times the given tfbs appears in this sequence.
	 */
	public int getTotalNumParticularTFBS(String tfbs) {
		int count = 0;

		for (int i = 0; i < arrTFBSs.length; i++) {
			if (arrTFBSs[i].name.equals(tfbs)) {
				count++;
			}
		}
		return count;
	}

	/**
	 * @return Length of sequence with gaps removed.
	 */
	public int lengthNoGaps() {
		return this.alignedSequence.replaceAll("-", "").length();
	}

	public String toString() {
		return super.toString();
	}

	public boolean equals(Object ob) {
		if (!(ob instanceof Sequence)) {
			return false;
		}

		Sequence seq = (Sequence) ob;

		if (!this.accessionNumberOriginal.equals(seq.accessionNumberOriginal)) {
			return false;
		} else if (!this.geneID.equals(seq.geneID)) {
			return false;
		} else if (!this.alignedSequence.equals(seq.alignedSequence)) {
			return false;
		} else if (!Arrays.equals(this.arrTFBSs, seq.arrTFBSs)) {
			return false;
		}

		return true;
	}

	public final String accessionNumberOriginal;
	public final String geneID;
	public final String alignedSequence;
	public final TranscriptionFactorBindingSite[] arrTFBSs;
	public final int offset;
}