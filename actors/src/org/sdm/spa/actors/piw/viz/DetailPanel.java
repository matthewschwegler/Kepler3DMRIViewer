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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import diva.canvas.CompositeFigure;
import diva.canvas.FigureLayer;
import diva.canvas.GraphicsPane;
import diva.canvas.JCanvas;
import diva.canvas.toolbox.BasicRectangle;
import diva.canvas.toolbox.LabelFigure;

public class DetailPanel extends JCanvas {

	private static final long serialVersionUID = 3257288028421894961L;
	private int _startViewPort;
	private int _sizeViewPort;

	private final SequenceCollection _sequenceCollection;
	private final SiteCollection _siteCollection;

	public DetailPanel(SequenceCollection sequenceCollection,
			SiteCollection siteCollection, ActionListener listenerViewPortSize) {

		// Set default view port parameters.
		_sizeViewPort = 100;
		_startViewPort = 0;

		this._sequenceCollection = sequenceCollection;
		this._siteCollection = siteCollection;

		this.addComponentListener(new ListenerComponent(listenerViewPortSize));
	}

	public void drawSequences() {
		drawSequences(_startViewPort);
	}

	public void drawSequences(int startViewPort) {
		this._startViewPort = startViewPort;

		this.clear();

		FigureLayer layer = ((GraphicsPane) this.getCanvasPane())
				.getForegroundLayer();

		// Draw consensus sequence.
		SequenceFigure sequenceFigure = new SequenceFigure(new Sequence("",
				"consensus", _sequenceCollection.getConsensus(),
				new TranscriptionFactorBindingSite[] {}), _startViewPort,
				_sizeViewPort, _sequenceCollection.getConsensus());

		sequenceFigure.translate(0, 50);
		layer.add(sequenceFigure);

		for (int i = 0; i < _sequenceCollection.size(); i++) {
			Sequence sequence = _sequenceCollection.getSequence(i);

			sequenceFigure = new SequenceFigure(sequence, _startViewPort,
					_sizeViewPort, _sequenceCollection.getConsensus());

			// Draw the sequence.
			sequenceFigure.translate(0, (i + 2) * 50);
			layer.add(sequenceFigure);
		}

		// Set preferred size.
		this.setPreferredSize(new Dimension(_sizeViewPort + 200,
				(_sequenceCollection.size() + 2) * 50));
	}

	public void clear() {
		((GraphicsPane) this.getCanvasPane()).getForegroundLayer().clear();
	}

	private class ListenerComponent extends ComponentAdapter {
		private final ActionListener _listener;

		protected ListenerComponent(ActionListener listener) {
			_listener = listener;
		}

		public void componentResized(ComponentEvent e) {
			Component component = e.getComponent();

			_sizeViewPort = (component.getWidth() - 200) / 7;

			if (null != _listener) {
				_listener.actionPerformed(new ActionEvent(this, _sizeViewPort,
						""));
			}
		}
	}

	private class SequenceFigure extends CompositeFigure {

		public SequenceFigure(Sequence sequence, int startViewPort,
				int sizeViewPort, String strConsensus) {

			// Add the gene ID label.
			LabelFigure label = new LabelFigure(sequence.geneID, "SansSerif",
					Font.ITALIC, 14);
			label.translate(20, 0);
			this.add(label);

			CompositeFigure compositeFigure = new CompositeFigure();
			shadeConsensus(compositeFigure, startViewPort, sizeViewPort,
					strConsensus);

			// Draw all the transcription factor binding sites.
			for (int i = 0; i < sequence.arrTFBSs.length; i++) {

				final TranscriptionFactorBindingSite tfbs = sequence.arrTFBSs[i];
				final SiteCollection.Site site = _siteCollection
						.getSite(tfbs.name);

				// If not visible, then don't paint.
				if (!site.selected) {
					continue;
				}

				// Count of non-hyphens.
				int count = 0;
				for (int j = sequence.getActualIndex(Math.abs(tfbs.location));; j++) {
					// If we're past the end of the sequence, then stop.
					if (j >= sequence.alignedSequence.length()
							+ sequence.offset) {
						break;
					}

					// If we're past the end of the viewport, then stop.
					if (j >= startViewPort + sizeViewPort) {
						break;
					}

					// If we see a gap, then go on to the next one.
					if ('-' == sequence.alignedSequence.charAt(j
							- sequence.offset)) {
						continue;
					}

					count++;

					// If we're in the view port, then add.
					if (j >= startViewPort) {
						compositeFigure.add(new BasicRectangle(
								(j - startViewPort) * 7, -10, 7, 15) {
							public void paint(Graphics2D g) {
								AlphaComposite c = AlphaComposite.getInstance(
										AlphaComposite.SRC_OVER, 0.2f);
								g.setComposite(c);
								g.setPaint(site.color);
								g.fill(this.getShape());
								g.setComposite(AlphaComposite.SrcOver);
							}
						});
					}

					if (10 == count) {
						break;
					}
				}
			}

			// Draw the actual sequence.
			compositeFigure
					.add(new LabelFigure(sequence.subsequence(startViewPort,
							sizeViewPort), "Monospaced", Font.PLAIN, 12));

			// Add that composite figure to this.
			compositeFigure.translate(150, 0);
			this.add(compositeFigure);
		}

		private void shadeConsensus(CompositeFigure compositeFigure,
				int startViewPort, int sizeViewPort, String strConsensus) {

			if (startViewPort >= strConsensus.length()) {
				return;
			}

			strConsensus = strConsensus.substring(startViewPort);
			int i = 0;
			while (i < sizeViewPort && 0 < strConsensus.length()) {
				// If not consensus, then jump ahead.
				if ('-' == strConsensus.charAt(0)) {
					String next = strConsensus.replaceFirst("-+", "");
					i += strConsensus.length() - next.length();
					strConsensus = next;
					continue;
				}

				// If the consensus sequence isn't long enough, then don't
				// shade.
				String oldconsensus = strConsensus;
				int oldi = i;
				strConsensus = strConsensus.replaceFirst("[^-]+", "");
				int length = oldconsensus.length() - strConsensus.length();
				i += length;

				if (length < _sequenceCollection.getMinimumConsensusLength()) {
					continue;
				}

				if (i >= sizeViewPort) {
					length = sizeViewPort - oldi;
				}

				compositeFigure.add(new BasicRectangle(oldi * 7, -10,
						7 * length, 15) {
					public void paint(Graphics2D g) {
						AlphaComposite c = AlphaComposite.getInstance(
								AlphaComposite.SRC_OVER, 0.3f);
						g.setComposite(c);
						g.setPaint(new Color(175, 175, 175));
						g.fill(this.getShape());
						g.setComposite(AlphaComposite.SrcOver);
					}
				});
			}
		}

	}
}