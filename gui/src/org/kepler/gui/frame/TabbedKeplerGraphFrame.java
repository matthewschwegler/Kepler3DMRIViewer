/*
 * Copyright (c) 2012 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-07-20 11:56:15 -0700 (Fri, 20 Jul 2012) $' 
 * '$Revision: 30255 $'
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

package org.kepler.gui.frame;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.kepler.gui.KeplerGraphFrame;
import org.kepler.gui.ModelToFrameManager;
import org.kepler.gui.TabManager;
import org.kepler.gui.TabPane;
import org.kepler.gui.TabbedCompositeTabComponent;
import org.kepler.gui.WorkflowOutlineTabPane;
import org.kepler.moml.NamedObjIdChangeRequest;
import org.kepler.objectmanager.ObjectManager;

import ptolemy.actor.CompositeActor;
import ptolemy.actor.gui.Tableau;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.ChangeRequest;
import ptolemy.kernel.util.NamedObj;
import ptolemy.moml.LibraryAttribute;
import ptolemy.moml.MoMLChangeRequest;
import ptolemy.util.MessageHandler;
import ptolemy.vergil.actor.ActorEditorGraphController;
import ptolemy.vergil.actor.ActorGraphModel;
import ptolemy.vergil.basic.AbstractBasicGraphModel;
import ptolemy.vergil.basic.EditorDropTarget;
import diva.canvas.event.LayerAdapter;
import diva.canvas.event.LayerEvent;
import diva.graph.GraphModel;
import diva.graph.GraphPane;
import diva.graph.JGraph;

/** This is a graph editor frame for actors containing composite actors.
  
 TODO:

    go to actor in exception dialog does not switch tabs
    fix NamedObjId errors when closing (KGF calls dispose() twice, second time getModel() returns null)
    frame close button closes tab instead of frame
    fix for case/contingency actors.
    add button to tabs to make separate window
    nest tabs for nested composites?
    
 DONE:

    add non-composites to tab: atomic actor source code
        difficult since not opened in graph frame
    does not save window size? see bug http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5637
    do not place in tabbed pane when no other composites open
    canvas scroll bars missing
        fix: update scrollbars to use selected sub-workflow in stateChanged()
    update outline tree when selected tab changes
        fix: refresh outline pane in stateChanged()
    change color of X for closing tab on rollover
    new in/out port buttons on toolbar place ports in top level instead of subwf
        fix: uncomment TabbedKeplerController.getGraphModel(), see next fix.
    one actor in composites rendered wrong when opened the first time
        fix: in _createGraphPane
    resize tab component on renames
        fix: call invalidate() in TabbedCompositeTabComponent.refreshName()
    cmd-l on composite not already opened should open in new window, not tab
        fix: added LookInsideTabbedComposite.actionIsRunning()
    rename tabs when composite names are changed
    rename tabs when parent composite is renamed
    delete tabs when composite is deleted
    delete tabs when composite parent is deleted
    ports are displayed wrong (probably controller)
        fix: updated ActorEditorGraphController to add port layout listener when using alternative actor controllers
    when tab changes, panner is repainted, but panner does not show actor highlight changes
        (same behavior in ptolemy; no fix)
    file->close / cmd-w should close selected tab
    open composite on one already opened set selected tab
    add X button to tabs to close
    close tab via button, then can't open again (need to close tableau?)
    frame title is wrong: seems to be set to last opened composite
    focus stops working (fix: use MultiCompositeController from CaseGraphFrame?)
    cmd-l on already opened composite should switch to tab
    clean up LookInsideAction, controller stuff

    @author Daniel Crawl
    @version $Id: TabbedKeplerGraphFrame.java 30255 2012-07-20 18:56:15Z crawl $

*/
public class TabbedKeplerGraphFrame extends KeplerGraphFrame implements ChangeListener {
    /** Construct a frame associated with the specified composite actor.
     *  After constructing this, it is necessary
     *  to call setVisible(true) to make the frame appear.
     *  This is typically done by calling show() on the controlling tableau.
     *  This constructor results in a graph frame that obtains its library
     *  either from the model (if it has one) or the default library defined
     *  in the configuration.
     *  @see Tableau#show()
     *  @param entity The model to put in this frame.
     *  @param tableau The tableau responsible for this frame.
     */
    public TabbedKeplerGraphFrame(CompositeActor entity, Tableau tableau) {
        this(entity, tableau, null);
    }

