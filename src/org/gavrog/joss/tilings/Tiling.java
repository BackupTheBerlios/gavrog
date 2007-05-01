/*
   Copyright 2007 Olaf Delgado-Friedrichs

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

package org.gavrog.joss.tilings;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.gavrog.box.collections.Cache;
import org.gavrog.box.collections.Iterators;
import org.gavrog.box.simple.Tag;
import org.gavrog.jane.compounds.LinearAlgebra;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.fpgroups.FreeWord;
import org.gavrog.joss.dsyms.basic.DSCover;
import org.gavrog.joss.dsyms.basic.DSPair;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.basic.Traversal;
import org.gavrog.joss.dsyms.derived.Covers;
import org.gavrog.joss.dsyms.derived.FundamentalGroup;
import org.gavrog.joss.dsyms.generators.InputIterator;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.SpaceGroup;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.IEdge;
import org.gavrog.joss.pgraphs.basic.IGraphElement;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.Morphism;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;
import org.gavrog.joss.pgraphs.io.Output;

/**
 * An instance of this class represents a tiling.
 * 
 * @author Olaf Delgado
 * @version $Id: Tiling.java,v 1.26 2007/05/01 02:12:20 odf Exp $
 */
public class Tiling {
    // --- the cache keys
    final protected static Object TRANSLATION_GROUP = new Tag();
    final protected static Object TRANSLATION_VECTORS = new Tag();
    final protected static Object EDGE_TRANSLATIONS = new Tag();
    final protected static Object CORNER_SHIFTS = new Tag();
    final protected static Object SKELETON = new Tag();
    final protected static Object DUAL_SKELETON = new Tag();
    final protected static Object BARYCENTRIC_POS_BY_VERTEX = new Tag();
    final protected static Object SPACEGROUP = new Tag();
    final protected static Object FACES = new Tag();
    final protected static Object BODIES = new Tag();
    
    // --- cache for this instance
    final protected Cache cache = new Cache();

    // --- the symbol this tiling is based on and its (pseudo-) toroidal cover
    final protected DelaneySymbol ds;
    final protected DSCover cov;

	/**
	 * Constructs an instance.
	 * 
	 * @param ds the Delaney symbol for the tiling.
	 */
	public Tiling(final DelaneySymbol ds) {
        // --- check basic properties
        if (!ds.isComplete()) {
            throw new IllegalArgumentException("symbol must be complete");
        }
        if (!ds.isConnected()) {
            throw new IllegalArgumentException("symbol must be connected");
        }
        
        // --- remember the input symbol
        this.ds = ds;
        
		// --- shortcut
		final int dim = ds.dim();

		// --- compute a torus cover
		if (dim == 2) {
			this.cov = Covers.toroidalCover2D(ds);
		} else if (dim == 3) {
			this.cov = Covers.pseudoToroidalCover3D(ds);
		} else {
            final String msg = "symbol must be 2- or 3-dimensional";
			throw new UnsupportedOperationException(msg);
		}

		if (this.cov == null) {
			throw new IllegalArgumentException("symbol is not euclidean");
		}
	}

    /**
     * @return the original symbol.
     */
    public DelaneySymbol getSymbol() {
        return this.ds;
    }
    
    /**
     * @return the toroidal or pseudo-toroidal cover.
     */
    public DSCover getCover() {
        return this.cov;
    }
    
    /**
     * @return the fundamental group of the toroidal or pseudo-toroidal cover.
     */
    public FundamentalGroup getTranslationGroup() {
        try {
            return (FundamentalGroup) this.cache.get(TRANSLATION_GROUP);
        } catch (Cache.NotFoundException ex) {
            final FundamentalGroup fg = new FundamentalGroup(getCover());
            return (FundamentalGroup) this.cache.put(TRANSLATION_GROUP, fg);
        }
    }
    
    /**
     * @return the generators of the translation group as vectors.
     */
    private Vector[] getTranslationVectors() {
        try {
            return (Vector[]) this.cache.get(TRANSLATION_VECTORS);
        } catch (Cache.NotFoundException ex) {
            final Matrix N = LinearAlgebra.columnNullSpace(
                    getTranslationGroup().getPresentation().relatorMatrix(),
                    true);
            if (N.numberOfColumns() != getCover().dim()) {
                final String msg = "could not compute translations";
                throw new RuntimeException(msg);
            }
            final Vector[] result = Vector.fromMatrix(N);
            return (Vector[]) this.cache.put(TRANSLATION_VECTORS, result);
        }
    }
    
