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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.collections.Partition;
import org.gavrog.jane.algorithms.Amoeba;
import org.gavrog.jane.compounds.LinearAlgebra;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.Real;
import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.geometry.Operator;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.SpaceGroup;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.IEdge;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.Morphism;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;

/**
 * @author Olaf Delgado
 * @version $Id: AmoebaEmbedder.java,v 1.15 2006/03/30 05:01:51 odf Exp $
 */
public class AmoebaEmbedder extends EmbedderAdapter {
    final static boolean DEBUG = false;
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

    final private int degreesOfFreedom;
    final private int dimGraph;
    final private int dimParSpace;
    final private int dimGramSpace;
    final private Matrix gramSpace;
    final private int gramIndex[][];
    final private Map node2index;
    final private Map node2mapping;
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

        // --- set up translation between parameter space and gram matrix entries
        final SpaceGroup group = new SpaceGroup(graph.getDimension(), graph
                .symmetryOperators());
        this.gramSpace = group.configurationSpaceForGramMatrix();
        k = this.dimGramSpace = this.gramSpace.numberOfRows();
        
        // --- set up translating parameter space values into point coordinates
        this.node2index = new HashMap();
        this.node2mapping = new HashMap();

        for (final Iterator nodeReps = nodeOrbitReps(); nodeReps.hasNext();) {
            final INode v = (INode) nodeReps.next();
            final Matrix N = normalizedPositionSpace(v);
            
            this.node2index.put(v, new Integer(k));
            this.node2mapping.put(v, N.asDoubleArray());
            if (DEBUG) {
                System.out.println("Mapped " + v + " to " + N);
            }
            final Map images = getImages(v);
            for (final Iterator iter = images.keySet().iterator(); iter.hasNext();) {
                final INode w = (INode) iter.next();
                if (w == v) {
                    continue;
                }
                final Matrix M = ((Operator) images.get(w)).getCoordinates();
                this.node2index.put(w, new Integer(k));
                final Matrix NM = (Matrix) N.times(M);
                this.node2mapping.put(w, NM.asDoubleArray());
                if (DEBUG) {
                    System.out.println("    Mapped " + w + " to " + NM);
                }
                if (NM.times(nodeSymmetrization(w).getCoordinates()).equals(NM) == false) {
                    throw new RuntimeException("bad parameter space for " + w + ": " + NM
                            + " (gets 'symmetrized' to "
                            + NM.times(nodeSymmetrization(w)));
                }
            }
            k += N.numberOfRows() - 1;
        }
        this.dimParSpace = k;
        if (DEBUG) {
            System.out.println("dimParSpace = " + this.dimParSpace);
        }
        
        // --- the encoded list of graph edge orbits
        final List edgeList = new ArrayList();
        for (final Iterator iter = graph.edgeOrbits(); iter.hasNext();) {
            final Set orbit = (Set) iter.next();
            final IEdge e = (IEdge) orbit.iterator().next();
            edgeList.add(new Edge(e.source(), e.target(), graph.getShift(e), EDGE, orbit
                    .size()));
        }
        
        // --- the encoded list of next nearest neighbor (angle) orbits
        for (final Iterator iter = angleOrbits(); iter.hasNext();) {
            final Set orbit = (Set) iter.next();
            final Angle a = (Angle) orbit.iterator().next();
            edgeList.add(new Edge(a.v, a.w, a.s, ANGLE, orbit.size()));
        }
        
        this.edges = new Edge[edgeList.size()];
        edgeList.toArray(this.edges);
        
        // --- initialize the parameter vector
        this.p = new double[this.dimParSpace];
        
        // --- compute the degrees of freedom
        this.degreesOfFreedom = this.dimParSpace - 1 - translationalFreedom(group);
        
