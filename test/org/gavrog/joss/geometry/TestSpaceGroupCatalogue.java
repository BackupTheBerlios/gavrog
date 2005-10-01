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

/**
 * Unit tests for class {@link org.gavrog.joss.geometry.SpaceGroupCatalogue}.
 * 
 * @author Olaf Delgado
 * @version $Id: TestSpaceGroupCatalogue.java,v 1.1 2005/10/01 00:40:12 odf Exp $
 */
public class TestSpaceGroupCatalogue extends TestCase {
    public void testOperators() {
        final List ops = SpaceGroupCatalogue.operators(3, "Ia-3d");
        assertNotNull(ops);
        assertEquals(96, ops.size());
    }
    
    public void testTransform() {
        final Operator T = SpaceGroupCatalogue.transform(3, "Ia-3d");
        assertNotNull(T);
        assertEquals(Operator.identity(3), T);
    }
}
