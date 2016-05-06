/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-09-13 17:07:14 -0700 (Thu, 13 Sep 2012) $' 
 * '$Revision: 30662 $'
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

import java.awt.event.ActionEvent;

import javax.swing.JDialog;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.vergil.toolbox.FigureAction;


/**
 * This action shows the module manager.
 *
 * @author     David Welker
 */
public class ModuleManagerAction extends FigureAction
{
    private TableauFrame parent;

    public ModuleManagerAction(TableauFrame parent)
    {
        super("");
        this.parent = parent;
    }

    public void actionPerformed(ActionEvent e)
    {
        super.actionPerformed(e);
        boolean done = false;
        while(!done) {
            JDialog dialog = new JDialog(parent, true);
            dialog.setContentPane(new ModuleManagerPane());
            dialog.pack();
            // place the dialog in the center of the window that created it.
            dialog.setLocationRelativeTo(parent);
            dialog.setVisible(true);
            
            done = CurrentSuitePanel.canExit();
        }
    }


}