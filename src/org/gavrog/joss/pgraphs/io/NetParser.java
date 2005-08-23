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
import org.gavrog.joss.geometry.Operator;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.SpaceGroup;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;


/**
 * Contains methods to parse a net specification in Systre format (file extension "cgd").
 * 
 * @author Olaf Delgado
 * @version $Id: NetParser.java,v 1.34 2005/08/23 22:02:49 odf Exp $
 */
public class NetParser extends GenericParser {
    // TODO make things work for nets of dimension 2 as well (4 also?)
    
    // --- used to enable or disable a log of the parsing process
    private final static boolean DEBUG = false;
    
    /**
     * Helper class - encapsulates the preliminary specification of a node.
     */
    private class NodeDescriptor {
        public final Object name;     // the node's name
        public final int connectivity; // the node's connectivity
        public final Operator site;     // the position or site of the node
        
        public NodeDescriptor(final Object name, final int connectivity, final Operator site) {
            this.name = name;
            this.connectivity = connectivity;
            this.site = site;
        }
        
        public String toString() {
            return "Node(" + name + ", " + connectivity + ", " + site + ")";
        }
    }
    
    /**
     * Helper class - encapsulates the preliminary specification of an edge.
     */
    private class EdgeDescriptor {
        public final Object source; // the edge's source node representative
        public final Object target;   // the edge's target node representative
        public final Operator shift;  // shift to be applied to the target representative
        
        public EdgeDescriptor(final Object source, final Object target, final Operator shift) {
            this.source = source;
            this.target = target;
            this.shift = shift;
        }
        
        public String toString() {
            return "Edge(" + source + ", " + target + ", " + shift + ")";
        }
    }
    
    /**
     * Constructs an instance.
     * 
     * @param input the input stream.
     */
    public NetParser(final BufferedReader input) {
        super(input);
        this.synonyms = makeSynonyms();
        this.defaultKey = "edge";
    }
    
    /**
     * Constructs an instance.
     * 
     * @param input the input stream.
     */
    public NetParser(final Reader input) {
        this(new BufferedReader(input));
    }
    
    /**
     * Sets up a keyword map to be used by {@link GenericParser#parseBlock()}.
     * 
     * @return the mapping of keywords.
     */
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
    
    /**
     * Utility method - takes a string and directly returns the net specified by it.
     * 
     * @param s the specification string.
     * @return the net constructed from the input string.
     */
    public static PeriodicGraph stringToNet(final String s) {
        return new NetParser(new StringReader(s)).parseNet();
    }
    
