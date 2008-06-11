/*
   Copyright 2008 Olaf Delgado-Friedrichs

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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;
import org.gavrog.joss.dsyms.basic.IndexList;

/**
 * Generates all tile-k-transitive tetrahedra tilings with edge degrees 4, 5 and
 * 6 only.
 * 
 * @author Olaf Delgado
 * @version $Id: FrankKasperExtended.java,v 1.9 2008/04/12 08:07:14 odf Exp $
 */

public class FrankKasperExtended extends TileKTransitive {
	final private static Set interesting_stabilizers = new HashSet();
	final private static Set allowed_stabilizer_sets = new HashSet();
	static {
		interesting_stabilizers.addAll(Arrays.asList(new String[] {
				"*332", "2*2", "222", "2x", "332"
		}));
		allowed_stabilizer_sets.addAll(Arrays.asList(new String[] {
				"", "*332", "*332/*332", "*332/*332/*332", "*332/*332/*332/*332",
				"*332/*332/2*2", "*332/*332/2*2/2*2", "*332/2*2",
				"*332/2*2/2*2", "*332/2*2/222", "*332/2*2/2x", "*332/222",
				"*332/2x", "2*2", "2*2/2*2", "2*2/2*2/2*2", "2*2/2*2/2*2/2*2",
				"2*2/2*2/2*2/2*2/222", "2*2/2*2/2*2/2*2/222/222",
				"2*2/2*2/2*2/222", "2*2/2*2/2*2/222/222", "2*2/2*2/222",
				"2*2/2*2/222/222", "2*2/2*2/222/2x", "2*2/2*2/2x", "2*2/222",
				"2*2/222/222", "2*2/222/2x", "2*2/2x", "222", "222/222",
				"222/222/222", "222/222/222/222", "222/222/222/222/222",
				"222/222/222/222/222/222", "222/222/222/222/222/222/222",
				"222/222/222/222/222/222/222/222", "222/222/222/222/2x",
				"222/222/222/222/2x/2x", "222/222/222/2x", "222/222/222/2x/2x",
				"222/222/222/332", "222/222/2x", "222/222/2x/2x",
				"222/222/332", "222/222/332/332", "222/2x", "222/2x/2x",
				"222/2x/2x/332", "222/2x/332", "222/332", "222/332/332", "2x",
				"2x/2x", "2x/2x/2x", "2x/2x/2x/2x", "2x/2x/332",
				"2x/2x/332/332", "2x/332", "2x/332/332", "332", "332/332",
				"332/332/332", "332/332/332/332"
		}));
	}

	final private boolean testParts;

	public FrankKasperExtended(
			final int k, final boolean verbose, final boolean testParts) {
		super(new DSymbol("1:1,1,1:3,3"), k, verbose);
		this.testParts = testParts;
	}

	protected boolean partsListOkay(final List parts) {
		if (!this.testParts) {
			return true;
		}
		
		final List stabs = new ArrayList();
		
		for (Iterator iter = parts.iterator(); iter.hasNext();) {
            final String type = guessOrbifoldSymbol((DSymbol) iter.next());
            if (interesting_stabilizers.contains(type)) {
            	stabs.add(type);
            }
		}
		Collections.sort(stabs);
		final StringBuffer buf = new StringBuffer(100);
		for (Iterator iter = stabs.iterator(); iter.hasNext();) {
			final String type = (String) iter.next();
			if (buf.length() > 0) {
				buf.append('/');
			}
			buf.append(type);
		}
		return allowed_stabilizer_sets.contains(buf.toString());
	}
	
	private static String guessOrbifoldSymbol(final DSymbol ds) {
		switch (ds.size()) {
		case 1:
			return "*332";
		case 2:
			return "332";
		case 3:
			return "2*2";
		case 4:
			return "*33";
		case 6:
			if (ds.isOriented()) {
				return "222";
			} else if (ds.isLoopless()) {
				return "2x";
			} else {
				return "*22";
			}
		case 8:
			return "33";
		case 12:
			if (ds.isOriented()) {
				return "22";
			} else {
				return "1*";
			}
		case 24:
			return "1";
		default:
			return null;
		}
	}
	
	protected Iterator defineBranching(final DelaneySymbol ds) {
		final DynamicDSymbol out = new DynamicDSymbol(new DSymbol(ds));
		final IndexList idx = new IndexList(0, 2, 3);
		final List choices = new LinkedList();
		for (final Iterator reps = out.orbitReps(idx); reps.hasNext();) {
			final Object D = reps.next();
			final Object D0 = out.op(0, D);
			final int r = out.r(2, 3, D);
			if (r == 4 || r == 5) {
				out.redefineV(2, 3, D, 1);
				out.redefineV(2, 3, D0, 1);
			} else if (r == 3 || r == 6) {
				out.redefineV(2, 3, D, 6 / r);
				out.redefineV(2, 3, D0, 6 / r);
			} else if (r == 1 || r == 2) {
				choices.add(D);
			} else {
				throw new RuntimeException("this should not happen: r = " + r
						+ " at D = " + D);
			}
		}
		
		return new IteratorAdapter() {
			final int n = choices.size();
			int count = 0;
			final Set seen = new HashSet();
			int a[] = null;

			protected Object findNext() throws NoSuchElementException {
				while (true) {
					if (a == null) {
						a = new int[n + 1]; // better not risk null result
						for (int i = 0; i < n; ++i) {
							choose(i, 4);
						}
					} else {
						int i = n - 1;
						while (i >= 0 && a[i] == 6) {
							--i;
						}
						if (i < 0) {
							throw new NoSuchElementException("at end");
						}
						choose(i, 6);
						while (i < n - 1) {
							choose(++i, 4);
						}
					}
					++count;
					final DSymbol res = new DSymbol(out);
					if (res.isLocallyEuclidean3D() && !seen.contains(res)) {
						seen.add(res);
						return res;
					}
				}
			}

			private void choose(final int i, final int m) {
				final Object D = choices.get(i);
				final Object D0 = out.op(0, D);
				final int r = out.r(2, 3, D);
				out.redefineV(2, 3, D, m / r);
				out.redefineV(2, 3, D0, m / r);
				a[i] = m;
			}
		};
	}

	protected Iterator extendTo3d(final DSymbol ds) {
		return new ExtendTo3d(ds) {
			protected List getExtraDeductions(final DelaneySymbol ds,
					final Move move) {
				final List out = new ArrayList();
				final Object D = move.element;
				Object E = D;
				int r = 0;
				List cuts = new ArrayList();
				do {
					E = ds.op(2, E);
					if (ds.definesOp(3, E)) {
						E = ds.op(3, E);
					} else {
						cuts.add(E);
					}
					++r;
				} while (E != D);

				switch (cuts.size()) {
				case 0:
					if (r > 6) {
						return null;
					}
					break;
				case 1:
					if (r > 6) {
						return null;
					} else if (r == 6) {
						final Object A = cuts.get(0);
						out.add(new Move(A, A, -1, -1, false));
					}
					break;
				case 2:
					if (r > 12) {
						return null;
					} else if (r == 12) {
						final Object A = cuts.get(0);
						final Object B = cuts.get(1);
						out.add(new Move(A, B, -1, -1, false));
					}
					break;
				default:
					throw new RuntimeException("this should not happen");
				}

				return out;
			}
		};
	}
}