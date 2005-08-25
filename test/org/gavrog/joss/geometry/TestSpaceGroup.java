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
 * @version $Id: TestSpaceGroup.java,v 1.4 2005/08/25 21:57:55 odf Exp $
 */
public class TestSpaceGroup extends TestCase {
    private SpaceGroup Fddd;
    private SpaceGroup c2mm;

    public void setUp() {
        Fddd = new SpaceGroup(3, "Fddd");
        c2mm = new SpaceGroup(2, "c2mm");
    }
    
    public void tearDown() {
        Fddd = null;
        c2mm = null;
    }
    
    public void testSpaceGroupByName() {
        // --- read all IT settings without structural tests
        for (final Iterator iter = SpaceGroup.groupNames(3); iter.hasNext();) {
            new SpaceGroup(3, (String) iter.next());
        }
        for (final Iterator iter = SpaceGroup.groupNames(2); iter.hasNext();) {
            new SpaceGroup(2, (String) iter.next());
        }
    }
    
    public void testSpaceGroupByFullOpsList() {
        // --- do the full testing for one group
        new SpaceGroup(3, SpaceGroup.operators(3, "Ia-3d"), false, true);
        // --- also for a 2-dimensional one
        new SpaceGroup(2, SpaceGroup.operators(2, "p4mg"));
        
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
        final Matrix B = (Matrix) new Matrix(new int[][] { { 1, 0, 1 }, { 0, 1, 1 },
                { 0, 0, 2 } }).dividedBy(2);
        assertEquals(B, Fddd.primitiveCell());
        final Matrix C = (Matrix) new Matrix(new int[][] {{1, 1}, {0, 2}}).dividedBy(2);
        assertEquals(C, c2mm.primitiveCell());
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
        final List ops = SpaceGroup.operators(3, "Ia-3d");
        assertNotNull(ops);
        assertEquals(96, ops.size());
    }
    
    public void testTransform() {
        final Operator T = SpaceGroup.transform(3, "Ia-3d");
        assertNotNull(T);
        assertEquals(Operator.identity(3), T);
    }
}
