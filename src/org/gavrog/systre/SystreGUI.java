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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.gavrog.box.simple.DataFormatException;
import org.gavrog.box.simple.Misc;
import org.gavrog.box.simple.Strings;
import org.gavrog.joss.geometry.SpaceGroupCatalogue;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;
import org.gavrog.joss.pgraphs.io.NetParser;
import org.gavrog.joss.pgraphs.io.Output;

import buoy.event.CommandEvent;
import buoy.event.EventProcessor;
import buoy.event.ValueChangedEvent;
import buoy.event.WindowClosingEvent;
import buoy.widget.BButton;
import buoy.widget.BCheckBox;
import buoy.widget.BDialog;
import buoy.widget.BFileChooser;
import buoy.widget.BFrame;
import buoy.widget.BLabel;
import buoy.widget.BScrollBar;
import buoy.widget.BScrollPane;
import buoy.widget.BStandardDialog;
import buoy.widget.BTextArea;
import buoy.widget.BorderContainer;
import buoy.widget.ColumnContainer;
import buoy.widget.GridContainer;
import buoy.widget.LayoutInfo;

/**
 * A simple GUI for Gavrog Systre.
 * 
 * @author Olaf Delgado
 * @version $Id: SystreGUI.java,v 1.27 2006/04/20 22:40:10 odf Exp $
 */
public class SystreGUI extends BFrame {
    final private static Color textColor = new Color(255, 250, 240);
	final private static Color buttonColor = new Color(224, 224, 240);
	final private static Insets defaultInsets = new Insets(5, 5, 5, 5);

    private final BFileChooser inFileChooser = new BFileChooser(BFileChooser.OPEN_FILE,
            "Open data file");
    private final BFileChooser outFileChooser = new BFileChooser(BFileChooser.SAVE_FILE,
            "Save output");

    private BTextArea output;
    private BScrollBar vscroll;
    private BButton openButton;
    private BButton nextButton;
    private BButton saveButton;
    private BButton optionsButton;
    
    private final SystreCmdline systre = new SystreCmdline();
    private NetParser parser;
	private String strippedFileName;
    private String fullFileName;
    private List bufferedNets = new LinkedList();
    
    private int count;
    
