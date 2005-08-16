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
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Whole;

/**
 * A d-dimensional point in cartesian coordinates represented by a row vector.
 * 
 * @author Olaf Delgado
 * @version $Id: PointCartesian.java,v 1.3 2005/08/16 19:43:59 odf Exp $
 */
public class PointCartesian extends PointHomogeneous {
    final int dimension;

    /**
     * Extends a matrix as to be suitable as input for the super constructor.
     * More precisely, the output has a 1 added as its last column.
     * 
     * @param M the input matrix.
     * @return the extended matrix.
     */
    private static Matrix extended(final Matrix M) {
        if (M.numberOfRows() != 1) {
            throw new IllegalArgumentException("matrix must have exactly 1 row");
        }
        final int d = M.numberOfColumns();
        final Matrix out = new Matrix(1, d + 1);
        out.setSubMatrix(0, 0, M);
        out.set(0, d, new Whole(1));
        out.makeImmutable();
        return out;
    }
    
    /**
     * Extends an array as to be suitable as input for the super constructor.
     * More precisely, the output is converted to a matrix (not strictly
     * necessary) and has a 1 added as its last column.
     * 
     * @param coords the input array.
     * @return the extended array as a matrix.
     */
    private static Matrix extended(final IArithmetic[] coords) {
        final int d = coords.length;
        final Matrix out = new Matrix(1, d + 1);
        for (int i = 0; i < d; ++i) {
            out.set(0, i, coords[i]);
        }
        out.set(0, d, new Whole(1));
        out.makeImmutable();
        return out;
    }
    
    /**
     * Create a new point.
     * 
     * @param M contains the coordinates for the point.
     */
    public PointCartesian(final Matrix M) {
        super(extended(M));
        this.dimension = M.numberOfColumns();
    }
    
    /**
     * Create a new point.
     * 
     * @param coords the coordinates for the point.
     */
    public PointCartesian(final IArithmetic[] coords) {
        super(extended(coords));
        this.dimension = coords.length;
    }
    
    /**
     * Create a new point from a given one.
     * @param p the point to copy.
     */
    public PointCartesian(final PointHomogeneous p) {
        super(p.normalized());
        this.dimension = p.getDimension();
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.joss.geometry.IPoint#getDimension()
     */
    public int getDimension() {
        return this.dimension;
    }

    /* (non-Javadoc)
     * @see org.gavrog.joss.geometry.IPoint#apply(org.gavrog.joss.geometry.IOperator)
     */
    public IPoint apply(final IOperator op) {
        return op.applyTo(this);
    }
}
