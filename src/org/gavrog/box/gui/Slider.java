/*
   Copyright 2008 Olaf Delgado-Friedrichs

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/


package org.gavrog.box.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Shape;

import buoy.event.MouseDraggedEvent;
import buoy.event.MousePressedEvent;
import buoy.event.MouseReleasedEvent;
import buoy.event.RepaintEvent;
import buoy.event.ValueChangedEvent;
import buoy.event.WidgetMouseEvent;
import buoy.widget.CustomWidget;

public class Slider extends CustomWidget {
	private double value;
	final private double min;
	final private double max;
	private Point clickPos;
	private boolean showTicks;
	private boolean showValue;
	private double majorTickSpacing;
	private double minorTickSpacing;
	private double snapInterval;

	public Slider(final double value, final double min, final double max) {
		this.min = min;
		this.max = Math.max(min, max);
		
	    addEventLink(MousePressedEvent.class, this, "mousePressed");
	    addEventLink(MouseReleasedEvent.class, this, "mouseReleased");
	    addEventLink(MouseDraggedEvent.class, this, "mouseDragged");
	    addEventLink(RepaintEvent.class, this, "paint");
	    
	    setValue(value);
	}
	
	public Dimension getPreferredSize() {
		final int width = showValue ? 210 : 180;
		final int height = showTicks ? 13 : 11;
		return new Dimension(width, height);
	}
	
	public void paint(final RepaintEvent ev) {
		final Graphics2D g = ev.getGraphics();
		if (g == null) return;
		
		g.setStroke(new BasicStroke(1));
		
		// -- clear canvas
		g.setColor(getBackground());
		g.fillRect(0, 0, getBounds().width, getBounds().height);
		
		// -- draw guide
		g.setColor(Color.WHITE);
		g.fillRect(3, 2, sliderWidth(), 5);
		g.setColor(Color.GRAY);
		g.drawRect(3, 2, sliderWidth(), 5);
		
		// -- draw ticks
		if (showTicks) {
			g.setColor(Color.GRAY);
			for (double t = min; t <= max; t += minorTickSpacing) {
				final int x = valueToX(t) + 3;
				g.drawLine(x, 8, x, 11);
			}
			for (double t = min; t <= max; t += majorTickSpacing) {
				final int x = valueToX(t) + 3;
				g.drawLine(x, 8, x, 13);
			}
		}
		
		// -- show the current value
		if (showValue) {
			final Font f = new Font("Verdana", Font.PLAIN, 10);
			g.setFont(f);
			g.setColor(new Color(0.0f, 0.4f, 0.6f));
			final String s;
			if (value == (int) value) {
				s = String.format("%d", (int) value);
			} else {
				s = String.format("%.2f", value);
			}
			g.drawString(s, sliderWidth() + 8, 10);
		}
		
		// -- fill guide left of marker
		final int x = valueToX(value);

		g.setColor(new Color(0.9f, 0.45f, 0.15f));
		g.drawLine(4, 3, x, 3);
		g.setColor(new Color(1.0f, 0.6f, 0.2f));
		g.drawLine(4, 4, x, 4);
		g.setColor(new Color(1.0f, 0.75f, 0.5f));
		g.drawLine(4, 5, x, 5);
		
		// -- draw marker
		final Shape sh = new Polygon(
				new int[] { x, x + 6, x + 6, x + 3, x },
				new int[] { 0,     0,     8,    11, 8 },
				5
				);
		g.setColor(new Color(0.9f, 0.9f, 0.9f));
		g.drawLine(x + 1, 0, x + 1,  9);
		g.drawLine(x + 2, 0, x + 2, 10);
		g.setColor(new Color(0.8f, 0.88f, 0.92f));
		g.drawLine(x + 3, 0, x + 3, 11);
		g.setColor(new Color(0.6f, 0.76f, 0.84f));
		g.drawLine(x + 4, 0, x + 4, 10);
		g.setColor(new Color(0.5f, 0.7f, 0.8f));
		g.drawLine(x + 5, 0, x + 5,  9);
		g.setColor(Color.BLACK);
		g.draw(sh);
	}

	private int sliderWidth() {
		return getBounds().width - 7 - (showValue ? 30 : 0);
	}
	
	private int valueToX(final double val) {
		return (int) Math.round(sliderWidth() * (val - min) / (max - min));
	}
	
	private double xToValue(final int x) {
		return min + (double) x / sliderWidth() * (max - min);
	}
	
	public void setShowTicks(final boolean b) {
		this.showTicks = b;
	}

	public void setShowValue(final boolean b) {
		this.showValue = b;
	}

	public void setMajorTickSpacing(final double major) {
		this.majorTickSpacing = major;
	}

	public void setMinorTickSpacing(final double minor) {
		this.minorTickSpacing = minor;
	}

	public void setSnapInterval(final double snap) {
		this.snapInterval = snap;
	}

	@SuppressWarnings("unused")
	private void mousePressed(MousePressedEvent ev) {
		clickPos = ev.getPoint();
		final int x = valueToX(value);
		if (clickPos.x < x || clickPos.x > x + 4) {
			clickPos = new Point(x, clickPos.y);
			mouseDragged(ev);
		}
	}

	private void mouseDragged(WidgetMouseEvent ev) {
		setValue(xToValue(ev.getPoint().x));
	}

	@SuppressWarnings("unused")
	private void mouseReleased(MouseReleasedEvent ev) {
		final Point pos = ev.getPoint();
		if (pos.x != clickPos.x) {
			dispatchEvent(new ValueChangedEvent(this));
		}
	}
	  
	public double getValue() {
		return value;
	}

	public void setValue(final double newValue) {
		value = newValue;
		if (value < min) value = min;
		if (value > max) value = max;
		if (snapInterval > 0) {
			value = Math.round((value - min) / snapInterval) * snapInterval
					+ min;
		}
		dispatchEvent(new RepaintEvent(this, (Graphics2D) getComponent()
				.getGraphics()));
	}
}
