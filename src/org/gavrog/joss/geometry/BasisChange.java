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
import org.gavrog.jane.numbers.Whole;

/**
 * An basis change operator that can act on points, vectors and operators in
 * order to transform their representation into one fitting for a different
 * choice of basis and or/origin.
 * 
 * @author Olaf Delgado
 * @version $Id: BasisChange.java,v 1.1 2005/09/26 04:42:22 odf Exp $
 */
public class BasisChange extends ArithmeticBase implements IArithmetic {
    final Matrix coords;
    final Matrix inverse;
    final int dimension;
    
    /**
     * Creates a new basis transform. The input consists a matrix, the rows of
     * which represent the new basis vectors and a point specifying the new
     * origin.
     * 
     * @param basis the d x d matrix representing the new basis.
     * @param origin the new origin.
     */
    public BasisChange(final Matrix basis, final Point origin) {
        final int d = basis.numberOfRows();
        if (basis.numberOfColumns() != d) {
            throw new IllegalArgumentException("bad shape");
        }
        this.coords = new Matrix(d+1, d+1);
        this.coords.setSubMatrix(0, 0, basis);
        this.coords.setSubMatrix(d, 0, origin.getCoordinates());
        this.coords.setSubMatrix(0, d, Matrix.zero(d, 1));
        this.coords.set(d, d, Whole.ONE);
        this.inverse = (Matrix) this.coords.inverse();
        this.dimension = d;
    }

    /**
     * Quick and dirty constructor for internal use.
     * 
     * @param coords the coordination matrix for the new instance.
     * @param inverse the inverse of the coordination matrix.
     */
    private BasisChange(final Matrix coords, final Matrix inverse) {
        final int d = coords.numberOfRows() - 1;
        this.coords = coords;
        this.inverse = inverse;
        this.dimension = d;
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.ArithmeticBase#isExact()
     */
    public boolean isExact() {
        return this.coords.isExact();
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.ArithmeticBase#zero()
     */
    public IArithmetic zero() {
        throw new UnsupportedOperationException("operation not defined");
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.ArithmeticBase#one()
     */
    public IArithmetic one() {
        final int d = this.dimension;
        return new BasisChange(Matrix.one(d), Point.origin(d));
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.ArithmeticBase#negative()
     */
    public IArithmetic negative() {
        throw new UnsupportedOperationException("operation not defined");
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.ArithmeticBase#inverse()
     */
    public IArithmetic inverse() {
        return new BasisChange(this.inverse, this.coords);
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.ArithmeticBase#plus(java.lang.Object)
     */
    public IArithmetic plus(final Object other) {
        throw new UnsupportedOperationException("operation not defined");
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.ArithmeticBase#times(java.lang.Object)
     */
    public IArithmetic times(final Object other) {
        if (other instanceof BasisChange) {
            final BasisChange bt = (BasisChange) other;
            return new BasisChange((Matrix) bt.coords.times(this.coords),
                    (Matrix) this.inverse.times(bt.inverse));
        } else if (other instanceof IArithmetic) {
            return ((IArithmetic) other).rtimes(this);
        } else {
            throw new UnsupportedOperationException("operation not defined");
        }
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#rtimes(org.gavrog.jane.numbers.IArithmetic)
     */
    public IArithmetic rtimes(final IArithmetic other) {
        if (other instanceof Point) {
            return new Point((Point) other, this.inverse);
        } else if (other instanceof Vector) {
            return new Vector((Vector) other, this.inverse);
        } else if (other instanceof Operator) {
            final Operator op = (Operator) other;
            return new Operator((Matrix) this.coords.times(op.getCoordinates()).times(
                    this.inverse));
        } else {
            throw new UnsupportedOperationException("operation not defined");
        }
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.ArithmeticBase#compareTo(java.lang.Object)
     */
    public int compareTo(final Object other) {
        if (other instanceof BasisChange) {
            final BasisChange ob = (BasisChange) other;
            final int dim = getDimension();
            if (dim != ob.getDimension()) {
                throw new IllegalArgumentException("dimensions must be equal");
            }
            final Matrix A = this.coords;
            final Matrix B = ob.coords;
            for (int i = 0; i < dim+1; ++i) {
                for (int j = 0; j < dim+1; ++j) {
                    final int d = A.get(i, j).compareTo(B.get(i, j));
                    if (d != 0) {
                        return d;
                    }
                }
            }
            return 0;
        } else {
            throw new IllegalArgumentException("illegal argument type");
        }
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.ArithmeticBase#floor()
     */
    public IArithmetic floor() {
        throw new UnsupportedOperationException();
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.ArithmeticBase#toString()
     */
    public String toString() {
        final StringBuffer buf = new StringBuffer(60);
        buf.append("BasisChange(");
        buf.append(getBasis().toString());
        buf.append(",");
        buf.append(getOrigin().toString());
        buf.append(")");
        return buf.toString();
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.ArithmeticBase#hashCode()
     */
    public int hashCode() {
        return coords.hashCode();
    }
    
    /**
     * Returns the dimension of the underlying space.
     * 
     * @return the dimension.
     */
    public int getDimension() {
        return this.dimension;
    }
    
    /**
     * Returns the new basis in terms of the old one.
     * 
     * @return the new basis.
     */
    public Matrix getBasis() {
        final int d = getDimension();
        return this.coords.getSubMatrix(0, 0, d, d);
    }
    
    /**
     * Returns the new origin in terms of the old origin and basis.
     * 
     * @return the new origin.
     */
    public Point getOrigin() {
        final int d = getDimension();
        return new Point(this.coords.getSubMatrix(d, 0, 1, d));
    }
}
