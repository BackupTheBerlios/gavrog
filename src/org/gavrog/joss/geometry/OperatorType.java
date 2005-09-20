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

import org.gavrog.jane.compounds.LinearAlgebra;
import org.gavrog.jane.compounds.Matrix;

/**
 * Encodes and determines the characteristica of the linear part of an operator
 * in a 2- or 3-dimensional crystallographic space group. When comparing two
 * objects, only the fields <code>dimension</code>,<code>order</code>,
 * <code>clockwise</code> and <code>orientationPreserving</code> are used.
 * 
 * @author Olaf Delgado
 * @version $Id: OperatorType.java,v 1.5 2005/09/20 04:19:18 odf Exp $
 */
public class OperatorType {
    final private int dimension;
    final private boolean orientationPreserving;
    final private int order;
    final private Vector axis;
    final private boolean clockwise;
    
    /**
     * Creates a new instance.
     * 
     * @param op the operator to analyze.
     */
    public OperatorType(final Operator op) {
        final int d = this.dimension = op.getDimension();
        Matrix M = op.getCoordinates().getSubMatrix(0, 0, d, d);

        this.orientationPreserving = M.determinant().isNonNegative();
        if (d == 3 && !this.orientationPreserving) {
            M = (Matrix) M.negative();
        }
        this.order = matrixOrder(M, 6);
        this.axis = getAxis(M);
        
        if (d == 2) {
            if (!this.isOrientationPreserving()) {
                this.clockwise = false;
            } else {
                if (this.order == 0 || this.order > 2) {
                    final Matrix v = new Matrix(new int[][] { { 1, 0 } });
                    final Matrix A = new Matrix(2, 2);
                    A.setRow(0, v);
                    A.setRow(1, (Matrix) v.times(M));
                    this.clockwise = A.determinant().isPositive();
                } else {
                    this.clockwise = true;
                }
            }
        } else if (d == 3) {
            if ((this.order == 0 || this.order > 2) && axis != null) {
                final Matrix a = axis.getCoordinates();
                final Matrix v;
                if (a.get(0, 1).isZero() && a.get(0, 2).isZero()) {
                    v = new Matrix(new int[][] { { 0, 1, 0 } });
                } else {
                    v = new Matrix(new int[][] { { 1, 0, 0 } });
                }
                final Matrix A = new Matrix(3, 3);
                A.setRow(0, axis.getCoordinates());
                A.setRow(1, v);
                A.setRow(2, (Matrix) v.times(M));
                this.clockwise = A.determinant().isPositive();
            } else {
                this.clockwise = true;
            }
        } else {
            final String msg = "operator dimension is " + d + ", must be 2 or 3";
            throw new UnsupportedOperationException(msg);
        }
    }
    
    /**
     * Given a matrix that leaves exactly a one-dimensional linear subspace
     * point-wise fixed, this method returns a vector representing that
     * subspace.
     * 
     * @param M the matrix to analyze.
     * @return the axis, if any, else <code>null</code>.
     */
    private static Vector getAxis(final Matrix M) {
        final Matrix Z = LinearAlgebra.rowNullSpace((Matrix) M.minus(M.one()), true);
        if (Z.numberOfRows() != 1) {
            return null;
        } else {
            final Vector v = new Vector(Z.getRow(0));
            if (v.isNegative()) {
                return (Vector) v.negative();
            } else {
                return v;
            }
        }
    }
    
    /**
     * Determines the order of a matrix.
     * 
     * @param M the matrix.
     * @param max the maximum order considered.
     * @return the order of the matrix or 0, if larger than the maximum.
     */
    private static int matrixOrder(final Matrix M, final int max) {
        Matrix A = M;
        for (int i = 1; i <= max; ++i) {
            if (A.isOne()) {
                return i;
            }
            A = (Matrix) A.times(M);
        }
        return 0;
    }
    
    /**
     * @return the dimension of the operator.
     */
    public int getDimension() {
        return dimension;
    }
    
    /**
     * @return the rotation (dimension 3) or mirror (dimension 2) axis.
     */
    public Vector getAxis() {
        return axis;
    }
    
    /**
     * @return true if rotation is clockwise.
     */
    public boolean isClockwise() {
        return clockwise;
    }
    
    /**
     * @return the rotation order.
     */
    public int getOrder() {
        return order;
    }
    
    /**
     * @return true if operator is orientation preserving.
     */
    public boolean isOrientationPreserving() {
        return orientationPreserving;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(final Object other) {
        if (other instanceof OperatorType) {
            final OperatorType type = (OperatorType) other;
            return this.dimension == type.dimension && this.clockwise == type.clockwise
                   && this.order == type.order
                   && this.orientationPreserving == type.orientationPreserving;
        } else {
            return false;
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        int res = dimension * 37 + order;
        res = res * 37 + (clockwise ? 37 : 0) + (orientationPreserving ? 1 : 0);
        return res;
    }
}