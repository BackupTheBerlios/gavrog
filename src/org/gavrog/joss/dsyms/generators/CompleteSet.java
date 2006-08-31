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

import java.util.LinkedList;
import java.util.List;

import org.gavrog.box.collections.Pair;
import org.gavrog.joss.algorithms.BranchAndCut;
import org.gavrog.joss.algorithms.Move;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;

public class CompleteSet extends BranchAndCut {
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
			return super.toString().replaceFirst(">",
					"(" + idx + ":" + from + "<->" + to + ")>");
		}
	}
	
	public CompleteSet(final DelaneySymbol ds) {
		this.current = new DynamicDSymbol(new DSymbol(ds.canonical()));
	}
	
	protected Move nextChoice(final Move previous) {
		final DynamicDSymbol ds = this.current;
		int i = 0;
		int D = 1;
		if (previous != null) {
			i = ((CMove) previous).idx;
			D = ((CMove) previous).from + 1;
		}
		while (i <= ds.dim()) {
			while (D <= ds.size() && ds.definesOp(i, new Integer(D))) {
				++D;
			}
			if (D <= ds.size()) {
				break;
			}
			++i;
			D = 1;
		}

		if (i > ds.dim()) {
			return null;
		} else {
			return new CMove(i, D, 0, Move.Type.CHOICE);
		}
	}

	protected Move nextDecision(final Move previous) {
		final CMove move = (CMove) previous;
		final DynamicDSymbol ds = this.current;
		final int i = move.idx;
		final int D = move.from;
		int E = move.to + 1;
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
		
		if (E.equals(ds.op(i, D))) {
			return Status.VOID;
		} else if (ds.definesOp(i, D) || ds.definesOp(i, E)) {
			return Status.ILLEGAL;
		} else {
			ds.redefineOp(i, D, E);
			
			// --- test special orbits
			for (int j = 0; j <= ds.dim(); ++j) {
				if (j < i-1 || j > i+1) {
					Object D1 = D;
					int k = i;
					int n = 0;
					boolean complete = true;
					boolean chain = false;
					do {
						final Object D2 = ds.op(k, D1);
						if (D2 == null) {
							complete = false;
						} else if (D2.equals(D1)){
							chain = true;
						} else {
							D1 = D2;
						}
						k = (i + j) - k;
						++n;
					} while (k != i || !D1.equals(D));
					
					final boolean bad;
					if (complete) {
						bad = (n > 4 || n == 3);
					} else if (chain) {
						bad = (n > 4);
					} else {
						bad = (n > 8);
					}
					if (bad) {
						ds.undefineOp(i, D);
						return Status.ILLEGAL;
					}
				}
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
		
		final List result = new LinkedList();
		
		// --- test special orbits
		for (int j = 0; j <= ds.dim(); ++j) {
			if (j < i-1 || j > i+1) {
				Object D1 = D;
				int k = i;
				int n = 0;
				boolean complete = true;
				boolean chain = false;
				final List ends = new LinkedList();
				do {
					final Object D2 = ds.op(k, D1);
					if (D2 == null) {
						complete = false;
						ends.add(new Pair(new Integer(k), D1));
					} else if (!D2.equals(D1)){
						chain = true;
					} else {
						D1 = D2;
					}
					k = (i + j) - k;
					++n;
				} while (k != i || !D1.equals(D));
				if (!complete) {
					if (chain && n == 4) {
						final Pair end = (Pair) ends.get(0);
						final int nu = ((Integer) end.getFirst()).intValue();
						final int F = ((Integer) end.getSecond()).intValue();
						result.add(new CMove(nu, F, F, Move.Type.DEDUCTION));
					} else if (!chain && n == 8) {
						final Pair end1 = (Pair) ends.get(0);
						final int nu = ((Integer) end1.getFirst()).intValue();
						final int F = ((Integer) end1.getSecond()).intValue();
						final Pair end2 = (Pair) ends.get(1);
						final int G = ((Integer) end2.getSecond()).intValue();
						result.add(new CMove(nu, F, G, Move.Type.DEDUCTION));
					}
				}
			}
		}

		return result;
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

    public static void main(final String[] args) {
        final DSymbol ds = new DSymbol(args[0]);
        final CompleteSet iter = new CompleteSet(ds);

        int countAll = 0;
        int countGood = 0;
        while (iter.hasNext()) {
        	final DSymbol out = (DSymbol) iter.next();
        	++ countAll;
        	out.setVDefaultToOne(true);
        	if (out.curvature2D().isNonNegative()) {
        		++countGood;
        		System.out.println(out);
        	}
        }
        System.out.flush();
        System.out.println("# Generated " + countAll + " sets, of which " + countGood
				+ " are potentially nonhyperbolic.");
    }
}