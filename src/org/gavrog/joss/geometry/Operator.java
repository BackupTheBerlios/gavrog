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
import org.gavrog.jane.numbers.Real;
import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.pgraphs.io.DataFormatException;

/**
 * An operator acting on d-dimensional space by projective transformation.
 * Operators are represented by (d+1)x(d+1) matrices which are to be applied to
 * a point in homogeneous coordinates by multiplication from the right.
 * 
 * @author Olaf Delgado
 * @version $Id: Operator.java,v 1.10 2005/08/23 02:03:41 odf Exp $
 */
public class Operator extends ArithmeticBase implements IArithmetic {
    //TODO handle zero scale entry gracefully
    final Matrix coords;
    final int dimension;

    /**
     * Creates a new operator.
     * 
     * @param M the (d+1)x(d+1) matrix representation.
     */
    public Operator(final Matrix M) {
        final int d = M.numberOfRows() - 1;
        if (M.numberOfColumns() != d + 1) {
            throw new IllegalArgumentException("bad shape");
        }
        final IArithmetic f = M.get(d, d);
        this.coords = (Matrix) M.dividedBy(f);
        this.dimension = d;
    }

    /**
     * Creates a new operator.
     * 
     * @param A coordinates for the (d+1)x(d+1) matrix representation.
     */
    public Operator(final IArithmetic[][] A) {
        this(new Matrix(A));
    }

    /**
     * Creates a new operator.
     * 
     * @param s a symbolic representation of the new operator.
     */
    public Operator(final String s) {
        this(parse(s));
    }

