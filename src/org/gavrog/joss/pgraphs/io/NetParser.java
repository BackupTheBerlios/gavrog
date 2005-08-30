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
 * @version $Id: NetParser.java,v 1.43 2005/08/30 23:16:56 odf Exp $
 */
public class NetParser extends GenericParser {
    // --- used to enable or disable a log of the parsing process
    private final static boolean DEBUG = false;
    
    /**
     * Helper class - encapsulates the preliminary specification of a node.
     */
    private static class NodeDescriptor {
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
    private static class EdgeDescriptor {
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
    private static Map makeSynonyms() {
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
            return parseCrystal(block);
        } else if (type.equals("net")) {
            return parseSymmetricNet(block);
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
     * <pre>
     * PERIODIC_GRAPH # the diamond net
     *   1 2  0 0 0
     *   1 2  1 0 0
     *   1 2  0 1 0
     *   1 2  0 0 1
     * END
     * </pre>
     * 
     * @param block the pre-parsed input.
     * @return the periodic graph constructed from the input.
     */
    private static PeriodicGraph parsePeriodicGraph(final Entry block[]) {
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
     * Parses a periodic net given in terms of a crystallographic group. Edges are
     * specified in a similar way as in parsePeriodicGraph(), but instead of just lattice
     * translation, any operator from the symmetry group may be used.
     * 
     * Group operators are in symbolic form, as in "y,x,z+1/2". For nodes not in general
     * position, i.e., with a non-trivial stabilizer, their respective special positions
     * must be given in symbolic form, as e.g. in "x,y,x+1/2". Symbolic specifications for
     * both operators and special positions are handled by {@link Operator#parse(String)}.
     * 
     * Example:
     * 
     * <pre>
     * 
     *  NET # the diamond net
     *    Group Fd-3m
     *    Node 1 3/8,3/8,3/8
     *    Edge 1 1 1-x,1-y,1-z
     *  END
     *  
     * </pre>
     * 
     * @param block the pre-parsed input.
     * @return the periodic graph constructed from the input.
     */
    private static PeriodicGraph parseSymmetricNet(final Entry[] block) {
        String groupName = null;
        int dimension = 0;
        SpaceGroup group = null;
        List ops = new ArrayList();
        List nodeDescriptors = new LinkedList();
        List edgeDescriptors = new LinkedList();
        final Map nodeNameToDesc = new HashMap();
        
        // --- collect data from the input
        for (int i = 0; i < block.length; ++i) {
            final List row = block[i].values;
            if (block[i].key.equals("group")) {
                if (groupName == null) {
                    if (row.size() < 1) {
                        final String msg = "Missing argument at line ";
                        throw new DataFormatException(msg + block[i].lineNumber);
                    }
                    groupName = (String) row.get(0);
                    if (Character.isLowerCase(groupName.charAt(0))) {
                        dimension = 2;
                    } else if (dimension == 0) {
                        dimension = 3;
                    }
                    group = new SpaceGroup(dimension, groupName);
                    ops.addAll(group.getOperators());
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
        final Set primitiveOps = group.primitiveOperators();
        final Operator to = group.transformationToPrimitive();
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
        final PeriodicGraph G = new PeriodicGraph(dimension);
        final Map addressToNode = new HashMap();
        final Map addressToShift = new HashMap();
        
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
    
    /**
     * Parses a crystal descriptor and constructs the corresponding atom-bond
     * network.
     * 
     * Example:
     * 
     * <pre>
     * CRYSTAL
     *   GROUP Fd-3m
     *   CELL         2.3094 2.3094 2.3094  90.0 90.0 90.0
     *   ATOM  1  4   5/8 5/8 5/8
     * END
     * </pre>
     * 
     * @param block the pre-parsed input.
     * @return the periodic graph constructed from the input.
     */
    private static PeriodicGraph parseCrystal(final Entry[] block) {
        // TODO make this work for general dimensions
        final Set seen = new HashSet();
        
        String groupName = null;
        int dim = 0;
        SpaceGroup group = null;
        List ops = new ArrayList();
        Matrix cellGram = null;
        
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
                groupName = (String) row.get(0);
                if (Character.isLowerCase(groupName.charAt(0))) {
                    dim = 2;
                } else if (dim == 0) {
                    dim = 3;
                }
                group = new SpaceGroup(dim, groupName);
                ops.addAll(group.getOperators());
                if (ops == null) {
                    final String msg = "Space group not recognized at line ";
                    throw new DataFormatException(msg + block[i].lineNumber);
                }
            } else if (key.equals("cell")) {
                if (seen.contains(key)) {
                    final String msg = "Cell specified twice at line ";
                    throw new DataFormatException(msg + block[i].lineNumber);
                }
                final int m = dim + dim * (dim-1) / 2;
                if (row.size() != m) {
                    final String msg = "Expected " + m + " arguments at line ";
                    throw new DataFormatException(msg + block[i].lineNumber);
                }
                for (int j = 0; j < m; ++j) {
                    if (!(row.get(i) instanceof Real)) {
                        final String msg = "Arguments must be real numbers at line ";
                        throw new DataFormatException(msg + block[i].lineNumber);
                    }
                }
                cellGram = gramMatrix(dim, row);
            } else if (block[i].key.equals("node")) {
                if (row.size() != dim + 2) {
                    final String msg = "Expected " + (dim + 2) + " arguments at line ";
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
                final Matrix position = Matrix.zero(dim+1, dim+1).mutableClone();
                for (int j = 0; j < dim; ++j) {
                    position.set(dim, j, (IArithmetic) row.get(j + 2));
                }
                position.set(dim, dim, Whole.ONE);
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
            System.err.println("Group name: " + groupName);
            System.err.println("  operators:");
            for (final Iterator iter = ops.iterator(); iter.hasNext();) {
                System.err.println("    " + iter.next());
            }
            System.err.println();

            System.err.println("Cell gram matrix = " + cellGram);
            System.err.println();
            
            System.err.println("Nodes:");
            for (final Iterator iter = nodeDescriptors.iterator(); iter.hasNext();) {
                System.err.println("  " + iter.next());
            }
        }
        
        // --- convert to primitive setting
        final Matrix primitiveCell = group.primitiveCell();
        final Set primitiveOps = group.primitiveOperators();
        final Operator to = group.transformationToPrimitive();
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
        final Vector basis[] = Vector.rowVectors(Matrix.one(group.getDimension()));
        final Vector dirichletVectors[] = Vector.dirichletVectors(basis, cellGram);
        
        // --- apply group operators to generate all nodes
        final PeriodicGraph G = new PeriodicGraph(dim);
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
            final Vector shift = dirichletShifts(p, dirichletVectors, cellGram, 1)[0];
            nodeToPosition.put(v, p.plus(shift));
        }
        
        // --- compute nodes in two times extended Dirichlet domain
        final List extended = new ArrayList();
        final Map addressToPosition = new HashMap();
        final Vector zero = Vector.zero(dim);
        for (final Iterator iter = G.nodes(); iter.hasNext();) {
            final INode v = (INode) iter.next();
            final Point pv = (Point) nodeToPosition.get(v);
            if (DEBUG) {
                System.err.println();
                System.err.println("Extending " + v + " at " + pv);
            }
            extended.add(new Pair(v, zero));
            addressToPosition.put(new Pair(v, zero), pv);
            for (int i = 0; i < dirichletVectors.length; ++i) {
                final Vector vec = dirichletVectors[i];
                if (DEBUG) {
                    System.err.println("  shifting by " + vec);
                }
                final Point p = (Point) pv.plus(vec);
                final Vector shifts[] = dirichletShifts(p, dirichletVectors, cellGram, 2);
                if (DEBUG) {
                    System.err
                            .println("    induced " + shifts.length + " further shifts");
                }
                for (int k = 0; k < shifts.length; ++k) {
                    final Vector shift = shifts[k];
                    if (DEBUG) {
                        System.err.println("      added with shift " + shift);
                    }
                    final Pair adr = new Pair(v, vec.plus(shift));
                    extended.add(adr);
                    addressToPosition.put(adr, p.plus(shift));
                }
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
     * @param dim the dimension of the cell.
     * @param cellParameters the list of cell parameters.
     * @return the gram matrix for the vectors.
     */
    private static Matrix gramMatrix(int dim, final List cellParameters) {
        if (dim == 2) {
            final Real a = (Real) cellParameters.get(0);
            final Real b = (Real) cellParameters.get(1);
            final Real angle = (Real) cellParameters.get(2);
            final Real x = (Real) cosine(angle).times(a).times(b);
            
            return new Matrix(new IArithmetic[][] { { a.raisedTo(2), x },
                    { x, b.raisedTo(2) } });
        } else if (dim == 3) {
            final Real a = (Real) cellParameters.get(0);
            final Real b = (Real) cellParameters.get(1);
            final Real c = (Real) cellParameters.get(2);
            final Real alpha = (Real) cellParameters.get(3);
            final Real beta = (Real) cellParameters.get(4);
            final Real gamma = (Real) cellParameters.get(5);
            
            final Real alphaG = (Real) cosine(alpha).times(b).times(c);
            final Real betaG = (Real) cosine(beta).times(a).times(c);
            final Real gammaG = (Real) cosine(gamma).times(a).times(b);

            return new Matrix(
                    new IArithmetic[][] { { a.raisedTo(2), gammaG, betaG },
                            { gammaG, b.raisedTo(2), alphaG },
                            { betaG, alphaG, c.raisedTo(2) }, });
        } else {
            throw new UnsupportedOperationException("supporting only dimensions 2 and 3");
        }
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
     * Returns the vector(s) by which a point has to be shifted in order to
     * obtain a translationally equivalent point within the Dirichlet cell
     * around the origin. All calculations are with respect to the unit lattice
     * and an specific metric, which is passed as one of the arguments. An
     * integral scaling factor can be specified, in which case both the unit
     * lattice and its Dirchlet domain are taken to be scaled by that factor.
     * 
     * If the shifted point is close to a boundary of the Dirichlet domain, more
     * multiple shift vectors may be returned, namely all those that would place
     * the original point close enough to the domain.
     * 
     * @param pos the original point position.
     * @param dirichletVectors normals to the parallel face pairs of the
     *            Dirichlet cell.
     * @param metric the underlying metric.
     * @param factor a scaling factor.
     * @return the shift vector needed to move the point inside.
     */
    private static Vector[] dirichletShifts(final Point pos,
            final Vector dirichletVectors[], final Matrix metric, final int factor) {

        final Whole one = Whole.ONE;
        final int dim = pos.getDimension();
        final Real half = new Fraction(1, 2);
        final Real minusHalf = new Fraction(-1, 2);
        final double eps = 1e-8;
        final Vector posAsVector = (Vector) pos.minus(Point.origin(dim));
        Vector shift = Vector.zero(dim);
        
        // --- compute the first shift
        while (true) {
            boolean changed = false;
            for (int i = 0; i < dirichletVectors.length; ++i) {
                final Vector v = (Vector) dirichletVectors[i].times(factor);
                final IArithmetic c = Vector.dot(v, v, metric);
                final Vector p = (Vector) posAsVector.plus(shift);
                final IArithmetic q = Vector.dot(p, v, metric).dividedBy(c);
                if (q.isGreaterThan(half.plus(eps))) {
                    shift = (Vector) shift.minus(v.times(q.floor().plus(one)));
                    changed = true;
                } else if (q.isLessOrEqual(minusHalf)) {
                    shift = (Vector) shift.minus(v.times(q.floor()));
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
        
        // --- compute further shifts
        final Vector p = (Vector) posAsVector.plus(shift);
        final Set shifts = new HashSet();
        shifts.add(shift);
        
        for (int i = 0; i < dirichletVectors.length; ++i) {
            final Vector v = (Vector) dirichletVectors[i].times(factor);
            final IArithmetic c = Vector.dot(v, v, metric);
            final IArithmetic q = Vector.dot(p, v, metric).dividedBy(c);
            if (q.isGreaterThan(half.minus(2*eps))) {
                shifts.add(shift.minus(v));
            } else if (q.isLessThan(minusHalf.plus(2*eps))) {
                shifts.add(shift.plus(v));
            }
        }

        // --- conver results
        final Vector results[] = new Vector[shifts.size()];
        shifts.toArray(results);
        return results;
    }
}
