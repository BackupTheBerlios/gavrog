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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.gavrog.box.collections.Iterators;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Real;
import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph.CoverNode;

/**
 * @author Olaf Delgado
 * @version $Id: Relaxer.java,v 1.4 2005/11/01 23:24:51 odf Exp $
 */
public class Relaxer {
    final private int d;
    final private int n;
    final private Map positions;
    final private Matrix gramMatrix;
    final private List nodes;
    final private Map firstNeighbors;
    final private Map secondNeighbors;
    final private Random randomGenerator;
    
    public Relaxer(final PeriodicGraph graph, final Map positions, final Matrix gramMatrix) {
        this.d = graph.getDimension();
        this.n = graph.numberOfNodes();
        this.positions = new HashMap();
        this.positions.putAll(positions);
        this.gramMatrix = gramMatrix;
        this.nodes = Iterators.asList(graph.nodes());
        this.firstNeighbors = new HashMap();
        this.secondNeighbors = new HashMap();
        
        for (final Iterator nodes = graph.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            final Set first = new HashSet();
            final Set second = new HashSet();
            for (Iterator edges = graph.new CoverNode(v).incidences(); edges.hasNext();) {
                final CoverNode w = (CoverNode) ((IEdge) edges.next()).target();
                first.add(w);
                for (final Iterator moreEdges = w.incidences(); moreEdges.hasNext();) {
                    final CoverNode u = (CoverNode) ((IEdge) moreEdges.next()).target();
                    if (!first.contains(u)) {
                        second.add(u);
                    }
                }
            }
            firstNeighbors.put(v, first);
            secondNeighbors.put(v, second);
        }
        
        this.randomGenerator = new Random();
    }
    
    public double step() {
        final int i = this.randomGenerator.nextInt(this.n);
        final INode v = (INode) this.nodes.get(i);
        final Point pv = (Point) this.positions.get(v);
        Vector displacement = Vector.zero(this.d);
        IArithmetic weightSum = Whole.ZERO;
        final Set first = (Set) this.firstNeighbors.get(v);
        final Set second = (Set) this.secondNeighbors.get(v);
        
        for (final Iterator nodes = first.iterator(); nodes.hasNext();) {
            final CoverNode cw = (CoverNode) nodes.next();
            final INode w = cw.getOrbitNode();
            final Vector s = cw.getShift();
            final Point pw = (Point) this.positions.get(w);
            final Vector d = (Vector) pw.plus(s).minus(pv);
            final IArithmetic wt = ((Real) Vector.dot(d, d, this.gramMatrix)).sqrt();
            displacement = (Vector) displacement.plus(d.times(wt));
            weightSum = weightSum.plus(wt);
        }
        
        for (final Iterator nodes = second.iterator(); nodes.hasNext();) {
            final CoverNode cw = (CoverNode) nodes.next();
            final INode w = cw.getOrbitNode();
            final Vector s = cw.getShift();
            final Point pw = (Point) this.positions.get(w);
            final Vector d = (Vector) pw.plus(s).minus(pv);
            if (!d.isZero()) {
                final IArithmetic wt = Vector.dot(d, d, this.gramMatrix).inverse();
                displacement = (Vector) displacement.minus(d.times(wt));
                weightSum = weightSum.plus(wt);
            }
        }
        
        displacement = (Vector) displacement.dividedBy(weightSum);
        this.positions.put(v, pv.plus(displacement));
        
        final IArithmetic len = Vector.dot(displacement, displacement, this.gramMatrix);
        return Math.sqrt(((Real) len).doubleValue());
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
