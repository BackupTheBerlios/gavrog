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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Real;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.io.NetParser;

/**
 * @author Olaf Delgado
 * @version $Id: SpringEmbedder.java,v 1.1 2005/11/04 22:32:51 odf Exp $
 */
public class SpringEmbedder {
    private final PeriodicGraph graph;

    private final Map positions;

    private Matrix gramMatrix;

    public SpringEmbedder(final PeriodicGraph graph, final Map positions, final Matrix gramMatrix) {
        this.graph = graph;
        this.positions = new HashMap();
        this.positions.putAll(positions);
        this.gramMatrix = gramMatrix;
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
            final Vector d = (Vector) q.plus(s).minus(p);
            final double length = ((Real) Vector.dot(d, d, this.gramMatrix)).doubleValue();
            if (length > 0) {
                minLength = Math.min(minLength, length);
            }
            maxLength = Math.max(maxLength, length);
            sumLength += length;
        }
        final double avgLength = sumLength / this.graph.numberOfEdges();
        return new double[] { Math.sqrt(minLength), Math.sqrt(maxLength),
                Math.sqrt(avgLength) };
    }
    
    private double length(final Vector v) {
        final Real squareLength = (Real) Vector.dot(v, v, this.gramMatrix);
        return Math.sqrt(squareLength.doubleValue());
    }
    
    private void move(final Map pos, final INode v, final Vector amount) {
        pos.put(v, ((IArithmetic) pos.get(v)).plus(amount));
    }
    
    public void step() {
        //TODO keep symmetries intact
        // --- scale so shortest edge has unit length
        final double x = edgeStatistics()[0];
        this.gramMatrix = (Matrix) this.gramMatrix.times(1.0 / (x * x));

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
            move(this.positions, v, (Vector) delta.times(f));
        }
    }
    
    public void stepCell() {
        //TODO keep symmetries intact
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
        
        this.gramMatrix = (Matrix) G.minus(dG.times(0.01));
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
                final Matrix M = (Matrix) G.symmetricBasis().inverse();
                final Matrix gram = (Matrix) M.times(M.transposed());
                final SpringEmbedder relaxer = new SpringEmbedder(G, G.barycentricPlacement(), gram);
                System.out.println(" --- relaxing ... ---");
                for (int i = 0; i < 201; ++i) {
                    if (i % 20 == 0) {
                        final double stats[] = relaxer.edgeStatistics();
                        final double avg = stats[2];
                        final double min = stats[0] / avg;
                        final double max = stats[1] / avg;
                        final double cubedAvg = Math.pow(avg, 3);
                        final Matrix gr = relaxer.getGramMatrix();
                        final double det = ((Real) gr.determinant()).doubleValue();
                        final double vol = Math.sqrt(det) / cubedAvg / G.numberOfNodes();
                        System.out.println("  edge lengths: min = "
                                + Math.rint(min * 1000) / 1000 + ", max = "
                                + Math.rint(max * 1000) / 1000 + ", avg = "
                                + Math.rint(1.0 * 1000) / 1000 + ";  volume/vertex = "
                                + Math.rint(vol * 1000) / 1000);
                    }
                    relaxer.step();
                    relaxer.stepCell();
                }
                System.out.println();
            }
        }
    }
}