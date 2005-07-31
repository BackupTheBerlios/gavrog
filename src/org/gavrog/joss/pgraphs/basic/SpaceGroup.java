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

package org.gavrog.joss.pgraphs.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.Rational;
import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.pgraphs.io.NetParser;



/**
 * A representation of a crystallographic space group. Operators are given as
 * (d+1)x(d+1) matrices of the form
 * 
 * <code>
 *   A 0
 *   v 1
 * </code>
 * 
 * where A is a d x d unimodular integer matrix and v is a d-dimensional vector
 * with rational entries.
 * 
 * Each matrix encodes a d-dimensional affine transformation, where d is the
 * dimension of the group. These act from the right on (d+1)-dimensional row
 * vectors of the form (p 1), representing points in d-dimensional space, where
 * p is a d-dimensional vector. This convention is used in order to allow for an
 * easy encoding of the translational component of a symmetry. It could be
 * extended to enable general projective transformations, but these are not
 * needed in this package. Consequently, the last column of each operator matrix
 * must always be of the form shown above.
 * 
 * Operators are interpreted in terms of a unit cell for the group, which is
 * either a primitive cell or a centered cell. A full set of operators is formed
 * by a representative of each class of group operators modulo cell
 * translations. A normalized representative has all the coordinates of its
 * translational part in the half-open interval [0,1).
 * 
 * @author Olaf Delgado
 * @version $Id: SpaceGroup.java,v 1.3 2005/07/31 19:44:58 odf Exp $
 */
public class SpaceGroup {
    private final int dimension;
    private final Set operators;
    
    /**
     * Constructs a new instance.
     * 
     * If the "generate" parameter is set, the given collection of operators is
     * taken to generate the group together with the unit cell translations, and
     * a full set of normalized operators is generated from it.
     * 
     * If the "generate" parameter is not set, the given operators are assumed
     * to form a full set already, not necessarily normalized.
     * 
     * @param dimension the dimension of the group.
     * @param operators a list of operators for the group.
     * @param generate if true, a full operator list is generated.
     * @param check verifies that the operator list as given is complete.
     */
    public SpaceGroup(final int dimension, final Collection operators,
            final boolean generate, boolean check) {
        
        // --- set the dimension here - needed by normalizedOperator()
        this.dimension = dimension;
        
        // --- check the individual operators
        final int d = dimension;
        
        for (final Iterator iter = operators.iterator(); iter.hasNext();) {
            final Matrix op = (Matrix) iter.next();
            if (op.numberOfRows() != d+1 || op.numberOfColumns() != d+1) {
                throw new IllegalArgumentException("bad shape for operator " + op);
            }
            for (int i = 0; i < d; ++i) {
                for (int j = 0; j < d; ++j) {
                    if (!(op.get(i, j) instanceof Whole)) {
                        final String msg = "bad linear part for operator " + op;
                        throw new IllegalArgumentException(msg);
                    }
                }
                if (!op.get(i, d).equals(Whole.ZERO)) {
                    final String msg = "bad last column for operator " + op;
                    throw new IllegalArgumentException(msg);
                }
            }
            for (int j = 0; j < d; ++j) {
                if (!(op.get(d, j) instanceof Rational)) {
                    final String msg = "bad translational part for operator " + op;
                    throw new IllegalArgumentException(msg);
                }
            }
            if (!op.get(d, d).equals(Whole.ONE)) {
                final String msg = "bad last column for operator " + op;
                throw new IllegalArgumentException(msg);
            }
            
            final Matrix A = op.getSubMatrix(0, 0, d, d);
            if (!A.determinant().abs().equals(Whole.ONE)) {
                final String msg = "linear part of operator " + op + " is not unimodular";
                throw new IllegalArgumentException(msg);
            }
        }
        
        // --- copy operators and normalize their translational parts
        final Set ops = new HashSet();
        for (final Iterator iter = operators.iterator(); iter.hasNext();) {
            ops.add(normalizedOperator((Matrix) iter.next()));
        }
        
        // --- generate a full set of operators, if required
        if (generate) {
            final Set gens = new HashSet();
            gens.addAll(ops);
            ops.clear();
            final LinkedList queue = new LinkedList();
            queue.addAll(gens);
            
            while (queue.size() > 0) {
                final Matrix A = (Matrix) queue.removeFirst();
                for (final Iterator iter = gens.iterator(); iter.hasNext();) {
                    final Matrix B = (Matrix) iter.next();
                    final Matrix AB = normalizedOperator((Matrix) A.times(B));
                    if (!ops.contains(AB)) {
                        ops.add(AB);
                        queue.addLast(AB);
                    }
                }
            }
        }
        
        // --- check products and inverses, if required
        if (check) {
            for (final Iterator iter1 = ops.iterator(); iter1.hasNext();) {
                final Matrix A = (Matrix) iter1.next();
                for (final Iterator iter2 = ops.iterator(); iter2.hasNext();) {
                    final Matrix B = (Matrix) iter2.next();
                    final Matrix AB_ = normalizedOperator((Matrix) A.times(B.inverse()));
                    if (!ops.contains(AB_)) {
                        throw new IllegalArgumentException("operators don't form a group");
                    }
                }
            }
        }
        
        // --- initialize the operator list
        this.operators = Collections.unmodifiableSet(ops);
    }
    
