/*
 * Copyright (c) 2010-2011 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-05-09 11:05:40 -0700 (Wed, 09 May 2012) $' 
 * '$Revision: 29823 $'
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

package org.kepler.gui.memory;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import org.kepler.build.project.MemoryProperties;

/**
 * Created by David Welker.
 * Date: 12/21/10
 * Time: 5:35 PM
 */
public class JvmMemoryPanel extends JPanel
{
    private JTextField minMemoryField;
    private JTextField maxMemoryField;
    private JTextField stackSizeField;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JDialog owner;

    public JvmMemoryPanel(JDialog owner)
    {
        super(new GridBagLayout());
        this.owner = owner;
        owner.setTitle("JVM Memory Settings");
        owner.setModal(true);
        initComponents();
        layoutComponents();
        owner.getRootPane().setDefaultButton(buttonOK);

        owner.addWindowFocusListener(new WindowAdapter()
        {
            @Override
            public void windowGainedFocus(WindowEvent e)
            {
                buttonOK.requestFocusInWindow();
            }
        });


        buttonOK.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                onCancel();
            }
        });


        owner.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        owner.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                onCancel();
            }
        });

        registerKeyboardAction(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

    }

    private void onOK()
    {
        MemoryProperties.setMinMemory(minMemoryField.getText());
        MemoryProperties.setMaxMemory(maxMemoryField.getText());
        MemoryProperties.setStackSize(stackSizeField.getText());
        owner.dispose();
    }

    private void onCancel()
    {
        owner.dispose();
    }

    private void initComponents()
    {
        minMemoryField = new JTextField(MemoryProperties.getMinMemory());
        maxMemoryField = new JTextField(MemoryProperties.getMaxMemory());
        stackSizeField = new JTextField(MemoryProperties.getStackSize());
        buttonOK = new JButton("OK");
        buttonCancel = new JButton("Cancel");
    }

    private void layoutComponents()
    {
        GridBagConstraints c = new GridBagConstraints();

        JLabel minMemoryLabel = new JLabel("Min Memory:");
        minMemoryField.setColumns(16);
        JLabel maxMemoryLabel = new JLabel("Max Memory:");
        maxMemoryField.setColumns(16);
        JLabel stackSizeLabel = new JLabel("Stack Size:");
        stackSizeField.setColumns(16);

        JLabel note = new JLabel("Note: You must restart Kepler for memory changes to take effect.");
        JPanel okCancel = new JPanel();
        okCancel.add(buttonOK);
        okCancel.add(buttonCancel);

        c.insets = new Insets(5,10,0,0);
        //c.ipady = 200;
        c.gridx = 0;
        c.gridy = 0;
        add(minMemoryLabel, c);
        
        c.gridx = 1;
        add(minMemoryField, c);
        
        c.gridx = 0;
        c.gridy = 1;
        add(maxMemoryLabel, c);
        
        c.gridx = 1;
        add(maxMemoryField, c);
        
        c.gridx = 0;
        c.gridy = 2;
        add(stackSizeLabel, c);
        
        c.gridx = 1;
        add(stackSizeField, c);
        
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 3;
        add(note, c);
        
        c.gridx = 2;
        c.gridy = 4;
        add(okCancel, c);
    }

    public static void main(String[] args)
    {
        JDialog dialog = new JDialog();
        dialog.setContentPane(new JvmMemoryPanel(dialog));
        dialog.pack();
        dialog.setVisible(true);
        //System.exit(0);
    }


}
