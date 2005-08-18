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

import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.ArithmeticBase;
import org.gavrog.jane.numbers.IArithmetic;

/**
 * An operator acting on d-dimensional space by projective transformation.
 * Operators are represented by (d+1)x(d+1) matrices which are to be applied to
 * a point in homogeneous coordinates by multiplication from the right.
 * 
 * @author Olaf Delgado
 * @version $Id: Operator.java,v 1.4 2005/08/18 02:45:32 odf Exp $
 */
public class Operator extends ArithmeticBase implements IArithmetic {
    Matrix coords;
    final int dimension;
    boolean normalized;

    /**
     * Creates a new operator.
     * 
     * @param M the (d+1)x(d+1) matrix representation.
     */
    public Operator(final Matrix M) {
        final int d = M.numberOfRows() - 1;
        if (M.numberOfColumns() != d + 1) {
            throw new IllegalArgumentException("bad shape");
        }
        this.coords = new Matrix(M);
        this.dimension = d;
        final IArithmetic f = M.get(d, d);
        this.normalized = f.isZero() || f.isOne();
    }

    /**
     * Creates a new operator.
     * 
     * @param A coordinates for the (d+1)x(d+1) matrix representation.
     */
    public Operator(final IArithmetic[][] A) {
        this(new Matrix(A));
    }

    /* (non-Javadoc)
     * @see org.gavrog.joss.geometry.IOperator#getDimension()
     */
    public int getDimension() {
        return this.dimension;
    }

    /**
     * Normalizes the representation of this operator by dividing it by the
     * final coordinate.
     */
    private void normalize() {
        if (!this.normalized) {
            final int d = getDimension();
            final IArithmetic f = this.coords.get(d, d);
            this.coords = (Matrix) this.coords.dividedBy(f);
            this.normalized = true;
            this.coords.makeImmutable();
        }
    }
    
    /**
     * Retrieves an entry of the normalized matrix representation of this
     * operator.
     * 
     * @param i the row index.
     * @param j the column index.
     * @return the coordinate value.
     */
    public IArithmetic get(final int i, final int j) {
        if (i < 0 || i > getDimension() + 1) {
            throw new IllegalArgumentException("row index out of range");
        }
        if (j < 0 || j > getDimension() + 1) {
            throw new IllegalArgumentException("column index out of range");
        }
        normalize();
        return this.coords.get(i, j);
    }
    
    /**
     * Retrieves all cartesian coordinate values for this point.
     * 
     * @return the coordinates as a row matrix.
     */
    public Matrix getCoordinates() {
        normalize();
        return this.coords;
    }

    /**
     * Creates an operator representing the linear portion of this one.
     * 
     * @return the linear operator.
     */
    public Operator getLinearPart() {
        normalize();
        final int d = getDimension();
        final Matrix M = this.coords.mutableClone();
        M.setSubMatrix(d, 0, Matrix.zero(1, d));
        M.setSubMatrix(0, d, Matrix.zero(d, 1));
        return new Operator(M);
    }

    /**
     * Returns the image of the coordinate origin, or, in other words, the
     * translational component of this operator.
     * 
     * @return the translation component represented as a point.
     */
    public Point getImageOfOrigin() {
        normalize();
        final int d = getDimension();
        return new Point(this.coords.getSubMatrix(d, 0, 1, d));
    }

    /**
     * Applies this operator to a point.
     * 
     * @param p the point to apply to.
     * @return the resulting point.
     */
    public IArithmetic applyTo(final Point p) {
        return new Point(p, this.coords);
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final Object other) {
        if (other instanceof Operator) {
            final Operator op = (Operator) other;
            final int dim = getDimension();
            if (dim != op.getDimension()) {
                throw new IllegalArgumentException("dimensions must be equal");
            }
            for (int i = 0; i < dim+1; ++i) {
                for (int j = 0; j < dim+1; ++j) {
                    final int d = get(i, j).compareTo(op.get(i, j));
                    if (d != 0) {
                        return d;
                    }
                }
            }
            return 0;
        } else {
            throw new IllegalArgumentException("can only compare two operators");
        }
    }

    /**
     * This implementation of floor computes the floor component-wise, but only
     * for the translational portion of the operator.
     * 
     * @return the modified operator.
     */
    public IArithmetic floor() {
        normalize();
        final int d = getDimension();
        final Matrix M = this.coords.mutableClone();
        for (int i = 0; i < d; ++i) {
            M.set(d, i, get(d, i).floor());
        }
        return new Operator(M);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return getCoordinates().hashCode();
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#inverse()
     */
    public IArithmetic inverse() {
        return new Operator((Matrix) this.coords.inverse());
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#isExact()
     */
    public boolean isExact() {
        return this.coords.isExact();
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#negative()
     */
    public IArithmetic negative() {
        throw new UnsupportedOperationException("operation not defined");
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#one()
     */
    public IArithmetic one() {
        return new Operator((Matrix) this.coords.one());
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#plus(java.lang.Object)
     */
    public IArithmetic plus(final Object other) {
        throw new UnsupportedOperationException("operation not defined");
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#times(java.lang.Object)
     */
    public IArithmetic times(final Object other) {
        if (other instanceof Operator) {
            final Matrix M = ((Operator) other).coords;
            return new Operator((Matrix) this.coords.times(M));
        } else {
            throw new UnsupportedOperationException("operation not defined");
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return this.coords.toString().replaceFirst("Matrix", "Operator");
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#zero()
     */
    public IArithmetic zero() {
        throw new UnsupportedOperationException("operation not defined");
    }
}
