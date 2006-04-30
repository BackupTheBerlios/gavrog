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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.collections.Iterators;
import org.gavrog.box.simple.DataFormatException;
import org.gavrog.box.simple.Misc;
import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Real;
import org.gavrog.joss.geometry.CoordinateChange;
import org.gavrog.joss.geometry.Operator;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.SpaceGroup;
import org.gavrog.joss.geometry.SpaceGroupCatalogue;
import org.gavrog.joss.geometry.SpaceGroupFinder;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;
import org.gavrog.joss.pgraphs.embed.AmoebaEmbedder;
import org.gavrog.joss.pgraphs.embed.IEmbedder;
import org.gavrog.joss.pgraphs.io.NetParser;

/**
 * The basic commandlne version of Gavrog Systre.
 * 
 * @author Olaf Delgado
 * @version $Id: SystreCmdline.java,v 1.37 2006/04/30 03:40:10 odf Exp $
 */
public class SystreCmdline {
    final static boolean DEBUG = false;
    
    static {
        Locale.setDefault(Locale.US);
    }
    private final static DecimalFormat fmtReal4 = new DecimalFormat("0.0000");

    // --- the last structure processed
    ProcessedNet lastStructure = null;
    
    // --- the output stream
    private PrintStream out = System.out;
    
    // --- the various archives
    private final Archive mainArchive;
    private final Map name2archive = new HashMap();
    private final Archive internalArchive = new Archive("1.0");
    
    // --- options
    private boolean relaxPositions = true;
    private boolean useBuiltinArchive = true;
    private boolean outputFullCell = false;
    private BufferedWriter outputArchive = null;
    
    // --- the last file that was opened for processing
    private String lastFileNameWithoutExtension;
    
    // --- signals a cancel request from outside
    private boolean cancelled = false;
    
    /**
     * Constructs an instance.
     */
    public SystreCmdline() {
        // --- read the default archive
        final Package pkg = Archive.class.getPackage();
        final String packagePath = pkg.getName().replaceAll("\\.", "/");
        final String archivePath = packagePath + "/rcsr.arc";
        final InputStream inStream = ClassLoader.getSystemResourceAsStream(archivePath);
        mainArchive = new Archive("1.0");
        mainArchive.addAll(new InputStreamReader(inStream));
    }
    
    /**
    /**
     * Reads an archive file and stores it internally.
     * 
     * @param filename the name of the archive file.
     */
    public void processArchive(final String filename) {
        final String name = filename;
        if (this.name2archive.containsKey(name)) {
            out.println("!!! WARNING (USAGE) - Archive \"" + name + "\" was given twice.");
        } else {
            final Archive arc = new Archive("1.0");
            try {
                arc.addAll(new FileReader(filename));
            } catch (FileNotFoundException ex) {
				out.println("!!! ERROR (FILE) - Could not find file \"" + filename
						+ "\".");
				return;
			} catch (Exception ex) {
				out.println("!!! ERROR (FILE) - " + ex.getMessage()
						+ " - ignoring archive \"" + filename + "\".");
				return;
			}
            this.name2archive.put(name, arc);
            final int n = arc.size();
            out.println("Read " + n + " entr" + (n == 1 ? "y" : "ies")
                        + " from archive \"" + name + "\"");
            out.println();
        }
    }
    
