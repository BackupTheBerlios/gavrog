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
import java.util.Set;

import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.Fraction;
import org.gavrog.jane.numbers.Real;
import org.gavrog.joss.geometry.Operator;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;

/**
 * @author Olaf Delgado
 * @version $Id: EmbedderAdapter.java,v 1.3 2006/03/01 05:21:34 odf Exp $
 */
public abstract class EmbedderAdapter implements IEmbedder{
    private final PeriodicGraph graph;
    private final Map node2sym;
    private final Map node2images;
    private final Set angles;

    private boolean optimizeCell = true;
    private boolean optimizePositions = true;


    protected class Angle {
        final public INode v;
        final public INode w;
        final public Vector s;
        
        public Angle(final INode v, final INode w, final Vector s) {
            this.v = v;
            this.w = w;
            this.s = s;
        }
        
        public boolean equals(final Object other) {
            if (!(other instanceof Angle)) {
                return false;
            }
            final Angle a = (Angle) other;
            return (this.v.equals(a.v) && this.w.equals(a.w) && this.s.equals(a.s))
                   || (this.v.equals(a.w) && this.w.equals(a.v) && this.s.equals(a.s
                           .negative()));
        }
        
        public int hashCode() {
            final int hv = this.v.hashCode();
            final int hw = this.w.hashCode();
            if (hv <= hw) {
                return (hv * 37 + hw) * 37 + this.s.hashCode();
            } else {
                return (hw * 37 + hv) * 37 + this.s.negative().hashCode();
            }
        }
    }
    
    public EmbedderAdapter(final PeriodicGraph graph) {
        this.graph = graph;
        final int d = graph.getDimension();

        this.angles = makeAngles();
        if (this.angles == null) {
            throw new RuntimeException("something wrong here");
        }
        
        this.node2sym = new HashMap();
        for (final Iterator nodes = this.graph.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            this.node2sym.put(v, nodeSymmetrization(v));
        }
        
        this.node2images = new HashMap();
        for (final Iterator nodes = this.graph.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            if (!this.node2images.containsKey(v)) {
                final Map img2sym = new HashMap();
                img2sym.put(v, new Operator(Matrix.one(d+1)));
                for (final Iterator syms = this.graph.symmetries().iterator(); syms
                        .hasNext();) {
                    final Morphism a = (Morphism) syms.next();
                    final INode va = (INode) a.get(v);
                    if (!img2sym.containsKey(va)) {
                        img2sym.put(va, a.getAffineOperator());
                    }
                }
                this.node2images.put(v, img2sym);
            }
        }
    }
    
    protected static Matrix defaultGramMatrix(final PeriodicGraph graph) {
        return resymmetrized(Matrix.one(graph.getDimension()), graph);
    }
    
    protected static Matrix resymmetrized(final Matrix G, final PeriodicGraph graph) {
        // -- preparations
        final int d = graph.getDimension();
        final Set syms = graph.symmetries();
        
        // --- compute a symmetry-invariant quadratic form
        Matrix M = Matrix.zero(d, d);
        for (final Iterator iter = syms.iterator(); iter.hasNext();) {
            final Matrix A = ((Morphism) iter.next()).getLinearOperator().getCoordinates()
                    .getSubMatrix(0, 0, d, d);
            M = (Matrix) M.plus(A.times(G).times(A.transposed()));
        }
        M = ((Matrix) M.times(new Fraction(1, syms.size()))).mutableClone();
        for (int i = 0; i < d; ++i) {
            for (int j = i+1; j < d; ++j) {
                M.set(j, i, M.get(i, j));
            }
        }
        return M;
    }
    
    public void reset() {
        setPositions(getGraph().barycentricPlacement());
        setGramMatrix(defaultGramMatrix(getGraph()));
    }
    
    private Set makeAngles() {
        final HashSet result = new HashSet();
        for (final Iterator nodes = this.graph.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            final List incidences = this.graph.allIncidences(v);
            final int n = incidences.size();
            for (int i = 0; i < n-1; ++i) {
                final IEdge e1 = (IEdge) incidences.get(i);
                final INode w1 = e1.target();
                for (int j = i+1; j < n; ++j) {
                    final IEdge e2 = (IEdge) incidences.get(j);
                    final INode w2 = e2.target();
                    final Vector s = (Vector) this.graph.getShift(e2).minus(
                            this.graph.getShift(e1));
                    result.add(new Angle(w1, w2, s));
                }
            }
        }
        return result;
    }
    
