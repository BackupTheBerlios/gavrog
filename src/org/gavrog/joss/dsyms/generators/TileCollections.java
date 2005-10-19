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

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.collections.Iterators;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;
import org.gavrog.joss.dsyms.derived.Covers;

/**
 * @author Olaf Delgado
 * @version $Id: TileCollections.java,v 1.1 2005/10/19 23:09:08 odf Exp $
 */
public class TileCollections extends IteratorAdapter {
    private final Iterator partLists;

    /**
     * Constructs an instance.
     * 
     */
    public TileCollections(final DelaneySymbol tile, final int k) {
        final List covers = Iterators.asList(Covers.allCovers(tile.minimal()));
        this.partLists = Iterators.selections(covers.toArray(), k);
    }

    /* (non-Javadoc)
     * @see javaDSym.util.IteratorAdapter#findNext()
     */
    protected Object findNext() throws NoSuchElementException {
        while (true) {
            if (partLists.hasNext()) {
                final List tiles = (List) partLists.next();
                final DynamicDSymbol tmp = new DynamicDSymbol(2);
                for (final Iterator iter = tiles.iterator(); iter.hasNext();) {
                    tmp.append((DSymbol) iter.next());
                }
                return new DSymbol(tmp);
            } else {
                throw new NoSuchElementException("At end");
            }
        }
    }
    
    public static void main(String[] args) {
        int i = 0;
        final DSymbol ds = new DSymbol(args[i]);
        final int k = Integer.parseInt(args[i+1]);
        final Iterator iter = new TileCollections(ds, k);

        int count = 0;
        try {
            while (iter.hasNext()) {
                final DSymbol out = (DSymbol) iter.next();
                System.out.println(out);
                ++count;
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        System.err.print("produced " + count + " symbols");
    }
}
