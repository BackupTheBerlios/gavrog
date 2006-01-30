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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
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
 * @version $Id: Demo.java,v 1.21 2006/01/30 23:35:41 odf Exp $
 */
public class Demo {
    private final static DecimalFormat fmtReal4 = new DecimalFormat("0.0000");
    private final static DecimalFormat fmtReal5 = new DecimalFormat("0.00000");

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
    
    
    /**
     * Analyzes all nets specified in a file and prints the results.
     * 
     * @param filename the name of the input file.
     */
    public static void run(final String filename) {
        // --- set the output stream
        final PrintStream out = System.out;
        
        // --- read the default archive
        final Package pkg = Archive.class.getPackage();
        final String packagePath = pkg.getName().replaceAll("\\.", "/");
        final String archivePath = packagePath + "/rcsr.arc";
        final Archive rcsr = new Archive("1.0");
        final InputStream inStream = ClassLoader.getSystemResourceAsStream(archivePath);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
        rcsr.addAll(reader);
        
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
            } else {
                out.println("   Given primitive cell is accurate.");
            }                
            out.flush();

            // --- determine the ideal symmetries
            final List ops = G.symmetryOperators();
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
                final String msg = "Internal error: spacegroup finder messed up operators";
                throw new RuntimeException(msg);
            }
            
            // --- determine the Systre key and look it up in the archive
            final String invariant = G.invariant().toString();
            final Archive.Entry found = rcsr.getByKey(invariant);
            if (found == null) {
                out.println("   Structure not in archive.");
            } else {
                out.println("   Structure was found in archive.");
                out.println("   Name: " + found.getName());
            }
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
            try {
                embedder.steps(200);
            } catch (Exception ex) {
                System.err.println(stackTrace(ex));
                System.err
                        .println("Could not refine cell parameters due to internal error!");
                embedder = new SpringEmbedder(G);
                embedder.setOptimizeCell(false);
                embedder.steps(200);
            }
            embedder.normalize();

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
            final Map pos = embedder.getPositions();
            
            //    ... print the atom positions
            out.println("   Refined atom positions:");
            for (final Iterator orbits = G.nodeOrbits(); orbits.hasNext();) {
                final Set orbit = (Set) orbits.next();
                final INode v = (INode) orbit.iterator().next();
                final Point p = ((Point) ((Point) pos.get(v)).times(toStd)).modZ();
                out.print("     ");
                for (int i = 0; i < d; ++i) {
                    out.print(" " + fmtReal5.format(((Real) p.get(i)).doubleValue()));
                }
                out.println();
            }
            
            //    ... print the edges
            out.println("   Edges:");
            for (final Iterator orbits = G.edgeOrbits(); orbits.hasNext();) {
                final Set orbit = (Set) orbits.next();
                final IEdge e = (IEdge) orbit.iterator().next();
                final INode v = e.source();
                final INode w = e.target();
                final Point p = ((Point) ((Point) pos.get(v)).times(toStd));
                final Point q = (Point) ((Point) pos.get(w)).plus(G.getShift(e)).times(
                        toStd);
                final Point p0 = p.modZ();
                final Point q0 = (Point) q.minus(p.minus(p0));
                out.print("     ");
                for (int i = 0; i < d; ++i) {
                    out.print(" " + fmtReal5.format(((Real) p0.get(i)).doubleValue()));
                }
                out.print("  <-> ");
                for (int i = 0; i < d; ++i) {
                    out.print(" " + fmtReal5.format(((Real) q0.get(i)).doubleValue()));
                }
                out.println();
            }
            out.println();

            // --- some blank lines as separators
            out.println();
            out.println();
            out.flush();
        }
    }
    
    /**
     * The main method takes the first command line argument and passes it to {@link #run}.
     * 
     * @param args the command line arguments.
     */
    public static void main(final String args[]) {
        run(args[0]);
    }
}
