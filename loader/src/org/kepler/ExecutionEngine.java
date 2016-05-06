/*
 * Copyright (c) 2008-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-05-15 11:43:58 -0700 (Wed, 15 May 2013) $' 
 * '$Revision: 32048 $'
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

package org.kepler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

import org.kepler.loader.util.ParseWorkflow;
import org.kepler.util.sql.HSQL;

import ptolemy.actor.CompositeActor;
import ptolemy.actor.ExecutionListener;
import ptolemy.actor.Manager;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.KernelException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Workspace;
import ptolemy.moml.MoMLParser;
import ptolemy.moml.filter.BackwardCompatibility;
import ptolemy.util.MessageHandler;

/**
 * This class is the main entry point for making API calls to the underlying
 * ptolemy execution engine and moml parser.
 */
public class ExecutionEngine implements ExecutionListener
{
  private static ExecutionEngine instance = null;
  private Vector<ExecutionListener> exeListeners;
  
  /**
   * singleton accessor
   */
  public static ExecutionEngine getInstance()
  {
    if(instance == null)
    {
    	instance = new ExecutionEngine();
    }
    return instance;
  }
  
  /**
   * private constructor
   */
  private ExecutionEngine()
  {
    MoMLParser.setMoMLFilters(BackwardCompatibility.allFilters());
    MessageHandler.setMessageHandler(new MessageHandler());
    exeListeners = new Vector<ExecutionListener>();
  }
  
  /**
   * run a single model in the current thread
   * @param model the model to run
   * @return Manager the manager to control the execution
   */
  public Manager runModel(CompositeActor model) 
    throws IllegalActionException, KernelException
  {
    System.out.println("Running model in current thread: " + model.getName());
    Manager manager = setupModel(model);
    runModel(manager, false);
    return manager;
  }
  
  /**
   * run a model in a new thread.  Use the manager to control the thread.
   * @param model the model to run
   * @return Manager the manager that controls the new thread
   */
  public Manager runModelInThread(CompositeActor model)
    throws IllegalActionException, KernelException
  {
    System.out.println("Running model in new thread: " + model.getName());
    Manager manager = setupModel(model);
    runModel(manager, true);
    return manager;
  }
  
  /**
   * add an execution listener
   * @param exeListener the listener to add
   */
  public void addExecutionListener(ExecutionListener exeListener)
  {
    exeListeners.addElement(exeListener);
  }
  
  /**
   * remove an execution listener
   * @param exeListener the listener to remove
   */
  public void removeExecutionListener(ExecutionListener exeListener)
  {
    exeListeners.remove(exeListener);
  }
  
  /**
   * return a vector of all of the registered ExecutionListeners
   */
  public Vector<ExecutionListener> getExecutionListeners()
  {
    return exeListeners;
  }
  
  /**
   * static method to parse a moml document and return a NamedObj
   * @param moml the moml to parse
   */
  public static NamedObj parseMoML(String moml)
    throws Exception
  {
    return parseMoML(moml, null);
  }
  
  /**
   * static method to parse a moml document and return a NamedObj
   * @param moml the moml to parse
   * @param workspace the workspace to parse the moml in to.
   */
  public static NamedObj parseMoML(String moml, Workspace workspace)
    throws Exception
  {
    MoMLParser parser;
    if(workspace == null)
    {
      parser = new MoMLParser();
    }
    else
    {
      parser = new MoMLParser(workspace);
    }
    NamedObj obj = parser.parse(moml);
    return obj;
  }
  
  /**
   * read a file and return its contents as a string
   * @param filename the name or path of the file to read
   */
  public static String readFile(String filename)
    throws FileNotFoundException, IOException
  {
    File f = new File(filename);
    FileReader fr = null;
    try
    {
        fr = new FileReader(f);
        char[] c = new char[1024];
        int numread = fr.read(c, 0, 1024);
        StringBuffer sb = new StringBuffer();
        while(numread != -1)
        {
          sb.append(c, 0, numread);
          numread = fr.read(c, 0, 1024);
        }
        return sb.toString();
    }
    finally
    {
        if(fr != null)
        {
            fr.close();
        }
    }
  }
  
  /**
   * command line interface to this class
   */
  public static void main(String[] args)
  {
    if(args.length != 1)
    {
      System.out.println("You must provide the path to the workflow to run.");
    }
    try
    {
    	
      Kepler.initialize();
      Kepler.setJavaPropertiesAndCopyModuleDirectories();

      String workflow = args[0];
      File workflowFile = new File(workflow);
      ExecutionEngine engine = ExecutionEngine.getInstance();
      CompositeActor model = (CompositeActor) ParseWorkflow.parseWorkflow(workflowFile);
      if(model == null) {
    	  MessageHandler.error("Error loading " + args[0]);
      } else {
    	  engine.runModel(model);
      }
    }
    catch(Exception e)
    {
      MessageHandler.error("Error running " + args[0], e);
    }
    Kepler.shutdown();
    HSQL.shutdownServers();
  }
  
  /**
   * implements executionError in ExecutionListener
   */
  public void executionError(Manager manager, Throwable throwable)
  {
    System.out.println("Execution finished with an error: " + throwable.getMessage());
  }
  
  /**
   * implements executionFinished in ExecutionListener
   */
  public void executionFinished(Manager manager)
  {
    System.out.println("Execution finished successfully.");
  }
  
  /**
   * implements managerStateChanged in ExecutionListener
   */
  public void managerStateChanged(Manager manager)
  {
    //System.out.println("Manager state changed to " + manager.getState());
  }
  
  /**
   * start the execution of the model, either in a new thread or in the
   * current thread.
   */
  private void runModel(Manager manager, boolean newThread)
    throws IllegalActionException, KernelException
  {
    if(newThread)
    {
      manager.startRun();
    }
    else
    {
      manager.execute();
    }
  }
  
  /**
   * prepare a model for running
   */
  private Manager setupModel(CompositeActor actor)
    throws IllegalActionException
  {
    Manager manager = actor.getManager();
    if (manager == null) 
    {
        manager = new Manager(actor.workspace(), "manager");
        actor.setManager(manager);
    }

    manager.addExecutionListener(this);
    for(int i=0; i<exeListeners.size(); i++)
    {
      ExecutionListener listener = (ExecutionListener)exeListeners.elementAt(i);
      manager.addExecutionListener(listener);
    }
    return manager;
  }
}