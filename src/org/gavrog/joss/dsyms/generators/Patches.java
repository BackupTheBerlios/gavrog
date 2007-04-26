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


package org.gavrog.joss.dsyms.generators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.gavrog.joss.algorithms.BranchAndCut;
import org.gavrog.joss.algorithms.Move;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.IndexList;

/**
 * Takes a connected 2-dimensional Delaney symbol and generates all patches of
 * the corresponding tiling that respect its symmetry. A patch is a collection
 * of tiles the union of which is homeomorphic to a disk. A patch respects the
 * symmetry if its image by any symmetry operation is either the patch itself of
 * a disjoint set of tiles. Patches are returned as sets of elements of the
 * original symbol.
 * 
 * TODO this implementation is incorrect
 * 
 * @author Olaf Delgado
 * @version $Id: Patches.java,v 1.3 2007/04/26 20:21:58 odf Exp $
 */
public class Patches extends BranchAndCut {
	static final private List idcs = new IndexList(0, 1);

	// --- the input symbol
	final private DelaneySymbol ds;
	// --- seeds for singleton patches
	final private LinkedList seeds;
	// --- the current patch
	final private Set current;
	// --- all patches produced so far
	final private Set seen;
	
	private class PMove extends Move {
		final public Object last;
		final public Object current;
		
		public PMove(final Object last, final Object current, final Type type) {
			super(type);
			this.last = last;
			this.current = current;
		}
		
		public String toString() {
			return super.toString().replaceFirst(">", "(" + last + "," + current + ")>");
		}
	}
	
    /**
     * Creates a new instance.
     * 
     * @param ds the symbol encoding the original tiling.
     */
    public Patches(final DelaneySymbol ds) {
        // --- check the argument
        if (ds == null) {
            throw new IllegalArgumentException("null argument");
        }
        if (ds.dim() != 2) {
            throw new IllegalArgumentException("dimension must be 2");
        }
        try {
            ds.size();
        } catch (UnsupportedOperationException ex) {
            throw new UnsupportedOperationException("symbol must be finite");
        }
        if (!ds.isConnected()) {
        	throw new UnsupportedOperationException("symbol must be connected");
        }
        this.ds = ds;
        this.current = new HashSet();
        this.seen = new HashSet();
        
        this.seeds = new LinkedList();
        for (final Iterator reps = this.ds.orbitReps(idcs); reps.hasNext();) {
        	this.seeds.add(reps.next());
        }
    }

	/* (non-Javadoc)
	 * @see org.gavrog.joss.algorithms.BranchAndCut#checkMove(org.gavrog.joss.algorithms.Move)
	 */
	protected Status checkMove(final Move move) {
		//TODO does new tile meet old patch and mirrors along a connected arc?
		final Object D = ((PMove) move).current;
		if (this.current.contains(D)) {
			return Status.VOID;
		} else {
			return Status.OK;
		}
	}

	/* (non-Javadoc)
	 * @see org.gavrog.joss.algorithms.BranchAndCut#deductions(org.gavrog.joss.algorithms.Move)
	 */
	protected List deductions(final Move move) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gavrog.joss.algorithms.BranchAndCut#isComplete()
	 */
	protected boolean isComplete() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.gavrog.joss.algorithms.BranchAndCut#isValid()
	 */
	protected boolean isValid() {
		final boolean valid = !this.seen.contains(this.current);
		this.seen.add(makeResult());
		return valid;
	}

	/* (non-Javadoc)
	 * @see org.gavrog.joss.algorithms.BranchAndCut#makeResult()
	 */
	protected Object makeResult() {
		final Set result = new HashSet();
		result.addAll(this.current);
		return result;
	}

	/* (non-Javadoc)
	 * @see org.gavrog.joss.algorithms.BranchAndCut#nextChoice(org.gavrog.joss.algorithms.Move)
	 */
	protected Move nextChoice(Move previous) {
		if (this.current.size() == 0) {
			return new PMove(null, null, Move.Type.CHOICE);
		} else {
			for (final Iterator patch = this.current.iterator(); patch.hasNext();) {
				final Object D = patch.next();
				final Object D2 = this.ds.op(2, D);
				if (!this.current.contains(D2)) {
					return new PMove(D2, D2, Move.Type.CHOICE);
				}
			}
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see org.gavrog.joss.algorithms.BranchAndCut#nextDecision(org.gavrog.joss.algorithms.Move)
	 */
	protected Move nextDecision(final Move previous) {
		final Object E = ((PMove) previous).last;
		if (E == null) {
			if (this.seeds.size() == 0) {
				return null;
			} else {
				return new PMove(null, this.seeds.removeFirst(), Move.Type.DECISION);
			}
		} else {
			Object D = ((PMove) previous).current;
			if (D.equals(E) && previous.isDecision()) {
				return null;
			}
			final DelaneySymbol ds = this.ds;
			D = ds.op(1, ds.op(0, D));
			while (!this.current.contains(ds.op(2, D))) {
				D = ds.op(1, ds.op(2, D));
			}
			return new PMove(E, D, Move.Type.DECISION);
		}
	}

	/* (non-Javadoc)
	 * @see org.gavrog.joss.algorithms.BranchAndCut#performMove(org.gavrog.joss.algorithms.Move)
	 */
	protected void performMove(final Move move) {
		final Object D = ((PMove) move).current;
		for (final Iterator orb = this.ds.orbit(idcs, D); orb.hasNext();) {
			this.current.add(orb.next());
		}
	}

	/* (non-Javadoc)
	 * @see org.gavrog.joss.algorithms.BranchAndCut#undoMove(org.gavrog.joss.algorithms.Move)
	 */
	protected void undoMove(Move move) {
		final Object D = ((PMove) move).current;
		for (final Iterator orb = this.ds.orbit(idcs, D); orb.hasNext();) {
			this.current.remove(orb.next());
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
        final DSymbol ds = new DSymbol(args[0]);
        final Patches iter = new Patches(ds);
        int count = 0;

        try {
            while (iter.hasNext()) {
                final Set out = (Set) iter.next();
                final List elms = new ArrayList();
                elms.addAll(out);
                Collections.sort(elms);
                System.out.println(elms);
                ++count;
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        
        System.out.println("### Generated " + count + " patches.");
	}
}
