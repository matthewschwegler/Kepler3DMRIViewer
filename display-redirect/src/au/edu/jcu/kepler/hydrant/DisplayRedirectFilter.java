/*
 * Copyright (c) 2008-2012 The Regents of the University of California.
 * All rights reserved.
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

package au.edu.jcu.kepler.hydrant;

import java.util.HashMap;

import ptolemy.actor.CompositeActor;
import ptolemy.actor.TypedCompositeActor;
import ptolemy.kernel.util.NamedObj;
import ptolemy.moml.MoMLParser;
import ptolemy.moml.filter.MoMLFilterSimple;


/**
 * A class to filter display related actors in Kepler/Ptolemy to the corresponding
 * one in hydrant package. So we can run Kepler in batch mode with display redirection
 * function.
 * 
 * @author Jianwu
 * @version $Id: DisplayRedirectFilter.java $
 */

public class DisplayRedirectFilter extends MoMLFilterSimple {

	private static HashMap _replacements = new HashMap();
	
	String outputPath;

    public DisplayRedirectFilter(String outputPath) {
    	this.outputPath = outputPath;
    }
	
//	static {
//		_replacements = new HashMap();
//		//_replacements.put("ptolemy.actor.lib.gui.Display", "au.edu.jcu.kepler.hydrant.DisplayReplacement");
////		_replacements.put("ptolemy.actor.lib.MonitorValue", "au.edu.jcu.kepler.hydrant.DisplayReplacement");
////		_replacements.put("ptolemy.actor.lib.gui.XYPlotter", "au.edu.jcu.kepler.hydrant.XYPlotterReplacement");
////		_replacements.put("ptolemy.actor.lib.gui.TimedPlotter", "au.edu.jcu.kepler.hydrant.TimedPlotterReplacement");
////		_replacements.put("util.ImageJActor", "au.edu.jcu.kepler.hydrant.ImageStorage");
////		//comment out file writer related actors for now.
////		//_replacements.put("ptolemy.actor.lib.io.LineWriter", "au.edu.jcu.kepler.hydrant.FileWriteReplacement");
////		//_replacements.put("org.geon.FileWrite", "au.edu.jcu.kepler.hydrant.FileWriteReplacement");
////		//_replacements.put("org.geon.BinaryFileWriter", "au.edu.jcu.kepler.hydrant.BinaryFileWriterReplacement");
////		_replacements.put("ptolemy.actor.lib.image.ImageDisplay", "au.edu.jcu.kepler.hydrant.ImageDisplayReplacement");
//	}
	
	public String filterAttributeValue(NamedObj container, String element,
			String attributeName, String attributeValue, String xmlFile) {
		if (attributeValue == null) {
			return null;
		} else if (_replacements.containsKey(attributeValue)) {
			MoMLParser.setModified(true);
			return (String)_replacements.get(attributeValue);
		}
		return attributeValue;
	}

	public void filterEndElement(NamedObj container, String elementName,
            StringBuffer currentCharData, String xmlFile)
			throws Exception {
        // see if this is the end of the top level workflow container
        if(container instanceof TypedCompositeActor && 
                container.getContainer() == null &&
                elementName.equals("entity")) {
				ReplacementManager man;
				man = new ReplacementManager((CompositeActor)container, "replacement-manager", "", "");
				man.setExpression(outputPath);
				man.setPersistent(false);
        }
	}
	
	public String toString() {
		return _replacements.toString();
	}

}
