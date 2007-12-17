/*
   Copyright 2006 Olaf Delgado-Friedrichs

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

/*
Copyright 2005 Olaf Delgado-Friedrichs

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
import buoy.widget.BSpinner;
import buoy.widget.LayoutInfo;
import buoy.widget.RowContainer;

public class OptionSpinnerBox extends RowContainer {
	private boolean eventsLocked = false;
	
	private BSpinner spinner;

	public OptionSpinnerBox(final String label, final Object target, final String option)
			throws Exception {

		super();
		this.setBackground(null);
		this.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE,
				new Insets(5, 5, 5, 5), null));
		
		this.spinner = new BSpinner();
		this.add(spinner);
		this.add(new BLabel(label));
		
		final Class klazz = (target instanceof Class ? (Class) target : target.getClass());
		final String optionCap = Strings.capitalized(option);
		final Method getter = klazz.getMethod("get" + optionCap, null);
		final Method setter = klazz.getMethod("set" + optionCap,
				new Class[] { int.class });

		this.spinner.setValue(getter.invoke(target, null));

		this.spinner.addEventLink(ValueChangedEvent.class,
				new EventProcessor() {
					public void handleEvent(final Object event) {
						if (obtainLock()) {
							try {
								setter.invoke(target, new Object[] { spinner
										.getValue() });
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
						final PropertyChangeEvent e = (PropertyChangeEvent) event;
						if (e.getPropertyName().equals(option)) {
							spinner.setValue(e.getNewValue());
						}
						releaseLock();
					}
				}
			});
		}
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
}
