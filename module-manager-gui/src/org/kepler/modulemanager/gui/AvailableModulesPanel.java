/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-03-28 15:06:02 -0700 (Thu, 28 Mar 2013) $' 
 * '$Revision: 31811 $'
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
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.jdesktop.layout.GroupLayout;
import org.kepler.build.Run;
import org.kepler.build.modules.CurrentSuiteTxt;
import org.kepler.build.modules.ModulesTxt;
import org.kepler.build.modules.RollbackTxt;
import org.kepler.build.project.PrintError;
import org.kepler.build.project.ProjectLocator;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationManagerException;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.modulemanager.ModuleDownloader;
import org.kepler.modulemanager.gui.patch.PatchChecker;
import org.kepler.util.ShutdownNotifier;

/**
 * Created by David Welker.
 * Date: Sep 18, 2009
 * Time: 10:26:30 AM
 */
public class AvailableModulesPanel extends JPanel 
{
  //Component Declarations

  //- List of available released suites.
  private JLabel suitesLabel = new JLabel("Available Suites:");
  private SuitesList suitesList = new SuitesList();
  private JScrollPane suitesListScrollPane = new JScrollPane(suitesList);
  private String suitesLabelToolTip = "List of Available Suites";

  private JCheckBox showSuitePatchesCheckBox = new JCheckBox("Show suite patches.");

  //- List of all available modules, including suites.
  private JLabel modulesLabel = new JLabel("Available Modules:                         ");
  private ModulesList modulesList = new ModulesList();
  private JScrollPane modulesListScrollPane = new JScrollPane(modulesList);
  private String modulesLabelToolTip = "List of Available Modules";

  private JCheckBox showTestReleasesCheckBox = new JCheckBox("Show test releases.");

  private JLabel selectedLabel = new JLabel("Selected Modules:");
  private JList selectedModulesList = new JList();
  private JScrollPane selectedModulesListScrollPane = new JScrollPane(selectedModulesList);
  private String selectedLabelToolTip = "The following modules or suites have been selected for installation";
   
  // private JButton retrieveButton = new JButton("Retrieve");
  private JButton restartButton = new JButton("Apply and Restart");
  private JButton cancelButton = new JButton("Cancel");
  private JButton selectButton = new JButton("\u2192");
  private JButton unselectButton = new JButton("\u2190");
  private JButton upButton = new JButton("\u2191");
  private JButton downButton = new JButton("\u2193");
  
  private ModuleDownloader downloader;

  private JCheckBox shouldCheckForPatchesCheckBox = new JCheckBox("Automatically check for patches on startup.");
  private JButton checkForPatchesNowButton = new JButton("Check for Patches Now");
  private JButton rollbackKeplerButton = new JButton("Rollback Kepler");

  public AvailableModulesPanel()
  {
      super();
      downloader = new ModuleDownloader();
      ModuleDownloadProgressMonitor mdpm = new ModuleDownloadProgressMonitor(this);
      downloader.addListener(mdpm);
      initComponents();
      layoutComponents();
  }

  private void writeModulesTxt()
  {
    ModulesTxt modulesTxt = ModulesTxt.instance();
    modulesTxt.clear();
    ListModel listModel = selectedModulesList.getModel();
    String firstModule = ((String)listModel.getElementAt(0));
    firstModule = ModuleManagerGuiUtil.writeStringForSuite(firstModule);
    boolean isSingleSuite = listModel.getSize() == 1 && firstModule.startsWith("*");
    String newSuite = isSingleSuite ? firstModule.substring(1, firstModule.length()) : "unknown";
    for (int i = 0; i < listModel.getSize(); i++)
    {
      String moduleName = (String) listModel.getElementAt(i);
      if( moduleName.startsWith("*") )
      {
    	  moduleName = ModuleManagerGuiUtil.writeStringForSuite(moduleName);
      }
      modulesTxt.add(moduleName);
    }
    modulesTxt.write();
    CurrentSuiteTxt.delete();
    CurrentSuiteTxt.setName(newSuite);
  }

