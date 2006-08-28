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

import java.util.Iterator;
import java.util.List;


/**
 * Generates all permutation of a given (small) degree. A first sample
 * application for the abstract {@link Generator} class. Does not use deductions
 * or canonicity, so is much too simplistic for a real test.
 * 
 * @author Olaf Delgado
 * @version $Id: Permutations.java,v 1.3 2006/08/28 19:37:53 odf Exp $
 */
public class Permutations extends Generator {
	final int degree;
	final int map[];
	final int inv[];

	private class PMove extends Move {
		public int from;
		public int to;
		
		public PMove(final int from, final int to, final Type type) {
			super(type);
			this.from = from;
			this.to = to;
		}
		
		public String toString() {
			return super.toString().replaceFirst("<", "(" + from + "->" + to + ")>");
		}
	}
	
	public Permutations(final int degree) {
		if (degree < 1 || degree > 9) {
			throw new IllegalArgumentException("degree must be between 1 and 9");
		}
		this.degree = degree;
		this.map = new int[degree+1];
		this.inv = new int[degree+1];
	}
	
	protected Move nextChoice(final Move previous) {
		int n = 1;
		if (previous != null) {
			n = ((PMove) previous).from;
			while (n <= this.degree && this.map[n] != 0) {
				++n;
			}
		}
		if (n > this.degree) {
			return null;
		} else {
			return new PMove(n, 0, Move.Type.CHOICE);
		}
	}

	protected Move nextDecision(final Move previous) {
		final int from = ((PMove) previous).from;
		int to = ((PMove) previous).to + 1;
		while (to <= this.degree && this.inv[to] != 0) {
			++to;
		}
		if (to > this.degree) {
			return null;
		} else {
			return new PMove(from, to, Move.Type.DECISION);
		}
	}

	protected Status performMove(final Move move) {
		final int from = ((PMove) move).from;
		final int to = ((PMove) move).to;
		if (map[from] == to) {
			return Status.VOID;
		} else if (map[from] != 0 || inv[to] != 0) {
			return Status.ILLEGAL;
		} else {
			this.map[from] = to;
			this.inv[to] = from;
			return Status.OK;
		}
	}

	protected void undoMove(final Move move) {
		final int from = ((PMove) move).from;
		final int to = ((PMove) move).to;
		map[from] = 0;
		inv[to] = 0;
	}

	protected List deductions(final Move move) {
		return null;
	}

	protected boolean isCanonical() {
		return true;
	}

	protected Object makeResult() {
		final StringBuffer tmp = new StringBuffer(10);
		for (int i = 1; i <= this.degree; ++i) {
			final int k = this.map[i];
			if (k == 0) {
				return null;
			} else {
				tmp.append(k);
			}
		}
		return tmp.toString();
	}
	
	public static void main(final String args[]) {
		final int n;
		if (args.length == 0) {
			n = 4;
		} else {
			n = Integer.parseInt(args[0]);
		}
		final Iterator gen = new Permutations(n);
		int count = 0;
		while (gen.hasNext()) {
			System.out.println(gen.next());
			++count;
		}
		System.err.println("Generated " + count + " permutations.");
	}
}