    public void processGraph(final PeriodicGraph graph, final String name,
            final String givenGroup) {
        setLastStructure(null);
        
        PeriodicGraph G = graph;
        final int d = G.getDimension();
        
        if (DEBUG) {
            out.println("\t\t@@@ Graph is " + G);
        }
        
        // --- print some information on net as given
        out.println("   Given space group is " + givenGroup + ".");
        out.flush();
        final int n = G.numberOfNodes();
        final int m = G.numberOfEdges();
        out.println("   " + n + " vert" + (n > 1 ? "ices" : "ex") + " and "
                + m + " edge" + (m > 1 ? "s" : "") + " in repeat unit as given.");
        out.flush();

        // --- test if it is Systre-compatible
        if (!G.isConnected()) {
            final String msg = "Structure is not connected";
            throw new SystreException(SystreException.STRUCTURE, msg);
        }
        if (!G.isStable()) {
            final String msg = "Structure has collisions";
            throw new SystreException(SystreException.STRUCTURE, msg);
        }
        
        quitIfCancelled();

        // --- determine a minimal repeat unit
        G = G.minimalImage();
        final int r = n / G.numberOfNodes();
        if (r > 1) {
            out.println("   Ideal repeat unit smaller than given ("
                    + G.numberOfEdges() + " vs " + m + " edges).");
            if (DEBUG) {
                out.println("\t\t@@@ minimal graph is " + G);
            }
        } else {
            out.println("   Given repeat unit is accurate.");
        }
        
        quitIfCancelled();

        // --- get and check the barycentric placement
        
        final Map barycentric = G.barycentricPlacement();
        if (!G.isBarycentric(barycentric)) {
            final String msg = "Incorrect barycentric placement.";
            throw new RuntimeException(msg);
        }
        if (DEBUG) {
            out.println("\t\t@@@ barycentric placement:");
            for (final Iterator nodes = G.nodes(); nodes.hasNext();) {
                final INode v = (INode) nodes.next();
                out.println("\t\t@@@    " + v.id() + " -> " + barycentric.get(v));
            }
        }
        out.println();
        out.flush();
        
        quitIfCancelled();
        
        // --- determine the ideal symmetries
        final List ops = G.symmetryOperators();
        if (DEBUG) {
            out.println("\t\t@@@ symmetry operators:");
            for (final Iterator iter = ops.iterator(); iter.hasNext();) {
                out.println("\t\t@@@    " + iter.next());
            }
        }
        out.println("   Point group has " + ops.size() + " elements.");
        out.flush();
        final int k = Iterators.size(G.nodeOrbits());
        out.println("   " + k + " kind" + (k > 1 ? "s" : "") + " of vertex.");
        out.println();
        out.flush();
        
        quitIfCancelled();
        
        // --- determine the coordination sequences
        out.println("   Coordination sequences:");
        int cum = 0;
        for (final Iterator orbits = G.nodeOrbits(); orbits.hasNext();) {
            final Set orbit = (Set) orbits.next();
            final INode v = (INode) orbit.iterator().next();
            out.print("      ");
            final Iterator cs = G.coordinationSequence(v);
            cs.next();
            int sum = 1;
            for (int i = 0; i < 10; ++i) {
            	final int x = ((Integer) cs.next()).intValue();
                out.print(" " + x);
                sum += x;
            }
            out.println();
            cum += orbit.size() * sum;
        }
        out.println();
        out.println("   TD10 = " + fmtReal4.format(((double) cum) / G.numberOfNodes()));
        out.println();
        out.flush();
        
        quitIfCancelled();
        
        // --- bail out - for now - if not a 3d structure
        if (d != 3) {
            final String msg = "No further support yet for dimension " + d;
            throw new SystreException(SystreException.STRUCTURE, msg);
        }

        // --- find the space group name and conventional settings
        final SpaceGroup group = new SpaceGroup(d, ops);
        final SpaceGroupFinder finder = new SpaceGroupFinder(group);
        final String groupName = finder.getGroupName();
        final String extendedGroupName = finder.getExtendedGroupName();
        final CoordinateChange toStd = finder.getToStd();
        out.println("   Ideal space group is " + groupName + ".");
        final String givenName = SpaceGroupCatalogue.normalizedName(givenGroup);
        if (!givenName.equals(groupName)) {
            out.println("   Ideal group differs from given (" + groupName
                    + " vs " + givenName + ").");
        }
        final String ext = finder.getExtension();
        if ("1".equals(ext)) {
        	out.println("     (using first origin choice)");
        } else if ("2".equals(ext)) {
        	out.println("     (using second origin choice)");
        } else if ("H".equals(ext)) {
        	out.println("     (using hexagonal setting)");
        } else if ("R".equals(ext)) {
        	out.println("     (using rhombohedral setting)");
        }
        if (DEBUG) {
            out.println("\t\t@@@ transformed operators:");
            for (final Iterator iter = toStd.applyTo(ops).iterator(); iter.hasNext();) {
                out.println("\t\t@@@    " + iter.next());
            }
        }
        out.println();
        out.flush();
        
        quitIfCancelled();
        
        // --- verify the output of the spacegroup finder
        final CoordinateChange trans = SpaceGroupCatalogue
				.transform(d, extendedGroupName);
        if (!trans.isOne()) {
            final String msg = "Produced non-conventional space group setting.";
            throw new RuntimeException(msg);
        }
        final Set conventionalOps = new SpaceGroup(d, extendedGroupName)
				.primitiveOperators();
        final Set opsFound = new HashSet();
        opsFound.addAll(ops);
        for (int i = 0; i < d; ++i) {
            opsFound.add(new Operator(Vector.unit(d, i)));
        }
        final Set probes = new SpaceGroup(d, toStd.applyTo(opsFound)).primitiveOperators();
        if (!probes.equals(conventionalOps)) {
            out.println("Problem with space group operators - should be:");
            for (final Iterator iter = conventionalOps.iterator(); iter.hasNext();) {
                out.println(iter.next());
            }
            out.println("but was:");
            for (final Iterator iter = toStd.applyTo(opsFound).iterator(); iter.hasNext();) {
                out.println(iter.next());
            }
            final String msg = "Spacegroup finder messed up operators.";
            throw new RuntimeException(msg);
        }
        
        
        quitIfCancelled();
        
        // --- determine the Systre key and look it up in the archives
        final String invariant = G.getSystreKey();
        int countMatches = 0;
        Archive.Entry found = null;
        if (this.useBuiltinArchive) {
            found = mainArchive.getByKey(invariant);
        }
        if (found != null) {
            ++countMatches;
            out.println("   Structure was found in builtin archive:");
            writeEntry(out, found);
            out.println();
        }
        for (Iterator iter = this.name2archive.keySet().iterator(); iter.hasNext();) {
            final String arcName = (String) iter.next();
            final Archive arc = (Archive) this.name2archive.get(arcName);
            found = arc.getByKey(invariant);
            if (found != null) {
                ++countMatches;
                out.println("   Structure was found in archive \"" + arcName + "\":");
                writeEntry(out, found);
                out.println();
            }
        }
        found = this.internalArchive.getByKey(invariant);
        if (found != null) {
            ++countMatches;
            out.println("   Structure already seen in this run.");
            writeEntry(out, found);
            out.println();
        }
        if (countMatches == 0) {
            out.println("   Structure is new for this run.");
            out.println();
            final Archive.Entry entry = this.internalArchive.add(G,
                    name == null ? "nameless" : name);
            if (this.outputArchive != null) {
                try {
                    this.outputArchive.write(entry.toString());
                    this.outputArchive.write("\n");
                    this.outputArchive.flush();
                } catch (IOException ex) {
                    final String msg = "Could not write to archive";
                    throw new SystreException(SystreException.FILE, msg);
                }
            }
        }
        out.flush();
        
        quitIfCancelled();
        
        // --- compute an embedding
        for (int pass = 0; pass <= 1; ++pass) {
            // --- relax the structure from the barycentric embedding
            IEmbedder embedder = new AmoebaEmbedder(G);
            try {
                embedder.setRelaxPositions(false);
                embedder.go(500);
                embedder.setRelaxPositions(relaxPositions && pass == 0);
                embedder.go(1000);
            } catch (Exception ex) {
                out.println("==================================================");
                final String msg = "!!! WARNING (INTERNAL) - Could not relax: " + ex;
                out.println(msg);
                out.println(Misc.stackTrace(ex));
                out.println("==================================================");
                embedder.reset();
            }
            embedder.normalize();
            
            quitIfCancelled();
            
            // --- do some checking
            final IArithmetic det = embedder.getGramMatrix().determinant();
            if (det.abs().isLessThan(new FloatingPoint(0.001))) {
                out.println("==================================================");
                final String msg = "!!! WARNING (INTERNAL) - Unit cell degenerated in relaxation.";
                out.println(msg);
                out.println("==================================================");
                embedder.reset();
                embedder.normalize();
            }
            if (!embedder.positionsRelaxed()) {
                final Map pos = embedder.getPositions();
                final Map bari = G.barycentricPlacement();
                int problems = 0;
                for (final Iterator nodes = G.nodes(); nodes.hasNext();) {
                    final INode v = (INode) nodes.next();
                    final Point p = (Point) pos.get(v);
                    final Point q = (Point) bari.get(v);
                    final Vector diff = (Vector) p.minus(q);
                    final double err = ((Real) Vector.dot(diff, diff)).sqrt()
                            .doubleValue();
                    if (err > 1e-12) {
                        out.println("\t\t@@@ " + v + " is at " + p + ", but should be "
                                    + q);
                        ++problems;
                    }
                }
                if (problems > 0) {
                    final String msg = "Embedder misplaced " + problems + " points";
                    throw new SystreException(SystreException.INTERNAL, msg);
                }
            }
            
            quitIfCancelled();
            
            // --- write a Systre readable net description to a string buffer
            final StringWriter cgdStringWriter = new StringWriter();
            final PrintWriter cgd = new PrintWriter(cgdStringWriter);
            final ProcessedNet net = new ProcessedNet(G, name, finder, embedder);
            setLastStructure(net);
            net.writeEmbedding(cgd, true, getOutputFullCell());

            final String cgdString = cgdStringWriter.toString();
			boolean success = false;
            try {
                out.println("   Consistency test:");
                out.print("       reading...");
                out.flush();
                final PeriodicGraph test = NetParser.stringToNet(cgdString);
                
                quitIfCancelled();
                
                out.println(" OK!");
                out.print("       comparing...");
                out.flush();
                if (!test.minimalImage().equals(G)) {
                    final String msg = "Output does not match original graph.";
                    throw new RuntimeException(msg);
                }
                out.println(" OK!");
                out.println();
                success = true;
                
                quitIfCancelled();
                
            } catch (Exception ex) {
                out.println(" Failed!");
                if (DEBUG) {
                    out.println("\t\t@@@ Failing output:");
                    out.println(cgdString);
                }
                if (pass == 0) {
                    if (relaxPositions) {
                        out.println("   Falling back to barycentric positions.");
                    }
                } else {
                    final String msg = "Could not verify output data";
                    throw new SystreException(SystreException.INTERNAL, msg, ex);
                }
            }
            
            quitIfCancelled();
            
            // --- now write the actual output
            if (success) {
                net.writeEmbedding(new PrintWriter(out), false, getOutputFullCell());
                net.setVerified(true);
                break;
            }
        }
    }

