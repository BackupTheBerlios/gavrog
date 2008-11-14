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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.beans.PropertyChangeEvent;
import java.lang.reflect.Method;

import org.gavrog.box.simple.Strings;

import buoy.event.EventProcessor;
import buoy.event.EventSource;
import buoy.event.MouseDraggedEvent;
import buoy.event.MousePressedEvent;
import buoy.event.MouseReleasedEvent;
import buoy.event.RepaintEvent;
import buoy.event.ValueChangedEvent;
import buoy.event.WidgetMouseEvent;
import buoy.widget.BLabel;
import buoy.widget.BorderContainer;
import buoy.widget.CustomWidget;
import buoy.widget.LayoutInfo;

public class OptionSliderBox extends BorderContainer {
	private boolean eventsLocked = false;
	private final Slider slider;
	private boolean isDouble;
	private double factor = 1.0;
	private Object target;
	private Method getter;
	
	private class Slider extends CustomWidget {
		private double value;
		final private double min;
		final private double max;
		private Point clickPos;

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
			return new Dimension(120, 15);
		}
		
		public void paint(final RepaintEvent ev) {
			final Rectangle dim = getBounds();
			final int w = dim.width;
			final int h = dim.height;
			if (w < 10 || h < 12) {
				return;
			}
			final int y = h / 2;
			final int x = valueToX(value);
			
			final Graphics2D g = ev.getGraphics();
			// -- clear canvas
			g.setColor(getBackground());
			g.fillRect(0, 0, w, h);
			
			// -- draw guide
			g.setColor(Color.WHITE);
			g.fillRect(2, y - 1, w - 6, 4);
			g.setColor(Color.GRAY);
			g.drawRect(2, y - 1, w - 6, 4);
			
			// -- draw marker
			final Shape sh = new Polygon(
					new int[] { x    , x + 4, x + 4, x + 2, x     },
					new int[] { y - 4, y - 4, y + 4, y + 6, y + 4 },
					5
					);
			g.setColor(Color.WHITE);
			g.fill(sh);
			g.setColor(Color.BLACK);
			g.draw(sh);
		}

		private int valueToX(final double val) {
			final int w = getBounds().width - 6;
			return (int) Math.round(w * (value - min) / (max - min));
		}
		
		private double xToValue(final int x) {
			final int w = getBounds().width - 6;
			return min + (double) x / w * (max - min);
		}
		
		private Graphics2D getGraphics() {
			return (Graphics2D) getComponent().getGraphics();
		}
		
		public void setShowTicks(boolean b) {
			// TODO Auto-generated method stub
		}

		public void setShowLabels(boolean b) {
			// TODO Auto-generated method stub
		}

		public void setMajorTickSpacing(int major) {
			// TODO Auto-generated method stub
		}

		public void setMinorTickSpacing(int minor) {
			// TODO Auto-generated method stub
		}

		public void setSnapToTicks(boolean snap) {
			// TODO Auto-generated method stub
		}

		@SuppressWarnings("unused")
		private void mousePressed(MousePressedEvent ev) {
			clickPos = ev.getPoint();
			final int x = valueToX(value);
			if (clickPos.x < x && clickPos.x > x + 4) {
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
			dispatchEvent(new RepaintEvent(this, getGraphics()));
		}
	}
	
	public OptionSliderBox(final String label, final Object target,
			final String option, final int min, final int max, final int major,
			final int minor, final boolean snap) throws Exception {
		super();
		this.setBackground(null);

		this.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST,
				LayoutInfo.HORIZONTAL, new Insets(2, 5, 2, 5), null));

		slider = new Slider(min, min, max);
		slider.setBackground(null);
		slider.setShowTicks(true);
		slider.setShowLabels(true);
		slider.setMajorTickSpacing(major);
		slider.setMinorTickSpacing(minor);
		slider.setSnapToTicks(snap);
		this.add(slider, BorderContainer.WEST);
		this.add(new BLabel(label), BorderContainer.EAST, new LayoutInfo(
				LayoutInfo.WEST, LayoutInfo.NONE, new Insets(2, 10, 2, 10),
				null));
		
		this.target = target;
		final Class<?> klazz = (target instanceof Class ? (Class) target
				: target.getClass());
		final String optionCap = Strings.capitalized(option);
		getter = klazz.getMethod("get" + optionCap);
		final Method setter;
		
		Method t;
		boolean d = false;
		try {
			t = klazz.getMethod("set" + optionCap, int.class);
			d = false;
		} catch (NoSuchMethodException ex) {
			t = klazz.getMethod("set" + optionCap, double.class);
			d = true;
		}
		setter = t;
		isDouble = d;

		updateValue(getter.invoke(target));

		slider.addEventLink(ValueChangedEvent.class, new EventProcessor() {
			public void handleEvent(final Object event) {
				if (obtainLock()) {
					try {
						final Object arg;
						if (isDouble) {
							arg = (double) slider.getValue() * factor;
						} else {
							arg = (int) Math.round(slider.getValue() * factor);
						}
						setter.invoke(target, arg);
					} catch (final Exception ex) {
					}
					releaseLock();
				}
			}
		});
		
		if (target instanceof EventSource) {
			final EventSource s = (EventSource) target;
			s.addEventLink(PropertyChangeEvent.class, new EventProcessor() {
				public void handleEvent(Object event) {
					if (obtainLock()) {
						PropertyChangeEvent e = (PropertyChangeEvent) event;
						if (e.getPropertyName().equals(option)) {
							updateValue(e.getNewValue());
						}
						releaseLock();
					}
				}
			});
		}
	}
	
	private void updateValue(final Object newValue) {
		final double val;
		if (isDouble) {
			val = ((Double) newValue) / factor;
		} else {
			val = ((Integer) newValue) / factor;
		}
		slider.setValue((int) Math.round(val));
	}
	
	private boolean obtainLock() {
		if (this.eventsLocked) {
			return false;
		} else {
			this.eventsLocked = true;
			return true;
		}
	}
	
	private void releaseLock() {
		this.eventsLocked = false;
	}

	public void setShowLabels(boolean show) {
		this.slider.setShowLabels(show);
	}

	public void setShowTicks(boolean show) {
		this.slider.setShowTicks(show);
	}

	public void setSnapToTicks(boolean snap) {
		this.slider.setSnapToTicks(snap);
	}

	public void setFactor(double factor) {
		this.factor = factor;
		try {
			updateValue(getter.invoke(target));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