    /**
     * Constructs an instance.
     */
    public SystreGUI() {
		super("Systre 1.0 beta");
        
		final BorderContainer main = new BorderContainer();
		main.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null,
                null));
		main.setBackground(textColor);

		final BorderContainer top = new BorderContainer();
		top.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE,
				defaultInsets, null));
		top.setBackground(null);
		final BLabel label = new BLabel("<html><h1>Gavrog Systre</h1><br>"
				+ "Version 1.0 beta 1 (2006-04-18)<br><br>"
				+ "by Olaf Delgado-Friedrichs 2001-2006</html>");
		top.add(label, BorderContainer.NORTH);
        
        final GridContainer buttonBar = new GridContainer(4, 1);
        buttonBar.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER,
                LayoutInfo.HORIZONTAL, null, null));
        
        buttonBar.add(openButton = makeButton("Open..."), 0, 0);
        buttonBar.add(nextButton = makeButton("Next"), 1, 0);
        buttonBar.add(saveButton = makeButton("Save as..."), 2, 0);
        buttonBar.add(optionsButton = makeButton("Options..."), 3, 0);
        
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
				LayoutInfo.NONE, defaultInsets, null));
        
        setContent(main);
        
        captureOutput();
        
        openButton.addEventLink(CommandEvent.class, this, "doOpen");
        nextButton.addEventLink(CommandEvent.class, this, "doNext");
        saveButton.addEventLink(CommandEvent.class, this, "doSave");
        optionsButton.addEventLink(CommandEvent.class, this, "doOptions");
        okButton.addEventLink(CommandEvent.class, this, "doQuit");
        addEventLink(WindowClosingEvent.class, this, "doQuit");
        
        nextButton.setEnabled(false);
        saveButton.setEnabled(false);
        
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
            private StringBuffer buffer = new StringBuffer(128);

            public void write(int b) throws IOException {
                final char c = (char) b;
                buffer.append(c);
                if (c == '\n' || buffer.length() > 1023) {
                    flush();
                }
            }
            
            public void flush() {
                output.append(buffer.toString());
                buffer.delete(0, buffer.length());
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        vscroll.setValue(vscroll.getMaximum());
                    }
                });
            }
        };

        this.systre.setOutStream(new PrintStream(stream));
    }
    
    public void doOpen() {
        final boolean success = this.inFileChooser.showDialog(this);
        if (success) {
            final String filename = this.inFileChooser.getSelectedFile().getName();
            final File dir = this.inFileChooser.getDirectory();
            final String path = new File(dir, filename).getAbsolutePath();
            this.output.setText("");
            disableMainButtons();
            
            if (filename.endsWith(".arc")) {
                systre.processArchive(path);
            } else {
                openFile(path);
                doNext();
            }
        }
    }
    
    public void doSave() {
        final String name = this.strippedFileName;
        this.outFileChooser.setSelectedFile(new File(name + ".out"));
        final boolean success = this.outFileChooser.showDialog(this);
        if (success) {
            final String filename = this.outFileChooser.getSelectedFile().getName();
            final File dir = this.outFileChooser.getDirectory();
            final File file = new File(dir, filename);
            final boolean append;
            if (file.exists()) {
                final int choice = new BStandardDialog("Systre - File exists", "File \""
                        + file + "\" exists. Overwrite?", BStandardDialog.QUESTION)
                        .showOptionDialog(this, new String[] { "Overwrite", "Append",
                                "Cancel" }, "Cancel");
                if (choice > 1) {
                    return;
                } else {
                    append = choice == 1;
                }
            } else {
                append = false;
            }
            disableMainButtons();
            
            new Thread(new Runnable() {
                public void run() {
                    try {
                        final BufferedWriter writer = new BufferedWriter(new FileWriter(
                                file, append));
                        if (filename.endsWith(".arc")) {
                            systre.writeInternalArchive(writer);
                        } else if (filename.endsWith(".cgd")) {
                            writeBufferedAsCGD(writer);
                        } else if (filename.endsWith(".pgr")) {
                            writeBufferedAsPGR(writer);
                        } else {
                            writer.write(output.getText());
                        }
                        writer.flush();
                        writer.close();
                    } catch (IOException ex) {
                        reportException(ex, "FILE", null, false);
                    } catch (Exception ex) {
                        reportException(ex, "INTERNAL",
                                "Unexpected exception while writing " + file, true);
                    } finally {
                        enableMainButtons();
                    }
                }
            }).start();
        }
    }
    
    private void writeBufferedAsCGD(final BufferedWriter writer) {
        for (final Iterator iter = this.bufferedNets.iterator(); iter.hasNext();) {
            final ProcessedNet net = (ProcessedNet) iter.next();
            if (net != null) {
                net.writeEmbedding(new PrintWriter(writer), true, systre.getOutputFullCell());
            }
        }
    }
    
    private void writeBufferedAsPGR(final Writer writer) {
        for (final Iterator iter = this.bufferedNets.iterator(); iter.hasNext();) {
            final ProcessedNet net = (ProcessedNet) iter.next();
            if (net != null) {
                final PeriodicGraph graph = net.getGraph().canonical();
                Output.writePGR(writer, graph, net.getName());
                try {
                    writer.write("\n");
                } catch (final Exception ex) {
                }
            }
        }
    }
    
    private class OptionCheckBox extends BCheckBox {
        public OptionCheckBox(final String label, final Object target, final String option)
                throws Exception {
            
            super(label, false);
            setBackground(null);

            final Class klazz = (target instanceof Class ? (Class) target : target
                    .getClass());
            final String optionCap = Strings.capitalized(option);
            final Method getter = klazz.getMethod("get" + optionCap, null);
            final Method setter = klazz.getMethod("set" + optionCap,
                    new Class[] { boolean.class });
            
            setState(((Boolean) getter.invoke(target, null)).booleanValue());

            addEventLink(ValueChangedEvent.class, new EventProcessor() {
                public void handleEvent(Object event) {
                    try {
                        setter.invoke(target, new Object[] { new Boolean(getState()) });
                    } catch (Exception ex) {
                        reportException(ex, "FATAL", "serious internal problem", true);
                    }
                }
            });
        }
    }
    
    public void doOptions() {
		final BDialog dialog = new BDialog(this, "Systre - Options", true);
		final ColumnContainer column = new ColumnContainer();
		column.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE,
                defaultInsets, null));
        column.setBackground(textColor);
        try {
            column.add(new OptionCheckBox("Use Builtin Archive", this.systre,
                    "useBuiltinArchive"));
            column.add(new OptionCheckBox("Prefer Second Origin On Input",
                    SpaceGroupCatalogue.class, "preferSecondOrigin"));
            column.add(new OptionCheckBox("Prefer Hexagonal Setting On Input",
                    SpaceGroupCatalogue.class, "preferHexagonal"));
            column.add(new OptionCheckBox("Relax Node Positions", this.systre,
                    "relaxPositions"));
            column.add(new OptionCheckBox("Output Full Conventional Cell", this.systre,
                    "outputFullCell"));
        } catch (final Exception ex) {
            reportException(ex, "FATAL", "serious internal problem", true);
            return;
        }
        
		final BButton okButton = makeButton("Ok");
		column.add(okButton, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE,
				defaultInsets, null));

		dialog.setContent(column);

        okButton.addEventLink(CommandEvent.class, dialog, "dispose");
		dialog.addEventLink(WindowClosingEvent.class, dialog, "dispose");

		dialog.pack();
		dialog.setVisible(true);
	}
    
    public void doNext() {
        disableMainButtons();
        new Thread(new Runnable() {
            public void run() {
                nextNet();
                enableMainButtons();
                if (parser == null) {
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            public void run() {
                                nextButton.setEnabled(false);
                            }
                        });
                    } catch (Exception ex) {
                    }
                }
            }
        }).start();
    }
    
    public void nextNet() {
        if (this.parser.atEnd()) {
            finishFile();
        }
        
        final PrintStream out = this.systre.getOutStream();
        PeriodicGraph G = null;
        Exception problem = null;

        final class BailOut extends Throwable {}
        
        try {
            // --- read the next net
            try {
                G = this.parser.parseNet();
            } catch (DataFormatException ex) {
                problem = ex;
            } catch (Exception ex) {
                reportException(ex, "INTERNAL", "Unexpected exception", true);
                throw new BailOut();
            }
            if (G == null) {
                reportException(problem, "INPUT", null, false);
                throw new BailOut();
            }
            ++this.count;
            // --- some blank lines as separators
            out.println();
            if (this.count > 1) {
                out.println();
                out.println();
            }
            String lastGraphName = null;
            try {
                lastGraphName = this.parser.getName();
            } catch (Exception ex) {
                if (problem == null) {
                    problem = ex;
                }
            }
            final String archiveName;
            final String displayName;
            if (lastGraphName == null) {
                archiveName = this.strippedFileName + "-#" + this.count;
                displayName = "";
            } else {
                archiveName = lastGraphName;
                displayName = " - \"" + lastGraphName + "\"";
            }
            out.println("Structure #" + this.count + displayName + ".");
            out.println();
            boolean success = false;
            if (problem != null) {
                reportException(problem, "INPUT", null, false);
            } else {
                try {
                    this.systre.processGraph(G, archiveName, this.parser.getSpaceGroup());
                    success = true;
                } catch (SystreException ex) {
                    reportException(ex, ex.getType().toString(), null, false);
                } catch (Exception ex) {
                    reportException(ex, "INTERNAL", "Unexpected exception", true);
                }
            }
            out.println();
            out.println("Finished structure #" + this.count + displayName + ".");
            if (success) {
                final ProcessedNet net = this.systre.getLastStructure();
                this.bufferedNets.add(net);
            }
        } catch (BailOut ex) {
        }
        if (this.parser.atEnd()) {
            finishFile();
        }
    }
    
    private void openFile(final String filePath) {
        final PrintStream out = this.systre.getOutStream();

        this.parser = null;
        this.count = 0;
        
        try {
            this.parser = new NetParser(new FileReader(filePath));
        } catch (FileNotFoundException ex) {
            reportException(ex, "FILE", null, false);
            return;
        }
        this.fullFileName = filePath;
        this.strippedFileName = new File(filePath).getName().replaceFirst("\\..*$", "");
        out.println("Data file \"" + filePath + "\".");

        this.bufferedNets.clear();
    }

    private void finishFile() {
        final PrintStream out = this.systre.getOutStream();
        
        out.println();
        out.println("Finished data file \"" + this.fullFileName + "\".");
        this.parser = null;
    }

    private void reportException(final Throwable ex, final String type,
			final String msg, final boolean details) {
		final PrintStream out = systre.getOutStream();
		out.println();
		if (details) {
			out.println("==================================================");
		}
		final String text = "ERROR (" + type + ") - " + (msg == null ? "" : msg + ": ");
        out.print("!!! " + text);
        if (details) {
            out.println();
            out.print(Misc.stackTrace(ex));
            out.println("==================================================");
        } else {
            out.println(ex.getMessage() + ".");
        }
        
        invokeAndWait(new Runnable() {
            public void run() {
                final String title = "Systre: " + type + " ERROR";
                final String msg = text + ex.getMessage() + ".";
                final BStandardDialog dialog = new BStandardDialog(title, msg,
                        BStandardDialog.ERROR);
                dialog.showMessageDialog(SystreGUI.this);
            }
        });
	}
    
    private void disableMainButtons() {
        invokeAndWait(new Runnable() {
            public void run() {
                openButton.setEnabled(false);
                nextButton.setEnabled(false);
                saveButton.setEnabled(false);
                optionsButton.setEnabled(false);
            }
        });
    }

    private void enableMainButtons() {
        invokeLater(new Runnable() {
            public void run() {
                openButton.setEnabled(true);
                nextButton.setEnabled(true);
                saveButton.setEnabled(true);
                optionsButton.setEnabled(true);
            }
        });
    }
    
    public void doQuit() {
        System.exit(0);
    }
    
    /**
     * Wrapper for {@link SwingUtilities.invokeAndWait}}. If we're in the event dispatch
     * thread, the argument is just invoked normally.
     * 
     * @param runnable what to invoke.
     */
    private void invokeAndWait(final Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(runnable);
            } catch (Exception ex) {
            }
        }
    }

    /**
     * Wrapper for {@link SwingUtilities.invokeLater}}. If we're in the event dispatch
     * thread, the argument is just invoked normally.
     * 
     * @param runnable what to invoke.
     */
    private void invokeLater(final Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            try {
                SwingUtilities.invokeLater(runnable);
            } catch (Exception ex) {
            }
        }
    }
    
	public static void main(final String args[]) {
        new SystreGUI();
    }
}