    private void writeEntry(final PrintStream out, final Archive.Entry entry) {
        out.println("       Name:\t\t" + entry.getName());
        if (entry.getDescription() != null) {
            out.println("       Description:\t" + entry.getDescription());
        }
        if (entry.getReference() != null) {
            out.println("       Reference:\t" + entry.getReference());
        }
        if (entry.getURL() != null) {
            out.println("       URL:\t\t" + entry.getURL());
        }
    }
    
    /**
	 * Analyzes all nets specified in a file and prints the results.
	 * 
	 * @param filePath
	 *            the name of the input file.
	 */
    public void processDataFile(final String filePath) {
        // --- set up a parser for reading input from the given file
        NetParser parser = null;
        int count = 0;
        try {
            parser = new NetParser(new FileReader(filePath));
        } catch (FileNotFoundException ex) {
            out.println("!!! ERROR (FILE) - Could not find file \"" + filePath + "\".");
            return;
        }
        this.lastFileNameWithoutExtension = new File(filePath).getName().replaceFirst(
                "\\..*$", "");
        out.println("Data file \"" + filePath + "\".");
        
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
                out.println("==================================================");
                out.println("!!! ERROR (INTERNAL) - Unexpected exception:");
                out.println(Misc.stackTrace(ex));
                out.println("==================================================");
                continue;
            }
            if (G == null) {
            	if (problem == null) {
            		break;
            	} else {
                    out.println("!!! ERROR (INPUT) - " + problem.getMessage());
            		continue;
            	}
            }
            ++count;
            
