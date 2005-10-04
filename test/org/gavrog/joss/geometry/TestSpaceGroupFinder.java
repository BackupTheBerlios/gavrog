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

import junit.framework.TestCase;

import org.gavrog.jane.compounds.Matrix;

/**
 * Unit test for {@link org.gavrog.joss.geometry.SpaceGroupFinder}.
 * 
 * @author Olaf Delgado
 * @version $Id: TestSpaceGroupFinder.java,v 1.11 2005/10/04 22:27:19 odf Exp $
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
        final Matrix basis = Fddd.getPreliminaryBasis();
        assertEquals(3, basis.numberOfRows());
        assertEquals(3, basis.numberOfColumns());
        assertTrue(basis.determinant().isPositive());
    }

    public void testGetPreliminaryBasis2() {
        final Matrix basis = P31.getPreliminaryBasis();
        assertEquals(3, basis.numberOfRows());
        assertEquals(3, basis.numberOfColumns());
        assertTrue(basis.determinant().isPositive());
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
