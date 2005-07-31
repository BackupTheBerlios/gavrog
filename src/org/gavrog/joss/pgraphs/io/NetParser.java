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

package org.gavrog.joss.pgraphs.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.collections.Pair;
import org.gavrog.jane.compounds.LinearAlgebra;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.Fraction;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Real;
import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;
import org.gavrog.joss.pgraphs.basic.SpaceGroup;


/**
 * @author Olaf Delgado
 * @version $Id: NetParser.java,v 1.22 2005/07/31 19:44:59 odf Exp $
 */
public class NetParser extends GenericParser {
    // TODO make things work for nets of dimension 2 as well (4 also?)
    
    private final static boolean DEBUG = false;
    
    private class NodeDescriptor {
        public final Object name;
        public final int connectivity;
        public final Matrix site;
        
        public NodeDescriptor(final Object name, final int connectivity, final Matrix site) {
            this.name = name;
            this.connectivity = connectivity;
            this.site = site;
        }
        
        public String toString() {
            return "Node(" + name + ", " + connectivity + ", " + site + ")";
        }
    }
    
    private class EdgeDescriptor {
        public final Object source;
        public final Object target;
        public final Matrix shift;
        
        public EdgeDescriptor(final Object source, final Object target, final Matrix shift) {
            this.source = source;
            this.target = target;
            this.shift = shift;
        }
        
        public String toString() {
            return "Edge(" + source + ", " + target + ", " + shift + ")";
        }
    }
    
    public NetParser(final BufferedReader input) {
        super(input);
        this.synonyms = makeSynonyms();
        this.defaultKey = "edge";
    }
    
    public NetParser(final Reader input) {
        this(new BufferedReader(input));
    }
    
    private Map makeSynonyms() {
        final Map result = new HashMap();
        result.put("vertex", "node");
        result.put("vertices", "node");
        result.put("vertexes", "node");
        result.put("atom", "node");
        result.put("atoms", "node");
        result.put("nodes", "node");
        result.put("bond", "edge");
        result.put("bonds", "edge");
        result.put("edges", "edge");
        result.put("spacegroup", "group");
        return Collections.unmodifiableMap(result);
    }
    
    public static PeriodicGraph stringToNet(final String s) {
        return new NetParser(new StringReader(s)).parseNet();
    }
    
    public PeriodicGraph parseNet() {
        final Entry block[] = parseBlock();
        final String type = lastBlockType().toLowerCase();
        if (type.equals("periodic_graph")) {
            return parsePeriodicGraph(block);
        } else if (type.equals("crystal")) {
            return parseCrystal3D(block);
        } else if (type.equals("net")) {
            return parseNet3D(block);
        } else {
            throw new UnsupportedOperationException("type " + type + " not supported");
        }
    }
    
    /**
     * Parses a specification for a raw periodic net. In this format, each line
     * specifies a translational equivalence class of edges of the net, given by
     * the names for the translational equivalence classes of the source and
     * target nodes and the additional lattice translation to be applied to the
     * target with respect to the lattice translation given for the source.
     * 
     * Example:
     * 
     * <code>
     * PERIODIC_GRAPH # the diamond net
     *   1 2  0 0 0
     *   1 2  1 0 0
     *   1 2  0 1 0
     *   1 2  0 0 1
     * END
     * </code>
     * 
     * @param block the pre-parsed input.
     * @return the periodic graph constructed from the input.
     */
    private PeriodicGraph parsePeriodicGraph(final Entry block[]) {
        PeriodicGraph G = null;
        final Map nameToNode = new HashMap();
        
        for (int i = 0; i < block.length; ++i) {
            if (block[i].key.equals("edge")) {
                final List row = block[i].values;
                final int d = row.size() - 2;
                if (d < 1) {
                    final String msg = "not enough fields at line ";
                    throw new DataFormatException(msg + block[i].lineNumber);
                } else if (G == null) {
                    G = new PeriodicGraph(d);
                } else if (d != G.getDimension()) {
                    final String msg = "inconsistent shift dimensions at line ";
                    throw new DataFormatException(msg + block[i].lineNumber);
                }
                INode v = (INode) nameToNode.get(row.get(0));
                if (v == null) {
                    v = G.newNode();
                    nameToNode.put(row.get(0), v);
                }
                INode w = (INode) nameToNode.get(row.get(1));
                if (w == null) {
                    w = G.newNode();
                    nameToNode.put(row.get(1), w);
                }
                final Matrix s = new Matrix(1, d);
                for (int k = 0; k < d; ++k) {
                    s.set(0, k, (Whole) row.get(k+2));
                }
                G.newEdge(v, w, s);
            }
        }
        return G;
    }

