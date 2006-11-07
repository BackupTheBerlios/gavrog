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

/**
 * @author Olaf Delgado
 * @version $Id: FaceTransitive.java,v 1.1 2006/11/07 22:53:47 odf Exp $
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
    
    public static class SingleFaceTiles extends IteratorAdapter {
        final private static Rational minCurv = new Fraction(1, 12);
        Iterator faces = Iterators.empty();
        Iterator preTiles = Iterators.empty();
        Iterator tiles = Iterators.empty();
        
        public SingleFaceTiles(final int size) {
            this.faces = new Faces(size);
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
                } else if (this.faces.hasNext()) {
                    final DSymbol ds = (DSymbol) this.faces.next();
                    this.preTiles = new CombineTiles(ds);
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
        final int n = (args.length > 0 ? Integer.parseInt(args[0]) : 10);
        final Iterator symbols = new SingleFaceTiles(n);

        final long start = System.currentTimeMillis();
        final int count = Iterators.print(System.out, symbols, "\n");
        final long stop = System.currentTimeMillis();
        System.out.println("\n#Generated " + count + " symbols.");
        System.out.println("#Execution time was " + (stop - start) / 1000.0
                + " seconds.");
    }
}
