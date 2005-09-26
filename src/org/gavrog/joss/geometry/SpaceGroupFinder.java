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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.jane.compounds.Matrix;


/**
 * Takes a two- or three-dimensional crystallographic group and identifies it,
 * producing its name as according to the international tables for
 * Crystallography.
 * 
 * @author Olaf Delgado
 * @version $Id: SpaceGroupFinder.java,v 1.15 2005/09/26 23:47:54 odf Exp $
 */
public class SpaceGroupFinder {
    final public static int CUBIC_SYSTEM = 432;
    final public static int ORTHORHOMBIC_SYSTEM = 222;
    final public static int HEXAGONAL_SYSTEM = 6;
    final public static int TETRAGONAL_SYSTEM = 4;
    final public static int TRIGONAL_SYSTEM = 3;
    final public static int MONOCLINIC_SYSTEM = 2;
    final public static int TRICLINIC_SYSTEM = 1;
    
    final private SpaceGroup G;
    
    final private int crystalSystem;
    final private Matrix intermediateBasis;
    final private List gensOriginalBasis;
    
    /**
     * Constructs a new instance.
     * 
     * @param G the group to identify.
     */
    public SpaceGroupFinder(final SpaceGroup G) {
        final int d = G.getDimension();
        this.G = G;
        final Point o = Point.origin(d);
        
        if (d == 3) {
            // --- first step of analysis
            final Object res[] = analyzePointGroup3D();
            crystalSystem = ((Integer) res[0]).intValue();
            intermediateBasis = (Matrix) res[1];
            gensOriginalBasis = (List) res[2];
            
            // --- convert generators to intermediate basis
            final BasisChange T1 = new BasisChange(intermediateBasis, o);
            final List gensIntermediateBasis = convert(gensOriginalBasis, T1);
            
            // --- get primitive cell vectors and convert to intermediate basis
            final Vector primitiveCell[] = Vector.fromMatrix(G.primitiveCell());
            for (int i = 0; i < primitiveCell.length; ++i) {
                primitiveCell[i] = (Vector) primitiveCell[i].times(T1);
            }
            
            // --- compute a lattice basis of smallest Dirichlet vectors
            final Vector reduced[] = Vector.reducedBasis(primitiveCell, Matrix.one(3));
            final Matrix reducedBasis = Vector.toMatrix(reduced);
            
            // --- compute a canonical basis based on the group's crystal system
            final Matrix latticeBasis = canonicalBasis(reducedBasis);
            
            // --- convert generators to lattice basis
            final BasisChange T2 = new BasisChange(latticeBasis, o);
            final List gensLatticeBasis = convert(gensIntermediateBasis, T2);
            
        } else if (d ==2) {
            throw new UnsupportedOperationException("dimension 2 not yet supported");
        } else {
            final String msg = "group dimension is " + d + ", must be 2 or 3";
            throw new UnsupportedOperationException(msg);
        }
    }
    
    /**
     * Performs a basis change on a list of geometric objects.
     * 
     * @param gens the objects to conver to the new basis.
     * @param T the basis change transformation.
     * @return the list of converted objects.
     */
    private List convert(final List gens, final BasisChange T) {
        final List tmp = new ArrayList();
        for (final Iterator iter = gensOriginalBasis.iterator(); iter.hasNext();) {
            tmp.add(((Operator) iter.next()).times(T));
        }
        return Collections.unmodifiableList(tmp);
    }
    
