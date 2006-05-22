package org.gavrog.systre;

import java.io.PrintWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.collections.Pair;
import org.gavrog.box.simple.Strings;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Real;
import org.gavrog.joss.geometry.CoordinateChange;
import org.gavrog.joss.geometry.CrystalSystem;
import org.gavrog.joss.geometry.Lattices;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.SpaceGroupFinder;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.Cover;
import org.gavrog.joss.pgraphs.basic.IEdge;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;
import org.gavrog.joss.pgraphs.embed.AmoebaEmbedder;
import org.gavrog.joss.pgraphs.embed.IEmbedder;

/**
 * Stores a graph with its name, embedding and space group symmetry.
 * 
 * @author Olaf Delgado
 * @version $Id: ProcessedNet.java,v 1.8 2006/05/22 23:02:13 odf Exp $
 */
class ProcessedNet {
    private final static DecimalFormat fmtReal4 = new DecimalFormat("0.0000");
    private final static DecimalFormat fmtReal5 = new DecimalFormat("0.00000");
    
    /*
     * Auxiliary type.
     */
    private class PlacedNode implements Comparable {
        final public INode v;
        final public Point p;
        
        public PlacedNode(final INode v, final Point p) {
            this.v = v;
            this.p = p;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(final Object other) {
            final Point p = this.p;
            final Point q = ((PlacedNode) other).p;
            final int dim = p.getDimension();
            final Point o = Point.origin(dim);
            final Vector s = (Vector) p.minus(o);
            final Vector t = (Vector) q.minus(o);
            if (s.isNegative()) {
                if (t.isNonNegative()) {
                    return 1;
                }
            } else if (t.isNegative()) {
                return -1;
            }
            int diff = cmpCoords(Vector.dot(s, s), Vector.dot(t, t));
            if (diff != 0) {
                return diff;
            }
            for (int i = 0; i < dim; ++i) {
                diff = cmpCoords(p.get(i), q.get(i));
                if (diff != 0) {
                    return diff;
                }
            }
            return 0;
        }
        
        private int cmpCoords(final IArithmetic a, final IArithmetic b) {
            final double x = ((Real) a).doubleValue();
            final double y = ((Real) b).doubleValue();
            if (x < 0) {
                if (y > 0) {
                    return 1;
                }
            } else if (y < 0) {
                return -1;
            }
            final double d = Math.abs(x) - Math.abs(y);
            if (Math.abs(d) < 1e-6) {
                return 0;
            } else {
                return Double.compare(Math.abs(x), Math.abs(y));
            }
        }
    };

    private boolean verified = false;
    private final PeriodicGraph graph;
    private final String name;
    private final Map node2name;
    private final SpaceGroupFinder finder;
    private final IEmbedder embedder;

    public ProcessedNet(
			final PeriodicGraph G, final String name, final Map node2name,
			final SpaceGroupFinder finder, final IEmbedder embedder) {
        this.graph = G;
        this.name = name;
        this.node2name = node2name;
        this.finder = finder;
        this.embedder = embedder;
    }

