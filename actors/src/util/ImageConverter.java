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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.imageio.ImageIO;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.StringToken;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * Converts one image type to another.
 */
public class ImageConverter extends TypedAtomicActor {

	public StringParameter convertTo;
	public TypedIOPort inputimageFilename;
	public TypedIOPort outputimageFilename;

	/**
	 * constructor
	 * 
	 *@param container
	 *            The container.
	 *@param name
	 *            The name of this actor.
	 *@exception IllegalActionException
	 *                If the actor cannot be contained by the proposed
	 *                container.
	 *@exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public ImageConverter(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		convertTo = new StringParameter(this, "convertTo");
		convertTo.setExpression("PNG");
		convertTo.addChoice("JPG");
		convertTo.addChoice("GIF");
		convertTo.addChoice("PNG");
		convertTo.addChoice("RAW");
		convertTo.addChoice("BMP");
		inputimageFilename = new TypedIOPort(this, "inputimageFilename", true,
				false);
		outputimageFilename = new TypedIOPort(this, "outputimageFilename",
				false, true);
	}

	/**
	 * 
	 *@exception IllegalActionException
	 *                If there is no director.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		try {
			String filename = ((StringToken) inputimageFilename.get(0))
					.toString();
			// remove the quotes
			filename = filename.substring(1, filename.length() - 1);
			File file = new File(filename);

			if (file.exists()) {
				// read in the original image
				BufferedImage image = ImageIO.read(file.toURL());

				if (image == null) {
					throw new IllegalActionException(
							"Error reading image format.");
				} else {
					// get the type of image to convert to
					String selection = convertTo.getExpression();
					if (selection.equals("")) {
						selection = "JPG";
					}

					String outputFilename = "";
					if (filename.indexOf(".") != -1) { // if there is already an
														// extension, remove it
						outputFilename = filename.substring(0, filename
								.lastIndexOf("."));
					}

					outputFilename += "." + selection;

					File outputFile = new File(outputFilename);
					// write out the new image
					boolean found = ImageIO.write(image, selection, outputFile);

					if (!found) {
						throw new IllegalActionException(
								"Error writing image to new format.");
					} else {
						// send out the output file name
						outputimageFilename.broadcast(new StringToken(
								outputFilename));
					}
				}
			} else {
				throw new IllegalActionException("Image file " + filename
						+ " does not exist.");
			}
		} catch (MalformedURLException mur) {
			throw new IllegalActionException("Bad filename");
		} catch (IOException ioe) {
			throw new IllegalActionException("Error reading file");
		}
	}

	/**
	 * Post fire the actor. Return false to indicate that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 * 
	 *	 */
	public boolean postfire() {
		return false;
	}

	/**
	 * Pre fire the actor. Calls the super class's prefire in case something is
	 * set there.
	 * 
	 *	 *@exception IllegalActionException
	 */
	public boolean prefire() throws IllegalActionException {
		return super.prefire();
	}
}