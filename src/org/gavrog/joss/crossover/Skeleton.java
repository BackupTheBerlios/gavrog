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
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.derived.Covers;
import org.gavrog.joss.dsyms.derived.FundamentalGroup;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;

/**
 * Represents a periodic graph derived as the 1-skeleton of a tiling.
 * 
 * @author Olaf Delgado
 * @version $Id: Skeleton.java,v 1.1 2006/04/24 02:50:28 odf Exp $
 */
public class Skeleton extends PeriodicGraph {
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
        final FpGroup G = FG.getPresentation();
        final Matrix N = LinearAlgebra.columnNullSpace(G.relatorMatrix(), false);
        if (N.numberOfColumns() != this.dimension) {
        	final String msg = "problem computing translations - probably a bug";
        	throw new RuntimeException(msg);
        }
        // --- extract the translation vectors
        final Vector L[] = Vector.fromMatrix(N);
        
        // --- set up index lists
        final List nodeIdcs = new LinkedList();
        final List edgeIdcs = new LinkedList();
        for (int i = 0; i < dim; ++i) {
        	if (i != 0) {
        		nodeIdcs.add(new Integer(i));
        	}
        	if (i != 1) {
        		edgeIdcs.add(new Integer(i));
        	}
        }
        
        // --- create nodes of the graph and maps between nodes and Delaney chambers
        final Map v2ch = new HashMap();
        final Map ch2v = new HashMap();
        for (final Iterator iter = cov.orbitRepresentatives(nodeIdcs); iter.hasNext();) {
        	final Object D = iter.next();
        	final INode v = newNode();
        	v2ch.put(v, D);
        	for (final Iterator orbit = cov.orbit(nodeIdcs, D); orbit.hasNext();) {
        		ch2v.put(orbit.next(), v);
        	}
        }
        
        // --- create the edges
        final Map e2w = FG.getEdgeToWord();
        for (final Iterator iter = cov.orbitRepresentatives(edgeIdcs); iter.hasNext();) {
        	final Object D = iter.next();
        	final INode v = (INode) ch2v.get(D);
        	final INode w = (INode) ch2v.get(cov.op(0, D));
        	final FreeWord word = (FreeWord) e2w.get(new Pair(new Integer(0), D));
        	Vector s = Vector.zero(dim);
        	for (int i = 0; i < word.length(); ++i) {
        		final int k = word.getLetter(i) - 1;
        		final int sign = word.getSign(i);
        		if (sign > 0) {
        			s = (Vector) s.plus(L[k]);
        		} else {
        			s = (Vector) s.minus(L[k]);
        		}
        	}
        	if (getEdge(v, w, s) == null) {
        		newEdge(v, w, s);
        	}
        }
	}
}
