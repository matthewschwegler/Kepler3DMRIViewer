/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * @author Chad Berkley
 * This class is work in progress.  It may not function.
 * A class to allow users to add a new remote repository
 */
public class AddRemoteRepositoryDialog extends JDialog 
{
  JButton okButton;
  JButton cancelButton;
  JPanel topPanel;
  JTextField repositoryNameTextArea;
  JTextField repositoryServerTextArea;
  
  /**
   * constructor
   */
  public AddRemoteRepositoryDialog(Frame owner, String title, Boolean modal)
  {
    super(owner, title, modal);
    System.out.println("AddRemoteRepositoryDialog opening.");
    init();
  }
  
  public void init() 
  { 
    topPanel = new JPanel();
    
    okButton = new JButton("Ok");
    okButton.setPreferredSize(new Dimension(100, 50));
    okButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e)
      {
        
      }
    });
    
    cancelButton = new JButton("Cancel");
    cancelButton.setPreferredSize(new Dimension(100, 50));
    cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e)
      {
        setVisible(false);
      }
    });
    
    topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
    
    repositoryNameTextArea = new JTextField();
    repositoryNameTextArea.setMaximumSize(new Dimension(200, 20));
    
    repositoryServerTextArea = new JTextField();
    repositoryServerTextArea.setMaximumSize(new Dimension(200, 20));
    
    JLabel nameLabel = new JLabel("Repository Name");
    JLabel serverLabel = new JLabel("Repository Server");
    nameLabel.setMaximumSize(new Dimension(150, 20));
    serverLabel.setMaximumSize(new Dimension(150, 20));
    
    JPanel namePanel = new JPanel();
    namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.X_AXIS));
    namePanel.add(Box.createRigidArea(new Dimension(5,0)));
    namePanel.add(nameLabel);
    namePanel.add(repositoryNameTextArea);
    namePanel.add(Box.createRigidArea(new Dimension(5,0)));
    
    JPanel serverPanel = new JPanel();
    serverPanel.add(Box.createRigidArea(new Dimension(5,0)));
    serverPanel.setLayout(new BoxLayout(serverPanel, BoxLayout.X_AXIS));
    serverPanel.add(serverLabel);
    serverPanel.add(repositoryServerTextArea);
    serverPanel.add(Box.createRigidArea(new Dimension(5,0)));
    
    JPanel inputsPanel = new JPanel();
    inputsPanel.setLayout(new BoxLayout(inputsPanel, BoxLayout.Y_AXIS));
    inputsPanel.setMaximumSize(new Dimension(350, 50));
    inputsPanel.add(Box.createRigidArea(new Dimension(0,5)));
    inputsPanel.add(namePanel);
    inputsPanel.add(Box.createRigidArea(new Dimension(0,5)));
    inputsPanel.add(serverPanel);
    inputsPanel.add(Box.createRigidArea(new Dimension(0,5)));
    
    topPanel.add(Box.createRigidArea(new Dimension(0,5)));
    topPanel.add(inputsPanel);
    topPanel.add(Box.createRigidArea(new Dimension(0,5)));
    
    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
    //buttonPanel.setMaximumSize(new Dimension(350, 40));
    buttonPanel.add(okButton);
    buttonPanel.add(cancelButton);
    topPanel.add(buttonPanel);
    
    
    setSize(new Dimension(370, 150));
    
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setContentPane(topPanel);
  }
  
  private void writeConfig()
  {
    
  }
}