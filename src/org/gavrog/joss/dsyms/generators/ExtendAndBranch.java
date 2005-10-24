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
import org.gavrog.joss.dsyms.derived.EuclidicityTester;


/**
 * @author Olaf Delgado
 * @version $Id: ExtendAndBranch.java,v 1.1 2005/10/19 23:09:08 odf Exp $
 */
public class ExtendAndBranch extends IteratorAdapter {
    private final boolean verbose;
    
    private Iterator extended;
    private Iterator symbols;

    private int count3dSets = 0;
    private int count3dSymbols = 0;
    private int countMinimal = 0;

    /**
     * Constructs an instance.
     * @param tiles the tile to use in the tilings.
     * @param verbose if true, some logging information is produced.
     */
    public ExtendAndBranch(final DSymbol tiles, final boolean verbose) {
        this.verbose = verbose;
        
        this.extended = new ExtendTo3d(tiles);
        this.symbols = null;
    }
    
    /* (non-Javadoc)
     * @see javaDSym.util.IteratorAdapter#findNext()
     */
    protected Object findNext() throws NoSuchElementException {
        while (true) {
            while (symbols == null || !symbols.hasNext()) {
                if (extended.hasNext()) {
                    final DSymbol ds = (DSymbol) extended.next();
                    ++this.count3dSets;
                    if (this.verbose) {
                        System.err.println("    " + setAsString(ds));
                    }
                    symbols = defineBranching(ds);
                } else {
                    throw new NoSuchElementException("At end");
                }
            }
            final DSymbol ds = (DSymbol) symbols.next();
            ++count3dSymbols;
            if (this.verbose) {
                System.err.println("        " + branchingAsString(ds));
                System.err.flush();
            }
            if (ds.equals(ds.minimal())) {
                ++countMinimal;
                return new DSymbol(ds.canonical());
            }
        }
    }
    
    /**
     * Override this to restrict or change the generation of branching number combination.
     * 
     * @param ds a Delaney symbol.
     * @return an iterator over all admissible extensions of ds with complete branching.
     */
    protected Iterator defineBranching(final DelaneySymbol ds) {
        return new DefineBranching(ds);
    }
    
    /**
     * Override this to restrict or change the generation of 3-neighbor relations.
     * 
     * @param ds a Delaney symbol.
     * @return an iterator over all admissible extensions.
     */
    protected Iterator extendTo3d(final DSymbol ds) {
        return new ExtendTo3d(ds);
    }
    
    public String statistics() {
        return "Constructed " + count3dSets
               + " partial spatial symbols and " + count3dSymbols + " complete spatial symbols, of which "
               + countMinimal + " were minimal.";
    }
    
    private static String setAsString(final DSymbol ds) {
        final String tmp = ds.toString();
        final int i = tmp.lastIndexOf(':');
        return tmp.substring(0, i);
    }
    
    private static String branchingAsString(final DSymbol ds) {
        final String tmp = ds.toString();
        final int i = tmp.lastIndexOf(':');
        return tmp.substring(i+1);
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
        
        final DSymbol ds = new DSymbol(args[i]);
        final ExtendAndBranch iter = new ExtendAndBranch(ds, verbose);
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