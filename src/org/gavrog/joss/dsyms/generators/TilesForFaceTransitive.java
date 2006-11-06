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
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.derived.Covers;


/**
 * Generates all spherical, non-degenerate 2d Delaney symbols with up to two
 * faces of the same size.
 * 
 * @author Olaf Delgado
 * @version $Id: TilesForFaceTransitive.java,v 1.1 2006/11/06 23:32:56 odf Exp $
 */
public class TilesForFaceTransitive extends IteratorAdapter {
    final private static Rational minCurv = new Fraction(1, 12);
    private int deg;
	private Iterator faceSets;
	private Iterator preSymbols;
    private Iterator symbols;

    public TilesForFaceTransitive() {
        this.deg = 3;
        this.faceSets = Iterators.empty();
        this.preSymbols = Iterators.empty();
        this.symbols = Iterators.empty();
    }
    
    protected Object findNext() throws NoSuchElementException {
        final List idcs = new IndexList(0, 1);
		while (true) {
			if (this.symbols.hasNext()) {
                boolean ok = true;
			    final DSymbol ds = (DSymbol) this.symbols.next();
                for (final Iterator iter = ds.orbitRepresentatives(idcs); iter
                        .hasNext();) {
                    if (ds.v(0, 1, iter.next()) == 5) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    return ds;
                }
			} else if (this.preSymbols.hasNext()) {
                final DSymbol ds = (DSymbol) this.preSymbols.next();
                this.symbols = new DefineBranching2d(ds, 3, 3, minCurv);
            } else if (this.faceSets.hasNext()) {
                final DSymbol ds = (DSymbol) this.faceSets.next();
                this.preSymbols = new CombineTiles(ds);
            } else if (this.deg <= 5) {
                final DSymbol min = new DSymbol("1 1:1,1,1:" + deg);
                final List tmp = new LinkedList();
                
                for (Iterator covers = Covers.allCovers(min); covers.hasNext();) {
                    final DSymbol ds = (DSymbol) covers.next();
                    tmp.add(ds);
                    final DynamicDSymbol dyn = new DynamicDSymbol(ds);
                    dyn.append(ds);
                    tmp.add(new DSymbol(dyn));
                }
                this.faceSets = tmp.iterator();
                ++deg;
			} else {
				throw new NoSuchElementException("at end");
			}
		}
	}
    
    public static void main(final String[] args) {
        final Iterator symbols = new TilesForFaceTransitive();

        final long start = System.currentTimeMillis();
        final int count = Iterators.print(System.out, symbols, "\n");
        final long stop = System.currentTimeMillis();
        System.out.println("\n#Generated " + count + " symbols.");
        System.out.println("#Execution time was " + (stop - start) / 1000.0
                + " seconds.");
    }
}