    /** Construct a frame associated with the specified composite actor.
     *  After constructing this, it is necessary
     *  to call setVisible(true) to make the frame appear.
     *  This is typically done by calling show() on the controlling tableau.
     *  This constructor results in a graph frame that obtains its library
     *  either from the model (if it has one), or the <i>defaultLibrary</i>
     *  argument (if it is non-null), or the default library defined
     *  in the configuration.
     *  @see Tableau#show()
     *  @param entity The model to put in this frame.
     *  @param tableau The tableau responsible for this frame.
     *  @param defaultLibrary An attribute specifying the default library
     *   to use if the model does not have a library.
     */
    public TabbedKeplerGraphFrame(CompositeActor entity, Tableau tableau,
            LibraryAttribute defaultLibrary) {
        super(entity, tableau, defaultLibrary);        
     }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////
    
    /** Add a new tab for a model. */
    public void addComposite(CompositeEntity model,
            TabbedKeplerGraphTableau keplerGraphMultiCompositeTableau,
            LibraryAttribute defaultLibrary) {

        JGraph jgraph = _addCompositeToPane(model, keplerGraphMultiCompositeTableau);
        ((TabbedKeplerController) _controller)._addHotKeys(jgraph);
        
    }
    
    /** Listen for name changes in the model. */
    public void changeExecuted(ChangeRequest change) {
        
        //System.out.println(change);
        
        if(_tabbedPane != null && (change instanceof MoMLChangeRequest) &&
            !(change instanceof NamedObjIdChangeRequest)) {
            //System.out.println("source = " + change.getSource());
            //System.out.println("context = " + ((MoMLChangeRequest)change).getContext());
            //System.out.println("description = " + change.getDescription());
            
            final NamedObj context = ((MoMLChangeRequest)change).getContext();
            final String description = change.getDescription();
            
            // see if something was renamed
            Matcher matcher = _MOML_RENAME_PATTERN.matcher(description);            
            if(matcher.find()) {
                //System.out.println("g1 = " + matcher.group(1) + " g2 = " + matcher.group(2));
                // get the previous name
                final String oldName = matcher.group(1);
                
                // construct the full name
                final String oldRenamedFullName = context.getFullName() + "." + oldName;
                // find the tab (if one exists) for the renamed composite.
                for(int i = 0; i < _tabbedPane.getTabCount(); i++) {
                    final TabbedCompositeTabComponent component = 
                        (TabbedCompositeTabComponent) _tabbedPane.getTabComponentAt(i);
                    final String componentFullName = component.getModelFullName();
                    if(componentFullName.startsWith(oldRenamedFullName)) {
                        component.refreshName();
                    }
                }              
            } else {
                // see if something was deleted
                matcher = _MOML_DELETE_PATTERN.matcher(description);
                if(matcher.find()) {
                    // get the item that was deleted
                    final String deletedName = matcher.group(1);

                    // construct the full name of the item deleted
                    final String deleteFullName = context.getFullName() + "." + deletedName;
                    final Set<Component> tabsToDelete = new HashSet<Component>();
                    for(int i = 0; i < _tabbedPane.getTabCount(); i++) {
                        final TabbedCompositeTabComponent tabComponent = 
                            (TabbedCompositeTabComponent) _tabbedPane.getTabComponentAt(i);
                        final String componentFullName = tabComponent.getModelFullName();
                        // find the tab (if one exists) for the deleted composite.
                        // also find any tabs that are models contained in the delete composite.
                        if(componentFullName.startsWith(deleteFullName)) {
                            tabsToDelete.add(_tabbedPane.getComponentAt(i));
                        }
                    }
                        
                    // remove the tabs
                    for(Component component : tabsToDelete) {
                        _removeTab(component);
                    }                    
                }
            }
        }
        
        // tell the parent class about the change
        // NOTE: this is necessary since KeplerGraphFrame.changeExecuted()
        // sets the frame as modified and repaints the jgraph and panner
        super.changeExecuted(change);
    }

    /** Free resources when closing. */
    public void dispose() {
        
        final ObjectManager objectManager = ObjectManager.getInstance();
        final ModelToFrameManager modelToFrameManager = ModelToFrameManager.getInstance();

        // remove the sub-workflows in tabs from the object manager and
        // model to frame manager.
        if(_tabbedPane != null) {
            for(Component component : _tabbedPane.getComponents()) {
                // only look at jgraphs
                if(component instanceof JGraph) {
                    final JGraph jgraph = (JGraph) component;
                    final NamedObj modelInTab = 
                        ((AbstractBasicGraphModel)jgraph.getGraphPane().getGraphModel()).getPtolemyModel();
                    // make sure it's not the top-level workflow, which is
                    // removed by KeplerGraphFrame
                    if(modelInTab.getContainer() != null) {
                        objectManager.removeNamedObjs(modelInTab);
                        modelToFrameManager.removeModel(modelInTab);                    
                    }
                }
            }
        }
        
        super.dispose();
    }
    
