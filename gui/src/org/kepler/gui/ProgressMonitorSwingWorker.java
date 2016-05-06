/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: brooks $'
 * '$Date: 2012-08-06 22:12:26 -0700 (Mon, 06 Aug 2012) $' 
 * '$Revision: 30363 $'
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

package org.kepler.gui;

import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

public class ProgressMonitorSwingWorker extends SwingWorker {
    Frame parent;
    JFrame controllingFrame;
    String message;

    /**
     * Create a ProgressMonitorSwingWorker.
     * @param message The message to be displayed by the progress monitor.
     */
    public ProgressMonitorSwingWorker(String message) {
        super();
        this.parent = parent;
        this.message = message;
    }

    /**
     * Construct the frame.
     */
    public Object doInBackground() {
        ProgressMonitorPanel contentFrame = new ProgressMonitorPanel(message);
        controllingFrame = new JFrame("Progress");
        controllingFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Create and set up the content pane.
        contentFrame.setOpaque(true); // content panes must be opaque
        controllingFrame.setContentPane(contentFrame);

        // Display the window.
        controllingFrame.pack();
        controllingFrame.setLocationRelativeTo(null); // stay in the center
        controllingFrame.setVisible(true);
        return null;
    }

    /**
     * Remove the frame.
     */
    public void done() {
        controllingFrame.setVisible(false);
        controllingFrame.dispose();
    }

    private class ProgressMonitorPanel extends JPanel {
        JLabel explanationLabel;
        JProgressBar progBar = new JProgressBar();

        /**
         * Create a simple Progress monitor panel.
         */
        public ProgressMonitorPanel(String message) {
            super();
            explanationLabel = new JLabel(message);
            progBar.setIndeterminate(true);
            add(explanationLabel);
            add(progBar);
            setSize(new Dimension(400, 200));
	}
    }
}
