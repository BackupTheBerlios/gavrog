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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Takes a two- or three-dimensional crystallographic group and identifies it,
 * producing its name as according to the international tables for
 * Crystallography.
 * 
 * @author Olaf Delgado
 * @version $Id: SpaceGroupFinder.java,v 1.9 2005/09/24 03:33:48 odf Exp $
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
    final private Vector preliminaryBasis[];
    private Vector latticeBasis[];
    final private List generatorsOriginalBasis;
    private List generatorsPreliminaryBasis;
    private List generatorsLatticeBasis;
    
    /**
     * Constructs a new instance.
     * 
     * @param G the group to identify.
     */
    public SpaceGroupFinder(final SpaceGroup G) {
        final int d = G.getDimension();
        this.G = G;
        if (d == 3) {
            final Object res[] = analyzePointGroup3D();
            crystalSystem = ((Integer) res[0]).intValue();
            preliminaryBasis = (Vector[]) res[1];
            generatorsOriginalBasis = (List) res[2];
            
            
        } else if (d ==2) {
            throw new UnsupportedOperationException("dimension 2 not yet supported");
        } else {
            final String msg = "group dimension is " + d + ", must be 2 or 3";
            throw new UnsupportedOperationException(msg);
        }
    }
    
    /**
     * Analyzes the point group to determine the crystal system and find an
     * appropriate set of generators and a preliminary basis based on it.
     */
    private Object[] analyzePointGroup3D() {
        final Map type2ops = G.fundamentalOperatorsByType();
        final List generators = new ArrayList();
        Vector x = null, y = null, z = null;
        Operator R = null;
        
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
        
        final int crystalSystem;
        
        if (sixFold.size() > 0) {
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
            crystalSystem = CUBIC_SYSTEM;
            final Operator A = (Operator) fourFold.iterator().next();
            z = A.linearAxis();
            R = (Operator) threeFold.iterator().next();
            x = (Vector) z.times(R);
            y = (Vector) x.times(R);
            generators.add(A);
            generators.add(R);
        } else if (fourFold.size() > 0) {
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
            crystalSystem = CUBIC_SYSTEM;
            final Operator A = (Operator) twoFold.iterator().next();
            z = A.linearAxis();
            R = (Operator) threeFold.iterator().next();
            x = (Vector) z.times(R);
            y = (Vector) x.times(R);
            generators.add(A);
            generators.add(R);
        } else if (threeFold.size() > 0) {
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
            crystalSystem = MONOCLINIC_SYSTEM;
            final Operator A = (Operator) twoFold.iterator().next();
            z = A.linearAxis();
            generators.add(A);
        } else {
            crystalSystem = TRICLINIC_SYSTEM;
            z = new Vector(new int[] { 1, 0, 0 });
            if (inversions.size() == 0) {
                generators.add(new Operator("x+1,y,z"));
            }
        }
        
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

        if (Vector.volume3D(x, y, z).isNegative()) {
            z = (Vector) z.negative();
        }

        if (inversions.size() > 0) {
            generators.add(inversions.iterator().next());
        }

        return new Object[] { new Integer(crystalSystem), new Vector[] { x, y, z },
                generators };
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
    Vector[] getPreliminaryBasis() {
        return this.preliminaryBasis;
    }
    
    /**
     * @return a set of group generators.
     */
    public List getGeneratorsOriginalBasis() {
        return this.generatorsOriginalBasis;
    }
}
