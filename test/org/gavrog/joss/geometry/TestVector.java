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
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Whole;

/**
 * Unit tests for the Vector class.
 * 
 * @author Olaf Delgado
 * @version $Id: TestVector.java,v 1.1 2005/08/23 04:18:41 odf Exp $
 */
public class TestVector extends TestCase {
    final Vector v = new Vector(new Matrix(new int[][] {{1, 2, 3}}));
    final Vector w = new Vector(new Matrix(new double[][] {{1, 2, 4}}));
    final Matrix M = new Matrix(new int[][] {
            { 0, 1, 0, 0 },
            { 0, 0, 1, 0 },
            { 1, 0, 0, 0 },
            { 1, 3, 2, 2 },
            });

    public void testHashCode() {
        final Vector a = new Vector(new Matrix(new int[][] {{1, 2, 3}}));
        final Vector b = new Vector(new Matrix(new int[][] {{1, 2, 4}}));
        assertEquals(v.hashCode(), a.hashCode());
        assertFalse(v.hashCode() == b.hashCode());
    }

    public void testIsExact() {
        assertTrue(v.isExact());
        assertFalse(w.isExact());
    }

    public void testZero() {
        final Vector z = new Vector(new Matrix(new int[][] {{0, 0, 0}}));
        assertEquals(z, v.zero());
    }

    public void testOne() {
        try {
            v.one();
            fail("should throw an UnsupportedOperationException");
        } catch (UnsupportedOperationException success) {
        }
    }

    public void testNegative() {
        final Vector n = new Vector(new Matrix(new int[][] {{-1, -2, -3}}));
        assertEquals(n, v.negative());
    }

    public void testInverse() {
        try {
            v.inverse();
            fail("should throw an UnsupportedOperationException");
        } catch (UnsupportedOperationException success) {
        }
    }

    public void testPlus() {
        final Vector s = new Vector(new Matrix(new double[][] {{2, 4, 7}}));
        final Point p = new Point(new Matrix(new double[][] {{1, 2, 4}}));
        final Point q = new Point(new Matrix(new double[][] {{2, 4, 7}}));
        assertEquals(s, v.plus(w));
        assertEquals(q, v.plus(p));
    }

    public void testTimes() {
        final Vector a = new Vector(new Matrix(new double[][] {{1.5, 0.5, 1}}));
        assertEquals(a, v.times(new Operator(M)));
    }

    public void testCompareTo() {
        final Vector a = new Vector(new Matrix(new int[][] {{1, 2, 3}}));
        assertTrue(v.compareTo(w) < 0);
        assertTrue(w.compareTo(v) > 0);
        assertTrue(v.compareTo(a) == 0);
    }

    public void testFloor() {
        try {
            v.floor();
            fail("should throw an UnsupportedOperationException");
        } catch (UnsupportedOperationException success) {
        }
    }

    public void testToString() {
        final String s = "Vector(1,2,3)";
        assertEquals(s, v.toString());
    }

    public void testVectorMatrix() {
        final Vector a = new Vector(new Matrix(new int[][] {{1, 2, 3}}));
        assertEquals(v, a);
    }

    public void testVectorIArithmeticArray() {
        final Vector a = new Vector(new IArithmetic[] { new Whole(1), new Whole(2),
                new Whole(3) });
        assertEquals(v, a);
    }

    public void testVectorVector() {
        final Vector a = new Vector(v);
        assertEquals(v, a);
    }

    public void testGetDimension() {
        assertEquals(3, v.getDimension());
    }

    public void testGet() {
        assertEquals(new Whole(2), v.get(1));
    }

    public void testGetCoordinates() {
        final Matrix A = new Matrix(new int[][] {{1, 2, 3}});
        assertEquals(A, v.getCoordinates());
    }
}
