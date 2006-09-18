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

package org.gavrog.joss.dsyms.filters;

import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.derived.Covers;
import org.gavrog.joss.dsyms.derived.Skeleton;
import org.gavrog.joss.dsyms.generators.InputIterator;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;


/**
 * A tiling is proper if it has the same symmetry as its underlying net.
 * 
 * @author Olaf Delgado
 * @version $Id: FilterProper.java,v 1.2 2006/09/18 21:07:56 odf Exp $
 */
public class FilterProper {

    public static void main(String[] args) {
        final String filename = args[0];

        int inCount = 0;
        int outCount = 0;

        for (final InputIterator input = new InputIterator(filename); input.hasNext();) {
            final DSymbol ds = (DSymbol) input.next();
            ++inCount;
            if (isProper(ds)) {
                ++outCount;
                System.out.println(ds);
            }
        }

        System.err.println("Read " + inCount + " symbols, " + outCount
                           + " of which were proper and had stable graphs.");
    }

    private static boolean isProper(final DSymbol ds) {
        final DSymbol min = new DSymbol(ds.minimal());
        final DSymbol cov = new DSymbol(Covers.pseudoToroidalCover3D(min));
        try {
            final PeriodicGraph gr = new Skeleton(cov);
            return gr.isMinimal() && gr.symmetries().size() == cov.size() / min.size();
        } catch (final Exception ex) {
            System.out.println("??? " + ds);
            return false;
        }
    }
}
