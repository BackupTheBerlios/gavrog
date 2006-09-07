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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.jane.numbers.Rational;
import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;

/**
 * @author Olaf Delgado
 */
public class Azulenoids extends IteratorAdapter {

	private ExtendTo2d sets;
	private DefineBranching2d syms;
	private int pos;
	private DSymbol ds;
	private Set seenInvariants;

	private static DSymbol template = new DSymbol("1.1:60:"
			+ "2 4 6 8 10 12 14 16 18 20 22 24 26 28 30 32 34 36 38 40 42 44 46 48 "
			+ "50 52 54 56 58 60,"
			+ "6 3 5 12 9 11 18 15 17 24 21 23 30 27 29 36 33 35 42 39 41 48 45 47 "
			+ "54 51 53 60 57 59,"
			+ "0 0 12 11 28 27 0 0 18 17 36 35 24 23 58 57 30 29 0 0 0 0 42 41 "
			+ "0 0 48 47 0 0 54 53 0 0 60 59 0 0:"
			+ "3 3 3 3 3 3 3 3 3 3,0 0 5 0 7 0 0 0 0 0");
	
	/**
	 * Constructs an instance.
	 */
	public Azulenoids() {
		super();
    	final int n = 8;
    	final DynamicDSymbol ds = new DynamicDSymbol(1);
    	ds.grow(2 * n);
    	for (int D = 1; D < 2*n + 1; D += 2) {
    		ds.redefineOp(0, new Integer(D), new Integer(D+1));
    		if (D > 1) {
    			ds.redefineOp(1, new Integer(D), new Integer(D-1));
    		}
    	}
    	ds.redefineOp(1, new Integer(1), new Integer(2*n));
    	ds.redefineV(0, 1, new Integer(1), 1);
    	
        this.sets = new ExtendTo2d(ds);
        this.syms = null;
        this.pos = 0;
        this.seenInvariants = new HashSet();
	}

	/* (non-Javadoc)
	 * @see org.gavrog.box.collections.IteratorAdapter#findNext()
	 */
	protected Object findNext() throws NoSuchElementException {
		while (true) {
			// --- if necessary, find the next octagon tiling to subdivide
			if (this.pos < 1 || this.pos > 16) {
				while (this.syms == null || !this.syms.hasNext()) {
	                if (this.sets.hasNext()) {
	                    final DSymbol ds = (DSymbol) this.sets.next();
	                    ds.setVDefaultToOne(true);
	                    final Rational curv = ds.curvature2D();
	                    ds.setVDefaultToOne(false);
	                    if (curv.isNegative()) {
	                    	continue;
	                    }
	                    this.syms = new DefineBranching2d(ds, 2, Whole.ZERO);
	                } else {
	                    throw new NoSuchElementException("At end");
	                }
				}
				this.ds = (DSymbol) syms.next();
				if (!this.ds.curvature2D().isZero()) {
					continue;
				}
				this.pos = 1;
			}
			final int p = this.pos;
			this.pos += 2;
			
			// --- check if the next subdivision creates no penta- or heptagons
//			boolean good = true;
//			final Integer A = new Integer(p);
//			final Integer B = new Integer(p + 6);
//			final List idcs = new IndexList(1, 2);
//			for (final Iterator reps = this.ds.orbitRepresentatives(idcs); reps.hasNext();) {
//				final Object D = reps.next();
//				if (this.ds.m(1, 2, D) <= 3) {
//					int deg = 0;
//					Object E = D;
//					do {
//						if (E.equals(A) || E.equals(B)) {
//							deg += 3;
//						} else {
//							deg += 2;
//						}
//						E = this.ds.op(2, this.ds.op(1, E));
//					} while (!D.equals(E));
//					if (deg == 5 || deg == 7) {
//						good = false;
//						break;
//					}
//				}
//			}
//			if (!good) {
//				continue
//			}
			
			// --- perform the subdivision if it is legal
			final DynamicDSymbol tmp = new DynamicDSymbol(template);

			// --- map template chambers to octagon chambers
			final Map tmp2oct = new HashMap();
			final Map oct2tmp = new HashMap();
			final Object E0 = new Integer(1);
			Object E = E0;
			int k = (3 - p + 16) % 16 + 1;
			do {
				tmp2oct.put(E, new Integer(k));
				oct2tmp.put(new Integer(k), E);
				E = tmp.op(0, E);
				k = k % 16 + 1;
				tmp2oct.put(E, new Integer(k));
				oct2tmp.put(new Integer(k), E);
				E = tmp.op(1, tmp.op(2, tmp.op(1, E)));
				if (tmp.definesOp(2, E)) {
					E = tmp.op(1, tmp.op(2, E));
				}
				k = k % 16 + 1;
			} while (!E0.equals(E));

			// --- complete the template based on the octagon tiling
			for (final Iterator iter = tmp.elements(); iter.hasNext();) {
				final Object D = iter.next();
				if (!tmp.definesOp(2, D)) {
					tmp.redefineOp(2, D, oct2tmp.get(this.ds.op(2, tmp2oct.get(D))));
				}
			}
			for (final Iterator iter = tmp2oct.keySet().iterator(); iter.hasNext();) {
				final Object D = iter.next();
				if (!tmp.definesV(1, 2, D)) {
					tmp.redefineV(1, 2, D, this.ds.v(1, 2, tmp2oct.get(D)));
				}
			}

			final DSymbol result = new DSymbol(tmp);
			final List key = result.invariant();
			if (!this.seenInvariants.contains(key)) {
				this.seenInvariants.add(key);
				return result.dual().canonical();
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int count = 0;
		for (final Iterator azul = new Azulenoids(); azul.hasNext();) {
			System.out.println(azul.next());
			++count;
		}
		System.out.println("#Generated " + count + " symbols.");
	}
}
