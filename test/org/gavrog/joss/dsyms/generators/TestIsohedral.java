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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.derived.EuclidicityTester;
import org.gavrog.joss.dsyms.generators.Isohedral;


import junit.framework.TestCase;

/**
 * @author Olaf Delgado
 * @version $Id: TestIsohedral.java,v 1.1.1.1 2005/07/15 21:58:40 odf Exp $
 */
public class TestIsohedral extends TestCase {
    
    public void xtestTetraExtensive() {
        final Iterator iter = new Isohedral(new DSymbol("1:1,1,1,1:3,3,3"), 0);
        final Set tilings = new HashSet();
        while (iter.hasNext()) {
            final DSymbol ds = (DSymbol) iter.next();
            if (new EuclidicityTester(ds).isGood()) {
                tilings.add(ds);
            }
        }
        assertEquals(9, tilings.size());
        assertTrue(tilings.contains(new DSymbol("3 3:1 3,2 3,1 3,1 2 3:3,3,4 6")));
        assertTrue(tilings.contains(new DSymbol(""
                + "12 3:1 3 5 7 9 11 12,2 3 9 6 8 10 12,"
                + "12 6 7 10 11 8 9,1 2 3 4 5 6 7 8 9 10 11 12:"
                + "3 3 3,3 3 3,4 6 6 8 8 3 3")));
        assertTrue(tilings.contains(new DSymbol(""
                + "12 3:1 3 5 7 9 11 12,2 3 9 6 8 10 12,"
                + "12 6 7 10 11 8 9,1 2 3 4 5 6 7 8 9 10 11 12:"
                + "3 3 3,3 3 3,8 4 4 6 6 4 4")));
        assertTrue(tilings.contains(new DSymbol(""
                + "12 3:1 3 5 7 9 11 12,2 3 9 6 8 10 12,"
                + "12 6 7 10 11 8 9,1 2 3 5 9 8 10 11 12:3 3 3,3 3 3,8 3 3 12")));
        assertTrue(tilings.contains(new DSymbol(""
                + "12 3:2 4 6 8 10 12,6 3 5 12 9 11,8 7 10 9 6 12,"
                + "1 2 3 4 5 6 7 8 9 10 11 12:3 3,3 3,4 4 8 8 4 6")));
        assertTrue(tilings.contains(new DSymbol(""
                + "24 3:2 4 6 8 10 12 14 16 18 20 22 24,"
                + "6 3 5 12 9 11 18 15 17 24 21 23,"
                + "22 21 16 15 10 9 24 23 14 13 20 19,"
                + "1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 20 24 23:"
                + "3 3 3 3,3 3 3 3,4 4 4 4 4 4 8 8 12")));
        assertTrue(tilings.contains(new DSymbol(""
                + "24 3:2 4 6 8 10 12 14 16 18 20 22 24,"
                + "6 3 5 12 9 11 18 15 17 24 21 23,"
                + "22 21 16 15 10 9 24 23 14 13 20 19,"
                + "1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 20 24 23:"
                + "3 3 3 3,3 3 3 3,4 4 4 4 6 6 6 6 12")));
        assertTrue(tilings.contains(new DSymbol(""
                + "24 3:2 4 6 8 10 12 14 16 18 20 22 24,"
                + "6 3 5 12 9 11 18 15 17 24 21 23,"
                + "22 21 16 15 10 9 24 23 14 13 20 19,"
                + "1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 20 24 23:"
                + "3 3 3 3,3 3 3 3,4 4 4 4 6 6 8 8 8")));
        assertTrue(tilings.contains(new DSymbol(""
                + "24 3:2 4 6 8 10 12 14 16 18 20 22 24,"
                + "6 3 5 12 9 11 18 15 17 24 21 23,"
                + "22 21 16 15 10 9 24 23 14 13 20 19,"
                + "1 2 3 4 5 6 7 8 9 10 11 12 14 18 17 20 24 23:"
                + "3 3 3 3,3 3 3 3,4 4 8 6 6 4")));
    }
    
    public void testTetra() {
        doTest(new DSymbol("1:1,1,1:3,3"), -1);
    }
    
    public void xtestTrigonalBipyramid() {
        doTest(new DSymbol("3:2 3,1 3,1 2 3:3,3 4"), -1);
    }
    
    public void xtestOcta() {
        doTest(new DSymbol("1:1,1,1:3,4"), -1);
    }
    
    public void xtestCube() {
        doTest(new DSymbol("1:1,1,1:4,3"), -1);
    }
    
    public void doTest(final DSymbol ds, final int xCount) {
        final Iterator iter = new Isohedral(ds, 3);
        int count = 0;
        while (iter.hasNext()) {
            final DSymbol out = (DSymbol) iter.next();
            if (xCount < 0) {
                System.out.println(out);
            }
            ++count;
        }
        if (xCount < 0) {
            System.out.println("Found " + count + " symbols.");
        } else {
            assertEquals(xCount, count);
        }
    }
}
