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

import java.awt.Image;
import java.util.HashMap;

import org.apache.log4j.Logger;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ImageToken;
import ptolemy.data.Token;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.kernel.util.NameDuplicationException;

public class ImageDisplayReplacement extends TypedAtomicActor {

	static Logger logger = Logger.getLogger(ImageDisplayReplacement.class.getName());

	
	public ImageDisplayReplacement(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
	    input = new TypedIOPort(this, "input", true, false);
	    input.setMultiport(true);
	    input.setTypeEquals(BaseType.GENERAL);
		// TODO Auto-generated constructor stub
	}
	

/** The input port, which is a multiport.
 */
public TypedIOPort input;	
	
    public boolean postfire() throws IllegalActionException {
        if (input.hasToken(0)) {
            final Token in = input.get(0);

            if (!(in instanceof ImageToken)) {
                throw new InternalErrorException(
                        "Input is not an ImageToken. It is: " + in);
            }
            
            Image image = ((ImageToken) in).asAWTImage();
            if (image != null) {
    	        ReplacementManager man = ReplacementUtils.getReplacementManager(this);
    			HashMap data_map = new HashMap();
    			data_map.put("name", getFullName());
    			data_map.put("type", "png");
    			data_map.put("imageData", image);
    			man.writeData(data_map);
            }

        }

        return super.postfire();
    }
    
	
}
