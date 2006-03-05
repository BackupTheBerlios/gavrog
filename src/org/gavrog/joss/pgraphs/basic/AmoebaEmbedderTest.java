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

import org.gavrog.box.collections.Iterators;
import org.gavrog.jane.algorithms.Amoeba;
import org.gavrog.jane.compounds.LinearAlgebra;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.Real;
import org.gavrog.joss.geometry.Operator;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;

/**
 * @author Olaf Delgado
 * @version $Id: AmoebaEmbedderTest.java,v 1.1 2006/03/05 04:50:11 odf Exp $
 */
public class AmoebaEmbedderTest extends EmbedderAdapter {
    // TODO IMPORTANT: keep net symmetric during optimization
    
    final static int EDGE = 1;
    final static int ANGLE = 2;
    
    private class Edge {
        public final INode v;
        public final INode w;
        public final double shift[];
        public final int type;
        public final double weight;
        public double length;
        
        public Edge(final INode v, final INode w, final Vector s, int type, final double weight) {
            this.v = v;
            this.w = w;
            final int d = s.getDimension();
            this.shift = new double[d];
            for (int i = 0; i < d; ++i) {
                this.shift[i] = ((Real) s.get(i)).doubleValue();
            }
            this.type = type;
            this.weight = weight;
        }
    }

    final private int dimGraph;
    final private int dimParSpace;
    final private int gramIndex[][];
    final private Map node2index;
    final private Map node2mapping;
    final private int nrEdges;
    final private int nrAngles;
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
    public AmoebaEmbedderTest(final PeriodicGraph graph, final Map positions,
            final Matrix gram) {
        
        // --- generic initialization
        super(graph);
        
        // --- dimensions of the problem and parameter spaces
        final int d = this.dimGraph = graph.getDimension();
        
        // --- translations for Gram matrix parameters
        this.gramIndex = new int[d][d];
        int k = 0;
        for (int i = 0; i < dimGraph; ++i) {
            this.gramIndex[i][i] = k++;
            for (int j = i+1; j < dimGraph; ++j) {
                this.gramIndex[i][j] = this.gramIndex[j][i] = k++;
            }
        }

        // --- set up translating parameter space values into point coordinates
        this.node2index = new HashMap();
        this.node2mapping = new HashMap();

        for (final Iterator nodeReps = nodeOrbitReps(); nodeReps.hasNext();) {
            final INode v = (INode) nodeReps.next();
            final Operator s = this.getSymmetrizer(v);
            final Matrix A = (Matrix) s.getCoordinates().minus(Matrix.one(d+1));
            final Matrix N = LinearAlgebra.rowNullSpace(A, false);
            this.node2index.put(v, new Integer(k));
            this.node2mapping.put(v, N.asDoubleArray());
            final Map images = getImages(v);
            for (final Iterator iter = images.keySet().iterator(); iter.hasNext();) {
                final INode w = (INode) iter.next();
                final Matrix M = ((Operator) images.get(w)).getCoordinates();
                this.node2index.put(w, new Integer(k));
                this.node2mapping.put(w, ((Matrix) N.times(M)).asDoubleArray());
            }
            k += N.numberOfRows();
        }
        this.dimParSpace = k;
        
        // --- the encoded list of graph edges
        this.nrEdges = graph.numberOfEdges();
        this.nrAngles = Iterators.size(this.angles());
        this.edges = new Edge[this.nrEdges + this.nrAngles];
        k = 0;
        for (final Iterator iter = graph.edges(); iter.hasNext();) {
            final IEdge e = (IEdge) iter.next();
            this.edges[k++] = new Edge(e.source(), e.target(), graph.getShift(e), EDGE,
                    1.0);
        }
        
        // --- the encoded list of next nearest neighbors (angles)
        for (final Iterator iter = this.angles(); iter.hasNext();) {
            final Angle a = (Angle) iter.next();
            this.edges[k++] = new Edge(a.v, a.w, a.s, ANGLE, 1.0);
        }
        
        // --- initialize the parameter vector
        this.p = new double[this.dimParSpace];
        
        // --- set initial positions and cell parameters
        setPositions(positions);
        setGramMatrix(gram);
    }