        // --- set initial positions and cell parameters
        setPositions(positions);
        setGramMatrix(gram);
    }

    static private int translationalFreedom(SpaceGroup group) {
        final int d = group.getDimension();
        final Set ops = group.primitiveOperators();
        final Matrix M = new Matrix(d, d * ops.size());
        final Matrix I = Matrix.one(d);
        int i = 0;
        for (final Iterator iter = ops.iterator(); iter.hasNext();) {
        	final Matrix op = ((Operator) iter.next()).getCoordinates();
        	final Matrix A = (Matrix) op.getSubMatrix(0, 0, d, d).minus(I);
        	M.setSubMatrix(0, i, A);
        	i += d;
        }
        final Matrix S = LinearAlgebra.rowNullSpace(M, false);
        
        return S.numberOfRows();
    }

    public AmoebaEmbedder(final PeriodicGraph G) {
        this(G, G.barycentricPlacement(), defaultGramMatrix(G));
    }
    
    public int degreesOfFreedom() {
        return this.degreesOfFreedom;
    }
    
   private Matrix normalizedPositionSpace(final INode v) {
       final int d = this.dimGraph;
       
       // --- get the affine subspace that is stabilized by the nodes stabilizer
       final Operator s = this.getSymmetrizer(v);
       final Matrix A = (Matrix) s.getCoordinates().minus(Matrix.one(d+1));
       final Matrix M = LinearAlgebra.rowNullSpace(A, false);
       
       final Matrix N = normalizedPositionSpace(M, true);
       
       if (N.times(s.getCoordinates()).equals(N) == false) {
           throw new RuntimeException("bad parameter space for " + v + ": " + N
                    + " (gets 'symmetrized' to " + N.times(s.getCoordinates()));
       }
       
       return N;
   }
   
   private Matrix normalizedPositionSpace(final Matrix M, final boolean movePivot) {
       // --- get the dimensions of the matrix
       final int n = M.numberOfRows();
       final int d = M.numberOfColumns();

       // --- make a local copy to work with
       final Matrix N = M.mutableClone();
       
       // --- not sure if this is of any use, but do it anyway
       Matrix.triangulate(N, null, false, true);
       
       if (movePivot) {
            // --- move pivot for last column into last row
            for (int i = 0; i < n - 1; ++i) {
                if (N.get(i, d - 1).isZero() == false) {
                    final Matrix tmp = N.getRow(n - 1);
                    N.setRow(n - 1, N.getRow(i));
                    N.setRow(i, tmp);
                    break;
                }
            }
        }
       
       // --- normalize the pivot
       N.setRow(n-1, (Matrix) N.getRow(n-1).dividedBy(N.get(n-1,d-1)));
       
       // --- make last column 0 in all other rows
       for (int i = 0; i < n-1; ++i) {
           N.setRow(i, (Matrix) N.getRow(i).minus(N.getRow(n-1).times(N.get(i, d-1))));
       }
       
       // --- that's it
       return N;
   }
   
   /**
    * Returns the orbits of the set of angles under the full combinatorial
    * symmetry group.
    * 
    * @return an iterator over the set of orbits.
    */
   private Iterator angleOrbits() {
       final Partition P = new Partition();
       final Map pos = getGraph().barycentricPlacement();
       for (final Iterator syms = getGraph().symmetries().iterator(); syms.hasNext();) {
           final Morphism phi = (Morphism) syms.next();
           final Operator A = phi.getAffineOperator();
           for (final Iterator angles = angles(); angles.hasNext();) {
               final Angle a = (Angle) angles.next();
               
               final INode vphi = (INode) phi.get(a.v);
               final Point pv = (Point) pos.get(a.v);
               final Vector dv = (Vector) pv.times(A).minus(pos.get(vphi));
               
               final INode wphi = (INode) phi.get(a.w);
               final Point pw = (Point) pos.get(a.w);
               final Vector dw = (Vector) pw.times(A).minus(pos.get(wphi));
               
               final Vector s = (Vector) a.s.times(A).plus(dw).minus(dv);
               
               P.unite(a, new Angle(vphi, wphi, s));
           }
       }
       return P.classes();
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
        final Matrix mapping = new Matrix((double[][]) this.node2mapping.get(v))
                .mutableClone();
        final int n = mapping.numberOfRows();
        
        if (n > 1) {
            final int d = this.dimGraph;
            final Matrix q = new Matrix(1, d + 1);
            q.setSubMatrix(0, 0, ((Point) p.times(getSymmetrizer(v))).getCoordinates());
            q.set(0, d, Whole.ONE);
            final Matrix r = mapping.getRow(n-1);
            mapping.setRow(n-1, (Matrix) r.minus(r));
            final Matrix s = LinearAlgebra.solutionInRows(mapping, (Matrix) q.minus(r),
                    false);
            if (s == null) {
                throw new RuntimeException("Could not solve x * " + mapping + " = " + q);
            }

            final int offset = ((Integer) this.node2index.get(v)).intValue();
            for (int i = 0; i < n-1; ++i) {
                this.p[offset+i] = ((Real) s.get(0, i)).doubleValue();
            }
        }
        final Vector diff = (Vector) getPosition(v).minus(p);
        if (((Real) Vector.dot(diff, diff)).sqrt().doubleValue() > 1e-12) {
            throw new RuntimeException("Position mismatch:" + v + " set to " + p
                    + ", but turned up as " + getPosition(v));
        }
    }

    public void setPositions(final Map map) {
        for (final Iterator nodes = map.keySet().iterator(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            setPosition(v, (Point) map.get(v));
        }
    }

    private double[] gramToArray(final Matrix gramMatrix) {
        final int d = this.dimGraph;
        final double p[] = new double[d * (d+1) / 2];
        for (int i = 0; i < d; ++i) {
            for (int j = i; j < d; ++j) {
                final int k = this.gramIndex[i][j];
                p[k] = ((Real) gramMatrix.get(i, j)).doubleValue();
            }
        }
        return p;
    }

    private Matrix gramFromArray(final double p[]) {
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
        return gram;
    }
        
    private void setGramMatrix(final Matrix gramMatrix, final double p[]) {
        final double g[] = gramToArray(gramMatrix);
        final Matrix S = this.gramSpace;
        final int n = S.numberOfRows();
        final Matrix M = LinearAlgebra.solutionInRows(S,
                new Matrix(new double[][] { g }), false);
        for (int i = 0; i < n; ++i) {
            p[i] = ((Real) M.get(0, i)).doubleValue();
        }
    }

    public void setGramMatrix(final Matrix gramMatrix) {
        setGramMatrix(gramMatrix, this.p);
    }

    private Matrix getGramMatrix(final double p[]) {
        final int d = this.dimGraph;
        final int m = d * (d+1) / 2;
        
        // --- extract the data for the Gram matrix
        final double g[] = new double[m];
        final Matrix S = this.gramSpace;
        final int n = S.numberOfRows();
        for (int i = 0; i < m; ++i) {
            g[i] = 0.0;
            for (int j = 0; j < n; ++j) {
                g[i] += p[j] * ((Real) S.get(j, i)).doubleValue();
            }
        }
        final Matrix gram = gramFromArray(g);
        
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

        // --- extract and adjust the Gram matrix
        final Matrix gram = getGramMatrix(point);
        
        // --- make it an easy to read array
        final double g[] = gramToArray(gram);
        
        // --- use our original coordinates if only cell is relaxed
        final double p[];
        if (getRelaxPositions() == false) {
            p = this.p;
        } else {
            p = point;
        }
        
        // --- compute variance of squared edge lengths
        double edgeSum = 0.0;
        double edgeWeightSum = 0.0;
        double minEdge = Double.MAX_VALUE;
        
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
                edgeSum += len * e.weight;;
                edgeWeightSum += e.weight;
                minEdge = Math.min(len, minEdge);
            }
        }
        final double avg = edgeSum / edgeWeightSum;
        if (edgeWeightSum != getGraph().numberOfEdges()) {
            System.out.println("edgeWeightSum is " + edgeWeightSum +", but should be " + getGraph().numberOfEdges());
        }
        final double scaling = 1 / avg;
        
        double edgeVariance = 0.0;
        double edgePenalty = 0.0;
        double anglePenalty = 0.0;
        for (int k = 0; k < this.edges.length; ++k) {
            final Edge e = this.edges[k];
            final double len = e.length * scaling;
            final double penalty;
            if (len < 0.5) {
                final double x = Math.max(len, 1e-12);
                penalty = Math.exp(Math.tan((0.25 - x) * 2.0 * Math.PI)) * e.weight;
            } else {
                penalty = 0.0;
            }
            if (e.type == EDGE) {
                final double t = (1 - len * len);
                edgeVariance += t * t * e.weight;
                edgePenalty += penalty;
            } else {
                anglePenalty += penalty;
            }
        }
        edgeVariance /= edgeWeightSum;
        if (edgeVariance < 0) {
            throw new RuntimeException("edge lengths variance got negative: "
                                       + edgeVariance);
        }
        
        // --- compute volume per node
        final Matrix gramScaled = (Matrix) gram.times(scaling * scaling);
        final double cellVolume = Math.sqrt(((Real) gramScaled.determinant())
                .doubleValue());
        final double volumePenalty = Math.exp(1/Math.max(cellVolume / n, 1e-12)) - 1;
        
        // --- compute and return the total energy
        return this.volumeWeight * volumePenalty + edgeVariance + edgePenalty
                + anglePenalty;
    }

    /* (non-Javadoc)
     * @see org.gavrog.joss.pgraphs.basic.IEmbedder#go(int)
     */
    public int go(final int steps) {
        if (dimParSpace == 0) {
            this._positionsRelaxed = getRelaxPositions();
            this._cellRelaxed = true;
            return 0;
        }
        
        final Amoeba.Function energy = new Amoeba.Function() {
            public int dim() {
                if (getRelaxPositions()) {
                    return dimParSpace;
                } else {
                    return dimGramSpace;
                }
            }

            public double evaluate(final double[] p) {
                return energy(p);
            }
        };
        
        // --- here's the relaxation procedure
        double p[] = this.p;
        final int nPasses = 3;
        
        if (DEBUG) {
            System.out.println("energy before optimization: " + energy.evaluate(p));
        }
        for (int pass = 0; pass < nPasses; ++pass) {
            this.volumeWeight = Math.pow(10, -pass);
            p = new Amoeba(energy, 1e-6, steps, 10, 1.0).go(p);
            if (DEBUG) {
                System.out.println("energy after optimization: " + energy.evaluate(p));
            }
            for (int i = 0; i < p.length; ++i) {
                this.p[i] = p[i];
            }
        }
        if (DEBUG) {
            System.out.println();
        }
        this._positionsRelaxed = getRelaxPositions();
        this._cellRelaxed = true;
        return steps;
    }
}
