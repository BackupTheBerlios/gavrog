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
 * An operator acting by linear transformation.
 * 
 * @author Olaf Delgado
 * @version $Id: AffineOperator.java,v 1.2 2005/08/15 18:33:56 odf Exp $
 */
public class AffineOperator extends Matrix implements IOperator {
    final int dimension;

    /**
     * Creates a new operator.
     * 
     * @param A coordinates for the matrix representation.
     */
    public AffineOperator(final IArithmetic[][] A) {
        super(A);
        this.dimension = A.length - 1;
        if (A[0].length != this.dimension) {
            throw new IllegalArgumentException("bad shape");
        }
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
    public IOperator getLinearPart() {
        final int d = getDimension();
        return new LinearOperator(getSubMatrix(0, 0, d, d));
    }

    /* (non-Javadoc)
     * @see org.gavrog.joss.geometry.IOperator#getImageOfOrigin()
     */
    public IPoint getImageOfOrigin() {
        return new PointCartesian(getRow(getDimension()));
    }

    /* (non-Javadoc)
     * @see org.gavrog.joss.geometry.IOperator#applyTo(org.gavrog.joss.geometry.IPoint)
     */
    public IPoint applyTo(final IPoint p) {
        if (p instanceof PointCartesian) {
            return new PointCartesian((Matrix) p.times(getLinearPart()).plus(getImageOfOrigin()));
        } else if (p instanceof PointHomogeneous) {
            return new PointHomogeneous((Matrix) p.times(this));
        } else {
            final String msg = "not supported for " + p.getClass().getName();
            throw new UnsupportedOperationException(msg);
        }
    }
}
