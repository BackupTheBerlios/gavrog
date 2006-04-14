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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.gavrog.box.simple.DataFormatException;
import org.gavrog.box.simple.Misc;
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
 * @version $Id: SystreGUI.java,v 1.16 2006/04/14 22:11:53 odf Exp $
 */
public class SystreGUI extends BFrame {
    final private static Color textColor = new Color(255, 250, 240);
	final private static Color buttonColor = new Color(224, 224, 240);
	final private static Insets defaultInsets = new Insets(5, 5, 5, 5);

    private final BFileChooser inFileChooser = new BFileChooser(BFileChooser.OPEN_FILE,
            "Open data file");
    private final BFileChooser outFileChooser = new BFileChooser(BFileChooser.SAVE_FILE,
            "Save output");
    
    private final SystreCmdline systre = new SystreCmdline();
	private String strippedFileName;

    private BTextArea output;
	private BScrollBar vscroll;
    private BButton openButton;
    private BButton saveButton;
    private BButton optionsButton;
    private String lastGraphName;
    
    private List bufferedNets = new LinkedList();

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
				+ "Version 1.0 beta 1 (2006-04-11)<br><br>"
				+ "by Olaf Delgado-Friedrichs 2001-2006</html>");
		top.add(label, BorderContainer.NORTH);
        
        final GridContainer buttonBar = new GridContainer(3, 1);
        buttonBar.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER,
                LayoutInfo.HORIZONTAL, null, null));
        
        buttonBar.add(openButton = makeButton("Open..."), 0, 0);
        buttonBar.add(saveButton = makeButton("Save as..."), 1, 0);
        buttonBar.add(optionsButton = makeButton("Options..."), 2, 0);
        
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
        saveButton.addEventLink(CommandEvent.class, this, "doSave");
        optionsButton.addEventLink(CommandEvent.class, this, "doOptions");
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
    
    public void doOpen() {
        final boolean success = this.inFileChooser.showDialog(this);
        if (success) {
            final String filename = this.inFileChooser.getSelectedFile().getName();
            final File dir = this.inFileChooser.getDirectory();
            final String path = new File(dir, filename).getAbsolutePath();
            this.output.setText("");
            disableButtons();
            
            new Thread(new Runnable() {
                public void run() {
                    try {
                        if (filename.endsWith(".arc")) {
                            systre.processArchive(path);
                        } else {
                            processDataFile(path);
                        }
                    } catch (Exception ex) {
                        reportException(ex, "INTERNAL", "Unexpected exception", true);
                    } finally {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                enableButtons();
                            }
                        });
                    }
                }
            }).start();
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
            disableButtons();
            
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
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                enableButtons();
                            }
                        });
                    }
                }
            }).start();
        }
    }
    
    private void writeBufferedAsCGD(final BufferedWriter writer) {
        for (final Iterator iter = this.bufferedNets.iterator(); iter.hasNext();) {
            final ProcessedNet net = (ProcessedNet) iter.next();
            if (net != null) {
                net.writeEmbedding(new PrintWriter(writer), true);
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
    
    public void doOptions() {
		final BDialog dialog = new BDialog(this, "Systre - Options", true);
		final ColumnContainer column = new ColumnContainer();
		column.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE,
				defaultInsets, null));
		column.setBackground(textColor);
		final BCheckBox baryBox = new BCheckBox("Relax Node Positions", this.systre
				.getRelaxPositions());
		baryBox.setBackground(null);
		column.add(baryBox);
		final BCheckBox builtinBox = new BCheckBox("Use Builtin Archive", this.systre
				.getUseBuiltinArchive());
		builtinBox.setBackground(null);
		column.add(builtinBox);
		final BCheckBox secondOriginBox = new BCheckBox("Prefer Second Origin Choice",
				SpaceGroupCatalogue.getPreferSecondOrigin());
		secondOriginBox.setBackground(null);
		column.add(secondOriginBox);
		final BCheckBox hexagonalBox = new BCheckBox("Prefer Hexagonal Group Setting",
				SpaceGroupCatalogue.getPreferHexagonal());
		hexagonalBox.setBackground(null);
		column.add(hexagonalBox);
		final BButton okButton = makeButton("Ok");
		column.add(okButton, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE,
				defaultInsets, null));

		dialog.setContent(column);

		baryBox.addEventLink(ValueChangedEvent.class, new EventProcessor() {
			public void handleEvent(final Object event) {
				systre.setRelaxPositions(baryBox.getState());
			}
		});
		builtinBox.addEventLink(ValueChangedEvent.class, new EventProcessor() {
			public void handleEvent(final Object event) {
				systre.setUseBuiltinArchive(builtinBox.getState());
			}
		});
		secondOriginBox.addEventLink(ValueChangedEvent.class, new EventProcessor() {
			public void handleEvent(final Object event) {
				SpaceGroupCatalogue.setPreferSecondOrigin(secondOriginBox.getState());
			}
		});
		hexagonalBox.addEventLink(ValueChangedEvent.class, new EventProcessor() {
			public void handleEvent(final Object event) {
				SpaceGroupCatalogue.setPreferHexagonal(hexagonalBox.getState());
			}
		});
		okButton.addEventLink(CommandEvent.class, dialog, "dispose");
		dialog.addEventLink(WindowClosingEvent.class, dialog, "dispose");

		dialog.pack();
		dialog.setVisible(true);
	}
    
    /**
	 * Analyzes all nets specified in a file and prints the results.
	 * 
	 * @param filePath the name of the input file.
	 */
	public void processDataFile(final String filePath) {
        final String skipping = "\n!!! SKIPPING REST OF FILE AS REQUESTED.";
		final PrintStream out = this.systre.getOutStream();
		
	    // --- set up a parser for reading input from the given file
	    NetParser parser = null;
	    int count = 0;
	    try {
	        parser = new NetParser(new FileReader(filePath));
	    } catch (FileNotFoundException ex) {
	    	reportException(ex, "FILE", null, false);
	        return;
	    }
	    strippedFileName = new File(filePath).getName().replaceFirst("\\..*$", "");
		out.println("Data file \"" + filePath + "\".");
	    
        this.bufferedNets.clear();
        
	    // --- loop through the structures specied in the input file
	    while (true) {
	        PeriodicGraph G = null;
	        Exception problem = null;
	        
	        // --- read the next net
	        try {
	            G = parser.parseNet();
	        } catch (DataFormatException ex) {
	            problem = ex;
	        } catch (Exception ex) {
	        	final boolean cancel = reportException(ex, "INTERNAL",
						"Unexpected exception", true);
	        	if (cancel) {
	        		out.println(skipping);
	        		break;
	        	} else {
	        		continue;
	        	}
	        }
	        if (G == null) {
	        	if (problem == null) {
	        		break;
	        	} else {
	        		final boolean cancel = reportException(problem, "INPUT", null, false);
		        	if (cancel) {
		        		out.println(skipping);
		        		break;
		        	} else {
		        		continue;
		        	}
	        	}
	        }
	        ++count;
	        
	        // --- some blank lines as separators
	        out.println();
	        if (count > 1) {
	            out.println();
	            out.println();
	        }
	        
            lastGraphName = null;
            try {
                lastGraphName = parser.getName();
            } catch (Exception ex) {
                if (problem == null) {
                    problem = ex;
                }
            }
	        final String archiveName;
	        final String displayName;
	        if (lastGraphName == null) {
	            archiveName = strippedFileName + "-#" + count;
	            displayName = "";
	        } else {
	            archiveName = lastGraphName;
	            displayName = " - \"" + lastGraphName + "\"";
	        }
	        
	        out.println("Structure #" + count + displayName + ".");
			out.println();
			boolean cancel = false;
            boolean success = false;
			if (problem != null) {
				cancel = reportException(problem, "INPUT", null, false);
			} else {
				try {
					systre.processGraph(G, archiveName, parser.getSpaceGroup());
                    success = true;
                } catch (SystreException ex) {
                    cancel = reportException(ex, ex.getType().toString(), null, false);
				} catch (Exception ex) {
					cancel = reportException(ex, "INTERNAL", "Unexpected exception", true);
				}
			}
	        out.println();
			out.println("Finished structure #" + count + displayName + ".");
            if (success) {
                final ProcessedNet net = systre.getLastStructure();
                this.bufferedNets.add(net);
            }
			if (cancel) {
        		out.println(skipping);
				break;
			}
	    }
	
	    out.println();
	    out.println("Finished data file \"" + filePath + "\".");
	}

	private boolean cancel;
	
    private boolean reportException(final Throwable ex, final String type,
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
        cancel = false;
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    final String title = "Systre: " + type + " ERROR";
                    final String msg = text + ex.getMessage() + ".";
                    final BStandardDialog dialog = new BStandardDialog(title, msg,
                            BStandardDialog.ERROR);
                    final String ok = "Continue with next structure";
                    final String skip = "Skip rest of file";
                    final String choices[] = new String[] { ok, skip };
                    final int val = dialog.showOptionDialog(SystreGUI.this, choices, ok);
                    cancel = val > 0;
                }
            });
		} catch (final Exception ex2) {
		}
		return cancel;
	}
    
    private void disableButtons() {
        openButton.setEnabled(false);
        saveButton.setEnabled(false);
        optionsButton.setEnabled(false);
    }

    private void enableButtons() {
        openButton.setEnabled(true);
        saveButton.setEnabled(true);
        optionsButton.setEnabled(true);
    }
    
    public void doQuit() {
        System.exit(0);
    }
    
	public static void main(final String args[]) {
        new SystreGUI();
    }
}
