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

package org.gavrog.joss.pgraphs.embed;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.gavrog.box.collections.Iterators;
import org.gavrog.jane.algorithms.Amoeba;
import org.gavrog.jane.compounds.LinearAlgebra;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.Real;
import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.geometry.Operator;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.IEdge;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;

/**
 * @author Olaf Delgado
 * @version $Id: AmoebaEmbedder.java,v 1.2 2006/03/08 22:51:10 odf Exp $
 */
public class AmoebaEmbedder extends EmbedderAdapter {
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
    public AmoebaEmbedder(final PeriodicGraph graph, final Map positions,
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
            final Matrix N = normalizedPositionSpace(v);
            
            this.node2index.put(v, new Integer(k));
            this.node2mapping.put(v, N.asDoubleArray());
            System.out.println("Mapped " + v + " to " + N);
            final Map images = getImages(v);
            for (final Iterator iter = images.keySet().iterator(); iter.hasNext();) {
                final INode w = (INode) iter.next();
                if (w == v) {
                    continue;
                }
                final Matrix M = ((Operator) images.get(w)).getCoordinates();
                this.node2index.put(w, new Integer(k));
                this.node2mapping.put(w, ((Matrix) N.times(M)).asDoubleArray());
                System.out.println("    Mapped " + w + " to " + N.times(M));
            }
            k += N.numberOfRows() - 1;
        }
        this.dimParSpace = k;
        System.out.println("dimParSpace = " + this.dimParSpace);
        
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

    public AmoebaEmbedder(final PeriodicGraph G) {
        this(G, G.barycentricPlacement(), defaultGramMatrix(G));
    }
    
   private Matrix normalizedPositionSpace(final INode v) {
       final int d = this.dimGraph;
       
       // --- get the affine subspace that is stabilized by the nodes stabilizer
       final Operator s = this.getSymmetrizer(v);
       final Matrix A = (Matrix) s.getCoordinates().minus(Matrix.one(d+1));
       final Matrix N = LinearAlgebra.rowNullSpace(A, false).mutableClone();
       
       // --- make last row encode to a point, all others vectors
       Matrix.triangulate(N, null, false, true);
       final int n = N.numberOfRows();
       for (int i = 0; i < n-1; ++i) {
           if (N.get(i, d).isZero() == false) {
               final Matrix tmp = N.getRow(n-1);
               N.setRow(n-1, N.getRow(i));
               N.setRow(i, tmp);
               break;
           }
       }
       N.setRow(n-1, (Matrix) N.getRow(n-1).dividedBy(N.get(n-1,d)));
       
       return N;
   }
   
    // --- we need to override some default implementations
    
    private double[] getPosition(final INode v, final double p[]) {
        final int d = this.dimGraph;
        final int offset = ((Integer) this.node2index.get(v)).intValue();
        final double mapping[][] = (double[][]) this.node2mapping.get(v);
        final int n = mapping.length;
        
        double loc[] = new double[d];
        for (int i = 0; i < d; ++i) {
            loc[i] = mapping[n-1][i];
            for (int j = 0; j < n-1; ++j) {
                loc[i] += p[offset + j] * mapping[j][i];
            }
        }
        return loc;
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
        final Matrix mapping = new Matrix((double[][]) this.node2mapping.get(v));
        final int n = mapping.numberOfRows();
        
        if (n > 1) {
            final int d = this.dimGraph;
            final Matrix q = new Matrix(1, d + 1);
            q.setSubMatrix(0, 0, ((Point) p.times(getSymmetrizer(v))).getCoordinates());
            q.set(0, d, Whole.ONE);
            final Matrix s = LinearAlgebra.solutionInRows(mapping, q, false);
            if (s == null) {
                System.out.println("Could not solve x * " + mapping + " = " + q);
            }

            final int offset = ((Integer) this.node2index.get(v)).intValue();
            for (int i = 0; i < n-1; ++i) {
                this.p[offset+i] = ((Real) s.get(0, i)).doubleValue();
            }
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
                if (len < 1) {
                    anglePenalty += Math.exp(1/Math.max(len, 1e-12)) - 1;
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
        final double volumePenalty = Math.exp(1/Math.max(cellVolume / n, 1e-12)) - 1;
        
        // --- compute and return the total energy
        return this.volumeWeight * volumePenalty + edgeVariance + anglePenalty;
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
        final int nPasses = 3;
        
        System.out.println("energy before optimization: " + energy.evaluate(p));
        for (int pass = 0; pass < nPasses; ++pass) {
            this.volumeWeight = Math.pow(10, nPasses - pass);
            p = new Amoeba(energy, 1e-6, steps, 10, 1.0).go(p);
            System.out.println("energy after optimization: " + energy.evaluate(p));
            for (int i = 0; i < p.length; ++i) {
                this.p[i] = p[i];
            }
            resymmetrizeCell();
            p = this.p;
            System.out.println("energy after resymmetrizing: " + energy.evaluate(p));
        }
        System.out.println();
        return steps;
    }
}