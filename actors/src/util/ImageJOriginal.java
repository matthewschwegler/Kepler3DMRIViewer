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

package util;

import ij.ImageJ;
import ij.ImagePlus;

import java.net.URL;

import ptolemy.actor.gui.BrowserLauncher;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

///////////////////////////////////////////////////////////////////
//// ImageJOriginal

/**
<p>
ImageJOriginal is the implementation of the ImageJInterface that display images for ImageJ actor
using imageJ library in a seperate window.
</p>

@author Jianwu Wang
@version $Id: ImageJOriginal.java 62778 2012-01-12 04:21:43Z cxh $
@since Kepler 2.3
*/

public class ImageJOriginal implements ImageJInterface {

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

	@Override
	public void init(ImageJActor ij) throws IllegalActionException,
			NameDuplicationException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void show(String figurePath, URL url) {
        // System.out.println("firing ImageJActor");
        // Check here to see if this is a PDF file and if it is, use the Browser
        // Launcher
        // rather than ImageJ (since ImageJ will not open a PDF!
        if (figurePath.toLowerCase().endsWith(".pdf")) {
            try {
            	BrowserLauncher.openURL(url.toString());
            } catch (Exception ee) {
                System.out.println("Error trying to open PDF file!");
            }
        } else if (url != null) { 
            try {
            	BrowserLauncher.openURL(url.toString());
            } catch (Exception ee) {
                System.out.println("Error trying to open URL file: " + url.toString());
                ee.printStackTrace();
            }
        } else {

            if (ij == null) {
                if (IJMacro.ij != null) {// IJMacro may already have a static
                    // instance of an ImageJ class; if
                    // so, use it
                    ij = IJMacro.ij;
                } else if (ShowLocations.ij != null) {
                    ij = ShowLocations.ij;
                } else {
                    ij = new ImageJ();
                }
            }
            if (ij != null && !ij.isShowing()) {
                ij.show();
            }
            if (figurePath != null) {
                // System.out.println("_fileRoot: "+_fileRoot);
                new ImagePlus(figurePath).show();
            }
        } // end of pdf check
		
	}



    ///////////////////////////////////////////////////////////////////
    ////                         public members                    ////




    ///////////////////////////////////////////////////////////////////
    ////                         private members                   ////

    /** Reference to the ImageJActor actor */
    ImageJActor imageJ;
    
    public static ImageJ ij = null;



}