    public void writeEmbedding(final Writer stream, final boolean cgdFormat, boolean fullCell) {
        final PrintWriter out = new PrintWriter(stream);
        
        // --- extract some data from the arguments
        final int d = graph.getDimension();
        final String extendedGroupName = finder.getExtendedGroupName();
        final CoordinateChange toStd = finder.getToStd();
        final CoordinateChange fromStd = (CoordinateChange) toStd.inverse();
        final boolean cellRelaxed = embedder.cellRelaxed();
        final boolean posRelaxed = embedder.positionsRelaxed();
        
        // --- print a header if necessary
        if (cgdFormat) {
            out.println("CRYSTAL");
            out.println("  NAME " + Strings.parsable(name, false));
            if (fullCell) {
                out.println("  GROUP P1");
            } else {
                out.println("  GROUP " + extendedGroupName);
            }
        }
        
        // --- get the relaxed Gram matrix
        final Matrix gram = embedder.getGramMatrix();
        
        // --- the cell vectors in the coordinate system used be the embedder
        Vector x = (Vector) Vector.unit(3, 0).times(fromStd);
        Vector y = (Vector) Vector.unit(3, 1).times(fromStd);
        Vector z = (Vector) Vector.unit(3, 2).times(fromStd);
    
        // --- correct to a reduced cell for monoclinic and triclinic groups
        final CoordinateChange correction = cell_correction(finder, gram, x, y, z);
        final CoordinateChange ctmp = (CoordinateChange) correction.inverse().times(
                fromStd);
        x = (Vector) Vector.unit(3, 0).times(ctmp);
        y = (Vector) Vector.unit(3, 1).times(ctmp);
        z = (Vector) Vector.unit(3, 2).times(ctmp);
        
        // --- compute the cell parameters
        final double a = Math.sqrt(((Real) Vector.dot(x, x, gram)).doubleValue());
        final double b = Math.sqrt(((Real) Vector.dot(y, y, gram)).doubleValue());
        final double c = Math.sqrt(((Real) Vector.dot(z, z, gram)).doubleValue());
        final double f = 180.0 / Math.PI;
        final double alpha = Math.acos(((Real) Vector.dot(y, z, gram)).doubleValue()
                / (b * c)) * f;
        final double beta = Math.acos(((Real) Vector.dot(x, z, gram)).doubleValue()
                / (a * c)) * f;
        final double gamma = Math.acos(((Real) Vector.dot(x, y, gram)).doubleValue()
                / (a * b)) * f;

        // --- print the cell parameters
        if (cgdFormat) {
            out.println("  CELL " + fmtReal5.format(a) + " " + fmtReal5.format(b) + " "
                    + fmtReal5.format(c) + " " + fmtReal4.format(alpha) + " "
                    + fmtReal4.format(beta) + " " + fmtReal4.format(gamma));
        } else {
            if (fullCell) {
                out.println("   Coordinates below are given for a full conventional cell.");
            }
            out.println("   " + (cellRelaxed ? "R" : "Unr") + "elaxed cell parameters:");
            out.println("       a = " + fmtReal5.format(a) + ", b = "
                    + fmtReal5.format(b) + ", c = " + fmtReal5.format(c));
            out.println("       alpha = " + fmtReal4.format(alpha) + ", beta = "
                    + fmtReal4.format(beta) + ", gamma = " + fmtReal4.format(gamma));
        }
        
        // --- compute graph representation with respect to a conventional unit cell
        final Cover cov = graph.conventionalCellCover();

        // --- compute the relaxed node positions in to the conventional unit cell
        final Map pos = embedder.getPositions();
        final INode v0 = (INode) cov.nodes().next();
        final Vector shift = (Vector) ((Point) pos.get(cov.image(v0))).times(toStd)
                .minus(cov.liftedPosition(v0, pos));
        final Map lifted = new HashMap();
        for (final Iterator nodes = cov.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            lifted.put(v, cov.liftedPosition(v, pos).plus(shift).times(correction));
        }
        
        // --- print the atom positions
        if (!cgdFormat) {
            out.println("   " + (posRelaxed ? "Relaxed" : "Barycentric") + " positions:");
        }
        final boolean allNodes = fullCell;
        final Map reps = nodeReps(cov, lifted, allNodes);
        for (final Iterator iter = reps.keySet().iterator(); iter.hasNext();) {
            // --- extract the next node and its position
            final INode v = (INode) iter.next();
            final Point p = (Point) reps.get(v);
            final String name = Strings.parsable((String) this.node2name
					.get(cov.image(v)), false);
            
            // --- print them
            if (cgdFormat) {
                out.print("  NODE " + name + " " + cov.new CoverNode(v).degree() + " ");
            } else {
                out.print("      Node " + name + ":   ");
            }
            for (int i = 0; i < d; ++i) {
                out.print(" " + fmtReal5.format(((Real) p.get(i)).doubleValue()));
            }
            out.println();
        }
        
        // --- print the edges
        if (!cgdFormat) {
            out.println("   Edges:");
        }
        final List ereps = edgeReps(cov, reps, lifted, correction, fullCell);
        for (final Iterator iter = ereps.iterator(); iter.hasNext();) {
            final Pair pair = (Pair) iter.next();
            final Point p = ((PlacedNode) pair.getFirst()).p;
            final Point q = ((PlacedNode) pair.getSecond()).p;

            // --- print its start and end positions
            if (cgdFormat) {
                out.print("  EDGE ");
            } else {
                out.print("     ");
            }
            for (int i = 0; i < d; ++i) {
                out.print(" " + fmtReal5.format(((Real) p.get(i)).doubleValue()));
            }
            if (cgdFormat) {
                out.print("  ");
            } else {
                out.print("  <-> ");
            }
            for (int i = 0; i < d; ++i) {
                out.print(" " + fmtReal5.format(((Real) q.get(i)).doubleValue()));
            }
            out.println();
        }
        
        // --- print the edges
        if (!cgdFormat) {
            out.println("   Edge centers:");
        }
        for (final Iterator iter = ereps.iterator(); iter.hasNext();) {
            final Pair pair = (Pair) iter.next();
            final Point p = ((PlacedNode) pair.getFirst()).p;
            final Point q = ((PlacedNode) pair.getSecond()).p;

            // --- print its start and end positions
            if (cgdFormat) {
                out.print("# EDGE_CENTER ");
            } else {
                out.print("     ");
            }
            for (int i = 0; i < d; ++i) {
            	final double s = ((Real) p.get(i)).doubleValue();
            	final double t = ((Real) q.get(i)).doubleValue();
                out.print(" " + fmtReal5.format((s + t) / 2));
            }
            out.println();
        }
        
        // --- finish up
        if (cgdFormat) {
            out.println("END");
            out.println();
        } else {
            // --- write edge statistics
            final String min = fmtReal5.format(embedder.minimalEdgeLength());
            final String max = fmtReal5.format(embedder.maximalEdgeLength());
            final String avg = fmtReal5.format(embedder.averageEdgeLength());
            out.println();
            out.println("   Edge statistics: minimum = " + min + ", maximum = " + max
                    + ", average = " + avg);
            
            // --- compute and write angle statistics
            double minAngle = Double.MAX_VALUE;
            double maxAngle = 0.0;
            double sumAngle = 0.0;
            int count = 0;
            
            for (final Iterator nodes = graph.nodes(); nodes.hasNext();) {
                final INode v = (INode) nodes.next();
                final Point p = (Point) pos.get(v);
                final List incidences = graph.allIncidences(v);
                final List vectors = new ArrayList();
                for (final Iterator iter = incidences.iterator(); iter.hasNext();) {
                    final IEdge e = (IEdge) iter.next();
                    final INode w = e.target();
                    final Point q = (Point) pos.get(w);
                    vectors.add(q.plus(graph.getShift(e)).minus(p));
                }
                final int m = vectors.size();
                for (int i = 0; i < m; ++i) {
                    final Vector s = (Vector) vectors.get(i);
                    final double ls = ((Real) Vector.dot(s, s, gram)).sqrt()
                            .doubleValue();
                    for (int j = i + 1; j < m; ++j) {
                        final Vector t = (Vector) vectors.get(j);
                        final double lt = ((Real) Vector.dot(t, t, gram)).sqrt()
                                .doubleValue();
                        final double dot = ((Real) Vector.dot(s, t, gram)).doubleValue();
                        final double arg = Math.max(-1, Math.min(1, dot / (ls * lt)));
                        final double angle = Math.acos(arg) / Math.PI * 180;
                        minAngle = Math.min(minAngle, angle);
                        maxAngle = Math.max(maxAngle, angle);
                        sumAngle += angle;
                        ++count;
                    }
                }
            }
            out.println("   Angle statistics: minimum = " + fmtReal5.format(minAngle)
                    + ", maximum = " + fmtReal5.format(maxAngle) + ", average = "
                    + fmtReal5.format(sumAngle / count));
            
            // --- write the shortest non-bonded distance
            out.println("   Shortest non-bonded distance = "
                    + fmtReal5.format(smallestNonBondedDistance(graph, embedder)));
            
            // --- write the degrees of freedom as found by the embedder
            if (embedder instanceof AmoebaEmbedder) {
                out.println();
                out.println("   Degrees of freedom: "
                        + ((AmoebaEmbedder) embedder).degreesOfFreedom());
            }
        }
        out.flush();
    }

