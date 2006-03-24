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
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.SwingUtilities;

import buoy.event.CommandEvent;
import buoy.event.WindowClosingEvent;
import buoy.widget.BButton;
import buoy.widget.BFrame;
import buoy.widget.BLabel;
import buoy.widget.BScrollBar;
import buoy.widget.BScrollPane;
import buoy.widget.BTextArea;
import buoy.widget.BorderContainer;
import buoy.widget.LayoutInfo;

/**
 * A simple GUI for Gavrog Systre (in the making).
 * 
 * @author Olaf Delgado
 * @version $Id: GUI.java,v 1.2 2006/03/24 18:36:23 odf Exp $
 */
public class GUI extends BFrame {
	final private static Color textColor = new Color(255, 250, 240);
	final private static Color buttonColor = new Color(224, 224, 240);
	private BTextArea output;
	private BScrollBar vscroll;
	private PrintStream stdout;
	private PrintStream stderr;

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
		final BButton openButton = makeButton("Open file");
		openButton.setEnabled(false);
		top.add(openButton, BorderContainer.SOUTH, new LayoutInfo(LayoutInfo.CENTER,
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
        
        captureOutput(true);
        
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
    
    private void captureOutput(final boolean capture) {
    	if (capture) {
    		this.stdout = System.out;
    		this.stderr = System.err;
    		
    		final PrintStream stream = new PrintStream(new OutputStream() {
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
			});
			System.setOut(stream);
			System.setErr(stream);
    	} else { 
    		System.setOut(stdout);
    		System.setErr(stderr);
    	}
    }
    
    protected void doQuit() {
        System.exit(0);
    }
    
    public static void main(final String args[]) {
        new GUI();
        final Demo demo = new Demo();
        demo.processDataFile(args[0]);
    }
}
