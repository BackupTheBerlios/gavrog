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
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.collections.Iterators;
import org.gavrog.jane.numbers.Fraction;
import org.gavrog.jane.numbers.Rational;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;
import org.gavrog.joss.dsyms.derived.EuclidicityTester;

/**
 * @author Olaf Delgado
 * @version $Id: FaceTransitive.java,v 1.3 2006/11/08 06:15:49 odf Exp $
 */
public class FaceTransitive {

    /**
     * Generates all feasible 1-dimensional symbols of a given size.
     */
    public static class Faces extends IteratorAdapter {
        final Iterator sets;
        int v;
        DynamicDSymbol currentSet = null;
        
        public Faces(final int size) {
            if (size % 2 == 1) {
                final DynamicDSymbol ds = new DynamicDSymbol(1);
                final List elms = ds.grow(size);
                for (int i = 1; i < size; i += 2) {
                    ds.redefineOp(0, elms.get(i-1), elms.get(i));
                    ds.redefineOp(1, elms.get(i), elms.get(i+1));
                }
                ds.redefineOp(1, elms.get(0), elms.get(0));
                ds.redefineOp(0, elms.get(size-1), elms.get(size-1));
                this.sets = Iterators.singleton(new DSymbol(ds));
            } else {
                final List tmp = new LinkedList();
                final DynamicDSymbol ds = new DynamicDSymbol(1);
                final List elms = ds.grow(size);
                for (int i = 1; i < size; i += 2) {
                    ds.redefineOp(0, elms.get(i-1), elms.get(i));
                    ds.redefineOp(1, elms.get(i), elms.get((i+1) % size));
                }
                tmp.add(new DSymbol(ds));

                ds.redefineOp(1, elms.get(0), elms.get(0));
                ds.redefineOp(1, elms.get(size-1), elms.get(size-1));
                tmp.add(new DSymbol(ds));

                for (int i = 1; i < size; i += 2) {
                    ds.redefineOp(1, elms.get(i-1), elms.get(i));
                    ds.redefineOp(0, elms.get(i), elms.get((i+1) % size));
                }
                ds.redefineOp(0, elms.get(0), elms.get(0));
                ds.redefineOp(0, elms.get(size-1), elms.get(size-1));
                tmp.add(new DSymbol(ds));

                this.sets = tmp.iterator();
            }
        }
        
        /* (non-Javadoc)
         * @see org.gavrog.box.collections.IteratorAdapter#findNext()
         */
        protected Object findNext() throws NoSuchElementException {
            while (true) {
                if (this.currentSet != null && this.v <= 4) {
                    final DynamicDSymbol ds = this.currentSet;
                    final Object D = ds.elements().next();
                    ds.redefineV(0, 1, D, this.v);
                    ++this.v;
                    if (ds.m(0, 1, D) >= 3) {
                        return new DSymbol(ds);
                    }
                } else if (this.sets.hasNext()) {
                    final DSymbol ds = (DSymbol) this.sets.next();
                    this.currentSet = new DynamicDSymbol(ds);
                    this.v = 1;
                } else {
                    throw new NoSuchElementException("at end");
                }
            }
        }
    }
    
    /**
     * Generates all feasible 2-dimensional symbols made from a given
     * 1-dimensional one.
     */
    public static class SingleFaceTiles extends IteratorAdapter {
        final private static Rational minCurv = new Fraction(1, 12);
        Iterator preTiles = Iterators.empty();
        Iterator tiles = Iterators.empty();
        
        public SingleFaceTiles(final DSymbol face) {
            this.preTiles = new CombineTiles(face);
        }

        /* (non-Javadoc)
         * @see org.gavrog.box.collections.IteratorAdapter#findNext()
         */
        protected Object findNext() throws NoSuchElementException {
            while (true) {
                if (this.tiles.hasNext()) {
                    final DSymbol ds = (DSymbol) this.tiles.next();
                    if (!ds.isSpherical2D()) {
                        continue;
                    }
                    for (final Iterator elms = ds.elements(); elms.hasNext();) {
                        if (ds.m(1, 2, elms.next()) > 2) {
                            return ds;
                        }
                    }
                } else if (this.preTiles.hasNext()) {
                    final DSymbol ds = (DSymbol) this.preTiles.next();
                    this.tiles = new DefineBranching2d(ds, 3, 2, minCurv);
                } else {
                    throw new NoSuchElementException("at end");
                }
            }
        }
    }
    
    /**
     * Generates all feasible 2-dimensional symbols made from two copies of a
     * given 1-dimensional one.
     */
    public static class DoubleFaceTiles extends IteratorAdapter {
        final private static Rational minCurv = new Fraction(1, 12);
        Iterator preTiles = Iterators.empty();
        Iterator tiles = Iterators.empty();
        
