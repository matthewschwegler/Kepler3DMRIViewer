/**
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: tao $'
 * '$Date: 2010-06-03 16:45:10 -0700 (Thu, 03 Jun 2010) $' 
 * '$Revision: 24730 $'
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

import org.kepler.authentication.AuthenticationListener;

import ptolemy.actor.gui.TableauFrame;


/**
 * This class represents an action to authenticate a user without the selection of 
 * the authentication domain. Its parent has both domain selection and authentication
 * @author tao
 *
 */
public class AuthenticationWithDomainNameAction extends AuthenticateAction
{
  /**
   * Constructor
   * 
   * @param parent
   *            the "frame" (derived from ptolemy.gui.Top) where the menu is
   *            being added.
   */
  public AuthenticationWithDomainNameAction(TableauFrame parent,String domainName) 
  {
     super(parent);
     this.domainName = domainName;
  }

  /**
   * Constructor
   * 
   * @param parent
   *            the "frame" (derived from ptolemy.gui.Top) where the menu is
   *            being added.
   * @param authListener
   *            listener of the authentication process
   */
  public AuthenticationWithDomainNameAction(TableauFrame parent,
              AuthenticationListener authListener, String domainName) 
  {
    super(parent,authListener);
    this.domainName = domainName;
  }
  
 
  /*
   *Overwrite the super method to do the real action - authenticate the user
   */
  protected void doAction() {
    fireAuthentication();
  }

}
