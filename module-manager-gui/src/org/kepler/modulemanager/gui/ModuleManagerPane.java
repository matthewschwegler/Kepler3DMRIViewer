/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-09-13 16:54:53 -0700 (Thu, 13 Sep 2012) $' 
 * '$Revision: 30661 $'
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

package org.kepler.modulemanager.gui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

public class ModuleManagerPane extends JTabbedPane
{
    public ModuleManagerPane()
    {
        super();
        addTab("Current Suite", new CurrentSuitePanel());
       // addTab("Downloaded Modules", new DownloadedModulesPanel());
        addTab("Available Suites and Modules", new AvailableModulesPanel());
    }


    public static void main(String[] args)
	{
        SwingUtilities.invokeLater( new Runnable()
        {

            public void run()
            {
                JFrame frame = new JFrame("Module Manager");
                //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                frame.add(new ModuleManagerPane());
                frame.pack();
                frame.setSize(1000,800);
                frame.setLocationRelativeTo(null);
                // center the window
                frame.setVisible(true);

                // add a window listener to close the window
                frame.addWindowListener(new WindowAdapter() {
                	
                	@Override
                	public void windowClosing(WindowEvent event) {
                        // before closing, make sure the suite looks ok
                		if(CurrentSuitePanel.canExit()) {
                			System.exit(0);
                		}
                	}
                });
            }
        });



	}
}