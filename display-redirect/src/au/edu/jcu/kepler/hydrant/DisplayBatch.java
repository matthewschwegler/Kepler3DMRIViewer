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

import java.util.HashMap;

import org.apache.log4j.Logger;

import ptolemy.actor.injection.PortableContainer;
import ptolemy.actor.lib.gui.Display;
import ptolemy.actor.lib.gui.DisplayInterface;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

///////////////////////////////////////////////////////////////////
////DisplayBatch

/**
<p>
DisplayBatch is the implementation of the DisplayInterface that saves the content for 
ptolemy.actor.lib.gui.Display actor into files.
</p>

@author Jianwu Wang
@version $Id: DisplayBatch.java 62778 2012-01-12 04:21:43Z cxh $
@since Kepler 2.3
*/
public class DisplayBatch implements DisplayInterface {
	
    private StringBuffer _output;
    private String _type = "txt";
    
    /** Reference to the Display actor */
    private Display _display;
    
	static Logger logger = Logger.getLogger(DisplayBatch.class.getName());


	@Override
	public void cleanUp() {

	}

	@Override
	public void display(String tokenValue) {
		// write tokens to the output source
			//_output.append(tokenValue+"\n");
//		System.out.println(_output);
//		System.out.println("--------");
		ReplacementManager man = ReplacementUtils.getReplacementManager(_display);
		HashMap data_map = new HashMap();
//		String name = title.getExpression();
		String name = _display.getFullName();
		data_map.put("name", name);
		data_map.put("type", _type);
		data_map.put("append", true);
		data_map.put("output", tokenValue);
		man.writeData(data_map);
		_output = null;
	}

	@Override
	public Object getTextArea() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init(Display display) throws IllegalActionException,
			NameDuplicationException {
        _display = display;
        _output = new StringBuffer();
	}

	@Override
	public void openWindow() throws IllegalActionException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void place(PortableContainer container) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void remove() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setColumns(int numberOfColumns) throws IllegalActionException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setRows(int numberOfRows) throws IllegalActionException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTitle(String stringValue) throws IllegalActionException {
		// TODO Auto-generated method stub
		
	}

}
