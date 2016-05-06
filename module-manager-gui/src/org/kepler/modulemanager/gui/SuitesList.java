/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-02-21 11:19:33 -0800 (Thu, 21 Feb 2013) $' 
 * '$Revision: 31473 $'
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.DefaultListModel;
import javax.swing.JList;

import org.jdesktop.swingworker.SwingWorker;
import org.kepler.build.modules.Module;
import org.kepler.build.util.HttpsSvnReader;
import org.kepler.modulemanager.RepositoryLocations;


/**
 * Created by David Welker.
 * Date: Sep 18, 2009
 * Time: 10:38:43 AM
 */
public class SuitesList extends JList
{
    private List<String> fullList = new ArrayList<String>();
    private List<String> patchableOnlyList = new ArrayList<String>();
    private List<String> testReleases = new ArrayList<String>();

    private boolean shouldShowFullList = false;

    public SuitesList()
    {
        super();
        //Use an empty default list model, since the permanent model will be calculated in the background.
        DefaultListModel model = new DefaultListModel();
        model.addElement("Calculating...");
        setModel( model );
        populate();
    }

    public void removeElement(String element)
    {
        ((DefaultListModel)getModel()).removeElement(element);
        fullList.remove(element);
        patchableOnlyList.remove(element);
    }

    public void addElement(String element)
    {
        ((DefaultListModel)getModel()).addElement(element);
        fullList.add(element);
        patchableOnlyList.add(element);
    }

    public void sort()
    {
        Collections.sort(fullList);
        Collections.sort(patchableOnlyList);
        DefaultListModel model = new DefaultListModel();
        List<String> listToShow = shouldShowFullList ? fullList : patchableOnlyList;
        for( String module : listToShow )
        {
            model.addElement(module);
        }
        setModel(model);
    }

    public void setShowTestReleases(boolean showTestReleases)
    {
        if( showTestReleases )
        {
            showTestReleases();
        }
        else
        {
            hideTestReleases();
        }
    }

    private void showTestReleases()
    {
        DefaultListModel model = (DefaultListModel)getModel();
        for( String testRelease : testReleases )
        {
            model.addElement(testRelease);
        }
    }

    private void hideTestReleases()
    {
        DefaultListModel model = (DefaultListModel)getModel();
        for( String testRelease : testReleases )
        {
          model.removeElement(testRelease);
        }
    }

    public void setShouldShowFullList(boolean shouldShowFullList)
    {
        this.shouldShowFullList = shouldShowFullList;
        DefaultListModel model = new DefaultListModel();
        List<String> listToShow = shouldShowFullList ? fullList : patchableOnlyList;
        for( String module : listToShow )
        {
            model.addElement(module);
        }
        setModel(model);
    }

    public void populate()
    {
        System.out.println("Release: " + RepositoryLocations.getReleaseLocation());
        SwingWorker worker = new SwingWorker<DefaultListModel, Void>()
        {
            public DefaultListModel doInBackground()
            {                
                DefaultListModel listModel = new DefaultListModel();
                List<String> lines = new ArrayList<String>();
                List<String> files = new ArrayList<String>();
                
                try
                {
                    URL url = new URL(RepositoryLocations.getReleaseLocation());

                    BufferedReader br = null;
                    InputStreamReader stream = null;
  	                try {
  	                	stream = new InputStreamReader(url.openStream());
  	                	br = new BufferedReader(stream);
  	                	String line = null;
  	                	while ((line = br.readLine()) != null)
  	                	{
  	                		lines.add(line);
  	                	}
  	                } finally {
  	                	if(br != null) {
  	                		br.close();
  	                	}
  	                	if(stream != null) {
  	                		stream.close();
  	                	}
  	                }


                    for (String l : lines)
                    {
                        if( l.trim().startsWith("<dir name=") || l.trim().startsWith("<file name=") )
                        {
                            files.add(l.split("\"")[1]);
                        }
                    }
                }
                catch(Exception e)
                {
                    System.out.println("Could not get suite list from SVN: " + e.getMessage());
                    e.printStackTrace();
                }

    
                
                for(String module : files )
                {
                    List<String> moduleInfo = HttpsSvnReader.readAll(RepositoryLocations.getReleaseLocation() + "/" + module + "/module-info");
                    if(moduleInfo != null && moduleInfo.contains("modules.txt") && !moduleInfo.contains("test-release") )
                    {
                    	String patchableSuite = module.substring(0, module.length() - String.valueOf(Module.make(module).getPatch()).length() - 1);
                        if( !fullList.contains(patchableSuite) )
                        {
                            fullList.add(patchableSuite);
                        }
                        if( !patchableOnlyList.contains(patchableSuite) )
                        {
                            patchableOnlyList.add(patchableSuite);
                            listModel.addElement(patchableSuite);
                        }
                        fullList.add(module);
                    }
                    if( moduleInfo != null && moduleInfo.contains("test-release") )
                    {
                        testReleases.add(module);
                    }
                }


                return listModel;
            }

            public void done()
            {
                try
                {
                    setModel(get());
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                catch (ExecutionException e)
                {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
}