    public AmoebaEmbedderTest(final PeriodicGraph G) {
        this(G, G.barycentricPlacement(), defaultGramMatrix(G));
    }
    
   
    // --- we need to override some default implementations
    
    private double[] getPosition(final INode v, final double p[]) {
        final int d = this.dimGraph;
        final int offset = ((Integer) this.node2index.get(v)).intValue();
        final double mapping[][] = (double[][]) this.node2mapping.get(v);
        final int n = mapping.length;
        
        double f = 0.0;
        for (int j = 0; j < n; ++j) {
            f += p[offset + j] * mapping[j][d];
        }
        final double coords[] = new double[d];
        for (int i = 0; i < d; ++i) {
            coords[i] = 0;
            for (int j = 0; j < n; ++j) {
                coords[i] += p[offset + j] * mapping[j][i];
            }
            coords[i] /= f;
        }
        return coords;
    }

    public Point getPosition(final INode v) {
        return new Point(getPosition(v, this.p));
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
        final Matrix A = new Matrix((double[][]) this.node2mapping.get(v));
        final Matrix b = new Matrix(1, d+1);
        b.set(0, d, FloatingPoint.ONE);
        b.setSubMatrix(0, 0, ((Point) p.times(getSymmetrizer(v))).getCoordinates());
        final Matrix x = LinearAlgebra.solutionInRows(A, b, true);
        System.out.println("A = " + A + ", b = " + b + ", x = " + x);
        
        final int offset = ((Integer) this.node2index.get(v)).intValue();
        for (int i = 0; i < d; ++i) {
            this.p[offset + i] = ((Real) x.get(0, i)).doubleValue();
        }
    }

    public void setPositions(final Map map) {
        for (final Iterator nodes = map.keySet().iterator(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            setPosition(v, (Point) map.get(v));
        }
    }

    private void setGramMatrix(final Matrix gramMatrix, final double p[]) {
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

    private Matrix getGramMatrix(final double p[]) {
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

    // --- the following methods do the actual optimization
    
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
        double edgeSum = 0.0;
        double angleSum = 0.0;
        
        for (int k = 0; k < this.edges.length; ++k) {
            final Edge e = this.edges[k];
            final double pv[] = getPosition(e.v, p);
            final double pw[] = getPosition(e.w, p);
            final double s[] = e.shift;
            final double diff[] = new double[dim];
            for (int i = 0; i < dim; ++i) {
                diff[i] = pw[i] + s[i] - pv[i];
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
            if (e.type == EDGE) {
                edgeSum += len;
            } else {
                angleSum += len;
            }
        }
        final double avg = edgeSum / m;
        
        double edgeVariance = 0.0;
        double anglePenalty = 0.0;
        for (int k = 0; k < this.edges.length; ++k) {
            final Edge e = this.edges[k];
            final double len = e.length / avg;
            if (e.type == EDGE) {
                final double t = (1 - len * len);
                edgeVariance += t * t;
            } else {
                if (len < 1.0) {
                    anglePenalty += Math.pow(len, -4) - 1.0;
                }
            }
        }
        edgeVariance /= m;
        if (edgeVariance < 0) {
            throw new RuntimeException("edge lengths variance got negative: "
                                       + edgeVariance);
        }
        
        // --- compute volume per node
        final Matrix gramScaled = (Matrix) gram.dividedBy(avg * avg);
        final double cellVolume = Math.sqrt(((Real) gramScaled.determinant())
                .doubleValue());
        final double vol = Math.max(cellVolume / n, 1e-12);
        
        // --- compute and return the total energy
        return this.volumeWeight / vol + edgeVariance + 1000 * anglePenalty;
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
            this.volumeWeight = Math.pow(10, 5 - pass);
            p = new Amoeba(energy, 1e-6, 5 * steps, 10, 1.0).go(p);
            System.out.println("energy after optimization: " + energy.evaluate(p));
            for (int i = 0; i < p.length; ++i) {
                this.p[i] = p[i];
            }
            //resymmetrizeCell();
            //resymmetrizePositions();
            p = this.p;
            System.out.println("energy after resymmetrizing: " + energy.evaluate(p));
        }
        System.out.println();
        return steps;
    }
}
