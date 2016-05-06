/*
 * Copyright (c) 1998-2012 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2012-04-27 13:16:27 -0700 (Fri, 27 Apr 2012) $' 
 * '$Revision: 29789 $'
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
package org.kepler.metadata;

import java.util.List;

import org.kepler.objectmanager.data.db.Entity;
import org.xml.sax.InputSource;

/**
 * A metadata parser will be used in the MetadataPaser actor.
 * @author tao
 *
 */
public interface ParserInterface {
  
  /**
   * parses the EML package using an InputSource
   */
  public void parse(InputSource source) throws Exception;
  /**
   * Get a collection of the Entity objects.
   * @return the collection of entities.
   */
  public List<Entity> getEntities();
  
  /**
   * returns the total number of entities in the data item collection that was
   * passed to this class when the object was created.
   */
  public int getEntityCount();
}
