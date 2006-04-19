/*
Copyright 2006 Olaf Delgado-Friedrichs

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.collections.Pair;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.joss.geometry.CoordinateChange;
import org.gavrog.joss.geometry.Operator;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;

/**
 * Represents a cover of a periodic graph.
 * 
 * @author Olaf Delgado
 * @version $Id: Cover.java,v 1.2 2006/04/06 01:12:51 odf Exp $
 */
public class Cover extends PeriodicGraph {
    final private Morphism coverMorphism;
    final private PeriodicGraph image;
    
    /**
     * Constructs an instance.
     * 
     * @param image the image graph.
     * @param cell the unit cell for the cover, in terms of the original unit cell.
     */
    public Cover(final PeriodicGraph image, final Vector cell[]) {
        super(image.getDimension());
        
        // --- check the arguments
        if (cell == null) {
            throw new IllegalArgumentException("No cell given.");
        }
        final int dim = image.getDimension();
        if (cell.length != dim) {
            throw new IllegalArgumentException("Expected exactly " + dim
                    + " cell vectors.");
        }
        for (int i = 0; i < cell.length; ++i) {
            if (!cell[i].isIntegral()) {
                throw new IllegalArgumentException("Cell vectors must be integral.");
            }
            if (cell[i].getDimension() != dim) {
                throw new IllegalArgumentException(
                        "Cell vectors must have same dimension as base graph.");
            }
        }
        
        // --- compute transformation to new coordinates 
        final Matrix M = Vector.toMatrix(cell);
        if (M.rank() < dim) {
            throw new IllegalArgumentException("Cell vectors must form a basis.");
        }
        final CoordinateChange C = new CoordinateChange(M);
        
        // --- find translation representatives modulo the new unit lattice
        final Set translations = new HashSet();
        final int d = getDimension();
        for (int i = 0; i < d; ++i) {
            final Vector e = Vector.unit(d, i);
            final Vector b = ((Vector) e.times(C)).modZ();
            if (!translations.contains(b)) {
                translations.add(b);
            }
        }
        final LinkedList Q = new LinkedList();
        Q.addAll(translations);
        final Set gens = new HashSet();
        gens.addAll(translations);
        while (Q.size() > 0) {
            final Vector s = (Vector) Q.removeFirst();
            for (final Iterator iter = gens.iterator(); iter.hasNext();) {
                final Vector t = (Vector) iter.next();
                final Vector sum = ((Vector) s.plus(t)).modZ();
                if (!translations.contains(sum)) {
                    translations.add(sum);
                    Q.addFirst(sum);
                }
            }
        }

        // --- find node and edge representatives in the new coordinate system
        final Map pos = image.barycentricPlacement();
        final List transformedNodes = new ArrayList();
        for (final Iterator iter = image.nodes(); iter.hasNext();) {
            final INode v = (INode) iter.next();
            transformedNodes.add(((Point) pos.get(v)).times(C));
        }
        final List transformedEdges = new ArrayList();
        for (final Iterator iter = image.edges(); iter.hasNext();) {
            final IEdge e = (IEdge) iter.next();
            final Point p = (Point) pos.get(e.source());
            final Point q = (Point) pos.get(e.target());
            final Point src = (Point) p.times(C);
            final Point dst = (Point) q.plus(image.getShift(e)).times(C);
            transformedEdges.add(new Pair(src, dst));
        }

        // --- extend the system of representatives to the new unit cell
        final List coverNodes = new ArrayList();
        
        for (final Iterator iter = transformedNodes.iterator(); iter.hasNext();) {
            final Point p = (Point) iter.next();
            for (final Iterator shifts = translations.iterator(); shifts.hasNext();) {
                final Vector v = (Vector) shifts.next();
                final Point pv = (Point) p.plus(v);
                coverNodes.add(pv.modZ());
            }
        }
        
        final List coverEdges = new ArrayList();
        for (final Iterator iter = transformedEdges.iterator(); iter.hasNext();) {
            final Pair e = (Pair) iter.next();
            final Point p = (Point) e.getFirst();
            final Point q = (Point) e.getSecond();
            for (final Iterator shifts = translations.iterator(); shifts.hasNext();) {
                final Vector v = (Vector) shifts.next();
                final Point pv = (Point) p.plus(v);
                final Point qv = (Point) q.plus(v);
                final Point rv = pv.modZ();
                coverEdges.add(new Pair(rv, qv.minus(pv).plus(rv)));
            }
        }

        // --- extract a representation of the new graph
        final Map pos2node = new HashMap();
        final Map node2pos = new HashMap();
        for (final Iterator iter = coverNodes.iterator(); iter.hasNext();) {
            final Point p = (Point) iter.next();
            final INode v = newNode();
            pos2node.put(p, v);
            node2pos.put(v, p);
        }
        for (final Iterator iter = coverEdges.iterator(); iter.hasNext();) {
            final Pair e = (Pair) iter.next();
            final Point p = (Point) e.getFirst();
            final Point q = (Point) e.getSecond();
            final Point r = q.modZ();
            final Vector s = (Vector) q.minus(r);
            final INode v = (INode) pos2node.get(p);
            final INode w = (INode) pos2node.get(r);
            newEdge(v, w, s);
        }
        
        // --- we do not need to recompute the cover's barycentric placement
        cache.put(BARYCENTRIC_PLACEMENT, node2pos);
        
        // --- compute the cover morphism
        this.coverMorphism = new Morphism((INode) this.nodes().next(), (INode) image
                .nodes().next(), ((CoordinateChange) C.inverse()).getOperator());
        
        // --- store some additional data
        this.image = image;
    }
    
    /**
     * @return the cover morphism.
     */
    public Morphism getCoverMorphism() {
        return this.coverMorphism;
    }
    
    /**
     * @return the image.
     */
    public PeriodicGraph getImage() {
        return this.image;
    }
    
    /**
     * Get the image of a node under the cover morphism.
     * 
     * @param v the original node.
     * @return the image.
     */
    public INode image(final INode v) {
        return (INode) getCoverMorphism().get(v);
    }
    
    /**
     * Computes the position of a node with respect to the cover's coordinate system
     * given the position of it's image under the cover morphism in the image's coordinate
     * system.
     * 
     * @param v the node.
     * @param p the position of its image.
     * @return the corresponding position of v.
     */
    public Point liftedPosition(final INode v, final Point p) {
        final PeriodicGraph image = this.getImage();
        final Operator A = getCoverMorphism().getAffineOperator();
        final Point b = (Point) this.barycentricPlacement().get(v);
        final Point bA = (Point) b.times(A);
        final Vector d = (Vector) bA.minus(image.barycentricPlacement().get(image(v)));
        return (Point) p.plus(d).times(A.inverse());
    }
    
    /**
     * Computes the position of a node with respect to the cover's coordinate system
     * given the position of it's image under the cover morphism in the image's coordinate
     * system.
     * 
     * @param v the node.
     * @param pos a map assigning positions to image nodes.
     * @return the corresponding position of v.
     */
    public Point liftedPosition(final INode v, final Map pos) {
        return liftedPosition(v, (Point) pos.get(image(v)));
    }
}