    /**
     * @return a mapping of cover-edges to their associated translations
     */
    public Map getEdgeTranslations() {
        try {
            return (Map) this.cache.get(EDGE_TRANSLATIONS);
        } catch (Cache.NotFoundException ex) {
            final int dim = getCover().dim();
            final Vector[] t = getTranslationVectors();
            final Map e2w = (Map) getTranslationGroup().getEdgeToWord();
            final Map e2t = new HashMap();
            for (Iterator edges = e2w.keySet().iterator(); edges.hasNext();) {
                final Object e = edges.next();
                final FreeWord w = (FreeWord) e2w.get(e);
                Vector s = Vector.zero(dim);
                for (int i = 0; i < w.length(); ++i) {
                    final int k = w.getLetter(i) - 1;
                    final int sign = w.getSign(i);
                    if (sign > 0) {
                        s = (Vector) s.plus(t[k]);
                    } else {
                        s = (Vector) s.minus(t[k]);
                    }
                }
                e2t.put(e, s);
            }
            return (Map) this.cache.put(EDGE_TRANSLATIONS, Collections
                    .unmodifiableMap(e2t));
        }
    }
    
    /**
     * Determines the translation associated to an edge in the toroidal or
     * pseudo-toroidal cover.
     * 
     * @param i the index of the edge.
     * @param D the source element of the edge.
     * @return the translation vector associated to the edge.
     */
    public Vector edgeTranslation(final int i, final Object D) {
        return (Vector) getEdgeTranslations().get(new DSPair(i, D));
    }

    /**
     * @return shifts to obtain chamber corner positions from node positions.
     */
    public Map getCornerShifts() {
        try {
            return (Map) this.cache.get(CORNER_SHIFTS);
        } catch (Cache.NotFoundException ex) {
            final int dim = getCover().dim();
            final HashMap c2s = new HashMap();
            for (int i = 0; i <= dim; ++i) {
                final List idcs = IndexList.except(getCover(), i);
                final Traversal trav = new Traversal(getCover(), idcs,
                        getCover().elements());
                while (trav.hasNext()) {
                    final DSPair e = (DSPair) trav.next();
                    final int k = e.getIndex();
                    final Object D = e.getElement();
                    if (k < 0) {
                        c2s.put(new DSPair(i, D), Vector.zero(dim));
                    } else {
                        final Object Dk = getCover().op(k, D);
                        final Vector v = (Vector) c2s.get(new DSPair(i, Dk));
                        c2s.put(new DSPair(i, D), v
                                .plus(edgeTranslation(k, Dk)));
                    }
                }

            }
            return (Map) cache.put(CORNER_SHIFTS, Collections
                    .unmodifiableMap(c2s));
        }
    }
    
    /**
     * Returns the necessary shift to obtain the position of a chamber corner
     * from the node position associated to it in the barycentric skeleton.
     * 
     * @param i index of the corner.
     * @param D the chamber the corner belongs to.
     * @return shifts for this corner.
     */
    public Vector cornerShift(final int i, final Object D) {
    	return (Vector) getCornerShifts().get(new DSPair(i, D));
    }
    
    /**
     * Class to represent a skeleton graph for this tiling.
     */
    public class Skeleton extends PeriodicGraph {
        final private Map node2chamber = new HashMap();
        final private Map chamber2node = new HashMap();
        final private Map edge2chamber = new HashMap();
        final private Map chamber2edge = new HashMap();
        final private List nodeIdcs;
        final private List edgeIdcs;
        
        /**
         * Constructs an instance.
         * @param dual if true, constructs a dual skeleton.
         * @param dimension
         */
        private Skeleton(boolean dual) {
            super(getCover().dim());
            final DelaneySymbol cover = getCover();
            final int d = cover.dim();
            if (dual) {
            	nodeIdcs = IndexList.except(cover, d);
            	edgeIdcs = IndexList.except(cover, d-1);
            } else {
            	nodeIdcs = IndexList.except(cover, 0);
            	edgeIdcs = IndexList.except(cover, 1);
            }
        }
        
