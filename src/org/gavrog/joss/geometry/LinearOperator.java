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
 * An operator acting by linear transformation.
 * 
 * @author Olaf Delgado
 * @version $Id: LinearOperator.java,v 1.1 2005/08/04 03:54:38 odf Exp $
 */
public class LinearOperator extends Matrix implements IOperator {
    final int dimension;

    /**
     * Creates a new operator.
     * 
     * @param A coordinates for the matrix representation.
     */
    public LinearOperator(final IArithmetic[][] A) {
        super(A);
        this.dimension = A.length;
        if (A[0].length != this.dimension) {
            throw new IllegalArgumentException("argument must be square");
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
        return this;
    }

    /* (non-Javadoc)
     * @see org.gavrog.joss.geometry.IOperator#getImageOfOrigin()
     */
    public IPoint getImageOfOrigin() {
        final IArithmetic v[] = new IArithmetic[getDimension()];
        for (int i = 0; i < getDimension(); ++i) {
            v[i] = Whole.ZERO;
        }
        return new PointCartesian(v);
    }

    /* (non-Javadoc)
     * @see org.gavrog.joss.geometry.IOperator#applyTo(org.gavrog.joss.geometry.IPoint)
     */
    public IPoint applyTo(IPoint p) {
        // TODO Auto-generated method stub
        return null;
    }
}
