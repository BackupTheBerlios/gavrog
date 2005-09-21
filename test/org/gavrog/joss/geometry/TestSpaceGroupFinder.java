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

import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

/**
 * Unit test for {@link org.gavrog.joss.geometry.SpaceGroupFinder}.
 * 
 * @author Olaf Delgado
 * @version $Id: TestSpaceGroupFinder.java,v 1.5 2005/09/21 22:25:19 odf Exp $
 */
public class TestSpaceGroupFinder extends TestCase {
    private SpaceGroupFinder Fddd;
    //private SpaceGroupFinder c2mm;

    public void setUp() {
        Fddd = new SpaceGroupFinder(new SpaceGroup(3, "Fddd"));
        //c2mm = new SpaceGroupFinder(new SpaceGroup(2, "c2mm"));
    }
    
    public void tearDown() {
        Fddd = null;
        //c2mm = null;
    }
    
    public void testOperatorsByType() {
        Map map;
        Set ops;
        
//        map = c2mm.operatorsByType();
//        assertEquals(3, map.size());
//        ops = (Set) map.get(new OperatorType(2, true, 1, true));
//        assertEquals(1, ops.size());
//        ops = (Set) map.get(new OperatorType(2, true, 2, true));
//        assertEquals(1, ops.size());
//        ops = (Set) map.get(new OperatorType(2, false, 2, false));
//        assertEquals(2, ops.size());
        
        map = Fddd.operatorsByType();
        assertEquals(4, map.size());
        ops = (Set) map.get(new OperatorType(3, true, 1, true));
        assertEquals(1, ops.size());
        ops = (Set) map.get(new OperatorType(3, false, 1, true));
        assertEquals(1, ops.size());
        ops = (Set) map.get(new OperatorType(3, true, 2, true));
        assertEquals(3, ops.size());
        ops = (Set) map.get(new OperatorType(3, false, 2, true));
        assertEquals(3, ops.size());
    }

    public void testGetCrystalSystem() {
        assertEquals(SpaceGroupFinder.ORTHORHOMBIC_SYSTEM, Fddd.getCrystalSystem());
    }

    public void testGetGeneratorsAndBasis() {
        final List gens = Fddd.getGenerators();
        assertEquals(4, gens.size());
        final Operator g[] = new Operator[4];
        for (int i = 0; i < 4; ++i) {
            g[i] = (Operator) gens.get(i);
        }
        for (int i = 0; i < 3; ++i) {
            assertEquals(new OperatorType(3, true, 2, true), new OperatorType(g[i]));
        }
        assertEquals(new OperatorType(3, false, 1, true), new OperatorType(g[3]));
        
        assertEquals(g[2], g[0].times(g[1]));
        
        final Vector basis[] = Fddd.getFirstBasis();
        assertEquals(3, basis.length);
        assertTrue(Vector.volume3D(basis[0], basis[1], basis[2]).isPositive());
        for (int i = 0; i < 3; ++i) {
            final Operator op = g[i];
            int countFixed = 0;
            int countTurned = 0;
            for (int j = 0; j < 3; ++j) {
                final Vector v = basis[j];
                final Vector w = (Vector) v.times(op);
                if (v.equals(w)) {
                    ++countFixed;
                } else if (v.equals(w.negative())) {
                    ++countTurned;
                }
            }
            assertEquals(1, countFixed);
            assertEquals(2, countTurned);
        }
    }
}
