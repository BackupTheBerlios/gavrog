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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.gavrog.jane.algorithms.Amoeba;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.Real;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;

/**
 * @author Olaf Delgado
 * @version $Id: AmoebaEmbedder.java,v 1.9 2006/03/02 06:28:10 odf Exp $
 */
public class AmoebaEmbedder extends EmbedderAdapter {
    private class Edge {
        public final int v;
        public final int w;
        public final double shift[];
        public final double weight;
        public double length;
        
        public Edge(final INode v, final INode w, final Vector s, final double weight) {
            this.v = ((Integer) node2index.get(v)).intValue();
            this.w = ((Integer) node2index.get(w)).intValue();
            final int d = s.getDimension();
            this.shift = new double[d];
            for (int i = 0; i < d; ++i) {
                this.shift[i] = ((Real) s.get(i)).doubleValue();
            }
            this.weight = weight;
        }
    }

    final private int dimGraph;
    final private int dimParSpace;
    final private int gramIndex[][];
    final private Map node2index;
    final private INode index2node[];
    final private Edge edges[];
    
    double volumeWeight;
    double p[];
    
    /**
     * Constructs an instance.
     * 
     * @param graph
     * @param positions
     * @param gram
     */
    public AmoebaEmbedder(final PeriodicGraph graph, final Map positions,
            final Matrix gram) {
        
        // --- generic initialization
        super(graph);
        
        // --- dimensions of the problem and parameter spaces
        final int d = this.dimGraph = graph.getDimension();
        final int n = graph.numberOfNodes();
        this.dimParSpace = d * (d+1) / 2 + d * n;
        
        // --- translations for Gram matrix parameters
        this.gramIndex = new int[d][d];
        int k = 0;
        for (int i = 0; i < dimGraph; ++i) {
            this.gramIndex[i][i] = k++;
            for (int j = i+1; j < dimGraph; ++j) {
                this.gramIndex[i][j] = this.gramIndex[j][i] = k++;
            }
        }
        
        // --- translations between nodes and parameter indices
        this.node2index = new HashMap();
        this.index2node = new INode[this.dimParSpace];
        for (final Iterator nodes = graph.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            this.node2index.put(v, new Integer(k));
            this.index2node[k] = v;
            k += d;
        }
        
        // --- the encoded list of graph edges
        this.edges = new Edge[graph.numberOfEdges()];
        k = 0;
        for (final Iterator iter = graph.edges(); iter.hasNext();) {
            final IEdge e = (IEdge) iter.next();
            this.edges[k++] = new Edge(e.source(), e.target(), graph.getShift(e), 1.0);
        }
        
        // --- initialize the parameter vector
        this.p = new double[this.dimParSpace];
        
        // --- set initial positions and cell parameters
        setPositions(positions);
        setGramMatrix(gram);
    }

    public AmoebaEmbedder(final PeriodicGraph G) {
        this(G, G.barycentricPlacement(), defaultGramMatrix(G));
    }
    
    // --- we need to override some default implementations
    
    public Point getPosition(final INode v, final double p[]) {
        final int d = this.dimGraph;
        final int offset = ((Integer) this.node2index.get(v)).intValue();
        
        final double coords[] = new double[d];
        for (int i = 0; i < d; ++i) {
            coords[i] = p[offset + i];
        }
        return new Point(coords);
    }

    public Point getPosition(final INode v) {
        return getPosition(v, this.p);
    }

    public Map getPositions() {
        final Map pos = new HashMap();
        for (final Iterator nodes = getGraph().nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            pos.put(v, getPosition(v));
        }
        return pos;
    }
    
    public void setPosition(final INode v, final Point p) {
        final int d = this.dimGraph;
        final int offset = ((Integer) this.node2index.get(v)).intValue();
        
        for (int i = 0; i < d; ++i) {
            this.p[offset + i] = ((Real) p.get(i)).doubleValue();
        }
    }

