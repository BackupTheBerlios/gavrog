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
 * An operator acting by affine transformation.
 * 
 * @author Olaf Delgado
 * @version $Id: AffineOperator.java,v 1.4 2005/08/17 02:50:12 odf Exp $
 */
public class AffineOperator extends Operator {
    /**
     * Extends a matrix as to be suitable as input for the super constructor.
     * More precisely, the output has a suitable last column added.
     * 
     * @param M the input matrix.
     * @return the extended matrix.
     */
    private static Matrix extended(final Matrix M) {
        final int d = M.numberOfColumns();
        if (M.numberOfRows() != d + 1) {
            throw new IllegalArgumentException("must have a (d+1, d) shape");
        }
        final Matrix out = new Matrix(d+1, d+1);
        out.setSubMatrix(0, 0, M);
        out.setColumn(d, Matrix.zero(d+1, 1));
        out.set(d, d, Whole.ONE);
        out.makeImmutable();
        return out;
    }
    
    /**
     * Creates a new operator.
     * 
     * @param M the matrix representation.
     */
    public AffineOperator(final Matrix M) {
        super(extended(M));
    }
    
    /**
     * Creates a new operator.
     * 
     * @param coords coordinates for the matrix representation.
     */
    public AffineOperator(final IArithmetic[][] coords) {
        super(extended(new Matrix(coords)));
    }
}