    /**
     * Parses a 3-periodic net given in terms of a crystallographic group. Edges
     * are specified in a similar way as in parsePeriodicGraph(), but instead of
     * just lattice translation, any operator from the symmetry group may be
     * used. Group operators are in symbolic form, as in "y,x,z+1/2". For nodes
     * not in general position, i.e., with a non-trivial stabilizer, their
     * respective special positions must be given in symbolic form, as e.g. in
     * "x,y,x+1/2". Only the variables "x", "y" and "z" are recognized in
     * symbolic forms for both group operators and special positions.
     * 
     * Example:
     * 
     * <code>
     * NET # the diamond net
     *   Group Fd-3m
     *   Node 1 3/8,3/8,3/8
     *   Edge 1 1 1-x,1-y,1-z
     * END
     * </code>
     * 
     * @param block the pre-parsed input.
     * @return the periodic graph constructed from the input.
     */
    private PeriodicGraph parseNet3D(final Entry[] block) {
        final PeriodicGraph G = new PeriodicGraph(3);
        String group = null;
        List ops = new ArrayList();
        List nodeDescriptors = new LinkedList();
        List edgeDescriptors = new LinkedList();
        final Map nodeNameToDesc = new HashMap();
        final Map addressToNode = new HashMap();
        final Map addressToShift = new HashMap();
        
        // --- collect data from the input
        for (int i = 0; i < block.length; ++i) {
            final List row = block[i].values;
            if (block[i].key.equals("group")) {
                if (group == null) {
                    if (row.size() < 1) {
                        final String msg = "Missing argument at line ";
                        throw new DataFormatException(msg + block[i].lineNumber);
                    }
                    group = (String) row.get(0);
                    ops.addAll(operators(group));
                    if (ops == null) {
                        final String msg = "Space group not recognized at line ";
                        throw new DataFormatException(msg + block[i].lineNumber);
                    }
                } else {
                    final String msg = "Group specified twice at line ";
                    throw new DataFormatException(msg + block[i].lineNumber);
                }
            } else if (block[i].key.equals("node")) {
                if (row.size() < 1) {
                    final String msg = "Missing argument at line ";
                    throw new DataFormatException(msg + block[i].lineNumber);
                }
                final Object name = row.get(0);
                if (nodeNameToDesc.containsKey(name)) {
                    final String msg = "Node specified twice at line ";
                    throw new DataFormatException(msg + block[i].lineNumber);
                }
                final Matrix position = parsePosition(row, 1);
                final NodeDescriptor node = new NodeDescriptor(name, -1, position);
                nodeDescriptors.add(node);
                nodeNameToDesc.put(name, node);
            } else if (block[i].key.equals("edge")) {
                if (row.size() < 2) {
                    final String msg = "Not enough arguments at line ";
                    throw new DataFormatException(msg + block[i].lineNumber);
                }
                final Object sourceName = row.get(0);
                final Object targetName = row.get(1);
                final Matrix shift = parsePosition(row, 2);
                if (!ops.contains(normalizedOperator(shift))) {
                    final String msg = "Operator not in given group at line ";
                    throw new DataFormatException(msg + block[i].lineNumber);
                }
                final EdgeDescriptor edge = new EdgeDescriptor(sourceName, targetName, shift);
                edgeDescriptors.add(edge);
            }
        }
        
        // --- convert to primitive setting
        final SpaceGroup sg = new SpaceGroup(3, ops, false, false);
        final Set primitiveOps = sg.primitiveOperators();
        final Matrix to = sg.transformationToPrimitive();
        final Matrix from = (Matrix) to.inverse();
        
        ops.clear();
        for (final Iterator iter = primitiveOps.iterator(); iter.hasNext();) {
            final Matrix op = (Matrix) iter.next();
            ops.add(normalizedOperator((Matrix) from.times(op).times(to)));
        }
        
        final List nodeDescsTmp = new LinkedList();
        for (final Iterator iter = nodeDescriptors.iterator(); iter.hasNext();) {
            final NodeDescriptor desc = (NodeDescriptor) iter.next();
            final Matrix site = (Matrix) desc.site.times(to);
            nodeDescsTmp.add(new NodeDescriptor(desc.name, desc.connectivity, site));
        }
        nodeDescriptors.clear();
        nodeDescriptors.addAll(nodeDescsTmp);
        
        final List edgeDescsTmp = new LinkedList();
        for (final Iterator iter = edgeDescriptors.iterator(); iter.hasNext();) {
            final EdgeDescriptor desc = (EdgeDescriptor) iter.next();
            final Matrix shift = (Matrix) from.times(desc.shift).times(to);
            edgeDescsTmp.add(new EdgeDescriptor(desc.source, desc.target, shift));
        }
        edgeDescriptors.clear();
        edgeDescriptors.addAll(edgeDescsTmp);
        
        // TODO provide better error handling in the following
        
        // --- apply group operators to generate all nodes
        for (final Iterator it1 = nodeDescriptors.iterator(); it1.hasNext();) {
            final NodeDescriptor node = (NodeDescriptor) it1.next();
            final Object name = node.name;
            final Matrix site = node.site;
            final Map siteToNode = new HashMap();
            for (final Iterator it2 = ops.iterator(); it2.hasNext();) {
                final Matrix op = (Matrix) it2.next();
                final Matrix mappedSite = (Matrix) site.times(op);
                final Matrix mappedSiteNormalized = normalizedOperator(mappedSite);
                final INode v;
                final Pair address = new Pair(name, op);
                if (siteToNode.containsKey(mappedSiteNormalized)) {
                    v = (INode) siteToNode.get(mappedSiteNormalized);
                } else {
                    v = G.newNode();
                    siteToNode.put(mappedSiteNormalized, v);
                }
                addressToNode.put(address, v);
                addressToShift.put(address, mappedSite.minus(mappedSiteNormalized));
            }
        }
        
        // --- apply group operators to generate all edges
        for (final Iterator it1 = edgeDescriptors.iterator(); it1.hasNext();) {
            final EdgeDescriptor edge = (EdgeDescriptor) it1.next();
            final Object sourceName = edge.source;
            final Object targetName = edge.target;
            final Matrix shift = edge.shift;
            for (final Iterator it2 = ops.iterator(); it2.hasNext();) {
                final Matrix sourceOp = (Matrix) it2.next();
                final Matrix targetOp = (Matrix) shift.times(sourceOp);
                final Matrix sourceOpNormalized = normalizedOperator(sourceOp);
                final Matrix targetOpNormalized = normalizedOperator(targetOp);
                final Pair sourceAddress = new Pair(sourceName, sourceOpNormalized);
                final Pair targetAddress = new Pair(targetName, targetOpNormalized);
                final Matrix sourceShift = (Matrix) sourceOp.minus(sourceOpNormalized);
                final Matrix targetShift = (Matrix) targetOp.minus(targetOpNormalized);
                final Matrix edgeShift = (Matrix) targetShift.minus(sourceShift);
                
                final INode v = (INode) addressToNode.get(sourceAddress);
                final INode w = (INode) addressToNode.get(targetAddress);
                final Matrix shiftv = (Matrix) addressToShift.get(sourceAddress);
                final Matrix shiftw = (Matrix) addressToShift.get(targetAddress);
                final Matrix totalShift = (Matrix) edgeShift.plus(shiftw.minus(shiftv));
                final Matrix s = new Matrix(1, 3);
                for (int i = 0; i < 3; ++i) {
                    s.set(0, i, totalShift.get(3, i));
                }
                if (G.getEdge(v, w, s) == null) {
                    G.newEdge(v, w, s);
                }
            }
        }
        
        return G;
    }

