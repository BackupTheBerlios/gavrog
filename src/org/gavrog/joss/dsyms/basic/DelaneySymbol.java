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

package org.gavrog.joss.dsyms.basic;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gavrog.box.NiceIntList;
import org.gavrog.jane.numbers.Rational;



/**
 * Rich interface for Delaney symbol (or D-symbol for short) classes.
 * 
 * @author Olaf Delgado
 * @version $Id: DelaneySymbol.java,v 1.2 2005/07/18 23:03:54 odf Exp $
 */

public interface DelaneySymbol extends Comparable {
    /**
     * Returns the dimension (one less than the number of indices) of this
     * symbol.
     * @return the dimension.
     */
    public int dim();

    /**
     * Returns the size (the number of elements) of this symbol. This is only
     * defined for finite symbols.
     * @return the size.
     * @throws UnsupportedOperationException if infinite or of unknown size.
     */
    public int size() throws UnsupportedOperationException;
    
    /**
     * @return an iterator over the elements of the symbol.
     */
    public Iterator elements();
    
    /**
     * Tests if an object is an element of this symbol.
     * @param D the object to test.
     * @return true if D is an element of this symbol.
     */
    public boolean hasElement(Object D);
    
    /**
     * @return an iterator over the valid neighbor indices for this symbol.
     */
    public Iterator indices();
    
    /**
     * Tests if a number is a valid index for this symbol.
     * @param i the number to test.
     * @return true if i is a valid index for this symbol.
     */
    public boolean hasIndex(int i);
    
    /**
     * Tests if the indices for this symbol run from 0 to dim() without gaps.
     * @return true if indices run from 0 to dim() without gaps.
     */
    public boolean hasStandardIndexSet();
    
    /**
     * Tests if a specific neighbor of an element exists.
     * @param i the neighbor index.
     * @param D the original element.
     * @return true if the i-neighbor of D exists..
     */
    public boolean definesOp(int i, Object D);
    
    /**
     * Returns a specific neighbor of an element.
     * @param i the neighbor index.
     * @param D the original element.
     * @return the i-neighbor of D.
     */
    public Object op(int i, Object D);
    
    /**
     * Tests if a certain branching number is defined..
     * @param i the first index.
     * @param j the second index.
     * @param D the element.
     * @return true if the (i,j) branching number of D exists.
     */
    public boolean definesV(int i, int j, Object D);
    
    /**
     * Tests if op and v are defined for all feasible elements and indices.
     * @return true if op and v are defined everywhere.
     */
    public boolean isComplete();
    
    /**
     * Returns a specific branching number (aka branching limit).
     * @param i the first index.
     * @param j the second index.
     * @param D the element.
     * @return the (i,j) branching number (branching limit) of D.
     */
    public int v(int i, int j, Object D);
    
    /**
     * Returns the smallest positive number of double steps which leads from
     * an element back to itself via an alternating sequence of i- and
     * j-neighbors.
     * @param i the first index.
     * @param j the second index.
     * @param D the element.
     * @return the smallest number of returning (i,h) double steps.
     */
    public int r(int i, int j, Object D);
    
    /**
     * Returns the product of v and r.
     * @param i the first index.
     * @param j the second index.
     * @param D the element.
     * @return <code>v(i,j,D) * r(i,j,D)</code>
     */
    public int m(int i, int j, Object D);

    /**
     * Counts the number of connected components (orbits) determined by the
     * specified collection of indices.
     * @param indices the indices to use.
     * @return the number of orbits.
     */
    public int numberOfOrbits(List indices);
    
    /**
     * Determines if the symbol is connected.
     * @return true if the symbol is connected.
     */
    public boolean isConnected();
    
    /**
     * Returns one element in each connected component (orbit) determined by
     * the specified collection of indices.
     * @param indices the indices to use.
     * @return an iterator containing one representative for each orbit.
     */
    public Iterator orbitRepresentatives(List indices);
    
    /**
     * Returns the elements of the connected component (orbit) determined by
     * the given set of indices and the given seed.
     * @param indices the indices to use.
     * @param seed one element in the orbit.
     * @return an iterator containing the elements of the orbit.
     */
    public Iterator orbit(List indices, Object seed);
    
