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
import java.util.List;

import junit.framework.TestCase;

/**
 * Unit test for {@link org.gavrog.joss.geometry.SpaceGroupFinder}.
 * 
 * @author Olaf Delgado
 * @version $Id: TestSpaceGroupFinder.java,v 1.14 2005/10/07 06:12:26 odf Exp $
 */
public class TestSpaceGroupFinder extends TestCase {
    public void setUp() {
    }
    
    public void tearDown() {
    }
    
//    public void testGetCrystalSystem() {
//        assertEquals(2, new SpaceGroupFinder(new SpaceGroup(3, "A121")).getCrystalSystem());
//        assertEquals(2, new SpaceGroupFinder(new SpaceGroup(3, "B112")).getCrystalSystem());
//        assertEquals(2, new SpaceGroupFinder(new SpaceGroup(3, "C121")).getCrystalSystem());
//        assertEquals(2, new SpaceGroupFinder(new SpaceGroup(3, "I121")).getCrystalSystem());
//        
//        assertEquals(222, new SpaceGroupFinder(new SpaceGroup(3, "A222")).getCrystalSystem());
//        assertEquals(222, new SpaceGroupFinder(new SpaceGroup(3, "B222")).getCrystalSystem());
//        assertEquals(222, new SpaceGroupFinder(new SpaceGroup(3, "C222")).getCrystalSystem());
//        assertEquals(222, new SpaceGroupFinder(new SpaceGroup(3, "F222")).getCrystalSystem());
//        assertEquals(222, new SpaceGroupFinder(new SpaceGroup(3, "I222")).getCrystalSystem());
//        
//        assertEquals(422, new SpaceGroupFinder(new SpaceGroup(3, "P4")).getCrystalSystem());
//        assertEquals(422, new SpaceGroupFinder(new SpaceGroup(3, "I-4")).getCrystalSystem());
//        
//        assertEquals(32, new SpaceGroupFinder(new SpaceGroup(3, "P-3")).getCrystalSystem());
//        assertEquals(32, new SpaceGroupFinder(new SpaceGroup(3, "R-3")).getCrystalSystem());
//        
//        assertEquals(622, new SpaceGroupFinder(new SpaceGroup(3, "P6")).getCrystalSystem());
//        assertEquals(622, new SpaceGroupFinder(new SpaceGroup(3, "P-62c")).getCrystalSystem());
//        
//        assertEquals(432, new SpaceGroupFinder(new SpaceGroup(3, "P23")).getCrystalSystem());
//        assertEquals(432, new SpaceGroupFinder(new SpaceGroup(3, "F23")).getCrystalSystem());
//        assertEquals(432, new SpaceGroupFinder(new SpaceGroup(3, "I23")).getCrystalSystem());
//    }
//
//    public void testGetCentering() {
//        assertEquals('A', new SpaceGroupFinder(new SpaceGroup(3, "A121")).getCentering());
//        assertEquals('A', new SpaceGroupFinder(new SpaceGroup(3, "B112")).getCentering());
//        assertEquals('A', new SpaceGroupFinder(new SpaceGroup(3, "C121")).getCentering());
//        assertEquals('A', new SpaceGroupFinder(new SpaceGroup(3, "I121")).getCentering());
//        
//        assertEquals('C', new SpaceGroupFinder(new SpaceGroup(3, "A222")).getCentering());
//        assertEquals('C', new SpaceGroupFinder(new SpaceGroup(3, "B222")).getCentering());
//        assertEquals('C', new SpaceGroupFinder(new SpaceGroup(3, "C222")).getCentering());
//        assertEquals('F', new SpaceGroupFinder(new SpaceGroup(3, "F222")).getCentering());
//        assertEquals('I', new SpaceGroupFinder(new SpaceGroup(3, "I222")).getCentering());
//        
//        assertEquals('P', new SpaceGroupFinder(new SpaceGroup(3, "P4")).getCentering());
//        assertEquals('I', new SpaceGroupFinder(new SpaceGroup(3, "I-4")).getCentering());
//        
//        assertEquals('P', new SpaceGroupFinder(new SpaceGroup(3, "P-3")).getCentering());
//        assertEquals('R', new SpaceGroupFinder(new SpaceGroup(3, "R-3")).getCentering());
//        
//        assertEquals('P', new SpaceGroupFinder(new SpaceGroup(3, "P6")).getCentering());
//        assertEquals('P', new SpaceGroupFinder(new SpaceGroup(3, "P-62c")).getCentering());
//        
//        assertEquals('P', new SpaceGroupFinder(new SpaceGroup(3, "P23")).getCentering());
//        assertEquals('F', new SpaceGroupFinder(new SpaceGroup(3, "F23")).getCentering());
//        assertEquals('I', new SpaceGroupFinder(new SpaceGroup(3, "I23")).getCentering());
//   }

    public void testGetGroupName() {
        final StringBuffer failed = new StringBuffer(100);
        int countFailed = 0;
        String canonicalName = null;
        for (final Iterator iter = SpaceGroupCatalogue.settingNames(3); iter.hasNext();) {
            final String name = (String) iter.next();
            final List ops = SpaceGroupCatalogue.operators(3, name);
            final CoordinateChange trans = SpaceGroupCatalogue.transform(3, name);
            if (trans.isOne()) {
                canonicalName = name.split(":")[0];
                System.out.println(canonicalName);
            }
            System.out.println("  " + name);
            final SpaceGroupFinder finder = new SpaceGroupFinder(new SpaceGroup(3, ops));
            if (!canonicalName.equals(finder.getGroupName())) {
                failed.append(name + " ==> " + finder.getGroupName() + " (should be "
                              + canonicalName + ")\n");
                ++countFailed;
            }
        }
        if (countFailed > 0) {
            failed.append(countFailed + " groups were not recognized.\n");
        }
        assertEquals("", failed.toString());
    }
    
    public void xtest() {
        final SpaceGroupFinder finder = new SpaceGroupFinder(new SpaceGroup(3, "Pmc21"));
        assertEquals("Pmc21", finder.getGroupName());
    }
}
