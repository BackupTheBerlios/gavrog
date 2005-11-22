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
import java.util.Iterator;
import java.util.Map;

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
 * @version $Id: SpringEmbedder.java,v 1.7 2005/11/22 03:35:48 odf Exp $
 */
public class SpringEmbedder {
    private final PeriodicGraph graph;
    private final Map positions;
    private Matrix gramMatrix;
    private Operator gramProjection;
    private double lastPositionChangeAmount = 0;
    private double lastCellChangeAmount = 0;

    public SpringEmbedder(final PeriodicGraph graph, final Map positions,
            final Matrix gramMatrix) {
        this.graph = graph;
        this.positions = new HashMap();
        this.positions.putAll(positions);
        this.gramMatrix = gramMatrix;
        final int d = graph.getDimension();
        final SpaceGroup G = new SpaceGroup(d, graph.symmetryOperators());
        final Matrix M = G.configurationSpaceForGramMatrix();
        this.gramProjection = Operator.orthogonalProjection(M, Matrix.one(d * (d+1) / 2));
    }

    public SpringEmbedder(final PeriodicGraph G) {
        this(G, G.barycentricPlacement(), gram(G));
    }
    
    private static Matrix gram(final PeriodicGraph G) {
        final Matrix M = (Matrix) G.symmetricBasis().inverse();
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

    private void normalizeUp() {
        final double avg = edgeStatistics()[0];
        this.gramMatrix = (Matrix) this.gramMatrix.dividedBy(avg * avg);
    }

    public void step() {
        // TODO keep symmetries intact

        // --- scale so shortest edge has unit length
        normalizeUp();

        // --- initialize displacements
        final Map deltas = new HashMap();
        final Vector zero = Vector.zero(this.graph.getDimension());
        for (final Iterator nodes = this.graph.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            deltas.put(v, zero);
        }

        // --- compute displacements
        for (final Iterator edges = this.graph.edges(); edges.hasNext();) {
            final IEdge e = (IEdge) edges.next();
            final INode v = e.source();
            final INode w = e.target();
            final Point pv = (Point) this.positions.get(v);
            final Point pw = (Point) this.positions.get(w);
            final Vector s = this.graph.getShift(e);
            final Vector d = (Vector) pw.plus(s).minus(pv);
            final double length = length(d);
            if (length > 1) {
                final Vector movement = (Vector) d.times(0.5 * (length - 1) / length);
                move(deltas, v, movement);
                move(deltas, w, (Vector) movement.negative());
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
            final Vector d = (Vector) delta.times(f);
            move(this.positions, v, d);
            final double l = length(d);
            movements += l * l;
        }
        
        this.lastPositionChangeAmount = Math.sqrt(movements);
    }

    public void stepCell() {
        final int dim = this.graph.getDimension();
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
            Matrix dE = new Matrix(dim, dim);
            for (int i = 0; i < dim; ++i) {
                dE.set(i, i, d.get(i).raisedTo(2));
                for (int j = 0; j < i; ++j) {
                    final Real x = (Real) d.get(i).times(d.get(j)).times(2);
                    dE.set(i, j, x);
                    dE.set(j, i, x);
                }
            }
            dE = (Matrix) dE.times(Vector.dot(d, d, G).minus(1)).times(4);
            dG = (Matrix) dG.plus(dE);
        }

        final Point before = encodeGramMatrix();
        this.gramMatrix = (Matrix) G.minus(dG.times(0.1));
        decodeGramMatrix((Point) encodeGramMatrix().times(this.gramProjection));
        normalize();
        final Point after = encodeGramMatrix();
        
        final Vector d = (Vector) after.minus(before);
        this.lastCellChangeAmount = Math.sqrt(((Real) Vector.dot(d, d)).doubleValue());
    }

    public int steps(final int n) {
        for (int i = 1; i <= n; ++i) {
            step();
            stepCell();
            final double cellChange = getLastCellChangeAmount();
            final double posChange = getLastPositionChangeAmount();
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
}
