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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.collections.Iterators;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.Real;
import org.gavrog.joss.geometry.CoordinateChange;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.SpaceGroup;
import org.gavrog.joss.geometry.SpaceGroupCatalogue;
import org.gavrog.joss.geometry.SpaceGroupFinder;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.IEdge;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;
import org.gavrog.joss.pgraphs.basic.SpringEmbedder;
import org.gavrog.joss.pgraphs.io.NetParser;

/**
 * First preview of the upcoming Gavrog version of Systre.
 * 
 * @author Olaf Delgado
 * @version $Id: Demo.java,v 1.30 2006/02/14 06:23:21 odf Exp $
 */
public class Demo {
    final static boolean DEBUG = false;
    
    static {
        Locale.setDefault(Locale.US);
    }
    private final static DecimalFormat fmtReal4 = new DecimalFormat("0.0000");
    private final static DecimalFormat fmtReal5 = new DecimalFormat("0.00000");

    // --- the output stream
    private PrintStream out = System.out;
    
    // --- the various archives
    private final Archive mainArchive;
    private final Map name2archive = new HashMap();
    private final Archive internalArchive = new Archive("1.0");
    
    // --- options
    private boolean relax = true;
    
    /**
     * Returns the stack trace of a throwable as a string.
     * 
     * @param throwable the throwable.
     * @return the string representation.
     */
    public static String stackTrace(final Throwable throwable) {
        StringBuffer sb = new StringBuffer();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.close();
        sb.append(sw.toString());
        return sb.toString();
    }
    
    
    public Demo() {
        // --- read the default archive
        final Package pkg = Archive.class.getPackage();
        final String packagePath = pkg.getName().replaceAll("\\.", "/");
        final String archivePath = packagePath + "/rcsr.arc";
        final InputStream inStream = ClassLoader.getSystemResourceAsStream(archivePath);
        mainArchive = new Archive("1.0");
        mainArchive.addAll(new InputStreamReader(inStream));
    }
    
    /**
     * Reads an archive file and stores it internally.
     * 
     * @param filename the name of the archive file.
     */
    public void processArchive(final String filename) {
        final String name = filename;
        if (this.name2archive.containsKey(name)) {
            System.err.println("WARNING: archive \"" + name + "\" was given twice.");
        } else {
            final Archive arc = new Archive("1.0");
            this.name2archive.put(name, arc);
            try {
                arc.addAll(new FileReader(filename));
            } catch (FileNotFoundException ex) {
                System.err.println("Could not find file \"" + filename + "\"");
                return;
            } catch (Exception ex) {
                System.err.println("Problem reading \"" + filename
                                   + "\" - ignoring this archive.");
                this.name2archive.remove(name);
            }
            final int n = arc.size();
            out.println("Read " + n + " entr" + (n == 1 ? "y" : "ies")
                        + " from archive \"" + name + "\"");
            out.println();
        }
    }
    
