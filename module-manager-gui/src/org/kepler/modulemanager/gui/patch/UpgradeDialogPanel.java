/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jianwu $'
 * '$Date: 2012-09-12 16:25:08 -0700 (Wed, 12 Sep 2012) $' 
 * '$Revision: 30650 $'
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

package org.kepler.modulemanager.gui.patch;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.jdesktop.layout.GroupLayout;
import org.jdesktop.swingworker.SwingWorker;
import org.kepler.build.Run;
import org.kepler.build.modules.Module;
import org.kepler.build.project.ProjectLocator;
import org.kepler.modulemanager.ModuleDownloader;
import org.kepler.modulemanager.gui.ModuleDownloadProgressMonitor;
import org.kepler.util.ShutdownNotifier;


/**
 * Created by David Welker.
 * Date: Oct 31, 2009
 * Time: 4:39:07 AM
 */
public class UpgradeDialogPanel extends JPanel
{
    List<ModulePair> upgradeList;

    JLabel intro = new JLabel("The following modules may be upgraded:");
    JList upgradeJList = new JList();
    JScrollPane upgradeScrollPane = new JScrollPane(upgradeJList);
    JLabel question = new JLabel("Would you like to upgrade these modules?");
    JButton yes = new JButton("Yes");
    JButton no = new JButton("No");

    public UpgradeDialogPanel(List<ModulePair> upgradeList)
    {
        super();
        this.upgradeList = upgradeList;
        initComponents();
        layoutComponents();
    }

    public boolean shouldDisplay()
    {
        return upgradeList != null && !upgradeList.isEmpty();
    }

    private void dispose()
    {
        Container c = this;

        while( c != null && !(c instanceof JDialog) )
            c = c.getParent();

        if( c != null && c instanceof JDialog )
            ((JDialog)c).dispose();
        else
            System.out.println("ERROR: This dialog is not on a JDialog!");
    }

    private void initComponents()
    {
        DefaultListModel lm = new DefaultListModel();
        for( ModulePair mp : upgradeList )
            lm.addElement(mp.from + " to " + mp.to);
        upgradeJList.setModel(lm);

        yes.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
            	int result = JOptionPane.showConfirmDialog(
            			UpgradeDialogPanel.this,
            			"Any unsaved work will be LOST. Continue?",
            			"Confirm", JOptionPane.YES_NO_OPTION);
            	if (result == JOptionPane.NO_OPTION){
            		return;
            	}
            	
            	yes.setEnabled(false);
            	no.setEnabled(false);
            	
                SwingWorker worker = new SwingWorker<Void, Void>()
                {
                    public Void doInBackground() throws Exception
                    {
                        downloadModules(upgradeList);
                        return null;
                    }

                    @Override
                    protected void done()
                    {
                        JOptionPane.showMessageDialog(UpgradeDialogPanel.this,  "Done upgrading modules");

                        Project project = new Project();
                        project.setBaseDir(ProjectLocator.getProjectDir());
                        DefaultLogger logger = new DefaultLogger();
                        logger.setMessageOutputLevel(Project.MSG_INFO);
                        logger.setOutputPrintStream(System.out);
                        logger.setErrorPrintStream(System.out);
                        project.addBuildListener(logger);

                        //XXX call ShutdownNotifer.shutdown() before spawning new process,
                        // to avoid 2nd instance potentially interfering with quitting first.
                        // see: http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5484
                        System.out.println("UpgradeDialogPanel notifiying shutdown listeners " +
                			"of impending shutdown. Any open databases may take awhile to close...");
                        ShutdownNotifier.shutdown();
                        
                        System.out.println("UpgradeDialogPanel Spawning new Kepler process");
                        Run run = new Run();
                        run.setTaskName("run");
                        run.setProject(project);
                        run.init();
                        run.setSpawn(true);
                        run.execute();

                        System.out.println("UpgradeDialogPanel Ending current Kepler process");
                        
                        //TODO why call exit with error arg here?
                        System.exit(1);
                    }


                };
                worker.execute();
            }
        });

        no.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                for(ModulePair mp : upgradeList)
                    mp.to.changeName(mp.from.getName());
                dispose();
            }
        });

    }

    private void downloadModules(List<ModulePair> upgradeList) throws Exception
    {

        List<String> downloadList = new ArrayList<String>();
        for( ModulePair mp : upgradeList )
        {
            downloadList.add( mp.to.getName() );
        }
        ModuleDownloader downloader = new ModuleDownloader();
        ModuleDownloadProgressMonitor mdpm = new ModuleDownloadProgressMonitor(this);
        downloader.addListener(mdpm);        
        try {
        	downloader.downloadModules(downloadList);
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(
            	UpgradeDialogPanel.this,
              	  e.getMessage());
        }

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
                .add(intro)
                .add(upgradeScrollPane)
                .add(question)
                .add(layout.createSequentialGroup()
                    .add(yes)
                    .add(no))
        );

        layout.setVerticalGroup
        (
            layout.createSequentialGroup()
                .add(intro)
                .add(upgradeScrollPane)
                .add(question)
                .add(layout.createParallelGroup()
                    .add(yes)
                    .add(no))
        );


    }

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater( new Runnable()
        {

            public void run()
            {
                ModulePair m1 = new ModulePair(Module.make("module-manager-1.0.0"), Module.make("module-manager-1.0.1"));
                ModulePair m2 = new ModulePair(Module.make("foo-1.0.2"), Module.make("foo-1.0.7"));

                List<ModulePair> upgradeList = new ArrayList<ModulePair>();
                upgradeList.add(m1);
                upgradeList.add(m2);


                JFrame frame = new JFrame("Upgrade Dialog");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.add(new UpgradeDialogPanel(upgradeList));
                frame.pack();
                frame.setSize(400,500);
                frame.setVisible(true);
            }
        });

    }
}
