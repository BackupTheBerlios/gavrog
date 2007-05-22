/*
   Copyright 2007 Olaf Delgado-Friedrichs

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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import javax.swing.JColorChooser;
import javax.swing.JDialog;

import buoy.event.CommandEvent;
import buoy.event.EventProcessor;
import buoy.widget.BButton;
import buoy.widget.BLabel;
import buoy.widget.LayoutInfo;
import buoy.widget.RowContainer;

public class OptionColorBox extends RowContainer {
	static private Color currentColor = null;
	final static private JColorChooser chooser = new JColorChooser();
	final static private ActionListener onOk = new ActionListener() {
		public void actionPerformed(final ActionEvent ev) {
			currentColor = chooser.getColor();
		}
	};
	final static private ActionListener onCancel = new ActionListener() {
		public void actionPerformed(final ActionEvent ev) {
			currentColor = null;
		}
	};
	final static private JDialog dialog = JColorChooser.createDialog(null,
			"Choose a color", true, chooser, onOk, onCancel);
	
	public OptionColorBox(
			final String label, final Object target, final String option)
			throws Exception {
		super();
		this.setBackground(null);

		this.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE,
				new Insets(2, 5, 2, 5), null));

		final BButton color = new BButton();
		this.add(color);
		this.add(new BLabel(label));

		final PropertyDescriptor prop = Config.namedProperty(target, option);
		if (prop == null) {
			throw new IllegalArgumentException("Target class has no property "
					+ option);
		}
		final Method getter = prop.getReadMethod();
		final Method setter = prop.getWriteMethod();

		color.setBackground((Color) getter.invoke(target, null));
		color.addEventLink(CommandEvent.class, new EventProcessor() {
			public void handleEvent(final Object event) {
				try {
					dialog.setVisible(true);
					if (currentColor != null) {
						color.setBackground(currentColor);
						setter.invoke(target, new Object[] { currentColor });
					}
				} catch (final Exception ex) {
				}
			}
		});
	}
}