    /** Remove a workflow from the frame. */
    public void removeComposite(NamedObj model) {
        Component component = _findComponentForModel(model);
        if(component != null) {
            _removeTab(component);
        }
    }
    
    /** Set the selected tab to the tab containing a specific model. */
    public void setSelectedTab(NamedObj model) {
        Component component = _findComponentForModel(model);
        if(component != null) {
            _tabbedPane.setSelectedComponent(component);
        }
    }
    
    /** React to a change in the state of the tabbed pane.
     *  @param event The event.
     */
    public void stateChanged(ChangeEvent event) {
        final Object source = event.getSource();
        if (source instanceof JTabbedPane) {
            final Component selected = ((JTabbedPane) source).getSelectedComponent();
            if (selected instanceof JGraph) {
                final JGraph jgraph = (JGraph) selected;
                setJGraph(jgraph);
                selected.requestFocus();        
                //System.out.println("changed focus");

                if (_graphPanner != null) {
                    _graphPanner.setCanvas(jgraph);
                    _graphPanner.repaint();
                    //System.out.println("repainted panner");
                }
                
                // update the outline tab, if present, to be rooted at the
                // selected sub-workflow
                TabManager manager = TabManager.getInstance();
                TabPane outlinePane = manager.getTab(this, WorkflowOutlineTabPane.class);
                if(outlinePane != null) {
                    final NamedObj modelInTab = 
                        ((AbstractBasicGraphModel)jgraph.getGraphPane().getGraphModel()).getPtolemyModel();
                    ((WorkflowOutlineTabPane)outlinePane).setWorkflow((CompositeEntity) modelInTab);
                }
                
                // update the scrollbars to use the selected sub-workflow
                if(_horizontalScrollBar != null) {
                    _horizontalScrollBar.setModel(_jgraph.getGraphPane().getCanvas()
                        .getHorizontalRangeModel());
                }
                if(_verticalScrollBar != null) {
                    _verticalScrollBar.setModel(_jgraph.getGraphPane().getCanvas()
                        .getVerticalRangeModel());
                }

            }
        }
    }
        
    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Create a new graph pane. Note that this method is called in
     *  constructor of the base class, so it must be careful to not reference
     *  local variables that may not have yet been created.
     *  This overrides the base class to create a specialized
     *  graph controller (an inner class).
     *  @param entity The object to be displayed in the pane.
     *  @return The pane that is created.
     */
    protected GraphPane _createGraphPane(NamedObj entity) {

        
        ActorEditorGraphController controller;

        // the first time this is called, set the _controller.
        // otherwise, use an ActorEditorGraphController for the controller.
        // NOTE: if the controller is always set to a TabbedKeplerController,
        // it causes one actor to always be rendered twice on the canvas the
        // first time the tab is opened.
        
        if(_controller == null) {
            _controller = new TabbedKeplerController();
            controller = _controller;
        } else {
            controller = new ActorEditorGraphController();
        }
        
        controller.setConfiguration(getConfiguration());
        controller.setFrame(this);
        
        // The cast is safe because the constructor only accepts
        // CompositeEntity.
        final ActorGraphModel graphModel = new ActorGraphModel(entity);
        return new GraphPane(controller, graphModel);
    }

    /** Create the component that goes to the right of the library.
     *  NOTE: This is called in the base class constructor, before
     *  things have been initialized. Hence, it cannot reference
     *  local variables.
     *  @param entity The entity to display in the component.
     *  @return The component that goes to the right of the library.
     */
    protected JComponent _createRightComponent(NamedObj entity) {
        if (!(entity instanceof CompositeActor)) {
            return super._createRightComponent(entity);
        }

        _panel = new JPanel(new BorderLayout());
        
        final JGraph jgraph = 
            _addCompositeToPane((CompositeEntity) entity, (TabbedKeplerGraphTableau) getTableau());
        
        if(entity.getContainer() == null) {
            
            setJGraph(jgraph);
            
            // listen for changes so that we can change titles when the 
            // composite name changes, or close tabs when composites are
            // deleted from the model.
            entity.addChangeListener(this);
        }

        return _panel;
        
        /*
        _panel = new JPanel(new GridLayout() {
            public Dimension minimumLayoutSize(Container parent) {
                return _tabbedPane.getMinimumSize();
            }
            public Dimension preferredLayoutSize(Container parent) {
                return _tabbedPane.getPreferredSize();
            }
        });
        
        _panel.add(_tabbedPane);        

        return _panel;
        */
    }
    

