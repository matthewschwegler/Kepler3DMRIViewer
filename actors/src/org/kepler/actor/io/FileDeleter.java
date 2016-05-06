/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: tao $'
 * '$Date: 2010-05-05 22:21:26 -0700 (Wed, 05 May 2010) $' 
 * '$Revision: 24234 $'
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
package org.kepler.actor.io;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ptolemy.actor.lib.LimitedFiringSource;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;


/**
 * Delete a file or directory. If this actor deletes a directory, then the directory
 * must be empty in order to be deleted.
 * The output is a  BooleanToken. true if and only if the file or directory is 
 * successfully deleted; false otherwise 
 * If the trigger port is connected, then this actor fires only if a token is
 * provided on any channel of the trigger port. The value of that token
 * does not matter. Specifically, if there is no such token, the prefire()
 * method returns false.
 * @author tao
 *
 */
public class FileDeleter extends LimitedFiringSource
{ 
  
  /** Construct an actor with the given container and name.
   *  @param container The container.
   *  @param name The name of this actor.
   *  @exception IllegalActionException If the entity cannot be contained
   *   by the proposed container.
   *  @exception NameDuplicationException If the container already has an
   *   actor with this name.
   */
  public FileDeleter(CompositeEntity container, String name)
          throws NameDuplicationException, IllegalActionException 
  {
    super(container, name);
    filePathInputParameter = new PortParameter(this, "File Path");
    filePathInputParameter.setStringMode(true);
    filePathInputParameter.setTypeEquals(BaseType.STRING);
    filePathInputParameter.getPort().setTypeEquals(BaseType.STRING);
    
    // Set the type constraint.
    output.setTypeEquals(BaseType.BOOLEAN);
  }
  
  ///////////////////////////////////////////////////////////////////
  ////                     ports and parameters                  ////

  /** 
   * The file or directory path which will be deleted.
   */
  public PortParameter filePathInputParameter;
  
  
  ///////////////////////////////////////////////////////////////////
  ////                         public methods                    ////

  /**
   * Reconfigure actor when certain attributes change.
   * 
   * @param attribute
   *            The changed Attribute.
   * @throws ptolemy.kernel.util.IllegalActionException
   * 
   */
  @Override
  public void attributeChanged(Attribute attribute) throws IllegalActionException {
        
    if (attribute != null && attribute == filePathInputParameter && 
            filePathInputParameter.getToken() != null) 
    {
      
      StringToken token = (StringToken)filePathInputParameter.getToken();
      filePath = token.stringValue();
    } 
    else
    {
      super.attributeChanged(attribute);
    }

  
   }
  
  /**
   * Delete the given file or directory and send out the status of the deleting.
   * 
   * @exception IllegalActionException
   *                If it is thrown by the send() method sending out the
   *                token.
   */
  @Override
  public void fire() throws IllegalActionException 
  {
    super.fire();
    filePathInputParameter.update();
    if(filePathInputParameter.getToken() != null)
    {
      StringToken token = (StringToken)filePathInputParameter.getToken();
      filePath = token.stringValue();
      try
      {
        File file = new File(filePath);
        result = file.delete();
      }
      catch(Exception e)
      {
        log.warn("Couldn't delete the file "+filePath+" - "+e.getMessage());
      }
    }
    output.send(0, new BooleanToken(result));
  }
  
  //File path
  private String filePath = null;
  private boolean result = false;
  
  private static final Log log = LogFactory.getLog(org.kepler.actor.io.FileDeleter.class);

}
