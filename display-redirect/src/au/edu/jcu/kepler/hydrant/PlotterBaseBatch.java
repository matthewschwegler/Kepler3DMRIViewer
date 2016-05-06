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

import ptolemy.actor.lib.gui.PlotterBase;
import ptolemy.actor.lib.gui.PlotterBaseInterface;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.plot.PlotBoxInterface;

public class PlotterBaseBatch implements PlotterBaseInterface {
	
    /**
     * Initialize the implementation.
     * @param plotterBase the instance that created the implementation.
     */
    public void init(PlotterBase plotterBase) {
        _plotterBase = plotterBase;
    }

    /**
     * Create a new instance of the PlotBoxInterface implementation.
     * @return a new instance of the PlotBoxInterface implementation.
     */
    public PlotBoxInterface newPlot() {
        return new JFreeChartPlot(_plotterBase);
    }



	@Override
	public void cleanUp() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void bringToFront() {
		// TODO Auto-generated method stub
		
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
	public void initWindowAndSizeProperties() throws IllegalActionException,
			NameDuplicationException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void remove() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeNullContainer() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFrame(Object frame) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTableauTitle(String title) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPlatformContainer(Object container) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateSize() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateWindowAndSizeAttributes() {
		// TODO Auto-generated method stub
		
	}
	
    ///////////////////////////////////////////////////////////////////
    ////                      protected variables                 ////

    /** The base instance that created the implementation. */
    protected PlotterBase _plotterBase;
        
}
