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

package org.gavrog.joss.symmetries;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.Fraction;
import org.gavrog.joss.pgraphs.io.DataFormatException;

/**
 * Unit tests for the class SpaceGroup.
 * 
 * @author Olaf Delgado
 * @version $Id: TestSpaceGroup.java,v 1.1 2005/08/01 17:39:56 odf Exp $
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
        for (final Iterator iter = SpaceGroup.groupNames(); iter.hasNext();) {
            new SpaceGroup(3, (String) iter.next());
        }
    }
    
    public void testSpaceGroupByFullOpsList() {
        // --- do the full testing for one group
        new SpaceGroup(3, SpaceGroup.operators("Ia-3d"), false, true);
        
        // --- try some illegal inputs
        final List L = new LinkedList();
        L.add(SpaceGroup.parseOperator("x,2y"));
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
        L.add(SpaceGroup.parseOperator("1/2x,y"));
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
        L.add(SpaceGroup.parseOperator("-x,y"));
        L.add(SpaceGroup.parseOperator("x,-y"));
        L.add(SpaceGroup.parseOperator("x-1/2,y-1/2"));
        final SpaceGroup G = new SpaceGroup(2, L, true, false);
        assertEquals(8, G.getOperators().size());
    }

    public void testGetDimension() {
        final List L = new LinkedList();
        L.add(Matrix.one(3));
        final SpaceGroup G = new SpaceGroup(2, L, false, false);
        assertEquals(2, G.getDimension());
    }
    
    public void testNormalized() {
        final Matrix A = SpaceGroup.parseOperator("z,x,y+1/2");
        final Matrix B = SpaceGroup.parseOperator("z-2,x+1,y-3/2");
        final Matrix C = SpaceGroup.parseOperator("z,x,y");
        assertEquals(SpaceGroup.normalized(A), SpaceGroup.normalized(B));
        assertFalse(SpaceGroup.normalized(A).equals(SpaceGroup.normalized(C)));
    }

    public void testPrimitiveCell() {
        final Matrix B = SpaceGroup.parseOperator("1/2x,1/2y,1/2x+1/2y+z");
        assertEquals(B.getSubMatrix(0, 0, 3, 3), Fddd.primitiveCell());
    }

    public void testGetOperators() {
        assertEquals(32, Fddd.getOperators().size());
    }
    
    public void testPrimitiveOperators() {
        final Set ops = Fddd.getOperators();
        final Set prim = Fddd.primitiveOperators();
        assertEquals(8, prim.size());
        for (final Iterator iter = prim.iterator(); iter.hasNext();) {
            assertTrue(ops.contains(iter.next()));
        }
    }

    public void testParseOperator() {
        String s;
        Matrix M;
        
        s = "x-4y+7*z-10, +5/3y-8z+11-2x, +3*x+ 9z-6y - 12";
        M = new Matrix(new int[][] {
                {  1, -2,   3, 0},
                { -4,  5,  -6, 0},
                {  7, -8,   9, 0},
                {-10, 11, -12, 1}}).mutableClone();
        M.set(1, 1, new Fraction(5, 3));
        assertEquals(M, SpaceGroup.parseOperator(s));
        assertFalse(SpaceGroup.parseOperator(s).isMutable());
        
        assertEquals(Matrix.one(4), SpaceGroup.parseOperator("x,y,z"));
        
        try {
            SpaceGroup.parseOperator("1,2,3,4");
            fail("should throw an DataFormatException");
        } catch (DataFormatException success) {
        }
        
        try {
            SpaceGroup.parseOperator("a,2,3");
            fail("should throw an DataFormatException");
        } catch (DataFormatException success) {
        }
        
        try {
            SpaceGroup.parseOperator("1,2/,3");
            fail("should throw an DataFormatException");
        } catch (DataFormatException success) {
        }
        
        try {
            SpaceGroup.parseOperator("x+3x,2,3");
            fail("should throw an DataFormatException");
        } catch (DataFormatException success) {
        }
    }
    
    public void testOperators() {
        final List ops = SpaceGroup.operators("Ia-3d");
        assertNotNull(ops);
        assertEquals(96, ops.size());
    }
    
    public void testTransform() {
        final Matrix T = SpaceGroup.transform("Ia-3d");
        assertNotNull(T);
        assertEquals(Matrix.one(4), T);
    }
}