    public void setPositions(final Map map) {
        for (final Iterator nodes = map.keySet().iterator(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            setPosition(v, (Point) map.get(v));
        }
    }

    public void setGramMatrix(final Matrix gramMatrix, final double p[]) {
        final int d = this.dimGraph;
        for (int i = 0; i < d; ++i) {
            for (int j = i; j < d; ++j) {
                final int k = this.gramIndex[i][j];
                p[k] = ((Real) gramMatrix.get(i, j)).doubleValue();
            }
        }
    }

    public void setGramMatrix(final Matrix gramMatrix) {
        setGramMatrix(gramMatrix, this.p);
    }

    public Matrix getGramMatrix(final double p[]) {
        final int d = this.dimGraph;
        final Matrix gram = new Matrix(d, d);
        for (int i = 0; i < d; ++i) {
            for (int j = i; j < d; ++j) {
                final int k = this.gramIndex[i][j];
                final Real x = new FloatingPoint(p[k]);
                gram.set(i, j, x);
                gram.set(j, i, x);
            }
        }
        
        // --- adjust the gram matrix
        for (int i = 0; i < d; ++i) {
            if (gram.get(i, i).isNegative()) {
                gram.set(i, i, FloatingPoint.ZERO);
            }
        }
        for (int i = 0; i < d; ++i) {
            for (int j = i+1; j < d; ++j) {
                final Real t = ((Real) gram.get(i, i).times(gram.get(j, j))).sqrt();
                if (gram.get(i, j).isGreaterThan(t)) {
                    gram.set(i, j, t);
                    gram.set(j, i, t);
                }
            }
        }
        return gram;
    }

    public Matrix getGramMatrix() {
        return getGramMatrix(this.p);
    }

    private double energy(final double point[]) {
        // --- get some general data
        final int dim = this.dimGraph;
        final int n = getGraph().numberOfNodes();
        final int m = this.edges.length;

        // --- extract and adjust the Gram matrix
        final Matrix gram = getGramMatrix(point);
        
        // --- make it an easy to read array
        final double g[] = new double[dim * (dim+1) / 2];
        setGramMatrix(gram, g);
        
        // --- use our original coordinates if only cell is relaxed
        final double p[];
        if (getRelaxPositions() == false) {
            p = this.p;
        } else {
            p = point;
        }
        
        // --- compute variance of squared edge lengths
        double sum = 0.0;
        
        for (int k = 0; k < this.edges.length; ++k) {
            final Edge e = this.edges[k];
            final int vOff = e.v;
            final int wOff = e.w;
            final double s[] = e.shift;
            final double diff[] = new double[dim];
            for (int i = 0; i < dim; ++i) {
                diff[i] = p[wOff + i] + s[i] - p[vOff + i];
            }
            double len = 0.0;
            for (int i = 0; i < dim; ++i) {
                len += diff[i] * diff[i] * g[this.gramIndex[i][i]];
                for (int j = i+1; j < dim; ++j) {
                    len += 2 * diff[i] * diff[j] * g[this.gramIndex[i][j]];
                }
            }
            len = Math.sqrt(len);
            e.length = len;
            sum += len;
        }
        final double avg = sum / m;
        
        double variance = 0.0;
        for (int k = 0; k < this.edges.length; ++k) {
            final double len = this.edges[k].length / avg;
            final double t = (1 - len * len);
            variance += t * t;
        }
        variance /= m;
        if (variance < 0) {
            throw new RuntimeException("variance got negative: " + variance);
        }
        
        // --- compute volume per node
        final Matrix gramScaled = (Matrix) gram.dividedBy(avg * avg);
        final double cellVolume = Math.sqrt(((Real) gramScaled.determinant())
                .doubleValue());
        final double volumePerNode = Math.max(cellVolume / n, 1e-12);
        
        // --- compute the total energy
        final double sqrVol = volumePerNode * volumePerNode;
        final double energy = this.volumeWeight / sqrVol + variance;

        // --- return the result
        return energy;
    }

    /* (non-Javadoc)
     * @see org.gavrog.joss.pgraphs.basic.IEmbedder#go(int)
     */
    public int go(final int steps) {
        final Amoeba.Function energy = new Amoeba.Function() {
            public int dim() {
                if (getRelaxPositions()) {
                    return dimParSpace;
                } else {
                    return dimGraph * (dimGraph+1) / 2;
                }
            }

            public double evaluate(final double[] p) {
                return energy(p);
            }
        };
        
        // --- here's the relaxation procedure
        double p[] = this.p;
        
        System.out.println("energy before optimization: " + energy.evaluate(p));
        for (int pass = 0; pass < 10; ++pass) {
            this.volumeWeight = Math.pow(10, -pass);
            p = new Amoeba(energy, 1e-6, 5 * steps, 10, 1.0).go(p);
            System.out.println("energy after optimization: " + energy.evaluate(p));
            for (int i = 0; i < p.length; ++i) {
                this.p[i] = p[i];
            }
            resymmetrizeCell();
            resymmetrizePositions();
            p = this.p;
            System.out.println("energy after resymmetrizing: " + energy.evaluate(p));
        }
        System.out.println();
        return steps;
    }
}
