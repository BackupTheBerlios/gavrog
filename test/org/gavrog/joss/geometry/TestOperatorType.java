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
 * @author Olaf Delgado
 * @version $Id: TestOperatorType.java,v 1.2 2005/09/18 03:22:34 odf Exp $
 */
public class TestOperatorType extends TestCase {
    final OperatorType ot1 = new OperatorType(new Operator("-y,x"));
    final OperatorType ot2 = new OperatorType(new Operator("y,x"));
    final OperatorType ot3 = new OperatorType(new Operator("z,x,y"));
    final OperatorType ot4 = new OperatorType(new Operator("y,z,x"));
    final OperatorType ot5 = new OperatorType(new Operator("x,y"));
    final OperatorType ot6 = new OperatorType(new Operator("x,y,z"));
    final OperatorType ot7 = new OperatorType(new Operator("y,-x"));
    final OperatorType ot8 = new OperatorType(new Operator("y,z,x"));

    public void testGetAxis() {
        assertNull(ot1.getAxis());
        assertTrue(ot2.getAxis().isCollinearTo(new Vector(new int[] {1, 1})));
        assertTrue(ot3.getAxis().isCollinearTo(new Vector(new int[] {1, 1, 1})));
        assertTrue(ot4.getAxis().isCollinearTo(new Vector(new int[] {1, 1, 1})));
        assertNull(ot5.getAxis());
        assertNull(ot6.getAxis());
        assertNull(ot7.getAxis());
        assertTrue(ot8.getAxis().isCollinearTo(new Vector(new int[] {1, 1, 1})));
    }

    public void testIsClockwise() {
        assertTrue(ot1.isClockwise());
        // isClockwise() is irrelevant for ot2
        assertTrue(ot3.isClockwise());
        assertFalse(ot4.isClockwise());
        assertTrue(ot5.isClockwise());
        assertTrue(ot6.isClockwise());
        assertFalse(ot7.isClockwise());
        assertFalse(ot8.isClockwise());
    }

    public void testGetOrder() {
        assertEquals(4, ot1.getOrder());
        assertEquals(2, ot2.getOrder());
        assertEquals(3, ot3.getOrder());
        assertEquals(3, ot4.getOrder());
        assertEquals(1, ot5.getOrder());
        assertEquals(1, ot6.getOrder());
        assertEquals(4, ot7.getOrder());
        assertEquals(3, ot8.getOrder());
    }

    public void testIsOrientationPreserving() {
        assertTrue(ot1.isOrientationPreserving());
        assertFalse(ot2.isOrientationPreserving());
        assertTrue(ot3.isOrientationPreserving());
        assertTrue(ot4.isOrientationPreserving());
        assertTrue(ot5.isOrientationPreserving());
        assertTrue(ot6.isOrientationPreserving());
        assertTrue(ot7.isOrientationPreserving());
        assertTrue(ot8.isOrientationPreserving());
    }
}
