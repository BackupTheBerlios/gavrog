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
import org.gavrog.jane.numbers.Complex;
import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Real;
import org.gavrog.jane.numbers.Whole;

/**
 * A d-dimensional vector represented by a row vector. To make interaction with
 * other geometry types easier, a zero coordinate is added internally.
 * 
 * @author Olaf Delgado
 * @version $Id: Vector.java,v 1.11 2005/09/21 01:37:38 odf Exp $
 */
public class Vector extends ArithmeticBase implements IArithmetic {
    final Matrix coords;
    final int dimension;

    /**
     * Creates a new vector from a row matrix containing its cartesian
     * coordinates. The dimension d of the vector created will correspond to the
     * number of columns in the given matrix. An extra coordinate of value 0
     * will be added internally.
     * 
     * @param M contains the coordinates for the vector.
     */
    public Vector(final Matrix M) {
        if (M.numberOfRows() != 1) {
            throw new IllegalArgumentException("matrix must have exactly 1 row");
        }
        final int d = M.numberOfColumns();
        this.dimension = d;
        this.coords = new Matrix(1, d + 1);
        this.coords.setSubMatrix(0, 0, M);
        this.coords.set(0, d, Whole.ZERO);
    }

    /**
     * Creates a new vector from its cartesian coordinates represented as an
     * array. The dimension d of the vector created will correspond to the
     * number of an entries in the array. An extra coordinate of value 0 will be
     * added internally.
     * 
     * @param coordinates the coordinates for the vector.
     */
    public Vector(final IArithmetic[] coordinates) {
        this(new Matrix(new IArithmetic[][] {coordinates}));
    }
    
    /**
     * Creates a new vector from its cartesian coordinates represented as an
     * array. The dimension d of the vector created will correspond to the
     * number of an entries in the array. An extra coordinate of value 0 will be
     * added internally.
     * 
     * @param coordinates the coordinates for the vector.
     */
    public Vector(final int[] coordinates) {
        this(new Matrix(new int[][] {coordinates}));
    }
    
    /**
     * Creates a new vector from its cartesian coordinates represented as an
     * array. The dimension d of the vector created will correspond to the
     * number of an entries in the array. An extra coordinate of value 0 will be
     * added internally.
     * 
     * @param coordinates the coordinates for the vector.
     */
    public Vector(final double[] coordinates) {
        this(new Matrix(new double[][] {coordinates}));
    }
    
    /**
     * Creates a new vector as a copy of a given one.
     * @param v the model vector.
     */
    public Vector(final Vector v) {
        this.dimension = v.dimension;
        this.coords = v.coords;
    }
    
    /**
     * This constructor applies a matrix to a vector. It is used by the
     * {@link #times(Object)} method to apply a general projective operator to
     * a vector.
     * 
     * @param v the original vector.
     * @param M the operator to apply as a (d+1)x(d+1) matrix.
     */
    private Vector(final Vector v, final Matrix M) {
        this(image(v, M));
    }

    /**
     * Applies an operator to a vector.
     * 
     * @param v the original vector.
     * @param M the operator to apply as a (d+1)x(d+1) matrix.
     * @return the resulting vector coordinates.
     */
    private static Matrix image(final Vector v, final Matrix M) {
        final int d = v.getDimension();
        if (d != M.numberOfRows() - 1 || d != M.numberOfColumns() - 1) {
            throw new IllegalArgumentException("dimensions don't match");
        }
        final Matrix img = (Matrix) v.coords.times(M);
        if (!img.get(0, d).isZero()) {
            throw new UnsupportedOperationException(
                    "the operation did not produce a vector");
        }
        return img.getSubMatrix(0, 0, 1, d);
    }
    
    /**
     * Returns a zero vector of a certain dimension.
     * 
     * @param dimension the requested dimension of the vector.
     * @return a zero vector of the requested dimension.
     */
    public static Vector zero(final int dimension) {
        return new Vector(Matrix.zero(1, dimension));
    }
    
