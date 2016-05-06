/*
 * Copyright (c) 2010-2012 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-05-09 11:05:40 -0700 (Wed, 09 May 2012) $' 
 * '$Revision: 29823 $'
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

package org.kepler.gui.memory;

import java.awt.event.ActionEvent;

import javax.swing.JDialog;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.vergil.toolbox.FigureAction;

/**
 * Created by David Welker.
 * Date: 12/21/10
 * Time: 5:07 PM
 */
public class JvmMemoryDialogAction extends FigureAction
{
    private TableauFrame parent;

    public JvmMemoryDialogAction(TableauFrame parent)
    {
        super("");
        this.parent = parent;
    }

    public void actionPerformed(ActionEvent e)
    {
        super.actionPerformed(e);

        JDialog dialog = new JDialog();
        dialog.setContentPane(new JvmMemoryPanel(dialog));
        dialog.pack();
        // place the dialog in the center of the window that created it.
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
}
