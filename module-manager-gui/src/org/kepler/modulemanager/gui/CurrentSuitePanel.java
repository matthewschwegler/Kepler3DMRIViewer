/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-03-28 15:01:25 -0700 (Thu, 28 Mar 2013) $' 
 * '$Revision: 31810 $'
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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.jdesktop.layout.GroupLayout;
import org.jdesktop.swingworker.SwingWorker;
import org.kepler.build.Run;
import org.kepler.build.modules.CurrentSuiteTxt;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.build.modules.ModulesTxt;
import org.kepler.build.project.ProjectLocator;
import org.kepler.build.util.Version;
import org.kepler.modulemanager.ModuleDownloader;
import org.kepler.util.ShutdownNotifier;

import ptolemy.gui.GraphicalMessageHandler;
import ptolemy.gui.JFileChooserBugFix;
import ptolemy.gui.PtFileChooser;
import ptolemy.gui.PtFilenameFilter;
import ptolemy.gui.PtGUIUtilities;
import ptolemy.util.MessageHandler;

/**
 * Created by David Welker.
 * Date: Oct 7, 2009
 * Time: 6:33:12 PM
 */
public class CurrentSuitePanel extends JPanel
{
    private static String CURRENT_SUITE_LABEL_TEXT = 
        "<html><p><font color=blue>If you're seeing this dialog when trying to launch Kepler, " +
        "Kepler had a problem starting up.</font></p><p><font color=blue>Please select a suite " +
        "from the Available Suites and Modules tab and click Apply and Restart.</font></p>" +
        "<p>Current Suite:</p></html>";
    private JLabel currentSuiteListLabel = new JLabel(CURRENT_SUITE_LABEL_TEXT);
    private JList currentSuiteList = new JList();
    private JScrollPane activeModulesListScrollPane = new JScrollPane(currentSuiteList);

    private JButton saveConfiguration = new JButton("Save Suite");
    private JButton loadConfiguration = new JButton("Load Suite");

    private ModuleDownloader downloader;

    public CurrentSuitePanel()
    {
        super();
        initComponents();
        layoutComponents();
    }

    private void initActiveModulesList()
    {
        String currentSuiteName = CurrentSuiteTxt.getName();
        if( currentSuiteName.equals("unknown") )
        {
            currentSuiteName = "custom";
        }
        currentSuiteListLabel.setText(CURRENT_SUITE_LABEL_TEXT + currentSuiteName );

        DefaultListModel listModel = new DefaultListModel();
        ModulesTxt.init();
        for(Module module : ModuleTree.instance())
        {
            listModel.addElement(module.toString());
        }
        currentSuiteList.setModel(listModel);
    }