    /**
     * Analyzes the point group to determine the crystal system and find an
     * appropriate set of generators and a preliminary basis based on it.
     * 
     * @return an array containing the crystal system, basis and set of generators.
     */
    private Object[] analyzePointGroup3D() {
        // --- categorize the group operators by their point actions
        final Map type2ops = G.fundamentalOperatorsByType();
        final Set twoFold = (Set) type2ops.get(new OperatorType(3, true, 2, true));
        final Set threeFold = (Set) type2ops.get(new OperatorType(3, true, 3, true));
        final Set fourFold = (Set) type2ops.get(new OperatorType(3, true, 4, true));
        final Set sixFold = (Set) type2ops.get(new OperatorType(3, true, 6, true));
        
        final Set inversions = (Set) type2ops.get(new OperatorType(3, false, 1, true));
        final Set mirrors = new HashSet();
        mirrors.addAll((Set) type2ops.get(new OperatorType(3, false, 2, true)));
        mirrors.addAll((Set) type2ops.get(new OperatorType(3, false, 3, true)));
        mirrors.addAll((Set) type2ops.get(new OperatorType(3, false, 4, true)));
        mirrors.addAll((Set) type2ops.get(new OperatorType(3, false, 6, true)));
        
        if (inversions.size() == 0) {
            twoFold.addAll((Set) type2ops.get(new OperatorType(3, false, 2, true)));
            fourFold.addAll((Set) type2ops.get(new OperatorType(3, false, 4, true)));
            sixFold.addAll((Set) type2ops.get(new OperatorType(3, false, 6, true)));
        }
        
        // --- initialize some variables
        final int crystalSystem;
        final List generators = new ArrayList();
        Vector x = null, y = null, z = null;
        Operator R = null;
        
        /* --- find some generators and basis vectors based on rotational and
         *     roto-inversive axes
         */
        
        if (sixFold.size() > 0) {
            // --- there is a six-fold axis
            crystalSystem = HEXAGONAL_SYSTEM;
            final Operator A = (Operator) sixFold.iterator().next();
            z = A.linearAxis();
            R = (Operator) A.times(A);
            for (final Iterator iter = twoFold.iterator(); iter.hasNext();) {
                final Operator B = (Operator) iter.next();
                final Vector t = B.linearAxis();
                if (!t.isCollinearTo(z)) {
                    generators.add(B);
                    generators.add(A.times(B));
                    break;
                }
            }
            if (generators.size() == 0) {
                generators.add(A);
            }
        } else if (fourFold.size() > 1) {
            // --- there is more than one four-fold, but no six-fold, axis
            crystalSystem = CUBIC_SYSTEM;
            final Operator A = (Operator) fourFold.iterator().next();
            z = A.linearAxis();
            R = (Operator) threeFold.iterator().next();
            x = (Vector) z.times(R);
            y = (Vector) x.times(R);
            generators.add(A);
            generators.add(R);
        } else if (fourFold.size() > 0) {
            // --- there is exactly one four-fold, but no six-fold, axis
            crystalSystem = TETRAGONAL_SYSTEM;
            final Operator A = (Operator) fourFold.iterator().next();
            z = A.linearAxis();
            for (final Iterator iter = twoFold.iterator(); iter.hasNext();) {
                final Operator B = (Operator) iter.next();
                final Vector t = B.linearAxis();
                if (!t.isCollinearTo(z)) {
                    generators.add(B);
                    generators.add(A.times(B));
                    break;
                }
            }
            if (generators.size() == 0) {
                generators.add(A);
            }
            R = A;
        } else if (threeFold.size() > 1) {
            // --- multiple three-fold, but no four- or six-fold, axes
            crystalSystem = CUBIC_SYSTEM;
            final Operator A = (Operator) twoFold.iterator().next();
            z = A.linearAxis();
            R = (Operator) threeFold.iterator().next();
            x = (Vector) z.times(R);
            y = (Vector) x.times(R);
            generators.add(A);
            generators.add(R);
        } else if (threeFold.size() > 0) {
            // --- exactly one three-fold axis, but no four- or six-fold axes
            crystalSystem = TRIGONAL_SYSTEM;
            R = (Operator) threeFold.iterator().next();
            z = R.linearAxis();
            if (twoFold.size() > 0) {
                final Operator B = (Operator) twoFold.iterator().next();
                generators.add(B);
                generators.add(R.times(B));
            } else {
                generators.add(R);
            }
        } else if (twoFold.size() > 1) {
            // --- mutliply two-fold, no three-, four- or six-fold, axes
            crystalSystem = ORTHORHOMBIC_SYSTEM;
            final Iterator ops = twoFold.iterator();
            final Operator A = (Operator) ops.next();
            final Operator B = (Operator) ops.next();
            final Operator C = (Operator) ops.next();
            x = A.linearAxis();
            y = B.linearAxis();
            z = C.linearAxis();
            generators.add(A);
            generators.add(B);
            generators.add(C);
        } else if (twoFold.size() > 0) {
            // --- exactly one two-fold, but no three-, four- or six-fold
            crystalSystem = MONOCLINIC_SYSTEM;
            final Operator A = (Operator) twoFold.iterator().next();
            z = A.linearAxis();
            generators.add(A);
        } else {
            // --- no two-, three-, four- or six-fold axes
            crystalSystem = TRICLINIC_SYSTEM;
            z = new Vector(new int[] { 1, 0, 0 });
            if (inversions.size() == 0) {
                generators.add(new Operator("x+1,y,z"));
            }
        }
        
        // --- add a first basis vector, if missing
        if (x == null) {
            for (final Iterator iter = twoFold.iterator(); iter.hasNext();) {
                final Operator B = (Operator) iter.next();
                final Vector t = B.linearAxis();
                if (!t.isCollinearTo(z)) {
                    x = t;
                    break;
                }
            }
            if (x == null) {
                x = new Vector(new int[] { 0, 0, 1 });
                if (x.isCollinearTo(z)) {
                    x = new Vector(new int[] { 1, 0, 0 });
                }
                if (mirrors.size() > 0) {
                    final Operator M = (Operator) mirrors.iterator().next();
                    x = (Vector) x.plus(x.times(M));
                } else if (twoFold.size() > 0) {
                    final Operator M = (Operator) twoFold.iterator().next();
                    x = (Vector) x.minus(x.times(M));
                } else if (this.crystalSystem == TRIGONAL_SYSTEM) {
                    x = (Vector) x.minus(x.times(R));
                    
                }
            }
        }
        
        // --- add a second basis vector, if missing
        if (y == null) {
            if (R != null) {
                y = (Vector) x.times(R);
            } else {
                y = Vector.crossProduct3D(z, x);
                if (mirrors.size() > 0) {
                    final Operator M = (Operator) mirrors.iterator().next();
                    y = (Vector) y.plus(y.times(M));
                } else if (twoFold.size() > 0) {
                    final Operator M = (Operator) twoFold.iterator().next();
                    y = (Vector) y.minus(y.times(M));
                }
            }
        }

        // --- make sure the new basis is oriented like the old one
        if (Vector.volume3D(x, y, z).isNegative()) {
            z = (Vector) z.negative();
        }

        // --- if there's an inversion, we always need it among the generators
        if (inversions.size() > 0) {
            generators.add(inversions.iterator().next());
        }

        // --- return the results
        return new Object[] { new Integer(crystalSystem),
                Vector.toMatrix(new Vector[] { x, y, z }),
                Collections.unmodifiableList(generators) };
    }
    
