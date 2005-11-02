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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.gavrog.jane.compounds.LinearAlgebra;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.Real;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.io.NetParser;
import org.gavrog.systre.Archive;

/**
 * @author Olaf Delgado
 * @version $Id: Relaxer.java,v 1.9 2005/11/02 22:28:00 odf Exp $
 */
public class Relaxer {
    private final PeriodicGraph graph;

    private final Map positions;

    private Matrix gramMatrix;

    public Relaxer(final PeriodicGraph graph, final Map positions, final Matrix gramMatrix) {
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
        return new double[] { minLength, maxLength, avgLength };
    }
    
    private double clamp(final double value, final double lo, final double hi) {
        return Math.min(Math.max(value, lo), hi);
    }
    
    public void step() {
        // --- scale so shortest edge has unit length
        this.gramMatrix = (Matrix) this.gramMatrix.times(1.0 / edgeStatistics()[2]);

        // --- compute displacements
        final int dim = this.graph.getDimension();
        final Vector zero = Vector.zero(dim);
        Vector globalPull = zero;
        final Map deltas = new HashMap();
        for (final Iterator nodes = this.graph.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            deltas.put(v, zero);
        }

        for (final Iterator edges = this.graph.edges(); edges.hasNext();) {
            final IEdge e = (IEdge) edges.next();
            final INode v = e.source();
            final INode w = e.target();
            final Point pv = (Point) this.positions.get(v);
            final Point pw = (Point) this.positions.get(w);
            final Vector s = this.graph.getShift(e);
            final Vector d = (Vector) pw.plus(s).minus(pv);
            final Real squareLength = (Real) Vector.dot(d, d, this.gramMatrix);
            final double length = Math.sqrt(squareLength.doubleValue());
            final double f = clamp(0.25 * (length - 1) / length, -0.1, 0.1);
            final Vector movement = (Vector) d.times(f);
            deltas.put(v, ((Vector) deltas.get(v)).plus(movement));
            deltas.put(w, ((Vector) deltas.get(w)).minus(movement));
            if ((movement.times(length-1)).isNegative()) {
                globalPull = (Vector) globalPull.minus(movement);
            } else {
                globalPull = (Vector) globalPull.plus(movement);
            }
        }

        // --- apply displacements
        for (final Iterator nodes = this.graph.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            this.positions.put(v, ((Point) this.positions.get(v)).plus(deltas.get(v)));
        }

        // --- apply global pull
        final Real len = (Real) Vector.dot(globalPull, globalPull, this.gramMatrix);
        if (Math.sqrt(len.doubleValue()) > 0.0001) {
            final Matrix A = Matrix.zero(dim, dim).mutableClone();
            A.setRow(0, globalPull.getCoordinates());
            int r = 1;
            final Matrix I = Matrix.one(dim);
            for (int i = 0; i < dim; ++i) {
                A.setRow(r, I.getRow(i));
                if (A.rank() > r) {
                    ++r;
                    if (r == dim) {
                        break;
                    }
                }
            }

            final Matrix B = LinearAlgebra.rowOrthonormalized(A, this.gramMatrix);
            final Vector v = new Vector(B.getRow(0));
            double lo = 0;
            double hi = 0;
            for (int i = 0; i <= 1; ++i) {
                for (int j = 0; j <= 1; ++j) {
                    for (int k = 0; k <= 1; ++k) {
                        final Vector t = new Vector(i, j, k);
                        final double x = ((Real) Vector.dot(t, v)).doubleValue();
                        lo = Math.min(lo, x);
                        hi = Math.max(hi, x);
                    }
                }
            }
            final double width = hi - lo;
            double delta = Math.abs(((Real) Vector.dot(globalPull, v)).doubleValue());
            if (globalPull.isNegative()) {
                delta = -delta;
            }
            final double f = clamp((width - delta) / width, 0.9, 1.1);
            final Matrix C = Matrix.one(dim).mutableClone();
            C.set(0, 0, new FloatingPoint(f));
            final Matrix Binv = (Matrix) B.inverse();
            final Matrix G = (Matrix) Binv.times(C).times(Binv.transposed());
            this.gramMatrix = (Matrix) ((Matrix) G.plus(G.transposed())).dividedBy(2);
        }
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
        final Package pkg = Archive.class.getPackage();
        final String packagePath = pkg.getName().replaceAll("\\.", "/");
        final String archivePath = packagePath + "/rcsr.arc";
        final Archive rcsr = new Archive("1.0");
        final InputStream inStream = ClassLoader.getSystemResourceAsStream(archivePath);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
        rcsr.addAll(reader);

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
                final Relaxer relaxer = new Relaxer(G, G.barycentricPlacement(), gram);
                System.out.println(" --- relaxing ... ---");
                for (int i = 0; i < 201; ++i) {
                    if (i % 20 == 0) {
                        final Matrix gr = relaxer.getGramMatrix();
                        final double det = ((Real) gr.determinant()).doubleValue();
                        final double vol = Math.sqrt(det) / G.numberOfNodes();
                        final double stats[] = relaxer.edgeStatistics();
                        System.out.println("  edge lengths: min = "
                                + Math.rint(stats[0] * 1000) / 1000 + ", max = "
                                + Math.rint(stats[1] * 1000) / 1000 + ", avg = "
                                + Math.rint(stats[2] * 1000) / 1000
                                + ";  volume/vertex = " + Math.rint(vol * 1000) / 1000);
                    }
                    relaxer.step();
                }
                System.out.println();
            }
        }
    }
}
