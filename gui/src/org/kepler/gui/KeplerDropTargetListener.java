/*
 * Copyright (c) 2011 The Regents of the University of California.
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

package org.kepler.gui;

import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.ToolTipManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.kepler.sms.SemanticType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedCompositeActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.gui.Configuration;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.kernel.Relation;
import ptolemy.kernel.util.DropTargetHandler;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.KernelRuntimeException;
import ptolemy.kernel.util.Location;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Singleton;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.moml.MoMLChangeRequest;
import ptolemy.util.MessageHandler;
import ptolemy.vergil.basic.AbstractBasicGraphModel;
import ptolemy.vergil.basic.EditorDropTargetListener;
import ptolemy.vergil.kernel.AnimationRenderer;
import ptolemy.vergil.kernel.Link;
import ptolemy.vergil.toolbox.PtolemyTransferable;
import ptolemy.vergil.toolbox.SnapConstraint;
import diva.canvas.CanvasComponent;
import diva.canvas.Figure;
import diva.canvas.FigureLayer;
import diva.graph.GraphController;
import diva.graph.GraphModel;
import diva.graph.GraphPane;
import diva.graph.JGraph;
import diva.util.UserObjectContainer;

/**
 * @author Sven Koehler
 *         Date: Nov 2, 2010
 */
public class KeplerDropTargetListener extends EditorDropTargetListener {

    ///////////////////////////////////////////////////////////////
    ////                     public methods                    ////

