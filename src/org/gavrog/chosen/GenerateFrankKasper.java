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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.derived.EuclidicityTester;
import org.gavrog.joss.dsyms.generators.DefineBranching;
import org.gavrog.joss.dsyms.generators.TileKTransitive;


/**
 * Generates all tile-k-transitive tetrahedra tilings with edge degrees not smaller than
 * 5.
 * 
 * @author Olaf Delgado
 * @version $Id: GenerateFrankKasper.java,v 1.1 2005/09/22 21:51:50 odf Exp $
 */

public class GenerateFrankKasper {
    private static class MyTileKTransitive extends TileKTransitive {
        public MyTileKTransitive(final DelaneySymbol tile, final int k, final boolean verbose) {
            super(tile, k, verbose);
        }
        
        protected Iterator defineBranching(final DelaneySymbol ds) {
            return new DefineBranching(ds) {
                protected List getExtraDeductions(final DelaneySymbol ds, final Move move) {
                    if (move.index == 2) {
                        final Integer D = new Integer(move.element);
                        if (ds.m(2, 3, D) < 5) {
                            return null;
                        }
                    }
                    return new ArrayList();
                }
            };
        }
    }
    
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
        
        final DSymbol ds = new DSymbol("1:1,1,1:3,3");
        final int k = Integer.parseInt(args[i]);
        final TileKTransitive iter = new MyTileKTransitive(ds, k, verbose);
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
