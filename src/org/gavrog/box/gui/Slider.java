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
import java.awt.Rectangle;
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
	private boolean showLabels;
	private double majorTickSpacing;
	private double minorTickSpacing;
	private boolean snapToTicks;

	public Slider(final double value, final double min, final double max) {
		this.min = min;
		this.max = Math.max(min, max);
		
		setPreferredSize(new Dimension(120, 15));
	    addEventLink(MousePressedEvent.class, this, "mousePressed");
	    addEventLink(MouseReleasedEvent.class, this, "mouseReleased");
	    addEventLink(MouseDraggedEvent.class, this, "mouseDragged");
	    addEventLink(RepaintEvent.class, this, "paint");
	    
	    setValue(value);
	}
	
	public Dimension getPreferredSize() {
		//TODO calculate this more accurately when labels are shown
		int height = 11;
		if (showTicks) {
			height += 2;
		}
		if (showLabels) {
			height += 12;
		}
		return new Dimension(180, height);
	}
	
	public void paint(final RepaintEvent ev) {
		final Rectangle dim = getBounds();
		final int w = dim.width;
		final int h = dim.height;
		
		final Graphics2D g = ev.getGraphics();
		if (g == null) return;
		
		g.setStroke(new BasicStroke(1));
		
		// -- clear canvas
		g.setColor(getBackground());
		g.fillRect(0, 0, w, h);
		
		// -- draw guide
		g.setColor(Color.WHITE);
		g.fillRect(3, 2, w - 7, 5);
		g.setColor(Color.GRAY);
		g.drawRect(3, 2, w - 7, 5);
		
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
		
		// -- draw labels
		if (showLabels) {
			final Font f = new Font("Verdana", Font.BOLD, 9);
			g.setFont(f);
			g.setColor(Color.DARK_GRAY);
			for (double t = min; t <= max; t += majorTickSpacing) {
				final int x = valueToX(t) + 3;
				//TODO make this nicer
				final String s;
				if (t == (int) t) {
					s = String.valueOf((int) t);
				} else {
					s = String.valueOf(t);
				}
				g.drawString(s, x, 23);
			}
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
		g.setColor(new Color(0.8f, 0.8f, 1.0f));
		g.drawLine(x + 3, 0, x + 3, 11);
		g.setColor(new Color(0.6f, 0.6f, 1.0f));
		g.drawLine(x + 4, 0, x + 4, 10);
		g.setColor(new Color(0.5f, 0.5f, 1.0f));
		g.drawLine(x + 5, 0, x + 5,  9);
		g.setColor(Color.BLACK);
		g.draw(sh);
	}

	private int valueToX(final double val) {
		final int w = getBounds().width - 7;
		return (int) Math.round(w * (val - min) / (max - min));
	}
	
	private double xToValue(final int x) {
		final int w = getBounds().width - 7;
		return min + (double) x / w * (max - min);
	}
	
	public void setShowTicks(final boolean b) {
		this.showTicks = b;
	}

	public void setShowLabels(final boolean b) {
		this.showLabels = b;
	}

	public void setMajorTickSpacing(final double major) {
		this.majorTickSpacing = major;
	}

	public void setMinorTickSpacing(final double minor) {
		this.minorTickSpacing = minor;
	}

	public void setSnapToTicks(final boolean snap) {
		this.snapToTicks = snap;
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
		if (snapToTicks) {
			value = Math.round((value - min) / minorTickSpacing)
					* minorTickSpacing + min;
		}
		dispatchEvent(new RepaintEvent(this, (Graphics2D) getComponent()
				.getGraphics()));
	}
}
