/*
 * Copyright (C) 2007-2008  James Cook University (http://www.jcu.edu.au).
 * 
 * This program was developed as part of the ARCHER project (Australian
 * (Research Enabling Environment) funded by a Systemic Infrastructure
 * Initiative (SII) grant and supported by the Australian Department of
 * Innovation, Industry, Science and Research.

 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, provided that the above
 * copyright notice and the following two paragraphs appear in all copies
 * of this software.

 * IN NO EVENT SHALL THE JAMES COOK UNIVERSITY BE LIABLE TO ANY PARTY FOR 
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING
 *  OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE 
 * JAMES COOK UNIVERSITY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 * THE JAMES COOK UNIVERSITY SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 * PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE JAMES COOK UNIVERSITY 
 * HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 * ENHANCEMENTS, OR MODIFICATIONS.
 */

package au.edu.jcu.kepler.hydrant;

import java.awt.Color;
import java.awt.Container;
import java.awt.Image;
import java.util.HashMap;

import ptolemy.actor.lib.image.ImageDisplay;
import ptolemy.actor.lib.image.ImageDisplayInterface;
import ptolemy.data.ImageToken;
import ptolemy.data.Token;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.kernel.util.NameDuplicationException;

///////////////////////////////////////////////////////////////////
////ImageDisplayBatch

/**
<p>
ImageDisplayBatch is the implementation of the ImageDisplayInterface that saves the content for 
ptolemy.actor.lib.image.ImageDisplay actor into a file.
</p>

@author Jianwu Wang
@version $Id: DisplayBatch.java 62778 2012-01-12 04:21:43Z cxh $
@since Kepler 2.3
*/
public class ImageDisplayBatch implements ImageDisplayInterface {

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////
	
	public void display(Token in) {
        if (!(in instanceof ImageToken)) {
            throw new InternalErrorException(
                    "Input is not an ImageToken. It is: " + in);
        }
        Image image = ((ImageToken) in).asAWTImage();
        if (image != null) {
	        ReplacementManager man = ReplacementUtils.getReplacementManager(_display);
			HashMap data_map = new HashMap();
			data_map.put("name", _display.getFullName());
			data_map.put("type", "png");
			data_map.put("imageData", image);
			man.writeData(data_map);
        }
		
	}

	@Override
	public void init(ImageDisplay imageDisplay) throws IllegalActionException,
			NameDuplicationException {
		_display = imageDisplay;
		
	}

	@Override
	public Object getFrame() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getPlatformContainer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getTableau() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initializeEffigy() throws IllegalActionException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFrame(Object frame) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initWindowAndSizeProperties() throws IllegalActionException,
			NameDuplicationException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void cleanUp() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPlatformContainer(Object container) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Color getBackground() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setBackground(Color background) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object getPicture() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPicture(Object picture) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void placeContainer(Container container) {
		// TODO Auto-generated method stub
		
	}
	
    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////
	
    /** Reference to the ImageDisplay actor */
    private ImageDisplay _display;
}