    protected void resymmetrizeCell() {
        setGramMatrix(resymmetrized(getGramMatrix(), getGraph()));
    }
    
    private Operator nodeSymmetrization(final INode v) {
        final List stab = this.graph.nodeStabilizer(v);
        final Point p = (Point) this.graph.barycentricPlacement().get(v);
        final int dim = p.getDimension();
        Matrix s = Matrix.zero(dim+1, dim+1);
        for (final Iterator syms = stab.iterator(); syms.hasNext();) {
            final Operator a = ((Morphism) syms.next()).getAffineOperator();
            final Vector d = (Vector) p.minus(p.times(a));
            final Operator ad = (Operator) a.times(d);
            s = (Matrix) s.plus(ad.getCoordinates());
        }

        return new Operator((Matrix) s.dividedBy(stab.size()));
    }
        
    protected void resymmetrizePositions() {
        for (final Iterator reps = nodeOrbitReps(); reps.hasNext();) {
            final INode v = (INode) reps.next();
            final Point pv = (Point) getPosition(v).times(getSymmetrizer(v));
            setPosition(v, pv);
            final Point bv = (Point) getGraph().barycentricPlacement().get(v);
            final Map img2sym = getImages(v);
            for (final Iterator imgs = img2sym.keySet().iterator(); imgs.hasNext();) {
                final INode w = (INode) imgs.next();
                final Operator a = (Operator) img2sym.get(w);
                final Point bw = (Point) getGraph().barycentricPlacement().get(w);
                final Vector d = (Vector) bw.minus(bv.times(a));
                setPosition(w, (Point) pv.times(a).plus(d));
            }
        }
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.joss.pgraphs.basic.IEmbedder#edgeStatistics()
     */
    public double[] edgeStatistics() {
        double minLength = Double.MAX_VALUE;
        double maxLength = 0.0;
        double sumLength = 0.0;
        for (final Iterator edges = this.graph.edges(); edges.hasNext();) {
            final IEdge e = (IEdge) edges.next();
            final Point p = getPosition(e.source());
            final Point q = getPosition(e.target());
            final Vector s = this.graph.getShift(e);
            final double length = length((Vector) q.plus(s).minus(p));
            if (length > 0) {
                minLength = Math.min(minLength, length);
            }
            maxLength = Math.max(maxLength, length);
            sumLength += length;
        }
        final double avgLength = sumLength / this.graph.numberOfEdges();
        return new double[] { minLength, maxLength, avgLength };
    }

    protected double length(final Vector v) {
        final Real squareLength = (Real) Vector.dot(v, v, getGramMatrix());
        return Math.sqrt(squareLength.doubleValue());
    }

    protected Iterator angles() {
        return this.angles.iterator();
    }
    
    private Iterator nodeOrbitReps() {
        return this.node2images.keySet().iterator();
    }
    
    private Map getImages(final INode v) {
        return (Map) this.node2images.get(v);
    }
    
    private Operator getSymmetrizer(final INode v) {
        return (Operator) this.node2sym.get(v);
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.joss.pgraphs.basic.IEmbedder#normalize()
     */
    public void normalize() {
        final double avg = edgeStatistics()[2];
        if (avg < 1e-3) {
            throw new RuntimeException("degenerate unit cell while relaxing");
        }
        setGramMatrix((Matrix) getGramMatrix().dividedBy(avg * avg));
    }

    /**
     * @return the current value of graph.
     */
    public PeriodicGraph getGraph() {
        return this.graph;
    }
    
    public boolean getRelaxCell() {
        return optimizeCell;
    }
    
    public void setRelaxCell(boolean optimizeCell) {
        this.optimizeCell = optimizeCell;
    }
    
    public boolean getRelaxPositions() {
        return optimizePositions;
    }
    
    public void setRelaxPositions(boolean optimizePositions) {
        this.optimizePositions = optimizePositions;
    }
}
