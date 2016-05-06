/*
 * $RCSfile$
 * 
 * $Author: barseghian $ $Date: 2012-08-09 16:50:32 -0700 (Thu, 09 Aug 2012) $ $Revision: 30396 $
 * 
 * For Details: http://kepler-project.org
 * 
 * Copyright (c) 2007 The Regents of the University of California. All rights
 * reserved.
 * 
 * Permission is hereby granted, without written agreement and without license
 * or royalty fees, to use, copy, modify, and distribute this software and its
 * documentation for any purpose, provided that the above copyright notice and
 * the following two paragraphs appear in all copies of this software.
 * 
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF
 * CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE PROVIDED HEREUNDER IS ON AN
 * "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO PROVIDE
 * MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */
package org.kepler.monitor;

import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import org.kepler.monitor.figure.BaseFigure;
import org.kepler.monitor.figure.ProgressBarFigure;
import org.kepler.monitor.figure.QualityFigure;
import org.kepler.monitor.figure.TrafficLightFigure;

import ptolemy.actor.IOPort;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.SingletonAttribute;
import diva.canvas.Figure;
import diva.canvas.toolbox.LabelFigure;

/**
 * An instance of this class is automatically added by the monitor manager for
 * each entity to be monitored.
 * 
 * <p>
 * This attribute contains a MonitorIcon.
 * 
 * @author Carlos Rueda
 * @version $Id: MonitorAttribute.java 30396 2012-08-09 23:50:32Z barseghian $
 */
public class MonitorAttribute extends SingletonAttribute {
	public StringParameter iconType;

	public StringParameter timerDelay;

	/**
	 * Creates a monitor attribute.
	 */
	public MonitorAttribute(NamedObj container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		iconType = new StringParameter(this, "iconType");
		String[] iconTypes = MonitorAttribute.iconTypes();
		iconType.setExpression(iconTypes[0]);
		for (int i = 0; i < iconTypes.length; i++) {
			// don't show the COUNTER style:
			if (!iconTypes[i].equals(MonitorAttribute.COUNTER)) {
				iconType.addChoice(iconTypes[i]);
			}
		}
		iconType.setDisplayName("Icon type");

		timerDelay = new StringParameter(this, "timerDelay");
		timerDelay.setExpression("1000");
		timerDelay.setDisplayName("Timer delay (ms)");

		_icon = new MonitorIcon(this, "_icon");
	}

	public void setMonitoredStatus(MonitoredStatus status) {
		_status = status;
	}

	/**
	 * @param updater
	 *            the FigureUpdater to set
	 */
	public void setFigureUpdater(FigureUpdater updater) {
		_figureUpdater = updater;
	}

	public FigureUpdater getFigureUpdater() {
		return _figureUpdater;
	}

	/**
	 * @return the IOPort. null by default.
	 */
	public IOPort getPort() {
		return port;
	}

	/**
	 * @param port
	 *            the IOPort to set
	 */
	public void setPort(IOPort port) {
		this.port = port;
	}

