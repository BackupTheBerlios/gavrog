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

package org.gavrog.joss.pgraphs.basic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.WeakHashMap;

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.collections.Iterators;
import org.gavrog.box.collections.NiceIntList;
import org.gavrog.box.collections.Pair;
import org.gavrog.box.collections.Partition;
import org.gavrog.jane.compounds.LinearAlgebra;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.Fraction;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Rational;
import org.gavrog.jane.numbers.Whole;


/**
 * Implements a representation of a periodic graph.
 * 
 * @author Olaf Delgado
 * @version $Id: PeriodicGraph.java,v 1.7 2005/10/13 05:23:53 odf Exp $
 */
public class PeriodicGraph extends UndirectedGraph {
    private static final String IS_CONNECTED = "isConnected";
    private static final String BARYCENTRIC_PLACEMENT = "barycentricPlacement";
    private static final String IS_LOCALLY_STABLE = "isLocallyStable";
    private static final String CHARACTERISTIC_BASES = "characteristicBases";
    private static final String SYMMETRIES = "symmetries";
    private static final String INVARIANT = "invariant";
    private static final String CANONICAL = "canonical";

    private static final boolean DEBUG = false;
    
    private final int dimension;
    private final Map edgeIdToShift = new HashMap();
    // === IMPORTANT: always check for a non-null return value of a cache.get() ===
    private Map cache = new WeakHashMap();

    /**
     * Constructs an instance.
     * 
     * @param dimension the dimension of periodicity of this graph.
     */
    public PeriodicGraph(final int dimension) {
        super();
        this.dimension = dimension;
    }

    /**
     * @return Returns the dimension.
     */
    public int getDimension() {
        return dimension;
    }
    
    /**
     * Retrieves the shift vector associated to an edge.
     * @param e an edge of the graph.
     * @return the shift vector for that edge.
     */
    public Matrix getShift(final IEdge e) {
        if (hasElement(e)) {
            final Matrix A = (Matrix) edgeIdToShift.get(e.id());
            if (((Edge) e).isReverse) {
                return (Matrix) A.negative();
            } else {
                return A;
            }
        } else {
            throw new IllegalArgumentException("no such edge");
        }
    }

    /**
     * Retrieve an edge with a given source, target and shift.
     * @param source the source node.
     * @param target the target node.
     * @param shift the shift vector.
     * @return the unique edge with this data, or null, if none exists.
     */
    public IEdge getEdge(final INode source, final INode target, final Matrix shift) {
        for (Iterator edges = directedEdges(source, target); edges.hasNext();) {
            final IEdge e = (IEdge) edges.next();
            if (getShift(e).equals(shift)) {
                return e;
            } else if (source.equals(target) && getShift(e).equals(shift.negative())) {
                return e.reverse();
            }
        }
        return null;
    }
    
    /**
     * Creates a new edge with a zero shift vector.
     * @param source the source node.
     * @param target the target node.
     * @return the newly created edge.
     */
    public IEdge newEdge(final INode source, final INode target) {
        return newEdge(source, target, new int[this.dimension]);
    }

    /**
     * Creates a new edge.
     * @param source the source node.
     * @param target the target node.
     * @param shift the shift vector associated to the new edge.
     * @return the newly created edge.
     */
    public IEdge newEdge(final INode source, final INode target, final int[] shift) {
        if (shift == null || shift.length != this.dimension) {
            throw new IllegalArgumentException("bad shift array");
        }
        final Matrix A = new Matrix(1, this.dimension);
        for (int i = 0; i < this.dimension; ++i) {
            A.set(0, i, new Whole(shift[i]));
        }
        return newEdge(source, target, A);
    }

    /**
     * Creates a new edge.
     * @param source the source node.
     * @param target the target node.
     * @param shift the shift vector associated to the new edge.
     * @return the newly created edge.
     */
    public IEdge newEdge(final INode source, final INode target, final Matrix shift) {
        final int[] shape = shift.getShape();
        if (shape[0] != 1 || shape[1] != this.dimension) {
            throw new IllegalArgumentException("bad shape for shift");
        } else if (getEdge(source, target, shift) != null) {
            throw new IllegalArgumentException("duplicate edge");
        } else if (source.equals(target) && shift.equals(shift.zero())) {
            throw new IllegalArgumentException("trivial loop");
        }
        cache.clear();
        final IEdge e = super.newEdge(source, target);
        edgeIdToShift.put(e.id(), shift.clone());
        return e;
    }

    /* (non-Javadoc)
     * @see javaPGraphs.IGraph#newNode()
     */
    public INode newNode() {
        cache.clear();
        return super.newNode();
    }
    
    /* (non-Javadoc)
     * @see javaPGraphs.IGraph#delete(javaPGraphs.IGraphElement)
     */
    public void delete(final IGraphElement element) {
        cache.clear();
        if (element instanceof IEdge) {
            edgeIdToShift.remove(element.id());
        }
        super.delete(element);
    }

