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

package org.gavrog.joss.dsyms.filters;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.gavrog.box.collections.Pair;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.derived.EuclidicityTester;
import org.gavrog.joss.dsyms.generators.InputIterator;


/**
 * @author Olaf Delgado
 * @version $Id: FilterEuclidean.java,v 1.3 2006/11/18 08:18:06 odf Exp $
 */
public class FilterEuclidean {

    public static void main(String[] args) {
        final String filename = args[0];

        final List good = new LinkedList();
        final List ambiguous = new LinkedList();
        int count = 0;
        final long before = System.currentTimeMillis();

        for (final InputIterator input = new InputIterator(filename); input.hasNext();) {
            final DSymbol ds = (DSymbol) input.next();
            ++count;

            final EuclidicityTester tester = new EuclidicityTester(ds);
            if (tester.isGood()) {
                System.out.println("#Symbol " + count + " is good: " + tester.getCause());
                good.add(new Integer(count));
                System.out.println(ds);
            } else if (tester.isBad()) {
                System.out.println("#Symbol " + count + " is bad: " + tester.getCause());
            } else {
                System.out.println("#Symbol " + count + " is ambiguous: "
                                   + tester.getCause());
                ambiguous.add(new Pair(new Integer(count), tester.getOutcome()));
                System.out.println("#??? " + ds);
            }
        }
        final long after = System.currentTimeMillis();

        System.out.print("### " + good.size() + " good symbols:");
        for (final Iterator iter = good.iterator(); iter.hasNext();) {
            System.out.print(" " + iter.next());
        }
        System.out.println();

        System.out.println("### " + ambiguous.size() + " ambiguous symbols:");
        for (final Iterator iter = ambiguous.iterator(); iter.hasNext();) {
            final Pair pair = (Pair) iter.next();
            System.out.println("  " + pair.getFirst() + " - " + pair.getSecond());
        }
        System.out.println("### Running time was " + (after - before) / 1000
        		+ " seconds.");
    }
}
