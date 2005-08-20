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
import org.gavrog.jane.numbers.Fraction;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.pgraphs.io.DataFormatException;

/**
 * Unit tests for the Operator class.
 * 
 * @author Olaf Delgado
 * @version $Id: TestOperator.java,v 1.5 2005/08/20 04:59:02 odf Exp $
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
        final Matrix A = new Matrix(new double[][] {{0, 1, 0}, {1, 0, 0}, {1, 0, 1}});
        final Operator opA = new Operator(A);
        assertTrue(op1.isExact());
        assertFalse(opA.isExact());
    }

    public void testZero() {
        try {
            op1.zero();
            fail("should throw an UnsupportedOperationException");
        } catch (UnsupportedOperationException success) {
        }
    }

    public void testOne() {
        assertEquals(op1, op1.times(op1.one()));
    }

    public void testNegative() {
        try {
            op1.negative();
            fail("should throw an UnsupportedOperationException");
        } catch (UnsupportedOperationException success) {
        }
    }

    public void testInverse() {
        assertEquals(op1.one(), op1.times(op1.inverse()));
    }

    public void testIdentity() {
        assertEquals(new Operator("x,y,z"), Operator.identity(3));
    }
    
    public void testPlus() {
        try {
            op1.plus(op1);
            fail("should throw an UnsupportedOperationException");
        } catch (UnsupportedOperationException success) {
        }
    }

    public void testTimes() {
        final Matrix A = new Matrix(new int[][] {{-1, 0, 0}, {0, -1, 0}, {1, 1, 1}});
        final Operator opA = new Operator(new Matrix(A));
        assertEquals(opA, op1.times(op1));
    }

    public void testCompareTo() {
        final Matrix A = new Matrix(new int[][] {{0, 1, 0}, {-1, 0, 0}, {2, 0, 1}});
        final Matrix B = new Matrix(new int[][] {{0, 1, 0}, {-1, 0, 0}, {1, 0, 1}});
        final Operator opA = new Operator(new Matrix(A));
        final Operator opB = new Operator(new Matrix(B));
        assertTrue(op1.compareTo(opA) < 0);
        assertTrue(opA.compareTo(op1) > 0);
        assertTrue(opB.compareTo(op1) == 0);
    }

    public void testFloor() {
        final Matrix A = new Matrix(new double[][] { { 0, 1.1, 0 }, { -1, 0, 0 },
                { 1.1, 0, 1 } });
        final Matrix B = new Matrix(new double[][] { { 0, 1.1, 0 }, { -1, 0, 0 },
                { 1, 0, 1 } });
        final Operator opA = new Operator(new Matrix(A));
        final Operator opB = new Operator(new Matrix(B));
        assertEquals(opB, opA.floor());
    }

    public void testMod1() {
        final Matrix A = new Matrix(new double[][] { { 0, 1.1, 0 }, { -1, 0, 0 },
                { 1.25, 0, 1 } });
        final Matrix B = new Matrix(new double[][] { { 0, 1.1, 0 }, { -1, 0, 0 },
                { .25, 0, 1 } });
        final Operator opA = new Operator(new Matrix(A));
        final Operator opB = new Operator(new Matrix(B));
        //CAVEAT: this test only works if the numbers used can be represented precisely
        assertEquals(opB, opA.mod(1));
    }

    public void testToString() {
        final String s = "Operator([[0,1,0],[-1,0,0],[1,0,1]])";
        assertEquals(s, op1.toString());
    }

    public void testOperatorMatrix() {
        final Matrix A = new Matrix(new int[][] {{0, 2, 0}, {-2, 0, 0}, {2, 0, 2}});
        final Operator opA = new Operator(A);
        assertEquals(op1, opA);
    }

    public void testOperatorIArithmeticArrayArray() {
        final IArithmetic A[][] = new IArithmetic[][] {
                {Whole.ZERO, Whole.ONE, Whole.ZERO},
                {new Whole(-1), Whole.ZERO, Whole.ZERO},
                {Whole.ONE, Whole.ZERO, Whole.ONE}
        };
        final Operator opA = new Operator(A);
        assertEquals(op1, opA);
    }

    public void testOperatorString() {
        final Operator opA = new Operator("1-y,x");
        assertEquals(op1, opA);
    }
    
    public void testGetDimension() {
        assertEquals(2, op1.getDimension());
    }

    public void testGet() {
        assertEquals(new Whole(-1), op1.get(1, 0));
    }

    public void testGetCoordinates() {
        assertEquals(M, op1.getCoordinates());
    }

    public void testGetLinearPart() {
        final Matrix A = new Matrix(new int[][] {{0, 1, 0}, {-1, 0, 0}, {0, 0, 1}});
        final Operator opA = new Operator(A);
        assertEquals(opA, op1.getLinearPart());
    }

    public void testGetImageOfOrigin() {
        final Point p = new Point(new Matrix(new int[][] {{1, 0}}));
        assertEquals(p, op1.getImageOfOrigin());
    }

    public void testApplyTo() {
        final Point x = new Point(new Matrix(new int[][] {{1, 0}}));
        final Point xy = new Point(new Matrix(new int[][] {{1, 1}}));
        final Point y = new Point(new Matrix(new int[][] {{0, 1}}));
        assertEquals(xy, op1.applyTo(x));
        assertEquals(y, op1.applyTo(xy));
        assertEquals(y.zero(), op1.applyTo(y));
        assertEquals(x, op1.applyTo((Point) y.zero()));
    }

    public void testParseOperator() {
        String s;
        Matrix M;
        
        s = "x-4y+7*z-10, +5/3y-8z+11-2x, +3*x+ 9z-6y - 12";
        M = new Matrix(new int[][] {
                {  1, -2,   3, 0},
                { -4,  5,  -6, 0},
                {  7, -8,   9, 0},
                {-10, 11, -12, 1}}).mutableClone();
        M.set(1, 1, new Fraction(5, 3));
        assertEquals(M, Operator.parse(s));
        assertFalse(Operator.parse(s).isMutable());
        
        assertEquals(Matrix.one(4), Operator.parse("x,y,z"));
        
        try {
            Operator.parse("1,2,3,4");
            fail("should throw an DataFormatException");
        } catch (DataFormatException success) {
        }
        
        try {
            Operator.parse("a,2,3");
            fail("should throw an DataFormatException");
        } catch (DataFormatException success) {
        }
        
        try {
            Operator.parse("1,2/,3");
            fail("should throw an DataFormatException");
        } catch (DataFormatException success) {
        }
        
        try {
            Operator.parse("x+3x,2,3");
            fail("should throw an DataFormatException");
        } catch (DataFormatException success) {
        }
    }
}
