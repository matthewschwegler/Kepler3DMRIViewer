/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: welker $'
 * '$Date: 2010-05-05 22:21:26 -0700 (Wed, 05 May 2010) $' 
 * '$Revision: 24234 $'
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

package org.sdm.spa.actors.piw.viz;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;

import javax.swing.SwingConstants;

import diva.canvas.CompositeFigure;
import diva.canvas.Figure;
import diva.canvas.FigureLayer;
import diva.canvas.GraphicsPane;
import diva.canvas.JCanvas;
import diva.canvas.event.LayerEvent;
import diva.canvas.event.MouseFilter;
import diva.canvas.interactor.DragInteractor;
import diva.canvas.interactor.Interactor;
import diva.canvas.toolbox.BasicRectangle;
import diva.canvas.toolbox.LabelFigure;

public class OverviewPanel extends JCanvas {

	private static final long serialVersionUID = 3257288041205608504L;
	private final ActionListener _listenerViewPortMove;
	private final ActionListener _listenerSequenceMove;
	private Figure _viewPort;
	private int _sizeViewPort;

	private final SequenceCollection _sequenceCollection;
	private final SiteCollection _siteCollection;

	public OverviewPanel(SequenceCollection sequenceCollection,
			SiteCollection siteCollection, ActionListener listenerViewPortMove,
			ActionListener listenerSequenceMove) {

		this._listenerViewPortMove = listenerViewPortMove;
		this._listenerSequenceMove = listenerSequenceMove;
		this._sizeViewPort = 100;

		this._sequenceCollection = sequenceCollection;
		this._siteCollection = siteCollection;
	}

	public void setPreferredSize() {
		int width = _sequenceCollection.getMaximumSequenceLength() + 200;
		int height = (_sequenceCollection.size() + 2) * 50;

		AffineTransform current = this.getCanvasPane().getTransformContext()
				.getTransform();
		width *= current.getScaleX();
		height *= current.getScaleY();

		this.setPreferredSize(new Dimension(width, height));
	}

	private void setViewPortSize(int height, int maxDrag) {
		int size = 100;
		int start = 150;

		if (null != _viewPort) {
			size = (int) _viewPort.getShape().getBounds().getWidth();
			start = (int) _viewPort.getShape().getBounds().getX();
		}

		setViewPortSize(size, start, height, maxDrag);
	}

	private void setViewPortSize(int size, int start, int height, int maxDrag) {
		FigureLayer layer = ((GraphicsPane) this.getCanvasPane())
				.getForegroundLayer();

		// Create anew.
		BasicRectangle newViewPort = new BasicRectangle(start, 25,
				_sizeViewPort, height, Color.MAGENTA) {
			public void paint(Graphics2D g) {
				AlphaComposite c = AlphaComposite.getInstance(
						AlphaComposite.SRC_OVER, 0.1f);
				g.setComposite(c);
				g.setPaint(new Color(150, 50, 150));
				g.fill(this.getShape());
				g.setComposite(AlphaComposite.SrcOver);
			}
		};
		newViewPort.setInteractor(new DraggerSnap(_listenerViewPortMove, 10,
				150, maxDrag));

		// If _viewPort exists, then remove it.
		if (null != _viewPort) {
			// Need to make sure the new viewport is inserted
			// at the same zlayer as the previous.
			int index = layer.getFigures().indexOf(_viewPort);
			if (-1 == index) {
				layer.add(newViewPort);
			} else {
				layer.add(index, newViewPort);
				layer.remove(_viewPort);
			}
		} else {
			layer.add(newViewPort);
		}

		_viewPort = newViewPort;
	}

	public void setViewPortSize(int size) {

		// If the input is non-positive, then set the size to 1.
		if (0 >= size) {
			this._sizeViewPort = 1;
		} else {
			this._sizeViewPort = size;
		}

		int start = 150;
		int height = 100;
		int maxDrag = 500;

		if (null != _viewPort) {
			start = (int) _viewPort.getShape().getBounds().getX();
			height = (int) _viewPort.getShape().getBounds().getHeight();
			maxDrag = ((DraggerSnap) _viewPort.getInteractor())._maximum;
		}

		setViewPortSize(this._sizeViewPort, start, height, maxDrag);
	}

	private void drawStartEndMarkers(Sequence sequence,
			CompositeFigure compositeFigure, int y) {
		LabelFigure start = new LabelFigure("1", "SansSerif", Font.PLAIN, 10);
		LabelFigure end = new LabelFigure(new Integer(sequence.lengthNoGaps())
				.toString(), "SansSerif", Font.PLAIN, 10);
		start.translateTo(0, y);
		end.translateTo(sequence.alignedSequence.length() - 1, y);
		compositeFigure.add(start);
		compositeFigure.add(end);
	}

	private void drawSequence(String strSequence,
			CompositeFigure compositeFigure, int y, int height, Color color,
			boolean isConsensus) {

		String loopSequence = strSequence;
		int loopSpot = 0;
		while (loopSequence.length() > 0) {
			int length = loopSequence.length();
			if (loopSequence.charAt(0) == '-') {
				loopSequence = loopSequence.replaceFirst("-+", "");
				length -= loopSequence.length();
			} else {
				loopSequence = loopSequence.replaceFirst("[^-]+", "");
				length -= loopSequence.length();

				if (isConsensus
						&& length < _sequenceCollection
								.getMinimumConsensusLength()) {
					continue;
				}

				compositeFigure.add(new BasicRectangle(loopSpot, y, length,
						height, color, 0));
			}
			loopSpot += length;
		}
	}