    /**
     * Takes a reduced lattice basis and produces a canonical lattice basis with
     * respect to the group's crystal system.
     * 
     * @param B the reduced lattice basis.
     * @return the canonical lattice basis.
     */
    private Matrix canonicalBasis(final Matrix B) {
        switch (this.crystalSystem) {
        case CUBIC_SYSTEM:
            return canonicalBasisCubic(B);
        case HEXAGONAL_SYSTEM:
            return canonicalBasisHexagonal(B);
        case TRIGONAL_SYSTEM:
            return canonicalBasisTrigonal(B);
        case TETRAGONAL_SYSTEM:
            return canonicalBasisTetragonal(B);
        case ORTHORHOMBIC_SYSTEM:
            return canonicalBasisOrthorhombic(B);
        case MONOCLINIC_SYSTEM:
            return canonicalBasisMonoclinic(B);
        case TRICLINIC_SYSTEM:
            return canonicalBasisTriclinic(B);
        default:
            throw new RuntimeException("unknown crystal system");
        }
    }

    /**
     * Takes a reduced lattice basis and produces a canonical lattice basis with
     * respect to the cubic crystal system.
     * 
     * @param B the reduced lattice basis.
     * @return the canonical lattice basis.
     */
    private Matrix canonicalBasisCubic(Matrix b) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Takes a reduced lattice basis and produces a canonical lattice basis with
     * respect to the hexagonal crystal system.
     * 
     * @param B the reduced lattice basis.
     * @return the canonical lattice basis.
     */
    private Matrix canonicalBasisHexagonal(Matrix b) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Takes a reduced lattice basis and produces a canonical lattice basis with
     * respect to the trigonal crystal system.
     * 
     * @param B the reduced lattice basis.
     * @return the canonical lattice basis.
     */
    private Matrix canonicalBasisTrigonal(Matrix b) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Takes a reduced lattice basis and produces a canonical lattice basis with
     * respect to the tetragonal crystal system.
     * 
     * @param B the reduced lattice basis.
     * @return the canonical lattice basis.
     */
    private Matrix canonicalBasisTetragonal(Matrix b) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Takes a reduced lattice basis and produces a canonical lattice basis with
     * respect to the orthorhombic crystal system.
     * 
     * @param B the reduced lattice basis.
     * @return the canonical lattice basis.
     */
    private Matrix canonicalBasisOrthorhombic(Matrix b) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Takes a reduced lattice basis and produces a canonical lattice basis with
     * respect to the monoclinic crystal system.
     * 
     * @param B the reduced lattice basis.
     * @return the canonical lattice basis.
     */
    private Matrix canonicalBasisMonoclinic(Matrix b) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Takes a reduced lattice basis and produces a canonical lattice basis with
     * respect to the triclinic crystal system.
     * 
     * @param B the reduced lattice basis.
     * @return the canonical lattice basis.
     */
    private Matrix canonicalBasisTriclinic(Matrix b) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @return the crystal system for the group.
     */
    public int getCrystalSystem() {
        return this.crystalSystem;
    }
    
    /**
     * @return a preliminary basis based on the point group structure.
     */
    Matrix getPreliminaryBasis() {
        return this.intermediateBasis;
    }
    
    /**
     * @return a set of group generators.
     */
    public List getGeneratorsOriginalBasis() {
        return this.gensOriginalBasis;
    }
}