    ///////////////////////////////////////////////////////////////////
    ////                         private methods                   ////

    /** Add a tabbed pane for the specified composite.
     *  @param composite The composite.
     *  @param newPane True to add the pane prior to the last pane.
     *  @return The pane.
     */
    protected JGraph _addCompositeToPane(CompositeEntity composite,
            TabbedKeplerGraphTableau keplerGraphMultiCompositeTableau) {
        GraphPane pane = _createGraphPane(composite);
        pane.getForegroundLayer().setPickHalo(2);
        pane.getForegroundEventLayer().setConsuming(false);
        pane.getForegroundEventLayer().setEnabled(true);
        pane.getForegroundEventLayer().addLayerListener(new LayerAdapter() {
            /** Invoked when the mouse is pressed on a layer
             * or figure.
             */
            public void mousePressed(LayerEvent event) {
                Component component = event.getComponent();

                if (!component.hasFocus()) {
                    component.requestFocus();
                    //System.out.println("set focus to " + component);
                }
            }
        });
        JGraph jgraph = new JGraph(pane);
        String name = composite.getName();
        // if the composite does not have a name, use the effigy's name
        if(name.isEmpty()) {
            name = _getName();
        }
        jgraph.setName(name);

        // see if this is the first composite to be added
        if(_panel.getComponentCount() == 0) {
            _panel.add(jgraph, BorderLayout.CENTER);
            _rootComposite = composite;
            _rootTableau = keplerGraphMultiCompositeTableau;
        } else {
            
            if(_tabbedPane == null) {
                
                _createJTabbedPane();
                
                // remove the first workflow from the pane and put in the tab
                final JGraph firstJgraph = (JGraph) _panel.getComponent(0);
                _panel.remove(firstJgraph);
                _tabbedPane.add(firstJgraph, BorderLayout.CENTER);
                int index = _tabbedPane.indexOfComponent(firstJgraph);
                TabbedCompositeTabComponent tabComponent =
                    new TabbedCompositeTabComponent(_rootComposite, _rootTableau);
                _tabbedPane.setTabComponentAt(index, tabComponent);
                _tabbedPane.setSelectedIndex(index);
                _panel.add(_tabbedPane, BorderLayout.CENTER);
            }
            
            // add the new workflow
            _tabbedPane.add(jgraph, BorderLayout.CENTER);
            int index = _tabbedPane.indexOfComponent(jgraph);
            TabbedCompositeTabComponent tabComponent =
                new TabbedCompositeTabComponent(composite, keplerGraphMultiCompositeTableau);
            _tabbedPane.setTabComponentAt(index, tabComponent);
            _tabbedPane.setSelectedIndex(index);
        }
        
        jgraph.setBackground(BACKGROUND_COLOR);
        // Create a drop target for the jgraph.
        // FIXME: Should override _setDropIntoEnabled to modify all the drop targets created.
        new EditorDropTarget(jgraph);
        
        ModelToFrameManager m2fm = ModelToFrameManager.getInstance();
        m2fm.add(composite, this);
        
        return jgraph;
    }
        
    /** Close the tab if it is not the top-level model. Otherwise, close the frame. */
    protected boolean _close() {
        
        if(_tabbedPane == null) {
            return super._close();
        } else {
            // remove the selected tab
            _removeTab(_tabbedPane.getSelectedComponent());
            return true;
        }
    }
    
    /** Remove the tab for a component. */
    private void _removeTab(Component component) {
        
        if(component instanceof JGraph) {
            //final JGraph jgraph = (JGraph) component;
            //final NamedObj modelInTab = 
                //((AbstractBasicGraphModel)jgraph.getGraphPane().getGraphModel()).getPtolemyModel();
            //modelInTab.removeChangeListener(this);
            final int index = _tabbedPane.indexOfComponent(component);
            final TabbedCompositeTabComponent tabComponent =
                (TabbedCompositeTabComponent) _tabbedPane.getTabComponentAt(index);
            final Tableau tableau = tabComponent.getTableau();
            try {
                tableau.setContainer(null);
            } catch (Exception e) {
                MessageHandler.error("Error closing tableau.", e);
            }
            _tabbedPane.remove(component);
            
            // if only one tab left, move back to panel
            if(_tabbedPane.getTabCount() == 1) {
                final Component firstComponent = _tabbedPane.getComponentAt(0);
                _panel.remove(_tabbedPane);
                _tabbedPane = null;
                _panel.add(firstComponent, BorderLayout.CENTER);
            }
        }
    }
    
    /** Create a tabbed pane for the composite models. */
    private void _createJTabbedPane() {
        
        _tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.WRAP_TAB_LAYOUT);
        _tabbedPane.addChangeListener(this);
        //_tabbedPane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);

