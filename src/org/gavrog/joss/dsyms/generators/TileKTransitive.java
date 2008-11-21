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

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.gavrog.box.collections.Iterators;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;
import org.gavrog.joss.dsyms.derived.Covers;
import org.gavrog.joss.dsyms.derived.EuclidicityTester;

/**
 * Generates all minimal, locally euclidean, tile-k-transitive tilings by a
 * given combinatorial tile.
 * 
 * @author Olaf Delgado
 * @version $Id: TileKTransitive.java,v 1.9 2008/04/02 11:09:59 odf Exp $
 */
public class TileKTransitive extends ResumableGenerator<DSymbol> {
    private final boolean verbose;

    private final Iterator partLists;
    private Iterator extended;
    private Iterator symbols;

    private int count2dSymbols = 0;
    private int count3dSets = 0;
    private int count3dSymbols = 0;
    private int countMinimal = 0;
    private int checkpoint[] = new int[] { 0, 0, 0 };
    private int resume[] = new int[] { 0, 0, 0 };
    private String resume1 = null;

    /**
     * Constructs an instance.
     * 
     * @param tile the tile to use in the tilings.
     * @param k the number of transitivity classes of tiles aimed for.
     * @param verbose if true, some logging information is produced.
     */
    public TileKTransitive(final DelaneySymbol tile, final int k,
            final boolean verbose) {
        this.verbose = verbose;

        final List covers = Iterators.asList(Covers.allCovers(tile.minimal()));
        this.partLists = Iterators.selections(covers.toArray(), k);

        this.extended = null;
        this.symbols = null;
    }

    /**
     * Sets the point in the search tree at which the algorithm should resume.
     * 
     * @param resume specifies the checkpoint to resume execution at.
     */
    public void setResumePoint(final String spec) {
    	if (spec == null || spec.length() == 0) {
    		return;
    	}
    	final String fields[] = spec.trim().split("-");
    	this.resume[0] = Integer.valueOf(fields[0]);
    	if (fields[1].startsWith("[")) {
    		this.resume1 = fields[1].substring(1, fields[1].length() - 1)
					.replaceAll("\\.", "-");
    	} else {
        	this.resume[1] = Integer.valueOf(fields[2]);
        	this.resume[2] = Integer.valueOf(fields[2]);
    	}
    }

    private boolean tooEarly() {
    	if (checkpoint[0] != resume[0]) {
    		return checkpoint[0] < resume[0];
    	} else if (resume1 == null) {
    		if (checkpoint[1] != resume[1]) {
    			return checkpoint[1] < resume[1];
    		} else {
    			return checkpoint[2] < resume[2];
    		}
    	}
    	return false;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see javaDSym.util.IteratorAdapter#findNext()
     */
    protected DSymbol findNext() throws NoSuchElementException {
        while (true) {
            while (symbols == null || !symbols.hasNext()) {
                while (extended == null || !extended.hasNext()) {
                    if (partLists.hasNext()) {
                        final List tiles = (List) partLists.next();
                        if (!partsListOkay(tiles)) {
                        	continue;
                        }
                        final DynamicDSymbol tmp = new DynamicDSymbol(2);
                        for (final Iterator iter = tiles.iterator(); iter
                                .hasNext();) {
                            tmp.append((DSymbol) iter.next());
                        }
                        ++checkpoint[0];
                        checkpoint[1] = checkpoint[2] = 0;
                        dispatchEvent(new CheckpointEvent(this, tooEarly()));
                        if (tooEarly()) {
                        	continue;
                        }
                        final DSymbol ds = new DSymbol(tmp);
                        ++this.count2dSymbols;
                        if (this.verbose) {
                            System.err.println(setAsString(ds));
                        }
                        extended = extendTo3d(ds);
                        if (extended instanceof ResumableGenerator) {
                        	final ResumableGenerator gen =
                        		(ResumableGenerator) extended;
							gen.addEventLink(CheckpointEvent.class, this,
									"dispatchEvent");
							if (checkpoint[0] == resume[0] && resume1 != null) {
								gen.setResumePoint(resume1);
							}
						}
                    } else {
                        throw new NoSuchElementException("At end");
                    }
                }
                final DSymbol ds = (DSymbol) extended.next();
                ++checkpoint[1];
                checkpoint[2] = 0;
                dispatchEvent(new CheckpointEvent(this, tooEarly()));
                if (tooEarly()) {
                	continue;
                }
                ++this.count3dSets;
                if (this.verbose) {
                    System.err.println("    " + setAsString(ds));
                }
                symbols = defineBranching(ds);
            }
            final DSymbol ds = (DSymbol) symbols.next();
            ++checkpoint[2];
            dispatchEvent(new CheckpointEvent(this, tooEarly()));
            if (tooEarly()) {
            	continue;
            }
            ++count3dSymbols;
            if (this.verbose) {
                System.err.println("        " + branchingAsString(ds));
                System.err.flush();
            }
            if (ds.equals(ds.minimal())) {
                ++countMinimal;
                return new DSymbol(ds.canonical());
            }
        }
    }

    /**
     * Retreives the current checkpoint value as a string.
     * 
     * @return the current checkpoint.
     */
    public String getCheckpoint() {
    	if (extended != null && extended instanceof ResumableGenerator) {
			return String.format("%d-[%s]", checkpoint[0],
					((ResumableGenerator) extended).getCheckpoint().replaceAll(
							"-", "."));
		} else {
			return String.format("%s-%s-%s", checkpoint[0], checkpoint[1],
					checkpoint[2]);
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
        return new DefineBranching3d(ds);
    }

    /**
     * Override this to restrict or change the generation of 3-neighbor
     * relations.
     * 
     * @param ds a Delaney symbol.
     * @return an iterator over all admissible extensions.
     */
    protected Iterator extendTo3d(final DSymbol ds) {
        return new CombineTiles(ds);
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
        int i = 0;
        while (i < args.length && args[i].startsWith("-")) {
            if (args[i].equals("-v")) {
                verbose = !verbose;
            } else if (args[i].equals("-e")) {
                check = !check;
            } else {
                System.err.println("Unknown option '" + args[i] + "'");
            }
            ++i;
        }

        final DSymbol ds = new DSymbol(args[i]);
        final int k = Integer.parseInt(args[i + 1]);
        final TileKTransitive iter = new TileKTransitive(ds, k, verbose);
        int countGood = 0;
        int countAmbiguous = 0;

        try {
        	for (final DSymbol out: iter) {
                if (check) {
                    final EuclidicityTester tester = new EuclidicityTester(out);
                    if (tester.isAmbiguous()) {
                        System.out.println("??? " + out);
                        ++countAmbiguous;
                    } else if (tester.isGood()) {
                        System.out.println(out);
                        ++countGood;
                    }
                } else {
                    System.out.println(out);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }

        System.err.println(iter.statistics());
        if (check) {
            System.err.println("Of the latter, " + countGood
                    + " were found euclidean.");
            if (countAmbiguous > 0) {
                System.err.println("For " + countAmbiguous
                        + " symbols, euclidicity could not yet be decided.");
            }
        }
        System.err.println("Options: " + (check ? "" : "no")
                + " euclidicity check, " + (verbose ? "verbose" : "quiet")
                + ".");
    }
}