    /**
     * Constructs a space group corresponding to a Hermann-Mauguin symbol.
     * 
     * @param dimension the dimension of the group.
     * @param name the Hermann-Maugain symbol for the group.
     */
    public SpaceGroup(final int dimension, final String name) {
        this(dimension, NetParser.operators(name), false, false);
    }
    
    /**
     * Constructs a space group with a given set of generators.
     * 
     * @param dimension the dimension of the group.
     * @param generators a set of generating operators modulo unit cell.
     */
    public SpaceGroup(final int dimension, final Collection generators) {
        this(dimension, generators, true, false);
    }
    
    /**
     * Reduces the coordinates of the translational part of the given operator
     * modulo 1.
     * 
     * @param op the input operator.
     * @return the normalized operator.
     */
    public Matrix normalizedOperator(final Matrix op) {
        final Matrix A = op.mutableClone();
        final int d = getDimension();
        for (int j = 0; j < d; ++j) {
            A.set(d, j, ((Rational) A.get(d, j)).mod(1));
        }
        A.makeImmutable();
        return A;
    }
    
    /**
     * Returns the dimension of this group.
     * @return the dimension.
     */
    public int getDimension() {
        return dimension;
    }
    
    /**
     * Retrieves the set of operators modulo unit cell.
     * @return the set of operators.
     */
    public Set getOperators() {
        return operators;
    }
    
    /**
     * Constructs a basis for the primitive lattice of this group. The output is
     * a matrix the rows of which are basis vectors for the primitive lattice,
     * or, put in other words, edge vectors for a primitive cell.
     * 
     * @return a matrix representing the primitive lattice.
     */
    public Matrix primitiveCell() {
        // --- some shortcuts
        final int d = getDimension();
        final Matrix I = Matrix.one(d);
        
        // --- collect translation vectors
        final List vecs = new ArrayList();
        for (final Iterator iter = this.operators.iterator(); iter.hasNext();) {
            final Matrix op = (Matrix) iter.next();
            final Matrix A = op.getSubMatrix(0, 0, d, d);
            if (A.equals(I)) {
                vecs.add(op.getSubMatrix(d, 0, 1, d));
            }
        }
        
        // --- copy the vectors into a matrix
        final Matrix B = new Matrix(vecs.size() + d, d);
        B.setSubMatrix(0, 0, I);
        for (int i = 0; i < vecs.size(); ++i) {
            B.setRow(i + d, (Matrix) vecs.get(i));
        }
        
        // --- triangulate to extract a basis
        Matrix.triangulate(B, null, true, true, 0);
        if (B.rank() != d) {
            throw new RuntimeException("sorry, encountered a program bug");
        }
        
        // --- return the basis
        return B.getSubMatrix(0, 0, d, d);
    }
    
    /**
     * Returns a matrix that, via multiplication from the right, transforms a
     * row vector in unit cell coordinates into one using primitive cell
     * coordinates.
     * 
     * @return the transformation matrix.
     */
    public Matrix transformationToPrimitive() {
        final int d = getDimension();
        final Matrix P = Matrix.one(d+1).mutableClone();
        P.setSubMatrix(0, 0, primitiveCell());
        return (Matrix) P.inverse();
    }
    
    /**
     * Constructs a set of operators which is full with respect to a primitive
     * cell for the group, but still expressed in the coordinate system defined
     * by the original cell.
     * 
     * @return a full set of operators for a primitive setting.
     */
    public Set primitiveOperators() {
        final Set result = new HashSet();
        final int d = getDimension();
        final Matrix P = Matrix.one(d+1).mutableClone();
        P.setSubMatrix(0, 0, primitiveCell());
        final Matrix P_1 = (Matrix) P.inverse();
        
        for (final Iterator iter = getOperators().iterator(); iter.hasNext();) {
            final Matrix op = (Matrix) iter.next();
            final Matrix tmp = normalizedOperator((Matrix) P.times(op).times(P_1));
            final Matrix out = normalizedOperator((Matrix) P_1.times(tmp).times(P));
            result.add(out);
        }
        
        return result;
    }
}
