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

/**
 * Unit test for {@link org.gavrog.joss.geometry.SpaceGroupFinder}.
 * 
 * @author Olaf Delgado
 * @version $Id: TestSpaceGroupFinder.java,v 1.13 2005/10/07 01:37:19 odf Exp $
 */
public class TestSpaceGroupFinder extends TestCase {
    public void setUp() {
    }
    
    public void tearDown() {
    }
    
    public void testGetCrystalSystem() {
        assertEquals(2, new SpaceGroupFinder(new SpaceGroup(3, "A121")).getCrystalSystem());
        assertEquals(2, new SpaceGroupFinder(new SpaceGroup(3, "B112")).getCrystalSystem());
        assertEquals(2, new SpaceGroupFinder(new SpaceGroup(3, "C121")).getCrystalSystem());
        assertEquals(2, new SpaceGroupFinder(new SpaceGroup(3, "I121")).getCrystalSystem());
        
        assertEquals(222, new SpaceGroupFinder(new SpaceGroup(3, "A222")).getCrystalSystem());
        assertEquals(222, new SpaceGroupFinder(new SpaceGroup(3, "B222")).getCrystalSystem());
        assertEquals(222, new SpaceGroupFinder(new SpaceGroup(3, "C222")).getCrystalSystem());
        assertEquals(222, new SpaceGroupFinder(new SpaceGroup(3, "F222")).getCrystalSystem());
        assertEquals(222, new SpaceGroupFinder(new SpaceGroup(3, "I222")).getCrystalSystem());
        
        assertEquals(422, new SpaceGroupFinder(new SpaceGroup(3, "P4")).getCrystalSystem());
        assertEquals(422, new SpaceGroupFinder(new SpaceGroup(3, "I-4")).getCrystalSystem());
        
        assertEquals(32, new SpaceGroupFinder(new SpaceGroup(3, "P-3")).getCrystalSystem());
        assertEquals(32, new SpaceGroupFinder(new SpaceGroup(3, "R-3")).getCrystalSystem());
        
        assertEquals(622, new SpaceGroupFinder(new SpaceGroup(3, "P6")).getCrystalSystem());
        assertEquals(622, new SpaceGroupFinder(new SpaceGroup(3, "P-62c")).getCrystalSystem());
        
        assertEquals(432, new SpaceGroupFinder(new SpaceGroup(3, "P23")).getCrystalSystem());
        assertEquals(432, new SpaceGroupFinder(new SpaceGroup(3, "F23")).getCrystalSystem());
        assertEquals(432, new SpaceGroupFinder(new SpaceGroup(3, "I23")).getCrystalSystem());
    }

    public void testGetCentering() {
        assertEquals('A', new SpaceGroupFinder(new SpaceGroup(3, "A121")).getCentering());
        assertEquals('A', new SpaceGroupFinder(new SpaceGroup(3, "B112")).getCentering());
        assertEquals('A', new SpaceGroupFinder(new SpaceGroup(3, "C121")).getCentering());
        assertEquals('A', new SpaceGroupFinder(new SpaceGroup(3, "I121")).getCentering());
        
        assertEquals('C', new SpaceGroupFinder(new SpaceGroup(3, "A222")).getCentering());
        assertEquals('C', new SpaceGroupFinder(new SpaceGroup(3, "B222")).getCentering());
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

    public void testGetGroupName() {
        assertEquals("C121", new SpaceGroupFinder(new SpaceGroup(3, "A121")).getGroupName());
        assertEquals("C121", new SpaceGroupFinder(new SpaceGroup(3, "B112")).getGroupName());
        assertEquals("C121", new SpaceGroupFinder(new SpaceGroup(3, "C121")).getGroupName());
        assertEquals("C121", new SpaceGroupFinder(new SpaceGroup(3, "I121")).getGroupName());
        
        assertEquals("C222", new SpaceGroupFinder(new SpaceGroup(3, "A222")).getGroupName());
        assertEquals("C222", new SpaceGroupFinder(new SpaceGroup(3, "B222")).getGroupName());
        assertEquals("C222", new SpaceGroupFinder(new SpaceGroup(3, "C222")).getGroupName());
        assertEquals("F222", new SpaceGroupFinder(new SpaceGroup(3, "F222")).getGroupName());
        assertEquals("I222", new SpaceGroupFinder(new SpaceGroup(3, "I222")).getGroupName());
        
        assertEquals("P4", new SpaceGroupFinder(new SpaceGroup(3, "P4")).getGroupName());
        assertEquals("I-4", new SpaceGroupFinder(new SpaceGroup(3, "I-4")).getGroupName());
        
        assertEquals("P-3", new SpaceGroupFinder(new SpaceGroup(3, "P-3")).getGroupName());
        assertEquals("R-3", new SpaceGroupFinder(new SpaceGroup(3, "R-3")).getGroupName());
        
        assertEquals("P6", new SpaceGroupFinder(new SpaceGroup(3, "P6")).getGroupName());
        assertEquals("P-62c", new SpaceGroupFinder(new SpaceGroup(3, "P-62c")).getGroupName());
        
        assertEquals("P23", new SpaceGroupFinder(new SpaceGroup(3, "P23")).getGroupName());
        assertEquals("F23", new SpaceGroupFinder(new SpaceGroup(3, "F23")).getGroupName());
        assertEquals("I23", new SpaceGroupFinder(new SpaceGroup(3, "I23")).getGroupName());
   }
}
