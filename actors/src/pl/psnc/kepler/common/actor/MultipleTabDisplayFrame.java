/* Dialog window used for showing messages within MultiTabDisplay dialog
 * 
 * Copyright (c) 2010 FP7 EU EUFORIA (211804) & POZNAN SUPERCOMPUTING AND
 * NETWORKING CENTER All rights reserved.
 *
 * Permission is hereby granted, without written agreement and without license
 * or royalty fees, to use, copy, modify, and distribute this software and its
 * documentation for any purpose, provided that the above copyright notice and
 * the following two paragraphs appear in all copies of this software.
 *
 * IN NO EVENT SHALL THE FP7 EU EUFORIA (211804) & POZNAN SUPERCOMPUTING AND
 * NETWORKING CENTER BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL,
 * INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OF THIS SOFTWARE
 * AND ITS DOCUMENTATION, EVEN IF THE FP7 EU EUFORIA (211804) & POZNAN
 * SUPERCOMPUTING AND NETWORKING CENTER HAS BEEN ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * THE FP7 EU EUFORIA (211804) & POZNAN SUPERCOMPUTING AND NETWORKING CENTER
 * SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 * THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE FP7 EU
 * EUFORIA (211804) & POZNAN SUPERCOMPUTING AND NETWORKING CENTER HAS NO
 * OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR
 * MODIFICATIONS.
 *
 */

package pl.psnc.kepler.common.actor;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicButtonUI;

import ptolemy.gui.Top;

//////////////////////////////////////////////////////////////////////////
////MultiTabDisplayFrame
class MultipleTabDisplayComponent extends JPanel {
    private class TabButton extends JButton implements ActionListener {
        private static final long serialVersionUID = 1L;

        public TabButton() {
            int size = 17;
            setPreferredSize(new Dimension(size, size));
            setToolTipText("Close this tab");
            // Make the button looks the same for all Laf's
            setUI(new BasicButtonUI());
            // Make it transparent
            setContentAreaFilled(false);
            // No need to be focusable
            setFocusable(false);
            setBorder(BorderFactory.createEtchedBorder());
            setBorderPainted(false);
            // Making nice rollover effect
            // we use the same listener for all buttons
            addMouseListener(MultipleTabDisplayComponent
                    .getButtonMouseListener());
            setRolloverEnabled(true);
            // Close the proper tab by clicking the button
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            getFrame().remove(getTitle());
        }

        // paint the cross
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            // shift the image for pressed buttons
            if (getModel().isPressed()) {
                g2.translate(1, 1);
            }
            g2.setStroke(new BasicStroke(2));
            g2.setColor(getColor());
            if (getModel().isRollover()) {
                g2.setColor(Color.MAGENTA);
            }
            int delta = 6;
            g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight()
                    - delta - 1);
            g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight()
                    - delta - 1);
            g2.dispose();
        }
    }

    private static final long serialVersionUID = 1L;

    private final static MouseListener buttonMouseListener = new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(true);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(false);
            }
        }
    };

    // /////////////////////////////////////////////////////////////////
    // // public methods ////
    public static MouseListener getButtonMouseListener() {
        return MultipleTabDisplayComponent.buttonMouseListener;
    }

    /** parent frame */
    private final MultipleTabDisplayFrame frame;
    /** title of the tab */
    private String title;
    /** determines whether tab is currently shown or not */
    private boolean tabSelected = false;
    /** current color used for painting the cross-hair */
    private Color color = Color.BLACK;

    public MultipleTabDisplayComponent(final MultipleTabDisplayFrame frame) {
        // unset default FlowLayout' gaps
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        if (frame == null) {
            throw new NullPointerException("TabbedPane is null");
        }
        this.frame = frame;
        setOpaque(false);

        // make JLabel read titles from JTabbedPane
        JLabel label = new JLabel() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getText() {
                if (getTitle() != null) {
                    return getTitle();
                }
                int i = frame
                        .indexOfTabComponent(MultipleTabDisplayComponent.this);
                if (i != -1) {
                    return frame.getTitleAt(i);
                }
                return null;
            }
        };

        add(label);
        // add more space between the label and the button
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        // tab button
        JButton button = new TabButton();
        add(button);
        // add more space to the top of the component
        setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
    }

    public MultipleTabDisplayComponent(final MultipleTabDisplayFrame frame,
            String title) {
        this(frame);
        setTitle(title);
    }

    public Color getColor() {
        return color;
    }

    public MultipleTabDisplayFrame getFrame() {
        return frame;
    }

    public String getTitle() {
        return title;
    }

    /**
     * Notify component that message was sent to tab
     * 
     * This allows component to change color of the crosshair in case of tab
     * being deselected
     */
    public void messageSent() {
        if (tabSelected == false) {
            setColor(Color.RED);
        }
        repaint();
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void tabDeselected() {
        tabSelected = false;
    }

    public void tabSelected() {
        tabSelected = true;
        setColor(Color.BLACK);
    }
}

/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * - Neither the name of Oracle or the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior
 * written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

// ////////////////////////////////////////////////////////////////////////
// //ButtonTabComponent

