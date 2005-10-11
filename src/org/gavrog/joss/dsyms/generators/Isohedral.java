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
import java.util.NoSuchElementException;

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.derived.Covers;
import org.gavrog.joss.dsyms.derived.EuclidicityTester;


/**
 * Generates all minimal, locally euclidean, isohedral tilings by a given combinatorial tile.
 * 
 * @author Olaf Delgado
 * @version $Id: Isohedral.java,v 1.3 2005/10/11 04:28:33 odf Exp $
 */
public class Isohedral extends IteratorAdapter {
    private final int verbosityLevel;
    
    private final Iterator equivariant;
    private Iterator extended;
    private Iterator symbols;

    private int count2dSymbols = 0;
    private int count3dSets = 0;
    private int count3dSymbols = 0;
    private int countMinimal = 0;

    /**
     * Constructs an instance.
     * @param tile the tile to use in the tilings.
     * @param verbosityLevel determines how much logging information is produced.
     */
    public Isohedral(final DelaneySymbol tile, final int verbosityLevel) {
        this.verbosityLevel = verbosityLevel;
        
        this.equivariant = Covers.allCovers(tile.minimal());
        this.extended = null;
        this.symbols = null;
    }
    
    /* (non-Javadoc)
     * @see javaDSym.util.IteratorAdapter#findNext()
     */
    protected Object findNext() throws NoSuchElementException {
        while (true) {
            while (symbols == null || !symbols.hasNext()) {
                while (extended == null || !extended.hasNext()) {
                    if (equivariant.hasNext()) {
                        final DSymbol ds = (DSymbol) equivariant.next();
                        ++this.count2dSymbols;
                        if (this.verbosityLevel >= 1) {
                            System.err.println(ds);
                        }
                        extended = new ExtendTo3d(ds);
                    } else {
                        throw new NoSuchElementException("At end");
                    }
                }
                final DSymbol ds = (DSymbol) extended.next();
                ++this.count3dSets;
                if (this.verbosityLevel >= 2) {
                    System.err.println("    " + withoutV23(ds));
                }
                symbols = new DefineBranching(ds);
            }
            final DSymbol ds = (DSymbol) symbols.next();
            ++count3dSymbols;
            if (this.verbosityLevel >= 3) {
                System.err.println("        " + justV23(ds));
                System.err.flush();
            }
            if (ds.equals(ds.minimal())) {
                ++countMinimal;
                return new DSymbol(ds.canonical());
            }
        }
    }
    
    public String statistics() {
        return "Constructed " + count2dSymbols + " spherical symbols, " + count3dSets
               + " partial spatial symbols and " + count3dSymbols + " complete spatial symbols, of which "
               + countMinimal + " were minimal.";
    }
    
    private static String withoutV23(final DSymbol ds) {
        final String tmp = ds.toString();
        final int i = tmp.lastIndexOf(',');
        return tmp.substring(0, i+1);
    }
    
    private static String justV23(final DSymbol ds) {
        final String tmp = ds.toString();
        final int i = tmp.lastIndexOf(',');
        return tmp.substring(i+1);
    }
    
    public static void main(final String[] args) {
        int verbosityLevel = 0;
        boolean check = true;
        int i = 0;
        while (i < args.length && args[i].startsWith("-")) {
            if (args[i].startsWith("-v")) {
                verbosityLevel = Integer.parseInt(args[i].substring(2));
            } else if (args[i].equals("-e")){
                check = !check;
            } else {
                System.err.println("Unknown option '" + args[i] + "'");
            }
            ++i;
        }
        
        final DSymbol ds = new DSymbol(args[i]);
        final Isohedral iter = new Isohedral(ds, verbosityLevel);
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
                           + (verbosityLevel <= 0 ? "quiet" : ("verbosity level " + verbosityLevel)) + ".");
    }
}