            // --- some blank lines as separators
            out.println();
            if (count > 1) {
                out.println();
                out.println();
            }
            
            // --- process the graph
            String name = null;
            try {
                name = parser.getName();
            } catch (Exception ex) {
                if (problem == null) {
                    problem = ex;
                }
            }
            final String archiveName;
            final String displayName;
            if (name == null) {
                archiveName = lastFileNameWithoutExtension + "-#" + count;
                displayName = "";
            } else {
                archiveName = name;
                displayName = " - \"" + name + "\"";
            }
            
            out.println("Structure #" + count + displayName + ".");
            out.println();
            if (problem != null) {
                out.println("!!! ERROR (INPUT) - " + problem.getMessage());
            } else {
                try {
                    processGraph(G, archiveName, parser.getSpaceGroup());
                } catch (SystreException ex) {
                    out.println("!!! ERROR (" + ex.getType() + ") - " + ex.getMessage() + ".");
                } catch (Exception ex) {
                    out.println("==================================================");
                    out.println("!!! ERROR (INTERNAL) - Unexpected exception:");
                    out.println(Misc.stackTrace(ex));
                    out.println("==================================================");
                }
            }
            out.println();
			out.println("Finished structure #" + count + displayName + ".");
        }

        out.println();
        out.println("Finished data file \"" + filePath + "\".");
    }
    
    /**
     * Writes all the entries read from data files onto a stream.
     * 
     * @param writer represents the output stream.
     * @throws IOException if writing to the stream did not work.
     */
    public void writeInternalArchive(final BufferedWriter writer) throws IOException {
        for (Iterator iter = this.internalArchive.keySet().iterator(); iter.hasNext();) {
            final String key = (String) iter.next();
            final Archive.Entry entry = this.internalArchive.getByKey(key);
            writer.write(entry.toString());
        }
        final int n = this.internalArchive.size();
        out.println("Wrote " + n + " entr" + (n == 1 ? "y" : "ies")
                    + " to output archive.");
    }
    
    /**
     * This method takes command line arguments one by one and passes them to
     * {@link #processDataFile} or {@link #processArchive}.
     * 
     * @param args the command line arguments.
     */
    public void run(final String args[]) {
        final List files = new LinkedList();
        String outputArchiveFileName = null;
        
        for (int i = 0; i < args.length; ++i) {
            final String s = args[i];
            if (s.equals("-b")) {
                setRelaxPositions(false);
            } else if (s.equals("-a")) {
                if (i == args.length - 1) {
                    out.println("!!! WARNING (USAGE) - Argument missing for \"-a\".");
                } else {
                    outputArchiveFileName = args[++i];
                }
            } else if (s.equalsIgnoreCase("--nobuiltin")
                    || s.equalsIgnoreCase("-nobuiltin")) {
                setUseBuiltinArchive(false);
            } else {
                files.add(args[i]);
            }
        }
        
        if (files.size() == 0) {
            out.println("!!! WARNING (USAGE) - No file names given.");
        }
        
        int count = 0;
        
        
        if (outputArchiveFileName != null) {
            try {
                this.outputArchive = new BufferedWriter(new FileWriter(outputArchiveFileName));
            } catch (IOException ex) {
                out.println("!!! ERROR (FILE) - Could not open output archive:" + ex);
            }
        }
        
        for (final Iterator iter = files.iterator(); iter.hasNext();) {
            final String filename = (String) iter.next();
            if (filename.endsWith(".arc")) {
                this.processArchive(filename);
            } else {
                ++count;
                if (count > 1) {
                    out.println();
                    out.println();
                    out.println();
                }
               this.processDataFile(filename);
            }
        }
        
        if (this.outputArchive != null) {
            try {
                this.outputArchive.flush();
                this.outputArchive.close();
            } catch (IOException ex) {
                out.println("!!! ERROR (FILE) - Output archive not completely written.");
            }
        }
    }
    
    public ProcessedNet getLastStructure() {
        return this.lastStructure;
    }

    protected void setLastStructure(ProcessedNet lastStructure) {
        this.lastStructure = lastStructure;
    }

    public boolean getUseBuiltinArchive() {
		return useBuiltinArchive;
	}

	public void setUseBuiltinArchive(boolean useBuiltinArchive) {
		this.useBuiltinArchive = useBuiltinArchive;
	}

	public boolean getRelaxPositions() {
		return relaxPositions;
	}

	public void setRelaxPositions(boolean relax) {
		this.relaxPositions = relax;
	}

    protected PrintStream getOutStream() {
        return this.out;
    }
    
    protected void setOutStream(final PrintStream out) {
        this.out = out;
    }
    
    public boolean getOutputFullCell() {
        return this.outputFullCell;
    }
    
    public void setOutputFullCell(boolean fullCellOutput) {
        this.outputFullCell = fullCellOutput;
    }
    
	public synchronized void cancel() {
		this.cancelled = true;
	}
	
	private void quitIfCancelled() {
		if (this.cancelled) {
			this.cancelled = false;
			throw new SystreException(SystreException.CANCELLED,
						"Execution stopped for this structure");
		}
	}
	
    public static void main(final String args[]) {
        new SystreCmdline().run(args);
    }
}