    /**
     * Parses the input stream as specified in the constructor and returns the net
     * specified by it.
     * 
     * @return the periodic net constructed from the input.
     */
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
                    ops.addAll(SpaceGroup.operators(group));
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
                final Operator position = parseSiteOrOperator(row, 1);
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
                final Operator shift = parseSiteOrOperator(row, 2);
                if (!ops.contains(shift.modZ())) {
                    final String msg = "Operator not in given group at line ";
                    throw new DataFormatException(msg + block[i].lineNumber);
                }
                final EdgeDescriptor edge = new EdgeDescriptor(sourceName, targetName,
                        shift);
                edgeDescriptors.add(edge);
            }
        }
        
        // --- convert to primitive setting
        final SpaceGroup sg = new SpaceGroup(3, ops, false, false);
        final Set primitiveOps = sg.primitiveOperators();
        final Operator to = sg.transformationToPrimitive();
        final Operator from = (Operator) to.inverse();
        
        ops.clear();
        for (final Iterator iter = primitiveOps.iterator(); iter.hasNext();) {
            final Operator op = (Operator) iter.next();
            ops.add(((Operator) from.times(op).times(to)).modZ());
        }
        
        final List nodeDescsTmp = new LinkedList();
        for (final Iterator iter = nodeDescriptors.iterator(); iter.hasNext();) {
            final NodeDescriptor desc = (NodeDescriptor) iter.next();
            final Operator site = (Operator) desc.site.times(to);
            nodeDescsTmp.add(new NodeDescriptor(desc.name, desc.connectivity, site));
        }
        nodeDescriptors.clear();
        nodeDescriptors.addAll(nodeDescsTmp);
        
        final List edgeDescsTmp = new LinkedList();
        for (final Iterator iter = edgeDescriptors.iterator(); iter.hasNext();) {
            final EdgeDescriptor desc = (EdgeDescriptor) iter.next();
            final Operator shift = (Operator) from.times(desc.shift).times(to);
            edgeDescsTmp.add(new EdgeDescriptor(desc.source, desc.target, shift));
        }
        edgeDescriptors.clear();
        edgeDescriptors.addAll(edgeDescsTmp);
        
        // TODO provide better error handling in the following
        
        // --- apply group operators to generate all nodes
        for (final Iterator it1 = nodeDescriptors.iterator(); it1.hasNext();) {
            // --- find the next node
            final NodeDescriptor node = (NodeDescriptor) it1.next();
            final Object name = node.name;
            final Operator site = node.site;
            final Map siteToNode = new HashMap();
            for (final Iterator it2 = ops.iterator(); it2.hasNext();) {
                final Operator op = (Operator) it2.next();
                final Operator image = (Operator) site.times(op);
                final Operator imageModZ = image.modZ();
                final INode v;
                final Pair address = new Pair(name, op);
                if (siteToNode.containsKey(imageModZ)) {
                    v = (INode) siteToNode.get(imageModZ);
                } else {
                    v = G.newNode();
                    siteToNode.put(imageModZ, v);
                }
                addressToNode.put(address, v);
                addressToShift.put(address, image.floorZ());
            }
        }
        
        // --- apply group operators to generate all edges
        for (final Iterator it1 = edgeDescriptors.iterator(); it1.hasNext();) {
            final EdgeDescriptor edge = (EdgeDescriptor) it1.next();
            final Object sourceName = edge.source;
            final Object targetName = edge.target;
            final Operator shift = edge.shift;
            for (final Iterator it2 = ops.iterator(); it2.hasNext();) {
                final Operator srcOp = (Operator) it2.next();
                final Operator trgOp = (Operator) shift.times(srcOp);
                final Pair sourceAddress = new Pair(sourceName, srcOp.modZ());
                final Pair targetAddress = new Pair(targetName, trgOp.modZ());
                final Matrix edgeShift = (Matrix) trgOp.floorZ().minus(srcOp.floorZ());
                
                final INode v = (INode) addressToNode.get(sourceAddress);
                final INode w = (INode) addressToNode.get(targetAddress);
                final Matrix shiftv = (Matrix) addressToShift.get(sourceAddress);
                final Matrix shiftw = (Matrix) addressToShift.get(targetAddress);
                final Matrix totalShift = (Matrix) edgeShift.plus(shiftw.minus(shiftv));
                if (G.getEdge(v, w, totalShift) == null) {
                    G.newEdge(v, w, totalShift);
                }
            }
        }
        
        if (DEBUG) {
            System.err.println("generated " + G);
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
                ops.addAll(SpaceGroup.operators(groupname));
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
                final Matrix position = Matrix.zero(4, 4).mutableClone();
                for (int j = 0; j < 3; ++j) {
                    position.set(3, j, (IArithmetic) row.get(j + 2));
                }
                position.set(3, 3, Whole.ONE);
                final int c = ((Whole) conn).intValue();
                final Operator op = new Operator(position).modZ();
                final NodeDescriptor node = new NodeDescriptor(name, c, op);
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
        final Operator to = sg.transformationToPrimitive();
        final Operator from = (Operator) to.inverse();
        
        ops.clear();
        for (final Iterator iter = primitiveOps.iterator(); iter.hasNext();) {
            final Operator op = (Operator) iter.next();
            ops.add(((Operator) from.times(op).times(to)).modZ());
        }
        
        final List nodeDescsTmp = new LinkedList();
        for (final Iterator iter = nodeDescriptors.iterator(); iter.hasNext();) {
            final NodeDescriptor desc = (NodeDescriptor) iter.next();
            final Operator site = (Operator) desc.site.times(to);
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
        final Vector t1 = new Vector(reducedBasis.getRow(0));
        final Vector t2 = new Vector(reducedBasis.getRow(1));
        final Vector t3 = new Vector(reducedBasis.getRow(2));
        final Vector dirichletVectors[] = new Vector[] {
                t1, t2, t3,
                (Vector) t1.plus(t2), (Vector) t1.plus(t3), (Vector) t2.plus(t3),
                (Vector) t1.plus(t2).plus(t3)
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
            final Operator site = desc.site;
            final Set stabilizer = stabilizer(site, ops, precision);
            if (DEBUG) {
                System.err.println("  stabilizer has size " + stabilizer.size());
            }
            // --- loop through the cosets of the stabilizer
            final Set opsSeen = new HashSet();
            for (final Iterator itOps = ops.iterator(); itOps.hasNext();) {
                // --- get the next coset representative
                final Operator op = ((Operator) itOps.next()).modZ();
                if (!opsSeen.contains(op)) {
                    if (DEBUG) {
                        System.err.println("  applying " + op);
                    }
                    // --- compute mapped node position
                    final Operator p = (Operator) site.times(op);
                    // --- construct a new node
                    final INode v = G.newNode();
                    // --- store some data for it
                    nodeToPosition.put(v, p.translationalPart().getCoordinates());
                    nodeToDescriptor.put(v, desc);
                    for (final Iterator itStab = stabilizer.iterator(); itStab.hasNext();) {
                        final Operator a = (Operator) itStab.next();
                        opsSeen.add(((Operator) a.times(op)).modZ());
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
            final Point p = new Point((Matrix) nodeToPosition.get(v));
            // --- shift into Dirichlet domain
            final Vector shift = dirichletShift(p, dirichletVectors, cellGram, Whole.ONE);
            nodeToPosition.put(v, p.plus(shift));
        }
        
        // --- compute nodes in two times extended Dirichlet domain
        final List extended = new ArrayList();
        final Map addressToPosition = new HashMap();
        final Vector zero = Vector.zero(3);
        for (final Iterator iter = G.nodes(); iter.hasNext();) {
            final INode v = (INode) iter.next();
            final Point pv = (Point) nodeToPosition.get(v);
            if (DEBUG) {
                System.err.println();
                System.err.println("Extending " + v + " at " + pv);
            }
            extended.add(new Pair(v, zero));
            addressToPosition.put(new Pair(v, zero), pv);
            for (int i = 0; i < 7; ++i) {
                final Vector vec = dirichletVectors[i];
                if (DEBUG) {
                    System.err.println("  shifting by " + vec);
                }
                final Point p = (Point) pv.plus(vec);
                final Vector shift = dirichletShift(p, dirichletVectors, cellGram,
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
            final Point pv = (Point) nodeToPosition.get(v);
            final List distances = new ArrayList();
            for (int i = 0; i < extended.size(); ++i) {
                final Pair adr = (Pair) extended.get(i);
                if (adr.equals(adr0)) {
                    continue;
                }
                final Point pos = (Point) addressToPosition.get(adr);
                final Vector diff0 = (Vector) pos.minus(pv);
                final Matrix diff = diff0.getCoordinates();
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
                final Matrix s = ((Vector) adr.getSecond()).getCoordinates();
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
    
    /**
     * Constructs a gram matrix for the edge vectors of a unit cell which is specified by
     * its cell parameters as according to crystallographic conventions.
     * 
     * @param a the length of the first vector.
     * @param b the length of the second vector.
     * @param c the length of the third vector.
     * @param alpha the angle between the second and third vector.
     * @param beta the angle between the first and third vector.
     * @param gamma the angle between the first and second vector.
     * @return the gram matrix for the vectors.
     */
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
    
    /**
     * Computes the cosine of an angle given in degrees, using the {@link Real} type for
     * the argument and return value.
     * 
     * @param arg the angle in degrees.
     * @return the value of the cosine.
     */
    private static Real cosine(final Real arg) {
        final double f = Math.PI / 180.0;
        return new FloatingPoint(Math.cos(arg.doubleValue() * f));
    }
    
    /**
     * Computes the stabilizer of a site modulo lattice translations.The infinity norm
     * (largest absolute value of a matrix entry) is used to determine the distances
     * between points.
     * 
     * Currently only tested for point sites.
     * 
     * @param site the site.
     * @param ops operators forming the symmetry group.
     * @param precision points this close are considered equal.
     * @return the set of operators forming the stabilizer
     */
    private static Set stabilizer(final Operator site, final List ops, final double precision) {
        final int dim = site.getDimension();
        final Set stabilizer = new HashSet();
        final Whole one = Whole.ONE;
        
        for (final Iterator it2 = ops.iterator(); it2.hasNext();) {
            final Operator op = (Operator) it2.next();
            final Matrix diff = (Matrix) site.getCoordinates().minus(
                    ((Operator) site.times(op)).getCoordinates());
            double maxD = 0.0;
            for (int i = 0; i < dim-1; ++i) {
                for (int j = 0; j < dim; ++j) {
                    final double d = ((Real) diff.get(i, j)).doubleValue();
                    maxD = Math.max(maxD, d);
                }
            }
            for (int j = 0; j < dim; ++j) {
                final double d = ((Real) diff.get(dim, j).mod(one)).doubleValue();
                maxD = Math.max(maxD, Math.min(d, 1.0 - d));
            }
            if (maxD <= precision) { // using "<=" allows for precision 0
                stabilizer.add(op.modZ());
            }
        }
        
        // --- check if stabilizer forms a group
        for (final Iterator iter1 = stabilizer.iterator(); iter1.hasNext();) {
            final Operator A = (Operator) iter1.next();
            for (final Iterator iter2 = stabilizer.iterator(); iter2.hasNext();) {
                final Operator B = (Operator) iter2.next();
                final Operator AB_ = ((Operator) A.times(B.inverse())).modZ();
                if (!stabilizer.contains(AB_)) {
                    throw new RuntimeException("precision problem in stabilizer computation");
                }
            }
        }

        return stabilizer;
    }
    
    /**
     * Returns the vector by which a point has to be shifted in order to obtain a
     * translationally equivalent point within the Dirichlet cell around the origin. All
     * calculations are with respect to the unit lattice and an specific metric, which is
     * passed as one of the arguments. An integral scaling factor can be specified, in
     * which case both the unit lattice and its Dirchlet domain are taken to be scaled by
     * that factor.
     * 
     * @param pos the original point position.
     * @param dirichletVectors normals to the parallel face pairs of the Dirichlet cell.
     * @param metric the underlying metric.
     * @param factor a scaling factor.
     * @return the shift vector needed to move the point inside.
     */
    private static Vector dirichletShift(final Point pos,
            final Vector dirichletVectors[], final Matrix metric, final Whole factor) {

        final Whole one = Whole.ONE;
        final Real half = new Fraction(1, 2);
        final Real eps = new FloatingPoint(1e-8);
        final Point origin = Point.origin(3);
        Vector shift = Vector.zero(3);
        
        while (true) {
            boolean changed = false;
            for (int i = 0; i < 7; ++i) {
                final Vector v = (Vector) dirichletVectors[i].times(factor);
                final IArithmetic c = Vector.dot(v, v, metric);
                final Vector p = (Vector) pos.plus(shift).minus(origin);
                final IArithmetic q = Vector.dot(p, v, metric).dividedBy(c);
                if (q.isGreaterThan(half.plus(eps))) {
                    shift = (Vector) shift.minus(v.times(q.floor().plus(one)));
                    changed = true;
                } else if (q.isLessOrEqual(half.negative())) {
                    shift = (Vector) shift.minus(v.times(q.floor()));
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
        
        return shift;
    }
    
    /**
     * Utility method to parse an operator or site (same format) from a string
     * specification which is broken up into fields. The specified fields are
     * concatenated, using blanks as field separators, and the result is passed to the
     * {@link Operator#Operator(String)} constructor.
     * 
     * @param fields a list of fields.
     * @param startIndex the field index to start parsing at.
     * @return the result as an {@link Operator}.
     */
    private static Operator parseSiteOrOperator(final List fields, final int startIndex) {
        if (fields.size() <= startIndex) {
            return Operator.identity(3);
        } else {
            final StringBuffer buf = new StringBuffer(40);
            for (int i = startIndex; i < fields.size(); ++i) {
                buf.append(' ');
                buf.append(fields.get(i));
            }
            return new Operator(buf.toString());
        }
    }
}
