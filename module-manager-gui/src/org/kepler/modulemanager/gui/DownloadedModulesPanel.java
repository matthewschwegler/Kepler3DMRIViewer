/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: berkley $'
 * '$Date: 2010-04-27 17:12:36 -0700 (Tue, 27 Apr 2010) $' 
 * '$Revision: 24000 $'
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

import java.io.File;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.layout.GroupLayout;
import org.kepler.build.project.ProjectLocator;

/**
 * Created by David Welker.
 * Date: Oct 6, 2009
 * Time: 6:27:43 PM
 */
public class DownloadedModulesPanel extends JPanel
{
    private JLabel downloadedModulesListLabel = new JLabel("Downloaded Modules:");
    private JList downloadedModulesList = new JList();
    private JScrollPane downloadedModulesListScollPane = new JScrollPane(downloadedModulesList);


    public DownloadedModulesPanel()
    {
        super();
        initComponents();
        layoutComponents();
    }

    public void refresh()
    {
        initComponents();
    }

    private void initComponents()
    {
        DefaultListModel listModel = new DefaultListModel();

        File projectDir = ProjectLocator.getProjectDir();
        for( File file : projectDir.listFiles() )
        {
            String filename = file.getName();
            if( filename.matches("[a-zA-Z\\-]+-\\d+\\.\\d+\\.\\d+") )
            listModel.addElement(filename);
        }
        downloadedModulesList.setModel(listModel);

        downloadedModulesList.addListSelectionListener(new ListSelectionListener()
        {
            public void valueChanged(ListSelectionEvent e)
            {
                downloadedModulesList.clearSelection();   
            }
        });
    }

    private void layoutComponents()
    {
        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        layout.setAutocreateContainerGaps(true);
        layout.setAutocreateGaps(true);

        layout.setHorizontalGroup
        (
            layout.createParallelGroup()
                .add(downloadedModulesListLabel)
                .add(downloadedModulesListScollPane)
        );

        layout.setVerticalGroup
        (
            layout.createSequentialGroup()
                .add(downloadedModulesListLabel)
                .add(downloadedModulesListScollPane)
        );
    }
}