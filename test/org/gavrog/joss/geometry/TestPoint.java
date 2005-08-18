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
 * Unit tests for the Point class.
 * 
 * @author Olaf Delgado
 * @version $Id: TestPoint.java,v 1.2 2005/08/18 02:45:32 odf Exp $
 */
public class TestPoint extends TestCase {
    final Point p = new Point(new Matrix(new int[][] {{1, 2, 3}}));
    final Point q = new Point(new Matrix(new double[][] {{1, 2, 4}}));
    final Matrix M = new Matrix(new int[][] {
            { 0, 1, 0, 0 },
            { 0, 0, 1, 0 },
            { 1, 0, 0, 0 },
            { 1, 3, 2, 2 },
            });

    public void testHashCode() {
        final Point a = new Point(new Matrix(new int[][] {{1, 2, 3}}));
        final Point b = new Point(new Matrix(new int[][] {{1, 2, 4}}));
        assertEquals(p.hashCode(), a.hashCode());
        assertFalse(p.hashCode() == b.hashCode());
    }

    public void testIsExact() {
        assertTrue(p.isExact());
        assertFalse(q.isExact());
    }

    public void testZero() {
        final Point z = new Point(new Matrix(new int[][] {{0, 0, 0}}));
        assertEquals(z, p.zero());
    }

    public void testOne() {
        try {
            p.one();
            fail("should throw an UnsupportedOperationException");
        } catch (UnsupportedOperationException success) {
        }
    }

    public void testNegative() {
        final Point m = new Point(new Matrix(new int[][] {{-1, -2, -3}}));
        assertEquals(m, p.negative());
    }

    public void testInverse() {
        try {
            p.inverse();
            fail("should throw an UnsupportedOperationException");
        } catch (UnsupportedOperationException success) {
        }
    }

    public void testPlus() {
        try {
            p.plus(q);
            fail("should throw an UnsupportedOperationException");
        } catch (UnsupportedOperationException success) {
        }
    }

    public void testTimes() {
        final Point x = new Point(new Matrix(new double[][] {{3, 6, 9}}));
        assertEquals(x, p.times(3.0));
        final Point a = new Point(new Matrix(new int[][] {{2, 2, 2}}));
        assertEquals(a, p.times(new Operator(M)));
    }

    public void testCompareTo() {
        final Point a = new Point(new Matrix(new int[][] {{1, 2, 3}}));
        assertTrue(p.compareTo(q) < 0);
        assertTrue(q.compareTo(p) > 0);
        assertTrue(p.compareTo(a) == 0);
    }

    public void testFloor() {
        final Point a = new Point(new Matrix(new double[][] {{1.1, 2.2, 3.3}}));
        assertEquals(p, a.floor());
    }

    public void testToString() {
        final String s = "Point(1,2,3)";
        assertEquals(s, p.toString());
    }

    public void testPointMatrix() {
        final Point a = new Point(new Matrix(new int[][] {{1, 2, 3}}));
        assertEquals(p, a);
    }

    public void testPointIArithmeticArray() {
        final Point a = new Point(new IArithmetic[] { new Whole(1), new Whole(2),
                new Whole(3) });
        assertEquals(p, a);
    }

    public void testPointPoint() {
        final Point a = new Point(p);
        assertEquals(p, a);
    }

    public void testPointPointMatrix() {
        final Point a = new Point(new Matrix(new int[][] {{2, 2, 2}}));
        assertEquals(a, new Point(p, M));
    }

    public void testGetDimension() {
        assertEquals(3, p.getDimension());
    }

    public void testGet() {
        assertEquals(new Whole(2), p.get(1));
    }

    public void testGetCoordinates() {
        final Matrix A = new Matrix(new int[][] {{1, 2, 3}});
        assertEquals(A, p.getCoordinates());
    }
}