        /**
         * Creates a new node associated to a chamber corner.
         * @param D the chamber the corner belongs to.
         * @return the newly created node.
         */
        private INode newNode(final Object D) {
            final DelaneySymbol cover = getCover();
            final INode v = super.newNode();
            this.node2chamber.put(v, D);
            for (final Iterator orb = cover.orbit(nodeIdcs, D); orb.hasNext();) {
                this.chamber2node.put(orb.next(), v);
            }
            return v;
        }

        /**
         * Creates a new edge associated to a chamber ridge.
         * @param v source node.
         * @param w target node.
         * @param s shift vector associated to this edge.
         * @param D chamber the ridge belongs to.
         * @return the newly created edge.
         */
        private IEdge newEdge(final INode v, final INode w, final Vector s,
                final Object D) {
            final DelaneySymbol cover = getCover();
            final IEdge e = super.newEdge(v, w, s);
            this.edge2chamber.put(e, D);
            for (final Iterator orb = cover.orbit(edgeIdcs, D); orb.hasNext();) {
                this.chamber2edge.put(orb.next(), e);
            }
            return e;
        }
        
        /**
         * Retrieves the chamber a node belongs to.
         * @param v the node.
         * @return a chamber associated to node v.
         */
        public Object chamberAtNode(final INode v) {
            return this.node2chamber.get(v);
        }
        
        /**
         * Retrieves the node associated to a chamber.
         * @param D the chamber.
         * @return the node associated to the chamber D.
         */
        public INode nodeForChamber(final Object D) {
            return (INode) this.chamber2node.get(D);
        }
        
        /**
         * Retrieves a chamber an edge touches.
         * @param e the edge.
         * @return a chamber associated to edge e.
         */
        public Object chamberAtEdge(final IEdge e) {
            return this.edge2chamber.get(e);
        }
        
        /**
         * Retrieves the edge associated to a chamber.
         * @param D the chamber.
         * @return the edge associated to the chamber D.
         */
        public IEdge edgeForChamber(final Object D) {
            return (IEdge) this.chamber2edge.get(D);
        }
        
        // --- we override the following to make skeleta immutable from outside
        public void delete(IGraphElement element) {
            throw new UnsupportedOperationException();
        }

        public IEdge newEdge(INode source, INode target, int[] shift) {
            throw new UnsupportedOperationException();
        }

        public IEdge newEdge(INode source, INode target, Vector shift) {
            throw new UnsupportedOperationException();
        }

        public IEdge newEdge(INode source, INode target) {
            throw new UnsupportedOperationException();
        }

        public INode newNode() {
            throw new UnsupportedOperationException();
        }

        public void shiftNode(INode node, Vector amount) {
            throw new UnsupportedOperationException();
        }
    }
    
	/**
	 * @return the skeleton graph of the tiling.
	 */
	public Skeleton getSkeleton() {
        try {
            return (Skeleton) this.cache.get(SKELETON);
        } catch (Cache.NotFoundException ex) {
            return (Skeleton) this.cache.put(SKELETON, makeSkeleton(false));
        }
    }

	/**
	 * @return the skeleton graph of the tiling.
	 */
	public Skeleton getDualSkeleton() {
        try {
            return (Skeleton) this.cache.get(DUAL_SKELETON);
        } catch (Cache.NotFoundException ex) {
            return (Skeleton) this.cache.put(DUAL_SKELETON, makeSkeleton(true));
        }
    }

	/**
	 * Constructs the skeleton or dual skeleton of the tiling modulo
	 * translations.
	 * 
	 * @param dual if true, the dual skeleton is constructed.
	 * @return the resulting skeleton graph.
	 */
	private Skeleton makeSkeleton(final boolean dual) {
        final DelaneySymbol cover = getCover();
        final Skeleton G = new Skeleton(dual);
        final int d = cover.dim();
        final int idx0 = dual ? d : 0;
        final int idx1 = dual ? d-1 : 1;
        List idcs;

        // --- create nodes of the graph and map Delaney chambers to nodes
        idcs = IndexList.except(cover, idx0);
        for (final Iterator iter = cover.orbitReps(idcs); iter.hasNext();) {
            G.newNode(iter.next());
        }

        // --- create the edges
        idcs = IndexList.except(cover, idx1);
        for (final Iterator iter = cover.orbitReps(idcs); iter.hasNext();) {
            final Object D = iter.next();
            final Object E = cover.op(idx0, D);
            final INode v = G.nodeForChamber(D);
            final INode w = G.nodeForChamber(E);
            final Vector t = edgeTranslation(idx0, D);
            final Vector sD = cornerShift(idx0, D);
            final Vector sE = cornerShift(idx0, E);
            final Vector s = (Vector) t.plus(sE).minus(sD);
            G.newEdge(v, w, s, D);
        }
        
        return G;
	}
	