    private void initComponents()
    {
        initActiveModulesList();
        downloader = new ModuleDownloader();
        ModuleDownloadProgressMonitor mdpm = new ModuleDownloadProgressMonitor(this);
        downloader.addListener(mdpm);

        currentSuiteList.addListSelectionListener(new ListSelectionListener()
        {
            public void valueChanged(ListSelectionEvent e)
            {
                currentSuiteList.clearSelection();
            }
        });

        saveConfiguration.addActionListener( new ActionListener()
        {

            public void actionPerformed(ActionEvent e)
            {
                // Avoid white boxes in file chooser, see
                // http://bugzilla.ecoinformatics.org/show_bug.cgi?id=3801
                JFileChooserBugFix jFileChooserBugFix = new JFileChooserBugFix();
                Color background = null;
                PtFileChooser fc = new PtFileChooser(null, "Save Suite", JFileChooser.SAVE_DIALOG);
                try {
                    background = jFileChooserBugFix.saveBackground();
                    fc.setSelectedFile(new File("unnamed.txt"));
                    int returnValue = fc.showDialog(CurrentSuitePanel.this, "Save");
                    File saveToFile = fc.getSelectedFile();
                    if(saveToFile.exists() && !PtGUIUtilities.useFileDialog()) {
                    	if(MessageHandler.yesNoQuestion("Overwrite \""
                                + saveToFile.getName() + "\"?")) {
                    		returnValue = JFileChooser.APPROVE_OPTION;
                    	} else {
                    		returnValue = JFileChooser.CANCEL_OPTION;
                    	}
                    }
                    if( returnValue == JFileChooser.APPROVE_OPTION )
                        {
                            Copy copy = new Copy();
                            copy.setFile(ModulesTxt.instance());
                            copy.setTofile(saveToFile);
                            copy.execute();
                        }
                        } finally {
                            jFileChooserBugFix.restoreBackground(background);
                        }
            }
        });

        loadConfiguration.addActionListener( new ActionListener()
        {

            public void actionPerformed(ActionEvent e)
            {
                PtFileChooser fc = new PtFileChooser(null, "Load Suite", JFileChooser.OPEN_DIALOG);
                
                fc.addChoosableFileFilter(new PtFilenameFilter()
                {
                	@Override
                    public boolean accept(File f)
                    {
                        return f.getName().endsWith(".txt");
                    }

                	@Override
                    public String getDescription()
                    {
                        return "Module configuration files.";
                    }
                });
                int returnValue = fc.showDialog(CurrentSuitePanel.this, "Load Configuration");
                if( returnValue == JFileChooser.APPROVE_OPTION )
                {
                    System.out.println("Load");

                    File loadFromFile = fc.getSelectedFile();
                    Copy copy = new Copy();
                    copy.setOverwrite(true);           
                    copy.setFile(loadFromFile);
                    copy.setTofile(ModulesTxt.instance());
                    copy.execute();
                    initActiveModulesList();

                    JOptionPane.showMessageDialog(CurrentSuitePanel.this, "Restarting...");
                    SwingWorker worker = new SwingWorker<Void, Void>()
                    {
                        public Void doInBackground() throws Exception
                        {
                            saveConfiguration.setEnabled(false);
                            loadConfiguration.setEnabled(false);

                            ListModel listModel = currentSuiteList.getModel();
                            List<String> moduleList = new ArrayList();
                            for (int i = 0; i < listModel.getSize(); i++)
                              moduleList.add((String) listModel.getElementAt(i));
                            try {
                            	downloader.downloadModules(moduleList);
                            } catch (FileNotFoundException e) {
                                JOptionPane.showMessageDialog(
                                	CurrentSuitePanel.this,
                                  	  e.getMessage());
                            }                            
                            return null;
                        }

                        @Override
                        protected void done()
                        {
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
                            System.out.println("CurrentSuitePanel notifiying shutdown listeners " +
                    			"of impending shutdown. Any open databases may take awhile to close...");
                            ShutdownNotifier.shutdown();
                            
                            Run run = new Run();
                            run.setTaskName("run");
                            run.setProject(project);
                            run.init();
                            run.setSpawn(true);
                            run.execute();

                            //TODO why call exit with error arg here?
                            System.exit(1);
                        }

                    };
                    worker.execute();
                }
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
                .add(currentSuiteListLabel)
                .add(activeModulesListScrollPane)
                .add( layout.createSequentialGroup()
                    .add(saveConfiguration)
                    .add(loadConfiguration))
        );

        layout.setVerticalGroup
        (
            layout.createSequentialGroup()
                .add(currentSuiteListLabel)
                .add(activeModulesListScrollPane)
                .add(layout.createParallelGroup()
                    .add(saveConfiguration)
                    .add(loadConfiguration))
        );
    }

    /** Perform sanity checks on the suite to see if Kepler will start
     *  using that suite. If it appears that Kepler will not start,
     *  ask the user if they really want to exit the Module Manager.
     * 
     * @return returns true if the Module Manager can exit.
     */
	public static boolean canExit() {
	    
	    //System.out.println("Performing sanity checks for selected suite.");
	    
	    boolean retval = true;
	    
	    GraphicalMessageHandler handler = new GraphicalMessageHandler();
	    MessageHandler.setMessageHandler(handler);
	    
	    final ModulesTxt modulesTxt = ModulesTxt.instance();
	    
	    // read the modules txt file
	    modulesTxt.read();
	    
	    // see if kepler suite is found
	    ModuleTree tree = new ModuleTree(modulesTxt);
	    if(tree.getModuleByStemName("kepler") == null) {
	        
	        // check the suite name
	        CurrentSuiteTxt.init();
	        String suiteName = CurrentSuiteTxt.getName();
	        if(!suiteName.equals("kepler") && !Version.stem(suiteName).equals("kepler")) {
    	        retval = MessageHandler.yesNoQuestion("The current suite does not contain the kepler module.\n" +
    	        		"Kepler will most likely not start with this suite.\n" +
    	                "Do you still want to exit the Module Manager?");
	        }
	        
	    }
	    /* This is commented out since allModulesPresent() can incorrectly return false.
	     * If use.keplerdata exists, allModulesPresent() appears to only look for modules
	     * in kepler.modules and not where Kepler is installed.
	    else if(!tree.allModulesPresent()) {
	        StringBuilder message = new StringBuilder("The following modules are not present: ");
	        for(Module module : tree.getMissingModules()) {
	            message.append("\n" + module.getName());
	        }
	        message.append("\n");
	        message.append("Do you still want to exit the Module Manager?");
	        retval = MessageHandler.yesNoQuestion(message.toString());
	    }
	    */
	    
	    return retval;
	    
	}
}