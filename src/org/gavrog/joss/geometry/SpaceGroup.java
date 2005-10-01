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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.collections.HashMapWithDefault;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.Rational;
import org.gavrog.jane.numbers.Whole;



/**
 * A representation of a crystallographic space group. Operators are given as
 * (d+1)x(d+1) matrices of the form
 * 
 * <pre>
 *   A 0
 *   v 1
 * </pre>
 * 
 * where A is a d x d unimodular integer matrix and v is a d-dimensional vector
 * with rational entries. Rather than raw matrices, we use instances of the
 * class {@link Operator}.
 * 
 * Each matrix encodes a d-dimensional affine transformation, where d is the
 * dimension of the group. These act from the right on (d+1)-dimensional row
 * vectors of the form (p 1), representing points in d-dimensional space, where
 * p is a d-dimensional vector. Again, points are not given as raw matrices, but
 * rather as instances of the class {@link Point}.
 * 
 * These convention are used in order to allow for an easy encoding of the
 * translational component of a symmetry. It could be extended to enable general
 * projective transformations, but these are not needed in this package.
 * Consequently, the last column of each operator matrix must always be of the
 * form shown above.
 * 
 * Operators are interpreted in terms of a unit cell for the group, which is
 * either a primitive cell or a centered cell. A full set of operators is formed
 * by a representative of each class of group operators modulo cell
 * translations. A normalized representative has all the coordinates of its
 * translational part in the half-open interval [0,1).
 * 
 * @author Olaf Delgado
 * @version $Id: SpaceGroup.java,v 1.20 2005/10/01 00:40:12 odf Exp $
 */
public class SpaceGroup {
    private final int dimension;
    private final Set operators;
    
    /**
     * Constructs a new instance.
     * 
     * If the "generate" parameter is set, the given collection of operators is
     * taken together with the unit cell translations to generate the group, and
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
        
        // --- set the dimension here
        this.dimension = dimension;
        
        // --- check the individual operators
        final int d = dimension;
        
        for (final Iterator iter = operators.iterator(); iter.hasNext();) {
            final Operator op = (Operator) iter.next();
            if (op.getDimension() != d) {
                throw new IllegalArgumentException("wrong dimension for operator " + op);
            }
            for (int i = 0; i < d; ++i) {
                for (int j = 0; j < d; ++j) {
                    if (!(op.get(i, j) instanceof Whole)) {
                        final String msg = "bad linear part for operator " + op;
                        throw new IllegalArgumentException(msg);
                    }
                }
                if (!op.get(i, d).isZero()) {
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
            if (!op.get(d, d).isOne()) {
                final String msg = "bad last column for operator " + op;
                throw new IllegalArgumentException(msg);
            }
            
            final Operator lin = op.linearPart();
            if (!lin.getCoordinates().determinant().abs().isOne()) {
                final String msg = "linear part of operator " + op + " is not unimodular";
                throw new IllegalArgumentException(msg);
            }
        }
        
        // --- copy operators and normalize their translational parts
        final Set ops = new HashSet();
        for (final Iterator iter = operators.iterator(); iter.hasNext();) {
            ops.add(((Operator) iter.next()).modZ());
        }
        
        // --- generate a full set of operators, if required
        if (generate) {
            final Set gens = new HashSet();
            gens.addAll(ops);
            ops.clear();
            final LinkedList queue = new LinkedList();
            queue.addAll(gens);
            
            while (queue.size() > 0) {
                final Operator A = (Operator) queue.removeFirst();
                for (final Iterator iter = gens.iterator(); iter.hasNext();) {
                    final Operator B = (Operator) iter.next();
                    final Operator AB = ((Operator) A.times(B)).modZ();
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
                final Operator A = (Operator) iter1.next();
                for (final Iterator iter2 = ops.iterator(); iter2.hasNext();) {
                    final Operator B = (Operator) iter2.next();
                    final Operator AB_ = (Operator) A.times(B.inverse());
                    if (!ops.contains(AB_.modZ())) {
                        throw new IllegalArgumentException("operators form no group");
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
     * Currently, the matching of Hermann-Mauguin symbols is case sensitive. Thus, the
     * centering letter must be lower-case for plane groups and upper-case for space
     * groups. Everything else must be lowe-case.
     * 
     * Extension letters for the hexagonal (":H") or rhombohedral (":R) settings are
     * case-insensitive. To explicitly specify first or second origin choice, use
     * extensions ":1" or ":2", respectively. The second origin choice is the default.
     * 
     * @param dimension the dimension of the group.
     * @param name the Hermann-Maugain symbol for the group.
     */
    public SpaceGroup(final int dimension, final String name) {
        this(dimension, SpaceGroupCatalogue.operators(dimension, name), false, false);
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
        final Operator I = Operator.identity(d);
        
        // --- collect translation vectors
        final List vecs = new ArrayList();
        for (final Iterator iter = this.operators.iterator(); iter.hasNext();) {
            final Operator op = (Operator) iter.next();
            final Operator A = op.linearPart();
            if (A.equals(I)) {
                vecs.add(op.translationalPart().getCoordinates());
            }
        }
        
        // --- copy the vectors into a matrix
        final Matrix B = new Matrix(vecs.size() + d, d);
        B.setSubMatrix(0, 0, Matrix.one(d));
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
     * Returns an operator that, via multiplication from the right, transforms a
     * point in unit cell coordinates into one using primitive cell coordinates.
     * 
     * @return the transformation matrix.
     */
    public Operator transformationToPrimitive() {
        final Matrix P = Matrix.one(getDimension()+1).mutableClone();
        P.setSubMatrix(0, 0, primitiveCell());
        return (Operator) (new Operator(P)).inverse();
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
        final Operator T_1 = transformationToPrimitive();
        final Operator T = (Operator) T_1.inverse();
        
        for (final Iterator iter = getOperators().iterator(); iter.hasNext();) {
            final Operator op = (Operator) iter.next();
            final Operator tmp = ((Operator) T.times(op).times(T_1)).modZ();
            final Operator out = ((Operator) T_1.times(tmp).times(T)).modZ();
            result.add(out);
        }
        
        return result;
    }

    /**
     * Constructs a sorted list of operators which is full with respect to a primitive
     * cell for the group, but still expressed in the coordinate system defined by the
     * original cell. The operators are sorted lexicographically by their linear parts.
     * 
     * @return the sorted list of operators for a primitive setting.
     */
    public List primitiveOperatorsSorted() {
        final List res = new ArrayList();
        res.addAll(primitiveOperators());
        
        Collections.sort(res, new Comparator() {
            public int compare(final Object o1, final Object o2) {
                final Operator op1 = ((Operator) o1).linearPart();
                final Operator op2 = ((Operator) o2).linearPart();
                return op1.compareTo(op2);
            }
        });
        
        return res;
    }
    
    /**
     * Returns a map with the occuring operator types as keys and the sets of
     * all operators of the respective types as values.
     * 
     * @return a map assigning operators types to operator sets.
     */
    public Map primitiveOperatorsByType() {
        final Map res = new HashMapWithDefault() {
            public Object makeDefault() {
                return new HashSet();
            }
        };
        for (final Iterator iter = primitiveOperators().iterator(); iter.hasNext();) {
            final Operator op = (Operator) iter.next();
            final OperatorType type = new OperatorType(op);
            ((Set) res.get(type)).add(op);
        }
        
        return res;
    }
}
