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

/**
 * A d-dimensional point in cartesian coordinates represented by a row vector.
 * 
 * @author Olaf Delgado
 * @version $Id: PointCartesian.java,v 1.2 2005/08/04 06:06:46 odf Exp $
 */
public class PointCartesian extends Matrix implements IPoint {
    final int dimension;

    /**
     * Create a new point.
     * 
     * @param M contains the coordinates for the point.
     */
    public PointCartesian(final Matrix M) {
        super(M);
        if (M.numberOfRows() != 1) {
            throw new IllegalArgumentException("matrix must have exactly 1 row");
        }
        this.dimension = M.numberOfColumns();
    }
    
    /**
     * Create a new point.
     * 
     * @param coordinates the coordinates for the point.
     */
    public PointCartesian(final IArithmetic[] coordinates) {
        super(1, coordinates.length);
        this.dimension = coordinates.length;
        for (int i = 0; i < this.dimension; ++i) {
            set(0, i, coordinates[i]);
        }
        makeImmutable();
    }
    
    /**
     * Create a new point from a given one.
     * @param p the point to copy.
     */
    public PointCartesian(final IPoint p) {
        super(1, p.getDimension());
        this.dimension = p.getDimension();
        if (p instanceof PointCartesian) {
            setRow(0, ((PointCartesian) p).getRow(0));
        } else if (p instanceof PointHomogeneous) {
            final Matrix P = (Matrix) p;
            final IArithmetic f = P.get(0, this.dimension);
            if (f.isZero()) {
                throw new IllegalArgumentException("point at infinity");
            }
            for (int i = 0; i < this.dimension; ++i) {
                set(0, i, P.get(0, i).dividedBy(f));
            }
        } else {
            final String msg = "not supported for " + p.getClass().getName();
            throw new UnsupportedOperationException(msg);
        }
        makeImmutable();
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
