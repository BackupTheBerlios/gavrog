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
 * @author Olaf Delgado
 * @version $Id: TestOperatorType.java,v 1.1 2005/09/18 02:33:46 odf Exp $
 */
public class TestOperatorType extends TestCase {
    final OperatorType ot1 = new OperatorType(new Operator("-y,x"));
    final Matrix M1 = new Matrix(new int[][] { { 0, 1 }, { -1, 0 } });
    final OperatorType ot2 = new OperatorType(new Operator("y,x"));
    final Matrix M2 = new Matrix(new int[][] { { 0, 1 }, { 1, 0 } });

    public void testGetAxisMatrix() {
        assertNull(OperatorType.getAxis(M1));
        assertTrue(OperatorType.getAxis(M2).isCollinearTo(new Vector(new int[] {1, 1})));
    }

    public void testMatrixOrder() {
       assertEquals(4, OperatorType.matrixOrder(M1, 6));
       assertEquals(2, OperatorType.matrixOrder(M2, 6));
    }

    public void testGetAxis() {
        assertNull(ot1.getAxis());
        assertTrue(ot2.getAxis().isCollinearTo(new Vector(new int[] {1, 1})));
    }

    public void testIsClockwise() {
        assertTrue(ot1.isClockwise());
        // isClockwise() is irrelevant for ot2
    }

    public void testGetOrder() {
        assertEquals(4, ot1.getOrder());
        assertEquals(2, ot2.getOrder());
    }

    public void testIsOrientationPreserving() {
        assertTrue(ot1.isOrientationPreserving());
        assertFalse(ot2.isOrientationPreserving());
    }
}
