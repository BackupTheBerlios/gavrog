/*
   Copyright 2007 Olaf Delgado-Friedrichs

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
import java.util.LinkedList;
import java.util.Map;

import org.gavrog.box.collections.Pair;
import org.gavrog.joss.geometry.CoordinateChange;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;

/**
 * Implements an embedded portion of an infinite graph with methods to
 * retrieve the addresses of elements in terms of the representation graph.
 *
 * @author Olaf Delgado
 * @version $Id: EmbeddedPortion.java,v 1.2 2007/04/24 19:59:51 odf Exp $
 */
public class EmbeddedPortion extends Embedding {
    final private PeriodicGraph graph;
    final private Map elementIdToAddress = new HashMap();
    final private Map addressToElementId = new HashMap();
    final private Map placement;
    final private CoordinateChange basis;
    
    public EmbeddedPortion(final PeriodicGraph graph, final Map placement,
            final CoordinateChange basis) {
        super(new UndirectedGraph());
        this.graph = graph;
        this.placement = placement;
        this.basis = basis;
    }
    
    public PeriodicGraph getRepresentationGraph() {
        return this.graph;
    }
    
    public INode newNode(final INode rep, final Vector shift) {
        if (getElement(rep, shift) != null) {
            throw new IllegalArgumentException("node already exists");
        }
        final INode v = getGraph().newNode();
        final Pair adr = new Pair(rep, shift);
        this.elementIdToAddress.put(v.id(), adr);
        this.addressToElementId.put(adr, v.id());
        return v;
    }
    
    public IEdge newEdge(final IEdge rep, final Vector shift) {
        if (getElement(rep, shift) != null) {
            throw new IllegalArgumentException("edge already exists");
        }
        final INode sourceRep = rep.source();
        final INode targetRep = rep.target();
        final Vector sourceShift = shift;
        final Vector targetShift = (Vector) shift.plus(this.graph.getShift(rep));

        final Pair sourceAdr = new Pair(sourceRep, sourceShift);
        final Object sourceId = this.addressToElementId.get(sourceAdr);
        final INode source = (INode) getGraph().getElement(sourceId);
        if (source == null) {
            throw new UnsupportedOperationException("source node must be present");
        }

        final Pair targetAdr = new Pair(targetRep, targetShift);
        final Object targetId = this.addressToElementId.get(targetAdr);
        final INode target = (INode) getGraph().getElement(targetId);
        if (target == null) {
            throw new UnsupportedOperationException("target node must be present");
        }
        
        final IEdge e = getGraph().newEdge(source, target);
        final Pair adr = new Pair(rep, shift);
        this.elementIdToAddress.put(e.id(), adr);
        this.addressToElementId.put(adr, e.id());
        final Vector t = getRepresentationGraph().getShift(rep);
        final Pair revAdr = new Pair(rep.reverse(), shift.plus(t));
        this.addressToElementId.put(revAdr, e.id());
        return null;
    }
    
    public void setPosition(final INode v, final Point p) {
        throw new UnsupportedOperationException("not allowed");
    }
    
    public IGraphElement getElement(final IGraphElement rep, final Vector shift) {
        final Object id = this.addressToElementId.get(new Pair(rep, shift));
        return getGraph().getElement(id);
    }
    
    public IGraphElement getRepresentative(final IGraphElement x) {
        if (!getGraph().hasElement(x)) {
            throw new IllegalArgumentException("no such node or edge");
        }
        final Pair adr = (Pair) elementIdToAddress.get(x.id());
        return (IGraphElement) adr.getFirst();
    }
    
    public Vector getShift(final IGraphElement x) {
        if (!getGraph().hasElement(x)) {
            throw new IllegalArgumentException("no such node or edge");
        }
        final Pair adr = (Pair) elementIdToAddress.get(x.id());
        return (Vector) adr.getSecond();
    }

    public Point getPosition(final INode v) {
        final Point p = (Point) this.placement.get(getRepresentative(v));
        return (Point) p.plus(getShift(v)).times(this.basis);
    }

    /**
     * Constructs a finite portion of a periodic graph in the form of an
     * ordinary graph with nodes labelled by their position in a given
     * embedding.
     * 
     * @param v0 the central node of the finite portion to construct.
     * @param radius the maximum graph distance from the central node.
     * @param positions coordinates for node representatives.
     * @param basis the basis to be used to convert into cartesian coordinates.
     * 
     * @return the newly constructed graph.
     */
    public static Embedding neighborhood(final INode v0, final int radius,
            final Map positions, final CoordinateChange basis) {

        final PeriodicGraph graph = (PeriodicGraph) v0.owner();
        final EmbeddedPortion result = new EmbeddedPortion(graph, positions,
                basis);
        final Map nodeToDist = new HashMap();
        final LinkedList queue = new LinkedList();
        final int d = graph.getDimension();
        
        final INode w0 = result.newNode(v0, Vector.zero(d));
        nodeToDist.put(w0, new Integer(0));
        queue.addLast(w0);
        
        while (queue.size() > 0) {
            final INode wOld = (INode) queue.removeFirst();
            final Integer distOld = (Integer) nodeToDist.get(wOld);
            final Integer distNew = new Integer(distOld.intValue() + 1);
            
            if (distNew.intValue() <= radius) {
                final INode vOld = (INode) result.getRepresentative(wOld);
                final Vector tOld = result.getShift(wOld);
                for (final Iterator iter = graph.allIncidences(vOld).iterator(); iter
                        .hasNext();) {
                    final IEdge e = (IEdge) iter.next();
                    final INode vNew = e.target();
                    final Vector tNew = (Vector) tOld.plus(graph.getShift(e));
                    INode wNew = (INode) result.getElement(vNew, tNew);
                    if (wNew == null) {
                        wNew = result.newNode(vNew, tNew);
                        nodeToDist.put(wNew, distNew);
                        queue.addLast(wNew);
                    }
                    if (result.getElement(e, tOld) == null) {
                        result.newEdge(e, tOld);
                    }
                }
            }
        }
        
        return result;
    }
}