    /**
     * Computes the product of two vectors with respect to a given quadratic form.
     * 
     * @param v the first vector.
     * @param w the second vector.
     * @param form a matrix representing the quadratic form.
     * @return the value of the product.
     */
    public static IArithmetic dot(final Vector v, final Vector w, final Matrix form) {
        final int d = v.getDimension();
        if (w.getDimension() != d || form.numberOfRows() != d
                || form.numberOfColumns() != d) {
            throw new IllegalArgumentException("dimensions do not match");
        }
        if (!form.equals(form.transposed())) {
            throw new IllegalArgumentException("matrix must be symmetric");
        }
        final Matrix vc = v.getCoordinates();
        final Matrix wc = w.getCoordinates();
        return ((Matrix) vc.times(form).times(wc.transposed())).get(0, 0);
    }
    
    /**
     * Computes the standard dot product of two vectors.
     * 
     * @param v the first vector.
     * @param w the second vector.
     * @return the value of the product.
     */
    public static IArithmetic dot(final Vector v, final Vector w) {
        if (v.getDimension() != w.getDimension()) {
            throw new IllegalArgumentException("dimensions do not match");
        }
        final Matrix vc = v.getCoordinates();
        final Matrix wc = w.getCoordinates();
        return ((Matrix) vc.times(wc.transposed())).get(0, 0);
    }
    
    /**
     * Computes the cross product of two 3-dimensional vectors.
     * 
     * @param v the first vector.
     * @param w the second vector.
     * @return the cross product.
     */
    public static Vector crossProduct3D(final Vector v, final Vector w) {
        if (v.getDimension() != 3 || w.getDimension() != 3) {
            throw new IllegalArgumentException("both vectors must be 3-dimensional");
        }
        final IArithmetic v0 = v.get(0);
        final IArithmetic v1 = v.get(1);
        final IArithmetic v2 = v.get(2);
        final IArithmetic w0 = w.get(0);
        final IArithmetic w1 = w.get(1);
        final IArithmetic w2 = w.get(2);
        final IArithmetic x0 = v1.times(w2).minus(v2.times(w1));
        final IArithmetic x1 = v2.times(w0).minus(v0.times(w2));
        final IArithmetic x2 = v0.times(w1).minus(v1.times(w0));
        return new Vector(new IArithmetic[] { x0, x1, x2 });
    }
    
    /**
     * Computes the oriented volume of the parallelohedron spanned by three
     * 3-dimensional vectors with respect to the usual metric.
     * 
     * @param u the first vector.
     * @param v the second vector.
     * @param w the third vector.
     * @return the volume.
     */
    public static IArithmetic volume3D(final Vector u, final Vector v, final Vector w) {
        return dot(u, crossProduct3D(v, w));
    }
    