    private Map nodeReps(final PeriodicGraph cov, final Map lifted, boolean allNodes) {
        final Map reps = new LinkedHashMap();
        for (final Iterator orbits = cov.nodeOrbits(); orbits.hasNext();) {
            // --- grab the next node orbit
            final Set orbit = (Set) orbits.next();
            
            // --- find positions for all nodes
            final List tmp = new ArrayList();
            for (final Iterator iter = orbit.iterator(); iter.hasNext();) {
                final INode v = (INode) iter.next();
                final Point p = ((Point) lifted.get(v)).modZ();
                tmp.add(new PlacedNode(v, p));
            }
            
            // --- sort by position
            Collections.sort(tmp);
            
            // --- extract the node (or nodes) to use
            if (allNodes) {
                for (int i = 0; i < tmp.size(); ++i) {
                    final PlacedNode pn = (PlacedNode) tmp.get(i);
                    reps.put(pn.v, pn.p);
                }
            } else {
                final PlacedNode pn = (PlacedNode) tmp.get(0);
                reps.put(pn.v, pn.p);
            }
        }
        
        return reps;
    }

    private List edgeReps(final PeriodicGraph cov, final Map reps, final Map lifted,
            final CoordinateChange correction, boolean allEdges) {
        final List result = new LinkedList();
        
        for (final Iterator orbits = cov.edgeOrbits(); orbits.hasNext();) {
            // --- grab the next edge orbit
            final Set orbit = (Set) orbits.next();
            
            // --- extract those edges starting or ending in a node that has been printed
            final List candidates = new ArrayList();
            for (final Iterator iter = orbit.iterator(); iter.hasNext();) {
                final IEdge e = (IEdge) iter.next();
                if (reps.containsKey(e.source())) {
                    candidates.add(e);
                }
                if (reps.containsKey(e.target())) {
                    candidates.add(e.reverse());
                }
            }
            
            // --- find positions for all the end points
            for (int i = 0; i < candidates.size(); ++i) {
                final IEdge e = (IEdge) candidates.get(i);
                final INode v = e.source();
                final INode w = e.target();
                final Point p = (Point) lifted.get(v);
                final Point q = (Point) ((Point) lifted.get(w)).plus(cov.getShift(e)
                        .times(correction));
                final Point p0 = p.modZ();
                final Point q0 = (Point) q.minus(p.minus(p0));
                candidates.set(i, new Pair(new PlacedNode(v, p0), new PlacedNode(w, q0)));
            }
            
            // --- sort edges by end point positions
            Collections.sort(candidates);
            
            // --- extract the edge (or edges) to use
            if (allEdges) {
                result.addAll(candidates);
            } else {
                result.add(candidates.get(0));
            }
        }
        return result;
    }
    
