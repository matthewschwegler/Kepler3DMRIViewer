/*
 * Copyright (c) 1997-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:19:36 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31113 $'
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

package org.sdm.spa;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import ptolemy.actor.CompositeActor;
import ptolemy.actor.Manager;
import ptolemy.actor.lib.Transformer;
import ptolemy.data.BooleanToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Nameable;
import ptolemy.kernel.util.TransientSingletonConfigurableAttribute;

//////////////////////////////////////////////////////////////////////////
//// Pause
/**
 * <p>
 * This actor is used for putting an expected pause in the workflow
 * specification to allow for execution to pause until the outputs until that
 * time are reviewed and the workflow is paused. This actor is mainly useful for
 * long-running jobs.
 * </p>
 * 
 * <p> Implementation of this actor is based on the
 * ptolemy.actor.lib.Stop actor code by Edward A. Lee.
 * </p>
 * 
 * @author Ilkay Altintas, Contributors: Edward A. Lee and Christopher Brooks.
 * @version $Id: Pause.java 31113 2012-11-26 22:19:36Z crawl $
 * @category.name flow control
 * @category.name local
 * @entity.description input Input content. Can be any input. They will be
 *                     transfered to the next actor once the workflow resumes.
 **/
public class Pause extends Transformer {

    /**
     * Construct an actor in the specified container with the specified name.
     * 
     * @param container
     *            The container.
     * @param name
     *            The name of this actor within the container.
     * @exception IllegalActionException
     *                If the actor cannot be contained by the proposed
     *                container.
     * @exception NameDuplicationException
     *                If the name coincides with an actor already in the
     *                container.
     */
    public Pause(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);

        input.setMultiport(true);
        input.setTypeEquals(BaseType.BOOLEAN);
        output.setTypeEquals(BaseType.STRING);
        // Icon is a yield sign.
        _attachText("_iconDescription", "<svg>\n"
                + "<polygon points=\"0,24 24,-22 -24,-22\" "
                + "style=\"fill:red\"/>\n" + "<text x=\"-15\" y=\"-8\""
                + "style=\"font-size:11; fill:white; font-family:SansSerif\">"
                + "PAUSE</text>\n" + "</svg>\n");
        // Hide the name because the name is in the icon.
        new TransientSingletonConfigurableAttribute(this, "_hideName");
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** If any of the channels on the input port contain a true token,
     *  the cause postfire() to pause the Manager, display a dialog
     *  with a button to resume execution and send a true token.
     *  @exception IllegalActionException If there is a problem
     *  reading the input channels or sending the token to the output.
     */
    public void fire() throws IllegalActionException {
        super.fire();
        _sawTrueInput = false;
        if (!input.isOutsideConnected()) {
            _sawTrueInput = true;
        }
        // NOTE: We need to consume data on all channels that have data.
        // If we don't then DE will go into an infinite loop.
        for (int i = 0; i < input.getWidth(); i++) {
            if (input.hasToken(i)) {
                if (((BooleanToken) input.get(i)).booleanValue()) {
                    _sawTrueInput = true;
                }
            }
        }
    }
    

    /** If we read a true token, then pause the Manager until the
     *  user presses a button.  If nothing at all is connected to the
     *  input port, then pause unconditionaly.
     *  @return the value returned by the postfire() method in the
     *  superclass.  Typically, true is returned if the iteration
     *  may continue.  If stop was requested, then the superclass
     *  usually returns false.
     *  @exception IllegalActionException If there is no Manager,
     *  if the container is not a CompositeActor or if the
     *  there is some other problem.
     */
    public boolean postfire() throws IllegalActionException {
        if (_debugging) {
            _debug("Pause.postfire(): _sawTrueInput: " + _sawTrueInput);
        }
        if (_sawTrueInput) {
            // Create the UI each time we get a true on an input channel.
            _resumeButton = new JButton("Resume model execution.");
            _resumeButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (_debugging) {
                            _debug("Pause.actionPerformed()");
                        }
                        if (_manager != null) {
                            _debug("Pause.actionPerformed(): resuming manager");
                            _manager.resume();
                        } 
                        _frame.dispose();
                    }
                });

            _panel = new JPanel();
            _panel.add(_resumeButton);

            if (_container == null) {
                _frame = new JFrame(getFullName());
                _frame.getContentPane().add(_panel);
                _frame.setSize(200, 250);
            } else {
                _container.add(_panel);
                _panel.setBackground(null);
                _panel.setBorder(new LineBorder(Color.black));
            }
            _panel.setBorder(BorderFactory
                    .createTitledBorder("The border title goes here..."));

            if (_frame != null) {
                _frame.setVisible(true);
            }

            Nameable container = getContainer();
            if (container instanceof CompositeActor) {
                _manager = ((CompositeActor) container).getManager();
                if (_manager != null) {
                    if (_debugging) {
                        _debug("Pause.postfire(): pausing Manager()");
                    }
                    _manager.pause();
                } else {
                    throw new IllegalActionException(this,
                            "Cannot pause without a Manager.");
                }
            } else {
                throw new IllegalActionException(this,
                        "Cannot pause without a container that is a CompositeActor.");
            }
        }
        if (_debugging) {
            _debug("Pause.postfire(): _sawTrueInput: " + _sawTrueInput);
        }

        // It is safer to do output in postfire() in case this method
        // is inside a Continuous director.
        output.send(0, new BooleanToken(_sawTrueInput));
        return super.postfire();
    }

    /** Dispose the frame of the button dialog.
     *  @exception IllegalActionException Not thrown in this base class.
     */   
    public void wrapup() throws IllegalActionException {
        if (_frame != null) {
            _frame.dispose();
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected variables               ////

    protected JPanel _panel;

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    private Container _container;

    /** The frame into which to put the button, if any. */
    private JFrame _frame;

    private Manager _manager;

    private JButton _resumeButton;

    private boolean _sawTrueInput;
}