    /**
     * Creates an identity operator.
     * 
     * @param dimension the dimension of the space to act on.
     * @return the new operator.
     */
    public static Operator identity(final int dimension) {
        return new Operator(Matrix.one(dimension + 1));
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.joss.geometry.IOperator#getDimension()
     */
    public int getDimension() {
        return this.dimension;
    }

    /**
     * Retrieves an entry of the normalized matrix representation of this
     * operator.
     * 
     * @param i the row index.
     * @param j the column index.
     * @return the coordinate value.
     */
    public IArithmetic get(final int i, final int j) {
        if (i < 0 || i > getDimension() + 1) {
            throw new IllegalArgumentException("row index out of range");
        }
        if (j < 0 || j > getDimension() + 1) {
            throw new IllegalArgumentException("column index out of range");
        }
        return this.coords.get(i, j);
    }
    
    /**
     * Retrieves all cartesian coordinate values for this point.
     * 
     * @return the coordinates as a row matrix.
     */
    public Matrix getCoordinates() {
        return this.coords;
    }

    /**
     * Creates an operator representing the linear portion of this one.
     * 
     * @return the linear operator.
     */
    public Operator linearPart() {
        final int d = getDimension();
        final Matrix M = this.coords.mutableClone();
        M.setSubMatrix(d, 0, Matrix.zero(1, d));
        M.setSubMatrix(0, d, Matrix.zero(d, 1));
        return new Operator(M);
    }

    /**
     * Returns the translational component of this operator.
     * 
     * @return the translation component.
     */
    public Operator translationalPart() {
        final int d = getDimension();
        final Matrix M = this.coords.mutableClone();
        M.setSubMatrix(0, 0, Matrix.one(d));
        M.setSubMatrix(0, d, Matrix.zero(d, 1));
        return new Operator(M);
    }

    /**
     * Applies this operator to a point.
     * 
     * @param p the point to apply to.
     * @return the resulting point.
     */
    public IArithmetic applyTo(final Point p) {
        return p.times(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final Object other) {
        if (other instanceof Operator) {
            final Operator op = (Operator) other;
            final int dim = getDimension();
            if (dim != op.getDimension()) {
                throw new IllegalArgumentException("dimensions must be equal");
            }
            for (int i = 0; i < dim+1; ++i) {
                for (int j = 0; j < dim+1; ++j) {
                    final int d = get(i, j).compareTo(op.get(i, j));
                    if (d != 0) {
                        return d;
                    }
                }
            }
            return 0;
        } else {
            throw new IllegalArgumentException("can only compare two operators");
        }
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#floor()
     */
    public IArithmetic floor() {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates an operator in which every coordinate of the translational part
     * is reduced modulo 1. Those coordinates must all be of type {@link Real}.
     * 
     * @return the modified operator.
     */
    public Operator modZ() {
        final int d = getDimension();
        final Matrix M = this.coords.mutableClone();
        for (int i = 0; i < d; ++i) {
            M.set(d, i, ((Real) get(d, i)).mod(1));
        }
        return new Operator(M);
    }

    /**
     * Creates a matrix which contains the difference between this operator and its
     * modZ() image. In other words, the matrix contains the floor values of all the
     * translation components.
     * 
     * @return the matrix of floor values.
     */
    public Matrix floorZ() {
        final int d = getDimension();
        final Matrix M = new Matrix(1, d);
        for (int i = 0; i < d; ++i) {
            M.set(0, i, ((Real) get(d, i)).div(1));
        }
        return M;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return getCoordinates().hashCode();
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#inverse()
     */
    public IArithmetic inverse() {
        return new Operator((Matrix) this.coords.inverse());
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#isExact()
     */
    public boolean isExact() {
        return this.coords.isExact();
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#negative()
     */
    public IArithmetic negative() {
        throw new UnsupportedOperationException("operation not defined");
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#one()
     */
    public IArithmetic one() {
        return new Operator((Matrix) this.coords.one());
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#plus(java.lang.Object)
     */
    public IArithmetic plus(final Object other) {
        throw new UnsupportedOperationException("operation not defined");
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#times(java.lang.Object)
     */
    public IArithmetic times(final Object other) {
        if (other instanceof Operator) {
            final Matrix M = ((Operator) other).coords;
            return new Operator((Matrix) this.coords.times(M));
        } else {
            throw new UnsupportedOperationException("operation not defined");
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return this.coords.toString().replaceFirst("Matrix", "Operator");
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#zero()
     */
    public IArithmetic zero() {
        throw new UnsupportedOperationException("operation not defined");
    }

    /**
     * Parses an operator given in symbolic form, as in "y+x,-x,z+1/2", and returns the
     * corresponding matrix, which can then be used as input to the constructor. In the
     * symbolic form of the operator, the letters "x", "y" and "z" stand for the first
     * three cartesian coordinates of the point the operator is to be applied to.
     * 
     * CAVEAT: currently, only affine operators and only dimensions up to 3 are supported.
     * 
     * @param s the input string.
     * @return the operator matrix encoded by the input string.
     */
    public static Matrix parse(final String s) {
        final String msg = "Bad operator format for \"" + s + "\": "; // just in case
        
        final String parts[] = s.replaceAll("\\s+", "").split(",");
        if (parts.length > 3) {
            throw new DataFormatException(msg + "more than 3 coordinates.");
        }
        final int d = parts.length;
        final String varNames = "xyz".substring(0, d) + "#";
        final Matrix M = new Matrix(d+1, d+1);
        M.setColumn(d, Matrix.zero(d+1, 1));
        M.set(d, d, new Whole(1));
        
        for (int i = 0; i < d; ++i) {
            final String term = parts[i] + "#";
            final int m = term.length() - 1;
            int k = 0;
            while (k < m) {
                IArithmetic f = new Whole(1);
                if (term.charAt(k) == '-') {
                    f = f.negative();
                    ++k;
                } else if (term.charAt(k) == '+') {
                    ++k;
                }
                int j = k;
                while (Character.isDigit(term.charAt(k))) {
                    ++k;
                }
                if (k > j) {
                    final int z = Integer.parseInt(term.substring(j, k));
                    f = f.times(new Whole(z));
                }
                if (term.charAt(k) == '/') {
                    ++k;
                    j = k;
                    while (Character.isDigit(term.charAt(k))) {
                        ++k;
                    }
                    if (k > j) {
                        final int z = Integer.parseInt(term.substring(j, k));
                        f = f.dividedBy(new Whole(z));
                    } else {
                        throw new DataFormatException(msg + "fraction has no denominator");
                    }
                }
                if (term.charAt(k) == '*') {
                    ++k;
                }
                final char c = term.charAt(k);
                j = varNames.indexOf(c);
                if (j >= 0) {
                    ++k;
                } else if (Character.isDigit(c) || "+-".indexOf(c) >= 0) {
                    j = d;
                } else {
                    throw new DataFormatException(msg + "illegal variable name " + c);
                }
                if (M.get(j, i) != null) {
                    throw new DataFormatException(msg + "variable used twice");
                } else {
                    M.set(j, i, f);
                }
            }
        }
        for (int i = 0; i < d+1; ++i) {
            for (int j = 0; j < d+1; ++j) {
                if (M.get(i, j) == null) {
                    M.set(i, j, Whole.ZERO);
                }
            }
        }
        M.makeImmutable();
        
        return M;
    }
}