	public void drawSequences() {
		FigureLayer layer = ((GraphicsPane) this.getCanvasPane())
				.getForegroundLayer();

		// Clear whatever's been drawn before.
		layer.clear();

		// Draw consensus sequence.
		CompositeFigure compositeFigure = new CompositeFigure();
		drawSequence(_sequenceCollection.getConsensus(), compositeFigure, 0,
				_sequenceCollection.size() * 50, Color.LIGHT_GRAY, true);
		compositeFigure.translate(150, 25);
		layer.add(compositeFigure);

		// Set the preferred size and draw the viewport.
		this.setPreferredSize();
		this.setViewPortSize(_sequenceCollection.size() * 50,
				_sequenceCollection.getConsensus().length() + 150);

		for (int i = 0; i < _sequenceCollection.size(); i++) {
			Sequence seq = _sequenceCollection.getSequence(i);

			Interactor boundedDragger = new DraggerSnapSequence(
					_listenerSequenceMove, 1, 148, _sequenceCollection
							.getConsensus().length() + 150 - 1);
			boundedDragger.setMouseFilter(MouseFilter.defaultFilter);

			// Add the label for accession number.
			LabelFigure labelFigure = new LabelFigure(
					seq.accessionNumberOriginal, "SansSerif", Font.PLAIN, 14);
			labelFigure.setFillPaint(Color.BLUE);
			labelFigure.translateTo(20, (i + 1) * 50);
			labelFigure.setAnchor(SwingConstants.SOUTH_WEST);
			layer.add(labelFigure);

			// Add the label for gene ID.
			labelFigure = new LabelFigure(seq.geneID, "SansSerif", Font.ITALIC,
					14);
			labelFigure.translateTo(20, (i + 1) * 50);
			labelFigure.setAnchor(SwingConstants.NORTH_WEST);
			layer.add(labelFigure);

			// Draw the sequence.
			compositeFigure = new CompositeFigure();
			drawSequence(seq.alignedSequence, compositeFigure, 0, 1,
					Color.DARK_GRAY, false);
			drawStartEndMarkers(seq, compositeFigure, 15);

			// Draw the transcription factor binding sites.
			for (int j = 0; j < seq.arrTFBSs.length; j++) {
				final TranscriptionFactorBindingSite tfbs = seq.arrTFBSs[j];
				final SiteCollection.Site site = _siteCollection
						.getSite(tfbs.name);
				// If not visible, then don't paint.
				if (!site.selected) {
					continue;
				}

				// Get the y.
				int y = 4;
				if (tfbs.location < 0) {
					y = -4;
				}

				compositeFigure.add(new BasicRectangle(Math.abs(tfbs.location),
						y, 10, 4) {
					public void paint(Graphics2D g) {
						AlphaComposite c = AlphaComposite.getInstance(
								AlphaComposite.SRC_OVER, 0.3f);
						g.setComposite(c);
						g.setPaint(site.color);
						g.fill(this.getShape());
						g.setComposite(AlphaComposite.SrcOver);
					}
				});
			}

			compositeFigure.translate(150 + seq.offset, (i + 1) * 50);
			compositeFigure.setInteractor(boundedDragger);
			layer.add(compositeFigure);
		}
	}

	class DraggerSnap extends DragInteractor {
		private double _remainderX = 0;
		protected final ActionListener _listener;
		public final double _step;
		public final int _minimum;
		public final int _maximum;

		protected DraggerSnap(ActionListener listener, double step,
				int minimum, int maximum) {
			this._listener = listener;
			this._step = step;
			this._minimum = minimum;
			this._maximum = maximum;
		}

		public void translate(LayerEvent e, double x, double y) {
			_remainderX += x;

			double translateX = ((int) ((_remainderX + _step / 2) / _step))
					* _step;
			_remainderX -= translateX;

			double figureX = e.getFigureSource().getShape().getBounds().getX();

			// If this puts us negative, then stop.
			if (figureX + translateX < _minimum) {
				translateX = -(((int) (figureX - _minimum) / _step) * _step);
			}

			// If this puts us past max, then to up to it.
			if (figureX + translateX > _maximum) {
				translateX = (((int) (_maximum - figureX)) / _step) * _step;
			}

			super.translate(e, translateX, 0);
		}

		public void mouseReleased(LayerEvent e) {
			if (null != _listener) {
				_listener.actionPerformed(new ActionEvent(this, (int) e
						.getFigureSource().getShape().getBounds().getX()
						- _minimum, "ViewPort move"));
			}
		}
	}

	class DraggerSnapSequence extends DraggerSnap {

		protected DraggerSnapSequence(ActionListener listener, double step,
				int minimum, int maximum) {

			super(listener, step, minimum, maximum);
		}

		public void mouseReleased(LayerEvent e) {
			if (null != _listener) {
				Rectangle r = e.getFigureSource().getShape().getBounds();
				_listener
						.actionPerformed(new ActionEvent(this, (int) r.getX()
								- _minimum, "Sequence move",
								((int) r.getY() - 20) / 50));
			}
		}
	}
}