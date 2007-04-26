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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.gavrog.box.collections.Iterators;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.derived.EuclidicityTester;


/**
 * Generates all tile-k-transitive tetrahedra tilings with edge degrees 5 and 6 only.
 * 
 * @author Olaf Delgado
 * @version $Id: FrankKasper.java,v 1.3 2007/04/26 20:21:58 odf Exp $
 */

public class FrankKasper extends TileKTransitive {
    public FrankKasper(final int k, final boolean verbose) {
        super(new DSymbol("1:1,1,1:3,3"), k, verbose);
    }

    protected Iterator defineBranching(final DelaneySymbol ds) {
        final DynamicDSymbol out = new DynamicDSymbol(new DSymbol(ds));
        final IndexList idx = new IndexList(2, 3);
        for (final Iterator reps = out.orbitReps(idx); reps.hasNext();) {
            final Object D = reps.next();
            final int r = out.r(2, 3, D);
            if (r == 5) {
                out.redefineV(2, 3, D, 1);
            } else if (r > 0 && 6 % r == 0) {
                out.redefineV(2, 3, D, 6 / r);
            } else {
                throw new RuntimeException("this should not happen: r = " + r + " at D = " + D);
            }
        }
        return Iterators.singleton(new DSymbol(out));
    }
    
    protected Iterator extendTo3d(final DSymbol ds) {
        return new ExtendTo3d(ds) {
            protected List getExtraDeductions(final DelaneySymbol ds, final Move move) {
                final List out = new ArrayList();
                final Object D = move.element;
                Object E = D;
                int r = 0;
                List cuts = new ArrayList();
                do {
                    E = ds.op(2, E);
                    if (ds.definesOp(3, E)) {
                        E = ds.op(3, E);
                    } else {
                        cuts.add(E);
                    }
                    ++r;
                } while (E != D);
                
                switch (cuts.size()) {
                case 0:
                    if (r == 4 || r > 6) {
                        return null;
                    }
                    break;
                case 1:
                    if (r > 6) {
                        return null;
                    } else if (r == 6) {
                        final Object A = cuts.get(0);
                        out.add(new Move(A, A, -1, -1, false));
                    }
                    break;
                case 2:
                    if (r > 12) {
                        return null;
                    } else if (r == 12) {
                        final Object A = cuts.get(0);
                        final Object B = cuts.get(1);
                        out.add(new Move(A, B, -1, -1, false));
                    }
                    break;
                default:
                    throw new RuntimeException("this should not happen");
                }
                
                return out;
            }
        };
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
        
        final int k = Integer.parseInt(args[i]);
        final TileKTransitive iter = new FrankKasper(k, verbose);
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