  private void initComponents()
  {
    selectedModulesList.setModel(new DefaultListModel());
    selectedModulesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    //retrieveButton.setEnabled(false);
    restartButton.setEnabled(false);
    cancelButton.setEnabled(false);
    upButton.setEnabled(false);
    downButton.setEnabled(false);

    restartButton.addActionListener(new ActionListener()
    {
        public void actionPerformed(ActionEvent event)
        {
            // retrieveButton.setEnabled(false);
            int result = JOptionPane.showConfirmDialog(
            	  AvailableModulesPanel.this,
            	  "Any unsaved work will be LOST. Continue?",
            	  "Confirm", JOptionPane.YES_NO_OPTION);
			      if (result == JOptionPane.NO_OPTION) {
				        return;
			      }
			                      
            restartButton.setEnabled(false);
            cancelButton.setEnabled(false);
            selectButton.setEnabled(false);
            unselectButton.setEnabled(false);
            upButton.setEnabled(false);
            downButton.setEnabled(false);

            final Project project = new Project();
            project.setBaseDir(ProjectLocator.getProjectDir());
            
            // NOTE: before performing sanity checks on the selected suite,
            // all the modules must be downloaded first so that we know
            // what modules are in the selected suite.

            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>()
            {
                @Override
                /** Download all the modules in the selected suite. */
                public Void doInBackground() throws Exception
                {
                    DefaultLogger logger = new DefaultLogger();
                    logger.setMessageOutputLevel(Project.MSG_INFO);
                    logger.setOutputPrintStream(System.out);
                    logger.setErrorPrintStream(System.out);
                    project.addBuildListener(logger);

                    ListModel listModel = selectedModulesList.getModel();
                    List<String> moduleList = new ArrayList();
                    for (int i = 0; i < listModel.getSize(); i++)
                    {
                    	moduleList.add((String) listModel.getElementAt(i));
                    }
                    try {
                    	downloader.downloadModules(moduleList);
                    } catch (FileNotFoundException e) {
                        JOptionPane.showMessageDialog(
                          	  AvailableModulesPanel.this,
                          	  e.getMessage());
                        return null;
                    }
                    
                    return null;
                }
                
                @Override
                /** Perform sanity checks before restarting with the new suite. */
                public void done() {
                    
                    // perform sanity checks on the suite
                    try {
                        RollbackTxt.save();
                    } catch(IOException e) {
                        PrintError.message("Error saving modules.txt to rollback file.", e);
                        _enableButtons();
                        return;
                    }
                    writeModulesTxt();
                    boolean restart = CurrentSuitePanel.canExit();
                    if(!restart) {
                        RollbackTxt.load();
                        _enableButtons();
                        return;
                    }

                    //XXX call ShutdownNotifer.shutdown() before spawning new process,
                    // to avoid 2nd instance potentially interfering with quitting first.
                    // see: http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5484
                    System.out.println("AvailableModulesPanel notifying shutdown listeners " +
                        "of impending shutdown. Closing any open databases may take awhile...");
                    ShutdownNotifier.shutdown();
                    
                    System.out.println("AvailableModulesPanel Spawning new Kepler process");

                    Run run = new Run();
                    run.setTaskName("run");
                    run.setProject(project);
                    run.init();
                    run.setSpawn(true);
                    run.execute();

                    System.out.println("AvailableModulesPanel Ending current Kepler process");
                    System.out.println(" NOTE: This will probably throw an exception as the current process is terminated while a new Kepler process starts. This is normal and expected.");
                    
                    //TODO why call exit with error arg here?
                    System.exit(1);
                }
                
                /** Enable buttons that were disabled when apply and restart was pushed. */
                private void _enableButtons() {   
                    restartButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                    selectButton.setEnabled(true);
                    unselectButton.setEnabled(true);
                    upButton.setEnabled(true);
                    downButton.setEnabled(true);
                }
            };
            worker.execute();
            

        }
    });

    selectButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        for (Object s : suitesList.getSelectedValues())
        {
          String suite = (String) s;
          ((DefaultListModel) selectedModulesList.getModel()).addElement("*" + suite);
          suitesList.removeElement(suite);
        }

        for (Object m : modulesList.getSelectedValues())
        {
          String module = (String) m;
          ((DefaultListModel) selectedModulesList.getModel()).addElement(module);
          ((DefaultListModel) modulesList.getModel()).removeElement(module);
        }
        DefaultListModel m = (DefaultListModel) selectedModulesList.getModel();
        if (m.getSize() > 0)
        {
         // retrieveButton.setEnabled(true);
          restartButton.setEnabled(true);
          cancelButton.setEnabled(true);
          upButton.setEnabled(true);
          downButton.setEnabled(true);
        }
      }
    });

    unselectButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        unselect(false);
        DefaultListModel m = (DefaultListModel) selectedModulesList.getModel();
        if (m.getSize() == 0)
        {
         // retrieveButton.setEnabled(false);
          restartButton.setEnabled(false);
          cancelButton.setEnabled(false);
          upButton.setEnabled(false);
          downButton.setEnabled(false);
        }
      }
    });

    upButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        moveUp();
      }
    });

    downButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        moveDown();
      }
    });

    showSuitePatchesCheckBox.addActionListener(new ActionListener()
    {
        public void actionPerformed(ActionEvent e)
        {
            suitesList.setShouldShowFullList(showSuitePatchesCheckBox.isSelected());
        }
    });

    showTestReleasesCheckBox.addActionListener(new ActionListener()
    {
        public void actionPerformed(ActionEvent e)
        {
            suitesList.setShowTestReleases(showTestReleasesCheckBox.isSelected());
            modulesList.setShowTestReleases(showTestReleasesCheckBox.isSelected());
        }
    });
    
    // the Show Test Releases checkbox no longer works, so hiding until
    // can be fixed.
    showTestReleasesCheckBox.setVisible(false);

    ConfigurationProperty mm = ConfigurationManager.getInstance().getProperty(ConfigurationManager.getModule("module-manager"));
    final ConfigurationProperty checkForPatches = mm.getProperty("check-for-patches");
    boolean shouldCheckForPatches = checkForPatches.getValue().trim().equals("true") ? true : false;

    shouldCheckForPatchesCheckBox.setSelected(shouldCheckForPatches);

    shouldCheckForPatchesCheckBox.addItemListener(new ItemListener()
    {
      public void itemStateChanged(ItemEvent e)
      {
        try
        {
          checkForPatches.setValue(""+shouldCheckForPatchesCheckBox.isSelected());
          ConfigurationManager.getInstance().saveConfiguration();
        }
        catch (ConfigurationManagerException e1)
        {
          e1.printStackTrace();
        }


      }
    });

    checkForPatchesNowButton.addActionListener(new ActionListener()
    {

      public void actionPerformed(ActionEvent e)
      {
        if( !PatchChecker.check(true, false) )
        {
          JOptionPane.showMessageDialog(AvailableModulesPanel.this, "No new patches.");
        }
      }
    });

    rollbackKeplerButton.setEnabled(RollbackTxt.exists());
    rollbackKeplerButton.addActionListener(new ActionListener()
    {

      public void actionPerformed(ActionEvent e)
      {

          SwingWorker worker = new SwingWorker<Void, Void>()
          {
              public Void doInBackground() throws Exception
              {
                  Project project = new Project();
                  project.setBaseDir(ProjectLocator.getProjectDir());
                  DefaultLogger logger = new DefaultLogger();
                  logger.setMessageOutputLevel(Project.MSG_INFO);
                  logger.setOutputPrintStream(System.out);
                  logger.setErrorPrintStream(System.out);
                  project.addBuildListener(logger);
                  
                  try {
                	  downloader.downloadModules(RollbackTxt.read());
                  } catch (FileNotFoundException e) {
                      JOptionPane.showMessageDialog(
                        	  AvailableModulesPanel.this,
                        	  e.getMessage());
                      return null;
                  }

                  RollbackTxt.load();

                  System.out.println("AvailableModulesPanel notifiying shutdown listeners " +
      					"of impending shutdown. Closing any open databases may take awhile...");
                  ShutdownNotifier.shutdown();
                  
                  System.out.println("AvailableModulesPanel Spawning new Kepler process");
                  Run run = new Run();
                  run.setTaskName("run");
                  run.setProject(project);
                  run.init();
                  run.setSpawn(true);
                  run.execute();

                  System.out.println("Ending current Kepler process");
                  System.out.println(" NOTE: This will probably throw an exception as the current process is terminated while a new Kepler process starts. This is normal and expected.");
                  
                  //TODO why call exit with error arg here?
                  System.exit(1);

                  return null;
              }
          };
          worker.execute();
      }
    });


  }

  private void unselect(boolean all)
  {
    if (all)
    {
      DefaultListModel selectedModulesListModel = (DefaultListModel) selectedModulesList.getModel();
      //note that size changes dynamically here, so you can't use .size() in the for loop or
      //you will always leave one item in the list
      int size = selectedModulesListModel.size();
      for (int i = 0; i < size; i++)
      {
        String module = (String) selectedModulesListModel.getElementAt(0);
        if (module.startsWith("*"))
        {
          suitesList.addElement(module.substring(1, module.length()));
        }
        else
        {
          ((DefaultListModel) modulesList.getModel()).addElement(module);
          ((DefaultListModel) selectedModulesList.getModel()).removeElement(module);
        }
      }
    }
    else
    {
      for (Object m : selectedModulesList.getSelectedValues())
      {
        String module = (String) m;
        if (module.startsWith("*"))
        {
            suitesList.addElement(module.substring(1, module.length()));
        }
        else
        {
            ((DefaultListModel) modulesList.getModel()).addElement(module);
        }
        ((DefaultListModel) selectedModulesList.getModel()).removeElement(module);
      }
    }

    DefaultListModel suitesListModel = (DefaultListModel) suitesList.getModel();

    suitesList.sort();

    DefaultListModel modulesListModel = (DefaultListModel) modulesList.getModel();
    Vector sortList = new Vector();
    for (int i = 0; i < modulesListModel.size(); i++)
    {
      sortList.add(modulesListModel.getElementAt(i));
    }
    modulesListModel.clear();
    Collections.sort(sortList);
    for (Object o : sortList)
    {
      modulesListModel.addElement(o);
    }

  }

  private void moveUp()
  {
    DefaultListModel m = (DefaultListModel) selectedModulesList.getModel();
    if (selectedModulesList.getSelectedIndices().length == 0)
    {
    	return;
    }
    int i = selectedModulesList.getSelectedIndex();
    if (i == 0)
    {
    	return;
    }
    m.add(i - 1, m.remove(i));
    selectedModulesList.setSelectedIndex(i - 1);
  }

  private void moveDown()
  {
    DefaultListModel m = (DefaultListModel) selectedModulesList.getModel();
    if (selectedModulesList.getSelectedIndices().length == 0)
    {
    	return;
    }
    int i = selectedModulesList.getSelectedIndex();
    if (i == m.getSize() - 1)
    {
    	return;
    }
    m.add(i + 1, m.remove(i));
    selectedModulesList.setSelectedIndex(i + 1);
  }

  private void layoutComponents()
  {
    JPanel buttonPanel = new JPanel();
    buttonPanel.add(upButton);
    buttonPanel.add(downButton);

    suitesLabel.setToolTipText(suitesLabelToolTip);
    modulesLabel.setToolTipText(modulesLabelToolTip);
    selectedLabel.setToolTipText(selectedLabelToolTip);
    
    GroupLayout layout = new GroupLayout(this);
    setLayout(layout);
    layout.setAutocreateContainerGaps(true);
    layout.setAutocreateGaps(true);

    layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.TRAILING)
        .add(
            layout.createSequentialGroup().add(
                layout.createParallelGroup().add(suitesLabel).add(
                    suitesListScrollPane).add(showSuitePatchesCheckBox).add(modulesLabel).add(
                    modulesListScrollPane).add(showTestReleasesCheckBox)).add(
                layout.createParallelGroup().add(selectButton).add(
                    unselectButton)).add(
                layout.createParallelGroup().add(selectedLabel).add(
                    selectedModulesListScrollPane).add(
                    layout.createSequentialGroup().add(upButton).add(downButton))
                    .add(shouldCheckForPatchesCheckBox)
                    .add(checkForPatchesNowButton)
                    .add(rollbackKeplerButton))).add(
            layout.createSequentialGroup().add(restartButton)));//.add(cancelButton)));

    layout.setVerticalGroup(layout.createSequentialGroup().add(
        layout.createParallelGroup(GroupLayout.CENTER).add(
            layout.createSequentialGroup().add(suitesLabel).add(
                suitesListScrollPane).add(showSuitePatchesCheckBox).add(10).add(modulesLabel).add(
                modulesListScrollPane).add(showTestReleasesCheckBox)).add(
            layout.createSequentialGroup().add(selectButton)
                .add(unselectButton)).add(
            layout.createSequentialGroup().add(selectedLabel).add(
                selectedModulesListScrollPane).add(
                layout.createParallelGroup().add(upButton).add(downButton))
                .add(shouldCheckForPatchesCheckBox)
                .add(checkForPatchesNowButton)
                .add(rollbackKeplerButton)))
//        .add(
//
//        )
        .add(
            layout.createParallelGroup().add(restartButton)//.add(cancelButton)));
    //.add(
         //   layout.createParallelGroup().add(lblrestartMsg)
            ));
  }
}