    private CoordinateChange cell_correction(final SpaceGroupFinder finder,
            final Matrix gram, final Vector a, Vector b, final Vector c) {
        
        // --- get and check the dimension
        final int dim = a.getDimension();
        if (dim != 3) {
            final String msg = "Method called with incorrect dimension";
            throw new SystreException(SystreException.INTERNAL, msg);
        }
        
        // --- little helper class
        final class NameSet extends HashSet {
            public NameSet(final String names[]) {
                super();
                for (int i = 0; i < names.length; ++i) {
                    this.add(names[i]);
                }
            }
        }
        
        // --- no centering, no glide, both a and c are free
        final Set type1 = new NameSet(new String[] { "P121", "P1211", "P1m1", "P12/m1",
                "P121/m1" });
        // --- no centering, a is free
        final Set type2 = new NameSet(new String[] { "P1c1", "P12/c1", "P121/c1" });
        // --- no glide, c is free
        final Set type3 = new NameSet(new String[] { "C121", "C1m1", "C12/m1" });
        // --- both glide and centering, only signs are free
        final Set type4 = new NameSet(new String[] { "C1c1", "C12/c1" });
        
        // --- extract some basic info
        final String name = finder.getGroupName();
        final CrystalSystem system = finder.getCrystalSystem();
        
        // --- old and new basis
        final Vector from[] = new Vector[] { a, b, c };
        final Vector to[];
        
        if (system == CrystalSystem.MONOCLINIC) {
            // --- find a pair of shortest vectors that span the same basis as x and z
            final Vector old[] = new Vector[] { a, c };
            final Vector nu[] = Lattices.reducedLatticeBasis(old, gram);
                        
            if (type1.contains(name)) {
                // --- use the new vectors
                to = new Vector[] { nu[0], b, nu[1] };
            } else if (type2.contains(name)) {
                // --- keep c and use the shortest independent new vector as a
                final Vector new_a;
                if (c.isCollinearTo(nu[0])) {
                    new_a = nu[1];
                } else if (c.isCollinearTo(nu[1])) {
                    new_a = nu[0];
                } else if (Vector.dot(nu[1], nu[1], gram).isLessThan(Vector.dot(nu[0], nu[0], gram))) {
                    new_a = nu[1];
                } else {
                    new_a = nu[0];
                }
                to = new Vector[] { new_a, b, c };
            } else if (type3.contains(name)) {
                // --- keep a and use the shortest independent new vector as c
                final Vector new_c;
                if (a.isCollinearTo(nu[0])) {
                    new_c = nu[1];
                } else if (a.isCollinearTo(nu[1])) {
                    new_c = nu[0];
                } else if (Vector.dot(nu[1], nu[1], gram).isLessThan(Vector.dot(nu[0], nu[0], gram))) {
                    new_c = nu[1];
                } else {
                    new_c = nu[0];
                }
                to = new Vector[] { a, b, new_c };
            } else if (type4.contains(name)) {
                // --- must keep all old vectors
                to = new Vector[] { a, b, c };
            } else {
                final String msg = "Cannot handle monoclinic space group " + name + ".";
                throw new SystreException(SystreException.INTERNAL, msg);
            }
            
            if (Vector.dot(to[0], to[2], gram).isPositive()) {
                to[2] = (Vector) to[2].negative();
            }
        } else if (system == CrystalSystem.TRICLINIC) {
            to = Lattices.reducedLatticeBasis(from, gram);
        } else {
            to = new Vector[] { a, b, c };
        }
        
        final CoordinateChange F = new CoordinateChange(Vector.toMatrix(from));
        final CoordinateChange T = new CoordinateChange(Vector.toMatrix(to));
        final CoordinateChange result = (CoordinateChange) F.inverse().times(T);
        return result;
    }

