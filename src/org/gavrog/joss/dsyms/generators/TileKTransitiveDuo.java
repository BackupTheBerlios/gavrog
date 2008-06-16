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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.collections.Iterators;
import org.gavrog.box.collections.Pair;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.derived.Covers;
import org.gavrog.joss.dsyms.derived.EuclidicityTester;

/**
 * Generates all minimal, locally euclidean, tile-k-transitive tilings by a
 * given combinatorial tile.
 * 
 * @author Olaf Delgado
 * @version $Id: TileKTransitiveDuo.java,v 1.1 2008/06/16 08:02:50 odf Exp $
 */
public class TileKTransitiveDuo extends IteratorAdapter {
    private final boolean verbose;
    private final boolean simple;

    private final Iterator partLists;
    private Iterator extended;
    private Iterator symbols;

    private int count2dSymbols = 0;
    private int count3dSets = 0;
    private int count3dSymbols = 0;
    private int countMinimal = 0;

    /**
     * Constructs an instance.
     * 
     * @param tile the tile to use in the tilings.
     * @param k the number of transitivity classes of tiles aimed for.
     * @param verbose if true, some logging information is produced.
     */
    public TileKTransitiveDuo(final DelaneySymbol tileA, final int nrA,
			final DelaneySymbol tileB, final int nrB, final boolean simple,
			final boolean verbose) {
        this.verbose = verbose;
        this.simple = simple;

        final List coversA = Iterators.asList(Covers.allCovers(tileA.minimal()));
        final List coversB = Iterators.asList(Covers.allCovers(tileB.minimal()));
        final Iterator listsA = Iterators.selections(coversA.toArray(), nrA);
        final Iterator listsB = Iterators.selections(coversB.toArray(), nrB);
        this.partLists = Iterators.cantorProduct(listsA, listsB);

        this.extended = null;
        this.symbols = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javaDSym.util.IteratorAdapter#findNext()
     */
    protected Object findNext() throws NoSuchElementException {
        while (true) {
            while (symbols == null || !symbols.hasNext()) {
                while (extended == null || !extended.hasNext()) {
                    if (partLists.hasNext()) {
                    	final Pair item = (Pair) partLists.next();
                        final List tiles = new ArrayList();
                        tiles.addAll((List) item.getFirst());
                        tiles.addAll((List) item.getSecond());
                        if (!partsListOkay(tiles)) {
                        	continue;
                        }
                        final DynamicDSymbol tmp = new DynamicDSymbol(2);
                        for (final Iterator iter = tiles.iterator(); iter
                                .hasNext();) {
                            tmp.append((DSymbol) iter.next());
                        }
                        final DSymbol ds = new DSymbol(tmp);
                        ++this.count2dSymbols;
                        if (this.verbose) {
                            System.out.println(setAsString(ds));
                        }
                        extended = extendTo3d(ds);
                    } else {
                        throw new NoSuchElementException("At end");
                    }
                }
                final DSymbol ds = (DSymbol) extended.next();
                ++this.count3dSets;
                if (this.verbose) {
                    System.out.println("    " + setAsString(ds));
                }
                symbols = defineBranching(ds);
            }
            final DSymbol ds = (DSymbol) symbols.next();
            ++count3dSymbols;
            if (this.verbose) {
                System.out.println("        " + branchingAsString(ds));
                System.out.flush();
            }
            if (ds.equals(ds.minimal())) {
                ++countMinimal;
                return new DSymbol(ds.canonical());
            }
        }
    }

    /**
     * Override this to restrict the equivariant tile combinations used.
     * 
     * @param list a list of D-symbols encoding tiles.
     * @return true if this combination should be used.
     */
    protected boolean partsListOkay(final List list) {
        return true;
    }

    /**
     * Override this to restrict or change the generation of branching number
     * combination.
     * 
     * @param ds a Delaney symbol.
     * @return an iterator over all admissible extensions of ds with complete
     *         branching.
     */
    protected Iterator defineBranching(final DelaneySymbol ds) {
		if (this.simple) {
			final DynamicDSymbol out = new DynamicDSymbol(new DSymbol(ds));
			final IndexList idx = new IndexList(2, 3);
			for (final Iterator reps = out.orbitReps(idx); reps.hasNext();) {
				final Object D = reps.next();
				final int r = out.r(2, 3, D);
				if (r == 3) {
					out.redefineV(2, 3, D, 1);
				} else if (r == 1) {
					out.redefineV(2, 3, D, 3);
				} else {
					throw new RuntimeException("this should not happen: r = "
							+ r + " at D = " + D);
				}
			}
			if (out.isLocallyEuclidean3D()) {
				return Iterators.singleton(new DSymbol(out));
			} else {
				return Iterators.empty();
			}
		} else {
			return new DefineBranching3d(ds);
		}
	}

    /**
	 * Override this to restrict or change the generation of 3-neighbor
	 * relations.
	 * 
	 * @param ds
	 *            a Delaney symbol.
	 * @return an iterator over all admissible extensions.
	 */
    protected Iterator extendTo3d(final DSymbol ds) {
		if (this.simple) {
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
						if (r == 2 || r > 3) {
							return null;
						}
						break;
					case 1:
						if (r > 3) {
							return null;
						} else if (r == 3) {
							final Object A = cuts.get(0);
							out.add(new Move(A, A, -1, -1, false));
						}
						break;
					case 2:
						if (r > 6) {
							return null;
						} else if (r == 6) {
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
		} else {
			return new ExtendTo3d(ds);
		}
	}

    public String statistics() {
        return "Constructed " + count2dSymbols + " spherical symbols, "
                + count3dSets + " partial spatial symbols and "
                + count3dSymbols + " complete spatial symbols, of which "
                + countMinimal + " were minimal.";
    }

    private static String setAsString(final DSymbol ds) {
        final String tmp = ds.toString();
        final int i = tmp.lastIndexOf(':');
        return tmp.substring(0, i);
    }

    private static String branchingAsString(final DSymbol ds) {
        final String tmp = ds.toString();
        final int i = tmp.lastIndexOf(':');
        return tmp.substring(i + 1);
    }

    public static void main(final String[] args) {
        boolean verbose = false;
        boolean check = true;
        boolean simple = false;
        int i = 0;
        while (i < args.length && args[i].startsWith("-")) {
            if (args[i].equals("-v")) {
                verbose = !verbose;
            } else if (args[i].equals("-e")) {
                check = !check;
            } else if (args[i].equals("-s")) {
                simple = !simple;
            } else {
                System.out.println("Unknown option '" + args[i] + "'");
                System.exit(1);
            }
            ++i;
        }

        final DSymbol dsA = new DSymbol(args[i]);
        final int nrA = Integer.parseInt(args[i + 1]);
        final DSymbol dsB = new DSymbol(args[i + 2]);
        final int nrB = Integer.parseInt(args[i + 3]);
        final TileKTransitiveDuo iter =
        	new TileKTransitiveDuo(dsA, nrA, dsB, nrB, simple, verbose);
        int countGood = 0;
        int countAmbiguous = 0;

        try {
            while (iter.hasNext()) {
                final DSymbol out = (DSymbol) iter.next();
                if (check) {
                    final EuclidicityTester tester = new EuclidicityTester(out);
                    if (tester.isAmbiguous()) {
                        System.out.println("# ??? " + out);
                        ++countAmbiguous;
                    } else if (tester.isGood()) {
                        System.out.println(out);
                        ++countGood;
                    }
                } else {
                    System.out.println(out);
                }
                System.out.flush();
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }

        System.out.println("# " + iter.statistics());
        if (check) {
            System.out.println("# Of the latter, " + countGood
                    + " were found euclidean.");
            if (countAmbiguous > 0) {
                System.out.println("# For " + countAmbiguous
                        + " symbols, euclidicity could not yet be decided.");
            }
        }
        System.out.println("# Options:");
		System.out.println("#     simple:            "
				+ (simple ? "on" : "off"));
		System.out
				.println("#     euclidicity check: " + (check ? "on" : "off"));
		System.out.println("#     verbose:           "
				+ (verbose ? "on" : "off"));
    }
}
