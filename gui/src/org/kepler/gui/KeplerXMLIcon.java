/*
 * Copyright (c) 1999-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-29 15:31:28 -0800 (Thu, 29 Nov 2012) $' 
 * '$Revision: 31163 $'
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

import java.awt.Image;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.swing.Icon;

import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.dom.svg.SVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.icon.ComponentEntityConfig;
import org.w3c.dom.svg.SVGDocument;

import ptolemy.actor.Director;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.ChangeRequest;
import ptolemy.kernel.util.ConfigurableAttribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Nameable;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Settable;
import ptolemy.kernel.util.ValueListener;
import ptolemy.kernel.util.Workspace;
import ptolemy.util.MessageHandler;
import ptolemy.vergil.icon.XMLIcon;
import util.EmptyChangeRequest;
import diva.canvas.Figure;
import diva.canvas.toolbox.ImageFigure;
import diva.canvas.toolbox.PaintedFigure;
import diva.canvas.toolbox.SVGParser;
import diva.gui.toolbox.FigureIcon;
import diva.util.java2d.PaintedList;
import diva.util.java2d.svg.SVGPaintedObject;
import diva.util.java2d.svg.SVGRenderingListener;
import diva.util.xml.XmlDocument;
import diva.util.xml.XmlElement;
import diva.util.xml.XmlReader;

//////////////////////////////////////////////////////////////////////////
//// XMLIcon
//////////////////////////////////////////////////////////////////////////

/**
 * An icon is a visual representation of an entity. Three such visual
 * representations are supported here. A background figure is returned by the
 * createBackgroundFigure() method. This figure is specified by an attribute
 * named "_iconDescription" of the container, if there is one. If there is no
 * such attribute, then a default icon is used. The createFigure() method
 * returns this same background figure, but decorated with a label giving the
 * name of the container, unless the container contains a parameter named
 * "_hideName" with value true. The createIcon() method returns a Swing icon
 * given by an attribute named "_smallIconDescription", if there is one. If
 * there is no such attribute, then the icon is simply a small representation of
 * the background figure.
 * <p>
 * The XML schema used in the "_iconDescription" and "_smallIconDescription"
 * attributes is SVG (scalable vector graphics), although currently Diva only
 * supports a small subset of SVG.
 * 
 * @author Chad Berkley, based on XMLIcon by Steve Neuendorffer, John Reekie, Contributor: Edward A. Lee
 * @version $Id: KeplerXMLIcon.java 31163 2012-11-29 23:31:28Z crawl $
 * @since Ptolemy II 2.0
 * @Pt.ProposedRating Yellow (neuendor)
 * @Pt.AcceptedRating Red (johnr)
 */

public class KeplerXMLIcon extends XMLIcon implements ValueListener {

	// parser used to parse XML for SVG rendering
	private static final String _DEFAULT_PARSER = "org.apache.xerces.parsers.SAXParser";

	// base uri to pass to createSVGDocument() method of SVGDocumentFactory
	private static final String _SVG_BASE_URI = "http://www.ecoinformatics.org/";

	private static Log log;
	private static boolean isDebugging;

	static {
		log = LogFactory.getLog("SVG." + XMLIcon.class.getName());
		isDebugging = log.isDebugEnabled();

		String parser = XMLResourceDescriptor.getXMLParserClassName();
		if (parser == null || parser.trim().equals("")) {
			parser = _DEFAULT_PARSER;
		}
		_df = new SAXSVGDocumentFactory(parser);
	}

	/**
	 * Construct an icon in the specified workspace and name. This constructor
	 * is typically used in conjunction with setContainerToBe() and
	 * createFigure() to create an icon and generate a figure without having to
	 * have write access to the workspace. If the workspace argument is null,
	 * then use the default workspace. The object is added to the directory of
	 * the workspace.
	 * 
	 * @see #setContainerToBe(NamedObj) Increment the version number of the
	 *      workspace.
	 * @param workspace
	 *            The workspace that will list the attribute.
	 * @param name
	 *            String
	 * @throws IllegalActionException
	 *             If the specified name contains a period.
	 */
	public KeplerXMLIcon(Workspace workspace, String name)
			throws IllegalActionException {

		super(workspace, name);
	}

