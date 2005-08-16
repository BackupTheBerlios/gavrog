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
 * A d-dimensional point in homogeneous coordinates represented by a row vector.
 * 
 * @author Olaf Delgado
 * @version $Id: PointHomogeneous.java,v 1.3 2005/08/16 19:43:59 odf Exp $
 */
public class PointHomogeneous extends Matrix implements IPoint {
    final int dimension;

    /**
     * Create a new point.
     * 
     * @param M contains the coordinates for the point.
     */
    public PointHomogeneous(final Matrix M) {
        super(M);
        if (M.numberOfRows() != 1) {
            throw new IllegalArgumentException("matrix must have exactly 1 row");
        }
        this.dimension = M.numberOfColumns() - 1;
    }
    
    /**
     * Create a new point from its coordinates.
     * 
     * @param coordinates the coordinates for the point.
     */
    public PointHomogeneous(final IArithmetic[] coordinates) {
        super(1, coordinates.length);
        this.dimension = coordinates.length - 1;
        for (int i = 0; i < this.dimension; ++i) {
            set(0, i, coordinates[i]);
        }
        makeImmutable();
    }
    
    /**
     * Create a new point from a given one.
     * @param p the point to copy.
     */
    public PointHomogeneous(final PointHomogeneous p) {
        super(p);
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

    /**
     * Normalizes a point by dividing its representation by the last coordinate.
     * @return the normalized point.
     */
    public PointHomogeneous normalized() {
        final IArithmetic f = this.get(0, getDimension());
        if (f.isZero()) {
            throw new IllegalArgumentException("point is at infinity");
        }
        return new PointHomogeneous((Matrix) this.dividedBy(f));
    }
    
    /**
     * Checks if this point is at infinity, which is reflected by its last
     * coordinate being 0.
     * 
     * @return true if this point is at infinity.
     */
    public boolean isAtInfinity() {
        return this.get(0, getDimension()).isZero();
    }
}
