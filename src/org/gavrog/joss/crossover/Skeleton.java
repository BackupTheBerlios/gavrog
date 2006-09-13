/*
   Copyright 2006 Olaf Delgado-Friedrichs

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

package org.gavrog.joss.crossover;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.gavrog.box.collections.Pair;
import org.gavrog.jane.compounds.LinearAlgebra;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.fpgroups.FpGroup;
import org.gavrog.jane.fpgroups.FreeWord;
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
 * Represents a periodic graph derived as the 1-skeleton of a tiling.
 * 
 * @author Olaf Delgado
 * @version $Id: Skeleton.java,v 1.4 2006/09/13 22:41:55 odf Exp $
 */
public class Skeleton extends PeriodicGraph {
    final private Map edgeToWord;
    final private Vector L[];
    
	/**
	 * Constructs an instance as the skeleton of a given tiling.
	 * 
	 * @param ds the Delaney symbol of the source tiling.
	 */
	public Skeleton(final DelaneySymbol ds) {
		super(ds.dim());
		
		// --- shortcut
		final int dim = ds.dim();
		
		// --- compute a torus cover
		final DelaneySymbol cov;
		if (dim == 2) {
			cov = Covers.toroidalCover2D(ds);
		} else if (dim == 3) {
			cov = Covers.pseudoToroidalCover3D(ds);
		} else {
			throw new UnsupportedOperationException("only dimensions 2 and 3 work");
		}
		
		if (cov == null) {
			throw new IllegalArgumentException("symbol is not euclidean");
		}
		
		// --- compute the fundamental group and find a translation representation for it
        final FundamentalGroup FG = new FundamentalGroup(cov);
        this.edgeToWord = FG.getEdgeToWord();
        final FpGroup G = FG.getPresentation();
        final Matrix N = LinearAlgebra.columnNullSpace(G.relatorMatrix(), false);
        if (N.numberOfColumns() != this.dimension) {
        	final String msg = "problem computing translations - probably a bug";
        	throw new RuntimeException(msg);
        }
        // --- extract the translation vectors
        this.L = Vector.fromMatrix(N);
        
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
        
        // --- create nodes of the graph and maps between nodes and Delaney chambers
        final Map ch2v = new HashMap();
        for (final Iterator iter = cov.orbitRepresentatives(nodeIdcs); iter.hasNext();) {
        	final Object D = iter.next();
        	final INode v = newNode();
        	for (final Iterator orbit = cov.orbit(nodeIdcs, D); orbit.hasNext();) {
        		ch2v.put(orbit.next(), v);
        	}
        }
        
        // --- map chambers to translations w.r.t. node orbit representatives
        Traversal trav = new Traversal(cov, nodeIdcs, cov.orbitRepresentatives(nodeIdcs));
        HashMap trans = new HashMap();
        while (trav.hasNext()) {
            final Pair e = (Pair) trav.next();
            int i = ((Integer) e.getFirst()).intValue();
            final Object D = e.getSecond();
            if (i < 0) {
                trans.put(D, Vector.zero(dim));
            } else {
                final Object Di = cov.op(i, D);
                final Vector v = (Vector) trans.get(Di);
                trans.put(D, v.plus(edgeTranslation(i, Di)));
            }
        }
        
        // --- create the edges
        for (final Iterator iter = cov.orbitRepresentatives(edgeIdcs); iter.hasNext();) {
        	final Object D = iter.next();
            final Object D0 = cov.op(0, D);
        	final INode v = (INode) ch2v.get(D);
        	final INode w = (INode) ch2v.get(D0);
        	final Vector s = (Vector) edgeTranslation(0, D).plus(trans.get(D0)).minus(
                    trans.get(D));
        	if (getEdge(v, w, s) == null) {
        		newEdge(v, w, s);
        	}
        }
	}
    
    /**
     * Helper method for computing the translation associated to an edge in the covering
     * Delaney symbol.
     * 
     * @param idx the index of the edge.
     * @param D the source element of the edge.
     * @return the translation vector associated to the edge.
     */
    private Vector edgeTranslation(final int idx, final Object D) {
        final FreeWord word = (FreeWord) edgeToWord.get(new Pair(new Integer(idx), D));
        Vector s = Vector.zero(getDimension());
        for (int i = 0; i < word.length(); ++i) {
            final int k = word.getLetter(i) - 1;
            final int sign = word.getSign(i);
            if (sign > 0) {
                s = (Vector) s.plus(L[k]);
            } else {
                s = (Vector) s.minus(L[k]);
            }
        }
        return s;
    }
    
    public static void main(String[] args) {
		try {
			final String infilename = args[0];
			final Writer out = new FileWriter(args[1]);

			int count = 0;

			for (final Iterator input = new InputIterator(infilename); input.hasNext();) {
				final DSymbol ds = (DSymbol) input.next();
				++count;
				final PeriodicGraph G = new Skeleton(ds);
				Output.writePGR(out, G, "T" + count);
				out.flush();
			}
		} catch (final IOException ex) {
			ex.printStackTrace();
		}
	}
}