    /**
     * Computes positions for chamber corners by first placing corners for
     * vertices (index 0) barycentrically, then placing corners for edges etc.
     * int the centers of their bounding vertices.
     * 
     * @return a mapping from corners to positions
     */
    public Map getVertexBarycentricPositions() {
        try {
            return (Map) this.cache.get(BARYCENTRIC_POS_BY_VERTEX);
        } catch (Cache.NotFoundException ex) {
            final Map p = cornerPositions(getSkeleton().barycentricPlacement());
            return (Map) cache.put(BARYCENTRIC_POS_BY_VERTEX, p);
        }
    }
    
    /**
     * Computes positions for all chamber corners from skeleton node positions.
     * A corner position is taken as the center of gravity of all nodes incident
     * to the component of the tiling associated to that corner.
     * 
     * @param nodePositions maps skeleton nodes to positions.
     * @return a map containing the positions for all corners.
     */
    public Map cornerPositions(final Map nodePositions) {
        final DelaneySymbol cover = getCover();
        final Skeleton skel = getSkeleton();
        final Map result = new HashMap();

        for (final Iterator elms = cover.elements(); elms.hasNext();) {
            final Object D = elms.next();
            final Point p = (Point) nodePositions.get(skel.nodeForChamber(D));
            final Vector t = cornerShift(0, D);
            result.put(new DSPair(0, D), p.plus(t));
        }
        final int dim = cover.dim();
        List idcs = new LinkedList();
        for (int i = 1; i <= dim; ++i) {
            idcs.add(new Integer(i-1));
            for (final Iterator reps = cover.orbitReps(idcs); reps.hasNext();) {
                final Object D = reps.next();
                Matrix s = Point.origin(dim).getCoordinates();
                int n = 0;
                for (Iterator orb = cover.orbit(idcs, D); orb.hasNext();) {
                    final Object E = orb.next();
                    final Point p = (Point) result.get(new DSPair(0, E));
                    final Vector t = cornerShift(i, E);
                    final Point pt = (Point) p.minus(t);
                    s = (Matrix) s.plus(pt.getCoordinates());
                    ++n;
                }
                final Point p = new Point((Matrix) s.dividedBy(n));
                for (Iterator orb = cover.orbit(idcs, D); orb.hasNext();) {
                    final Object E = orb.next();
                    final Vector t = cornerShift(i, E);
                    result.put(new DSPair(i, E), p.plus(t));
                }
            }
        }
        return result;
    }
    
    /**
	 * Returns the position for a corner as computed by
	 * {@link #getVertexBarycentricPositions()}.
	 * 
	 * @param i the index for the corner.
	 * @param D the chamber which the corner belongs to.
	 * @return the position of the corner.
	 */
    public Point vertexBarycentricPosition(final int i, final Object D) {
    	return (Point) getVertexBarycentricPositions().get(new DSPair(i, D));
    }
    
    /**
     * Determines the space group of this tiling.
     * 
     * @return the space group.
     */
    public SpaceGroup getSpaceGroup() {
        try {
            return (SpaceGroup) this.cache.get(SPACEGROUP);
        } catch (Cache.NotFoundException ex) {
            // --- get the toroidal cover of the base symbol
            final DSCover cover = getCover();

            // --- find a chamber with nonzero volume
            Object D0 = null;
            for (final Iterator elms = cover.elements(); elms.hasNext();) {
                final Object D = elms.next();
                if (!spanningMatrix(D).determinant().isZero()) {
                    D0 = D;
                    break;
                }
            }
            if (D0 == null) {
                throw new RuntimeException("all chambers have zero volume");
            }

            // --- compute affine maps from start chamber to its images
            final List ops = new ArrayList();
            final Object E = cover.image(D0);
            final Skeleton skel = getSkeleton();
            final INode v = skel.nodeForChamber(D0);
            final Matrix Minv = (Matrix) spanningMatrix(D0).inverse();
            for (final Iterator elms = cover.elements(); elms.hasNext();) {
                final Object D = elms.next();
                if (cover.image(D).equals(E)) {
                    final Matrix A = (Matrix) Minv.times(spanningMatrix(D));
                    final INode w = skel.nodeForChamber(D);
                    ops.add(new Morphism(v, w, A).getAffineOperator());
                }
            }

            // --- construct the group, cache and return it
            final SpaceGroup group = new SpaceGroup(cover.dim(), ops);
            return (SpaceGroup) this.cache.put(SPACEGROUP, group);
        }
    }
    
