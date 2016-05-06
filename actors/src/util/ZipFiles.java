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

package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.StringToken;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * Name: ZipFiles.java Purpose: The purpose of this actor is to 'zip' multiple
 * files into a single zipped archive
 * 
 * input is a string array of file names to be zipped and desired name of the
 * output zip file Zip file name will be output when finished.
 * 
 *@author Dan Higgins NCEAS UC Santa Barbara
 * 
 */

public class ZipFiles extends TypedAtomicActor {
	/**
	 * An array of file names of the files to be put into a zipped files
	 */
	public TypedIOPort zipFilenamesArray = new TypedIOPort(this,
			"zipFilenamesArray", true, false);

	/**
	 * The name of the zip file to be created
	 */
	public TypedIOPort zippedFileName = new TypedIOPort(this, "zippedFileName",
			true, false);

	/**
	 * The name of the zip file that was created; acts as an output trigger
	 */
	public TypedIOPort zippedFileResult = new TypedIOPort(this,
			"zippedFileResult", false, true);

	public ZipFiles(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		zipFilenamesArray.setTypeEquals(new ArrayType(BaseType.STRING));
		zippedFileName.setTypeEquals(BaseType.STRING);
		zippedFileResult.setTypeEquals(BaseType.STRING);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"30\" " + "style=\"fill:white\"/>\n"
				+ "<text x=\"16\" y=\"22\" "
				+ "style=\"font-size:20; fill:blue; font-family:SansSerif\">"
				+ "ZIP</text>\n" + "</svg>\n");

	}

	/**
   *
   */
	public boolean prefire() throws IllegalActionException {
		return super.prefire();
	}

	/**
   *
   */
	public void fire() throws IllegalActionException {
		String zipNameStr = "";
		FileOutputStream fos;
		super.fire();
		try {
			if (zippedFileName.getWidth() > 0) {
				zipNameStr = ((StringToken) zippedFileName.get(0))
						.stringValue();
				// System.out.println("ZipFileName: "+zipNameStr);
				File zipOutFile = new File(zipNameStr);
				fos = new FileOutputStream(zipOutFile);
				// System.out.println("FileOutputStream: "+fos);
			} else {
				System.out.println("No Zipped file name!");
				return;
			}
			if (zipFilenamesArray.getWidth() > 0) {
				ArrayToken token = (ArrayToken) zipFilenamesArray.get(0);
				ZipOutputStream zipoutputstream = new ZipOutputStream(fos);
				// Select your choice of stored (not compressed) or
				// deflated (compressed).
				zipoutputstream.setMethod(ZipOutputStream.DEFLATED);

				// now iterate over all the filenames in the array
				for (int i = 0; i < token.length(); i++) {
					StringToken s_token = (StringToken) token.getElement(i);
					String ascfilename = s_token.stringValue();
					File file = new File(ascfilename);
					// System.out.println("File being zipped: "+file);
					byte[] rgb = new byte[1000];
					int n;
					FileInputStream fileinputstream;
					// Calculate the CRC-32 value. This isn't strictly necessary
					// for deflated entries, but it doesn't hurt.

					CRC32 crc32 = new CRC32();

					fileinputstream = new FileInputStream(file);

					while ((n = fileinputstream.read(rgb)) > -1) {
						crc32.update(rgb, 0, n);
					}
					fileinputstream.close();
					// Create a zip entry.
					ZipEntry zipentry = new ZipEntry(file.getName());
					zipentry.setSize(file.length());
					zipentry.setTime(file.lastModified());
					zipentry.setCrc(crc32.getValue());
					// Add the zip entry and associated data.
					zipoutputstream.putNextEntry(zipentry);
					fileinputstream = new FileInputStream(file);

					while ((n = fileinputstream.read(rgb)) > -1) {
						zipoutputstream.write(rgb, 0, n);
					}

					fileinputstream.close();
					zipoutputstream.closeEntry();

				}
				zipoutputstream.close();
				fos.close();
			}
		} catch (Exception w) {
		}

		zippedFileResult.broadcast(new StringToken(zipNameStr));
	}

	/**
	 * Post fire the actor. Return false to indicate that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 * 
	 *	 */
	public boolean postfire() throws IllegalActionException {
		return super.postfire();
	}

}