        public DoubleFaceTiles(final DSymbol face) {
            final DynamicDSymbol ds = new DynamicDSymbol(face);
            ds.append(face);
            this.preTiles = new CombineTiles(new DSymbol(ds));
        }

        /* (non-Javadoc)
         * @see org.gavrog.box.collections.IteratorAdapter#findNext()
         */
        protected Object findNext() throws NoSuchElementException {
            while (true) {
                if (this.tiles.hasNext()) {
                    final DSymbol ds = (DSymbol) this.tiles.next();
                    if (!ds.isSpherical2D()) {
                        continue;
                    }
                    for (final Iterator elms = ds.elements(); elms.hasNext();) {
                        if (ds.m(1, 2, elms.next()) > 2) {
                            return ds;
                        }
                    }
                } else if (this.preTiles.hasNext()) {
                    final DSymbol ds = (DSymbol) this.preTiles.next();
                    this.tiles = new DefineBranching2d(ds, 3, 2, minCurv);
                } else {
                    throw new NoSuchElementException("at end");
                }
            }
        }
    }
    
    /**
	 * Generates all minimal, euclidean, 3-dimensional symbols made from a given
	 * 1-dimensional symbol.
	 */
    public static class ONE extends IteratorAdapter {
    	Iterator tiles = Iterators.empty();
        Iterator preTilings = Iterators.empty();
        Iterator tilings = Iterators.empty();
        
    	public ONE(final DSymbol face) {
    		this.tiles = new SingleFaceTiles(face);
    	}

		/* (non-Javadoc)
		 * @see org.gavrog.box.collections.IteratorAdapter#findNext()
		 */
		protected Object findNext() throws NoSuchElementException {
            while (true) {
                if (this.tilings.hasNext()) {
                    final DSymbol ds = (DSymbol) this.tilings.next();
                    if (!ds.isMinimal()) {
                    	continue;
                    }
                    if (new EuclidicityTester(ds).isBad()) {
                        continue;
                    }
                    return ds;
                } else if (this.preTilings.hasNext()) {
                    final DSymbol ds = (DSymbol) this.preTilings.next();
                    this.tilings = new DefineBranching3d(ds);
                } else if (this.tiles.hasNext()) {
                	final DSymbol ds = (DSymbol) this.tiles.next();
                	this.preTilings = new CombineTiles(ds);
                } else {
                    throw new NoSuchElementException("at end");
                }
            }
		}
    }
    
    /**
	 * Generates all minimal, euclidean, 3-dimensional symbols containing two
	 * 2-dimensional symbols each made from the single given 1-dimensional one.
	 */
    public static class TWO extends IteratorAdapter {
    	final List tiles;
    	int i, j;
        Iterator preTilings = Iterators.empty();
        Iterator tilings = Iterators.empty();
        
    	public TWO(final DSymbol face) {
    		this.tiles = Iterators.asList(new SingleFaceTiles(face));
    		i = j = 0;
    	}

		/* (non-Javadoc)
		 * @see org.gavrog.box.collections.IteratorAdapter#findNext()
		 */
		protected Object findNext() throws NoSuchElementException {
            while (true) {
                if (this.tilings.hasNext()) {
                    final DSymbol ds = (DSymbol) this.tilings.next();
                    if (!ds.isMinimal()) {
                    	continue;
                    }
                    if (new EuclidicityTester(ds).isBad()) {
                        continue;
                    }
                    return ds;
                } else if (this.preTilings.hasNext()) {
                    final DSymbol ds = (DSymbol) this.preTilings.next();
                    this.tilings = new DefineBranching3d(ds);
                } else if (this.i < this.tiles.size()) {
                	final DSymbol t1 = (DSymbol) this.tiles.get(i);
                	final DSymbol t2 = (DSymbol) this.tiles.get(j);
                	++j;
                	if (j >= this.tiles.size()) {
                		j = ++i;
                	}
                	final DynamicDSymbol ds = new DynamicDSymbol(t1);
                	ds.append(t2);
                	this.preTilings = new CombineTiles(new DSymbol(ds));
                } else {
                    throw new NoSuchElementException("at end");
                }
            }
		}
    }
    
    /**
     * @param args
     */
    public static void main(final String[] args) {
        final DSymbol face;
        if (args.length > 0) {
            face = new DSymbol(args[0]);
        } else {
            face = new DSymbol("6 1:2 4 6,6 3 5:3");
        }
        final Iterator symbols = new TWO(face);

        final long start = System.currentTimeMillis();
        final int count = Iterators.print(System.out, symbols, "\n");
        final long stop = System.currentTimeMillis();
        System.out.println("\n#Generated " + count + " symbols.");
        System.out.println("#Execution time was " + (stop - start) / 1000.0
                + " seconds.");
    }
}