    /**
     * Analyzes all nets specified in a file and prints the results.
     * 
     * @param filename the name of the input file.
     */
    public void processData(final String filename) {
        // --- set up a parser for reading input from the given file
        NetParser parser = null;
        int count = 0;
        try {
            parser = new NetParser(new FileReader(filename));
        } catch (FileNotFoundException ex) {
            System.err.println("Could not find file \"" + filename + "\"");
            return;
        }
        out.println("Data file \"" + filename + "\".");
        
        // --- loop through the structures specied in the input file
        while (true) {
            // --- read the next net
            PeriodicGraph G = parser.parseNet();
            if (G == null) {
                break;
            }
            ++count;
            
            if (DEBUG) {
                out.println("\t\t@@@ Graph is " + G);
            }
            
            // --- print some information on net as given
            final String name = parser.getName();
            if (name == null) {
                out.println("Nameless structure.");
            } else {
                out.println("Structure \"" + name + "\".");
            }
            out.println();
            out.println("   Given space group is " + parser.getSpaceGroup() + ".");
            out.flush();
            final int n = G.numberOfNodes();
            final int m = G.numberOfEdges();
            out.println("   " + n + " vert" + (n > 1 ? "ices" : "ex") + " and "
                    + m + " edge" + (m > 1 ? "s" : "") + " in primitive cell as given.");
            out.println();
            out.flush();

            // --- test if it is Systre-compatible
            if (!G.isConnected()) {
                out.println("   Sorry, graph is not connected! Giving up.");
                out.println();
                out.flush();
                continue;
            }
            if (!G.isStable()) {
                out.println("   Sorry, graph has collisions! Giving up.");
                out.println();
                out.flush();
                continue;
            }

            // --- determine a minimal repeat unit
            G = G.minimalImage();
            final int r = n / G.numberOfNodes();
            if (r > 1) {
                out.println("   WARNING: ideal repeat unit smaller than given ("
                        + G.numberOfEdges() + " vs " + m + " edges).");
                if (DEBUG) {
                    out.println("\t\t@@@ minimal graph is " + G);
                }
            } else {
                out.println("   Given primitive cell is accurate.");
            }
            if (DEBUG) {
                out.println("\t\t@@@ barycentric placement:");
                final Map pos = G.barycentricPlacement();
                for (final Iterator nodes = G.nodes(); nodes.hasNext();) {
                    final INode v = (INode) nodes.next();
                    out.println("\t\t@@@    " + v.id() + " -> " + pos.get(v));
                }
            }
            out.flush();

            // --- determine the ideal symmetries
            final List ops = G.symmetryOperators();
            if (DEBUG) {
                out.println("\t\t@@@ symmetry operators:");
                for (final Iterator iter = ops.iterator(); iter.hasNext();) {
                    out.println("\t\t@@@    " + iter.next());
                }
            }
            out.println("   point group has " + ops.size() + " elements.");
            out.flush();
            final int k = Iterators.size(G.nodeOrbits());
            out.println("   " + k + " kind" + (k > 1 ? "s" : "") + " of vertex.");
            out.flush();
            
            // --- determine the coordination sequences
            out.println("   Coordination sequences:");
            for (final Iterator orbits = G.nodeOrbits(); orbits.hasNext();) {
                final Set orbit = (Set) orbits.next();
                final INode v = (INode) orbit.iterator().next();
                out.print("      ");
                final Iterator cs = G.coordinationSequence(v);
                cs.next();
                for (int i = 0; i < 10; ++i) {
                    out.print(" " + ((Integer) cs.next()).intValue());
                }
                out.println();
            }
            out.println();
            out.flush();

            // --- find the space group name and conventional settings
            final int d = G.getDimension();
            final SpaceGroup group = new SpaceGroup(d, ops);
            final SpaceGroupFinder finder = new SpaceGroupFinder(group);
            final String groupName = finder.getGroupName();
            final CoordinateChange toStd = finder.getToStd();
            final CoordinateChange fromStd = (CoordinateChange) toStd.inverse();
            out.println("   Ideal space group is " + groupName + ".");
            final String givenName = SpaceGroupCatalogue.normalizedName(parser
                    .getSpaceGroup());
            if (!givenName.equals(groupName)) {
                out.println("   WARNING: Ideal group differs from given (" + groupName
                        + " vs " + givenName + ").");
            }
            out.println();
            out.flush();

            // --- verify the output of the spacegroup finder
            final CoordinateChange trans = SpaceGroupCatalogue.transform(d, groupName);
            if (!trans.isOne()) {
                final String msg = "Internal error: converted to non-conventional setting";
                throw new RuntimeException(msg);
            }
            final Set conventionalOps = new SpaceGroup(d, groupName).primitiveOperators();
            final Set probes = new SpaceGroup(d, toStd.applyTo(ops)).primitiveOperators();
            if (!probes.equals(conventionalOps)) {
                out.println("Problem with space group operators - should be:");
                for (final Iterator iter = conventionalOps.iterator(); iter.hasNext();) {
                    out.println(iter.next());
                }
                out.println("but was:");
                //for (final Iterator iter = probes.iterator(); iter.hasNext();) {
                for (final Iterator iter = toStd.applyTo(ops).iterator(); iter.hasNext();) {
                    out.println(iter.next());
                }
                final String msg = "Internal error: spacegroup finder messed up operators";
                throw new RuntimeException(msg);
            }
            
            // --- determine the Systre key and look it up in the archives
            final String invariant = G.invariant().toString();
            int countMatches = 0;
            Archive.Entry found = mainArchive.getByKey(invariant);
            if (found != null) {
                ++countMatches;
                out.println("   Structure was found in builtin archive.");
                out.println("       Name: " + found.getName());
            }
            for (Iterator iter = this.name2archive.keySet().iterator(); iter.hasNext();) {
                final String arcName = (String) iter.next();
                final Archive arc = (Archive) this.name2archive.get(arcName);
                found = arc.getByKey(invariant);
                if (found != null) {
                    ++countMatches;
                    out.println("   Structure was found in archive \"" + arcName + "\"");
                    out.println("       Name: " + found.getName());
                }
            }
            found = this.internalArchive.getByKey(invariant);
            if (found != null) {
                ++countMatches;
                out.println("   Structure already seen in this run.");
                out.println("       Name: " + found.getName());
            }
            if (countMatches == 0) {
                out.println("   Structure is new.");
                this.internalArchive.add(G, name == null ? "nameless" : name);
            }
            out.println();
            out.flush();

            // --- bail out - for now - if not a 3d structure
            if (d != 3) {
                out.println("Sorry, currently no refined output for dimension != 3.");
                out.println();
                out.flush();
                continue;
            }

            // --- relax the structure from the barycentric embedding (EXPERIMENTAL CODE)
            SpringEmbedder embedder = new SpringEmbedder(G);
            if (this.relax) {
                try {
                    embedder.setOptimizePositions(false);
                    embedder.steps(200);
                    embedder.setOptimizePositions(true);
                    embedder.steps(500);
                } catch (Exception ex) {
                    System.err.println(stackTrace(ex));
                    System.err.println("Internal error while relaxing!");
                    embedder = new SpringEmbedder(G);
                    embedder.setOptimizeCell(false);
                    embedder.steps(200);
                }
            } else {
                try {
                    embedder.setOptimizePositions(false);
                    embedder.steps(200);
                } catch (Exception ex) {
                    System.err.println(stackTrace(ex));
                    System.err.println("Internal error while relaxing!");
                    embedder = new SpringEmbedder(G);
                }
            }
            embedder.normalize();
            

            // --- set up a buffer to write a Systre readable output description to
            final StringWriter cgdStringWriter = new StringWriter();
            final PrintWriter cgd = new PrintWriter(cgdStringWriter);
            cgd.println("CRYSTAL");
            cgd.println("  GROUP " + groupName);
            
            // --- print the results of the relaxation in the conventional setting
            final Matrix gram = embedder.getGramMatrix();
            final Vector x = (Vector) Vector.unit(3, 0).times(fromStd);
            final Vector y = (Vector) Vector.unit(3, 1).times(fromStd);
            final Vector z = (Vector) Vector.unit(3, 2).times(fromStd);

            final Real a = ((Real) Vector.dot(x, x, gram)).sqrt();
            final Real b = ((Real) Vector.dot(y, y, gram)).sqrt();
            final Real c = ((Real) Vector.dot(z, z, gram)).sqrt();
            final Real f = new FloatingPoint(180.0 / Math.PI);
            final Real alpha = (Real) ((Real) Vector.dot(y, z, gram)
                    .dividedBy(b.times(c))).acos().times(f);
            final Real beta = (Real) ((Real) Vector.dot(x, z, gram).dividedBy(a.times(c)))
                    .acos().times(f);
            final Real gamma = (Real) ((Real) Vector.dot(x, y, gram)
                    .dividedBy(a.times(b))).acos().times(f);

            //    ... print the cell parameters
            out.println("   Refined cell parameters:");
            out.println("       a = " + fmtReal5.format(a.doubleValue()) + ", b = "
                        + fmtReal5.format(b.doubleValue()) + ", c = "
                        + fmtReal5.format(c.doubleValue()));
            out.println("       alpha = " + fmtReal4.format(alpha.doubleValue())
                        + ", beta = " + fmtReal4.format(beta.doubleValue())
                        + ", gamma = " + fmtReal4.format(gamma.doubleValue()));
            cgd.println("  CELL " + fmtReal5.format(a.doubleValue()) + " "
                        + fmtReal5.format(b.doubleValue()) + " "
                        + fmtReal5.format(c.doubleValue()) + " "
                        + fmtReal4.format(alpha.doubleValue()) + " "
                        + fmtReal4.format(beta.doubleValue()) + " "
                        + fmtReal4.format(gamma.doubleValue()));
            
            if (DEBUG) {
                for (int i = 0; i < d; ++i) {
                    final Vector v = Vector.unit(d, i);
                    out.println("\t\t@@@ " + v + " -> " + v.times(toStd));
                }
                out.flush();
            }
            
            //    ... print the atom positions
            if (this.relax) {
                out.println("   Refined atom positions:");
            } else {
                out.println("   Barycentric atom positions:");
            }
            final Map pos = embedder.getPositions();
            for (final Iterator orbits = G.nodeOrbits(); orbits.hasNext();) {
                final Set orbit = (Set) orbits.next();
                final INode v = (INode) orbit.iterator().next();
                final Point p = ((Point) ((Point) pos.get(v)).times(toStd)).modZ();
                out.print("     ");
                cgd.print("  NODE " + v.id() + " " + G.new CoverNode(v).degree() + " ");
                for (int i = 0; i < d; ++i) {
                    out.print(" " + fmtReal5.format(((Real) p.get(i)).doubleValue()));
                    cgd.print(" " + fmtReal5.format(((Real) p.get(i)).doubleValue()));
                }
                out.println();
                cgd.println();
            }
            
            if (DEBUG) {
                for (final Iterator nodes = G.nodes(); nodes.hasNext();) {
                    final Point p = (Point) pos.get(nodes.next());
                    out.println("\t\t@@@ " + p + " -> " + p.times(toStd));
                }
                out.flush();
            }
            
            //    ... print the edges
            out.println("   Edges:");
            for (final Iterator orbits = G.edgeOrbits(); orbits.hasNext();) {
                final Set orbit = (Set) orbits.next();
                final IEdge e = (IEdge) orbit.iterator().next();
                final INode v = e.source();
                final INode w = e.target();
                if (DEBUG) {
                    final Point p1 = (Point) pos.get(v);
                    final Point q1 = (Point) ((Point) pos.get(w)).plus(G.getShift(e));
                    out.println("\t\t@@@ " + p1 + ", " + q1 + "  -> " + p1.times(toStd) + ", " + q1.times(toStd));
                    out.flush();
                }
                final Point p = ((Point) ((Point) pos.get(v)).times(toStd));
                final Point q = (Point) ((Point) pos.get(w)).plus(G.getShift(e)).times(
                        toStd);
                final Point p0 = p.modZ();
                final Point q0 = (Point) q.minus(p.minus(p0));
                out.print("     ");
                cgd.print("  EDGE ");
                for (int i = 0; i < d; ++i) {
                    out.print(" " + fmtReal5.format(((Real) p0.get(i)).doubleValue()));
                    cgd.print(" " + fmtReal5.format(((Real) p0.get(i)).doubleValue()));
                }
                out.print("  <-> ");
                cgd.print("  ");
                for (int i = 0; i < d; ++i) {
                    out.print(" " + fmtReal5.format(((Real) q0.get(i)).doubleValue()));
                    cgd.print(" " + fmtReal5.format(((Real) q0.get(i)).doubleValue()));
                }
                out.println();
                cgd.println();
            }
            out.println();
            cgd.println("END");
            cgd.println();
            cgd.flush();
            final String cgdString = cgdStringWriter.toString();
            final PeriodicGraph test = NetParser.stringToNet(cgdString);
            if (!test.equals(G)) {
                final String msg = "Internal error: output does not match original graph";
                throw new RuntimeException(msg);
            }

            // --- some blank lines as separators
            out.println();
            out.println();
            out.flush();
        }
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
     * The main method takes command line arguments one by one and passes them to
     * {@link #processData} or {@link #processArchive}.
     * 
     * @param args the command line arguments.
     */
    public static void main(final String args[]) {
        final Demo demo = new Demo();
        final List files = new LinkedList();
        String outputArchiveFileName = null;
        
        for (int i = 0; i < args.length; ++i) {
            final String s = args[i];
            if (s.equals("-b")) {
                demo.relax = false;
            } else if (s.equals("-a")) {
                if (i == args.length - 1) {
                    System.err.println("WARNING: -a option without an argument.");
                } else {
                    outputArchiveFileName = args[++i];
                }
            } else {
                files.add(args[i]);
            }
        }
        
        if (files.size() == 0) {
            System.err.println("WARNING: no filenames given.");
        }
        
        for (final Iterator iter = files.iterator(); iter.hasNext();) {
            final String filename = (String) iter.next();
            if (filename.endsWith(".arc")) {
                demo.processArchive(filename);
            } else {
                demo.processData(filename);
            }
        }
        
        if (outputArchiveFileName != null) {
            try {
                final BufferedWriter writer = new BufferedWriter(new FileWriter(outputArchiveFileName));
                demo.writeInternalArchive(writer);
                writer.flush();
                writer.close();
            } catch (IOException ex) {
                System.err.println("Could not write output archive:");
                System.err.println(ex);
            }
        }
    }
}