	/**
	 * Do updates according to the changed attribute.
	 */
	public void attributeChanged(Attribute attribute)
			throws IllegalActionException {

		if (attribute == iconType) {

			// Note: for simplicity, just recreate the figure.

			Figure fig;

			String iconTypeValue = iconType.stringValue();
			if (iconTypeValue.equals(TRAFFIC_LIGHT3_VRT)) {
				fig = new TrafficLightFigure(3, new Rectangle2D.Double(3, 3,
						_radius, 3 * _radius));
			} else if (iconTypeValue.equals(TRAFFIC_LIGHT3_HRZ)) {
				fig = new TrafficLightFigure(3, new Rectangle2D.Double(3, 3,
						3 * _radius, _radius));
				((TrafficLightFigure) fig)
						.setOrientation(BaseFigure.Orientation.HORIZONTAL);
			} else if (iconTypeValue.equals(TRAFFIC_LIGHT2_VRT)) {
				fig = new TrafficLightFigure(2, new Rectangle2D.Double(3, 3,
						_radius, 2 * _radius));
			} else if (iconTypeValue.equals(TRAFFIC_LIGHT2_HRZ)) {
				fig = new TrafficLightFigure(2, new Rectangle2D.Double(3, 3,
						2 * _radius, _radius));
				((TrafficLightFigure) fig)
						.setOrientation(BaseFigure.Orientation.HORIZONTAL);
			} else if (iconTypeValue.equals(TRAFFIC_LIGHT1)) {
				fig = new TrafficLightFigure(1, new Rectangle2D.Double(3, 3,
						_radius, _radius));
			} else if (iconTypeValue.equals(PROGRESS_BAR_HRZ)) {
				ProgressBarFigure pb = new ProgressBarFigure(
						new RoundRectangle2D.Double(3, 3, 6 * _radius, _radius,
								_radius, _radius));
				pb.setOrientation(BaseFigure.Orientation.HORIZONTAL);
				pb.setIndeterminate(true);
				fig = pb;
			} else if (iconTypeValue.equals(PROGRESS_BAR_VRT)) {
				ProgressBarFigure pb = new ProgressBarFigure(
						new RoundRectangle2D.Double(3, 3, _radius, 3 * _radius,
								_radius, _radius));
				pb.setOrientation(BaseFigure.Orientation.VERTICAL);
				pb.setIndeterminate(true);
				fig = pb;
			} else if (iconTypeValue.equals(COUNTER)) {
				fig = new LabelFigure("0",
						new Font("monospaced", Font.PLAIN, 8));

				// FIXME if the figure is set to a different style later,
				// the background color is lost.
				_icon.setBackgroundColor(null);
			} else if (iconTypeValue.equals(QUALITY_FIGURE)) { 
				fig = new QualityFigure(new Rectangle2D.Double(3, 3,
						_radius, 3 * _radius));
			} else {
				throw new IllegalActionException("Unexpected iconType: "
						+ iconTypeValue);
			}

			_icon.setFigure(fig);

			if (_figureUpdater != null) {
				_figureUpdater.setFigure(fig);
			}
		} else if (attribute == timerDelay) {
			int delay;
			try {
				delay = Integer.parseInt(timerDelay.stringValue());
			} catch (NumberFormatException e) {
				throw new IllegalActionException("NumberFormatException: " + e);
			}

			if (_status != null) {
				_status.getPropertyTimer().setDelay(delay);
			}
		} else {
			super.attributeChanged(attribute);
		}
	}

	/** If this component is removed from its container, finish monitoring */
	public void setContainer(NamedObj container) throws IllegalActionException,
			NameDuplicationException {
		super.setContainer(container);
		if (container == null && _figureUpdater != null) {
			_figureUpdater.stop();
		}
		// else: what TODO if container != getContainer() ?
	}

	public MonitorIcon getIcon() {
		return _icon;
	}

	// ///////////////////////////////////////////////////////////////
	// package private members ////

	/**
	 * Returns the types of the supported styles.
	 */
	static String[] iconTypes() {
		return ICON_TYPES;
	}

	static final String COUNTER = "counter";

	// ///////////////////////////////////////////////////////////////
	// private members ////

	private static final String TRAFFIC_LIGHT3_HRZ = "horizontal trafficLight3";

	private static final String TRAFFIC_LIGHT3_VRT = "vertical trafficLight3";

	private static final String TRAFFIC_LIGHT2_HRZ = "horizontal trafficLight2";

	private static final String TRAFFIC_LIGHT2_VRT = "vertical trafficLight2";

	private static final String TRAFFIC_LIGHT1 = "trafficLight1";

	private static final String PROGRESS_BAR_HRZ = "horizontal progress bar";

	private static final String PROGRESS_BAR_VRT = "vertical progress bar";
		
	private static final String  QUALITY_FIGURE = "quality figure"; 

	private static final String[] ICON_TYPES = { TRAFFIC_LIGHT3_HRZ,
			TRAFFIC_LIGHT3_VRT, TRAFFIC_LIGHT2_HRZ, TRAFFIC_LIGHT2_VRT,
			TRAFFIC_LIGHT1, PROGRESS_BAR_HRZ, PROGRESS_BAR_VRT, COUNTER, QUALITY_FIGURE };

	// TODO parameterize
	private static final double _radius = 8;

	private MonitoredStatus _status;

	private FigureUpdater _figureUpdater;

	private MonitorIcon _icon;

	private IOPort port;

	private static final long serialVersionUID = 1L;
}
