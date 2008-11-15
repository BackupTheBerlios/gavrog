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

import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.lang.reflect.Method;

import org.gavrog.box.simple.Strings;

import buoy.event.EventProcessor;
import buoy.event.EventSource;
import buoy.event.ValueChangedEvent;
import buoy.widget.BLabel;
import buoy.widget.BorderContainer;
import buoy.widget.LayoutInfo;

public class OptionSliderBox extends BorderContainer {
	private boolean eventsLocked = false;
	private final Slider slider;
	private boolean isDouble;
	private double factor = 1.0;
	private Object target;
	private Method getter;
	
	public OptionSliderBox(final String label, final Object target,
			final String option, final double min, final double max,
			final double major, final double minor, final double snap)
			throws Exception {
		super();
		this.setBackground(null);

		this.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST,
				LayoutInfo.HORIZONTAL, new Insets(2, 8, 2, 8), null));

		slider = new Slider(min, min, max);
		slider.setBackground(null);
		slider.setShowTicks(true);
		slider.setShowValue(true);
		slider.setMajorTickSpacing(major);
		slider.setMinorTickSpacing(minor);
		slider.setSnapInterval(snap);
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
		slider.setValue(val);
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

	public void setShowValue(boolean show) {
		this.slider.setShowValue(show);
	}

	public void setShowTicks(boolean show) {
		this.slider.setShowTicks(show);
	}

	public void setSnapInterval(final double snap) {
		this.slider.setSnapInterval(snap);
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
