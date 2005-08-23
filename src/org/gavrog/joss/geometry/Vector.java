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
import org.gavrog.jane.numbers.Complex;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Whole;

/**
 * A d-dimensional vector represented by a row vector. To make interaction with
 * other geometry types easier, a zero coordinate is added internally.
 * 
 * @author Olaf Delgado
 * @version $Id: Vector.java,v 1.1 2005/08/23 02:03:41 odf Exp $
 */
public class Vector extends ArithmeticBase implements IArithmetic {
    final Matrix coords;
    final int dimension;

    /**
     * Creates a new vector from a row matrix containing its cartesian
     * coordinates. The dimension d of the vector created will correspond to the
     * number of columns in the given matrix. An extra coordinate of value 0
     * will be added internally.
     * 
     * @param M contains the coordinates for the vector.
     */
    public Vector(final Matrix M) {
        if (M.numberOfRows() != 1) {
            throw new IllegalArgumentException("matrix must have exactly 1 row");
        }
        final int d = M.numberOfColumns();
        this.dimension = d;
        this.coords = new Matrix(1, d + 1);
        this.coords.setSubMatrix(0, 0, M);
        this.coords.set(0, d, Whole.ZERO);
    }

    /**
     * Creates a new vector from its cartesian coordinates represented as an
     * array. The dimension d of the vector created will correspond to the
     * number of an entries in the array. An extra coordinate of value 0 will be
     * added internally.
     * 
     * @param coordinates the coordinates for the vector.
     */
    public Vector(final IArithmetic[] coordinates) {
        this(new Matrix(new IArithmetic[][] {coordinates}));
    }
    
    /**
     * Creates a new vector as a copy of a given one.
     * @param v the model vector.
     */
    public Vector(final Vector v) {
        this.dimension = v.dimension;
        this.coords = v.coords;
    }
    
    /**
     * This constructor applies a matrix to a vector. It is used by the
     * {@link #times(Object)} method to apply a general projective operator to
     * a vector.
     * 
     * @param v the original vector.
     * @param M the operator to apply as a (d+1)x(d+1) matrix.
     */
    private Vector(final Vector v, final Matrix M) {
        this(image(v, M));
    }

    /**
     * Applies an operator to a vector.
     * 
     * @param v the original vector.
     * @param M the operator to apply as a (d+1)x(d+1) matrix.
     * @return the resulting vector coordinates.
     */
    private static Matrix image(final Vector v, final Matrix M) {
        final int d = v.getDimension();
        if (d != M.numberOfRows() - 1 || d != M.numberOfColumns() - 1) {
            throw new IllegalArgumentException("dimensions don't match");
        }
        final Matrix img = (Matrix) v.coords.times(M);
        if (!img.get(0, d).isZero()) {
            throw new UnsupportedOperationException(
                    "the operation did not produce a vector");
        }
        return img;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.gavrog.joss.geometry.IPoint#getDimension()
     */
    public int getDimension() {
        return this.dimension;
    }
    
    /**
     * Retrieves a cartesian coordinate value for this vector.
     * 
     * @param i the index of the coordinate to retrieve.
     * @return the coordinate value.
     */
    public IArithmetic get(final int i) {
        if (i < 0 || i > getDimension()) {
            throw new IllegalArgumentException("index out of range");
        }
        return this.coords.get(0, i);
    }
    
    /**
     * Retrieves all cartesian coordinate values for this vector.
     * 
     * @return the coordinates as a row matrix.
     */
    public Matrix getCoordinates() {
        return this.coords.getSubMatrix(0, 0, 1, getDimension());
    }

    /**
     * Compares two vectors lexicographically.
     * 
     * @param other the vector to compare with.
     */
    public int compareTo(final Object other) {
        if (other instanceof Vector) {
            final Vector v = (Vector) other;
            if (getDimension() != v.getDimension()) {
                throw new IllegalArgumentException("dimensions must be equal");
            }
            for (int i = 0; i < getDimension(); ++i) {
                final int d = get(i).compareTo(v.get(i));
                if (d != 0) {
                    return d;
                }
            }
            return 0;
        } else {
            throw new IllegalArgumentException("can only compare two vectors");
        }
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#floor()
     */
    public IArithmetic floor() {
        throw new UnsupportedOperationException("not defined on vectors");
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
        throw new UnsupportedOperationException("not defined on vectors");
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#isExact()
     */
    public boolean isExact() {
        return coords.isExact();
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#negative()
     */
    public IArithmetic negative() {
        return new Vector((Matrix) getCoordinates().negative());
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#one()
     */
    public IArithmetic one() {
        throw new UnsupportedOperationException("not defined on vectors");
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#plus(java.lang.Object)
     */
    public IArithmetic plus(final Object other) {
        if (other instanceof Vector) {
            final Vector v = (Vector) other;
            return new Vector((Matrix) this.getCoordinates().plus(v.getCoordinates()));
        } else if (other instanceof Point){
            final Point p = (Point) other;
            return new Point((Matrix) this.getCoordinates().plus(p.getCoordinates()));
        } else {
            throw new UnsupportedOperationException("operation not defined");
        }
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#times(java.lang.Object)
     */
    public IArithmetic times(final Object other) {
        if (other instanceof Operator) {
            return new Vector(this, ((Operator) other).getCoordinates());
        } else if (other instanceof Complex) {
            return new Vector((Matrix) getCoordinates().times(other));
        } else {
            throw new UnsupportedOperationException("operation not defined");
        }
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#times(java.lang.Object)
     */
    public IArithmetic rtimes(final IArithmetic other) {
        if (other instanceof Complex) {
            return new Vector((Matrix) getCoordinates().times(other));
        } else {
            throw new UnsupportedOperationException("operation not defined");
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        final StringBuffer tmp = new StringBuffer(1000);
        tmp.append("Vector(");
        for (int i = 0; i < getDimension(); ++i) {
            if (i > 0) {
                tmp.append(",");
            }
            if (get(i) != null) {
                tmp.append(get(i).toString());
            }
        }
        tmp.append(")");
        return tmp.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.gavrog.jane.numbers.IArithmetic#zero()
     */
    public IArithmetic zero() {
        return new Vector((Matrix) getCoordinates().zero());
    }
}
