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
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Rational;
import org.gavrog.jane.numbers.Whole;


/**
 * Takes a two- or three-dimensional crystallographic group and identifies it,
 * producing its name as according to the international tables for
 * Crystallography.
 * 
 * @author Olaf Delgado
 * @version $Id: SpaceGroupFinder.java,v 1.21 2005/09/28 00:58:59 odf Exp $
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
    final private String centering;
    final private Matrix preliminaryBasis;
    final private Matrix latticeBasis;
    final private List gensOriginalBasis;
    final private List gensLatticeBasis;
    
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
            Object res[] = analyzePointGroup3D();
            crystalSystem = ((Integer) res[0]).intValue();
            preliminaryBasis = (Matrix) res[1];
            gensOriginalBasis = (List) res[2];
            
            // --- convert generators to preliminary basis
            final BasisChange T1 = new BasisChange(preliminaryBasis, o);
            final List gensPreliminaryBasis = convert(gensOriginalBasis, T1);
            
            // --- get primitive cell vectors and convert to preliminary basis
            final Vector primitiveCell[] = Vector.fromMatrix(G.primitiveCell());
            for (int i = 0; i < primitiveCell.length; ++i) {
                primitiveCell[i] = (Vector) primitiveCell[i].times(T1);
            }
            
            // --- compute a canonical basis based on the group's crystal system
            res = canonicalLatticeBasis(primitiveCell);
            this.latticeBasis = (Matrix) res[0];
            this.centering = (String) res[1];
            
            // --- convert generators to lattice basis
            final BasisChange T2 = new BasisChange(this.latticeBasis, o);
            this.gensLatticeBasis = convert(gensPreliminaryBasis, T2);
            
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
            z = new Vector(1, 0, 0);
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
                x = new Vector(0, 0, 1);
                if (x.isCollinearTo(z)) {
                    x = new Vector(1, 0, 0);
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
     * Takes a lattice basis and produces a canonical lattice basis and
     * centering with respect to the group's crystal system.
     * 
     * @param lattice the lattice basis.
     * @return the canonical lattice basis and centering.
     */
    private Object[] canonicalLatticeBasis(final Vector lattice[]) {
        // --- compute a lattice basis of smallest Dirichlet vectors
        final Vector reduced[] = Vector.reducedLatticeBasis(lattice, Matrix.one(3));
        final Object res[];
        
        // --- call the appropriate method for the group's crystal system
        switch (this.crystalSystem) {
        case CUBIC_SYSTEM:
            res = canonicalLatticeBasisCubic(reduced);
            break;
        case HEXAGONAL_SYSTEM:
            res = canonicalLatticeBasisHexagonal(reduced);
            break;
        case TRIGONAL_SYSTEM:
            res = canonicalLatticeBasisTrigonal(reduced);
            break;
        case TETRAGONAL_SYSTEM:
            res = canonicalLatticeBasisTetragonal(reduced);
            break;
        case ORTHORHOMBIC_SYSTEM:
            res = canonicalLatticeBasisOrthorhombic(reduced);
            break;
        case MONOCLINIC_SYSTEM:
            res = canonicalLatticeBasisMonoclinic(reduced);
            break;
        case TRICLINIC_SYSTEM:
            res = canonicalLatticeBasisTriclinic(reduced);
        default:
            throw new RuntimeException("unknown crystal system");
        }
        
        final Vector L[] = (Vector[]) res[0];
        if (Vector.volume3D(L[0], L[1], L[2]).isNegative()) {
            L[2] = (Vector) L[2].negative();
        }
        return new Object[] { Vector.toMatrix(L), res[1] };
    }

    /**
     * Takes a reduced lattice basis and produces a canonical lattice basis and
     * centering with respect to the cubic crystal system.
     * 
     * @param v the reduced lattice basis.
     * @return the canonical lattice basis and centering.
     */
    private Object[] canonicalLatticeBasisCubic(final Vector[] v) {
        int n = 0;
        int k = 3;
        for (int i = 2; i >= 0; --i) {
            if (!v[0].get(i).isZero()) {
                ++n;
                k = i;
            }
        }
        final Rational r = (Rational) v[0].get(k).abs();
        final String centering;
        final Rational a;
        if (n == 1) {
            a = r;
            centering = "P";
        } else if (n == 2) {
            a = (Rational) r.times(2);
            centering = "F";
        } else if (n == 3) {
            a = (Rational) r.times(2);
            centering = "I";
        } else {
            throw new RuntimeException("this should not happen");
        }
        final Rational o = Whole.ZERO;
        final Matrix A = new Matrix(new IArithmetic[][] {
                { a, o, o },
                { o, a, o },
                { o, o, a },
                });
        return new Object[] { Vector.rowVectors(A), centering };
    }

    /**
     * Takes a reduced lattice basis and produces a canonical lattice basis and
     * centering with respect to the hexagonal crystal system.
     * 
     * @param b the reduced lattice basis.
     * @return the canonical lattice basis and centering.
     */
    private Object[] canonicalLatticeBasisHexagonal(final Vector[] b) {
        final Vector v[];
        final Vector z = new Vector(0, 0, 1);
        if (z.isCollinearTo(b[0])) {
            v = new Vector[] { b[2], b[1], b[0] };
        } else if (z.isCollinearTo(b[1])) {
            v = new Vector[] { b[0], b[2], b[1] };
        } else {
            v = new Vector[] { b[0], b[1], b[2] };
        }
        v[1] = (Vector) v[1].times(new Operator("-y, x-y, z"));

        return new Object[] { v, "P" };
    }

    /**
     * Takes a reduced lattice basis and produces a canonical lattice basis and
     * centering with respect to the trigonal crystal system.
     * 
     * @param b the reduced lattice basis.
     * @return the canonical lattice basis and centering.
     */
    private Object[] canonicalLatticeBasisTrigonal(final Vector[] b) {
        final Vector v[];
        final Vector z = new Vector(0, 0, 1);
        if (z.isCollinearTo(b[0])) {
            v = new Vector[] { b[1], b[2], b[0] };
        } else if (z.isCollinearTo(b[1])) {
            v = new Vector[] { b[0], b[2], b[1] };
        } else {
            v = new Vector[] { b[0], b[1], b[2] };
        }

        String centering = "P";
        for (int i = 0; i < 3; ++i) {
            if (!v[i].get(2).isZero()) {
                final Vector r = v[i];
                if (!z.isCollinearTo(r)) {
                    v[2] = (Vector) r.times(new Operator("0, 0, 3z"));
                    centering = "R";
                    v[0] = (Vector) r.times(new Operator("2x-y, x+y, 0"));
                }
                break;
            }
        }
        v[1] = (Vector) v[0].times(new Operator("-y, x-y, z"));

        return new Object[] { v, centering };
    }

    /**
     * Takes a reduced lattice basis and produces a canonical lattice basis and
     * centering with respect to the tetragonal crystal system.
     * 
     * @param b the reduced lattice basis.
     * @return the canonical lattice basis and centering.
     */
    private Object[] canonicalLatticeBasisTetragonal(final Vector[] b) {
        String centering = "P";
        final Vector v[] = new Vector[] { b[0], b[1], b[2] };
        final Vector z = new Vector(0, 0, 1);
        if (z.isCollinearTo(v[0])) {
            v[2] = v[0];
            v[0] = v[1];
            if (!z.isOrthogonalTo(v[0])) {
                centering = "I";
                v[0] = (Vector) v[0].times(new Operator("x-y, x+y, 0"));
            }
        } else if (z.isOrthogonalTo(v[0])) {
            if (!z.isOrthogonalTo(v[1])) {
                v[2] = v[1];
            }
            if (!z.isCollinearTo(v[2])) {
                v[2] = (Vector) v[2].times(new Operator("0, 0, 2z"));
                centering = "I";
            }
        } else {
            centering = "I";
            v[2] = (Vector) v[0].times(new Operator("0, 0, 2z"));
            v[0] = (Vector) v[0].times(new Operator("x-y, x+y, 0"));
        }

        v[1] = (Vector) v[0].times(new Operator("-y, x, z"));

        return new Object[] { v, centering };
    }

    /**
     * Takes a reduced lattice basis and produces a canonical lattice basis and
     * centering with respect to the orthorhombic crystal system.
     * 
     * @param basis the reduced lattice basis.
     * @return the canonical lattice basis and centering.
     */
    private Object[] canonicalLatticeBasisOrthorhombic(final Vector[] basis) {
        final int d[] = new int[3];
        for (int i = 0; i < 3; ++i) {
            d[i] = 0;
            for (int j = 0; j < 3; ++j) {
                if (!basis[i].get(j).isZero()) {
                    ++d[i];
                }
            }
        }

        final Vector x = new Vector(1, 0, 0);
        final Vector y = new Vector(0, 1, 0);
        final Vector z = new Vector(0, 0, 1);
        final Vector v[];

        final int n;
        if (d[1] == 3) {
            v = new Vector[] { basis[1], basis[2], basis[0] };
            n = d[1];
        } else if (d[2] == 3) {
            v = new Vector[] { basis[2], basis[0], basis[1] };
            n = d[2];
        } else if (d[1] == 2 && basis[1].isOrthogonalTo(z)) {
            v = new Vector[] { basis[1], basis[2], basis[0] };
            n = d[1];
        } else if (d[2] == 2 && basis[2].isOrthogonalTo(z)) {
            v = new Vector[] { basis[2], basis[0], basis[1] };
            n = d[2];
        } else if (d[1] == 2 && basis[1].isOrthogonalTo(y)) {
            v = new Vector[] { basis[1], basis[2], basis[0] };
            n = d[1];
        } else if (d[2] == 2 && basis[2].isOrthogonalTo(y)) {
            v = new Vector[] { basis[2], basis[0], basis[1] };
            n = d[2];
        } else if (basis[1].isCollinearTo(x)) {
            v = new Vector[] { basis[1], basis[2], basis[0] };
            n = d[1];
        } else if (basis[2].isCollinearTo(x)) {
            v = new Vector[] { basis[2], basis[0], basis[1] };
            n = d[2];
        } else {
            v = new Vector[] { basis[0], basis[1], basis[2] };
            n = d[0];
        }

        final IArithmetic a;
        final IArithmetic b;
        final IArithmetic c;
        final String centering;

        switch (n) {
        case 3:
            final Vector u = (Vector) v[0].times(2);
            a = u.get(0);
            b = u.get(1);
            c = u.get(2);
            centering = "I";
            break;
        case 2:
            int p;
            for (p = 0; p < 3; ++p) {
                if (v[0].get(p).isZero()) {
                    break;
                }
            }
            final IArithmetic v1p = v[1].get(p);
            final IArithmetic v2p = v[2].get(p);
            final int m;
            if (v2p.isZero() || (!v1p.isZero() && v2p.abs().isGreaterThan(v1p.abs()))) {
                final Vector t = v[1];
                v[1] = v[2];
                v[2] = t;
                m = d[1];
            } else {
                m = d[2];
            }

            switch (p) {
            case 1:
                a = v[0].get(0).times(new Whole(2));
                b = v[2].get(1).times(new Whole(m));
                c = v[0].get(2).times(new Whole(2));
                centering = m == 2 ? "F" : "B";
                break;
            case 2:
                a = v[0].get(0).times(new Whole(2));
                b = v[0].get(1).times(new Whole(2));
                c = v[2].get(2).times(new Whole(m));
                centering = m == 2 ? "F" : "C";
                break;
            default:
                throw new RuntimeException("this should not happen");
            }
            break;
        case 1:
            if (!v[0].isCollinearTo(x)) {
                throw new RuntimeException("this should not happen");
            }
            final IArithmetic v11 = v[1].get(1);
            final IArithmetic v21 = v[2].get(1);
            if (v11.isZero() || (!v21.isZero() && v11.abs().isGreaterThan(v21.abs()))) {
                final Vector t = v[2];
                v[2] = v[1];
                v[1] = t;
                m = d[2];
            } else {
                m = d[1];
            }
            a = v[0].get(0);
            if (m == 2) {
                final Vector s = (Vector) v[1].times(2);
                b = s.get(1);
                c = s.get(2);
                centering = "A";
            } else {
                if (!v[1].get(1).isZero()) {
                    b = v[1].get(1);
                    c = v[2].get(2);
                } else {
                    b = v[2].get(1);
                    c = v[1].get(2);
                }
                centering = "P";
            }
            break;
        default:
            throw new RuntimeException("this should not happen");
        }

        final Rational o = Whole.ZERO;
        final Matrix A = new Matrix(new IArithmetic[][] {
                { a, o, o },
                { o, b, o },
                { o, o, c },
                });
        return new Object[] { Vector.rowVectors(A), centering };
    }

    /**
     * Takes a reduced lattice basis and produces a canonical lattice basis and
     * centering with respect to the monoclinic crystal system.
     * 
     * @param b the reduced lattice basis.
     * @return the canonical lattice basis and centering.
     */
    private Object[] canonicalLatticeBasisMonoclinic(final Vector[] b) {
        String centering = "P";
        final Vector v[];
        final Vector z = new Vector(0, 0, 1);

        if (b[0].isCollinearTo(z)) {
            v = new Vector[] { b[1], b[2], b[0] };
        } else if (b[1].isCollinearTo(z)) {
            v = new Vector[] { b[0], b[2], b[1] };
        } else {
            v = new Vector[] { b[0], b[1], b[2] };
        }

        final IArithmetic two = new Whole(2);
        IArithmetic h = null;

        if (!v[0].isOrthogonalTo(z)) {
            h = v[0].get(2).times(two);
            if (!v[1].isOrthogonalTo(z)) {
                final Vector t = new Vector(v[0]);
                v[0] = (Vector) t.plus(v[1]).times(new Operator("x, y, 0"));
                v[1] = (Vector) t.minus(v[1]).times(new Operator("x, y, 0"));
                centering = "I";
            } else {
                v[0] = (Vector) v[0].times(new Operator("2x, 2y, 0"));
                centering = "B";
            }
        } else if (!v[1].isOrthogonalTo(z)) {
            h = v[1].get(2).times(two);
            v[1] = (Vector) v[1].times(new Operator("2x, 2y, 0"));
            centering = "A";
        }

        if (!v[2].isCollinearTo(z)) {
            if (h == null) {
                h = v[2].get(2).times(two);
                if (!v[0].isOrthogonalTo(v[2])) {
                    if (!v[1].isOrthogonalTo(v[2])) {
                        centering = "I";
                    } else {
                        centering = "B";
                    }
                } else {
                    centering = "A";
                }
            }
            final IArithmetic o = Whole.ZERO;
            v[2] = new Vector(new IArithmetic[] { o, o, h });
        }

        if (centering == "B") {
            centering = "A";
            final Vector t = v[0];
            v[0] = v[1];
            v[1] = (Vector) t.negative();
        } else if (centering == "I") {
            centering = "A";
            final Vector t = v[0];
            v[0] = v[1];
            v[1] = (Vector) t.plus(v[1]).negative();
        }

        return new Object[] { v, centering };
    }

    /**
     * Takes a reduced lattice basis and produces a canonical lattice basis and
     * centering with respect to the triclinic crystal system.
     * 
     * @param b the reduced lattice basis.
     * @return the canonical lattice basis and centering.
     */
    private Object[] canonicalLatticeBasisTriclinic(final Vector[] b) {
        return new Object[] { b, "P" };
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
        return this.preliminaryBasis;
    }

    /**
     * @return the uncorrected lattice basis.
     */
    Matrix getLatticeBasis() {
        return this.latticeBasis;
    }

    /**
     * @return a set of group generators.
     */
    public List getGeneratorsOriginalBasis() {
        return this.gensOriginalBasis;
    }

    /**
     * @return group generators converted to the uncorrected lattice basis.
     */
    public List getGeneratorsLatticeBasis() {
        return this.gensLatticeBasis;
    }

    /**
     * @return the centering code.
     */
    public String getCentering() {
        return this.centering;
    }
}
