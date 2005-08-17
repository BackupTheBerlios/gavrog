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
 * An operator acting by projective transformation.
 * 
 * @author Olaf Delgado
 * @version $Id: ProjectiveOperator.java,v 1.2 2005/08/17 02:01:17 odf Exp $
 */
public class ProjectiveOperator extends Matrix implements IOperator {
    final int dimension;

    /**
     * Creates a new operator.
     * 
     * @param M the matrix representation.
     */
    public ProjectiveOperator(final Matrix M) {
        super(M);
        this.dimension = M.numberOfRows() - 1;
        if (M.numberOfColumns() != this.dimension + 1) {
            throw new IllegalArgumentException("bad shape");
        }
    }

    /**
     * Creates a new operator.
     * 
     * @param A coordinates for the matrix representation.
     */
    public ProjectiveOperator(final IArithmetic[][] A) {
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
    public IOperator getLinearPart() {
        final int d = getDimension();
        return new LinearOperator(getSubMatrix(0, 0, d, d));
    }

    /* (non-Javadoc)
     * @see org.gavrog.joss.geometry.IOperator#getImageOfOrigin()
     */
    public Point getImageOfOrigin() {
        final int d = getDimension();
        return new Point(getSubMatrix(d, 0, 1, d+1));
    }

    /* (non-Javadoc)
     * @see org.gavrog.joss.geometry.IOperator#applyTo(org.gavrog.joss.geometry.IPoint)
     */
    public Point applyTo(final Point p) {
        if (p instanceof Point) {
            return new Point((Matrix) p.times(this));
        } else {
            final String msg = "not supported for " + p.getClass().getName();
            throw new UnsupportedOperationException(msg);
        }
    }
}