    /**
     * Returns the sign of a shift vector.
     * 
     * @param A the matrix containing the shift vector.
     * @return the sign of the first non-zero entry, or zero, if none.
     */
    private int signOfShift(final Matrix A) {
        for (int i = 0; i < this.dimension; ++i) {
            final IArithmetic x = A.get(0, i);
            if (x.isNegative()) {
                return -1;
            } else if (x.isPositive()) {
                return 1;
            }
        }
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javaPGraphs.UndirectedGraph#compareEdges(javaPGraphs.IEdge,
     *      javaPGraphs.IEdge)
     */
    protected int compareEdges(IEdge e1, IEdge e2) {
        final int d = super.compareEdges(e1, e2);
        if (d != 0) {
            return d;
        } else {
            return signOfShift((Matrix) (getShift(e1)).minus(getShift(e2)));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javaPGraphs.UndirectedGraph#normalizedEdge(javaPGraphs.IEdge)
     */
    protected IEdge normalizedEdge(final IEdge e) {
        int d = compareIds(e.source(), e.target());
        if (d == 0) {
            d = signOfShift(getShift(e));
        }
        if (d > 0) {
            return e.reverse();
        } else {
            return e;
        }
    }

    /* (non-Javadoc)
     * @see javaPGraphs.UndirectedGraph#formatEdgeInfo(javaPGraphs.IEdge)
     */
    protected String formatEdgeInfo(final IEdge e) {
        final StringBuffer buf = new StringBuffer(100);
        buf.append("[");
        final Matrix A = getShift(e);
        for (int i = 0; i < this.dimension; ++i) {
            if (i > 0) {
                buf.append(",");
            }
            buf.append(A.get(0, i));
        }
        buf.append("]");
        return buf.toString();
    }
    
    /**
     * Constructs an iterator that generates the numbers of nodes in consecutive
     * shells around a start vertex. This count is for the actual (infinite)
     * periodic graph, not the representing finite multigraph.
     * 
     * @param start the start vertex.
     * @return an iterator for the coordination sequence.
     */
    public Iterator coordinationSequence(final INode start) {
        final Set previousShell = new HashSet();
        final Set currentShell = new HashSet();
        
        return new IteratorAdapter() {
            protected Object findNext() throws NoSuchElementException {
                if (currentShell.size() == 0) {
                    final Matrix zero = new Matrix(new int[1][dimension]);
                    currentShell.add(new Pair(start, zero));
                } else {
                    final Set nextShell = new HashSet();
                    for (Iterator iter = currentShell.iterator(); iter.hasNext();) {
                        final Pair point = (Pair) iter.next();
                        final INode v = (INode) point.getFirst();
                        final Matrix s = (Matrix) point.getSecond();
                        for (Iterator edges = v.incidences(); edges.hasNext();) {
                            final IEdge e = (IEdge) edges.next();
                            final Matrix t = (Matrix) s.plus(getShift(e));
                            final Pair newPoint = new Pair(e.target(), t);
                            if (!previousShell.contains(newPoint)
                                    && !currentShell.contains(newPoint)
                                    && !nextShell.contains(newPoint)) {
                                nextShell.add(newPoint);
                            }
                        }
                    }
                    previousShell.clear();
                    previousShell.addAll(currentShell);
                    currentShell.clear();
                    currentShell.addAll(nextShell);
                }
                return new Integer(currentShell.size());
            }
        };
    }
    
    /**
     * Tests if the periodic graph (not just the representing multigraph) is connected.
     * @return true if the periodic graph is connected.
     */
    public boolean isConnected() {
        final Boolean cached = (Boolean) cache.get(IS_CONNECTED);
        if (cached != null) {
            return cached.booleanValue();
        }
        
        // --- an empty graph is considered connected
        if (!nodes().hasNext()) {
            cache.put(IS_CONNECTED, new Boolean(true));
            return true;
        }
        
        // --- little shortcut
        final Matrix zero = new Matrix(new int[1][dimension]);
        
        // --- start node for traversing the representation graph
        final INode start = (INode) nodes().next();
        
        // --- make a breadth first traversal on the representation graph,
        //     determining reachable vertex classes and reachable translations
        final Map nodeClassToSeenShift = new HashMap();
        final LinkedList queue = new LinkedList();
        final List shifts = new ArrayList();
        
        nodeClassToSeenShift.put(start, zero);
        queue.addLast(start);
        
        while (queue.size() > 0) {
            final INode v = (INode) queue.removeFirst();
            final Matrix s = (Matrix) nodeClassToSeenShift.get(v);
            for (Iterator edges = v.incidences(); edges.hasNext();) {
                final IEdge e = (IEdge) edges.next();
                final INode w = e.target();
                final Matrix t = (Matrix) s.plus(getShift(e));
                if (nodeClassToSeenShift.containsKey(w)) {
                    final Matrix d = (Matrix) t.minus(nodeClassToSeenShift.get(w));
                    if (!d.isZero()) {
                        shifts.add(d);
                    }
                } else {
                    nodeClassToSeenShift.put(w, t);
                    queue.addLast(w);
                }
            }
        }

        // --- check if all vertex representatives have been reached
        for (final Iterator iter = nodes(); iter.hasNext();) {
            if (!nodeClassToSeenShift.containsKey(iter.next())) {
                cache.put(IS_CONNECTED, new Boolean(false));
                return false;
            }
        }
        
        // --- check if the reachable translations found generate all translations
        final Matrix M = new Matrix(shifts.size(), this.dimension);
        for (int i = 0; i < shifts.size(); ++i) {
            final Matrix s = (Matrix) shifts.get(i);
            for (int j = 0; j < this.dimension; ++j) {
                M.set(i, j, s.get(0, j));
            }
        }
        if (!M.determinant().norm().isOne()) {
            cache.put(IS_CONNECTED, new Boolean(false));
            return false;
        }
        
        // --- at this point, the graph must be connected
        cache.put(IS_CONNECTED, new Boolean(true));
        return true;
    }
    
    /**
     * Computes a barycentric placement for the nodes. Nodes are in barycentric
     * positions if each node is in the center of gravity of its neighbors. In
     * other words, each coordinate for its position is the average of the
     * corresponding coordinates for its neighbors. The barycentric positions
     * are, of course, with respect to the periodic graph. In particular, shifts
     * are taken into account. The returned map, however, contains only the
     * positions for the node representatives.
     * 
     * The barycentric placement of connected graph are unique up to affine
     * transformations, i.e., general basis and origin changes. This method
     * computes coordinates expressed in terms of the basis used for edge shift
     * vectors in this graph. Moreover, the first vertex, as produced by the
     * iterator returned by nodes(), is placed at the origin.
     * 
     * The graph in question must, for now, be connected as for multiple
     * components the barycentric placement is no longer unique.
     * 
     * @return a map giving barycentric positions for the node representatives.
     */
    public Map barycentricPlacement() {
        if (!isConnected()) {
            throw new UnsupportedOperationException("graph must be connected");
        }
        
        // --- see if placement has already been computed
        final Map cached = (Map) cache.get(BARYCENTRIC_PLACEMENT);
        if (cached != null) {
            return cached;
        }
        
        // --- assign an integer index to each node representative
        final Map idToIndex = new HashMap();
        final List indexToId = new ArrayList();
        for (final Iterator iter = nodes(); iter.hasNext();) {
            final Object id = ((INode) iter.next()).id();
            idToIndex.put(id, new Integer(indexToId.size()));
            indexToId.add(id);
        }
        
        // --- set up a system of equations
        final int n = indexToId.size(); // the number of nodes
        final int[][] M = new int[n+1][n];
        final int[][] t = new int[n+1][this.dimension];
        
        for (int i = 0; i < n; ++i) {
            final INode v = (INode) getElement(indexToId.get(i));
            for (final Iterator iter = v.incidences(); iter.hasNext();) {
                final IEdge e = (IEdge) iter.next();
                final INode w = e.target();
                if (v.equals(w)) {
                    // loops cancel out with their reverses, so we must consider
                    // each loop twice (once in each direction) or not at all
                    continue;
                }
                final Matrix s = getShift(e);
                final int j = ((Integer) idToIndex.get(w.id())).intValue();
                --M[i][j];
                ++M[i][i];
                for (int k = 0; k < this.dimension; ++k) {
                    t[i][k] += ((Whole) s.get(0, k)).intValue();
                }
            }
        }
        M[n][0] = 1;
        
        // --- solve the system
        final Matrix P = Matrix.solve(new Matrix(M), new Matrix(t));

        // --- extract the positions found
        final Map result = new HashMap();
        for (int i = 0; i < n; ++i) {
            result.put(getElement(indexToId.get(i)), P.getRow(i));
        }
        
        // --- cache and return the result
        cache.put(BARYCENTRIC_PLACEMENT, result);
        return result;
    }
    
    /**
     * Checks if this graph is stable, meaning that no two nodes have the same
     * positions in a barycentric placement.
     * 
     * @return true if the graph is stable.
     */
    public boolean isStable() {
        final int d = getDimension();
        final Map positions = barycentricPlacement();
        final Set seen = new HashSet();
        for (final Iterator iter = nodes(); iter.hasNext();) {
            final Matrix p = (Matrix) positions.get(iter.next());
            final Matrix p0 = new Matrix(1, d);
            for (int i = 0; i < d; ++i) {
                p0.set(0, i, ((Rational) p.get(0, i)).mod(1));
            }
            p0.makeImmutable();
            if (seen.contains(p0)) {
                return false;
            } else {
                seen.add(p0);
            }
        }
        return true;
    }
    
    /**
     * Checks if this graph is locally stable, meaning that no two nodes with a
     * common neighbor have the same positions in a barycentric placement.
     * 
     * @return true if the graph is locally stable.
     */
    public boolean isLocallyStable() {
        final Boolean cached = (Boolean) cache.get(IS_LOCALLY_STABLE);
        if (cached != null) {
            return cached.booleanValue();
        }
        
        final Map positions = barycentricPlacement();
        for (final Iterator iter = nodes(); iter.hasNext();) {
            final INode v = (INode) iter.next();
            final Set positionsSeen = new HashSet();
            for (final Iterator incident = v.incidences(); incident.hasNext();) {
                final IEdge e = (IEdge) incident.next();
                final Matrix s = getShift(e);
                final Matrix p0 = (Matrix) positions.get(e.target());
                final Matrix p = (Matrix) p0.plus(s);
                if (positionsSeen.contains(p)) {
                    cache.put(IS_LOCALLY_STABLE, new Boolean(false));
                    return false;
                } else {
                    positionsSeen.add(p);
                }
            }
        }
        cache.put(IS_LOCALLY_STABLE, new Boolean(true));
        return true;
    }
    
    /**
     * Finds all additional topological translations in this periodic graph,
     * which must be connected and locally stable and constructs the equivalence
     * classes of nodes defined by these. A topological translation in this case
     * is defined as a graph automorphism which commutes with all the given
     * translations for this periodic graph and leaves no node fixed. This
     * includes "translations" of finite orders, as they may occur in
     * ladder-like structures. A topological translation of finite order must
     * correspond to an identity transformation in the barycentric placement, so
     * it can only occur in non-stable graphs.
     * 
     * An iterator is returned which contains the equivalence classes as sets if
     * additional translations are found. In the special case that none are
     * found, the iterator, however, covers the empty set rather than the set of
     * all single node sets.
     * 
     * @return an iterator over the set of equivalence classes.
     */
    public Iterator translationalEquivalenceClasses() {
        // --- check prerequisites
        if (!isConnected()) {
            throw new UnsupportedOperationException("graph must be connected");
        }
        if (!isLocallyStable()) {
            throw new UnsupportedOperationException("graph must be locally stable");
        }
        
        // --- find extra translations
        final Matrix I = Matrix.one(getDimension());
        final Partition P = new Partition();
        final Iterator iter = nodes();
        final INode start = (INode) iter.next();
        
        while (iter.hasNext()) {
            final INode v = (INode) iter.next();
            if (!P.areEquivalent(start, v)) {
                final Morphism iso;
                try {
                    iso = new Morphism(start, v, I);
                } catch (Morphism.NoSuchMorphismException ex) {
                    continue;
                }
                for (final Iterator it = nodes(); it.hasNext();) {
                    final INode w = (INode) it.next();
                    P.unite(w, iso.get(w));
                }
            }
        }
        
        // --- return the result
        return P.classes();
    }
    
    /**
     * Reduces the entries of a matrix modulo one. All entries must be of type
     * Rational.
     * 
     * @param A the input matrix.
     * @return a copy of the input matrix with each entry reduced modulo one.
     */
    private static Matrix modOne(final Matrix A) {
        final Matrix res = new Matrix(A.numberOfRows(), A.numberOfColumns());
        for (int i = 0; i < A.numberOfRows(); ++i) {
            for (int j = 0; j < A.numberOfColumns(); ++j) {
                res.set(i, j, ((Rational) A.get(i, j)).mod(Whole.ONE));
            }
        }
        return res;
    }
    
    /**
     * Returns a minimal image of the representation graph. This corresponds to
     * a maximal extension of the translation group of the periodic graph
     * consisting of topological translations of infinite order.
     * 
     * Currently, this only works for graphs with no nontrivial translations of
     * finite order.
     * 
     * @return a minimal image.
     */
    public PeriodicGraph minimalImage() {
        // --- some preparations
        final int d = getDimension();
        final Map pos = barycentricPlacement();
        
        // --- find translationally equivalent node classes
        final List classes = Iterators.asList(translationalEquivalenceClasses());
        if (classes.size() == 0) {
            // --- no extra translations, graph is minimal
            return this;
        }
        
        // --- collect the translation vectors
        final List vectors = new ArrayList();
        {
            final Iterator iter = ((Set) classes.get(0)).iterator();
            final INode v = (INode) iter.next();
            final Matrix pv = (Matrix) pos.get(v);

            while (iter.hasNext()) {
                final INode w = (INode) iter.next();
                final Matrix pw = (Matrix) pos.get(w);
                final Matrix t = modOne((Matrix) pw.minus(pv));
                if (t.isZero()) {
                    final String s = "found translation of finite order";
                    throw new UnsupportedOperationException(s);
                } else {
                    vectors.add(t);
                }
            }
        }
        
        // --- init new graph and map old nodes to new nodes and vice versa
        final PeriodicGraph G = new PeriodicGraph(d);
        final Map old2new = new HashMap();
        final Map new2old = new HashMap();
        for (final Iterator iter = classes.iterator(); iter.hasNext();) {
            final Set cl = (Set) iter.next();
            final INode vNew = G.newNode();
            for (final Iterator nodes = cl.iterator(); nodes.hasNext();) {
                final INode vOld = (INode) nodes.next();
                old2new.put(vOld, vNew);
                if (!new2old.containsKey(vNew)) {
                    new2old.put(vNew, vOld);
                }
            }
        }
        
        // --- determine a basis for the extended translation group
        final Matrix M = new Matrix(vectors.size() + d, d);
        final Matrix I = Matrix.one(d);
        for (int i = 0; i < vectors.size(); ++i) {
            M.setRow(i, (Matrix) vectors.get(i));
        }
        for (int i = 0; i < d; ++i) {
            M.setRow(vectors.size() + i, I.getRow(i));
        }
        Matrix.triangulate(M, null, true, false, 0);
        if (M.rank() != d) {
            throw new RuntimeException("internal error - please contact author");
        }
        
        // --- compute the basis change matrix
        final Matrix A = new Matrix(d, d);
        for (int i = 0; i < d; ++i) {
            A.setRow(i, M.getRow(i));
        }
        final Matrix basisChange = (Matrix) A.inverse();
        
        // --- now add the edges for the new graph
        for (final Iterator iter = edges(); iter.hasNext();) {
            // --- extract the data for the next edge
            final IEdge e = (IEdge) iter.next();
            final INode v = e.source();
            final INode w = e.target();
            final Matrix s = getShift(e);
            
            // --- construct the corresponding edge in the new graph
            final INode vNew = (INode) old2new.get(v);
            final INode wNew = (INode) old2new.get(w);
            final INode vRep = (INode) new2old.get(vNew);
            final INode wRep = (INode) new2old.get(wNew);
            final Matrix vShift = (Matrix) ((Matrix) pos.get(v)).minus(pos.get(vRep));
            final Matrix wShift = (Matrix) ((Matrix) pos.get(w)).minus(pos.get(wRep));
            final Matrix sNew = (Matrix) wShift.minus(vShift).plus(s).times(basisChange);
            
            // --- insert this edge if it is not already there
            if (G.getEdge(vNew, wNew, sNew) == null) {
                G.newEdge(vNew, wNew, sNew);
            }
        }
        
        // --- return the new graph
        return G;
    }
    
    /**
     * Derives the set of characteristic bases of this periodic graph. Each
     * basis is represented by an ordered list of d directed edges, where d is
     * the dimension of periodicity of the graph, such that the difference
     * vectors between the source and target of each edge in a barycentric
     * placement are linearly independent. The list of bases is characterstic in
     * the sense that an isomorphism between periodic graphs will induce a
     * bijection between their associated sets of bases.
     * 
     * As this method depends on barycentric placement, the graph must, for now,
     * be connected.
     * 
     * CAVEAT: Please note that the actual set of bases depends on some
     * arbitrary choices in the algorithm. Thus, in order to compare graphs
     * using their characteristic bases or any structure derived using
     * characteristic bases, the same version of the algorithm has to be used on
     * both graphs. This applies, in particular, to canonical forms.
     * 
     * @return the set of characteristic bases, represented by edge lists.
     */
    public List characteristicBases() {
        final List cached = (List) cache.get(CHARACTERISTIC_BASES);
        if (cached != null) {
            return cached;
        }
        
        final List result = new LinkedList();
        final Map pos = barycentricPlacement();
        final int d = getDimension();

        // --- look for edge lists with a common source
        for (final Iterator iter = nodes(); iter.hasNext();) {
            final INode v = (INode) iter.next();
            final List edges = allIncidences(v);
            for (final Iterator good = goodCombinations(edges, pos); good.hasNext();) {
                result.add(good.next());
            }
        }

        if (result.size() == 0) {
            // --- no results, now look for edge lists that form chains
            for (final Iterator iter = nodes(); iter.hasNext();) {
                // --- get the next start node for the chain
                final INode v0 = (INode) iter.next();
                
                // --- initialize objects used in the subsequent search
                final LinkedList iterators = new LinkedList();
                final LinkedList edges = new LinkedList();
                final Matrix M = Matrix.zero(d, d).mutableClone();
                final Matrix z = Matrix.zero(1, d);
                iterators.addLast(allIncidences(v0).iterator());
                edges.addLast(null);
                
                // --- do a depth first search for usable chains
                while (iterators.size() > 0) {
                    final int k = iterators.size();
                    final Iterator current = (Iterator) iterators.getLast();
                    if (current.hasNext()) {
                        // -- get next edge and data related to it
                        final IEdge e = (IEdge) current.next();
                        final INode v = e.source();
                        final INode w = e.target();
                        final Matrix pv = (Matrix) pos.get(v);
                        final Matrix pw = (Matrix) pos.get(w);
                        final Matrix diff = (Matrix) pw.minus(pv).plus(getShift(e));
                        
                        // --- see if so far edge vectors are independent
                        M.setRow(k-1, diff);
                        if (M.rank() == k) {
                            // --- they are
                            edges.removeLast();
                            edges.addLast(e.oriented());
                            if (k == d) {
                                // --- found a result here
                                result.add(edges.clone());
                            } else {
                                // --- have to extend the chain
                                iterators.addLast(allIncidences(w).iterator());
                                edges.addLast(null);
                            }
                        }
                    } else {
                        // --- backtracking
                        iterators.removeLast();
                        edges.removeLast();
                        M.setRow(k-1, z);
                    }
                }
            }
        }
        
        if (result.size() == 0) {
            // --- still nothing, so use general edge lists
            final List edges = allDirectedEdges();
            for (final Iterator good = goodCombinations(edges, pos); good.hasNext();) {
                result.add(good.next());
            }
        }
        
        cache.put(CHARACTERISTIC_BASES, result);
        return result;
    }

    /**
     * Returns all edges incident to the given node, with loops in the representation
     * graph listed once in each direction.
     * 
     * @param v the common source node.
     * @return the list of edges found.
     */
    private List allIncidences(final INode v) {
        final List result = new ArrayList();
        for (final Iterator iter = v.incidences(); iter.hasNext();) {
            final IEdge e = ((IEdge) iter.next()).oriented();
            result.add(e);
            if (e.source().equals(e.target())) {
                result.add(e.reverse());
            }
        }
        return result;
    }

    /**
     * Returns representatives for all directed edges present in this graph. Thus, each
     * undirected edge is produced twice, namely once in each direction.
     * 
     * @return the list of edges found.
     */
    private List allDirectedEdges() {
        final List result = new ArrayList();
        for (final Iterator iter = edges(); iter.hasNext();) {
            final IEdge e = ((IEdge) iter.next()).oriented();
            result.add(e);
            result.add(e.reverse());
        }
        return result;
    }

    /**
     * Constructs an iterator over all ordered sublists of size d of the given
     * edge list, for which the associated vectors form a linearly independent
     * set. Here, d is the dimension of periodicity of this graph. The vector
     * associated to an edge is the difference between the position of its
     * source and target.
     * 
     * @param edges the edge list to pick from.
     * @param pos associates d-dimensional coordinates to nodes.
     * @return an iterator over all good edge combinations.
     */
    private Iterator goodCombinations(final List edges, final Map pos) {
        final int d = getDimension();
        final int n = edges.size();
        if (n < d) {
            throw new IllegalArgumentException("not enough edges to pick from");
        }

        return new IteratorAdapter() {
            private Iterator perms = Iterators.empty();
            private final int a[] = new int[d];

            protected Object findNext() throws NoSuchElementException {
                while (!perms.hasNext()) {
                    int k;
                    if (a[1] == 0) {
                        for (int i = 0; i < d; ++i) {
                            a[i] = i;
                        }
                        k = 0;
                    } else {
                        k = d - 1;
                        while (k >= 0 && n - a[k] <= d - k) {
                            --k;
                        }
                        if (k < 0) {
                            throw new NoSuchElementException("at end");
                        } else {
                            ++a[k];
                            for (int i = k + 1; i < d; ++i) {
                                a[i] = a[k] + i - k;
                            }
                        }
                    }
                    
                    final Matrix M = new Matrix(d, d);
                    for (int i = 0; i < d; ++i) {
                        final IEdge e = (IEdge) edges.get(a[i]);
                        final Matrix pv = (Matrix) pos.get(e.source());
                        final Matrix pw = (Matrix) pos.get(e.target());
                        M.setRow(i, (Matrix) pw.minus(pv).plus(getShift(e)));                        
                    }
                    if (M.rank() == d) {
                        final Object picks[] = new Object[d];
                        for (int i = 0; i < d; ++i) {
                            picks[i] = edges.get(a[i]);
                        }
                        perms = Iterators.permutations(picks);
                    }
                }
                return perms.next();
            }
        };
    }
    
    /**
     * Determines the periodic automorphisms of this periodic graph. A periodic
     * automorphism is one that reflects the periodicity if the graph. It can be
     * represented as an automorphism of the representation graph that induces a
     * linear transformation on the edge shift vectors which is expressed by a
     * unimodular integer matrix.
     * 
     * @return the set of automorphisms, each expressed as a map between nodes
     */
    public Set symmetries() {
        final Set cached = (Set) cache.get(SYMMETRIES);
        if (cached != null) {
            return cached;
        }
        
        // --- check prerequisites
        if (!isConnected()) {
            throw new UnsupportedOperationException("graph must be connected");
        }
        if (!isLocallyStable()) {
            throw new UnsupportedOperationException("graph must be locally stable");
        }
        
        final List generators = new LinkedList();
        final int d = getDimension();
        final List bases = characteristicBases();
        
        final List basis0 = (List) bases.get(0);
        final INode v0 = ((IEdge) basis0.get(0)).source();
        final Matrix B0 = differenceMatrix(basis0);
        final Partition P = new Partition();
        
        for (int i = 0; i < bases.size(); ++i) {
            final List b = (List) bases.get(i);
            boolean seen = true;
            for (int k = 0; k < d; ++k) {
                if (!P.areEquivalent(basis0.get(k), b.get(k))) {
                    seen = false;
                }
            }
            if (seen) {
                continue;
            }
            final INode v = ((IEdge) b.get(0)).source();
            final Matrix B = differenceMatrix(b);
            final Matrix M = Matrix.solve(B0, B);
            if (isUnimodularIntegerMatrix(M)) {
                final Morphism iso;
                try {
                    iso = new Morphism(v0, v, M);
                } catch (Morphism.NoSuchMorphismException ex) {
                    continue;
                }
                generators.add(iso);
                final List edges = allDirectedEdges();
                for (int k = 0; k < edges.size(); ++k) {
                    final IEdge e = (IEdge) edges.get(k);
                    P.unite(e, iso.get(e));
                }
            }
        }
        
        final Set seen = new HashSet();
        final LinkedList queue = new LinkedList();
        final Morphism identity = new Morphism(v0, v0, Matrix.one(d));
        seen.add(identity);
        queue.addLast(identity);
        while (queue.size() > 0) {
            final Morphism phi = (Morphism) queue.removeFirst();
            for (final Iterator gens = generators.iterator(); gens.hasNext();) {
                final Morphism psi = (Morphism) gens.next();
                final Morphism product = phi.times(psi);
                if (!seen.contains(product)) {
                    seen.add(product);
                    queue.addLast(product);
                }
            }
        }
        
        cache.put(SYMMETRIES, seen);
        return seen;
    }

    /**
     * Extracts the difference vectors for an edge.
     * 
     * @param e an edge.
     * @return the difference vectors
     */
    public Matrix differenceVector(final IEdge e) {
        final Map pos = barycentricPlacement();
        final Matrix pv = (Matrix) pos.get(e.source());
        final Matrix pw = (Matrix) pos.get(e.target());
        return (Matrix) pw.minus(pv).plus(getShift(e));
    }
    
    /**
     * Extracts the difference vectors from a list of edges and turns them into
     * the rows of a matrix.
     * 
     * @param edges a list of edges.
     * @return a matrix composed of difference vectors
     */
    private Matrix differenceMatrix(final List edges) {
        final int n = edges.size();
        
        final Matrix M = new Matrix(n, getDimension());
        for (int i = 0; i < n; ++i) {
            M.setRow(i, differenceVector((IEdge) edges.get(i)));                        
        }
        return M;
    }
    
    /**
     * Checks if all entries of the given matrix are whole numbers.
     * 
     * @param M a matrix
     * @return true if M has only integer entries
     */
    private boolean isUnimodularIntegerMatrix(final Matrix M) {
        for (int i = 0; i < M.numberOfRows(); ++i) {
            for (int j = 0; j < M.numberOfColumns(); ++j) {
                if (!(M.get(i, j) instanceof Whole)) {
                    return false;
                }
            }
        }
        return M.determinant().norm().isOne();
    }
    
    /**
     * Computes a basis for the translation lattice of this graph which turns
     * all the affine transformations associated to periodic symmetries into
     * isometries.
     * 
     * @return the new basis as a matrix.
     */
    public Matrix symmetricBasis() {
        // -- preparations
        final int d = getDimension();
        final Set syms = symmetries();
        
        // --- compute a symmetry-invariant quadratic form
        Matrix M = Matrix.zero(d, d);
        for (final Iterator iter = syms.iterator(); iter.hasNext();) {
            final Matrix A = ((Morphism) iter.next()).getMatrix();
            M = (Matrix) M.plus(A.times(A.transposed()));
        }
        M = (Matrix) M.times(new Fraction(1, syms.size()));
        
        // --- compute an orthonormal basis for the new form
        final Matrix B = LinearAlgebra.orthonormalRowBasis(M);
        
        // --- result is old basis expressed in terms of new
        return (Matrix) B.inverse();
    }

    /**
     * Implements an embedded portion of the infinite graph with methods to
     * retrieve the addresses of elements in terms of the representation graph.
     */
    public class EmbeddedPortion extends Embedding {
        final private Map elementIdToAddress = new HashMap();
        final private Map addressToElementId = new HashMap();
        final private Map placement;
        final private Matrix basis;
        
        public EmbeddedPortion(final Map placement, final Matrix basis) {
            super(new UndirectedGraph());
            this.placement = placement;
            this.basis = basis;
        }
        
        public PeriodicGraph getRepresentationGraph() {
            return PeriodicGraph.this;
        }
        
        public INode newNode(final INode rep, final Matrix shift) {
            if (getElement(rep, shift) != null) {
                throw new IllegalArgumentException("node already exists");
            }
            final INode v = getGraph().newNode();
            final Pair adr = new Pair(rep, shift);
            this.elementIdToAddress.put(v.id(), adr);
            this.addressToElementId.put(adr, v.id());
            return v;
        }
        
        public IEdge newEdge(final IEdge rep, final Matrix shift) {
            if (getElement(rep, shift) != null) {
                throw new IllegalArgumentException("edge already exists");
            }
            final INode sourceRep = rep.source();
            final INode targetRep = rep.target();
            final Matrix sourceShift = shift;
            final Matrix targetShift = (Matrix) shift.plus(PeriodicGraph.this.getShift(rep));

            final Pair sourceAdr = new Pair(sourceRep, sourceShift);
            final Object sourceId = this.addressToElementId.get(sourceAdr);
            final INode source = (INode) getGraph().getElement(sourceId);
            if (source == null) {
                throw new UnsupportedOperationException("source node must be present");
            }

            final Pair targetAdr = new Pair(targetRep, targetShift);
            final Object targetId = this.addressToElementId.get(targetAdr);
            final INode target = (INode) getGraph().getElement(targetId);
            if (target == null) {
                throw new UnsupportedOperationException("target node must be present");
            }
            
            final IEdge e = getGraph().newEdge(source, target);
            final Pair adr = new Pair(rep, shift);
            this.elementIdToAddress.put(e.id(), adr);
            this.addressToElementId.put(adr, e.id());
            final Matrix t = getRepresentationGraph().getShift(rep);
            final Pair revAdr = new Pair(rep.reverse(), shift.plus(t));
            this.addressToElementId.put(revAdr, e.id());
            return null;
        }
        
        public void setPosition(final INode v, final Matrix p) {
            throw new UnsupportedOperationException("not allowed");
        }
        
        public IGraphElement getElement(final IGraphElement rep, final Matrix shift) {
            final Object id = this.addressToElementId.get(new Pair(rep, shift));
            return getGraph().getElement(id);
        }
        
        public IGraphElement getRepresentative(final IGraphElement x) {
            if (!getGraph().hasElement(x)) {
                throw new IllegalArgumentException("no such node or edge");
            }
            final Pair adr = (Pair) elementIdToAddress.get(x.id());
            return (IGraphElement) adr.getFirst();
        }
        
        public Matrix getShift(final IGraphElement x) {
            if (!getGraph().hasElement(x)) {
                throw new IllegalArgumentException("no such node or edge");
            }
            final Pair adr = (Pair) elementIdToAddress.get(x.id());
            return (Matrix) adr.getSecond();
        }

        public Matrix getPosition(final INode v) {
            final Matrix p = (Matrix) this.placement.get(getRepresentative(v));
            return (Matrix) p.plus(getShift(v)).times(this.basis);
        }
    }
    
    /**
     * Constructs a finite portion of the periodic graph in the form of an
     * ordinary graph with nodes labelled by their position in a given
     * embedding.
     * 
     * @param v0 the central node of the finite portion to construct.
     * @param radius the maximum graph distance from the central node.
     * @param positions coordinates for node representatives.
     * @param basis the basis to be used to convert into cartesian coordinates.
     * 
     * @return the newly constructed graph.
     */
    public Embedding embeddedNeighborhood(final INode v0, final int radius,
            final Map positions, final Matrix basis) {
        final EmbeddedPortion result = new EmbeddedPortion(positions, basis);
        final Map nodeToDist = new HashMap();
        final LinkedList queue = new LinkedList();        
        
        final INode w0 = result.newNode(v0, Matrix.zero(1, getDimension()));
        nodeToDist.put(w0, new Integer(0));
        queue.addLast(w0);
        
        while (queue.size() > 0) {
            final INode wOld = (INode) queue.removeFirst();
            final Integer distOld = (Integer) nodeToDist.get(wOld);
            final Integer distNew = new Integer(distOld.intValue() + 1);
            
            if (distNew.intValue() <= radius) {
                final INode vOld = (INode) result.getRepresentative(wOld);
                final Matrix tOld = result.getShift(wOld);
                for (final Iterator iter = allIncidences(vOld).iterator(); iter.hasNext();) {
                    final IEdge e = (IEdge) iter.next();
                    final INode vNew = e.target();
                    final Matrix tNew = (Matrix) tOld.plus(getShift(e));
                    INode wNew = (INode) result.getElement(vNew, tNew);
                    if (wNew == null) {
                        wNew = result.newNode(vNew, tNew);
                        nodeToDist.put(wNew, distNew);
                        queue.addLast(wNew);
                    }
                    if (result.getElement(e, tOld) == null) {
                        result.newEdge(e, tOld);
                    }
                }
            }
        }
        
        return result;
    }
    
    // TODO compute orbits and stabilizers
    
    /**
     * Computes a invariant for this periodic graph. An invariant is an object,
     * in this case a list, that is unique for an isomorphism class of periodic
     * graphs. In other words, the invariant does not depend on the original
     * representation of a graph, but only on the "essential structure" of the
     * graph itself. The invariants computed here have the additional advantage
     * that nonisomorphic graphs necessarily have different invariants. Even
     * further, they can be used to construct a canonical representation for a
     * given isomorphism class.
     * 
     * @return the invariant.
     */
    public NiceIntList invariant() {
        if (DEBUG) {
            System.out.println("\nComputing invariant for " + this);
        }
        final NiceIntList cached = (NiceIntList) cache.get(INVARIANT);
        if (cached != null) {
            return cached;
        }
        
        // --- check prerequisites
        if (!isConnected()) {
            throw new UnsupportedOperationException("graph must be connected");
        }
        if (!isLocallyStable()) {
            throw new UnsupportedOperationException("graph must be locally stable");
        }
        
        final int d = getDimension();
        final int m = numberOfEdges();
        final List bases = characteristicBases();
        final Matrix zero = Matrix.zero(1, d);

        class EdgeCmd implements Comparable {
            public int source;
            public int target;
            public Matrix shift;
            
            public EdgeCmd(final int v, final int w, final Matrix s) {
                this.source = v;
                this.target = w;
                this.shift = s;
            }
            
            public int compareTo(final Object other) {
                if (other instanceof EdgeCmd) {
                    final EdgeCmd e = (EdgeCmd) other;
                    if (e.source != this.source) {
                        return this.source - e.source;
                    } else if (e.target != this.target) {
                        return this.target - e.target;
                    } else {
                        return signOfShift((Matrix) this.shift.minus(e.shift));
                    }
                } else {
                    throw new IllegalArgumentException();
                }
            }
            
            private String shiftAsString() {
                final StringBuffer buf = new StringBuffer(10);
                buf.append("[");
                final Matrix A = this.shift;
                for (int i = 0; i < A.numberOfColumns(); ++i) {
                    if (i > 0) {
                        buf.append(",");
                    }
                    buf.append(A.get(0, i));
                }
                buf.append("]");
                return buf.toString();
            }
        
            public String toString() {
                return "(" + source + "," + target + "," + shiftAsString() + ")";
            }
        }
        
        final EdgeCmd bestScript[] = new EdgeCmd[m];
        List bestBasis = null;
        if (DEBUG) {
            System.out.println("  Found " + bases.size() + " bases\n");
        }
        
        for (int i = 0; i < bases.size(); ++i) {
            final List b = (List) bases.get(i);
            if (DEBUG) {
                System.out.println("  Checking basis " + b);
            }
            final INode v0 = ((IEdge) b.get(0)).source();
            final Matrix B = differenceMatrix(b);
            final Matrix B_1 = (Matrix) B.inverse();
            
            final LinkedList Q = new LinkedList();
            Q.addLast(new Pair(v0, zero));
            final Map old2new = new HashMap();
            old2new.put(v0, new Integer(1));
            final Map newPos = new HashMap();
            newPos.put(v0, zero);

            int nextVertex = 2;
            int edgesSoFar = 0;
            boolean equal = (bestBasis != null);
            
            class Break extends Throwable {
            }
            
            try {
                while (Q.size() > 0) {
                    final Pair entry = (Pair) Q.removeFirst();
                    final INode v = (INode) entry.getFirst();
                    final int vn = ((Integer) old2new.get(v)).intValue();
                    final Matrix p = (Matrix) entry.getSecond();
                    
                    // --- collect neighbors and sort by mapped difference vectors
                    final List incident = allIncidences(v);
                    final Matrix M = (Matrix) differenceMatrix(incident).times(B_1);
                    final Map edgeToRow = new HashMap();
                    for (int k = 0; k < incident.size(); ++k) {
                        edgeToRow.put(incident.get(k), M.getRow(k));
                    }
                    Collections.sort(incident, new Comparator() {
                        public int compare(final Object arg0, final Object arg1) {
                            final Matrix a = (Matrix) edgeToRow.get(arg0);
                            final Matrix b = (Matrix) edgeToRow.get(arg1);
                            return signOfShift((Matrix) a.minus(b));
                        }
                    });
                    
                    // --- loop over neighbors
                    for (final Iterator it = incident.iterator(); it.hasNext();) {
                        final IEdge e = (IEdge) it.next();
                        final INode w = e.target();
                        final Matrix s = (Matrix) p.plus(edgeToRow.get(e));
                        final int wn;
                        final Matrix shift;
                        
                        if (!old2new.containsKey(w)) {
                            // --- edge connects to new vertex class
                            Q.addLast(new Pair(w, s));
                            wn = nextVertex++;
                            old2new.put(w, new Integer(wn));
                            newPos.put(w, s);
                            shift = zero;
                        } else {
                            wn = ((Integer) old2new.get(w)).intValue();
                            if (wn < vn) {
                                // --- wrong direction
                                continue;
                            }
                            // --- compute shift vector for new edge
                            shift = (Matrix) s.minus(newPos.get(w));
                        }
                        if (vn < wn || (vn == wn && signOfShift(shift) < 0)) {
                            // --- compare with the best result to date
                            final EdgeCmd newEdge = new EdgeCmd(vn, wn, shift);
                            if (DEBUG) {
                                System.out.print("    New edge " + newEdge);
                            }
                            
                            if (equal) {
                                if (DEBUG) {
                                    System.out.print(" - compared to " + bestScript[edgesSoFar] + " at " + edgesSoFar + " -");
                                }
                                final int cmp = newEdge.compareTo(bestScript[edgesSoFar]);
                                if (cmp < 0) {
                                    if (DEBUG) {
                                        System.out.print(" is a winner.");
                                    }
                                    equal = false;
                                    bestBasis = b;
                                } else if (cmp > 0) {
                                    if (DEBUG) {
                                        System.out.println(" is a loser.");
                                    }
                                    throw new Break();
                                } else {
                                    if (DEBUG) {
                                        System.out.print(" is equal.");
                                    }
                                }
                            }
                            if (DEBUG) {
                                System.out.println("");
                            }
                            if (!equal) {
                                if (DEBUG) {
                                    System.out.println("    Writing " + newEdge + " at position " + edgesSoFar);
                                }
                                bestScript[edgesSoFar] = newEdge;
                            }
                            edgesSoFar++;
                        }
                    }
                }
            } catch (Break done) {
                continue;
            }
            bestBasis = b;
        }
        
        // --- collect basis vectors for the lattice
        
        final Matrix A = Matrix.zero(d, d).mutableClone();
        int k = 0;
        for (int i = 0; i < m; ++i) {
            A.setRow(k, bestScript[i].shift);
            if (A.rank() == k + 1) {
                ++k;
                if (k == d) {
                    break;
                }
            }
        }
        final Matrix B = differenceMatrix(bestBasis);

        if (!((Matrix) A.times(B)).determinant().abs().equals(Whole.ONE)) {
            final Matrix M = Matrix.zero(m, d).mutableClone();
            for (int i = 0; i < m; ++i) {
                M.setRow(i, bestScript[i].shift);
            }
            Matrix.triangulate(M, null, true, false, 0);

            for (int i = 0; i < d; ++i) {
                A.setRow(i, M.getRow(i));
            }
        }
        
        // --- compute the basis change matrix
        if (!((Matrix) A.times(B)).determinant().abs().equals(Whole.ONE)) {
            throw new RuntimeException("internal error - please contact author");
        }
        final Matrix basisChange = (Matrix) A.inverse();
        
        // --- apply the basis change to the best script
        for (int i = 0; i < m; ++i) {
            final EdgeCmd cmd = bestScript[i];
            Matrix shift = (Matrix) cmd.shift.times(basisChange);
            for (int j = 0; j < d; ++j) {
                if (!(shift.get(0, j) instanceof Whole)) {
                    throw new RuntimeException("internal error - please contact author");
                }
            }
            if (cmd.source == cmd.target && signOfShift(shift) > 0) {
                shift = (Matrix) shift.negative();
            }
            bestScript[i] = new EdgeCmd(cmd.source, cmd.target, shift);
        }

        // --- sort the converted script
        Arrays.sort(bestScript);
        
        // --- construct the canonical form and the invariant
        final PeriodicGraph canonical = new PeriodicGraph(d);
        final List invariant = new LinkedList();
        invariant.add(new Integer(this.getDimension()));
        
        final int n = numberOfNodes();
        final INode nodes[] = new INode[n+1];
        for (int i = 1; i <= n; ++i) {
            nodes[i] = canonical.newNode();
        }
        for (int i = 0; i < m; ++i) {
            final EdgeCmd cmd = bestScript[i];
            canonical.newEdge(nodes[cmd.source], nodes[cmd.target], cmd.shift);
            invariant.add(new Integer(cmd.source));
            invariant.add(new Integer(cmd.target));
            for (int j = 0; j < d; ++j) {
                invariant.add(new Integer(((Whole) cmd.shift.get(0, j)).intValue()));
            }
        }
        
        // --- consistency test
        final Matrix B1 = (Matrix) B.inverse().times(basisChange);
        try {
            new Morphism(((IEdge) bestBasis.get(0)).source(), nodes[1], B1);
        } catch (Morphism.NoSuchMorphismException ex) {
            throw new RuntimeException("internal error - please contact author");
        }

        // --- cache the results
        cache.put(CANONICAL, canonical);
        cache.put(INVARIANT, new NiceIntList(invariant));
        
        return new NiceIntList(invariant);
    }
    
    /**
     * Computes a canonical form for this periodic graph. A canonical form is a
     * representation for a given graph that is unique for its isomorphism
     * class. In other words, the canonical form does not depend on the original
     * representation of a graph, but only on the "essential structure" of the
     * graph itself.
     * 
     * @return the canonical form.
     */
    public PeriodicGraph canonical() {
        invariant();
        return (PeriodicGraph) cache.get(CANONICAL);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals()
     */
    public boolean equals(final Object other) {
        if (other instanceof PeriodicGraph) {
            final PeriodicGraph G = (PeriodicGraph) other;
            return (G.getDimension() == this.getDimension()
                    && G.numberOfNodes() == this.numberOfNodes()
                    && G.numberOfEdges() == this.numberOfEdges()
                    && this.invariant().equals(G.invariant()));
        } else {
            return false;
        }
    }
    
    /* (non-Javadoc)
     * @see int java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final Object arg) {
        if (!(arg instanceof PeriodicGraph)) {
            throw new IllegalArgumentException("argument must be a PeriodicGraph");
        }
        final PeriodicGraph other = (PeriodicGraph) arg;
        if (this.getDimension() != other.getDimension()) {
            return this.getDimension() - other.getDimension();
        } else if (this.numberOfNodes() != other.numberOfNodes()){
            return this.numberOfNodes() - other.numberOfNodes();
        } else if (this.numberOfEdges() != other.numberOfEdges()) {
            return this.numberOfEdges() - other.numberOfEdges();
        } else {
            return this.invariant().compareTo(other.invariant());
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return this.invariant().hashCode();
    }
    
    // TODO compute configuration space for embeddings
}
