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
 * @version $Id: Point.java,v 1.1 2005/08/17 02:01:17 odf Exp $
 */
public class Point extends ArithmeticBase implements IArithmetic {
    Matrix coords;
    final int dimension;
    boolean normalized;

    /**
     * Creates a new point.
     * 
     * @param M contains the coordinates for the point.
     */
    public Point(final Matrix M) {
        if (M.numberOfRows() != 1) {
            throw new IllegalArgumentException("matrix must have exactly 1 row");
        }
        final int d = M.numberOfColumns();
        this.dimension = d;
        this.coords = new Matrix(1, d+1);
        this.coords.setSubMatrix(0, 0, M);
        this.coords.set(0, d, Whole.ONE);
        this.normalized = true;
    }
    
    /**
     * Creates a new point from its coordinates.
     * 
     * @param coordinates the coordinates for the point.
     */
    public Point(final IArithmetic[] coordinates) {
        this(new Matrix(new IArithmetic[][] {coordinates}));
    }
    
    /**
     * Creates a new point from a given one.
     * @param p the point to copy.
     */
    public Point(final Point p) {
        this.dimension = p.dimension;
        this.coords = p.coords;
        this.normalized = p.normalized;
    }
    
    /**
     * Normalizes the representation of this point by dividing it by the final
     * coordinate.
     */
    private void normalize() {
        if (!this.normalized) {
            final IArithmetic f = this.coords.get(0, getDimension());
            if (!f.isOne() && !f.isZero()) {
                this.coords = (Matrix) this.coords.dividedBy(f);
            }
            this.normalized = true;
        }
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.joss.geometry.IPoint#getDimension()
     */
    public int getDimension() {
        return this.dimension;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final Object other) {
        //TODO compare points lexicographically
        return 0;
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#floor()
     */
    public IArithmetic floor() {
        //TODO implement by taking floor of every coordinate
        return null;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return coords.hashCode();
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
        return new Point((Matrix) coords.negative());
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
        if (other instanceof IOperator) {
            return ((IOperator) other).applyTo(this);
        } else if (other instanceof Complex){
            return new Point((Matrix) coords.times(other));
        } else if (other instanceof IArithmetic){
            return ((IArithmetic) other).rtimes(this);
        } else {
            throw new UnsupportedOperationException("operation not defined");
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        normalize();
        final StringBuffer tmp = new StringBuffer(1000);
        tmp.append("Point([");
        for (int j = 0; j < getDimension(); ++j) {
            if (j > 0) {
                tmp.append(",");
            }
            if (coords.get(0, j) != null) {
                tmp.append(coords.get(0, j).toString());
            }
        }
        tmp.append("])");
        return tmp.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.gavrog.jane.numbers.IArithmetic#zero()
     */
    public IArithmetic zero() {
        return new Point((Matrix) coords.zero());
    }
    
    /**
     * Retrieves a coordinate value from this point's representation.
     * 
     * @param i the index of the coordinate to retrieve.
     * @return the coordinate value.
     */
    public IArithmetic get(final int i) {
        if (i < 0 || i > getDimension()) {
            throw new IllegalArgumentException("index out of range");
        }
        normalize();
        return this.coords.get(0, i);
    }
}
