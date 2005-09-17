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

import org.gavrog.jane.compounds.LinearAlgebra;
import org.gavrog.jane.compounds.Matrix;

/**
 * Takes a two- or three-dimensional crystallographic group and identifies it,
 * producing its name as according to the international tables for
 * Crystallography.
 * 
 * @author Olaf Delgado
 * @version $Id: SpaceGroupFinder.java,v 1.1 2005/09/17 05:00:35 odf Exp $
 */
public class SpaceGroupFinder {
    private SpaceGroup G;
    
    /**
     * Constructs a new instance.
     * 
     * @param G the group to identify.
     */
    public SpaceGroupFinder(final SpaceGroup G) {
        final int d = G.getDimension();
        if (d != 2 && d != 3) {
            throw new UnsupportedOperationException("group dimension must be 2 or 3");
        }
        this.G = G;
    }
    
    /**
     * Encodes and determines the characteristica of the linear part of an
     * operator.
     */
    private static class OperatorType {
        final public boolean orientationPreserving;
        final public int order;
        final public Vector axis;
        final public boolean clockwise;
        
        public OperatorType(final Operator op) {
            final int d = op.getDimension();
            Matrix M = op.getCoordinates().getSubMatrix(0, 0, d, d);

            if (d == 2) {
                if (M.determinant().isNegative()) {
                    this.orientationPreserving = false;
                    this.order = 2;
                    this.axis = getAxis(M);
                    this.clockwise = false;
                } else {
                    this.orientationPreserving = true;
                    this.order = matrixOrder(M, 6);
                    this.axis = null;
                    if (order > 2) {
                        final Matrix v = new Matrix(new int[][] { { 1, 0 } });
                        final Matrix A = new Matrix(2, 2);
                        A.setRow(0, v);
                        A.setRow(1, (Matrix) v.times(M));
                        this.clockwise = A.determinant().isPositive();
                    } else {
                        this.clockwise = true;
                    }
                }
            } else if (d == 3) {
                if (M.determinant().isNegative()) {
                    M = (Matrix) M.negative();
                    this.orientationPreserving = true;
                } else {
                    this.orientationPreserving = false;
                }

                if (M.isOne()) {
                    this.order = 1;
                    this.axis = null;
                    this.clockwise = true;
                } else {
                    this.order = matrixOrder(M, 6);
                    this.axis = getAxis(M);
                    if (order > 2) {
                        final Matrix a = axis.getCoordinates();
                        final Matrix v;
                        if (a.get(0, 1).isZero() && a.get(0, 2).isZero()) {
                            v = new Matrix(new int[][] { { 0, 1, 0 } });
                        } else {
                            v = new Matrix(new int[][] { { 1, 0, 0 } });
                        }
                        final Matrix A = new Matrix(3, 3);
                        A.setRow(0, axis.getCoordinates());
                        A.setRow(1, v);
                        A.setRow(2, (Matrix) v.times(M));
                        this.clockwise = A.determinant().isPositive();
                    } else {
                        this.clockwise = true;
                    }
                }
            } else {
                final String msg = "operator must be 2- or 3-dimensional, got " + op;
                throw new UnsupportedOperationException(msg);
            }
        }
        
        private static Vector getAxis(final Matrix M) {
            final Matrix Z = LinearAlgebra.rowNullSpace((Matrix) M.minus(M.one()), true);
            if (Z.numberOfRows() == 1) {
                return null;
            } else {
                final Vector v = new Vector(Z.getRow(0));
                if (v.isNegative()) {
                    return (Vector) v.negative();
                } else {
                    return v;
                }
            }
        }
        
        private static int matrixOrder(final Matrix M, final int max) {
            Matrix A = M;
            for (int i = 1; i <= max; ++i) {
                if (A.isOne()) {
                    return i;
                }
                A = (Matrix) A.times(M);
            }
            return 0;
        }
    }
}