    /**
     * Produces a map which assigns to every element of the symbol which is
     * reached by a specific partial traversal a value of 1 or -1, where two
     * elements connected by a traversal edge obtain different values.
     * 
     * @param indices the indices for the traversal.
     * @param seeds the seeds for the traversal.
     * @return the resulting orientation map.
     */
    public Map partialOrientation(List indices, Iterator seeds);
    
    /**
     * This version uses a complete traversal.
     * @return the orientation map.
     */
    public Map partialOrientation();
    
    /**
     * Determines if a specified orbit is oriented. This is the case if the
     * partial orientation has the property that for any pair of elements in
     * the orbit connected by one of the specified indices, the orientation
     * values are different.
     * @param indices the indices to use.
     * @param seed the seed for the orbit.
     * @return true if the orbit is oriented.
     */
    public boolean orbitIsOriented(List indices, Object seed);
    
    /**
     * This version checks if the symbol as a whole is oriented.
     * @return true if the symbol is oriented.
     */
    public boolean isOriented();
    
    /**
     * Produces a list of integers that is characteristic for this symbol. The
     * result is identical to the invariant of another symbol if and only if
     * the two symbols are isomorphic.
     * @return the invariant integer list of this symbol.
     */
    public NiceIntList invariant();
    
    /**
     * Produces canonical form for this symbol. A canonical form is a symbol
     * which is isomorphic to the given one and has the property that any pair
     * of isomorphic symbols will have identical canonical forms.
     * @return the symbol in canonical form.
     */
    public DelaneySymbol canonical();
    
    /**
     * Returns the mapping of elements which transforms this symbol into its
     * canonical form. If there is more than one such mapping, the one with the
     * earliest preimage of 1, with respect to the original order of elements,
     * is returned.
     * 
     * @return the map from the original to the canonical symbol.
     */
    public Map getMapToCanonical();
    
    /**
     * Tests if this symbol is minimal. A symbol is minimal if it has no non-trivial
     * Delaney morphism image.
     * 
     * @return true is the symbol is minimal, false else.
     */
    public boolean isMinimal();
    
    /**
     * Produces the minimal image of this symbol by Delaney symbol morphisms.
     * @return the minimal image.
     */
    public DelaneySymbol minimal();
    
    /**
     * Produces a symbol isomorphic to this and with the same order of elements
     * and indices.
     * 
     * @return the new symbol.
     */
    public DelaneySymbol flat();

    /**
     * Computes the curvature of this symbol, which must be 2-dimensional.
     * @return the curvature.
     */
    public Rational curvature2D();
    
    /**
     * Computes the size of the spherical symmetry group of the equivariant
     * tiling corresponding to this symbol. This is only defined for
     * 2-dimensional, spherical symbols.
     * @return the symmetry group size.
     */
    public int sphericalGroupSize2D();
    
    /**
     * Determines if this symbol is spherical. Only defined for 2-dimensional
     * symbols.
     * @return true if this symbol is spherical.
     */
    public boolean isSpherical2D();
    
    /**
     * Determines if this symbol is locally euclidean (all 2-dimensional orbits
     * are spherical). This is only defined for 3-dimensional symbols.
     * @return true if this symbol is locally euclidean.
     */
    public boolean isLocallyEuclidean3D();
    
    /**
     * Constructs the oriented cover for this symbol. For an oriented symbol,
     * the cover is the symbol itself, otherwise it is a 2-fold cover which is
     * oriented.
     * @return the oriented cover.
     */
    public DelaneySymbol orientedCover();
    
    /**
     * @return a string representation of the symbol.
     */
    public String toString();

    /**
     * Produces a string containing a tabular display of this symbol.
     * @return a tabular display of the operations and branchings.
     */
    public String tabularDisplay();
    
    /**
     * Determines if an undefined v-value is returned as 1 rather than 0.
     * @param value if true, undefined v-values shall be assumed to be 1.
     */
    public void setVDefaultToOne(final boolean value);
    
    /**
     * Checks if an undefined v-value would currently be returned as 1 rather than 0.
     * @return true if undefined v-values are currently assumed to be 1.
     */
    public boolean isVDefaultToOne();
}
