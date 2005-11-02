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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Real;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;

/**
 * @author Olaf Delgado
 * @version $Id: Relaxer.java,v 1.6 2005/11/02 04:23:36 odf Exp $
 */
public class Relaxer {
    private final PeriodicGraph graph;
    private final Map positions;
    private Matrix gramMatrix;
    
    public Relaxer(final PeriodicGraph graph, final Map positions, final Matrix gramMatrix) {
        this.graph = graph;
        this.positions = new HashMap();
        this.positions.putAll(positions);
        this.gramMatrix = gramMatrix;
    }

    public void step() {
        // --- scale so shortest edge has unit length
        IArithmetic minLength = null;
        for (final Iterator edges = this.graph.edges(); edges.hasNext();) {
            final IEdge e = (IEdge) edges.next();
            final Point p = (Point) this.positions.get(e.source());
            final Point q = (Point) this.positions.get(e.target());
            final Vector s = this.graph.getShift(e);
            final Vector d = (Vector) q.plus(s).minus(p);
            final IArithmetic length = Vector.dot(d, d, this.gramMatrix);
            if (minLength == null
                || (length.isPositive() && length.isLessThan(minLength))) {
                minLength = length;
            }
        }
        this.gramMatrix = (Matrix) this.gramMatrix.dividedBy(minLength);
        
        // --- compute displacements
        final Vector zero = Vector.zero(this.graph.getDimension());
        Vector globalPull = zero;
        final Map displacements = new HashMap();
        for (final Iterator nodes = this.graph.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            displacements.put(v, zero);
        }
        
        for (final Iterator edges = this.graph.edges(); edges.hasNext();) {
            final IEdge e = (IEdge) edges.next();
            final INode v = e.source();
            final INode w = e.target();
            final Point pv = (Point) this.positions.get(v);
            final Point pw = (Point) this.positions.get(w);
            final Vector s = this.graph.getShift(e);
            final Vector d = (Vector) pw.plus(s).minus(pv);
            final Real squareLength = (Real) Vector.dot(d, d, this.gramMatrix);
            final double length = Math.sqrt(squareLength.doubleValue());
            if (length > 1) {
                final Vector delta = (Vector) d.times(0.25 * (length - 1) / length);
                displacements.put(v, ((Vector) displacements.get(v)).plus(delta));
                displacements.put(w, ((Vector) displacements.get(w)).minus(delta));
                if (delta.isNegative()) {
                    globalPull = (Vector) globalPull.minus(delta);
                } else {
                    globalPull = (Vector) globalPull.plus(delta);
                }
            }
        }
        
        //TODO continue from here
    }

    public void setPositions(final Map map) {
        this.positions.putAll(map);
    }
    
    public Map getPositions() {
        final Map copy = new HashMap();
        copy.putAll(this.positions);
        return copy;
    }
    
    public void setGramMatrix(final Matrix gramMatrix) {
        this.gramMatrix.setSubMatrix(0, 0, gramMatrix);
    }
    
    public Matrix getGramMatrix() {
        return (Matrix) this.gramMatrix.clone();
    }
}
