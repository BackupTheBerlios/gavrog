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

import java.awt.Color;
import java.awt.Insets;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.gavrog.box.simple.Strings;

import buoy.event.EventProcessor;
import buoy.event.ValueChangedEvent;
import buoy.widget.BLabel;
import buoy.widget.BTextField;
import buoy.widget.LayoutInfo;
import buoy.widget.RowContainer;

public class OptionInputBox extends RowContainer {
	final private static Map mappedTypes = new HashMap();
	static {
		mappedTypes.put(int.class, Integer.class);
		mappedTypes.put(long.class, Long.class);
		mappedTypes.put(float.class, Float.class);
		mappedTypes.put(double.class, Double.class);
		mappedTypes.put(Color.class, ColorWrapper.class);
	}
	
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

		final Class klazz = (target instanceof Class ? (Class) target : target
				.getClass());
		final String optionCap = Strings.capitalized(option);
		final Method getter = klazz.getMethod("get" + optionCap, null);
		final Class optionType = getter.getReturnType();
		final Class mappedType = mappedType(optionType);
		final Constructor fromString = mappedType
				.getConstructor(new Class[] { String.class });
		final Constructor wrapper = mappedType
				.getConstructor(new Class[] { optionType });
		final Method setter = klazz.getMethod("set" + optionCap,
				new Class[] { optionType });
		final Object oldValue = getter.invoke(target, null);
		final Object mappedValue = wrapper.newInstance(new Object[] { oldValue });
		
		this.input.setText(String.valueOf(mappedValue));

		this.input.addEventLink(ValueChangedEvent.class, new EventProcessor() {
			public void handleEvent(final Object event) {
				try {
					final Object value = fromString
							.newInstance(new Object[] { input.getText() });
					setter.invoke(target, new Object[] { value });
				} catch (final Exception ex) {
				}
			}
		});
	}
	
	private Class mappedType(final Class type) {
		if (mappedTypes.containsKey(type)) {
			return (Class) mappedTypes.get(type);
		} else {
			return type;
		}
	}
}