    /**
     * Parses a crystal descriptor and constructs the corresponding atom-bond
     * network.
     * 
     * Example:
     * 
     * <code>
     * CRYSTAL
     *   GROUP Fd-3m
     *   CELL         2.3094 2.3094 2.3094  90.0 90.0 90.0
     *   ATOM  1  4   5/8 5/8 5/8
     * END
     * </code>
     * 
     * @param block the pre-parsed input.
     * @return the periodic graph constructed from the input.
     */
    private PeriodicGraph parseCrystal3D(final Entry[] block) {
        final Matrix I = Matrix.one(3);        

        final Set seen = new HashSet();
        
        String groupname = "P1";
        List ops = new ArrayList();
        
        Real cellA = FloatingPoint.ONE;
        Real cellB = FloatingPoint.ONE;
        Real cellC = FloatingPoint.ONE;
        Real cellAlpha = new FloatingPoint(90.0);
        Real cellBeta = cellAlpha;
        Real cellGamma = cellAlpha;
        Matrix cellGram = I;
        
        double precision = 0.001;
        double minEdgeLength = 0.95;
        double maxEdgeLength = Double.MAX_VALUE;
        
        final List nodeDescriptors = new LinkedList();
        final Map nodeNameToDesc = new HashMap();
        
        // --- collect data from the input
        for (int i = 0; i < block.length; ++i) {
            final List row = block[i].values;
            final String key = block[i].key;
            if (key.equals("group")) {
                if (seen.contains(key)) {
                    final String msg = "Group specified twice at line ";
                    throw new DataFormatException(msg + block[i].lineNumber);
                }
                if (row.size() < 1) {
                    final String msg = "Missing argument at line ";
                    throw new DataFormatException(msg + block[i].lineNumber);
                }
                groupname = (String) row.get(0);
                ops.addAll(operators(groupname));
                if (ops == null) {
                    final String msg = "Space group not recognized at line ";
                    throw new DataFormatException(msg + block[i].lineNumber);
                }
            } else if (key.equals("cell")) {
                if (seen.contains(key)) {
                    final String msg = "Cell specified twice at line ";
                    throw new DataFormatException(msg + block[i].lineNumber);
                }
                if (row.size() != 6) {
                    final String msg = "Expected 6 arguments at line ";
                    throw new DataFormatException(msg + block[i].lineNumber);
                }
                for (int j = 0; j < 6; ++j) {
                    if (!(row.get(i) instanceof Real)) {
                        final String msg = "Arguments must be real numbers at line ";
                        throw new DataFormatException(msg + block[i].lineNumber);
                    }
                }
                cellA = (Real) row.get(0);
                cellB = (Real) row.get(1);
                cellC = (Real) row.get(2);
                cellAlpha = (Real) row.get(3);
                cellBeta = (Real) row.get(4);
                cellGamma = (Real) row.get(5);
                cellGram = gramMatrix(cellA, cellB, cellC, cellAlpha, cellBeta, cellGamma);
            } else if (block[i].key.equals("node")) {
                if (row.size() != 5) {
                    final String msg = "Expected 5 arguments at line ";
                    throw new DataFormatException(msg + block[i].lineNumber);
                }
                final Object name = row.get(0);
                if (nodeNameToDesc.containsKey(name)) {
                    final String msg = "Node specified twice at line ";
                    throw new DataFormatException(msg + block[i].lineNumber);
                }
                final Object conn = row.get(1);
                if (!(conn instanceof Whole && ((Whole) conn).isPositive())) {
                    final String msg = "Connectivity must be a positive integer ";
                    throw new DataFormatException(msg + block[i].lineNumber);
                }
                final Matrix position = Matrix.zero(1, 4).mutableClone();
                for (int j = 0; j < 3; ++j) {
                    position.set(0, j, ((Real) row.get(j + 2)).mod(1));
                }
                position.set(0, 3, Whole.ONE);
                final int c = ((Whole) conn).intValue();
                final NodeDescriptor node = new NodeDescriptor(name, c, position);
                nodeDescriptors.add(node);
                nodeNameToDesc.put(name, node);
            }
            seen.add(key);
        }
        
        if (DEBUG) {
            System.err.println();
            System.err.println("Group name: " + groupname);
            System.err.println("  operators:");
            for (final Iterator iter = ops.iterator(); iter.hasNext();) {
                System.err.println("    " + iter.next());
            }
            System.err.println();

            System.err.println("Cell parameters:");
            System.err.println("  a = " + cellA);
            System.err.println("  b = " + cellB);
            System.err.println("  c = " + cellC);
            System.err.println("  alpha = " + cellAlpha);
            System.err.println("  beta  = " + cellBeta);
            System.err.println("  gamma = " + cellGamma);
            System.err.println("  gram matrix = " + cellGram);
            System.err.println();
            
            System.err.println("Nodes:");
            for (final Iterator iter = nodeDescriptors.iterator(); iter.hasNext();) {
                System.err.println("  " + iter.next());
            }
        }
        
        // --- convert to primitive setting
        final SpaceGroup sg = new SpaceGroup(3, ops, false, false);
        final Matrix primitiveCell = sg.primitiveCell();
        final Set primitiveOps = sg.primitiveOperators();
        final Matrix to = sg.transformationToPrimitive();
        final Matrix from = (Matrix) to.inverse();
        
        ops.clear();
        for (final Iterator iter = primitiveOps.iterator(); iter.hasNext();) {
            final Matrix op = (Matrix) iter.next();
            ops.add(normalizedOperator((Matrix) from.times(op).times(to)));
        }
        
        final List nodeDescsTmp = new LinkedList();
        for (final Iterator iter = nodeDescriptors.iterator(); iter.hasNext();) {
            final NodeDescriptor desc = (NodeDescriptor) iter.next();
            final Matrix site = (Matrix) desc.site.times(to);
            nodeDescsTmp.add(new NodeDescriptor(desc.name, desc.connectivity, site));
        }
        nodeDescriptors.clear();
        nodeDescriptors.addAll(nodeDescsTmp);
        
        cellGram = (Matrix) primitiveCell.times(cellGram).times(
                primitiveCell.transposed());
        
        if (DEBUG) {
            System.err.println();
            System.err.println("Primitive cell: " + primitiveCell);
        }
        
        // --- construct a Dirichlet domain for the translation group
        final Matrix reducedBasis = LinearAlgebra.sellingReducedRows(I, cellGram);
        final Matrix t1 = reducedBasis.getRow(0);
        final Matrix t2 = reducedBasis.getRow(1);
        final Matrix t3 = reducedBasis.getRow(2);
        final IArithmetic dirichletVectors[] = new IArithmetic[] {
                t1, t2, t3, t1.plus(t2), t1.plus(t3), t2.plus(t3), t1.plus(t2).plus(t3)
        };
        if (DEBUG) {
            System.err.println();
            System.err.println("Selling reduced basis: " + reducedBasis);
            System.err.println("  in terms of original cell: "
                               + reducedBasis.times(primitiveCell));
        }
        
        // --- apply group operators to generate all nodes
        final PeriodicGraph G = new PeriodicGraph(3);
        final Map nodeToPosition = new HashMap();
        final Map nodeToDescriptor = new HashMap();
        
        for (final Iterator itNodes = nodeDescriptors.iterator(); itNodes.hasNext();) {
            final NodeDescriptor desc = (NodeDescriptor) itNodes.next();
            if (DEBUG) {
                System.err.println();
                System.err.println("Mapping node " + desc);
            }
            final Matrix site = desc.site;
            final Set stabilizer = stabilizer(site, ops, precision);
            if (DEBUG) {
                System.err.println("  stabilizer has size " + stabilizer.size());
            }
            // --- loop through the cosets of the stabilizer
            final Set opsSeen = new HashSet();
            for (final Iterator itOps = ops.iterator(); itOps.hasNext();) {
                // --- get the next coset representative
                final Matrix op = normalizedOperator((Matrix) itOps.next());
                if (!opsSeen.contains(op)) {
                    if (DEBUG) {
                        System.err.println("  applying " + op);
                    }
                    // --- compute mapped node position
                    final Matrix p = (Matrix) site.times(op);
                    final Matrix pos = p.getSubMatrix(0, 0, 1, 3);
                    // --- construct a new node
                    final INode v = G.newNode();
                    // --- store some data for it
                    nodeToPosition.put(v, pos);
                    nodeToDescriptor.put(v, desc);
                    for (final Iterator itStab = stabilizer.iterator(); itStab.hasNext();) {
                        final Matrix a = (Matrix) itStab.next();
                        opsSeen.add(normalizedOperator((Matrix) a.times(op)));
                    }
                }
            }
        }

        if (DEBUG) {
            System.err.println();
            System.err.println("Generated " + G.numberOfNodes() + " nodes in unit cell.");
        }
        
        // --- shift generated nodes into the Dirichlet domain
        for (final Iterator iter = nodeToPosition.keySet().iterator(); iter.hasNext();) {
            final INode v = (INode) iter.next();
            final Matrix p = (Matrix) nodeToPosition.get(v);
            // --- shift into Dirichlet domain
            final Matrix shift = dirichletShift(p, dirichletVectors, cellGram, Whole.ONE);
            nodeToPosition.put(v, p.plus(shift));
        }
        
        // --- compute nodes in two times extended Dirichlet domain
        final List extended = new ArrayList();
        final Map addressToPosition = new HashMap();
        final Matrix zero = Matrix.zero(1, 3);
        for (final Iterator iter = G.nodes(); iter.hasNext();) {
            final INode v = (INode) iter.next();
            final Matrix pv = (Matrix) nodeToPosition.get(v);
            if (DEBUG) {
                System.err.println();
                System.err.println("Extending " + v + " at " + pv);
            }
            extended.add(new Pair(v, zero));
            addressToPosition.put(new Pair(v, zero), pv);
            for (int i = 0; i < 7; ++i) {
                final Matrix vec = (Matrix) dirichletVectors[i];
                if (DEBUG) {
                    System.err.println("  shifting by " + vec);
                }
                final Matrix p = (Matrix) pv.plus(vec);
                final Matrix shift = dirichletShift(p, dirichletVectors, cellGram,
                        new Whole(2));
                if (DEBUG) {
                    System.err.println("    additional shift is " + shift);
                }
                final Pair adr = new Pair(v, vec.plus(shift));
                extended.add(adr);
                addressToPosition.put(adr, p.plus(shift));
            }
        }
        
        if (DEBUG) {
            System.err.println();
            System.err.println("Generated " + extended.size()
                               + " nodes in extended Dirichlet domain.");
        }
        
        // --- compute the edges using nearest neighbors
        for (final Iterator iter = G.nodes(); iter.hasNext();) {
            final INode v = (INode) iter.next();
            final Pair adr0 = new Pair(v, zero);
            final Matrix pv = (Matrix) nodeToPosition.get(v);
            final List distances = new ArrayList();
            for (int i = 0; i < extended.size(); ++i) {
                final Pair adr = (Pair) extended.get(i);
                if (adr.equals(adr0)) {
                    continue;
                }
                final Matrix pos = (Matrix) addressToPosition.get(adr);
                final Matrix diff = (Matrix) pos.minus(pv);
                final IArithmetic dist = LinearAlgebra.dotRows(diff, diff, cellGram);
                distances.add(new Pair(dist, new Integer(i)));
            }

            Collections.sort(distances);
            
            if (DEBUG) {
                System.err.println();
                System.err.println("Neighbors for " + v + " at " + nodeToPosition.get(v)
                                   + ":");
                for (int i = 0; i < 6 && i < distances.size(); ++i) {
                    final Pair entry = (Pair) distances.get(i);
                    final Object dist = entry.getFirst();
                    final int index = ((Integer) entry.getSecond()).intValue();
                    final Pair adr = (Pair) extended.get(index);
                    System.err.println("  " + new Pair(dist, adr));
                }
            }

            final NodeDescriptor desc = (NodeDescriptor) nodeToDescriptor.get(v);
            final int connectivity = desc.connectivity;
            int neighbors = 0;
            
            for (final Iterator it2 = distances.iterator(); it2.hasNext();) {
                if (v.degree() >= connectivity) {
                    if (v.degree() > connectivity) {
                        final String msg = "too many neighbors found for node ";
                        throw new DataFormatException(msg + v);
                    }
                    break;
                }
                final Pair entry = (Pair) it2.next();
                final double dist = ((Real) entry.getFirst()).doubleValue();
                final int index = ((Integer) entry.getSecond()).intValue();
                final Pair adr = (Pair) extended.get(index);
                final INode w = (INode) adr.getFirst();
                final Matrix s = (Matrix) adr.getSecond();
                if (dist < minEdgeLength) {
                    final String msg = "found points closer than minimal edge length of ";
                    throw new DataFormatException(msg + minEdgeLength);
                } else if (dist > maxEdgeLength) {
                    final String msg = "not enough neighbors found for node ";
                    throw new DataFormatException(msg + v);
                }
                if (G.getEdge(v, w, s) == null) {
                    G.newEdge(v, w, s);
                }
                ++neighbors;
                if (w.equals(v)) {
                    ++neighbors;
                }
            }
        }
        
        if (DEBUG) {
            System.err.println("--------------------");
        }
        
        return G;
    }
    
