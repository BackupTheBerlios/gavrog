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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.collections.Pair;
import org.gavrog.box.simple.DataFormatException;
import org.gavrog.jane.compounds.LinearAlgebra;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Real;
import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.geometry.Lattices;
import org.gavrog.joss.geometry.Operator;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.SpaceGroup;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.IEdge;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;


/**
 * Contains methods to parse a net specification in Systre format (file extension "cgd").
 * 
 * @author Olaf Delgado
 * @version $Id: NetParser.java,v 1.66 2006/03/19 05:16:29 odf Exp $
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
        public final IArithmetic site;     // the position or site of the node
        public boolean isEdgeCenter; // is this really an edge center?
        
        public NodeDescriptor(final Object name, final int connectivity,
                final IArithmetic site, final boolean isEdgeCenter) {
            this.name = name;
            this.connectivity = connectivity;
            this.site = site;
            this.isEdgeCenter = isEdgeCenter;
        }
        
        public String toString() {
            if (isEdgeCenter) {
                return "EdgeCenter(" + name + ", " + connectivity + ", " + site + ")";
            } else {
                return "Node(" + name + ", " + connectivity + ", " + site + ")";
            }
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
     * Constructs an instance.
     * 
     * @param filename the name of a file read from.
     * @throws FileNotFoundException if no file of that name exists.
     */
    public NetParser(final String filename) throws FileNotFoundException {
        this(new BufferedReader(new FileReader(filename)));
    }
    
    /**
     * Sets up a keyword map to be used by {@link GenericParser#parseDataBlock()}.
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
        result.put("space_group", "group");
        result.put("id", "name");
        result.put("edge_centers", "edge_center");
        result.put("edge_centre", "edge_center");
        result.put("edge_centres", "edge_center");
        result.put("edgecenter", "edge_center");
        result.put("edgecenters", "edge_center");
        result.put("edgecentre", "edge_center");
        result.put("edgecentres", "edge_center");
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
        parseDataBlock();
        final Entry entries[] = getDataEntries();
        if (entries == null) {
            return null;
        }
        final String type = getDataType().toLowerCase();
        if (type.equals("periodic_graph")) {
            return parsePeriodicGraph(entries);
        } else if (type.equals("crystal")) {
            return parseCrystal(entries);
        } else if (type.equals("net")) {
            return parseSymmetricNet(entries);
        } else {
            throw new UnsupportedOperationException("type " + type + " not supported");
        }
    }
    
    /**
     * Retrieves the name of the net last read, if any.
     * 
     * @return everything present under the "name" of "id" key.
     */
    public String getName() {
        return getDataEntriesAsString("name");
    }
    
    /**
     * Retrieves the specgroup given for the net last read, if any.
     * 
     * @return everything present under the "name" of "id" key.
     */
    public String getSpaceGroup() {
        final String group = getDataEntriesAsString("group");
        if (group == null) {
            return "P1";
        } else {
            return group;
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
                final int s[] = new int[d];
                for (int k = 0; k < d; ++k) {
                    s[k] = ((Whole) row.get(k+2)).intValue();
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
                final NodeDescriptor node = new NodeDescriptor(name, -1, position, false);
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
            final Operator site = (Operator) ((Operator) desc.site).times(to);
            nodeDescsTmp.add(new NodeDescriptor(desc.name, desc.connectivity, site,
                    desc.isEdgeCenter));
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
            final Operator site = (Operator) node.site;
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
                final Vector edgeShift = (Vector) trgOp.floorZ().minus(srcOp.floorZ());
                
                final INode v = (INode) addressToNode.get(sourceAddress);
                final INode w = (INode) addressToNode.get(targetAddress);
                final Vector shiftv = (Vector) addressToShift.get(sourceAddress);
                final Vector shiftw = (Vector) addressToShift.get(targetAddress);
                final Vector totalShift = (Vector) edgeShift.plus(shiftw.minus(shiftv));
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
        int dim = 3;
        SpaceGroup group = null;
        List ops = new ArrayList();
        Matrix cellGram = null;
        
        double precision = 0.001;
        double minEdgeLength = 0.1;
        double maxEdgeLength = Double.MAX_VALUE;
        
        final List nodeDescriptors = new LinkedList();
        final Map nameToDesc = new HashMap();
        final List edgeDescriptors = new LinkedList();
        
        // --- collect data from the input
        for (int i = 0; i < block.length; ++i) {
            final List row = block[i].values;
            final String key = block[i].key;
            final int lineNr = block[i].lineNumber;
            if (key.equals("group")) {
                if (seen.contains(key)) {
                    final String msg = "Group specified twice at line ";
                    throw new DataFormatException(msg + lineNr);
                }
                if (row.size() < 1) {
                    final String msg = "Missing argument at line ";
                    throw new DataFormatException(msg + lineNr);
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
                    throw new DataFormatException(msg + lineNr);
                }
            } else if (key.equals("cell")) {
                if (seen.contains(key)) {
                    final String msg = "Cell specified twice at line ";
                    throw new DataFormatException(msg + lineNr);
                }
                final int m = dim + dim * (dim-1) / 2;
                if (row.size() != m) {
                    final String msg = "Expected " + m + " arguments at line ";
                    throw new DataFormatException(msg + lineNr);
                }
                for (int j = 0; j < m; ++j) {
                    if (!(row.get(i) instanceof Real)) {
                        final String msg = "Arguments must be real numbers at line ";
                        throw new DataFormatException(msg + lineNr);
                    }
                }
                cellGram = gramMatrix(dim, row);
            } else if (key.equals("node") || key.equals("edge_center")) {
                if (row.size() != dim + 2) {
                    final String msg = "Expected " + (dim + 2) + " arguments at line ";
                    throw new DataFormatException(msg + lineNr);
                }
                final Object name = row.get(0);
                if (nameToDesc.containsKey(name)) {
                    final String msg = "Node specified twice at line ";
                    throw new DataFormatException(msg + lineNr);
                }
                final Object conn = row.get(1);
                if (!(conn instanceof Whole && ((Whole) conn).isPositive())) {
                    final String msg = "Connectivity must be a positive integer ";
                    throw new DataFormatException(msg + lineNr);
                }
                final IArithmetic pos[] = new IArithmetic[dim];
                for (int j = 0; j < dim; ++j) {
                    pos[j] = (IArithmetic) row.get(j + 2);
                }
                final int c = ((Whole) conn).intValue();
                final boolean isCenter;
                if (key.equals("node")) {
                    isCenter = false;
                } else {
                    if (c != 2) {
                        final String msg = "Edge center connectivity must be 2";
                        throw new DataFormatException(msg + lineNr);
                    }
                    isCenter = true;
                }
                final NodeDescriptor node = new NodeDescriptor(name, c, new Point(pos),
                        isCenter);
                nodeDescriptors.add(node);
                nameToDesc.put(name, node);
            } else if (key.equals("edge")) {
                final Object source;
                final Object target;
                if (row.size() == 2) {
                    // --- two node names given
                    source = row.get(0);
                    target = row.get(1);
                } else if (row.size() == dim + 1) {
                    // --- a node name and a neighbor position
                    source = row.get(0);
                    final double a[] = new double[dim];
                    for (int k = 0; k < dim; ++k) {
                        a[k] = ((Real) row.get(k + 1)).doubleValue();
                    }
                    target = new Point(a);
                } else if (row.size() == 2 * dim) {
                    // --- two node positions
                    final double a[] = new double[dim];
                    for (int k = 0; k < dim; ++k) {
                        a[k] = ((Real) row.get(k)).doubleValue();
                    }
                    source = new Point(a);
                    final double b[] = new double[dim];
                    for (int k = 0; k < dim; ++k) {
                        b[k] = ((Real) row.get(k + dim)).doubleValue();
                    }
                    target = new Point(b);
                } else {
                    final String msg = "Expected 2, " + (dim + 1) + " or " + 2 * dim
                            + " arguments at line";
                    throw new DataFormatException(msg + lineNr);
                }
                final EdgeDescriptor edge = new EdgeDescriptor(source, target, null);
                edgeDescriptors.add(edge);
            } else {
                // TODO store additional entrys
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
            
            System.err.println("Edges:");
            for (final Iterator iter = edgeDescriptors.iterator(); iter.hasNext();) {
                System.err.println("  " + iter.next());
            }
        }
        
        // --- get info for converting to a primitive setting
        final Matrix primitiveCell = group.primitiveCell();
        final Operator to = group.transformationToPrimitive();
        final Operator from = (Operator) to.inverse();
        if (DEBUG) {
            System.err.println();
            System.err.println("Primitive cell: " + primitiveCell);
        }
        
        // --- extract and convert operators
        final Set primitiveOps = group.primitiveOperators();
        ops.clear();
        for (final Iterator iter = primitiveOps.iterator(); iter.hasNext();) {
            final Operator op = (Operator) iter.next();
            ops.add(((Operator) from.times(op).times(to)).modZ());
        }
        
        // --- convert node descriptors
        final List nodeDescsTmp = new LinkedList();
        for (final Iterator iter = nodeDescriptors.iterator(); iter.hasNext();) {
            final NodeDescriptor desc = (NodeDescriptor) iter.next();
            final Point site = (Point) desc.site.times(to);
            final NodeDescriptor newDesc = new NodeDescriptor(desc.name,
                    desc.connectivity, site, desc.isEdgeCenter);
            nodeDescsTmp.add(newDesc);
            nameToDesc.put(desc.name, newDesc);
        }
        nodeDescriptors.clear();
        nodeDescriptors.addAll(nodeDescsTmp);
        
        // --- convert edge descriptors
        final List edgeDescsTmp = new LinkedList();
        for (final Iterator iter = edgeDescriptors.iterator(); iter.hasNext();) {
            final EdgeDescriptor desc = (EdgeDescriptor) iter.next();
            final Object source;
            if (desc.source instanceof Point) {
                source = ((Point) desc.source).times(to);
            } else {
                source = desc.source;
            }
            final Object target;
            if (desc.target instanceof Point) {
                target = ((Point) desc.target).times(to);
            } else {
                target = desc.target;
            }
            edgeDescsTmp.add(new EdgeDescriptor(source, target, desc.shift));
        }
        edgeDescriptors.clear();
        edgeDescriptors.addAll(edgeDescsTmp);
        
        // --- convert gram matrix
        if (cellGram != null) {
            cellGram = ((Matrix) primitiveCell.times(cellGram).times(
                    primitiveCell.transposed())).symmetric();
        }
        
        // --- apply group operators to generate all nodes
        final PeriodicGraph G = new PeriodicGraph(dim);
        final Map nodeToPosition = new HashMap();
        final Map nodeToDescriptorAddress = new HashMap();
        
        for (final Iterator itNodes = nodeDescriptors.iterator(); itNodes.hasNext();) {
            final NodeDescriptor desc = (NodeDescriptor) itNodes.next();
            if (DEBUG) {
                System.err.println();
                System.err.println("Mapping node " + desc);
            }
            final Point site = (Point) desc.site;
            final Set stabilizer = pointStabilizer(site, ops, precision);
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
                    final Point p = (Point) site.times(op);
                    // --- construct a new node
                    final INode v = G.newNode();
                    // --- store some data for it
                    nodeToPosition.put(v, p);
                    nodeToDescriptorAddress.put(v, new Pair(desc, op));
                    for (final Iterator iter = stabilizer.iterator(); iter.hasNext();) {
                        final Operator a = (Operator) ((Operator) iter.next()).times(op);
                        final Operator aModZ = a.modZ();
                        opsSeen.add(aModZ);
                        if (DEBUG) {
                            System.err.println("  marking operator " + aModZ + " as used");
                        }
                    }
                }
            }
        }

        if (DEBUG) {
            System.err.println();
            System.err.println("Generated " + G.numberOfNodes() + " nodes in unit cell.");
        }
        
        // --- handle explicit edges
        final Vector zero = Vector.zero(dim);
        for (final Iterator itEdges = edgeDescriptors.iterator(); itEdges.hasNext();) {
            final EdgeDescriptor desc = (EdgeDescriptor) itEdges.next();
            if (DEBUG) {
                System.err.println();
                System.err.println("Adding edge " + desc);
            }
            final Point sourcePos;
            if (desc.source instanceof Point) {
                sourcePos = (Point) desc.source;
            } else {
                sourcePos = (Point) ((NodeDescriptor) nameToDesc.get(desc.source)).site;
            }
            final Point targetPos;
            if (desc.target instanceof Point) {
                targetPos = (Point) desc.target;
            } else {
                targetPos = (Point) ((NodeDescriptor) nameToDesc.get(desc.target)).site;
            }
            
            // --- loop through the operators to generate all images
            for (final Iterator itOps = ops.iterator(); itOps.hasNext();) {
                // --- get the next coset representative
                final Operator op = ((Operator) itOps.next()).modZ();
                if (DEBUG) {
                    System.err.println("  applying " + op);
                }
                final Point p = (Point) sourcePos.times(op);
                final Point q = (Point) targetPos.times(op);
                final Pair pAdr = lookup(p, nodeToPosition, precision);
                final Pair qAdr = lookup(q, nodeToPosition, precision);
                if (pAdr == null) {
                    throw new RuntimeException("no point at " + p.times(from));
                }
                if (qAdr == null) {
                    throw new RuntimeException("no point at " + q.times(from));
                }
                final INode v = (INode) pAdr.getFirst();
                final INode w = (INode) qAdr.getFirst();
                final Vector vShift = (Vector) pAdr.getSecond();
                final Vector wShift = (Vector) qAdr.getSecond();
                final Vector s = (Vector) wShift.minus(vShift);
                if (G.getEdge(v, w, s) == null) {
                    G.newEdge(v, w, s);
                }
            }
        }
        
        if (DEBUG) {
            System.err.println("Graph after adding explicit edges: " + G);
        }
        
        // --- construct a Dirichlet domain for the translation group
        final Vector basis[] = Vector.rowVectors(Matrix.one(group.getDimension()));
        if (DEBUG) {
            System.err.println("Computing Dirichlet vectors...");
        }
        final Vector dirichletVectors[] = Lattices.dirichletVectors(basis, cellGram);
        if (DEBUG) {
            for (int i = 0; i < dirichletVectors.length; ++i) {
                System.err.println("  " + dirichletVectors[i]);
            }
        }
        
        // --- shift generated nodes into the Dirichlet domain
        for (final Iterator iter = nodeToPosition.keySet().iterator(); iter.hasNext();) {
            final INode v = (INode) iter.next();
            final Point p = (Point) nodeToPosition.get(v);
            // --- shift into Dirichlet domain
            if (DEBUG) {
                System.err.println("Shifting " + p + " into Dirichlet domain...");
            }
            final Vector shift = Lattices.dirichletShifts(p, dirichletVectors, cellGram,
					1)[0];
            if (DEBUG) {
                System.err.println("  shift is " + shift);
            }
            nodeToPosition.put(v, p.plus(shift));
            G.shiftNode(v, shift);
            if (DEBUG) {
                System.err.println("  shifting done");
            }
        }
        
        // --- compute nodes in two times extended Dirichlet domain
        final List extended = new ArrayList();
        final Map addressToPosition = new HashMap();
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
                final Vector shifts[] = Lattices.dirichletShifts(p, dirichletVectors,
						cellGram, 2);
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

            final Pair address = (Pair) nodeToDescriptorAddress.get(v);
            final NodeDescriptor desc = (NodeDescriptor) address.getFirst();
            final int connectivity = desc.connectivity;
            int neighbors = 0;
            
            for (final Iterator it2 = distances.iterator(); it2.hasNext();) {
                if (v.degree() >= connectivity) {
                    if (v.degree() > connectivity) {
                        final String msg = "Too many neighbors found for node ";
                        throw new DataFormatException(msg + v);
                    }
                    break;
                }
                final Pair entry = (Pair) it2.next();
                final double dist = ((Real) entry.getFirst()).doubleValue();
                final int index = ((Integer) entry.getSecond()).intValue();
                final Pair adr = (Pair) extended.get(index);
                final INode w = (INode) adr.getFirst();
                final Vector s = (Vector) adr.getSecond();
                if (dist < minEdgeLength) {
                    final String msg = "Found points closer than minimal edge length of ";
                    throw new DataFormatException(msg + minEdgeLength);
                } else if (dist > maxEdgeLength) {
                    final String msg = "Not enough neighbors found for node ";
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
        
        // --- cleanup phase: remove nodes that are really meant to be edge centers
        final Set bogus = new HashSet();
        for (final Iterator nodes = G.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            final Pair adr = (Pair) nodeToDescriptorAddress.get(v);
            final NodeDescriptor desc = (NodeDescriptor) adr.getFirst();
            if (desc.isEdgeCenter) {
                bogus.add(v);
            }
        }
        for (final Iterator nodes = bogus.iterator(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            final List inc = G.allIncidences(v);
            if (inc.size() != 2) {
                throw new RuntimeException("Edge center has connectivity != 2");
            }
            final IEdge e1 = (IEdge) inc.get(0);
            final Vector s1 = G.getShift(e1);
            final INode w1 = e1.opposite(v);
            final IEdge e2 = (IEdge) inc.get(1);
            final Vector s2 = G.getShift(e2);
            final INode w2 = e2.opposite(v);
            G.newEdge(w1, w2, (Vector) s2.minus(s1));
            G.delete(e1);
            G.delete(e2);
            G.delete(v);
        }
        
        if (DEBUG) {
            System.err.println("--------------------");
        }
        
        return G;
    }
    
    /**
     * Finds the node and shift associated to a point position.
     * @param pos the position to look up.
     * @param nodeToPos maps nodes to positions.
     * @param precision how close must points be to considered equal.
     * 
     * @return the (node, shift) pair found or else null.
     */
    private static Pair lookup(final Point pos, final Map nodeToPos,
            final double precision) {
        final int d = pos.getDimension();
        for (final Iterator iter = nodeToPos.keySet().iterator(); iter.hasNext();) {
            final INode v = (INode) iter.next();
            final Point p = (Point) nodeToPos.get(v);
            if (distModZ(pos, p) <= precision) {
                final Vector diff = (Vector) pos.minus(p);
                final int s[] = new int[d];
                for (int i = 0; i < d; ++i) {
                    final double x = ((Real) diff.get(i)).doubleValue();
                    s[i] = (int) Math.round(x);
                }
                return new Pair(v, new Vector(s));
            }
        }
        return null;
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
    private static Set pointStabilizer(final Point site, final List ops, final double precision) {
        final Set stabilizer = new HashSet();
        
        for (final Iterator it2 = ops.iterator(); it2.hasNext();) {
            final Operator op = (Operator) it2.next();
            final double dist = distModZ(site, (Point) site.times(op));
            if (dist <= precision) { // using "<=" allows for precision 0
                stabilizer.add(op.modZ());
            }
        }
        
        // --- check if stabilizer forms a group
        if (!formGroup(stabilizer)) {
            throw new RuntimeException("precision problem in stabilizer computation");
        }

        return stabilizer;
    }
    
    /**
     * Measures the distance between two sites in terms of the infinity norm of
     * the representing matrices. The distance is computed modulo Z^d, where Z
     * is the dimension of the sites, thus, sites are interpreted as residing in
     * the d-dimensional torus.
     * 
     * Currently only implemented for point sites.
     * 
     * @param site1 first point site.
     * @param site2 second point site.
     * @return the distance.
     */
    private static double distModZ(final Point site1, final Point site2) {
        final int dim = site1.getDimension();
        final Vector diff = (Vector) site1.minus(site2);
        double maxD = 0.0;
        for (int j = 0; j < dim; ++j) {
            final double d = ((Real) diff.get(j).mod(Whole.ONE)).doubleValue();
            maxD = Math.max(maxD, Math.min(d, 1.0 - d));
        }
        return maxD;
    }
    
    /**
     * Determines if the given operators form a group modulo Z^d.
     * @param operators a collection of operators.
     * @return true if the operators form a group.
     */
    final static boolean formGroup(final Collection operators) {
        for (final Iterator iter1 = operators.iterator(); iter1.hasNext();) {
            final Operator A = (Operator) iter1.next();
            for (final Iterator iter2 = operators.iterator(); iter2.hasNext();) {
                final Operator B = (Operator) iter2.next();
                final Operator AB_ = ((Operator) A.times(B.inverse())).modZ();
                if (!operators.contains(AB_)) {
                    return false;
                }
            }
        }
        return true;
    }
}
