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

import java.awt.Image;

import ptolemy.actor.lib.Transformer;
import ptolemy.data.AWTImageToken;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * This actor takes a StringToken containing the raw data representing an image
 * in a standard format (for instance JPEG or PNG) and produces an ImageToken
 * containing that image.
 * 
 * Bug: Maybe we should rename it "ImageDataToImageToken," or is that too much
 * of a mouthfull?
 * 
 * Bug: Since java is smart about character encodings, it might get creative
 * with the whole "getBytes" call. We really want the raw bytes that were used
 * to construct the string in the first place, without any kind of translation.
 * I'm not sure how to accomplish this. I wonder if StringTokens should carry
 * around information about their encoding? I guess String's probably do that
 * already. Or something.
 * 
 * @author Tobin Fricke (tobin@splorg.org), University of California
 * @Pt.ProposedRating Red (tobin)
 */

public class StringToImage extends Transformer {

	public StringToImage(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		output.setTypeEquals(BaseType.OBJECT);
		input.setTypeEquals(BaseType.STRING);
	}

	public boolean prefire() throws IllegalActionException {
		return (input.hasToken(0) && super.prefire());
	}

	public void fire() throws IllegalActionException {
		super.fire();
		String s = ((StringToken) (input.get(0))).stringValue();

		/*
		 * This will fail if the string was constructed using a different
		 * default encoding. Too bad there's no 'raw' encoding. )-:
		 */

		byte[] data = s.getBytes();
		Image image = (new javax.swing.ImageIcon(data)).getImage();
		output.broadcast(new AWTImageToken(image));
	}
}