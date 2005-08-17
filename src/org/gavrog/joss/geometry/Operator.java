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
 * An operator acting by projective transformation.
 * 
 * @author Olaf Delgado
 * @version $Id: Operator.java,v 1.1 2005/08/17 02:50:12 odf Exp $
 */
public class Operator extends ArithmeticBase implements IArithmetic {
    //TODO some methods will require the operator to be normalized
    final Matrix coords;
    final int dimension;

    /**
     * Creates a new operator.
     * 
     * @param M the matrix representation.
     */
    public Operator(final Matrix M) {
        this.dimension = M.numberOfRows() - 1;
        if (M.numberOfColumns() != this.dimension + 1) {
            throw new IllegalArgumentException("bad shape");
        }
        this.coords = new Matrix(M);
    }

    /**
     * Creates a new operator.
     * 
     * @param A coordinates for the matrix representation.
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

    /* (non-Javadoc)
     * @see org.gavrog.joss.geometry.IOperator#getLinearPart()
     */
    public Operator getLinearPart() {
        //TODO reimplement this
        final int d = getDimension();
        return new LinearOperator(coords.getSubMatrix(0, 0, d, d));
    }

    /* (non-Javadoc)
     * @see org.gavrog.joss.geometry.IOperator#getImageOfOrigin()
     */
    public Point getImageOfOrigin() {
        //TODO reimplement this
        final int d = getDimension();
        return new Point(coords.getSubMatrix(d, 0, 1, d+1));
    }

    //TODO make all the remaining methods make sense
    public int compareTo(Object other) {
        return coords.compareTo(other);
    }

    public IArithmetic floor() {
        return coords.floor();
    }

    public int hashCode() {
        return coords.hashCode();
    }

    public IArithmetic inverse() {
        return coords.inverse();
    }

    public boolean isExact() {
        return coords.isExact();
    }

    public IArithmetic negative() {
        return coords.negative();
    }

    public IArithmetic one() {
        return coords.one();
    }

    public IArithmetic plus(Object other) {
        return coords.plus(other);
    }

    public IArithmetic times(Object other) {
        return coords.times(other);
    }

    public String toString() {
        return coords.toString();
    }

    public IArithmetic zero() {
        return coords.zero();
    }
}
