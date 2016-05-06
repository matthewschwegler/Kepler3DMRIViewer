/* @Copyright (c) 1998-2012 The Regents of the University of California.
 All rights reserved.

 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the
 above copyright notice and the following two paragraphs appear in all
 copies of this software.

 IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.
 */

package au.edu.jcu.kepler.hydrant;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import util.ImageJActor;
import util.ImageJInterface;

///////////////////////////////////////////////////////////////////
//// ImageJBatch

/**
<p>
ImageJBatch is the implementation of the ImageJInterface that save images for ImageJ actor
to file system.
</p>

@author Jianwu Wang
@version $Id: ImageJBatch.java $
@since Kepler 2.3
*/

public class ImageJBatch implements ImageJInterface {

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

	@Override
	public void init(ImageJActor ij) throws IllegalActionException,
			NameDuplicationException {
		imageJ = ij;
		
	}


	@Override
	public void show(String figurePath, URL url) {
 		ReplacementManager man = ReplacementUtils.getReplacementManager(imageJ);
		if (url != null) {
        	System.out.println("url:"+ url);
			HashMap data_map = new HashMap();
			data_map.put("name", imageJ.getFullName());
			data_map.put("type", "urlimg");
			data_map.put("url", url);
			//System.out.println("data_map"+data_map);
			String ext = url.toString().substring(url.toString().lastIndexOf(".") + 1);
			data_map.put("format", ext.toLowerCase());			
			man.writeData(data_map);
		} else {
			File f = new File(figurePath);
			HashMap data_map = new HashMap();
			data_map.put("name", imageJ.getFullName());
			data_map.put("filename", figurePath);
			//System.out.println("data_map"+data_map);
			String ext = figurePath.substring(figurePath.lastIndexOf(".") + 1);
			data_map.put("format", ext.toLowerCase());			
			man.writeData(data_map);
        }		
	}

    ///////////////////////////////////////////////////////////////////
    ////                         private members                   ////

    /** Reference to the ImageJActor actor */
    ImageJActor imageJ;



}
