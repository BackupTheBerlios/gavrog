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
import java.util.NoSuchElementException;

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.collections.Iterators;
import org.gavrog.jane.fpgroups.FpGroup;
import org.gavrog.jane.fpgroups.GroupAction;
import org.gavrog.jane.fpgroups.SmallActionsIterator;
import org.gavrog.jane.numbers.Fraction;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.derived.Covers;
import org.gavrog.joss.dsyms.derived.FundamentalGroup;


/**
 * Generates all spherical, non-degenerate 2d Delaney symbols with up to two
 * faces of the same size.
 * 
 * @author Olaf Delgado
 * @version $Id: MakeTilesForFaceTransitive.java,v 1.2 2006/11/01 23:07:09 odf Exp $
 */
public class MakeTilesForFaceTransitive extends IteratorAdapter {
    final private int size;
    private int deg;
    private FundamentalGroup G;
	private Iterator actions;
	private Iterator current;

    public MakeTilesForFaceTransitive(final int size) {
        this.size = size;
        this.deg = 3;
        this.actions = Iterators.empty();
        this.current = Iterators.empty();
    }
    
    protected Object findNext() throws NoSuchElementException {
        final List idcs = new IndexList(0, 1);
		while (true) {
			if (this.current.hasNext()) {
                boolean ok = true;
			    final DSymbol ds = (DSymbol) this.current.next();
                for (Iterator iter = ds.orbitRepresentatives(idcs); iter
                        .hasNext();) {
                    if (ds.v(0, 1, iter.next()) == 5) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    return ds;
                }
			} else if (this.actions.hasNext()) {
				final GroupAction action = (GroupAction) this.actions.next();
				final DSymbol set = new DSymbol(Covers.cover(G, action));
                if (set.numberOfOrbits(idcs) > 2) {
                    continue;
                }
                this.current = new DefineBranching2d(set, 3, 3, new Fraction(1, 12));
            } else if (this.deg <= 5) {
                final String code = "1:1,1,1:" + deg + ",0";
                this.G = new FundamentalGroup(new DSymbol(code));
                final FpGroup pG = G.getPresentation();
                this.actions = new SmallActionsIterator(pG, size, false);
                ++this.deg;
			} else {
				throw new NoSuchElementException("at end");
			}
		}
	}
    
    public static void main(final String[] args) {
        int i = 0;
        final int maxSize = args.length > i ? Integer.parseInt(args[i]) : 6;
        final Iterator symbols = new MakeTilesForFaceTransitive(maxSize);

        final long start = System.currentTimeMillis();
        final int count = Iterators.print(System.out, symbols, "\n");
        final long stop = System.currentTimeMillis();
        System.out.println("\nGenerated " + count + " symbols.");
        System.out.println("Execution time was " + (stop - start) / 1000.0
                + " seconds.");
    }
}
