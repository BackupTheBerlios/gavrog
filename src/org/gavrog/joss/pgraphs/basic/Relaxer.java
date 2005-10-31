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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph.CoverNode;

/**
 * @author Olaf Delgado
 * @version $Id: Relaxer.java,v 1.3 2005/10/31 22:23:56 odf Exp $
 */
public class Relaxer {
    private PeriodicGraph graph;
    private Map positions;
    private int dimension;
    private HashSet neighbors;
    private HashSet secondNeighbors;
    
    private class Relation {
        public INode source;
        public INode target;
        public Vector shift;

        public Relation(final CoverNode v, final CoverNode w) {
            this.source = v.getOrbitNode();
            this.target = w.getOrbitNode();
            this.shift = (Vector) w.getShift().minus(v.getShift());
        }
        
        //TODO add appropriate hashCode() and equals()
    }
    
    public Relaxer(final PeriodicGraph graph, final Map positions) {
        this.graph = graph;
        this.positions = positions;
        this.dimension = graph.getDimension();
        this.neighbors = new HashSet();
        this.secondNeighbors = new HashSet();
        
        final Vector zero = Vector.zero(this.dimension);
        
        for (final Iterator nodes = this.graph.nodes(); nodes.hasNext();) {
            final CoverNode v = graph.new CoverNode((INode) nodes.next(), zero);
            for (final Iterator edges = v.incidences(); edges.hasNext();) {
                final IEdge e = (IEdge) edges.next();
                final CoverNode w = (CoverNode) e.target();
                this.neighbors.add(new Relation(v, w));
                for (final Iterator moreEdges = w.incidences(); moreEdges.hasNext();) {
                    final IEdge f = (IEdge) moreEdges.next();
                    final CoverNode u = (CoverNode) f.target();
                    this.secondNeighbors.add(new Relation(v, w));
                }
            }
        }
    }
}
