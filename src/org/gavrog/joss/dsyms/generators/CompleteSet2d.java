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

import java.util.List;

import org.gavrog.box.collections.Iterators;
import org.gavrog.joss.algorithms.BranchAndCut;
import org.gavrog.joss.algorithms.Move;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;
import org.gavrog.joss.dsyms.basic.IndexList;

public class CompleteSet2d extends BranchAndCut {
	// --- the current symbol
	final private DynamicDSymbol current;
	
	private class CMove extends Move {
		final public int idx;
		final public int from;
		final public int to;
		
		public CMove(final int idx, final int from, final int to, final Type type) {
			super(type);
			this.idx = idx;
			this.from = from;
			this.to = to;
		}
		
		public String toString() {
			return super.toString().replaceFirst("<",
					"(" + from + "-" + idx + "->" + to + ")>");
		}
	}
	
	public CompleteSet2d(final DelaneySymbol ds) {
		this.current = new DynamicDSymbol(new DSymbol(ds.canonical()));
	}
	
	protected Move nextChoice(final Move previous) {
		final DynamicDSymbol ds = this.current;
		int i = 0;
		int D = 1;
		if (previous != null) {
			i = ((CMove) previous).idx;
			D = ((CMove) previous).from;
			while (i <= ds.dim()) {
				while (D <= ds.size() && ds.definesOp(i, new Integer(D))) {
					++D;
				}
				++i;
				D = 1;
			}
		}
		if (i > ds.dim()) {
			return null;
		} else {
			return new CMove(i, D, 0, Move.Type.CHOICE);
		}
	}

	protected Move nextDecision(final Move previous) {
		final DynamicDSymbol ds = this.current;
		final int i = ((CMove) previous).idx;
		final int D = ((CMove) previous).from;
		int E = D;
		while (E <= ds.size() && ds.definesOp(i, new Integer(E))) {
			++E;
		}
		if (E > ds.size()) {
			return null;
		} else {
			return new CMove(i, D, E, Move.Type.DECISION);
		}
	}

	protected Status performMove(final Move move) {
		final DynamicDSymbol ds = this.current;
		final int i = ((CMove) move).idx;
		final Integer D = new Integer(((CMove) move).from);
		final Integer E = new Integer(((CMove) move).to);
		
		if (ds.op(i, D).equals(E)) {
			return Status.VOID;
		} else if (ds.definesOp(i, D) || ds.definesOp(i, E)) {
			return Status.ILLEGAL;
		} else {
			ds.redefineOp(i, D, E);
			
			//TODO test 0,2 orbits
			if (i == 0 || i == 2) {
				
			}
			
			return Status.OK;
		}
	}

	protected void undoMove(final Move move) {
		final DynamicDSymbol ds = this.current;
		final int i = ((CMove) move).idx;
		final Integer D = new Integer(((CMove) move).from);
		
		ds.undefineOp(i, D);
	}

	protected List deductions(final Move move) {
		final DynamicDSymbol ds = this.current;
		final int i = ((CMove) move).idx;
		final Integer D = new Integer(((CMove) move).from);
		final Integer E = new Integer(((CMove) move).to);
		
		//TODO finish
		return null;
	}

	protected boolean isWellFormed() {
        final DSymbol flat = new DSymbol(this.current);
        return flat.getMapToCanonical().get(new Integer(1)).equals(new Integer(1));
	}

	protected Object makeResult() {
		final DynamicDSymbol ds = this.current;
		
		// --- check for completeness
		for (int i = 0; i <= ds.dim(); ++i) {
			for (int D = 1; D <= ds.size(); ++D) {
				if (!ds.definesOp(i, new Integer(D))) {
					return null;
				}
			}
		}
		
		// --- return the result as a flat symbol.
		return new DSymbol(this.current);
	}

}