    /**
     * Computes a matrix of chamber edge vectors. The i-th row contains the
     * vector from the 0-corner to the (i+1)-corner.
     * @param D a chamber.
     * @return the matrix of edge vectors.
     */
    private Matrix spanningMatrix(final Object D) {
        final int d = getCover().dim();
        final Point p = vertexBarycentricPosition(0, D);
        final Vector dif[] = new Vector[d];
        for (int i = 0; i < d; ++i) {
            dif[i] = (Vector) vertexBarycentricPosition(i + 1, D).minus(p);
        }
        return Vector.toMatrix(dif);
    }
    
    /**
     * Represents a face (2-dimensional constituent) of this tiling.
     */
    public class Face {
    	final private List edges;
    	final private List nodeShifts;
    	final private Object chamber;
        final private int index;
    	
    	private Face(final Object D, final int index) {
    		final DelaneySymbol cover = getCover();
    		final Skeleton skel = getSkeleton();
            final List idcs = IndexList.except(cover, 0, 1);
            this.edges = new LinkedList();
            this.nodeShifts = new LinkedList();
            Object E = D;
            Vector shift = Vector.zero(cover.dim());
            do {
                IEdge e = skel.edgeForChamber(E);
                final Object F = skel.chamberAtEdge(e);
                if (Iterators.contains(cover.orbit(idcs, E), F)) {
                	e = e.reverse();
                }
                this.edges.add(e);
                this.nodeShifts.add(shift);
                shift = (Vector) (shift.plus(skel.getShift(e)));
                E = cover.op(1, cover.op(0, E));
            } while (!E.equals(D));
            this.chamber = D;
            this.index = index;
    	}

    	public int size() {
    		return this.edges.size();
    	}
    	
		public IEdge edge(final int i) {
			return (IEdge) this.edges.get(i);
		}

		public INode node(final int i) {
			return edge(i).source();
		}
		
		public Vector shift(final int i) {
			return (Vector) this.nodeShifts.get(i);
		}
		
		public Object getChamber() {
			return this.chamber;
		}

        public int getIndex() {
            return this.index;
        }
    }
    
    /**
	 * Computes a list of representatives for the translational types of faces
	 * (2-dimensional constituents) of this tiling. A translational type is
	 * defined as the set of all translates of a single tile.
	 * 
	 * @return the list of 2-dimensional constituents for this tiling.
	 */
    public List getFaces() {
        try {
            return (List) this.cache.get(FACES);
        } catch (Cache.NotFoundException ex) {
            final DelaneySymbol cover = getCover();
            final List idcs = IndexList.except(cover, 2);
            final List faces = new ArrayList();
            int i = 0;
            for (final Iterator reps = cover.orbitReps(idcs); reps.hasNext();) {
                faces.add(new Face(reps.next(), i++));
            }
            return (List) this.cache.put(FACES, Collections
					.unmodifiableList(faces));
        }
    }
    
    /**
     * Represents a body (3-dimensional constituent) of this tiling.
     */
    public class Body {
        final private Object chamber;
        final private int index;
        final private int size;
        final private int faces[];
        final private int neighbors[];
        final private Vector faceShifts[];
        final private Vector neighborShifts[];

        private Body(final Object D, final int index, final int size) {
            this.chamber = D;
            this.index = index;
            this.size = size;
            this.faces = new int[size];
            this.neighbors = new int[size];
            this.faceShifts = new Vector[size];
            this.neighborShifts = new Vector[size];
        }

