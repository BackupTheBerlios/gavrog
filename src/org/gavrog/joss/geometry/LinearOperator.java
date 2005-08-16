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
 * @version $Id: LinearOperator.java,v 1.5 2005/08/16 20:22:36 odf Exp $
 */
public class LinearOperator extends AffineOperator {
    /**
     * Extends a matrix as to be suitable as input for the super constructor.
     * More precisely, the output has a suitable (all zero) last row added.
     * 
     * @param M the input matrix.
     * @return the extended matrix.
     */
    private static Matrix extended(final Matrix M) {
        final int d = M.numberOfColumns();
        if (M.numberOfRows() != d) {
            throw new IllegalArgumentException("must have a square shape");
        }
        final Matrix out = new Matrix(d+1, d);
        out.setSubMatrix(0, 0, M);
        out.setRow(d, Matrix.zero(1, d));
        out.makeImmutable();
        return out;
    }
    
    /**
     * Creates a new operator.
     * 
     * @param M the matrix representation.
     */
    public LinearOperator(final Matrix M) {
        super(extended(M));
    }
    
    /**
     * Creates a new operator.
     * 
     * @param A coordinates for the matrix representation.
     */
    public LinearOperator(final IArithmetic[][] A) {
        super(extended(new Matrix(A)));
    }
}
