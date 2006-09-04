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


package org.gavrog.joss.dsyms.generators;

import java.util.List;

import org.gavrog.jane.numbers.Rational;
import org.gavrog.joss.algorithms.BranchAndCut;
import org.gavrog.joss.algorithms.Move;
import org.gavrog.joss.algorithms.Move.Type;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;
import org.gavrog.joss.dsyms.derived.Morphism;

/**
 * An iterator that takes a 3-dimensional Delaney symbol with some undefined
 * branching numbers and defines these in all possible combinations such that
 * the results are locally euclidean symbols.
 * 
 * For each isomorphism class of resulting symbols, only one respresentative is
 * produced. The order or naming of elements is not preserved.
 * 
 * @author Olaf Delgado
 * @version $Id: DefineBranching2d.java,v 1.2 2006/09/04 02:25:37 odf Exp $
 *
 */
public class DefineBranching2d extends BranchAndCut {

	private int minEdgeDeg;
	private Rational minCurv;
	private DynamicDSymbol current;
	private List inputAutomorphisms;
	private Rational curvature;

    /**
	 * The instances of this class represent individual moves of setting branch
	 * values. These become the entries of the trial stack.
	 */
	private class BMove extends Move {
		final public int index;
		final public int element;
		final public Integer value;

		public BMove(
				final int index, final int element, final Integer value,
				final Move.Type type) {
			super(type);
			this.index = index;
			this.element = element;
			this.value = value;
		}

		public String toString() {
			return super.toString().replaceFirst(">",
					index + ", " + element + ", " + value + ")");
		}
	}
    
	/**
	 * Constructs a new instance.
	 */
	public DefineBranching2d(
			final DelaneySymbol ds, final int minEdgeDeg, final Rational minCurv) {
		super();
		this.minEdgeDeg = minEdgeDeg;
		this.minCurv = minCurv;
		
		// --- create a canonical dynamic symbol isomorphic to the input one
        this.current = new DynamicDSymbol(new DSymbol(ds.canonical()));
        
        // --- compute the maximal curvature
        this.current.setVDefaultToOne(true);
        this.curvature = this.current.curvature2D();
        this.current.setVDefaultToOne(false);
        
        // --- compute automorphisms for later use in {@link #isValid}.
        this.inputAutomorphisms = Morphism.automorphisms(this.current);
	}

	/* (non-Javadoc)
	 * @see org.gavrog.joss.algorithms.BranchAndCut#nextChoice(org.gavrog.joss.algorithms.Move)
	 */
	protected Move nextChoice(Move previous) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gavrog.joss.algorithms.BranchAndCut#nextDecision(org.gavrog.joss.algorithms.Move)
	 */
	protected Move nextDecision(Move previous) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gavrog.joss.algorithms.BranchAndCut#performMove(org.gavrog.joss.algorithms.Move)
	 */
	protected Status performMove(Move move) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gavrog.joss.algorithms.BranchAndCut#undoMove(org.gavrog.joss.algorithms.Move)
	 */
	protected void undoMove(Move move) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.gavrog.joss.algorithms.BranchAndCut#deductions(org.gavrog.joss.algorithms.Move)
	 */
	protected List deductions(Move move) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gavrog.joss.algorithms.BranchAndCut#isValid()
	 */
	protected boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.gavrog.joss.algorithms.BranchAndCut#makeResult()
	 */
	protected Object makeResult() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
