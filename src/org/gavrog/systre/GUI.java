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

package org.gavrog.systre;

import java.awt.Color;
import java.awt.Insets;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;

import javax.swing.SwingUtilities;

import buoy.event.CommandEvent;
import buoy.event.WindowClosingEvent;
import buoy.widget.BButton;
import buoy.widget.BFileChooser;
import buoy.widget.BFrame;
import buoy.widget.BLabel;
import buoy.widget.BScrollBar;
import buoy.widget.BScrollPane;
import buoy.widget.BStandardDialog;
import buoy.widget.BTextArea;
import buoy.widget.BorderContainer;
import buoy.widget.GridContainer;
import buoy.widget.LayoutInfo;

/**
 * A simple GUI for Gavrog Systre (in the making).
 * 
 * @author Olaf Delgado
 * @version $Id: GUI.java,v 1.3 2006/03/24 22:24:53 odf Exp $
 */
public class GUI extends BFrame {
	final private static Color textColor = new Color(255, 250, 240);
	final private static Color buttonColor = new Color(224, 224, 240);

    private final BFileChooser inFileChooser = new BFileChooser(BFileChooser.OPEN_FILE,
            "Open data file");
    private final BFileChooser outFileChooser = new BFileChooser(BFileChooser.SAVE_FILE,
            "Save output");
    
    private final Demo systre = new Demo();

    private BTextArea output;
	private BScrollBar vscroll;
    private BButton openButton;
    private BButton saveButton;

    /**
     * Constructs an instance.
     */
    public GUI() {
		super("Systre 1.0 beta");
		
		final BorderContainer main = new BorderContainer();
		main.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null,
                null));
		main.setBackground(textColor);

		final BorderContainer top = new BorderContainer();
		top.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE,
				new Insets(5, 5, 5, 5), null));
		top.setBackground(null);
		final BLabel label = new BLabel("<html><h1>Gavrog Systre</h1><br>"
				+ "Version 1.0 beta 0<br><br>"
				+ "by Olaf Delgado-Friedrichs 2001-2006</html>");
		top.add(label, BorderContainer.NORTH);
        
        final GridContainer buttonBar = new GridContainer(2, 1);
        buttonBar.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER,
                LayoutInfo.HORIZONTAL, null, null));
        
        buttonBar.add(openButton = makeButton("Open"), 0, 0);
        buttonBar.add(saveButton = makeButton("Save"), 1, 0);
        
        top.add(buttonBar, BorderContainer.SOUTH, new LayoutInfo(LayoutInfo.CENTER,
				LayoutInfo.HORIZONTAL, null, null));
		main.add(top, BorderContainer.NORTH);
		
		output = new BTextArea(20, 40);
		output.setBackground(null);
		final BScrollPane scrollPane = new BScrollPane(output,
				BScrollPane.SCROLLBAR_ALWAYS, BScrollPane.SCROLLBAR_ALWAYS);
		scrollPane.setForceHeight(true);
		scrollPane.setForceWidth(true);
		this.vscroll = scrollPane.getVerticalScrollBar();
		scrollPane.setBackground(null);
		main.add(scrollPane, BorderContainer.CENTER);
		
		final BButton okButton = makeButton("Exit");
        main.add(okButton, BorderContainer.SOUTH, new LayoutInfo(LayoutInfo.CENTER,
				LayoutInfo.NONE, new Insets(5, 5, 5, 5), null));
        
        setContent(main);
        
        captureOutput();
        
        openButton.addEventLink(CommandEvent.class, this, "doOpen");
        saveButton.addEventLink(CommandEvent.class, this, "doSave");
        okButton.addEventLink(CommandEvent.class, this, "doQuit");
        addEventLink(WindowClosingEvent.class, this, "doQuit");
        
        pack();
        setVisible(true);
    }
    
    private BButton makeButton(final String label) {
    	final BButton button = new BButton(label);
    	button.setBackground(buttonColor);
    	return button;
    }
    
    private void captureOutput() {
        final OutputStream stream = new OutputStream() {
            public void write(int b) throws IOException {
                output.append(new String(new char[] { (char) b }));
                if ((char) b == '\n') {
                    // --- finally found out what invokeLater is for :)
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            vscroll.setValue(vscroll.getMaximum());
                        }
                    });
                }
            }
        };

        this.systre.setOutStream(new PrintStream(stream));
    }
    
    protected void doOpen() {
        final boolean success = this.inFileChooser.showDialog(this);
        if (success) {
            final String filename = this.inFileChooser.getSelectedFile().getName();
            final File dir = this.inFileChooser.getDirectory();
            final String path = new File(dir, filename).getAbsolutePath();
            this.output.setText("");
            new Thread(new Runnable() {
                public void run() {
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            public void run() {
                                disableButtons();
                            }
                        });
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                    systre.processDataFile(path);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            enableButtons();
                        }
                    });
                }
            }).start();
        }
    }
    
    protected void doSave() {
        final String name = systre.getLastFileNameWithoutExtension();
        this.outFileChooser.setSelectedFile(new File(name + ".out"));
        final boolean success = this.outFileChooser.showDialog(this);
        if (success) {
            final String filename = this.outFileChooser.getSelectedFile().getName();
            final File dir = this.outFileChooser.getDirectory();
            final File file = new File(dir, filename);
            if (file.exists()) {
                final int choice = new BStandardDialog("File exists", "File \"" + file
                        + "\" already exists. Overwrite?", BStandardDialog.WARNING)
                        .showOptionDialog(this, new String[] { "yes", "no" }, "no");
                if (choice > 0) {
                    return;
                }
            }
            try {
                final Writer writer = new FileWriter(file);
                writer.write(this.output.getText());
                writer.flush();
                writer.close();
            } catch (IOException ex) {
            }
        }
    }
    
    private void disableButtons() {
        this.openButton.setEnabled(false);
        this.saveButton.setEnabled(false);
    }
    
    private void enableButtons() {
        this.openButton.setEnabled(true);
        this.saveButton.setEnabled(true);
    }
    
    protected void doQuit() {
        System.exit(0);
    }
    
    public static void main(final String args[]) {
        new GUI();
    }
}
