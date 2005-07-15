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

package org.gavrog.jane.numbers;

import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Matrix;
import org.gavrog.jane.numbers.Real;
import org.gavrog.jane.numbers.Whole;

import junit.framework.TestCase;

/**
 * @author Olaf Delgado
 * @version $Id: TestMatrix.java,v 1.1.1.1 2005/07/15 21:58:40 odf Exp $
 */
public class TestMatrix extends TestCase {
    private Matrix A;
    private Matrix B;
    private Matrix C;
    private Matrix R;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        A = new Matrix(new int[][] { 
                {  1,  2,  3,  4 },
                {  5,  6,  7,  8 },
                {  9, 10, 11, 12 },
                { 13, 14, 15, 16 } }).mutableClone();
        B = new Matrix(new int[][] {
                { 1, 2, 3 },
                { 4, 5, 6 },
                { 7, 8, 9 } }).mutableClone();
        C = new Matrix(new int[][] {{-1}, {-2}, {-3}, {-4}});
        R = new Matrix(new int[][] {{-1, -2, -3, -4}});
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testHashCode() {
        //TODO Implement hashCode().
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
        final Matrix A = new Matrix(3, 3);
        A.set(0, 0, new FloatingPoint(0.8164965809277261));
        A.set(0, 1, new FloatingPoint(0.0));
        A.set(0, 2, new FloatingPoint(0.0));
        A.set(1, 0, new FloatingPoint(-0.47140452079103173));
        A.set(1, 1, new FloatingPoint(0.9428090415820632));
        A.set(1, 2, new FloatingPoint(0.0));
        A.set(2, 0, new FloatingPoint(-0.3333333333333335));
        A.set(2, 1, new FloatingPoint(-0.3333333333333332));
        A.set(2, 2, new FloatingPoint(1.0));

        final Real eps = new FloatingPoint(1e-12);
        final Matrix D = (Matrix) Matrix.one(3).minus(A.times(A.inverse()));
        assertTrue(D.norm().isLessOrEqual(eps));
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

    /*
     * Class under test for String toString()
     */
    public void testToString() {
        //TODO Implement toString().
    }

    public void testRplus() {
        //TODO Implement rplus().
    }

    /*
     * Class under test for IArithmetic minus(Object)
     */
    public void testMinusObject() {
        //TODO Implement minus().
    }

    public void testRminus() {
        //TODO Implement rminus().
    }

    public void testRtimes() {
        //TODO Implement rtimes().
    }

    public void testNorm() {
        //TODO Implement norm().
    }

    /*
     * Class under test for boolean equals(Object)
     */
    public void testEqualsObject() {
        //TODO Implement equals().
    }

    /*
     * Class under test for void Matrix(int, int)
     */
    public void testMatrixintint() {
        //TODO Implement Matrix().
    }

    /*
     * Class under test for void Matrix(IArithmetic[][])
     */
    public void testMatrixIArithmeticArrayArray() {
        //TODO Implement Matrix().
    }

    /*
     * Class under test for void Matrix(int[][])
     */
    public void testMatrixintArrayArray() {
        //TODO Implement Matrix().
    }

    /*
     * Class under test for void Matrix(long[][])
     */
    public void testMatrixlongArrayArray() {
        //TODO Implement Matrix().
    }

    /*
     * Class under test for void Matrix(double[][])
     */
    public void testMatrixdoubleArrayArray() {
        //TODO Implement Matrix().
    }

    public void testGetShape() {
        //TODO Implement getShape().
    }

    public void testMakeImmutable() {
        //TODO Implement makeImmutable().
    }

    public void testIsMutable() {
        //TODO Implement isMutable().
    }

    public void testGet() {
        //TODO Implement get().
    }

    public void testSet() {
        //TODO Implement set().
    }

    public void testIsScalar() {
        //TODO Implement isScalar().
    }

    public void testTransposed() {
        //TODO Implement transposed().
    }

    /*
     * Class under test for Object clone()
     */
    public void testClone() {
        //TODO Implement clone().
    }

    public void testMutableClone() {
        //TODO Implement mutableClone().
    }

    public void testScaled() {
        //TODO Implement scaled().
    }

    public void testRscaled() {
        //TODO Implement rscaled().
    }

    public void testTriangulate() {
        testTriangulate(new Matrix(new int[][] {{13, 21}, {34, 55}}));
        testTriangulate(new Matrix(new int[][] {{-13, 21}, {34, 55}}));
        testTriangulate(new Matrix(new int[][] {{34, 55}, {13, 21}}));
        testTriangulate(new Matrix(new int[][] {{34, 55}, {-13, 21}}));
    }
    
    private void testTriangulate(final Matrix A) {
        final IArithmetic det = A.get(0, 0).times(A.get(1, 1)).minus(
                A.get(1, 0).times(A.get(0, 1)));
        Matrix B, M;
        int sign;

        M = A.mutableClone();
        B = ((Matrix) A.one()).mutableClone();
        sign = Matrix.triangulate(M, B, false, true, 0);
        assertEquals(M, B.times(A));
        if (A.numberOfRows() == 2 && A.numberOfColumns() == 2) {
            assertEquals(det, M.get(0, 0).times(M.get(1, 1)).times(new Whole(sign)));
        }

        M = A.mutableClone();
        B = ((Matrix) A.one()).mutableClone();
        sign = Matrix.triangulate(M, B, true, true, 0);
        for (int i = 0; i < M.numberOfColumns(); ++i) {
            for (int j = 0; j < M.numberOfRows(); ++j) {
                assertTrue(M.get(i, j) instanceof Whole);
            }
        }
        for (int i = 0; i < B.numberOfColumns(); ++i) {
            for (int j = 0; j < B.numberOfRows(); ++j) {
                assertTrue(B.get(i, j) instanceof Whole);
            }
        }
        assertEquals(M, B.times(A));
        if (A.numberOfRows() == 2 && A.numberOfColumns() == 2) {
            assertEquals(det, M.get(0, 0).times(M.get(1, 1)).times(new Whole(sign)));
        }
    }

    public void testRank() {
        assertEquals(2, new Matrix(new int[][] {{1,2,3}, {4,5,6}, {7,8,9}}).rank());
        assertEquals(3, new Matrix(new int[][] {{1,2,3}, {4,5,6}, {8,8,9}}).rank());
    }

    public void testDeterminant() {
        assertEquals(Whole.ZERO, new Matrix(new int[][] { { 1, 2, 3 }, { 4, 5, 6 },
                { 7, 8, 9 } }).determinant());
        assertEquals(new Whole(-3), new Matrix(new int[][] { { 1, 2, 3 }, { 4, 5, 6 },
                { 8, 8, 9 } }).determinant());
    }
    
    public void testSolve() {
        final Matrix A1 = new Matrix(new int[][] {{1,2,3},{4,5,6},{7,8,9}});
        final Matrix A2 = new Matrix(new int[][] {{1,2,3},{4,5,6},{8,8,9}});
        final Matrix b1 = new Matrix(new int[][] {{6},{15},{24}});
        final Matrix b2 = new Matrix(new int[][] {{6},{15},{25}});
        testSolve(A1, b1, true);
        testSolve(A1, b2, false);
        testSolve(A2, b1, true);
        testSolve(A2, b2, true);
    }
    
    private void testSolve(final Matrix A, final Matrix b, final boolean hasSolution) {
        final Matrix A_saved = (Matrix) A.clone();
        final Matrix b_saved = (Matrix) b.clone();
        final Matrix x = Matrix.solve(A, b);
        assertEquals(A, A_saved);
        assertEquals(b, b_saved);
        if (hasSolution) {
            assertNotNull(x);
            assertEquals(b, A.times(x));
        } else {
            assertNull(x);
        }
    }
    
    public void test__getitem__() {
        //TODO Implement __getitem__().
    }

    /*
     * Class under test for void __setitem__(int[], IArithmetic)
     */
    public void test__setitem__intArrayIArithmetic() {
        //TODO Implement __setitem__().
    }

    /*
     * Class under test for void __setitem__(int[], long)
     */
    public void test__setitem__intArraylong() {
        //TODO Implement __setitem__().
    }

    /*
     * Class under test for void __setitem__(int[], double)
     */
    public void test__setitem__intArraydouble() {
        //TODO Implement __setitem__().
    }
    
    public void testGetSubMatrix() {
        final Matrix T = new Matrix(new int [][] {{10, 11, 12}, {14, 15, 16}});
        assertEquals(T, A.getSubMatrix(2, 1, 2, 3));
    }
    
    public void testSetSubMatrix() {
        final Matrix T = new Matrix(new int [][] {
                {  1,  2,  3,  4 },
                {  1,  2,  3,  8 },
                {  4,  5,  6, 12 },
                {  7,  8,  9, 16 } });
        A.setSubMatrix(1, 0, B);
        assertEquals(T, A);
    }
    
    public void testGetColumn() {
        final Matrix T = new Matrix(new int [][] {{3}, {7}, {11}, {15}});
        assertEquals(T, A.getColumn(2));
    }
    
    public void testSetColumn() {
        final Matrix T = new Matrix(new int [][] {
                {  1,  2, -1,  4 },
                {  5,  6, -2,  8 },
                {  9, 10, -3, 12 },
                { 13, 14, -4, 16 } });
        A.setColumn(2, C);
        assertEquals(T, A);
        try {
            A.setRow(2, C);
            fail("should throw an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
    }
    
    public void testGetRow() {
        final Matrix T = new Matrix(new int [][] {{13, 14, 15, 16}});
        assertEquals(T, A.getRow(3));
    }
    
    public void testSetRow() {
        final Matrix T = new Matrix(new int [][] {
                {  1,  2,  3,  4 },
                { -1, -2, -3, -4 },
                {  9, 10, 11, 12 },
                { 13, 14, 15, 16 } });
        A.setRow(1, R);
        assertEquals(T, A);
        assertEquals(T, A);
        try {
            A.setColumn(2, R);
            fail("should throw an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
    }
}
