/*
 * Copyright (c) 2012 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-07-20 11:55:10 -0700 (Fri, 20 Jul 2012) $' 
 * '$Revision: 30254 $'
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.kepler.gui.frame.TabbedKeplerGraphFrame;

import ptolemy.actor.gui.Tableau;
import ptolemy.kernel.util.NamedObj;

/** A Swing component on the tabs of composite actors.
 * 
 * @author Daniel Crawl
 * @version $Id: TabbedCompositeTabComponent.java 30254 2012-07-20 18:55:10Z crawl $
 *
 */

public class TabbedCompositeTabComponent extends JPanel implements ActionListener {

    public TabbedCompositeTabComponent(NamedObj model, Tableau tableau) {
        
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        
        setOpaque(false);
        
        _model = model;
        _modelName = model.getName(model.toplevel());
        _modelFullName = model.getFullName();
        _tableau = tableau;
        
        // add the name of the model
        JLabel label = new JLabel() {
            public String getText() {
                return _modelName;
            }
        };        
        add(label);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
        
        if(model.getContainer() != null) {
            // add a button for closing the model            
            _closeButton = new TabCloseButton();
            _closeButton.addActionListener(this);
            add(_closeButton);
        }
        
        setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
    }
    
    public void actionPerformed(ActionEvent event) {
        
        final Object source = event.getSource();
        if(source == _closeButton) {
            TabbedKeplerGraphFrame frame = (TabbedKeplerGraphFrame) ModelToFrameManager.getInstance().getFrame(_model);
            if(frame != null) {
                frame.removeComposite(_model);
            }
        }
    }
    
    public String getModelFullName() {
        return _modelFullName;
    }
    
    public Tableau getTableau() {
        return _tableau;
    }
    
    public void refreshName() {
        _modelName = _model.getName(_model.toplevel());
        _modelFullName = _model.getFullName();
        invalidate();
    }
    
    private class TabCloseButton extends JButton { //implements ActionListener {

        public TabCloseButton() {
            
            setToolTipText("Close this sub-workflow tab.");
            setOpaque(false);
            setPreferredSize(new Dimension(18, 18));
            setRolloverEnabled(true);
            setContentAreaFilled(false);
            setFocusable(false);
            setBorder(BorderFactory.createEtchedBorder());
            setBorderPainted(false);
            
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    Component component = e.getComponent();
                    if (component instanceof AbstractButton) {
                        AbstractButton button = (AbstractButton) component;
                        button.setBorderPainted(true);
                    }
                }

                public void mouseExited(MouseEvent e) {
                    Component component = e.getComponent();
                    if (component instanceof AbstractButton) {
                        AbstractButton button = (AbstractButton) component;
                        button.setBorderPainted(false);
                    }
                }
            });
        }

        /** Draw an X */
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            //shift the image for pressed buttons
            if (getModel().isPressed()) {
                g2.translate(1, 1);
            }
            g2.setStroke(new BasicStroke(2));
            g2.setColor(Color.BLACK);
            if (getModel().isRollover()) {
                g2.setColor(Color.RED);
            }
            int delta = 6;
            g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta - 1);
            g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta - 1);
            g2.dispose();
        }

    }

    private String _modelName;
    private NamedObj _model;
    private JButton _closeButton;
    private Tableau _tableau;
    private String _modelFullName;

}
