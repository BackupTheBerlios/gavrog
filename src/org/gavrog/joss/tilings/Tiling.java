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
import java.util.WeakHashMap;

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
 * @version $Id: Tiling.java,v 1.15 2007/04/25 22:51:30 odf Exp $
 */
public class Tiling {
    final protected static Object TRANSLATION_GROUP = new Tag();
    final protected static Object TRANSLATION_VECTORS = new Tag();
    final protected static Object EDGE_TRANSLATIONS = new Tag();
    final protected static Object CORNER_SHIFTS = new Tag();
    final protected static Object SKELETON = new Tag();
    final protected static Object BARYCENTRIC_SKELETON = new Tag();
    final protected static Object BARYCENTRIC_POS_BY_VERTEX = new Tag();
    final protected static Object SPACEGROUP = new Tag();
    
    // === IMPORTANT: always check if cache.get() returns a non-null value ===
    protected Map cache = new WeakHashMap();

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
        final FundamentalGroup cached = (FundamentalGroup) this.cache
                .get(TRANSLATION_GROUP);
        if (cached != null) {
            return cached;
        } else {
            final FundamentalGroup fg = new FundamentalGroup(getCover());
            this.cache.put(TRANSLATION_GROUP, fg);
            return fg;
        }
    }
    
    /**
     * @return the generators of the translation group as vectors.
     */
    private Vector[] getTranslationVectors() {
        final Vector[] cached = (Vector[]) this.cache.get(TRANSLATION_VECTORS);
        if (cached != null) {
            return cached;
        } else {
            final Matrix N = LinearAlgebra.columnNullSpace(
                    getTranslationGroup().getPresentation().relatorMatrix(),
                    true);
            if (N.numberOfColumns() != getCover().dim()) {
                final String msg = "could not compute translations";
                throw new RuntimeException(msg);
            }
            final Vector[] result = Vector.fromMatrix(N);
            cache.put(TRANSLATION_VECTORS, result);
            return result;
        }
    }
    
    /**
     * @return a mapping of cover-edges to their associated translations
     */
    public Map getEdgeTranslations() {
        final Map cached = (Map) this.cache.get(EDGE_TRANSLATIONS);
        if (cached != null) {
            return cached;
        } else {
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
            
            cache.put(EDGE_TRANSLATIONS, Collections.unmodifiableMap(e2t));
            return e2t;
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
        final Map cached = (Map) this.cache.get(CORNER_SHIFTS);
        if (cached != null) {
            return cached;
        } else {
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
            cache.put(CORNER_SHIFTS, Collections.unmodifiableMap(c2s));
            return c2s;
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
        final Map node2corner = new HashMap();
        final Map corner2node = new HashMap();
        
        /**
         * Constructs an instance.
         * @param dimension
         */
        private Skeleton() {
            super(getCover().dim());
        }
        
        /**
         * Creates a new node associated to a chamber corner.
         * @param i the index of the corner.
         * @param D the chamber the corner belongs to.
         * @return the newly created node.
         */
        private INode newNode(final int i, final Object D) {
            final DelaneySymbol cover = getCover();
            final INode v = super.newNode();
            this.node2corner.put(v, new DSPair(i, D));
            final List idcs = IndexList.except(cover, i);
            for (final Iterator orb = cover.orbit(idcs, D); orb.hasNext();) {
                this.corner2node.put(new DSPair(i, orb.next()), v);
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
            return super.newEdge(v, w, s);
        }
        
        /**
         * Retrieves the chamber corner a node belongs to.
         * @param v the node.
         * @return a DSPair describing the corner associated to node v.
         */
        public DSPair cornerAtNode(final INode v) {
            return (DSPair) this.node2corner.get(v);
        }
        
        /**
         * Retrieves the node associated to a chamber corner.
         * @param i the index for the corner.
         * @param D the chamber to which the corner belongs.
         * @return the node associated to the corner.
         */
        public INode nodeForCorner(final int i, final Object D) {
            return (INode) this.corner2node.get(new DSPair(i, D));
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
		final Skeleton cached = (Skeleton) this.cache.get(SKELETON);
		if (cached != null) {
			return cached;
		}
		
        final DelaneySymbol dsym = getCover();
        final Skeleton G = new Skeleton();
        List idcs;

		// --- create nodes of the graph and map Delaney chambers to nodes
        idcs = IndexList.except(dsym, 0);
        for (Iterator iter = dsym.orbitRepresentatives(idcs); iter.hasNext();) {
            G.newNode(0, iter.next());
        }

        // --- create the edges
        idcs = IndexList.except(dsym, 1);
        for (Iterator iter = dsym.orbitRepresentatives(idcs); iter.hasNext();) {
            final Object D = iter.next();
            final Object E = dsym.op(0, D);
            final INode v = G.nodeForCorner(0, D);
			final INode w = G.nodeForCorner(0, E);
            final Vector t = edgeTranslation(0, D);
            final Vector sD = cornerShift(0, D);
            final Vector sE = cornerShift(0, E);
			final Vector s = (Vector) t.plus(sE).minus(sD);
			G.newEdge(v, w, s, D);
		}
        
		this.cache.put(SKELETON, G);
        return G;
	}

	/**
	 * @return the skeleton graph of the barycentric subdivision.
	 */
	public Skeleton getBarycentricSkeleton() {
		final Skeleton cached = (Skeleton) this.cache.get(BARYCENTRIC_SKELETON);
		if (cached != null) {
			return cached;
		}
		
        final DelaneySymbol dsym = getCover();
		final int dim = dsym.dim();
        final Skeleton G = new Skeleton();

		// --- create nodes and maps to chamber corners to nodes
		for (int i = 0; i <= dim; ++i) {
            final List idcs = IndexList.except(dsym, i);
            for (final Iterator iter = dsym.orbitRepresentatives(idcs); iter
                    .hasNext();) {
                G.newNode(i, iter.next());
            }
        }

		// --- create the edges
		for (int i = 0; i < dim; ++i) {
			for (int j = i + 1; j <= dim; ++j) {
				final List idcs = IndexList.except(dsym, i, j);
				for (final Iterator iter = dsym.orbitRepresentatives(idcs); iter
                        .hasNext();) {
					final Object D = iter.next();
					final INode v = G.nodeForCorner(i, D);
					final INode w = G.nodeForCorner(j, D);
                    final Vector si = cornerShift(i, D);
                    final Vector sj = cornerShift(j, D);
					final Vector s = (Vector) sj.minus(si);
					G.newEdge(v, w, s, D);
				}
			}
		}
        
		this.cache.put(BARYCENTRIC_SKELETON, G);
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
        final Map cached = (Map) cache.get(BARYCENTRIC_POS_BY_VERTEX);
        if (cached != null) {
            return cached;
        } else {
            final Map p = cornerPositions(getSkeleton().barycentricPlacement());
            cache.put(BARYCENTRIC_POS_BY_VERTEX, p);
            return p;
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
            final Point p = (Point) nodePositions.get(skel.nodeForCorner(0, D));
            final Vector t = cornerShift(0, D);
            result.put(new DSPair(0, D), p.plus(t));
        }
        final int dim = cover.dim();
        List idcs = new LinkedList();
        for (int i = 1; i <= dim; ++i) {
            idcs.add(new Integer(i-1));
            for (final Iterator reps = cover.orbitRepresentatives(idcs); reps
                    .hasNext();) {
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
        // --- see if the result is already cached
        final SpaceGroup cached = (SpaceGroup) this.cache.get(SPACEGROUP);
        if (cached != null) {
            return cached;
        }

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
        final INode v = skel.nodeForCorner(0, D0);
        final Matrix Minv = (Matrix) spanningMatrix(D0).inverse();
        for (final Iterator elms = cover.elements(); elms.hasNext();) {
            final Object D = elms.next();
            if (cover.image(D).equals(E)) {
                final Matrix A = (Matrix) Minv.times(spanningMatrix(D));
                final INode w = skel.nodeForCorner(0, D);
                ops.add(new Morphism(v, w, A).getAffineOperator());
            }
        }
        
        // --- construct the group, cache and return it
        final SpaceGroup group = new SpaceGroup(cover.dim(), ops);
        this.cache.put(SPACEGROUP, group);
        return group;
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
