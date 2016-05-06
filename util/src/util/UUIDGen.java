/*
 * Copyright (c) 2010 The Regents of the University of California.
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

/**
 * Name: 		UUIDGen.java
 * Author: 		uk-dave (http://www.uk-dave.com)
 * Date: 		23th July 2003
 * Description:	Generates random-number based UUIDs
 *              This program should really use java.security.SecureRandom to ensure that random numbers are truly random (well, as near as),
 *              but becuase this program is to be used on TINI's and SNAP's plain old java.util.Random has to be used instead.
 * Useful Links:
 *              What is a UUID: http://www.dsps.net/uuid.html
 *              UUID Spec: http://www.opengroup.org/onlinepubs/9629399/apdxa.htm
 *              Proper Java UUID Generator: http://www.doomdark.org/doomdark/proj/jug/
 */

package util;

//import java.security.SecureRandom;
import java.util.Random;

public class UUIDGen {
	private static final String hexChars = "0123456789abcdef";
	private static final byte INDEX_TYPE = 6;
	private static final byte INDEX_VARIATION = 8;
	private static final byte TYPE_RANDOM_BASED = 4;

	private Random rnd;

	/**
	 * Constructor. Instantiates the rnd object to generate random numbers.
	 */
	public UUIDGen() {
		rnd = new Random(System.currentTimeMillis());
	}

	/**
	 * Generates a random UUID and returns the String representation of it.
	 * 
	 * @return a String representing a randomly generated UUID.
	 */
	public String generateUUID() {
		// Generate 128-bit random number
		byte[] uuid = new byte[16];
		nextRandomBytes(uuid);

		// Set various bits such as type
		uuid[INDEX_TYPE] &= (byte) 0x0F;
		uuid[INDEX_TYPE] |= (byte) (TYPE_RANDOM_BASED << 4);
		uuid[INDEX_VARIATION] &= (byte) 0x3F;
		uuid[INDEX_VARIATION] |= (byte) 0x80;

		// Convert byte array into a UUID formated string
		StringBuffer b = new StringBuffer(36);
		for (int i = 0; i < 16; i++) {
			if (i == 4 || i == 6 || i == 8 || i == 10)
				b.append('-');
			int hex = uuid[i] & 0xFF;
			b.append(hexChars.charAt(hex >> 4));
			b.append(hexChars.charAt(hex & 0x0F));
		}

		// Return UUID
		return b.toString();
	}

	/**
	 * Generates random bytes and places them into a user-supplied byte array.
	 * The number of random bytes produced is equal to the length of the byte
	 * array. Nicked from java.util.Random becuase the stupid SNAP board doesn't
	 * have this method!
	 * 
	 * @param bytes
	 *            the non-null byte array in which to put the random bytes.
	 */
	private void nextRandomBytes(byte[] bytes) {
		int numRequested = bytes.length;
		int numGot = 0, rand = 0;
		while (true) {
			for (int i = 0; i < 4; i++) {
				if (numGot == numRequested)
					return;
				rand = (i == 0 ? rnd.nextInt() : rand >> 8);
				bytes[numGot++] = (byte) rand;
			}
		}
	}

	/**
	 * Main. Only here for testing purposes.
	 */
	public static void main(String[] args) {
		UUIDGen uuidgen = new UUIDGen();
		System.out.println(uuidgen.generateUUID());
	}
}