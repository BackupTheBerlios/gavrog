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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.gavrog.jane.compounds.LinearAlgebra;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.fpgroups.FreeWord;
import org.gavrog.joss.dsyms.basic.DSPair;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.Traversal;
import org.gavrog.joss.dsyms.derived.Covers;
import org.gavrog.joss.dsyms.derived.FundamentalGroup;
import org.gavrog.joss.dsyms.generators.InputIterator;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;
import org.gavrog.joss.pgraphs.io.Output;

/**
 * An instance of this class represents a tiling.
 * 
 * @author Olaf Delgado
 * @version $Id: Tiling.java,v 1.3 2007/04/18 23:01:32 odf Exp $
 */
public class Tiling {
    protected static class CacheKey {
        private static int nextId = 1;
        final private int id;
        
        public CacheKey() {
            this.id = nextId++;
        }
        
        public int hashCode() {
            return id;
        }
        
        public int compareTo(final Object other) {
            if (other instanceof CacheKey) {
                return this.id - ((CacheKey) other).id;
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    private static final CacheKey TRANSLATION_GROUP = new CacheKey();
    private static final CacheKey TRANSLATION_VECTORS = new CacheKey();
    private static final CacheKey EDGE_TRANSLATIONS = new CacheKey();
    
    // === IMPORTANT: always assert non-null return value of a cache.get() ===
    protected Map cache = new WeakHashMap();

    
    final private DelaneySymbol ds;
    final private DelaneySymbol cov;

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
     * @return the toroidal or pseudo-toroidal cover.
     */
    public DelaneySymbol getCover() {
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
    public Vector[] getTranslationVectors() {
        final Vector[] cached = (Vector[]) this.cache.get(TRANSLATION_VECTORS);
        if (cached != null) {
            return cached;
        } else {
            final Matrix N = LinearAlgebra.columnNullSpace(
                    getTranslationGroup().getPresentation().relatorMatrix(),
                    true);
            if (N.numberOfColumns() != this.ds.dim()) {
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
            final int dim = this.ds.dim();
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
            
            cache.put(EDGE_TRANSLATIONS, e2t);
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
    private Vector edgeTranslation(final int i, final Object D) {
        return (Vector) getEdgeTranslations().get(new DSPair(i, D));
    }

	/**
	 * @return the skeleton graph of the tiling.
	 */
	public PeriodicGraph getSkeleton() {
		final int dim = this.cov.dim();
        final PeriodicGraph G = new PeriodicGraph(dim);

		// --- set up index lists
		final List nodeIdcs = new LinkedList();
		final List edgeIdcs = new LinkedList();
		for (int i = 0; i <= dim; ++i) {
			if (i != 0) {
				nodeIdcs.add(new Integer(i));
			}
			if (i != 1) {
				edgeIdcs.add(new Integer(i));
			}
		}

		// --- create nodes of the graph and map Delaney chambers to nodes
		final Map ch2v = new HashMap();
		for (final Iterator iter = this.cov.orbitRepresentatives(nodeIdcs); iter
				.hasNext();) {
			final Object D = iter.next();
			final INode v = G.newNode();
			for (final Iterator orbit = this.cov.orbit(nodeIdcs, D); orbit
                    .hasNext();) {
                ch2v.put(orbit.next(), v);
            }
        }

        // --- map chambers to translations w.r.t. node orbit representatives
        final Traversal trav = new Traversal(this.cov, nodeIdcs, this.cov
                .elements());
        final HashMap trans = new HashMap();
        while (trav.hasNext()) {
			final DSPair e = (DSPair) trav.next();
			int i = e.getIndex();
			final Object D = e.getElement();
			if (i < 0) {
				trans.put(D, Vector.zero(dim));
			} else {
				final Object Di = this.cov.op(i, D);
				final Vector v = (Vector) trans.get(Di);
				trans.put(D, v.plus(edgeTranslation(i, Di)));
			}
		}

		// --- create the edges
		for (final Iterator iter = this.cov.orbitRepresentatives(edgeIdcs); iter
				.hasNext();) {
			final Object D = iter.next();
			final Object D0 = this.cov.op(0, D);
			final INode v = (INode) ch2v.get(D);
			final INode w = (INode) ch2v.get(D0);
			final Vector s = (Vector) edgeTranslation(0, D).plus(trans.get(D0))
					.minus(trans.get(D));
			if (G.getEdge(v, w, s) == null) {
				G.newEdge(v, w, s);
			}
		}
        
        return G;
	}

	/**
	 * @return the skeleton graph of the barycentric subdivision.
	 */
	public PeriodicGraph getBarycentricSkeleton() {
		final int dim = this.cov.dim();
        final PeriodicGraph G = new PeriodicGraph(dim);
		final HashMap trans = new HashMap();

		// --- create nodes and auxiliary info
		final Map corner2node = new HashMap();
		for (int i = 0; i <= dim; ++i) {
			final List idcs = new LinkedList();
			for (int j = 0; j <= dim; ++j) {
				if (j != i) {
					idcs.add(new Integer(j));
				}
			}
			for (Iterator iter = this.cov.orbitRepresentatives(idcs); iter
                    .hasNext();) {
                final Object D = iter.next();
                final INode v = G.newNode();
                for (final Iterator orbit = this.cov.orbit(idcs, D); orbit
                        .hasNext();) {
                    corner2node.put(new DSPair(i, orbit.next()), v);
                }
            }
            // --- map chamber corners to translations
            final Traversal trav = new Traversal(this.cov, idcs, this.cov
                    .elements());
            while (trav.hasNext()) {
				final DSPair e = (DSPair) trav.next();
				final int k = e.getIndex();
				final Object D = e.getElement();
				if (k < 0) {
					trans.put(new DSPair(i, D), Vector.zero(dim));
				} else {
					final Object Dk = this.cov.op(k, D);
					final Vector v = (Vector) trans.get(new DSPair(i, Dk));
					trans.put(new DSPair(i, D), v.plus(edgeTranslation(k, Dk)));
				}
			}

		}

		// --- create the edges
		for (int i = 0; i < dim; ++i) {
			for (int j = i + 1; j <= dim; ++j) {
				final List idcs = new LinkedList();
				for (int k = 0; k <= dim; ++k) {
					if (k != j && k != i) {
						idcs.add(new Integer(k));
					}
				}
				for (Iterator iter = this.cov.orbitRepresentatives(idcs); iter
                        .hasNext();) {
					final Object D = iter.next();
					final DSPair iCorner = new DSPair(i, D);
					final DSPair jCorner = new DSPair(j, D);
					final INode v = (INode) corner2node.get(iCorner);
					final INode w = (INode) corner2node.get(jCorner);
					final Vector s = (Vector) ((Vector) trans.get(jCorner))
							.minus(trans.get(iCorner));
					if (G.getEdge(v, w, s) == null) {
						G.newEdge(v, w, s);
					}
				}
			}
		}
        
        return G;
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
				final PeriodicGraph G = new Tiling(ds).getSkeleton();
				Output.writePGR(out, G, "T" + count);
				out.flush();
			}
		} catch (final IOException ex) {
			ex.printStackTrace();
		}
	}
}
