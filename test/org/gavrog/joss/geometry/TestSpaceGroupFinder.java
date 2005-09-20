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

import java.util.Map;

import junit.framework.TestCase;

/**
 * Unit test for {@link org.gavrog.joss.geometry.SpaceGroupFinder}.
 * 
 * @author Olaf Delgado
 * @version $Id: TestSpaceGroupFinder.java,v 1.2 2005/09/20 05:25:36 odf Exp $
 */
public class TestSpaceGroupFinder extends TestCase {
    private SpaceGroupFinder Fddd;
    private SpaceGroupFinder c2mm;

    public void setUp() {
        Fddd = new SpaceGroupFinder(new SpaceGroup(3, "Fddd"));
        c2mm = new SpaceGroupFinder(new SpaceGroup(2, "c2mm"));
    }
    
    public void tearDown() {
        Fddd = null;
        c2mm = null;
    }
    
    public void testOperatorsByType() {
        final Map map1 = c2mm.operatorsByType();
        final Map map2 = Fddd.operatorsByType();
        
        assertEquals(3, map1.size());
        assertEquals(4, map2.size());
    }
}
