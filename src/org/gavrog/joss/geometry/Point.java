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
 * A d-dimensional point in homogeneous coordinates represented by a row vector.
 * 
 * @author Olaf Delgado
 * @version $Id: Point.java,v 1.5 2005/08/18 02:45:32 odf Exp $
 */
public class Point extends ArithmeticBase implements IArithmetic {
    //TODO handle points at infinity gracefully
    final Matrix coords;
    final int dimension;

    /**
     * Creates a new point from a row matrix containing its cartesian
     * coordinates. The dimension d of the point created will correspond to the
     * number of columns in the given matrix. An extra coordinate of value 1
     * will be added internally.
     * 
     * @param M contains the coordinates for the point.
     */
    public Point(final Matrix M) {
        if (M.numberOfRows() != 1) {
            throw new IllegalArgumentException("matrix must have exactly 1 row");
        }
        final int d = M.numberOfColumns();
        this.dimension = d;
        this.coords = new Matrix(1, d + 1);
        this.coords.setSubMatrix(0, 0, M);
        this.coords.set(0, d, Whole.ONE);
    }

    /**
     * Creates a new point from its cartesian coordinates represented as an
     * array. The dimension d of the point created will correspond to the number
     * of an entries in the array. An extra coordinate of value 1 will be added
     * internally.
     * 
     * @param coordinates the coordinates for the point.
     */
    public Point(final IArithmetic[] coordinates) {
        this(new Matrix(new IArithmetic[][] {coordinates}));
    }
    
    /**
     * Creates a new point as a copy of a given one.
     * @param p the model point.
     */
    public Point(final Point p) {
        this.dimension = p.dimension;
        this.coords = p.coords;
    }
    
    /**
     * This constructor applies a matrix to a point. It is used by the Operator
     * class to apply a general projective operator to a point.
     * 
     * @param p the original point.
     * @param M the operator to apply as a (d+1)x(d+1) matrix.
     */
    Point(final Point p, final Matrix M) {
        this(image(p, M));
    }

    /**
     * Applies an operator to a point and normalizes the result by dividing
     * through the last coordinate.
     * 
     * @param p the original point.
     * @param M the operator to apply as a (d+1)x(d+1) matrix.
     * @return the resulting normalized point coordinates.
     */
    private static Matrix image(final Point p, final Matrix M) {
        final int d = p.getDimension();
        if (d != M.numberOfRows() - 1 || d != M.numberOfColumns() - 1) {
            throw new IllegalArgumentException("dimensions don't match");
        }
        final Matrix img = (Matrix) p.coords.times(M);
        final IArithmetic f = img.get(0, d);
        return ((Matrix) img.dividedBy(f)).getSubMatrix(0, 0, 1, d);
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
     * Retrieves a cartesian coordinate value for this point.
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
     * Retrieves all cartesian coordinate values for this point.
     * 
     * @return the coordinates as a row matrix.
     */
    public Matrix getCoordinates() {
        return this.coords.getSubMatrix(0, 0, 1, getDimension());
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final Object other) {
        if (other instanceof Point) {
            final Point p = (Point) other;
            if (getDimension() != p.getDimension()) {
                throw new IllegalArgumentException("dimensions must be equal");
            }
            for (int i = 0; i < getDimension(); ++i) {
                final int d = get(i).compareTo(p.get(i));
                if (d != 0) {
                    return d;
                }
            }
            return 0;
        } else {
            throw new IllegalArgumentException("can only compare two points");
        }
    }

    /**
     * This implementation of floor computes the floor component-wise.
     * 
     * @return a new point with all coordinates set to the floor of the originals.
     */
    public IArithmetic floor() {
        final int d = getDimension();
        final Matrix M = new Matrix(1, d);
        for (int i = 0; i < d; ++i) {
            M.set(0, i, get(i).floor());
        }
        return new Point(M);
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
        throw new UnsupportedOperationException("not defined on points");
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
        return new Point((Matrix) getCoordinates().negative());
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#one()
     */
    public IArithmetic one() {
        throw new UnsupportedOperationException("not defined on points");
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#plus(java.lang.Object)
     */
    public IArithmetic plus(final Object other) {
        throw new UnsupportedOperationException("not defined on points");
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#times(java.lang.Object)
     */
    public IArithmetic times(final Object other) {
        if (other instanceof Complex){
            return new Point((Matrix) getCoordinates().times(other));
        } else if (other instanceof Operator) {
            return ((Operator) other).applyTo(this);
        } else {
            throw new UnsupportedOperationException("operation not defined");
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        final StringBuffer tmp = new StringBuffer(1000);
        tmp.append("Point(");
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
        return new Point((Matrix) getCoordinates().zero());
    }
}