    /**
     * Tests if this vector is collinear to another. Both vectors must have the
     * same dimension. The zero vector is considered non-collinear to any other,
     * even another zero vector.
     * 
     * @param v the other vector.
     * @return true if the two vectors are collinear.
     */
    public boolean isCollinearTo(final Vector v) {
        final int d = v.getDimension();
        if (getDimension() != d) {
            throw new UnsupportedOperationException("dimensions must be equal");
        }
        for (int i = 0; i < d; ++i) {
            if (!v.get(i).isZero()) {
                final IArithmetic q = v.get(i).dividedBy(this.get(i));
                return v.equals(this.times(q));
            }
        }
        return false;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.gavrog.joss.geometry.IPoint#getDimension()
     */
    public int getDimension() {
        return this.dimension;
    }
    
    /**
     * Retrieves a cartesian coordinate value for this vector.
     * 
     * @param i the index of the coordinate to retrieve.
     * @return the coordinate value.
     */
    public IArithmetic get(final int i) {
        if (i < 0 || i > getDimension()) {
            throw new IllegalArgumentException("index out of range");
        }
        return this.coords.get(0, i);
    }
    
    /**
     * Retrieves all cartesian coordinate values for this vector.
     * 
     * @return the coordinates as a row matrix.
     */
    public Matrix getCoordinates() {
        return this.coords.getSubMatrix(0, 0, 1, getDimension());
    }

    /**
     * Compares two vectors lexicographically.
     * 
     * @param other the vector to compare with.
     */
    public int compareTo(final Object other) {
        if (other instanceof Vector) {
            final Vector v = (Vector) other;
            if (getDimension() != v.getDimension()) {
                throw new IllegalArgumentException("dimensions must be equal");
            }
            for (int i = 0; i < getDimension(); ++i) {
                final int d = get(i).compareTo(v.get(i));
                if (d != 0) {
                    return d;
                }
            }
            return 0;
        } else {
            throw new IllegalArgumentException("can only compare two vectors");
        }
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#floor()
     */
    public IArithmetic floor() {
        throw new UnsupportedOperationException("not defined on vectors");
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
        throw new UnsupportedOperationException("not defined on vectors");
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#isExact()
     */
    public boolean isExact() {
        return coords.isExact();
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#negative()
     */
    public IArithmetic negative() {
        return new Vector((Matrix) getCoordinates().negative());
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#one()
     */
    public IArithmetic one() {
        throw new UnsupportedOperationException("not defined on vectors");
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#plus(java.lang.Object)
     */
    public IArithmetic plus(final Object other) {
        if (other instanceof Vector) {
            final Vector v = (Vector) other;
            return new Vector((Matrix) this.getCoordinates().plus(v.getCoordinates()));
        } else if (other instanceof Point){
            final Point p = (Point) other;
            return new Point((Matrix) this.getCoordinates().plus(p.getCoordinates()));
        } else {
            throw new UnsupportedOperationException("operation not defined");
        }
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#times(java.lang.Object)
     */
    public IArithmetic times(final Object other) {
        if (other instanceof Operator) {
            return new Vector(this, ((Operator) other).getCoordinates());
        } else if (other instanceof Complex) {
            return new Vector((Matrix) getCoordinates().times(other));
        } else {
            throw new UnsupportedOperationException("operation not defined");
        }
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#times(java.lang.Object)
     */
    public IArithmetic rtimes(final IArithmetic other) {
        if (other instanceof Complex) {
            return new Vector((Matrix) getCoordinates().times(other));
        } else {
            throw new UnsupportedOperationException("operation not defined");
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        final StringBuffer tmp = new StringBuffer(1000);
        tmp.append("Vector(");
        for (int i = 0; i < getDimension(); ++i) {
            if (i > 0) {
                tmp.append(",");
            }
            if (get(i) != null) {
                tmp.append(get(i).toString());
            }
        }
        tmp.append(")");
        return tmp.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.gavrog.jane.numbers.IArithmetic#zero()
     */
    public IArithmetic zero() {
        return new Vector((Matrix) getCoordinates().zero());
    }
    
    // TODO create a new class for things like the following?
    
    /**
     * Constructs an array of vectors from the rows of the given matrix.
     * 
     * @param M the input matrix.
     * @return the rows of M as vectors.
     */
    public static Vector[] rowVectors(final Matrix M) {
        final int n = M.numberOfRows();
        final Vector v[] = new Vector[n];
        for (int i = 0; i < n; ++i) {
            v[i] = new Vector(M.getRow(i));
        }
        return v;
    }
    
    public static boolean isBasis(Vector[] v) {
        final int d = v.length;
        final Matrix M = new Matrix(d, d);
        for (int i = 0; i < d; ++i) {
            if (v[i].getDimension() != d) {
                return false;
            }
            M.setRow(i, v[i].getCoordinates());
        }
        return !M.determinant().isZero();
    }
    
    /**
     * Performs a gauss elimination on a lattice basis.
     * 
     * @param v the vectors forming the basis.
     * @param M the quadratic form determining the metric.
     * @return vectors forming a reduced basis.
     */
    public static Vector[] gaussReduced(Vector[] v, Matrix M) {
        if (v.length != 2 || v[0].getDimension() != 2) {
            final String msg = "first argument must contain 2 vectors of dimension 2";
            throw new IllegalArgumentException(msg);
        }
        if (M.numberOfRows() != 2 || !M.equals(M.transposed())) {
            final String msg = "second argument must be a symmetric 2x2 matrix";
            throw new IllegalArgumentException(msg);
        }
        
        final Real eps = new FloatingPoint(1e-12);
        IArithmetic sl[] = new IArithmetic[] { dot(v[0], v[0], M), dot(v[1], v[1], M) };
        while (true) {
            final int i = sl[0].isLessThan(sl[1]) ? 0 : 1;
            final int j = 1 - i;
            final IArithmetic t = dot(v[i], v[j], M).dividedBy(sl[i]).round();
            v[j] = (Vector) v[j].minus(t.times(v[i]));
            sl[j] = dot(v[j], v[j], M);
            if (sl[j].isGreaterOrEqual(sl[i].minus(eps))) {
                break;
            }
        }

        if (dot(v[0], v[1], M).isPositive()) {
            v[1] = (Vector) v[1].negative();
        }
        return v;
    }
    
    /**
     * Performs a single step of the Selling reduction algorithm.
     * 
     * @param v the augmented list of basis vectors.
     * @param M the quadratic form determining the metric.
     * @return true if there was a change.
     */
    private static boolean sellingStep(final Vector v[], final Matrix M) {
        final Real eps = new FloatingPoint(1e-12);
        for (int i = 0; i < 3; ++i) {
            for (int j = i+1; j < 4; ++j) {
                if (dot(v[i], v[j], M).isGreaterThan(eps)) {
                    for (int k = 0; k < 4; ++k) {
                        if (k != i && k != j) {
                            v[k] = (Vector) v[k].plus(v[i]);
                        }
                    }
                    v[i] = (Vector) v[i].negative();
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Performs a Selling reduction on a lattice basis
     * 
     * @param v the vectors forming the basis.
     * @param M the quadratic form determining the metric.
     * @return vectors forming a reduced basis.
     */
    public static Vector[] sellingReduced(final Vector[] v, final Matrix M) {
        if (v.length != 3 || v[0].getDimension() != 3) {
            final String msg = "first argument must contain 3 vectors of dimension 3";
            throw new IllegalArgumentException(msg);
        }
        if (M.numberOfRows() != 3 || !M.equals(M.transposed())) {
            final String msg = "second argument must be a symmetric 3x3 matrix";
            throw new IllegalArgumentException(msg);
        }
        
        final Vector[] w = new Vector[] { v[0], v[1], v[2],
                (Vector) v[0].plus(v[1]).plus(v[2]).negative() };
        
        while (sellingStep(w, M)) {
        }
        
        return new Vector[] { w[0], w[1], w[2] };
    }
    
    /**
     * Computes the Dirichlet domain for a given vector lattice and returns the
     * set of normal vectors for the pairs of parallel planes that bound it.
     * 
     * @param b vectors forming a lattice basis.
     * @param M the quadratic form determining the metric.
     * @return the normal vectors to the faces of the Dirichlet domain.
     */
    public static Vector[] dirichletVectors(final Vector[] b, final Matrix M) {
        final int dim = b.length;
        if (b[0].getDimension() != dim) {
            final String msg = "illegal first argument";
            throw new IllegalArgumentException(msg);
        }
        if (M.numberOfRows() != dim || !M.equals(M.transposed())) {
            final String msg = "illegal second argument";
            throw new IllegalArgumentException(msg);
        }

        if (dim == 2) {
            final Vector t[] = Vector.gaussReduced(b, M);
            return new Vector[] { t[0], t[1], (Vector) t[0].plus(t[1]) };
        } else if (dim == 3) {
            final Vector t[] = Vector.sellingReduced(b, M);
            return new Vector[] { t[0], t[1], t[2], (Vector) t[0].plus(t[1]),
                    (Vector) t[0].plus(t[2]), (Vector) t[1].plus(t[2]),
                    (Vector) t[0].plus(t[1]).plus(t[2]) };
        } else {
            throw new UnsupportedOperationException("only dimensions 2 and 3 work");
        }
    }
}
