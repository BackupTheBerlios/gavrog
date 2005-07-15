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

package org.gavrog.chosen;

import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.derived.EuclidicityTester;
import org.gavrog.joss.dsyms.generators.TileKTransitive;


/**
 * Generates all tile-k-transitivel tilings by a given tile, specified as a
 * spherical 2d Delaney symbol.
 * 
 * @author Olaf Delgado
 * @version $Id: GenerateTileKTransitive.java,v 1.1 2005/07/15 21:12:51 odf Exp $
 */

public class GenerateTileKTransitive {
    public static void main(final String[] args) {
        boolean verbose = false;
        boolean check = true;
        int i = 0;
        while (i < args.length && args[i].startsWith("-")) {
            if (args[i].equals("-v")) {
                verbose = !verbose;
            } else if (args[i].equals("-e")){
                check = !check;
            } else {
                System.err.println("Unknown option '" + args[i] + "'");
            }
            ++i;
        }
        
        final DSymbol ds = new DSymbol(args[i]);
        final int k = Integer.parseInt(args[i+1]);
        final TileKTransitive iter = new TileKTransitive(ds, k, verbose);
        int countGood = 0;
        int countAmbiguous = 0;

        try {
            while (iter.hasNext()) {
                final DSymbol out = (DSymbol) iter.next();
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
            System.err.println("Of the latter, " + countGood + " were found euclidean.");
            if (countAmbiguous > 0) {
                System.err.println("For " + countAmbiguous
                                   + " symbols, euclidicity could not yet be decided.");
            }
        }
        System.err.println("Options: " + (check ? "" : "no") + " euclidicity check, "
                           + (verbose ? "verbose" : "quiet") + ".");
    }
}
