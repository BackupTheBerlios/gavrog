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
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import buoy.event.EventProcessor;
import buoy.event.ValueChangedEvent;
import buoy.widget.BLabel;
import buoy.widget.BTextField;
import buoy.widget.LayoutInfo;
import buoy.widget.RowContainer;

public class OptionInputBox extends RowContainer {
	private BTextField input;

	public OptionInputBox(
			final String label, final Object target, final String option)
			throws Exception {
		this(label, target, option, 10);
	}
	
	public OptionInputBox(
			final String label, final Object target, final String option,
			final int size) throws Exception {

		super();
		this.setBackground(null);
		this.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE,
				new Insets(2, 5, 2, 5), null));

		this.input = new BTextField(size);
		this.add(input);
		this.add(new BLabel(label));

		final PropertyDescriptor prop = Config.namedProperty(target, option);
		if (prop == null) {
			throw new IllegalArgumentException("Target class has no property "
					+ option);
		}
		final Method getter = prop.getReadMethod();
		final Method setter = prop.getWriteMethod();
		final Class optionType = setter.getParameterTypes()[0];

		this.input.setText(Config.asString(getter.invoke(target, null)));
		this.input.addEventLink(ValueChangedEvent.class, new EventProcessor() {
			public void handleEvent(final Object event) {
				try {
					Object val = Config.construct(optionType, input.getText());
					setter.invoke(target, new Object[] { val });
				} catch (final Exception ex) {
				}
			}
		});
	}
}
