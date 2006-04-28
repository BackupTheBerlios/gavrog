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


package org.gavrog.systre;

import java.awt.Insets;
import java.io.File;
import java.lang.reflect.Method;

import org.gavrog.box.simple.Misc;
import org.gavrog.box.simple.Strings;

import buoy.event.CommandEvent;
import buoy.event.EventProcessor;
import buoy.event.ValueChangedEvent;
import buoy.event.WindowClosingEvent;
import buoy.widget.BButton;
import buoy.widget.BFileChooser;
import buoy.widget.BFrame;
import buoy.widget.BLabel;
import buoy.widget.BScrollPane;
import buoy.widget.BTextArea;
import buoy.widget.BorderContainer;
import buoy.widget.ColumnContainer;
import buoy.widget.LayoutInfo;

public class FileOptionPane extends BorderContainer {
    final private BFileChooser fileChooser;
	final private BTextArea nameField;
	final private Object target;
	final private Method setter;

	public FileOptionPane(
			final BFileChooser.SelectionMode mode, final String labelText,
			final Object target, final String option) throws Exception {
		
		super();
		this.setBackground(null);

		this.target = target;
	    this.fileChooser = new BFileChooser(mode, labelText);
	    
		final Class klazz = (target instanceof Class ? (Class) target : target.getClass());
		final String optionCap = Strings.capitalized(option);
		final Method getter = klazz.getMethod("get" + optionCap, null);
		this.setter = klazz.getMethod("set" + optionCap, new Class[] { File.class });
		
		final BLabel label = new BLabel(labelText);
		label.setBackground(null);
		this.add(label, BorderContainer.NORTH, new LayoutInfo(LayoutInfo.WEST,
				LayoutInfo.NONE, null, null));

		this.nameField = new BTextArea(1, 20);
		final BScrollPane scrollPane = new BScrollPane(nameField,
				BScrollPane.SCROLLBAR_ALWAYS, BScrollPane.SCROLLBAR_NEVER);
		scrollPane.setForceHeight(false);
		scrollPane.setForceWidth(true);
		scrollPane.setBackground(null);
		this.add(scrollPane, BorderContainer.CENTER, new LayoutInfo(LayoutInfo.CENTER,
				LayoutInfo.HORIZONTAL, new Insets(5, 0, 0, 5), null));

		final BButton browseButton = new BButton("Browse...");
		browseButton.setBackground(null);
		this.add(browseButton, BorderContainer.EAST, new LayoutInfo(LayoutInfo.EAST,
				LayoutInfo.NONE, null, null));
		
		final File current = (File) getter.invoke(target, null);
		if (current != null) {
			this.nameField.setText(current.getName());
		} else {
			this.nameField.setText("");
		}

		this.nameField.addEventLink(ValueChangedEvent.class, this, "doUpdate");
		browseButton.addEventLink(CommandEvent.class, this, "doBrowse");
	}
	
	public void doUpdate() {
		final String text = this.nameField.getText();
		try {
			this.setter.invoke(this.target, new Object[] { new File(text) });
		} catch (final Exception ex) {
		}
	}
	
	public void doBrowse() {
        final boolean success = this.fileChooser.showDialog(this);
		if (success) {
			final String filename = this.fileChooser.getSelectedFile().getName();
			final File dir = this.fileChooser.getDirectory();
			final String path = new File(dir, filename).getAbsolutePath();
			this.nameField.setText(path);
			doUpdate();
		}
	}
	
	public static void main(final String args[]) {
		class Dummy {
			private File file = new File("");
			public File getFile() {
				return file;
			}
			public void setFile(final File file) {
				this.file = file;
			}
		};
		final Dummy target1 = new Dummy();
		final Dummy target2 = new Dummy();
		target1.setFile(new File(""));
		
		final BFrame frame = new BFrame();
		final ColumnContainer bc = new ColumnContainer();
		
		try {
			bc.add(new FileOptionPane(BFileChooser.OPEN_FILE, "Select File 1", target1,
					"file"), new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.HORIZONTAL,
					new Insets(5, 5, 5, 5), null));
			bc.add(new FileOptionPane(BFileChooser.OPEN_FILE, "Select File 2", target2,
					"file"), new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.HORIZONTAL,
					new Insets(5, 5, 5, 5), null));
		} catch (final Exception ex) {
			System.err.println(Misc.stackTrace(ex));
			return;
		}
		
        frame.addEventLink(WindowClosingEvent.class, new EventProcessor() {
			public void handleEvent(final Object event) {
				System.out.println("File 1 = " + target1.getFile());
				System.out.println("File 2 = " + target2.getFile());
				System.exit(1);
			}
		});
        
		frame.setContent(bc);
		frame.pack();
		frame.setVisible(true);
	}
}
