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

import org.gavrog.box.collections.Iterators;
import org.gavrog.box.collections.Pair;


/**
 * Base class for covers of Delaney symbols. A cover is a new Delaney symbol
 * together with a map, say f, onto the original one, such that op(i, f(D)) =
 * f(op(i, D)) and m(i, j, f(D)) = m(i, j, D) hold for all elements D and
 * indices i, j.
 * 
 * The map f is specified in terms of the method {@link #getImage(Object)}. In
 * addition, each element of the cover is assigned to a layer, which can be
 * retrieved by the method {@link #getLayer(Object)}. Each layer has exactly
 * one element mapping to each element of the original symbol. In effect, a
 * cover is completely determined by specifying the set of layers and for each
 * index,element-pair (i, D) the layer of the neighbor op(i, D).
 * 
 * Notice that the contract for all derived classes requires that the first
 * element of the cover, as retrieved by {@link #elements()}, always maps to
 * the first element of the original symbol. Otherwise, the order of elements
 * may be arbitrary.
 * 
 * @author Olaf Delgado
 * @version $Id: Cover.java,v 1.2 2005/07/18 23:32:57 odf Exp $
 */
public abstract class Cover extends AbstractDelaneySymbol {
    protected DelaneySymbol base;

    /**
     * Constructs a Cover instance.
     * @param base the base symbol.
     */
    public Cover(DelaneySymbol base) {
    	this.base = base;
    }

    /**
     * Determines the layer of an element.
     * @param D the element.
     * @return the layer.
     */
    public Object getLayer(Object D) {
    	Object layer = ((Pair) D).getSecond();
    	if (hasLayer(layer)) {
    		return layer;
    	} else {
    		return null;
    	}
    }
    
    /**
     * Determines the image of an element in the base symbol.
     * @param D the element.
     * @return the image.
     */
    public Object getImage(Object D) {
    	Object elm = ((Pair) D).getFirst();
    	if (base.hasElement(elm)) {
    		return elm;
    	} else {
    		return null;
    	}
    }
    
    // --- Abstract methods to be implemented by derived classes:
    
    /**
     * Determines the number of layers. Obviously, this is only defined if
     * that number is finite.
     * @return the number of layers.
     */
    public abstract int numberOfLayers();
    
    /**
     * Produces an iterator over the set of layers.
     * @return an iterator over the layers.
     */
    public abstract Iterator layers();
    
    /**
     * Checks if an object is a layer of this cover.
     * @param x the object to check.
     * @return true if x is a layer.
     */
    public abstract boolean hasLayer(Object x);
    
    /**
     * Determines the neighbor layer for a given element and neighbor index.
     * @param i the neighbor index.
     * @param D the element.
     * @return the layer of the i-neighbor of D.
     */
    public abstract Object targetLayer(int i, Object D);
    
    /* --- Implementation of the DelaneySymbol interface. */
    
    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#dim()
     */
    public int dim() {
        return base.dim();
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#size()
     */
    public int size() {
        return base.size() * numberOfLayers();
    }

    /**
     * Implements {@link DelaneySymbol#elements()}, where here and in every
     * derived class, the first element retrieved by the iterator returned must
     * map to the first element of the original symbol.
     */
    public Iterator elements() {
        return Iterators.cantorProduct(base.elements(), layers());
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#isElement(java.lang.Object)
     */
    public boolean hasElement(Object D) {
        if (D instanceof Pair) {
            return hasLayer(getLayer(D)) && base.hasElement(getImage(D));
        } else {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#indices()
     */
    public Iterator indices() {
        return base.indices();
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#isIndex(int)
     */
    public boolean hasIndex(int i) {
        return base.hasIndex(i);
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#opDefined(int, java.lang.Object)
     */
    public boolean definesOp(int i, Object D) {
        return hasIndex(i) && hasElement(D)
                && base.definesOp(i, getImage(D))
                && targetLayer(i, D) != null;
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#op(int, java.lang.Object)
     */
    public Object op(int i, Object D) {
        if (!hasElement(D)) {
            throw new IllegalArgumentException("not an element: " + D);
        }
        if (!hasIndex(i)) {
            throw new IllegalArgumentException("invalid index: " + i);
        }
        if (base.definesOp(i, getImage(D)) && targetLayer(i, D) != null) {
            return new Pair(base.op(i, getImage(D)), targetLayer(i, D));
        } else {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#vDefined(int, int, java.lang.Object)
     */
    public boolean definesV(int i, int j, Object D) {
		return hasIndex(i) && hasIndex(j) && hasElement(D)
				&& base.definesV(i, j, getImage(D))
				&& base.m(i, j, getImage(D)) % r(i, j, D) == 0;
	}

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#v(int, int, java.lang.Object)
     */
    public int v(int i, int j, Object D) {
        if (!hasElement(D)) {
            throw new IllegalArgumentException("not an element: " + D);
        }
        if (!hasIndex(i)) {
            throw new IllegalArgumentException("invalid index: " + i);
        }
        if (!hasIndex(j)) {
            throw new IllegalArgumentException("invalid index: " + j);
        }
        final Object elm = getImage(D);
        final int m = base.m(i, j, elm);
        final int r = r(i, j, D);
        if (m % r != 0) {
            throw new RuntimeException(r + " is no divider of " + m);
        }
        return normalizedV(m / r);
    }
}
