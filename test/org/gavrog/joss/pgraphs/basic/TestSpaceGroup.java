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

package org.gavrog.joss.pgraphs.basic;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.Matrix;
import org.gavrog.joss.pgraphs.basic.SpaceGroup;
import org.gavrog.joss.pgraphs.io.NetParser;


import junit.framework.TestCase;

/**
 * Unit tests for the class SpaceGroup.
 * 
 * @author Olaf Delgado
 * @version $Id: TestSpaceGroup.java,v 1.1 2005/07/15 21:12:51 odf Exp $
 */
public class TestSpaceGroup extends TestCase {
    private SpaceGroup Fddd;

    public void setUp() {
        Fddd = new SpaceGroup(3, "Fddd");
    }
    
    public void tearDown() {
        Fddd = null;
    }
    
    public void testSpaceGroupByName() {
        // --- read all IT settings without structural tests
        for (final Iterator iter = NetParser.groupNames(); iter.hasNext();) {
            new SpaceGroup(3, (String) iter.next());
        }
    }
    
    public void testSpaceGroupByFullOpsList() {
        // --- do the full testing for one group
        new SpaceGroup(3, NetParser.operators("Ia-3d"), false, true);
        
        // --- try some illegal inputs
        final List L = new LinkedList();
        L.add(NetParser.parseOperator("x,2y"));
        try {
            new SpaceGroup(2, L, false, false);
            fail("should throw an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
        
        L.clear();
        L.add(new Matrix(new int[][] { { 1, 0, 1 }, { 0, 1, 0 }, { 0, 0, 1 } }));
        try {
            new SpaceGroup(2, L, false, false);
            fail("should throw an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
        
        L.clear();
        L.add(NetParser.parseOperator("1/2x,y"));
        try {
            new SpaceGroup(2, L, false, false);
            fail("should throw an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
        
        L.clear();
        final Matrix B = Matrix.one(3).mutableClone();
        B.set(2, 1, new FloatingPoint(0.5));
        L.add(B);
        try {
            new SpaceGroup(2, L, false, false);
            fail("should throw an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
    }
    
    public void testSpaceGroupByGenerators() {
        final List L = new LinkedList();
        L.add(NetParser.parseOperator("-x,y"));
        L.add(NetParser.parseOperator("x,-y"));
        L.add(NetParser.parseOperator("x-1/2,y-1/2"));
        final SpaceGroup G = new SpaceGroup(2, L, true, false);
        assertEquals(8, G.getOperators().size());
    }

    public void testGetDimension() {
        final List L = new LinkedList();
        L.add(Matrix.one(3));
        final SpaceGroup G = new SpaceGroup(2, L, false, false);
        assertEquals(2, G.getDimension());
    }
    
    public void testNormalizedOperator() {
        final SpaceGroup G = new SpaceGroup(3, "P1");
        final Matrix A = NetParser.parseOperator("z,x,y+1/2");
        final Matrix B = NetParser.parseOperator("z-2,x+1,y-3/2");
        final Matrix C = NetParser.parseOperator("z,x,y");
        assertEquals(G.normalizedOperator(A), G.normalizedOperator(B));
        assertFalse(G.normalizedOperator(A).equals(G.normalizedOperator(C)));
    }

    public void testPrimitiveCell() {
        final Matrix B = NetParser.parseOperator("1/2x,1/2y,1/2x+1/2y+z");
        assertEquals(B.getSubMatrix(0, 0, 3, 3), Fddd.primitiveCell());
    }

    public void testGetOperators() {
        assertEquals(32, Fddd.getOperators().size());
    }
    
    public void testPrimitiveOperators() {
        assertEquals(8, Fddd.primitiveOperators().size());
    }
}