	/**
	 * Create a new icon with the given name in the given container. By default,
	 * the icon contains no graphic objects.
	 * 
	 * @param container
	 *            The container for this attribute.
	 * @param name
	 *            The name of this attribute.
	 * @throws NameDuplicationException
	 * @throws IllegalActionException
	 */
	public KeplerXMLIcon(NamedObj container, String name)
			throws NameDuplicationException, IllegalActionException {

		super(container, name);
	}

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Create a background Figure based on this icon.
	 * 
	 * Looks for attributes in the following order, stopping if a match is
	 * found:
	 * 
	 * 1) "_svgIcon", which contains a pointer (typically a classpath-relative
	 * file path). If it exists, uses it to create the background Figure
	 * 
	 * 2) "_iconDescription", which contains an xml simple-svg description. If
	 * it exists, uses it to create the icon, using the simple Diva rendering
	 * system
	 * 
	 * If no match is found, it simply defers to the base class.
	 * 
	 * @return A figure for this icon.
	 */
    @Override
	public Figure createBackgroundFigure() {

		// Get the container.
		NamedObj container = (NamedObj) getContainerOrContainerToBe();
		
		// do not use batik to render the icons for non-director attributes.
		// see http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5266
		if(container instanceof Attribute && !(container instanceof Director)) {
		    isBatikRendering = false;
		}

		boolean iconChanged = false;

		if (isBatikRendering) {
			if (isDebugging) {
				log
						.debug("*** createBackgroundFigure() calling _batikCreateBackgroundFigure("
								+ container.getName() + ")\n ");
			}

			if (!lsidAssignmentDone) {
				_doLSIDIconAssignment(container);
				// this flag is to determine whether the _doLSIDIconAssignment()
				// method has been called from within the
				// createBackgroundFigure() method.
				lsidAssignmentDone = true;
			}
			iconChanged = _batikCreateBackgroundFigure(container);

			// If both _svgIconAttrib and _bgFigure are null after calling
			// _batikCreateBackgroundFigure(), this means that the actor has no
			// batik-style SVG icon assigned. Since all actors are assigned with
			// a
			// batik icon - even if it's just the default blank one - this means
			// we must be dealing with an annotation or shape actor etc - so we
			// should not generate a default icon, since this would overwrite
			// the
			// desired textual or shape-drawing display. Therefore, simply call:
			// _divaCreateBackgroundFigure() to handle this icon
			if (_batikSVGIconAttrib == null && _bgFigure == null) {
				if (isDebugging) {
					log
							.info("\n*** could not assign a Batik SVG icon - therefore rendering "
									+ "old-style Diva SVG icon for "
									+ container.getName() + "\n ");
				}
				iconChanged = _divaCreateBackgroundFigure(container);
			}
		} else {
			if (isDebugging) {
				log
						.debug("\n*** createBackgroundFigure() calling _divaCreateBackgroundFigure("
								+ container.getName() + ")\n ");
			}
			iconChanged = _divaCreateBackgroundFigure(container);
		}

		if (iconChanged) {
			// clear the caches
			_recreateFigure();

			// Update the painted list.
			_updatePaintedList();
		}

		// PERMUTATIONS:
		// (new icon) (cached icon)
		// _paintedList _bgFigure iconChanged Action
		// ------------ --------- ------------- -----------------------
		// null null true/false return *default* icon
		// null exists true/false return cached Icon
		// exists null true/false create & return new Icon
		// exists exists true create & return new Icon
		// exists exists false return cached Icon
		//
		// There are much cooler ways to do this, but the
		// following method was used for clarity...
		//
		boolean newIconExists = (_paintedList != null);
		boolean cachedIconExists = (_bgFigure != null);

		if (isDebugging) {
			log.debug("\n*** createBackgroundFigure() (" + container.getName()
					+ "):" + "\n newIconExists = " + newIconExists
					+ "\n cachedIconExists = " + cachedIconExists
					+ "\n iconChanged = " + iconChanged);
		}

		if (!newIconExists && !cachedIconExists) {
			_bgFigure = _getDefaultBackgroundFigure();

		} else if (!newIconExists && cachedIconExists) {

			// do nothing - cached icon (_bgFigure) will be returned as is

		} else if (newIconExists && !cachedIconExists) {

			_bgFigure = new PaintedFigure(_paintedList);

		} else if (iconChanged) {

			_bgFigure = new PaintedFigure(_paintedList);

		} else {

			// do nothing - cached icon (_bgFigure) will be returned as is
		}
		return _bgFigure;
	}

	/**
	 * Create a new Swing icon to use as a thumbnail in the Actor Library.
	 * 
	 * Looks for attributes in the following order, stopping if a match is
	 * found:
	 * 
	 * 1) if SVG_BATIK_RENDERING enabled - looks for "_thumbnailRasterIcon",
	 * which contains a pointer (typically a classpath-relative file path). If
	 * it exists, uses it to create the thumbnail icon. If (a) it doesn't exist,
	 * or (b) if rendering method is SVG_DIVA_RENDERING, proceed to next step,
	 * since (a) we're dealing with an annotation or shape actor etc, or (b) we
	 * don't want to render the new-style thumbnail and the old-style actor
	 * icons.
	 * 
	 * 2) "_smallIconDescription", which contains an xml simple-svg description.
	 * If it exists, uses it to create the icon, using the simple Diva rendering
	 * system
	 * 
	 * If no match is found, a default image is used, as follows:
	 * 
	 * 3) looks for a cached actor icon (_bgFigure).
	 * 
	 * 4) If no cached one is available, it creates a scaled version of the
	 * background figure by calling _divaCreateBackgroundFigure(), using the
	 * old-style Diva svg rendering to render the xml simple-svg description in
	 * the "_iconDescription" attribute, if it exists. Uses old diva rendering
	 * because batik is very memory intensive, and is not required for this
	 * task.
	 * 
	 * 5) If all else fails, it simply defers to the base class.
	 * 
	 * @return A Swing Icon.
	 */
    @Override
	public Icon createIcon() {

		// In this class, we cache the rendered icon, since creating icons from
		// figures is expensive.
		if (_iconCache != null) {
			return _iconCache;
		}

		// We know that at this point,
		// _iconCache == null

		NamedObj container = (NamedObj) getContainerOrContainerToBe();

		// do not use batik to render the icons for non-director attributes.
        // see http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5266
		if(container instanceof Attribute && !(container instanceof Director)) {
		    isBatikRendering = false;
		}
		
		Figure thumbFig = null;

		// 1) if SVG_BATIK_RENDERING enabled...
		if (isBatikRendering) {

			// ...looks for "_thumbnailRasterIcon", which contains a pointer
			// (typically a classpath-relative file path).
			ConfigurableAttribute rasterThumbAtt = null;
			try {
				rasterThumbAtt = (ConfigurableAttribute) container
						.getAttribute(ComponentEntityConfig.RASTER_THUMB_ATTRIB_NAME);
			} catch (Exception ex2) {
				if (isDebugging) {
					log.warn(container.getName()
							+ ": exception getting rasterThumbAtt attribute: "
							+ ex2.getMessage());
				}
				rasterThumbAtt = null;
			}
			if (isDebugging) {
				log.debug("createIcon(): " + container.getClass().getName()
						+ " - just got rasterThumbAtt=" + rasterThumbAtt);
			}
			// If it exists, uses it to create the thumbnail icon.
			if (rasterThumbAtt != null) {

				// If the description has changed, update _divaThumbAttrib
				// and listeners
				if (_rasterThumbAttrib != rasterThumbAtt) {
					if (_rasterThumbAttrib != null) {
						// Remove this as a listener if there
						// was a previous description.
						_rasterThumbAttrib.removeValueListener(this);
					}

					_rasterThumbAttrib = rasterThumbAtt;

					// Listen for changes in value to the icon description.
					_rasterThumbAttrib.addValueListener(this);
				}

				String thumbPath = rasterThumbAtt.getExpression();
				if (thumbPath == null || thumbPath.trim().length() < 1) {
					thumbFig = null;
				} else {
					if (isDebugging) {
						log
								.debug("createIcon(): SVG_BATIK_RENDERING and rasterThumbAtt="
										+ thumbPath.trim());
					}
					try {
						URL url = getClass().getResource(thumbPath.trim());
						if (url == null) {
							if (isDebugging) {
								log.warn("\n ERROR - createIcon(): "
										+ container.getClass().getName()
										+ " : url==null for thumb path:\n"
										+ thumbPath.trim());
							}
							thumbFig = null;
						} else {
							if (isDebugging) {
								log.debug(":::: "
										+ container.getClass().getName()
										+ " - got thumbPath.trim() = "
										+ thumbPath.trim());
							}
							Toolkit tk = Toolkit.getDefaultToolkit();
							Image thumbImg = tk.getImage(url);
							thumbFig = new ImageFigure(thumbImg);
						}
					} catch (Exception ex) {
						if (isDebugging) {
							log
									.warn("createIcon(): "
											+ container.getClass().getName()
											+ "\n exception getting thumbnail icon. Path = "
											+ thumbPath.trim()
											+ "\n exception = " + ex);
						}
						thumbFig = null;
					}
				}
			}
		}
		// If(a)it doesn't
		// exist, or (b) if rendering method is SVG_DIVA_RENDERING, proceed to
		// next step, since (a) we're dealing with an annotation or shape actor
		// etc, or (b) we don't want to render the new-style thumbnail and the
		// old-style actor icons....

		if (thumbFig == null) {
			// 2) "_smallIconDescription", which contains an xml simple-svg
			// description. If it exists, uses it to create the icon, using the
			// simple Diva rendering system
			ConfigurableAttribute divaThumbAtt = null;
			try {
				divaThumbAtt = (ConfigurableAttribute) container
						.getAttribute(OLD_SVG_THUMB_ATTNAME);
			} catch (Exception ex1) {
				divaThumbAtt = null;
			}

			if (divaThumbAtt != null) {

				// If the description has changed, update _divaThumbAttrib
				// and listeners
				if (_divaThumbAttrib != divaThumbAtt) {
					if (_divaThumbAttrib != null) {
						// Remove this as a listener if there
						// was a previous description.
						_divaThumbAttrib.removeValueListener(this);
					}

					_divaThumbAttrib = divaThumbAtt;

					// Listen for changes in value to the icon description.
					_divaThumbAttrib.addValueListener(this);
				}

				String divaThumbPath = null;
				try {
					divaThumbPath = divaThumbAtt.value();
				} catch (IOException ex3) {
					divaThumbPath = null;
				}
				if (divaThumbPath == null || divaThumbPath.trim().length() < 1) {
					thumbFig = null;
				} else {
					// clear the caches
					_recreateFigure();

					PaintedList paintedList = null;
					try {
						paintedList = _divaCreatePaintedList(divaThumbPath
								.trim());
						thumbFig = new PaintedFigure(paintedList);
					} catch (Exception ex4) {
						thumbFig = null;
					}
				}
			}
		}

		// 3) looks for a cached actor icon (_bgFigure).
		if (thumbFig == null && _bgFigure != null) {
			thumbFig = _bgFigure;
		}

		// 4) If no cached one is available, it creates a scaled version of the
		// background figure by calling _divaCreateBackgroundFigure(), using
		// old-style Diva svg rendering to render the simple-svg description
		// in the (*large*) "_iconDescription" attribute, if it exists. Uses
		// diva because batik is very memory intensive, and is not needed
		// for this simple svg.
		if (thumbFig == null) {
			if (isDebugging) {
				log.debug("getIcon() : " + container.getClass().getName()
						+ " - doing thumbFig = createBackgroundFigure()");
			}
			thumbFig = createBackgroundFigure();
		}

		// 5) If all else fails, it simply defers to the base class.
		if (thumbFig == null) {
			return super.createIcon();
		}

		// The last argument says to turn anti-aliasing on.
		if (isBatikRendering) {
			_iconCache = new FigureIcon(thumbFig, 16, 16, 0, true);
		} else {
			// NOTE: The size is hardwired here. Should it be?
			// The second to last argument specifies the border.
			_iconCache = new FigureIcon(thumbFig, 20, 15, 0, true);
		}
		return _iconCache;
	}

	/**
	 * Return a string representing this Icon.
	 * 
	 * @return String
	 */
    @Override
	public String toString() {
		String str = super.toString() + "(";

		str += (_svgXML != null) ? _svgXML : "";
		return str + ")";
	}

	/**
	 * React to the fact that the value of an attribute named "_iconDescription"
	 * contained by the same container has changed value by redrawing the
	 * figure.
	 * 
	 * @param settable
	 *            The object that has changed value.
	 */
    @Override
	public void valueChanged(Settable settable) {

		String name = ((Nameable) settable).getName();

		if (name.equals(ComponentEntityConfig.SVG_ICON_ATTRIB_NAME)
				|| name.equals(OLD_SVG_ICON_ATTNAME)
				|| name.equals(OLD_SVG_THUMB_ATTNAME)
				|| name.equals(ComponentEntityConfig.SVG_ICON_ATTRIB_NAME)
				|| name.equals(ComponentEntityConfig.RASTER_THUMB_ATTRIB_NAME)) {

			_recreateFigure();
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////
	// /////////////////////////////////////////////////////////////////

	private void _doLSIDIconAssignment(NamedObj container) {
		if (isBatikRendering) {
			try {
				boolean success = ComponentEntityConfig
						.tryToAssignIconByLSID((NamedObj) getContainerOrContainerToBe());
				if (!success) {
					throw new Exception("Icon was not assigned by LSID");
				}
			} catch (Exception ex1) {
				log.debug(ex1.getMessage());
			}

		}
	}

	/**
	 * CALLED ONLY IF svgRenderingMethod is SVG_BATIK_RENDERING Looks for batik
	 * svg icon attribute (@see SVG_ICON_ATTRIB_NAME), and if it exists, and has
	 * changed from previous value (which will be the case if this is the first
	 * call to this method), reads the SVG icon file and puts its String
	 * contents in the global <code>_svgXML</code> variable and returns
	 * <code>true</code>.
	 * 
	 * @param container
	 *            NamedObj - the container or container-to-be (typically the
	 *            actor)
	 * @return boolean true if the attribute exists and has changed (which is
	 *         also the case if this is the first call to this method), false
	 *         otherwise
	 */
	private boolean _batikCreateBackgroundFigure(NamedObj container) {

		// get the SVG_ICON_ATTRIB_NAME attribute
		ConfigurableAttribute svgIconAtt = null;
		try {
			svgIconAtt = (ConfigurableAttribute) container
					.getAttribute(ComponentEntityConfig.SVG_ICON_ATTRIB_NAME);
		} catch (Exception ex) {
			ex.printStackTrace();
			if (isDebugging) {
				log.warn("_batikCreateBackgroundFigure("
						+ container.getClass().getName()
						+ ") : exception getting svgIcon attribute: "
						+ ex.getMessage() + "\n\n");
			}
			svgIconAtt = null;
		}
		if (isDebugging && svgIconAtt != null) {
			log.warn("_batikCreateBackgroundFigure("
					+ container.getClass().getName()
					+ ": GOT svgIcon attribute: " + svgIconAtt.getExpression()
					+ "\n\n");
		}

		// if svgIconAttrib not null and has changed, get icon svg file contents
		if (svgIconAtt != null && svgIconAtt != _batikSVGIconAttrib
				&& svgIconAtt.getExpression().trim().length() > 0) {

			try {
				_svgXML = readResourceAsString(svgIconAtt.getExpression());
			} catch (Exception ex) {
				// if we can't find/read icon, default one will be used
				if (isDebugging) {
					log.warn(container.getName()
							+ ": exception getting _svgXML from path ("
							+ svgIconAtt.getExpression() + ") - "
							+ ex.getMessage() + "\n\n");
				}
			}

			if (_batikSVGIconAttrib != null) {
				// Remove this as a listener if there
				// was a previous description.
				_batikSVGIconAttrib.removeValueListener(this);
			}

			// update the global _svgIconAttrib variable.
			_batikSVGIconAttrib = svgIconAtt;

			if (_batikSVGIconAttrib != null) {
				// Listen for changes in value to the icon description.
				_batikSVGIconAttrib.addValueListener(this);
			}
			return true;
		}
		return false;
	}

	/**
	 * CALLED ONLY IF svgRenderingMethod is *not* SVG_BATIK_RENDERING Looks for
	 * old-style diva svg icon attribute ("_iconDescription"), and if it exists,
	 * and has changed from previous value (which will be the case if this is
	 * the first call to this method), puts its String contents in the global
	 * <code>_svgXML</code> variable and returns <code>true</code>.
	 * 
	 * @param container
	 *            NamedObj - the container or container-to-be (typically the
	 *            actor)
	 * @return boolean true if the attribute exists and has changed (which is
	 *         also the case if this is the first call to this method), false
	 *         otherwise
	 */
	private boolean _divaCreateBackgroundFigure(NamedObj container) {

		// get the "_iconDescription" attribute
		ConfigurableAttribute descriptionConfAtt = null;
		try {
			descriptionConfAtt = (ConfigurableAttribute) container
					.getAttribute(OLD_SVG_ICON_ATTNAME);
		} catch (Exception ex2) {
			if (isDebugging) {
				log.warn(container.getName()
						+ ": exception getting _iconDescription attribute: "
						+ ex2.getMessage() + "\n\n");
			}
			descriptionConfAtt = null;
		}

		if (isDebugging) {
			log.debug("_divaCreateBackgroundFigure(" + container.getName()
					+ "): _iconDescription attribute: " + descriptionConfAtt
					+ "\n\n");
		}

		// if description not null and has changed, get icon svg file contents
		if (descriptionConfAtt != null
				&& _divaSVGIconAttrib != descriptionConfAtt) {

			if (_divaSVGIconAttrib != null) {
				// Remove this as a listener if there
				// was a previous description.
				_divaSVGIconAttrib.removeValueListener(this);
			}

			// update the global _divaSVGIconAttrib variable.
			_divaSVGIconAttrib = descriptionConfAtt;

			if (_divaSVGIconAttrib != null) {
				// Listen for changes in value to the icon description.
				_divaSVGIconAttrib.addValueListener(this);
			}

			try {
				_svgXML = _divaSVGIconAttrib.value();
			} catch (IOException ex1) {

				ex1.printStackTrace();
			}
			if (isDebugging) {
				log.debug("_divaCreateBackgroundFigure(" + container.getName()
						+ "): _iconDescription attribute CONTENTS: \n"
						+ _svgXML + "\n\n");
			}
			return true;
		}
		return false;
	}

	/**
	 * Update the painted list of the icon based on the SVG data in the
	 * associated _svgXML variable, if there is one.
	 */
	private void _updatePaintedList() {

		if (_svgXML == null || _svgXML.trim().length() == 0) {
			_paintedList = null;
			return;
		}

		try {	    
			if (isBatikRendering) {
				_paintedList = _batikCreatePaintedList(_svgXML);
				_addListenersToPaintedList();
			} else {
				_paintedList = _divaCreatePaintedList(_svgXML);
			}
		} catch (Exception ex) {
			_paintedList = null;
			if (isBatikRendering) {
				_removeListenersFromPaintedList();
			}
		}
	}

	private PaintedList _batikCreatePaintedList(String svgXMLStr)
			throws Exception {
		// START - EXECUTED ONLY IF svgRenderingMethod is SVG_BATIK_RENDERING
		Reader sr = new StringReader(svgXMLStr);

		final String uri = _SVG_BASE_URI + sr.hashCode();
		SVGDocument doc = _df.createSVGDocument(uri, sr);

		PaintedList list = new PaintedList();
		String name = doc.getDocumentElement().getNodeName();

		if (!name.equals("svg")) {
			throw new IllegalArgumentException(
					"Input XML has a root name which is '" + name
							+ "' instead of 'svg'");
		}

		SVGPaintedObject object = new SVGPaintedObject(doc);
		if (object != null) {
			list.add(object);
		}

		return list;
		// END - EXECUTED ONLY IF svgRenderingMethod is SVG_BATIK_RENDERING
	}

	private PaintedList _divaCreatePaintedList(String svgXMLStr)
			throws Exception {
		Reader in = new StringReader(svgXMLStr);

		// NOTE: Do we need a base here?
		XmlDocument document = new XmlDocument((URL) null);
		XmlReader reader = new XmlReader();
		reader.parse(document, in);

		XmlElement root = document.getRoot();
		return SVGParser.createPaintedList(root);
	}

	/**
	 * add Listeners to all elements in the _paintedList, so they can be
	 * repainted when Batik has finished rendering them
	 */
	private void _addListenersToPaintedList() {

		if (_paintedList == null) {
			return;
		}
		List objects = _paintedList.paintedObjects;
		Iterator it = objects.iterator();
		while (it.hasNext()) {
			SVGPaintedObject po = (SVGPaintedObject) (it.next());
			po.addSVGRenderingListener(_svgrListener);
		}
	}

	/**
	 * remove Listeners from all elements in the _paintedList
	 */
	private void _removeListenersFromPaintedList() {

		if (_paintedList == null) {
			return;
		}
		List objects = _paintedList.paintedObjects;
		Iterator it = objects.iterator();
		while (it.hasNext()) {
			SVGPaintedObject po = (SVGPaintedObject) (it.next());
			po.removeSVGRenderingListener(_svgrListener);
		}
	}

	// make sure we create the default (blank) BG figure
	// only once, and then only if it is needed
	private Figure _getDefaultBackgroundFigure() {

		if (_defaultBackgroundFigure == null) {
			_defaultBackgroundFigure = _createDefaultBackgroundFigure();
		}
		return _defaultBackgroundFigure;
	}

	private void fireChangeRequest() {
		// doing a _bgFigure.repaint() will make the icons show up, but the
		// ports are in the wrong places, because when the actors are being
		// rendered, getBounds() called on the SVGPaintedObject returns the
		// default Dim(20,20), since the SVG hasn't been parsed/built yet, so
		// SVGPaintedObject doesn't yet know it's finished size. We therefore
		// need to issue a ChangeRequest (albeit one that doesn't actually
		// involve any changes) to get the icons to update after the SVG has
		// finished rendering, which will then cause the ports to be rendered
		// in the correct locations...
		// This actually seems like a bit of a hack. Future improvement - see
		// if there's a better way
		NamedObj container = toplevel();
		if (container == null) {
			return;
		}
		ChangeRequest request = new EmptyChangeRequest(this, "update request");
		container.requestChange(request);
	}

	/**
	 * Read the contents of a resource and return as a String. Use
	 * getResourceAsStream() to find the named resource using this classes
	 * classloader. Then spool the contents into a StringBuffer and return the
	 * String.
	 * 
	 * @param name
	 *            resource to retrieve.
	 * @return String containing the contents of the named resource
	 */
	private String readResourceAsString(String name) {
		// System.out.println("Trying to open file: " + file.toString());
		InputStream is = getClass().getResourceAsStream(name);

		Reader r = null;
		try {
		    r = new InputStreamReader(is);
    		char[] chars = new char[1024];
    
    		StringBuffer result = new StringBuffer("");
    
    		while (true) {
    			try {
    				int bread = r.read(chars);
    				if (bread < 0) {
    					break;
    				}
    				result.append(chars, 0, bread);
    			} catch (IOException e) {
    			    MessageHandler.error("Error reading " + name, e);
    				break;
    			}
    
    		}
    
    		return result.toString();
		} finally {
		    if(r != null) {
		        try {
                    r.close();
                } catch (IOException e) {
                    MessageHandler.error("Error reading " + name, e);
                }
		    }
		}

	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////
	// /////////////////////////////////////////////////////////////////

	// The list of painted objects contained in this icon.
	private PaintedList _paintedList;

	// The attribute containing the XMS string describing this actor's svg icon,
	// to be rendered by diva's simple svg rendering framework
	private ConfigurableAttribute _divaSVGIconAttrib;

	// The attribute containing the description of the thumbnail icon in SVG
	// XML,
	// to be rendered by diva.
	private ConfigurableAttribute _divaThumbAttrib;

	// The attribute containing the path of the raster thumbnail icon
	private ConfigurableAttribute _rasterThumbAttrib;

	// The attribute containing the path of the actor svg icon, to be
	// rendered by batik
	private ConfigurableAttribute _batikSVGIconAttrib;

	// the Figure returned by the createBackgroundFigure() method. Need a global
	// handle so we can update it after svg rendering is finished
	private Figure _bgFigure;

	private Figure _defaultBackgroundFigure;

	private static SVGDocumentFactory _df;

	private String _svgXML;

	private boolean isBatikRendering = (StaticGUIResources
			.getSVGRenderingMethod() == StaticGUIResources.SVG_BATIK_RENDERING);

	private static final String OLD_SVG_ICON_ATTNAME = "_iconDescription";
	private static final String OLD_SVG_THUMB_ATTNAME = "_smallIconDescription";

	private boolean lsidAssignmentDone = false;

	private final SVGRenderingListener _svgrListener = new SVGRenderingListener() {
		public void svgRenderingComplete() {

			// refresh icon in case it's being used by the getIcon() method
			// to create an icon in the actor library
			if (_bgFigure != null) {
				_bgFigure.repaint();
			}

			// redraw model so ports get moved to correct bounds
			fireChangeRequest();
		}
	};
}