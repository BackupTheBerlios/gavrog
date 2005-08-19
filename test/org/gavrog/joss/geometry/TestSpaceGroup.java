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

package org.gavrog.joss.geometry;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.FloatingPoint;

/**
 * Unit tests for the class SpaceGroup.
 * 
 * @author Olaf Delgado
 * @version $Id: TestSpaceGroup.java,v 1.3 2005/08/19 22:43:41 odf Exp $
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
        L.add(new Operator("x,2y"));
        try {
            new SpaceGroup(2, L, false, false);
            fail("should throw an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
        
        L.clear();
        L.add(new Operator(new Matrix(
                new int[][] { { 1, 0, 1 }, { 0, 1, 0 }, { 0, 0, 1 } })));
        try {
            new SpaceGroup(2, L, false, false);
            fail("should throw an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
        
        L.clear();
        L.add(new Operator("1/2x,y"));
        try {
            new SpaceGroup(2, L, false, false);
            fail("should throw an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
        
        L.clear();
        final Matrix B = Matrix.one(3).mutableClone();
        B.set(2, 1, new FloatingPoint(0.5));
        L.add(new Operator(B));
        try {
            new SpaceGroup(2, L, false, false);
            fail("should throw an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
    }
    
    public void testSpaceGroupByGenerators() {
        final List L = new LinkedList();
        L.add(new Operator("-x,y"));
        L.add(new Operator("x,-y"));
        L.add(new Operator("x-1/2,y-1/2"));
        final SpaceGroup G = new SpaceGroup(2, L, true, false);
        assertEquals(8, G.getOperators().size());
    }

    public void testGetDimension() {
        final List L = new LinkedList();
        L.add(Operator.identity(2));
        final SpaceGroup G = new SpaceGroup(2, L, false, false);
        assertEquals(2, G.getDimension());
    }
    
    public void testPrimitiveCell() {
        final Matrix B = Operator.parse("1/2x,1/2y,1/2x+1/2y+z");
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

    public void testOperators() {
        final List ops = SpaceGroup.operators("Ia-3d");
        assertNotNull(ops);
        assertEquals(96, ops.size());
    }
    
    public void testTransform() {
        final Operator T = SpaceGroup.transform("Ia-3d");
        assertNotNull(T);
        assertEquals(Operator.identity(3), T);
    }
}
