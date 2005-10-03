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

import junit.framework.TestCase;

import org.gavrog.jane.compounds.Matrix;

/**
 * Unit test for {@link org.gavrog.joss.geometry.SpaceGroupFinder}.
 * 
 * @author Olaf Delgado
 * @version $Id: TestSpaceGroupFinder.java,v 1.10 2005/10/03 22:12:45 odf Exp $
 */
public class TestSpaceGroupFinder extends TestCase {
    private SpaceGroupFinder Fddd;
    private SpaceGroupFinder P31;
    //private SpaceGroupFinder c2mm;

    public void setUp() {
        Fddd = new SpaceGroupFinder(new SpaceGroup(3, "Fddd"));
        P31 = new SpaceGroupFinder(new SpaceGroup(3, "P31"));
        //c2mm = new SpaceGroupFinder(new SpaceGroup(2, "c2mm"));
    }
    
    public void tearDown() {
        Fddd = null;
        P31 = null;
        //c2mm = null;
    }
    
    public void testGetCrystalSystem() {
        assertEquals(SpaceGroupFinder.ORTHORHOMBIC_SYSTEM, Fddd.getCrystalSystem());
        assertEquals(SpaceGroupFinder.TRIGONAL_SYSTEM, P31.getCrystalSystem());
    }

    public void testGetPreliminaryBasis1() {
        final List gens = Fddd.getGeneratorsOriginalBasis();
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
        
        final Matrix basis = Fddd.getPreliminaryBasis();
        assertEquals(3, basis.numberOfRows());
        assertEquals(3, basis.numberOfColumns());
        assertTrue(basis.determinant().isPositive());
        for (int i = 0; i < 3; ++i) {
            final Operator op = g[i];
            int countFixed = 0;
            int countTurned = 0;
            for (int j = 0; j < 3; ++j) {
                final Vector v = new Vector(basis.getRow(j));
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

    public void testGetPreliminaryBasis2() {
        final List gens = P31.getGeneratorsOriginalBasis();
        assertEquals(1, gens.size());
        final Operator g = (Operator) gens.get(0);
        assertEquals(new OperatorType(3, true, 3, true), new OperatorType(g));
        
        final Matrix basis = P31.getPreliminaryBasis();
        assertEquals(3, basis.numberOfRows());
        assertEquals(3, basis.numberOfColumns());
        assertTrue(basis.determinant().isPositive());
        final Vector v[] = Vector.fromMatrix(basis);
        assertEquals(v[2], v[2].times(g));
        assertEquals(v[1], v[0].times(g));
    }
    
    public void testGetCentering() {
        assertEquals('A', new SpaceGroupFinder(new SpaceGroup(3, "A121")).getCentering());
        assertEquals('A', new SpaceGroupFinder(new SpaceGroup(3, "B112")).getCentering());
        assertEquals('A', new SpaceGroupFinder(new SpaceGroup(3, "C121")).getCentering());
        assertEquals('A', new SpaceGroupFinder(new SpaceGroup(3, "I121")).getCentering());
        
        assertEquals('A', new SpaceGroupFinder(new SpaceGroup(3, "A222")).getCentering());
        assertEquals('B', new SpaceGroupFinder(new SpaceGroup(3, "B222")).getCentering());
        assertEquals('C', new SpaceGroupFinder(new SpaceGroup(3, "C222")).getCentering());
        assertEquals('F', new SpaceGroupFinder(new SpaceGroup(3, "F222")).getCentering());
        assertEquals('I', new SpaceGroupFinder(new SpaceGroup(3, "I222")).getCentering());
        
        assertEquals('P', new SpaceGroupFinder(new SpaceGroup(3, "P4")).getCentering());
        assertEquals('I', new SpaceGroupFinder(new SpaceGroup(3, "I-4")).getCentering());
        
        assertEquals('P', new SpaceGroupFinder(new SpaceGroup(3, "P-3")).getCentering());
        assertEquals('R', new SpaceGroupFinder(new SpaceGroup(3, "R-3")).getCentering());
        
        assertEquals('P', new SpaceGroupFinder(new SpaceGroup(3, "P6")).getCentering());
        assertEquals('P', new SpaceGroupFinder(new SpaceGroup(3, "P-62c")).getCentering());
        
        assertEquals('P', new SpaceGroupFinder(new SpaceGroup(3, "P23")).getCentering());
        assertEquals('F', new SpaceGroupFinder(new SpaceGroup(3, "F23")).getCentering());
        assertEquals('I', new SpaceGroupFinder(new SpaceGroup(3, "I23")).getCentering());
   }
}