/**
 * This class allows showing messages within multi display dialog.
 * 
 * Each message can be shown within different Tab. This way, users are presented
 * with information in more convenient way.
 * 
 * This class implements Singleton pattern
 * 
 * 
 * @author Michal Owsiak - michalo [at] man.poznan.pl
 * @version $Id:$
 * 
 */
@SuppressWarnings("serial")
public class MultipleTabDisplayFrame extends Top {
    // /////////////////////////////////////////////////////////////////
    // // public variables ////
    // revision value used for getting current class revision
    public static final String REVISION = "$Revision: 2353 $";

    // /////////////////////////////////////////////////////////////////
    // // public methods ////
    /**
     * Retrieves MultiTabDisplay frame for given workflow name
     * 
     * @param workflowName
     *            Name of the workflow that frame should be bound to
     * @return MultiTabDisplay object
     */
    public static MultipleTabDisplayFrame getInstance(String workflowName) {
        MultipleTabDisplayFrame instance;
        // if MultiTabDisplay for given workflow exists - return it
        // otherwise, create new MultiTabDisplay and bound it to given workflow
        if (MultipleTabDisplayFrame.instances.containsKey(workflowName) == true) {
            instance = MultipleTabDisplayFrame.instances.get(workflowName);
        } else {
            instance = new MultipleTabDisplayFrame(workflowName);

            URL rsrcUrl = Thread.currentThread().getContextClassLoader()
                    .getResource("ptolemy/configs/kepler/KeplerSmallIcon.gif");
            Image img;
            try {
                img = ImageIO.read(rsrcUrl);
                instance.setIconImage(img);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            instance.setTitle("Multiple Tab Display Dialog - " + workflowName);
            MultipleTabDisplayFrame.instances.put(workflowName, instance);
        }
        return instance;
    }

    /** contains JTabbedPanes for given instance of the MultiTabDisplay */
    private Hashtable<String, JTextArea> infoTable;
    /** contains all panels stored within JTabPanel */
    private Hashtable<String, JPanel> tabPanels;
    /** contains index of currently selected tab */
    private int tabIdx = -1;
    /** contains all the instances of MultiTabDisplay objects */
    private static Hashtable<String, MultipleTabDisplayFrame> instances = new Hashtable<String, MultipleTabDisplayFrame>();
    // Variables declaration - do not modify
    private JPanel jPanel;
    private JTabbedPane jTabbedPane = new JTabbedPane();

    /**
     * This class can not be instanced directly. It implements singleton pattern
     * use <it>getInstance</it> instead
     */
    private MultipleTabDisplayFrame() {
    }

    /** Creates new form MultiTabDisplayFrame */
    private MultipleTabDisplayFrame(String parent) {
        super();

        setTitle(parent);
        infoTable = new Hashtable<String, JTextArea>();
        tabPanels = new Hashtable<String, JPanel>();

        // ptolemy.gui.Top windows always has a menu bar and a status bar,
        // explicitly disable them
        _statusBar = null;
        hideMenuBar();

        initComponents();
    }

    /**
     * Add JPanel with given name into MultiTabDisplay
     * 
     * This methods allows adding virtually any JPanel into MultiTabDisplay.
     * This way, it is possible to add, for example, Interactive channel into
     * MultiTabDisplay
     * 
     * @param name
     *            Name of the panel
     * @param panel
     *            JPanel to be added
     * @throws IllegalArgumentException
     *             Thrown when there is already panel existing with given name
     */
    public void addJPanel(String name, JPanel panel)
            throws IllegalArgumentException {
        if (jTabbedPane.indexOfTab(name) == -1) {
            jTabbedPane.add((String) null, panel);
            tabPanels.put(name, panel);
        } else {
            throw new IllegalArgumentException("Tab with specifed name: ["
                    + name + "] already exists");
        }
    }

    /**
     * Add text into specified display and put new line before text is inserted
     * 
     * @param displayName
     *            Display to be used
     * @param text
     *            Text to be shown
     */
    public void addText(String displayName, String text) {
        addText(displayName, text, true);
    }

    /**
     * Add text into specified display
     * 
     * @param displayName
     *            Display to be used
     * @param text
     *            Text to be shown
     * @param newLine
     *            determines whether new line character should be printed before
     *            each new line added to TextArea
     */
    public void addText(String displayName, String text, boolean newLine) {
        if (getContentPane().getComponents().length == 0) {
            infoTable.clear();
            jTabbedPane = new JTabbedPane();
            initComponents();
        }

        // If there is display with given name, we can use it
        // otherwise, we have to create new JTabbedPane and new JTextArea
        if (infoTable.containsKey(displayName)) {
            JTextArea area = infoTable.get(displayName);
            area.append(text + (newLine ? "\n" : ""));
            area.setCaretPosition(area.getText().length());
        } else {
            JTextArea area = new JTextArea();
            area.setEditable(false);
            area.setLineWrap(true);
            area.setText(text + (newLine ? "\n" : ""));
            area.setWrapStyleWord(true);

            JScrollPane scrollPane = new JScrollPane(area);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;

            JPanel panel = new JPanel();
            panel.setLayout(new GridBagLayout());
            panel.add(scrollPane, gbc);

            jTabbedPane.add((String) null, panel);
            tabPanels.put(displayName, panel);
            infoTable.put(displayName, area);

            initTabComponent(panel, displayName);
        }

        int idx = jTabbedPane.indexOfComponent(tabPanels.get(displayName));
        if (idx != -1) {
            MultipleTabDisplayComponent component = getTabComponent(idx);
            if (component != null) {
                component.messageSent();
            }
        }
    }

    /**
     * Clean up all displays apart from "Info"
     */
    private void cleanUpDisplay() {
        Iterator<String> it = infoTable.keySet().iterator();
        ArrayList<String> list = new ArrayList<String>();
        while (it.hasNext()) {
            String key = it.next();
            list.add(key);
        }

        for (int i = 0; i < list.size(); i++) {
            String key = list.get(i);
            remove(key);
        }
    }

    @Override
    public void dispose() {
        setVisible(false);
    }

    public Container getPane() {
        return getContentPane();
    }

    // retrieve renderer for given TAB
    MultipleTabDisplayComponent getTabComponent(int idx) {
        if (jTabbedPane.getTabCount() > 0 && idx < jTabbedPane.getTabCount()) {
            return (MultipleTabDisplayComponent) jTabbedPane
                    .getTabComponentAt(idx);
        }
        return null;
    }

    /**
     * Return title of given component at specified index
     * 
     * @param idx
     *            Index of component
     * @return title of given component
     */
    public String getTitleAt(int idx) {
        return jTabbedPane.getTitleAt(idx);
    }

    /**
     * Return index of given Component
     * 
     * This method allows to retrieve index of given Component
     * 
     * @param c
     *            Component to be located within TabPane
     * @return index of the component
     */
    public int indexOfTabComponent(Component c) {
        return jTabbedPane.indexOfTabComponent(c);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        jPanel = new JPanel();
        jPanel.setLayout(new GridBagLayout());

        getContentPane().add(jTabbedPane, BorderLayout.CENTER);

        jTabbedPane.getAccessibleContext().setAccessibleName("Info");
        jTabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                // If tab changes, we have to mark it as "read"
                JTabbedPane tabSource = (JTabbedPane) e.getSource();
                int idx = tabSource.getSelectedIndex();

                if (tabIdx != -1) {
                    MultipleTabDisplayComponent component = getTabComponent(tabIdx);
                    if (component != null) {
                        component.tabDeselected();
                    }
                }

                MultipleTabDisplayComponent component = getTabComponent(idx);
                if (component != null) {
                    component.tabSelected();
                }
                tabIdx = idx;
            }
        });

        jTabbedPane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);

        setMinimumSize(new Dimension(500, 200));
        setPreferredSize(new Dimension(500, 200));

        Container parent = null;
        do {
            parent = getParent();
        } while (parent != null && !(parent instanceof JFrame));

        if (parent != null && parent instanceof JFrame) {
            setIconImage(((JFrame) parent).getIconImage());
        }

        pack();
    }// </editor-fold>

    /**
     * Perform frame specific initialization
     */
    public void initialize() {
        cleanUpDisplay();

        // Bring frame to front
        if (getState() != Frame.NORMAL) {
            setState(Frame.NORMAL);
        }
        toFront();
        repaint();
    }

    // initialize tab component with specified Tab rendered
    private void initTabComponent(Component c, String name) {
        int idx = jTabbedPane.indexOfComponent(c);
        jTabbedPane.setTabComponentAt(idx, new MultipleTabDisplayComponent(
                this, name));
    }

    /**
     * Remove panel with given name
     * 
     * @param name
     */
    public void remove(String name) {
        JPanel panel = tabPanels.get(name);

        if (panel != null) {
            infoTable.remove(name);
            tabPanels.remove(name);
            jTabbedPane.remove(panel);
        }
    }

    /**
     * Remove JPanel from MultiTabDisplayFrame
     * 
     * This method allows removing given JPanel from the MultiTabDisplayFrame
     * through the API instead of UI.
     * 
     * @param name
     *            Name of the panel to be removed
     * @throws IllegalArgumentException
     *             Thrown if there is no panel with given name
     */
    public void removeJPanel(String name) throws IllegalArgumentException {
        int idx = -1;
        if ((idx = jTabbedPane.indexOfTab(name)) == -1) {
            throw new IllegalArgumentException("Tab with specifed name: ["
                    + name + "] does not exist");
        }
        jTabbedPane.remove(idx);
        infoTable.remove(name);
        tabPanels.remove(name);
    }

    public static boolean isInstance(String workflowName) {
        return MultipleTabDisplayFrame.instances.containsKey(workflowName);
    }

    public static void removeInstance(String workflowName) {
        MultipleTabDisplayFrame.instances.remove(workflowName);
    }

    @Override
    protected void _read(URL url) throws Exception {
        // do nothing
    }

    @Override
    protected void _writeFile(File file) throws IOException {
        // do nothing
    }
}
