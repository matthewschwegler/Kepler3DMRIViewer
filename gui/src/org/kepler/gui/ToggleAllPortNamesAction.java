/* An action to toggle displaying all port names.
 * 
 * Copyright (c) 2011 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2011-10-04 14:11:45 -0700 (Tue, 04 Oct 2011) $' 
 * '$Revision: 28755 $'
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

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import ptolemy.actor.gui.PtolemyFrame;
import ptolemy.actor.gui.TableauFrame;
import ptolemy.data.BooleanToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.Entity;
import ptolemy.kernel.Port;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.moml.MoMLChangeRequest;
import ptolemy.util.MessageHandler;
import ptolemy.vergil.toolbox.FigureAction;

/** An action to toggle displaying all port names.
 * 
 * @author Daniel Crawl
 * @version $Id: ToggleAllPortNamesAction.java 28755 2011-10-04 21:11:45Z crawl $
 *
 */

public class ToggleAllPortNamesAction extends FigureAction {

    public ToggleAllPortNamesAction(TableauFrame frame) {
        super("");
        _parent = frame;
    }

    public void actionPerformed(ActionEvent event) {
        super.actionPerformed(event);
                
        NamedObj model = ((PtolemyFrame)_parent).getModel().toplevel();
     
        // see if we've already toggled for this model
        Boolean showNames = _modelMap.get(model);
        if(showNames == null)
        {
            showNames = Boolean.TRUE;
        }
        
        // toggle the names
        _togglePortNames((Entity)model, showNames.booleanValue());
        
        // add this model to the map
        _modelMap.put(model, !showNames.booleanValue());
        
        // perform an empty change request to repaint the canvas
        MoMLChangeRequest change = new MoMLChangeRequest(model, model, "<group></group>");
        change.setPersistent(true);
        model.requestChange(change);
    }
    
    /** Toggle the port names for an entity and any contained entities. */
    private void _togglePortNames(Entity entity, boolean showNames)
    {
        List<?> portList = entity.portList();
        for(Object obj : portList)
        {
            final Port port = (Port)obj;
            final boolean isSet = _isPropertySet(port, "_showName");
            final boolean isHidden = _isPropertySet(port, "_hide"); 
            
            if(showNames && !isSet && !isHidden)
            {
                try {
                    new Parameter(port, "_showName", BooleanToken.TRUE);
                } catch (Exception e) {
                    MessageHandler.error("Unable to show port name for " +
                        port.getFullName(), e);
                }
            }
            else if(!showNames && isSet)
            {
                try {
                    port.getAttribute("_showName").setContainer(null);
                } catch (Exception e) {
                    MessageHandler.error("Unable to remove _showName for " +
                        port.getFullName(), e);
                }
            }
        }
        
        // recursively toggle port names of contained entities
        if(entity instanceof CompositeEntity)
        {
            List<?> entityList = ((CompositeEntity)entity).entityList();
            for(Object entityObj : entityList)
            {
                _togglePortNames((Entity)entityObj, showNames);
            }
        }
    }
    
    /** Return true if the property of the specified name is set for
     *  the specified object. A property is specified if the specified
     *  object contains an attribute with the specified name and that
     *  attribute is either not a boolean-valued parameter, or it is a
     *  boolean-valued parameter with value true.
     *  FIXME copied from PortConfigurerDialog
     *  @param object The object.
     *  @param name The property name.
     *  @return True if the property is set.
     */
    private boolean _isPropertySet(NamedObj object, String name) {
        Attribute attribute = object.getAttribute(name);

        if (attribute == null) {
            return false;
        }

        if (attribute instanceof Parameter) {
            try {
                Token token = ((Parameter) attribute).getToken();

                if (token instanceof BooleanToken) {
                    if (!((BooleanToken) token).booleanValue()) {
                        return false;
                    }
                }
            } catch (IllegalActionException e) {
                // Ignore, using default of true.
            }
        }

        return true;
    }

    /** The parent frame. */
    private TableauFrame _parent;
    
    /** A mapping from model to current toggle value. Each model will be toggled
     *  independently. 
     */
    private static Map<NamedObj,Boolean> _modelMap =
        Collections.synchronizedMap(new WeakHashMap<NamedObj,Boolean>());
}