    /**
     * Does what it says.
     * 
     * @param G
     *            a periodic graph.
     * @param embedder
     *            an embedding for G.
     * @return the smallest distance between nodes that are not connected.
     */
    private double smallestNonBondedDistance(final PeriodicGraph G,
            final IEmbedder embedder) {
        // --- get some data about the embedding
        final Matrix gram = embedder.getGramMatrix();
        final Map pos = embedder.getPositions();
        
        // --- compute a Dirichlet domain for the translation lattice
        final Vector basis[] = Vector.rowVectors(Matrix.one(G.getDimension()));
        final Vector dirichletVectors[] = Lattices.dirichletVectors(basis, gram);
        
        // --- determine how to shift each node into the Dirichlet domain
        final Map shift = new HashMap();
        for (final Iterator nodes = G.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            final Point p = (Point) pos.get(v);
            shift.put(v, Lattices.dirichletShifts(p, dirichletVectors, gram, 1)[0]);
        }
        
        // --- list all points in two times extended Dirichlet domain
        final Set moreNodes = new HashSet();
        for (final Iterator iter = G.nodes(); iter.hasNext();) {
            final INode v = (INode) iter.next();
            final Vector s = (Vector) shift.get(v);
            final Point p = (Point) pos.get(v);
            moreNodes.add(new Pair(v, s));
            for (int i = 0; i < dirichletVectors.length; ++i) {
                final Vector vec = (Vector) s.plus(dirichletVectors[i]);
                final Vector shifts[] = Lattices.dirichletShifts((Point) p.plus(vec),
                        dirichletVectors, gram, 2);
                for (int k = 0; k < shifts.length; ++k) {
                    moreNodes.add(new Pair(v, vec.plus(shifts[k])));
                }
            }
        }
        
        // --- determine all distances from orbit representatives
        double minDist = Double.MAX_VALUE;
        for (final Iterator orbits = G.nodeOrbits(); orbits.hasNext();) {
            // --- get shift and position for next orbit representative
            final Set orbit = (Set) orbits.next();
            final INode v = (INode) orbit.iterator().next();
            final Vector s = (Vector) shift.get(v);
            final Point p = (Point) ((Point) pos.get(v)).plus(s);
            
            // --- ignore node itself and its neighbors
            final Set ignore = new HashSet();
            ignore.add(new Pair(v, s));
            for (final Iterator inc = G.allIncidences(v).iterator(); inc.hasNext();) {
                final IEdge e = (IEdge) inc.next();
                final INode w = e.target();
                final Vector t = (Vector) G.getShift(e).plus(s);
                ignore.add(new Pair(w, t));
            }
            
            // --- now looks for closest other point in extended Dirichlet domain
            for (final Iterator others = moreNodes.iterator(); others.hasNext();) {
                final Pair item = (Pair) others.next();
                if (ignore.contains(item)) {
                    continue;
                }
                final INode w = (INode) item.getFirst();
                final Vector t = (Vector) item.getSecond();
                final Point q = (Point) ((Point) pos.get(w)).plus(t);
                final Vector d = (Vector) q.minus(p);
                final double dist = ((Real) Vector.dot(d, d, gram)).sqrt().doubleValue();
                minDist = Math.min(minDist, dist);
            }
        }
        
        // --- return the result
        return minDist;
    }
    
    /**
     * @return the graph.
     */
    public PeriodicGraph getGraph() {
        return this.graph;
    }
    
    /**
     * @return the name.
     */
    public String getName() {
        return this.name;
    }
    
    /**
     * @return the current value of verified.
     */
    public boolean getVerified() {
        return this.verified;
    }
    
    /**
     * @param verified The new value for verified.
     */
    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}
