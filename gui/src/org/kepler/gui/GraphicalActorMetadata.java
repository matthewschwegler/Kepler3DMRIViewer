/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
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

package org.kepler.gui;

import java.io.InputStream;

import org.kepler.icon.IconMetadataHandler;
import org.kepler.objectmanager.ActorMetadata;
import org.kepler.objectmanager.InvalidMetadataException;

import ptolemy.kernel.util.NamedObj;

public class GraphicalActorMetadata extends ActorMetadata
{
	/**
	 * Constructor. Takes in xml metadata. This should be a moml entity with the
	 * kepler additional metadata properties. The entity is parsed and an
	 * ActorMetadata object is created with appropriate fields.
	 * 
	 * @param moml the xml metadata
	 */
	public GraphicalActorMetadata(InputStream moml) throws InvalidMetadataException 
  {
    super(moml);
    addGraphicalHandlers();
  }
  
  /**
	 * builds a new ActorMetadata object from an existing NamedObj
	 * 
	 * @param am the ActorMetadata to build this object from.
	 */
  public GraphicalActorMetadata(NamedObj obj)
  {
    super(obj);
    addGraphicalHandlers();
  }
  
  /**
   * add the graphical handlers to the actormetadata object
   */
  public GraphicalActorMetadata(ActorMetadata am)
    throws Exception
  {
    super((NamedObj)am.getActorAsNamedObj(null));
    addGraphicalHandlers();
  }
  
  /**
   * add the handlers to add graphical elements to the actor
   */
  private void addGraphicalHandlers()
  {
    addMetadataHandler(new IconMetadataHandler());
  }
}