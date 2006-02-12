/*
 * Copyright 2005 Olaf Delgado-Friedrichs
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.gavrog.joss.pgraphs.basic;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Real;
import org.gavrog.joss.geometry.Operator;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.SpaceGroup;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.io.NetParser;

/**
 * @author Olaf Delgado
 * @version $Id: SpringEmbedder.java,v 1.18 2006/02/12 04:30:46 odf Exp $
 */
public class SpringEmbedder {
    private final PeriodicGraph graph;
    private final Map positions;
    private final Map node2sym;
    private Matrix gramMatrix;
    private Operator gramProjection;
    private double lastPositionChangeAmount = 0;
    private double lastCellChangeAmount = 0;
    private boolean optimizeCell = true;
    private boolean optimizePositions = true;
    private final Set angles;

    private class Angle {
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
    
    public SpringEmbedder(final PeriodicGraph graph, final Map positions,
            final Matrix gramMatrix) {
        this.graph = graph;
        this.positions = new HashMap();
        this.positions.putAll(positions);
        this.gramMatrix = gramMatrix;

        final int d = graph.getDimension();
        final SpaceGroup G = new SpaceGroup(d, graph.symmetryOperators());
        final Matrix M = G.configurationSpaceForGramMatrix();
        this.gramProjection = Operator.orthogonalProjection(M, Matrix
                .one(d * (d + 1) / 2));
        
        this.angles = angles();
        if (this.angles == null) {
            throw new RuntimeException("something wrong here");
        }
        
        this.node2sym = new HashMap();
        for (final Iterator nodes = this.graph.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            this.node2sym.put(v, nodeSymmetrization(v));
        }
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
    
    private Set angles() {
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
    
    public SpringEmbedder(final PeriodicGraph G) {
        this(G, G.barycentricPlacement(), gram(G));
    }
    
    private static Matrix gram(final PeriodicGraph G) {
        final Matrix M = G.symmetricBasis();
        return (Matrix) M.times(M.transposed());
    }
    
    public double[] edgeStatistics() {
        double minLength = Double.MAX_VALUE;
        double maxLength = 0.0;
        double sumLength = 0.0;
        for (final Iterator edges = this.graph.edges(); edges.hasNext();) {
            final IEdge e = (IEdge) edges.next();
            final Point p = (Point) this.positions.get(e.source());
            final Point q = (Point) this.positions.get(e.target());
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

    private double length(final Vector v) {
        final Real squareLength = (Real) Vector.dot(v, v, this.gramMatrix);
        return Math.sqrt(squareLength.doubleValue());
    }

    private void move(final Map pos, final INode v, final Vector amount) {
        pos.put(v, ((IArithmetic) pos.get(v)).plus(amount));
    }

    public void normalize() {
        final double avg = edgeStatistics()[2];
        this.gramMatrix = (Matrix) this.gramMatrix.dividedBy(avg * avg);
    }

    public void step() {
        // TODO keep symmetries intact

        // --- scale to make unit average edge length
        normalize();

        // --- initialize displacements
        final Map deltas = new HashMap();
        final Vector zero = Vector.zero(this.graph.getDimension());
        for (final Iterator nodes = this.graph.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            deltas.put(v, zero);
        }

        // --- compute displacements
        //    ... using edge forces
        for (final Iterator edges = this.graph.edges(); edges.hasNext();) {
            final IEdge e = (IEdge) edges.next();
            final INode v = e.source();
            final INode w = e.target();
            final Point pv = (Point) this.positions.get(v);
            final Point pw = (Point) this.positions.get(w);
            final Vector s = this.graph.getShift(e);
            final Vector d = (Vector) pw.plus(s).minus(pv);
            final double length = length(d);
            if (length > 1.0) {
                final Vector movement = (Vector) d.times(1.0 - 1.0 / length);
                move(deltas, v, (Vector) movement.times(0.5));
                move(deltas, w, (Vector) movement.times(-0.5));
            }
        }
        //    ... using angle forces
        for (final Iterator angles = this.angles.iterator(); angles.hasNext();) {
            final Angle a = (Angle) angles.next();
            final INode v = a.v;
            final INode w = a.w;
            final Point pv = (Point) this.positions.get(v);
            final Point pw = (Point) this.positions.get(w);
            final Vector s = a.s;
            final Vector d = (Vector) pw.plus(s).minus(pv);
            final double length = length(d);
            if (length < 1.0) {
                final Vector movement = (Vector) d.times(1.0 - 1.0 / length);
                // --- smaller than edge forces
                move(deltas, v, (Vector) movement.times(0.125));
                move(deltas, w, (Vector) movement.times(-0.125));
            }
        }

        // --- limit and apply displacements
        double movements = 0;
        
        for (final Iterator nodes = this.graph.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            final Vector delta = (Vector) deltas.get(v);
            final double length = length(delta);
            final double f;
            if (length > 0.1) {
                f = 0.1 / length;
            } else if (length < 0.0) {
                f = 0.0;
            } else {
                f = 1.0;
            }
            
//          final Vector d = (Vector) delta.times(f);
//          move(this.positions, v, d);
          
            final Point po = (Point) this.positions.get(v);
            final Point pt = (Point) po.plus(delta.times(f));
            final Point pn = (Point) pt.times(this.node2sym.get(v));
            this.positions.put(v, pn);
            final Vector d = (Vector) pn.minus(po); 
            
            final double l = length(d);
            movements += l * l;
        }
        
        this.lastPositionChangeAmount = Math.sqrt(movements);
    }

    public void stepCell() {
        final int dim = this.graph.getDimension();
        
        // --- scale to make unit average edge length
        normalize();

        final Matrix G = this.gramMatrix;
        Matrix dG = new Matrix(dim, dim);

        // --- evaluate the gradient of the inverse cell volume
        for (int i = 0; i < dim; ++i) {
            dG.set(i, i, G.getMinor(i, i).determinant());
            for (int j = 0; j < i; ++j) {
                final Matrix M = G.getMinor(i, i).getMinor(j, j);
                final Real x = (Real) G.get(i, j).times(-2).times(M.determinant());
                dG.set(i, j, x);
                dG.set(j, i, x);
            }
        }
        final Real vol = (Real) G.determinant();
        final Real f = new FloatingPoint(0.01 / this.graph.numberOfNodes());
        dG = (Matrix) dG.times(vol.raisedTo(-2)).negative().times(f);

        // --- evaluate the gradients of the edge energies
        for (final Iterator edges = this.graph.edges(); edges.hasNext();) {
            final IEdge e = (IEdge) edges.next();
            final INode v = e.source();
            final INode w = e.target();
            final Point pv = (Point) this.positions.get(v);
            final Point pw = (Point) this.positions.get(w);
            final Vector s = this.graph.getShift(e);
            final Vector d = (Vector) pw.plus(s).minus(pv);
            final double length = length(d);
            if (length > 1.0) {
                Matrix dE = new Matrix(dim, dim);
                for (int i = 0; i < dim; ++i) {
                    dE.set(i, i, d.get(i).raisedTo(2));
                    for (int j = 0; j < i; ++j) {
                        final Real x = (Real) d.get(i).times(d.get(j)).times(2);
                        dE.set(i, j, x);
                        dE.set(j, i, x);
                    }
                }
                dE = (Matrix) dE.times(1.0 - 1.0 / length);
                dG = (Matrix) dG.plus(dE.times(2));
            }
        }
        // --- do the same with the angle energies
        for (final Iterator angles = this.angles.iterator(); angles.hasNext();) {
            final Angle a = (Angle) angles.next();
            final INode v = a.v;
            final INode w = a.w;
            final Point pv = (Point) this.positions.get(v);
            final Point pw = (Point) this.positions.get(w);
            final Vector s = a.s;
            final Vector d = (Vector) pw.plus(s).minus(pv);
            final double length = length(d);
            if (length < 1.0) {
                Matrix dE = new Matrix(dim, dim);
                for (int i = 0; i < dim; ++i) {
                    dE.set(i, i, d.get(i).raisedTo(2));
                    for (int j = 0; j < i; ++j) {
                        final Real x = (Real) d.get(i).times(d.get(j)).times(2);
                        dE.set(i, j, x);
                        dE.set(j, i, x);
                    }
                }
                dE = (Matrix) dE.times(1.0 - 1.0 / length);
                dG = (Matrix) dG.plus(dE.times(0.5));
            }
        }
        
       // --- determine the step size
        final IArithmetic norm = dG.norm();
        final IArithmetic scale;
        if (norm.isGreaterThan(new FloatingPoint(0.1))) {
            scale = new FloatingPoint(0.1).dividedBy(norm);
        } else {
            scale = new FloatingPoint(1);
        }
        
        // --- apply the step
        final Point before = encodeGramMatrix();
        this.gramMatrix = (Matrix) G.minus(dG.times(scale));
        decodeGramMatrix((Point) encodeGramMatrix().times(this.gramProjection));
        final Point after = encodeGramMatrix();
        
        final Vector d = (Vector) after.minus(before);
        this.lastCellChangeAmount = Math.sqrt(((Real) Vector.dot(d, d)).doubleValue());
    }

    public int steps(final int n) {
        for (int i = 1; i <= n; ++i) {
            final double posChange;
            final double cellChange;
            
            if (getOptimizePositions()) {
                step();
                posChange = getLastPositionChangeAmount();
            } else {
                posChange = 0.0;
            }
            if (getOptimizeCell()) {
                stepCell();
                cellChange = getLastCellChangeAmount();
            } else {
                cellChange = 0.0;
            }
            if (Math.abs(cellChange) < 1e-4 && Math.abs(posChange) < 1e-4) {
                return i;
            }
        }
        return n;
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

    /**
     * @return the last cell change amount.
     */
    public double getLastCellChangeAmount() {
        return lastCellChangeAmount;
    }
    
    /**
     * @return the last position change amount.
     */
    public double getLastPositionChangeAmount() {
        return lastPositionChangeAmount;
    }
    
    private Point encodeGramMatrix() {
        final int d = this.graph.getDimension();
        final IArithmetic a[] = new IArithmetic[d * (d + 1) / 2];
        int k = 0;
        for (int i = 0; i < d; ++i) {
            for (int j = i; j < d; ++j) {
                a[k++] = this.gramMatrix.get(i, j);
            }
        }
        return new Point(a);
    }
    
    private void decodeGramMatrix(final Point p) {
        final int d = this.graph.getDimension();
        this.gramMatrix = new Matrix(d, d);
        int k = 0;
        for (int i = 0; i < d; ++i) {
            for (int j = i; j < d; ++j) {
                final IArithmetic x = p.get(k++);
                this.gramMatrix.set(i, j, x);
                this.gramMatrix.set(j, i, x);
            }
        }
    }
    
    private final  static DecimalFormat formatter = new DecimalFormat("0.000000");
    
    private static String format(final double x) {
        return formatter.format(x);
    }
    
    public static void main(final String args[]) {
        NetParser parser = null;
        int count = 0;
        try {
            parser = new NetParser(new FileReader(args[0]));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        while (true) {
            final PeriodicGraph G = parser.parseNet();
            if (G == null) {
                break;
            }
            ++count;
            System.out.println("Graph " + count + ":");
            final int d = G.getDimension();
            System.out.println("  dimension:\t\t" + d);
            System.out.println("  number of nodes:\t" + G.numberOfNodes());
            System.out.println("  number of edges:\t" + G.numberOfEdges());
            System.out.flush();
            System.out.println("  connected:\t\t" + (G.isConnected() ? "yes" : "no"));
            System.out.flush();
            System.out.println("  stable:\t\t" + (G.isStable() ? "yes" : "no"));
            System.out.flush();
            System.out.println("  locally stable:\t"
                    + (G.isLocallyStable() ? "yes" : "no"));
            System.out.flush();

            if (!G.isStable()) {
                System.out.println("  --- need stable graph ---");
                System.out.println();
                System.out.flush();
            } else {
                final SpringEmbedder relaxer = new SpringEmbedder(G);
                boolean done = false;
                int stepCount = 0;
                System.out.println(" --- relaxing ... ---");
                for (int i = 0; i < 10; ++i) {
                    relaxer.normalize();
                    final double stats[] = relaxer.edgeStatistics();
                    final double min = stats[0];
                    final double max = stats[1];
                    final double avg = stats[2];
                    final Matrix gr = relaxer.getGramMatrix();
                    final double det = ((Real) gr.determinant()).doubleValue();
                    final double vol = Math.sqrt(det) / G.numberOfNodes();
                    System.out.println("  After " + stepCount + " steps:"
                                       + " edge lengths: min = " + format(min)
                                       + ", max = " + format(max) + ", avg = "
                                       + format(avg) + ";  volume/vertex = "
                                       + format(vol) + "; cell change = "
                                       + format(relaxer.getLastCellChangeAmount())
                                       + "; movements = "
                                       + format(relaxer.getLastPositionChangeAmount()));
                    if (done) {
                        break;
                    }
                    final int n = relaxer.steps(50);
                    stepCount += n;
                    if (n < 50) {
                        done = true;
                    }
                }
                System.out.println();
            }
        }
    }
    
    /**
     * @return Returns the optimizeCell.
     */
    public boolean getOptimizeCell() {
        return optimizeCell;
    }
    /**
     * @param optimizeCell The optimizeCell to set.
     */
    public void setOptimizeCell(boolean optimizeCell) {
        this.optimizeCell = optimizeCell;
    }
    /**
     * @return Returns the optimizePositions.
     */
    public boolean getOptimizePositions() {
        return optimizePositions;
    }
    /**
     * @param optimizePositions The optimizePositions to set.
     */
    public void setOptimizePositions(boolean optimizePositions) {
        this.optimizePositions = optimizePositions;
    }
}