        // register keyboard shortcut to scroll tabs left
        final ActionListener actionSelectLeft = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                final JTabbedPane pane = (JTabbedPane)event.getSource();
                final int newIndex = pane.getSelectedIndex() - 1;
                pane.setSelectedIndex(newIndex < 0 ? pane.getTabCount() - 1 : newIndex);
            }
        };

        _tabbedPane.registerKeyboardAction(actionSelectLeft, 
                KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.META_DOWN_MASK),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        // register keyboard shortcut to scroll tabs right
        final ActionListener actionSelectRight = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                final JTabbedPane pane = (JTabbedPane)event.getSource();
                final int newIndex = (pane.getSelectedIndex() + 1) % pane.getTabCount();
                pane.setSelectedIndex(newIndex);
            }
        };
        
        _tabbedPane.registerKeyboardAction(actionSelectRight,
                KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.META_DOWN_MASK),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        /*
        TabbedPaneUI foo = _tabbedPane.getUI();
        _tabbedPane.setUI(new BasicTabbedPaneUI() {  
            @Override  
            protected int calculateTabAreaHeight(int tabPlacement, int horizRunCount, int maxTabHeight) {  
                if (_tabbedPane.getTabCount() > 1) {  
                    return super.calculateTabAreaHeight(tabPlacement, horizRunCount, maxTabHeight);  
                } else {  
                    return 0;  
                }  
            }
            @Override  
            protected void paintTab(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect) {  
                if (_tabbedPane.getTabCount() > 1) {  
                    super.paintTab(g, tabPlacement, rects, tabIndex, iconRect, textRect);  
                }  
            }  
        });  
        
        */
    }

    /** Find the component for a NamedObj; returns null if not found. */
    private Component _findComponentForModel(NamedObj model) {
        // look through all the tabs
        if(_tabbedPane != null) {
            for(Component component : _tabbedPane.getComponents()) {
                // only look at jgraphs
                if(component instanceof JGraph) {
                    final JGraph jgraph = (JGraph) component;
                    final NamedObj modelInTab = 
                        ((AbstractBasicGraphModel)jgraph.getGraphPane().getGraphModel()).getPtolemyModel();
                    // see if this tab's model matches
                    if(modelInTab == model) {
                        return component;
                    }
                }
            }
        }
        return null;
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    /** The root panel. This contains either a single JGraph if no
     *  composite actors have been opened, or _tabbedPane, which
     *  contains a tab for each open composite actor.
     */
    protected JPanel _panel; 

    /** The tabbed pane for composites. This is null if no
     *  composite actors have been opened.
     */
    protected JTabbedPane _tabbedPane;
    
    /** The composite actor for which this frame was opened. */
    private CompositeEntity _rootComposite;
    
    /** The tableau for which this frame was opened. */
    private TabbedKeplerGraphTableau _rootTableau;
    
    /** A regex for MoML change requests that rename actors. */
    private static final Pattern _MOML_RENAME_PATTERN = 
        Pattern.compile("entity name=\"(\\w+)\"><(?:rename|display) name=\"(\\w+)\"");

    /** A regex for MoML change requests that delete actors. */
    private static final Pattern _MOML_DELETE_PATTERN = Pattern.compile("deleteEntity name=\"(\\w+)\"");


    ///////////////////////////////////////////////////////////////////
    ////                     public inner classes                  ////

    /** Specialized graph controller that handles multiple graph models. */
    public class TabbedKeplerController extends ActorEditorGraphController {
        /** Override the base class to select the graph model associated
         *  with the selected pane.
         */
        public GraphModel getGraphModel() {
           
            if(_panel != null) {
                if(_tabbedPane != null) {
                    Component tab = _tabbedPane.getSelectedComponent();
                    if (tab instanceof JGraph) {
                        GraphPane pane = ((JGraph) tab).getGraphPane();
                        return pane.getGraphModel();
                    }
                } else {                
                    JGraph jgraph = (JGraph) _panel.getComponent(0);
                    return jgraph.getGraphPane().getGraphModel();
                }
            }
            
            // Fallback position.
            //System.out.println(System.nanoTime() + " MultiCompositeController.getGraphModel could not find JGraph");
            return super.getGraphModel();            
        }
        

        /** Add hot keys to the actions in the given JGraph.
         *  This method is overridden so that we can call it in
         *  addComposite().
         *  @param jgraph The JGraph to which hot keys are to be added.
         */
        protected void _addHotKeys(JGraph jgraph) {
            super._addHotKeys(jgraph);
        }
    }
}