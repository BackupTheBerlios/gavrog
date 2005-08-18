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
 * Unit tests for the Operator class.
 * 
 * @author Olaf Delgado
 * @version $Id: TestOperator.java,v 1.1 2005/08/18 02:00:42 odf Exp $
 */
public class TestOperator extends TestCase {
    final Matrix M = new Matrix(new int[][] {{0, 1, 0}, {-1, 0, 0}, {1, 0, 1}});
    final Operator op1 = new Operator(new Matrix(M));

    public void testHashCode() {
        final Matrix A = new Matrix(new int[][] {{0, 1, 0}, {1, 0, 0}, {1, 0, 1}});
        final Operator opM = new Operator(M);
        final Operator opA = new Operator(A);
        assertEquals(op1.hashCode(), opM.hashCode());
        assertFalse(op1.hashCode() == opA.hashCode());
    }

    public void testIsExact() {
        //TODO Implement isExact().
    }

    public void testZero() {
        //TODO Implement zero().
    }

    public void testOne() {
        //TODO Implement one().
    }

    public void testNegative() {
        //TODO Implement negative().
    }

    public void testInverse() {
        //TODO Implement inverse().
    }

    /*
     * Class under test for IArithmetic plus(Object)
     */
    public void testPlusObject() {
        //TODO Implement plus().
    }

    /*
     * Class under test for IArithmetic times(Object)
     */
    public void testTimesObject() {
        //TODO Implement times().
    }

    public void testCompareTo() {
        //TODO Implement compareTo().
    }

    public void testFloor() {
        //TODO Implement floor().
    }

    /*
     * Class under test for String toString()
     */
    public void testToString() {
        //TODO Implement toString().
    }

    /*
     * Class under test for void Operator(Matrix)
     */
    public void testOperatorMatrix() {
        //TODO Implement Operator().
    }

    /*
     * Class under test for void Operator(IArithmetic[][])
     */
    public void testOperatorIArithmeticArrayArray() {
        //TODO Implement Operator().
    }

    public void testGetDimension() {
        //TODO Implement getDimension().
    }

    public void testGet() {
        //TODO Implement get().
    }

    public void testGetCoordinates() {
        //TODO Implement getCoordinates().
    }

    public void testGetLinearPart() {
        //TODO Implement getLinearPart().
    }

    public void testGetImageOfOrigin() {
        //TODO Implement getImageOfOrigin().
    }

    /*
     * Class under test for IArithmetic rtimes(Object)
     */
    public void testRtimesObject() {
        //TODO Implement rtimes().
    }

}
