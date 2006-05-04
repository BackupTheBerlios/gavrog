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
import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import org.gavrog.box.collections.FilteredIterator;
import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.collections.Pair;
import org.gavrog.box.simple.DataFormatException;
import org.gavrog.box.simple.Misc;
import org.gavrog.joss.crossover.Skeleton;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.generators.InputIterator;
import org.gavrog.joss.geometry.SpaceGroupCatalogue;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;
import org.gavrog.joss.pgraphs.io.NetParser;
import org.gavrog.joss.pgraphs.io.Output;

import buoy.event.CommandEvent;
import buoy.event.WindowClosingEvent;
import buoy.widget.BButton;
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
 * @version $Id: SystreGUI.java,v 1.41 2006/05/04 01:49:56 odf Exp $
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
    private Iterator netsToProcess = null;
	private String strippedFileName;
    private String fullFileName;
    private StringBuffer currentTranscript = new StringBuffer();
    private String lastFinishedTranscript = null;
    private List bufferedNets = new LinkedList();
    
    private boolean singleWrite = false;
    
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
				+ "Version 1.0 beta 060503<br><br>"
				+ "by Olaf Delgado-Friedrichs 2001-2006</html>");
		top.add(label, BorderContainer.NORTH);
        
        final GridContainer buttonBar = new GridContainer(4, 1);
        buttonBar.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER,
                LayoutInfo.HORIZONTAL, null, null));
        
        buttonBar.add(openButton = makeButton("Open...", this, "doOpen"), 0, 0);
        buttonBar.add(nextButton = makeButton("Next", this, "doNext"), 1, 0);
        buttonBar.add(saveButton = makeButton("Save as...", this, "doSave"), 2, 0);
        buttonBar.add(optionsButton = makeButton("Options...", this, "doOptions"), 3, 0);
        
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
		
		final BButton cancelButton = makeButton("Cancel", this, "doCancel");
		final BButton exitButton = makeButton("Exit", this, "doQuit");
		final BorderContainer bottom = new BorderContainer();
		bottom.setBackground(null);
		bottom.add(cancelButton, BorderContainer.WEST);
		bottom.add(exitButton, BorderContainer.EAST);
        main.add(bottom, BorderContainer.SOUTH, new LayoutInfo(LayoutInfo.CENTER,
				LayoutInfo.NONE, defaultInsets, null));
        
        setContent(main);
        
        captureOutput();
        
        addEventLink(WindowClosingEvent.class, this, "doQuit");
        
        nextButton.setEnabled(false);
        saveButton.setEnabled(false);
        
        final JFileChooser inchsr = (JFileChooser) inFileChooser.getComponent();
        inchsr.addChoosableFileFilter(new ExtensionFilter("arc", "Systre Archives"));
        inchsr.addChoosableFileFilter(new ExtensionFilter(new String[] {"cgd", "pgr" },
        		"Systre Input Files"));
        final JFileChooser outchsr = (JFileChooser) outFileChooser.getComponent();
        outchsr.addChoosableFileFilter(new ExtensionFilter("arc", "Systre Archive Files"));
        outchsr.addChoosableFileFilter(new ExtensionFilter("cgd", "Embedded Nets"));
        outchsr.addChoosableFileFilter(new ExtensionFilter("pgr", "Abstract Topologies"));
        outchsr.addChoosableFileFilter(new ExtensionFilter("out", "Systre Transcripts"));
        
        pack();
        setVisible(true);
    }
    
    private BButton makeButton(final String label, final Object target,
            final String method) {
    	final BButton button = new BButton(label);
    	button.setBackground(buttonColor);
        button.addEventLink(CommandEvent.class, target, method);
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
                currentTranscript.append(buffer);
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
        	this.netsToProcess = null;
            final String filename = this.inFileChooser.getSelectedFile().getName();
            final File dir = this.inFileChooser.getDirectory();
            final String path = new File(dir, filename).getAbsolutePath();
            this.output.setText("");
            disableMainButtons();
            
            if (filename.endsWith(".arc")) {
                systre.processArchive(path);
                enableMainButtons();
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
                        final int n = filename.lastIndexOf('.');
                        final String extension = filename.substring(n+1);
                        if (singleWrite) {
							writeStructure(extension, writer, systre.getLastStructure(),
									lastFinishedTranscript);
						} else {
							for (final Iterator iter = bufferedNets.iterator(); iter
									.hasNext();) {
								final Pair item = (Pair) iter.next();
								final ProcessedNet net = (ProcessedNet) item.getFirst();
								final String transcript = (String) item.getSecond();
								writeStructure(extension, writer, net, transcript);
							}
						}
						writer.flush();
						writer.close();
                    } catch (IOException ex) {
                        reportException(null, "FILE", "I/O error writing to " + file,
                                false);
                    } catch (Exception ex) {
                        reportException(ex, "INTERNAL",
                                "Unexpected exception while writing to " + file, true);
                    } finally {
                        enableMainButtons();
                    }
                }
            }).start();
        }
    }
    
    private void writeStructure(final String extension, final BufferedWriter writer,
			final ProcessedNet net, final String transcript) throws IOException {
        if (net != null) {
            if ("arc".equals(extension)) {
            	// --- write archive entry
				final String txt = new Archive.Entry(net.getGraph(), net.getName())
						.toString();
				writer.write(txt);
				writer.write("\n");
            } else if ("cgd".equals(extension)) {
            	// --- write embedding structure with full symmetry
                net.writeEmbedding(writer, true, systre.getOutputFullCell());
            } else if ("pgr".equals(extension)) {
            	// --- write abstract, unembedded periodic graph
                Output.writePGR(writer, net.getGraph().canonical(), net.getName());
				writer.write("\n");
            } else {
            	final String lineSeparator = System.getProperty("line.separator");
            	// --- write the full transcript
                writer.write(transcript.replaceAll(lineSeparator, "\n"));
                writer.write("\n");
            }
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
			column.add(new OptionCheckBox("Save only last net finished", this,
					"singleWrite"));
		} catch (final Exception ex) {
			reportException(ex, "FATAL", "serious internal problem", true);
			return;
		}
        
		final BButton okButton = makeButton("Ok", dialog, "dispose");
		column.add(okButton, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE,
				defaultInsets, null));

		dialog.setContent(column);

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
            }
        }).start();
    }
    
    public boolean moreNets() {
    	return this.netsToProcess != null && this.netsToProcess.hasNext();
    }
    
    public void nextNet() {
        if (!moreNets()) {
            finishFile();
            return;
        }
        
        final PrintStream out = this.systre.getOutStream();
        InputStructure net = null;
        PeriodicGraph G = null;
        Exception problem = null;
        this.currentTranscript.delete(0, this.currentTranscript.length());

        final class BailOut extends Throwable {}
        
        try {
            // --- read the next net
            try {
            	net = (InputStructure) this.netsToProcess.next();
            	G = net.getGraph();
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
            final String archiveName;
            final String displayName;
            if (net.getName() == null) {
                archiveName = this.strippedFileName + "-#" + this.count;
                displayName = "";
            } else {
                archiveName = net.getName();
                displayName = " - \"" + net.getName() + "\"";
            }
            out.println("Structure #" + this.count + displayName + ".");
            out.println();
            boolean success = false;
            if (problem != null) {
                reportException(problem, "INPUT", null, false);
            } else {
                try {
                    this.systre.processGraph(G, archiveName, net.getGroup());
                    success = true;
                } catch (SystreException ex) {
                    reportException(ex, ex.getType().toString(), null, false);
                } catch (Exception ex) {
                    reportException(ex, "INTERNAL", "Unexpected exception", true);
                }
            }
            out.println();
            out.println("Finished structure #" + this.count + displayName + ".");
            this.lastFinishedTranscript = this.currentTranscript.toString();
            if (success) {
                final ProcessedNet tmp = this.systre.getLastStructure();
                this.bufferedNets.add(new Pair(tmp, this.lastFinishedTranscript));
            }
        } catch (BailOut ex) {
        }
        if (!moreNets()) {
            finishFile();
        }
    }
    
    private boolean openFile(final String filePath) {
        final PrintStream out = this.systre.getOutStream();

        this.netsToProcess = null;
        this.count = 0;
        
        final Reader reader;
        try {
            reader = new FileReader(filePath);
        } catch (FileNotFoundException ex) {
            reportException(ex, "FILE", null, false);
            return false;
        }
        this.fullFileName = filePath;
        this.strippedFileName = new File(filePath).getName().replaceFirst("\\..*$", "");
        final String extension = filePath.substring(filePath.lastIndexOf('.') + 1);
        out.println("Data file \"" + filePath + "\".");
        this.bufferedNets.clear();

        if ("cgd".equals(extension) || "pgr".equals(extension)) {
			final NetParser parser = new NetParser(reader);

			this.netsToProcess = new IteratorAdapter() {
				protected Object findNext() throws NoSuchElementException {
					if (parser.atEnd()) {
						throw new NoSuchElementException("at end");
					} else {
						return new InputStructure(parser.parseNet(), parser.getName(),
								parser.getSpaceGroup());
					}
				}
			};
			return true;
        } else if ("ds".equals(extension)) {
        	this.netsToProcess = new FilteredIterator(new InputIterator(reader)) {
				public Object filter(Object x) {
					final DelaneySymbol ds = (DelaneySymbol) x;
					final PeriodicGraph graph = new Skeleton(ds);
					final String group = (ds.dim() == 3) ? "P1" : "p1";
					return new InputStructure(graph, null, group);
				}
        	};
        	return true;
		} else {
			reportException(null, "FILE", "Unrecognized extension " + extension, false);
		}
		return false;
	}

    private void finishFile() {
        final PrintStream out = this.systre.getOutStream();
        
        out.println();
        out.println("Finished data file \"" + this.fullFileName + "\".");
        this.netsToProcess = null;
    }

    private void reportException(final Throwable ex, final String type,
			final String msg, final boolean details) {
		final PrintStream out = systre.getOutStream();
		out.println();
		if (details) {
			out.println("==================================================");
		}
		final boolean cancelled = ex instanceof SystreException
				&& ((SystreException) ex).getType().equals(SystreException.CANCELLED);
		final String text;
		if (cancelled) {
			text = "CANCELLING";
		} else {
			text = "ERROR (" + type + ") - " + (msg == null ? "" : msg);
		}
        out.print("!!! " + text);
        if (ex != null) {
            if (details) {
                out.println();
                out.print(Misc.stackTrace(ex));
                out.println("==================================================");
            } else {
                out.println(ex.getMessage() + ".");
            }
        }
        
        invokeAndWait(new Runnable() {
            public void run() {
                final String title = "Systre: " + type + " ERROR";
                final String msg = text + (ex != null ? " - " + ex.getMessage() : "")
						+ ".";
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
                if (moreNets()) {
                	nextButton.setEnabled(true);
                }
                saveButton.setEnabled(true);
                optionsButton.setEnabled(true);
            }
        });
    }
    
    public void doCancel() {
        this.systre.cancel();
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
    
	public boolean getSingleWrite() {
		return singleWrite;
	}

	public void setSingleWrite(boolean singleWrite) {
		this.singleWrite = singleWrite;
	}
	
	public static void main(final String args[]) {
        new SystreGUI();
    }
}
