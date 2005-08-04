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
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Whole;

/**
 * A d-dimensional point in homogeneous coordinates represented by a row vector.
 * 
 * @author Olaf Delgado
 * @version $Id: PointHomogeneous.java,v 1.1 2005/08/04 03:54:38 odf Exp $
 */
public class PointHomogeneous extends Matrix implements IPoint {
    final int dimension;

    /**
     * Create a new point from its coordinates.
     * 
     * @param coordinates the coordinates for the point.
     */
    public PointHomogeneous(final IArithmetic[] coordinates) {
        super(1, coordinates.length);
        this.dimension = coordinates.length - 1;
        for (int i = 0; i < this.dimension; ++i) {
            set(0, i, coordinates[i]);
        }
        makeImmutable();
    }
    
    /**
     * Create a new point from a given one.
     * @param p the point to copy.
     */
    public PointHomogeneous(final PointHomogeneous p) {
        super(1, p.getDimension());
        this.dimension = p.getDimension();
        setRow(0, p.getRow(0));
        makeImmutable();
    }
    
    /**
     * Create a new point from one given in cartesian coordinates.
     * @param p the point to copy.
     */
    public PointHomogeneous(final PointCartesian p) {
        super(1, p.getDimension() + 1);
        this.dimension = p.getDimension();
        for (int i = 0; i < this.dimension; ++i) {
            set(0, i, p.get(0, i));
        }
        set(0, this.dimension, Whole.ONE);
        makeImmutable();
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.joss.geometry.IPoint#getDimension()
     */
    public int getDimension() {
        return this.dimension;
    }

    /* (non-Javadoc)
     * @see org.gavrog.joss.geometry.IPoint#apply(org.gavrog.joss.geometry.IOperator)
     */
    public IPoint apply(final IOperator op) {
        return op.applyTo(this);
    }
}