    /**
     *  This method is derived from the original drop() method in
     *  DTListener.
     *  If the transferrable object is recognized as a Ptolemy II object,
     *  then use the MoML description of the object to create a new
     *  instance of the object at the drop location. If the drop
     *  location is on top of an icon representing an instance of
     *  NamedObj, then make that instance the container of the new
     *  object. Otherwise, make the model associated with the graph
     *  the container.
     *  This is called when the drag operation has terminated with a
     *  drop on the operable part of the drop site for the DropTarget
     *  registered with this listener.
     *  @param dtde The drop event.
     */
    public void drop(DropTargetDropEvent dtde) {
        //notify additionalListeners
        for (int i = 0; i < _dropTarget.getAdditionalListeners().size(); i++) {
            DropTargetListener l = _dropTarget.getAdditionalListeners().elementAt(i);
            l.drop(dtde);
        }
        // Unhighlight the target. Do this first in case
        // errors occur... Don't want to leave highlighting.
        if (_highlighted != null) {
            _highlighter.renderDeselected(_highlightedFigure);
            _highlighted = null;
            _highlightedFigure = null;
        }

        // See whether there is a container under the point.
        Point2D originalPoint = SnapConstraint.constrainPoint(dtde
                .getLocation());
        NamedObj container = _getObjectUnder(originalPoint);
        Link link = _getLinkUnder(originalPoint);

        GraphPane pane = ((JGraph) _dropTarget.getComponent()).getGraphPane();

        GraphController controller = pane.getGraphController();
        GraphModel model = controller.getGraphModel();
        NamedObj tempContainer = (NamedObj) model.getRoot();

        boolean relink = false;
        boolean insertlink = false;
        Vector<Vector<String>> oldLinks = new Vector<Vector<String>>();
        HashMap<String, Token> oldParams = new HashMap<String,Token>();
        HashSet<String> newParams = new HashSet<String>();
        HashSet<String> newPorts = new HashSet<String>();
        String createdActorName = "";  //TODO Where to get the name from ? Could there be more than one actor?
        String oldRelationName = null;

        if (container != null) {
            Figure f = _getFigureUnder(originalPoint);
            java.util.List<TypedIOPort> l = new LinkedList<TypedIOPort>();
            if (((Location)f.getUserObject()).getContainer() instanceof TypedAtomicActor) {
                l = ((TypedAtomicActor)((Location)f.getUserObject()).getContainer()).portList();
            } else if (((Location)f.getUserObject()).getContainer() instanceof TypedCompositeActor) {
                l = ((TypedCompositeActor)((Location)f.getUserObject()).getContainer()).portList();
            }
            for (TypedIOPort p : l) {
                java.util.List<Relation> rels = p.linkedRelationList();
                for (Relation r : rels) {
                    java.util.List<TypedIOPort> l2 = r.linkedPortList();
                    for (TypedIOPort p2 : l2) {
                        if (p2 == p) continue;
                        Vector<String> newEntry = new Vector<String>();
                        newEntry.add(p.getName());
                        newEntry.add(r.getName());
                        String fn = p2.getFullName();
                        int lastdot = fn.lastIndexOf(".");
                        String fn2 = fn.substring(0,lastdot);
                        int lastdot2 = fn2.lastIndexOf(".");
                        newEntry.add(fn.substring(lastdot2+1));
                        oldLinks.add(newEntry);
                    }
                }
            }

            // Parameters
            java.util.List al = new LinkedList();
            if (((Location)f.getUserObject()).getContainer() instanceof TypedAtomicActor) {
                al = ((TypedAtomicActor)((Location)f.getUserObject()).getContainer()).attributeList();
            } else if (((Location)f.getUserObject()).getContainer() instanceof TypedCompositeActor) {
                al = ((TypedCompositeActor)((Location)f.getUserObject()).getContainer()).attributeList();
            }
            for (Object o : al) {
                if (o instanceof Parameter && !(o instanceof SemanticType)) {
                    try {
                        oldParams.put(((Parameter)o).getName(), ((Parameter)o).getToken());
                    } catch (IllegalActionException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }

            container = null;
            relink = true;

            // Find the default container for the dropped object
            StringBuffer delFigMoml = new StringBuffer();
            delFigMoml.append("<group>");
            delFigMoml.append(((AbstractBasicGraphModel)model).getDeleteNodeMoML(f.getUserObject()));
            //delFigMoml.append("<deleteRelation name=\""+oldRelationName+"\"/>");
            delFigMoml.append("</group>");
            MoMLChangeRequest mcr = new MoMLChangeRequest(this, tempContainer, delFigMoml.toString());
            tempContainer.requestChange(mcr);

        } else if (link != null) {
            insertlink = true;
            oldRelationName = link.getRelation().getName();
            java.util.List<TypedIOPort> l2 = link.getRelation().linkedPortList();

            for (TypedIOPort p2 : l2) {
                String fn = p2.getFullName();
                int lastdot = fn.lastIndexOf(".");
                String fn2 = fn.substring(0,lastdot);
                int lastdot2 = fn2.lastIndexOf(".");
                Vector<String> newEntry = new Vector<String>(5);
                String type = p2.getType().toString();
                String portname = fn.substring(lastdot2+1);
                newEntry.add(oldRelationName); // relation name
                newEntry.add(portname); // port name including actor name
                newEntry.add(p2.isInput() ? "input" : (p2.isOutput() ? "output" : "unknown" ));
                newEntry.add(type); // type of that port
                newEntry.add(fn.substring(lastdot+1)); // JUST port name
                oldLinks.add(newEntry);
            }
        }

        if ((container == null) || !_dropTarget.isDropIntoEnabled()) {
            // Find the default container for the dropped object
            container = tempContainer;
        }

        // Find the location for the dropped objects.
        // Account for the scaling in the pane.
        Point2D transformedPoint = new Point2D.Double();
        pane.getTransformContext().getInverseTransform().transform(
                originalPoint, transformedPoint);

        // Get an iterator over objects to drop.
        Iterator iterator = null;

        java.util.List dropObjects = null;
        if (dtde.isDataFlavorSupported(PtolemyTransferable.namedObjFlavor)) {
            try {
                dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                dropObjects = (java.util.List) dtde
                        .getTransferable()
                        .getTransferData(PtolemyTransferable.namedObjFlavor);
                iterator = dropObjects.iterator();
            } catch (Exception e) {
                MessageHandler.error(
                        "Can't find a supported data flavor for drop in "
                                + dtde, e);
                return;
            }
        } else {
            dtde.rejectDrop();
        }

        if (iterator == null) {
            // Nothing to drop!
            return;
        }

        // Create the MoML change request to instantiate the new objects.
        StringBuffer moml = new StringBuffer();

        while (iterator.hasNext()) {
            final NamedObj dropObj = (NamedObj) iterator.next();
            final String name;

            if (dropObj instanceof Singleton) {
                name = dropObj.getName();
            } else {
                name = container.uniqueName(dropObj.getName());
            }

            // Constrain point to snap to grid.
            Point2D newPoint = SnapConstraint
                    .constrainPoint(transformedPoint);
            /* At this point we wish to provide for an alternative way to get the MOML
             * code for the drop object. This is to allow for a drop object that may just
             * contain a reference (e.g. an LSID) to the complete MOML rather than including
             * all the information in the tree node object being dragged.
             * DFH July 2007
             */

            // first try to get the LSID; if one cannot be found use the Ptolemy method for dropping
            boolean lsidFlag = true;
            try {
                String lsidString = ((StringAttribute) (dropObj
                        .getAttribute("entityId"))).getExpression();
                if ((lsidString == null) || (lsidString.equals(""))) {
                    lsidFlag = false;
                }
            } catch (Exception eee) {
                lsidFlag = false;
            }

            String result = "";
            String rootNodeName = dropObj.getElementName();
            Object object = null;

            StringAttribute alternateGetMomlActionAttribute = null;
            alternateGetMomlActionAttribute = (StringAttribute) dropObj
                    .getAttribute("_alternateGetMomlAction");
            if (alternateGetMomlActionAttribute == null && lsidFlag) {
                Configuration config = null;
                java.util.List configsList = Configuration.configurations();
                for (Iterator it = configsList.iterator(); it.hasNext();) {
                    config = (Configuration) it.next();
                    if (config != null) {
                        break;
                    }
                }
                if (config == null) {
                    throw new KernelRuntimeException(
                            dropObj,
                            "Could not find "
                                    + "configuration, list of configurations was "
                                    + configsList.size()
                                    + " elements, all were null.");
                }
                alternateGetMomlActionAttribute = (StringAttribute) config
                        .getAttribute("_alternateGetMomlAction");
            }

            boolean appendGroupAuto = true;
            if (alternateGetMomlActionAttribute != null) {
                String alternateGetMomlClassName = alternateGetMomlActionAttribute
                        .getExpression();
                try {
                    Class getMomlClass = Class
                            .forName(alternateGetMomlClassName);
                    object = getMomlClass.newInstance();
                    try {
                        Method getMomlMethod = getMomlClass.getMethod(
                                "getMoml", new Class[] { NamedObj.class,
                                        String.class });
                        result = (String) getMomlMethod.invoke(object,
                                new Object[] { dropObj, name });
                        appendGroupAuto = false;
                    } catch (NoSuchMethodException e) {
                        Method getMomlMethod = getMomlClass.getMethod(
                                "getMoml", new Class[] { NamedObj.class });
                        result = (String) getMomlMethod.invoke(object,
                                new Object[] { dropObj });
                        int int1 = 1;
                        int int2 = result.indexOf(" ");
                        rootNodeName = result.substring(int1, int2);
                        // following string substitution is needed to
                        // replace possible name changes when multiple
                        // copies of an actor are added to a workspace
                        // canvas (name then has integer appended to it)
                        // -- DFH
                        int1 = result.indexOf("\"", 1);
                        int2 = result.indexOf("\"", int1 + 1);

                        result = result.substring(0, int1 + 1) + name
                                + result.substring(int2, result.length());

                         createdActorName = name;
                    }
                    moml.append(result);
                } catch (Exception w) {
                    System.out.println("Error creating alternateGetMoml!");
                }
            } else { // default method for PtolemyII use
                result = dropObj.exportMoML(name);
                moml.append(result);
            }


            if ( (relink) || insertlink ) {
                try {
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder db = dbf.newDocumentBuilder();

                    ByteArrayInputStream bais = new ByteArrayInputStream(moml.toString().getBytes("ISO-8859-1"));
                    Document doc = null;
                    try {
                        doc = db.parse(bais);
                    } catch (SAXException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    } catch (IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                    doc.getDocumentElement().normalize();
                    NodeList nl = doc.getDocumentElement().getChildNodes();
                    for (int i = 0; i < nl.getLength(); i++) {
                        Node n = nl.item(i);
                        if (n.getNodeType() == Node.ELEMENT_NODE) {
                            Element fstElmnt = (Element)n;
                            if (fstElmnt.getNodeName().equals("property")) {
                                NamedNodeMap subnem = fstElmnt.getAttributes();
                                Node nameNode = subnem.getNamedItem("name");
                                newParams.add(nameNode.getNodeValue());
                            } else if (fstElmnt.getNodeName().equals("port")) {
                                NamedNodeMap subnem = fstElmnt.getAttributes();
                                Node nameNode = subnem.getNamedItem("name");
                                newPorts.add(nameNode.getNodeValue());
                            }
                        }
                    }
                    // TODO: Do we get all Ports of an actor or do we need to instantiate it here as well ?

                } catch (ParserConfigurationException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }

            if (appendGroupAuto) {
                moml.insert(0, "<group name=\"auto\">\n");
                moml.append("<" + rootNodeName + " name=\"" + name
                        + "\">\n");
                if (relink) {
                    for (Map.Entry<String,Token> e : oldParams.entrySet()) {
                        if (newParams.contains(e.getKey())) {

                            String s = null;
                            if (e.getValue() instanceof StringToken) {
                                s = ((StringToken)e.getValue()).stringValue();
                            } else {
                                s = e.getValue().toString();
                            }
                            // TODO: Fix quoting of certain Parameters
                            //s = s.replaceFirst("\"", "");
                            s = s.replace("\"", "&quot;");
                            moml.append("<property name=\""+e.getKey()+"\" value=\""+s+"\"/> ");
                            //System.out.println("<property name=\""+e.getKey()+"\" value=\""+e.getValue().toString()+"\"/> ");
                        }
                    }
                }

                moml.append("<property name=\"_location\" "
                                + "class=\"ptolemy.kernel.util.Location\" value=\"{");

                // todo: another hack to position actors approximately centered around the cursor
                int xoffset = 0;
                int yoffset = 0;
                if (insertlink || relink)
                {
                    xoffset = -32;
                    yoffset = -20;
                }
                moml.append(((int) newPoint.getX()) + xoffset);
                moml.append(", ");
                moml.append(((int) newPoint.getY()) + yoffset);
                moml.append("}\"/>\n</" + rootNodeName + ">\n");
                moml.append("</group>\n");
            }
        }

        if (container instanceof DropTargetHandler) {
            try {
                ((DropTargetHandler) container).dropObject(container,
                        dropObjects, moml.toString());
            } catch (IllegalActionException e) {
                MessageHandler.error("Unable to drop the object to "
                        + container.getName() + ".", e);
            }
        } else {
            moml.insert(0, "<group>");
            moml.append("</group>");

            //System.out.println(moml);
            MoMLChangeRequest request = new MoMLChangeRequest(this,
                    container, moml.toString());
            request.setUndoable(true);
            container.requestChange(request);
        }

        if (insertlink) {
            StringBuffer delMoml = null;
            delMoml = new StringBuffer();

            for (Vector<String> p : oldLinks)
            {
                // TODO: a very BAD hack !!! to get unique names
                String newRelationName = UUID.randomUUID().toString();
                if (p.get(2).equals("input")) {
                    // todo this only works in COMAD for now
                    if (p.get(4).equals("input") && newPorts.contains("output")) // todo : replace true by newPorts.contains(x)
                    {
                        delMoml.append("<relation name=\""+newRelationName+"\" class= \"ptolemy.actor.TypedIORelation\"></relation>");
                        delMoml.append("<link port=\""+ createdActorName+".output"+"\" relation=\""+ newRelationName +"\"/>");
                        delMoml.append("<link port=\""+p.get(1)+"\" relation=\""+ newRelationName +"\"/>");
                    }
                } else if (p.get(2).equals("output")) {
                    // todo this only works in COMAD for now
                    if (p.get(4).equals("output") && newPorts.contains("input")) // todo : replace true by newPorts.contains(x)
                    {
                        delMoml.append("<relation name=\""+newRelationName+"\" class= \"ptolemy.actor.TypedIORelation\"></relation>");
                        delMoml.append("<link port=\""+ createdActorName+".input"+"\" relation=\""+newRelationName+"\"/>");
                        delMoml.append("<link port=\""+p.get(1)+"\" relation=\""+newRelationName+"\"/>");
                    }
                }
            }

            if (delMoml.length() != 0) {
                MoMLChangeRequest mcr = new MoMLChangeRequest(this, container, "<group><deleteRelation name=\""+oldRelationName+"\"/></group>");
                container.requestChange(mcr);
                delMoml.insert(0,"<group>");
                delMoml.append("</group>");
                mcr = new MoMLChangeRequest(this, container, delMoml.toString());
                container.requestChange(mcr);
            }
        }

        if (relink) {
            StringBuffer relinkMoML = new StringBuffer();
            relinkMoML.insert(0, "<group name=\"auto\">\n");

            for (Vector<String> e : oldLinks)
            {
                if (true) // todo has a port of the right name: works in comad
                {
                    // todo check if relation exists already
                    relinkMoML.append("<relation name=\""+e.get(1)+"\" class= \"ptolemy.actor.TypedIORelation\"></relation>");
                    relinkMoML.append("<link port=\""+  createdActorName+"."+e.get(0) +"\" relation=\""+e.get(1)+"\"/>");
                    relinkMoML.append("<link port=\""+ e.get(2) +"\" relation=\""+e.get(1)+"\"/>");
                }
            }

            relinkMoML.append("</group>\n");
            MoMLChangeRequest cr = new MoMLChangeRequest(this, container, relinkMoML.toString());
            cr.setUndoable(true);
            container.requestChange(cr);
        }

        dtde.dropComplete(true); //success!

        //Added by MB 6Apr06 - without this, tooltips don't work
        //after first actor is dragged to canvas from library, until
        //pane loses & regains focus
        JComponent comp = (JComponent) _dropTarget.getComponent();
        if (comp != null) {
            ToolTipManager.sharedInstance().registerComponent(comp);
        }
    }


    ///////////////////////////////////////////////////////////////
    ////                     private methods                   ////

    /** Return the link that is under the specified point, or null
     *  if there is none.
     *  @param point The point in the graph pane.
     *  @return The Link under the specified point, or null if there
     *   is none or it is not a NamedObj.
     */
    private Link _getLinkUnder(Point2D point) {
        GraphPane pane = ((JGraph) _dropTarget.getComponent()).getGraphPane();

        // Account for the scaling in the pane.
        Point2D transformedPoint = new Point2D.Double();
        pane.getTransformContext().getInverseTransform().transform(point,
                transformedPoint);

        FigureLayer layer = pane.getForegroundLayer();

        // Find the figure under the point.
        // NOTE: Unfortunately, FigureLayer.getCurrentFigure() doesn't
        // work with a drop target (I guess it hasn't seen the mouse events),
        // so we have to use a lower level mechanism.
        double halo = layer.getPickHalo();
        double width = halo * 8;
        Rectangle2D region = new Rectangle2D.Double(transformedPoint.getX()
                - halo*4, transformedPoint.getY() - halo*4, width, width);
        CanvasComponent figureUnderMouse = layer.pick(region);

        // Find a user object belonging to the figure under the mouse
        // or to any figure containing it (it may be a composite figure).
        Object objectUnderMouse = null;

        while (figureUnderMouse instanceof UserObjectContainer
                && (objectUnderMouse == null)) {
            objectUnderMouse = ((UserObjectContainer) figureUnderMouse)
                    .getUserObject();

            if (objectUnderMouse instanceof Link) {
                return (Link) objectUnderMouse;
            }

            figureUnderMouse = figureUnderMouse.getParent();
        }

        return null;
    }

    ///////////////////////////////////////////////////////////////
    ////                     private variables                 ////
    // Currently highlighted drop target.
    private NamedObj _highlighted = null;

    // Currently highlighted figure.
    private Figure _highlightedFigure = null;

    // The renderer used for highlighting.
    private AnimationRenderer _highlighter = null;
    
}