    private static Matrix gramMatrix(final Real a, final Real b, final Real c,
            final Real alpha, final Real beta, final Real gamma) {

        final Real alphaG = (Real) cosine(alpha).times(b).times(c);
        final Real betaG = (Real) cosine(beta).times(a).times(c);
        final Real gammaG = (Real) cosine(gamma).times(a).times(b);

        return new Matrix(new IArithmetic[][] {
                { a.raisedTo(2), gammaG, betaG },
                { gammaG, b.raisedTo(2), alphaG },
                { betaG, alphaG, c.raisedTo(2) },
        });
    }
    
    private static Real cosine(final Real arg) {
        final double f = Math.PI / 180.0;
        return new FloatingPoint(Math.cos(arg.doubleValue() * f));
    }
    
    private static Set stabilizer(final Matrix site, final List ops, final double precision) {
        // --- compute the stabilizer of this node's translation class
        final Set stabilizer = new HashSet();
        final Whole one = Whole.ONE;
        
        for (final Iterator it2 = ops.iterator(); it2.hasNext();) {
            final Matrix op = (Matrix) it2.next();
            final Matrix diff = (Matrix) site.minus(site.times(op));
            double maxD = 0.0;
            for (int i = 0; i < 3; ++i) {
                final double d = ((Real) diff.get(0, i).mod(one)).doubleValue();
                maxD = Math.max(maxD, Math.min(d, 1.0 - d));
            }
            if (maxD < precision) {
                stabilizer.add(normalizedOperator(op));
            }
        }
        
        // --- check if stabilizer forms a group
        for (final Iterator iter1 = stabilizer.iterator(); iter1.hasNext();) {
            final Matrix A = (Matrix) iter1.next();
            for (final Iterator iter2 = stabilizer.iterator(); iter2.hasNext();) {
                final Matrix B = (Matrix) iter2.next();
                final Matrix AB_ = normalizedOperator((Matrix) A.times(B.inverse()));
                if (!stabilizer.contains(AB_)) {
                    throw new RuntimeException("precision problem in stabilizer computation");
                }
            }
        }

        return stabilizer;
    }
    
