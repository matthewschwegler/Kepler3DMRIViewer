/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
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

package org.kepler.objectmanager.data;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.kepler.objectmanager.cache.DataCacheListener;
import org.kepler.objectmanager.cache.DataCacheObject;

/**
 * @author globus
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Generation - Code and Comments
 */
public class DataSrcProgressFrame extends JFrame implements DataCacheListener {
	protected JLabel _msgText = new JLabel();
	protected JProgressBar _progressBar = new JProgressBar(0, 100);

	/**
	 * Constructor
	 * 
	 * @param aMsg
	 *            initial message
	 */
	public DataSrcProgressFrame(String aMsg) {
		setTitle("Data Download in Progress...");
		JPanel panel = new JPanel(new BorderLayout());
		_progressBar.setValue(0);
		_progressBar.setStringPainted(false);
		_progressBar.setIndeterminate(true);
		_msgText = new JLabel(aMsg);
		panel.add(_msgText, BorderLayout.NORTH);
		panel.add(_progressBar, BorderLayout.CENTER);
		setContentPane(panel);

		setSize(new Dimension(500, 60));

		// Center on Screen
		Rectangle screenRect = getGraphicsConfiguration().getBounds();
		/*
		Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(
				getGraphicsConfiguration());

		// Make sure we don't place the demo off the screen.
		 * int centerWidth = screenRect.width < getSize().width ? screenRect.x :
		 * screenRect.x + screenRect.width / 2 - getSize().width / 2; int
		 * centerHeight = screenRect.height < getSize().height ? screenRect.y :
		 * screenRect.y + screenRect.height / 2 - getSize().height / 2;
		 * 
		 * centerHeight = centerHeight < screenInsets.top ? screenInsets.top :
		 * centerHeight;
		 * 
		 * setLocation(centerWidth, centerHeight);
		 */

		int x = screenRect.width - getSize().width;
		// int y = screenRect.height - getSize().height;
		setLocation(x, screenRect.y);
		// show();
	}

	/**
	 * Sets the message text in the frame
	 * 
	 * @param aMsg
	 */
	public void setMsg(String aMsg) {
		_msgText.setText(aMsg);
		_msgText.repaint();
	}

	// ------------------------------------------------------------------------
	// -- DataCacheListener
	// ------------------------------------------------------------------------

	public void complete(DataCacheObject aItem) {
		hide();
		_progressBar.setValue(100);
		_progressBar.setIndeterminate(false);
		// aItem.clearProgress();
	}
}