        public Object getChamber() {
            return this.chamber;
        }

        public int getIndex() {
            return this.index;
        }

        public int size() {
            return this.size;
        }
        
        public Face face(final int i) {
            return (Face) getFaces().get(this.faces[i]);
        }
        
        public Vector faceShift(final int i) {
            return this.faceShifts[i];
        }
        
        public Body neighbor(final int i) {
            return (Body) getBodies().get(this.neighbors[i]);
        }
        
        public Vector neighborShift(final int i) {
            return this.neighborShifts[i];
        }
    }
    
    /**
     * @return the list of 3-dimensional constituents for this tiling.
     */
    public List getBodies() {
        try {
            return (List) this.cache.get(BODIES);
        } catch (Cache.NotFoundException ex) {
        }
        
        final DelaneySymbol cover = getCover();
        
        // --- map each chamber to the face it belongs to
        final List faces = getFaces();
        final Map ch2f = new HashMap();
        final List idcsF = IndexList.except(cover, 2);
        for (int i = 0; i < faces.size(); ++i) {
            final Face f = (Face) faces.get(i);
            final Object D = f.getChamber();
            for (final Iterator orb = cover.orbit(idcsF, D); orb.hasNext();) {
                final Object E = orb.next();
                ch2f.put(E, f);
            }
        }

        // --- map chambers to body indices and vice versa
        final List idcs = IndexList.except(cover, 3);
        final Map ch2b = new HashMap();
        int n = 0;
        for (final Iterator reps = cover.orbitReps(idcs); reps.hasNext();) {
            final Object D = reps.next();
            for (final Iterator orb = cover.orbit(idcs, D); orb.hasNext();) {
                ch2b.put(orb.next(), new Integer(n));
            }
            ++n;
        }
        
        // --- construct the list of bodies with associated data
        final Skeleton skel = getDualSkeleton();
        final List bodies = new ArrayList();
        for (final Iterator nodes = skel.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            final Object D = skel.chamberAtNode(v);
            final Integer k = (Integer) ch2b.get(D);
            final Body body = new Body(D, k.intValue(), v.degree());
            int i = 0;
            for (final Iterator conn = v.incidences(); conn.hasNext();) {
                final IEdge e = (IEdge) conn.next();
                Object Df = skel.chamberAtEdge(e);
                if (!k.equals(ch2b.get(Df))) {
                    Df = cover.op(3, Df);
                }
                final Face f = (Face) ch2f.get(Df);
                final Vector t = edgeTranslation(3, Df);
                body.faces[i] = f.getIndex();
                body.faceShifts[i] = (Vector) cornerShift(2, Df).minus(
                        cornerShift(2, f.getChamber())).plus(t);
                final Object Dn = skel.chamberAtNode(e.target());
                body.neighbors[i] = ((Integer) ch2b.get(Dn)).intValue();
                body.neighborShifts[i] = t;
                ++i;
                if (e.source().equals(e.target())) {
                    body.faces[i] = body.faces[i - 1];
                    body.faceShifts[i] = (Vector) cornerShift(2,
                            cover.op(3, Df)).minus(
                            cornerShift(2, f.getChamber())).minus(t);
                    body.neighbors[i] = body.neighbors[i - 1];
                    body.neighborShifts[i] = (Vector) t.negative();
                    ++i;
                }
            }
            bodies.add(body);
        }
        
        // --- cache and return
        return (List) this.cache.put(BODIES, bodies);
    }
    
	/**
     * Main method for testing purposes.
     * 
	 * @param args command line arguments.
	 */
	public static void main(String[] args) {
		try {
			final Reader in;
			final Writer out;
			if (args.length > 0) {
				in = new FileReader(args[0]);
			} else {
				in = new InputStreamReader(System.in);
			}
			if (args.length > 1) {
				out = new FileWriter(args[1]);
			} else {
				out = new OutputStreamWriter(System.out);
			}

			int count = 0;

			for (Iterator input = new InputIterator(in); input.hasNext();) {
				final DSymbol ds = (DSymbol) input.next();
				++count;
				final Skeleton G = new Tiling(ds).getSkeleton();
				Output.writePGR(out, G, "T" + count);
				out.flush();
			}
		} catch (final IOException ex) {
			ex.printStackTrace();
		}
	}
}