    private static Matrix dirichletShift(final Matrix pos,
            final IArithmetic dirichletVectors[],
            final Matrix cellGram, final Whole factor) {

        final Whole one = Whole.ONE;
        final Real half = new Fraction(1, 2);
        final Real eps = new FloatingPoint(1e-8);
        Matrix shift = Matrix.zero(1, 3);
        
        while (true) {
            boolean changed = false;
            for (int i = 0; i < 7; ++i) {
                final Matrix v = (Matrix) dirichletVectors[i].times(factor);
                final IArithmetic c = LinearAlgebra.dotRows(v, v, cellGram);
                final Matrix p = (Matrix) pos.plus(shift);
                final IArithmetic q = LinearAlgebra.dotRows(p, v, cellGram).dividedBy(c);
                if (q.isGreaterThan(half.plus(eps))) {
                    shift = (Matrix) shift.minus(v.times(q.floor().plus(one)));
                    changed = true;
                } else if (q.isLessOrEqual(half.negative())) {
                    shift = (Matrix) shift.minus(v.times(q.floor()));
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
        
        return shift;
    }
    
    private static Matrix parsePosition(final List fields, final int startIndex) {
        if (fields.size() <= startIndex) {
            return Matrix.one(4);
        } else {
            final StringBuffer buf = new StringBuffer(40);
            for (int i = startIndex; i < fields.size(); ++i) {
                buf.append(' ');
                buf.append(fields.get(i));
            }
            return parseOperator(buf.toString());
        }
    }
    
    public static Matrix parseOperator(final String s) {
        final String msg = "Bad operator format for \"" + s + "\": "; // just in case
        
        final String parts[] = s.replaceAll("\\s+", "").split(",");
        if (parts.length > 3) {
            throw new DataFormatException(msg + "more than 3 coordinates.");
        }
        final int d = parts.length;
        final String varNames = "xyz".substring(0, d) + "#";
        final Matrix M = new Matrix(d+1, d+1);
        M.setColumn(d, Matrix.zero(d+1, 1));
        M.set(d, d, new Whole(1));
        
        for (int i = 0; i < d; ++i) {
            final String term = parts[i] + "#";
            final int m = term.length() - 1;
            int k = 0;
            while (k < m) {
                IArithmetic f = new Whole(1);
                if (term.charAt(k) == '-') {
                    f = f.negative();
                    ++k;
                } else if (term.charAt(k) == '+') {
                    ++k;
                }
                int j = k;
                while (Character.isDigit(term.charAt(k))) {
                    ++k;
                }
                if (k > j) {
                    final int z = Integer.parseInt(term.substring(j, k));
                    f = f.times(new Whole(z));
                }
                if (term.charAt(k) == '/') {
                    ++k;
                    j = k;
                    while (Character.isDigit(term.charAt(k))) {
                        ++k;
                    }
                    if (k > j) {
                        final int z = Integer.parseInt(term.substring(j, k));
                        f = f.dividedBy(new Whole(z));
                    } else {
                        throw new DataFormatException(msg + "fraction has no denominator");
                    }
                }
                if (term.charAt(k) == '*') {
                    ++k;
                }
                final char c = term.charAt(k);
                j = varNames.indexOf(c);
                if (j >= 0) {
                    ++k;
                } else if (Character.isDigit(c) || "+-".indexOf(c) >= 0) {
                    j = d;
                } else {
                    throw new DataFormatException(msg + "illegal variable name " + c);
                }
                if (M.get(j, i) != null) {
                    throw new DataFormatException(msg + "variable used twice");
                } else {
                    M.set(j, i, f);
                }
            }
        }
        for (int i = 0; i < d+1; ++i) {
            for (int j = 0; j < d+1; ++j) {
                if (M.get(i, j) == null) {
                    M.set(i, j, Whole.ZERO);
                }
            }
        }
        M.makeImmutable();
        
        return M;
    }
    
    private static Matrix normalizedOperator(final Matrix op) {
        final Matrix result = op.mutableClone();
        final int d = op.numberOfRows() - 1;
        for (int i = 0; i < d; ++i) {
            result.set(d, i, op.get(d, i).mod(Whole.ONE));
        }
        return result;
    }
    
    private static Map nameToOps = null;
    private static Map nameToTransform = null;
    private static Map translationTable = null;
        
    private static void parseGroups(final String filename) {
        final ClassLoader classLoader = NetParser.class.getClassLoader();
        final InputStream inStream = classLoader.getResourceAsStream(filename);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));

        nameToOps = new HashMap();
        nameToTransform = new HashMap();
        
        String currentName = null;
        
        while (true) {
            final String line;
            try {
                line = reader.readLine();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            if (line == null) {
                break;
            }
            if (line.length() == 0) {
                continue;
            }
            final int i = line.indexOf(' ');
            if (i > 0) {
                currentName = line.substring(0, i);
                nameToOps.put(currentName, new LinkedList());
                final Matrix T = NetParser.parseOperator(line
                        .substring(i + 1));
                nameToTransform.put(currentName, T);
            } else if (currentName != null) {
                Matrix op = NetParser.parseOperator(line);
                op = NetParser.normalizedOperator(op);
                ((List) nameToOps.get(currentName)).add(op);
            } else {
                throw new DataFormatException("error in space group table file");
            }
        }
        
        for (final Iterator iter = nameToOps.keySet().iterator(); iter.hasNext();) {
            final Object key = iter.next();
            nameToOps.put(key, Collections.unmodifiableList((List) nameToOps.get(key)));
        }
    }
    
    public static Iterator groupNames() {
        if (nameToOps == null) {
            parseGroups("javaPGraphs/IO/sgtable.data");
        }

        return nameToOps.keySet().iterator();
    }
    
    private static Object retrieve(final String name, final boolean getOps) {
        if (translationTable == null) {
            translationTable = new HashMap();
            translationTable.put("P2", "P121");
            translationTable.put("P21", "P1211");
            translationTable.put("C2", "C121");
            translationTable.put("Pm", "P1m1");
            translationTable.put("Pc", "P1c1");
            translationTable.put("Cm", "C1m1");
            translationTable.put("Cc", "C1c1");
            translationTable.put("P2/m", "P12/m1");
            translationTable.put("P21/m", "P121/m1");
            translationTable.put("C2/m", "C12/m1");
            translationTable.put("P2/c", "P12/c1");
            translationTable.put("P21/c", "P121/c1");
            translationTable.put("C2/c", "C12/c1");
        }
        
        if (nameToOps == null) {
            parseGroups("org/gavrog/joss/pgraphs/io/sgtable.data");
        }

        final String parts[] = name.split(":");
        String base = capitalized(parts[0]);
        if (translationTable.containsKey(base)) {
            base = (String) translationTable.get(base);
        }
        final String ext = parts.length > 1 ? capitalized(parts[1]) : "";
        
        final String candidates[];
        if (base.charAt(0) == 'R') {
            if (ext.equals("R")) {
                candidates = new String[] { base + ":R" };
            } else {
                candidates = new String[] { base + ":H", base + ":R" };
            }
        } else if (ext.equals("1")) {
            candidates = new String[] { base + ":1", base };
        } else {
            candidates = new String[] { base, base + ":2", base + ":1" };
        }
        
        for (int i = 0; i < candidates.length; ++i) {
            final String key = candidates[i];
            if (getOps) {
                if (nameToOps.containsKey(key)) {
                    return nameToOps.get(key);
                }
            } else {
                if (nameToTransform.containsKey(key)) {
                    return nameToTransform.get(key);
                }
            }
        }

        return null;
    }
    
    public static List operators(final String name) {
        return (List) retrieve(name, true);
    }
    
    public static Matrix transform(final String name) {
        return (Matrix) retrieve(name, false);
    }
    
    /**
     * Turn a string's first letter to upper case.
     * @param s the source string.
     * @return the capitalized version.
     */
    private static String capitalized(String s) {
        if (s.length() > 1) {
            return s.substring(0, 1).toUpperCase() + s.substring(1);
        } else {
            return s.toUpperCase();
        }
    }
}
