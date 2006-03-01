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

import org.gavrog.box.simple.Misc;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Real;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.io.NetParser;

/**
 * @author Olaf Delgado
 * @version $Id: SpringEmbedder.java,v 1.36 2006/03/01 05:21:34 odf Exp $
 */
public class SpringEmbedder extends EmbedderAdapter {
    private static final boolean DEBUG = false;
    
    private double lastPositionChangeAmount = 0;
    private double lastCellChangeAmount = 0;
    
    private final Map positions = new HashMap();
    private Matrix gramMatrix;

    public SpringEmbedder(final PeriodicGraph graph, final Map positions,
            final Matrix gram) {
        super(graph);
        
        // --- set initial positions and cell parameters
        setPositions(positions);
        setGramMatrix(gram);
    }
    
    public SpringEmbedder(final PeriodicGraph G) {
        this(G, G.barycentricPlacement(), defaultGramMatrix(G));
    }
    
    public void setPositions(final Map map) {
        this.positions.putAll(map);
    }

    public Map getPositions() {
        final Map copy = new HashMap();
        copy.putAll(this.positions);
        return copy;
    }
    
    public Point getPosition(final INode v) {
        return (Point) this.positions.get(v);
    }

    public void setPosition(final INode v, final Point p) {
        this.positions.put(v, p);
    }

    public void setGramMatrix(final Matrix gramMatrix) {
        this.gramMatrix = (Matrix) gramMatrix.clone();
        this.gramMatrix.makeImmutable();
    }

    public Matrix getGramMatrix() {
        return (Matrix) this.gramMatrix.clone();
    }

    private String gramAsString(final Matrix G) {
        final Real G00 = (Real) G.get(0,0);
        final Real G01 = (Real) G.get(0,1);
        final Real G02 = (Real) G.get(0,2);
        final Real G11 = (Real) G.get(1,1);
        final Real G12 = (Real) G.get(1,2);
        final Real G22 = (Real) G.get(2,2);
        final Real a = G00.sqrt();
        final Real b = G11.sqrt();
        final Real c = G22.sqrt();
        final Real f = new FloatingPoint(180.0 / Math.PI);
        final Real alpha = (Real) ((Real) G12.dividedBy(b.times(c))).acos().times(f);
        final Real beta = (Real) ((Real) G02.dividedBy(a.times(c))).acos().times(f);
        final Real gamma = (Real) ((Real) G01.dividedBy(a.times(b))).acos().times(f);
        return
        "a=" + format(a.doubleValue()) +
        ", b=" + format(b.doubleValue()) +
        ", c=" + format(c.doubleValue()) +
        ", alpha=" + format(alpha.doubleValue()) +
        ", beta=" + format(beta.doubleValue()) +
        ", gamma=" + format(gamma.doubleValue()) +
        ", G00=" + format(G00.doubleValue()) +
        ", G11=" + format(G11.doubleValue()) +
        ", G22=" + format(G22.doubleValue()) +
        ", G12=" + format(G12.doubleValue()) +
        ", G02=" + format(G02.doubleValue()) +
        ", G01=" + format(G01.doubleValue());
    }
    
    private void move(final Map pos, final INode v, final Vector amount) {
        pos.put(v, ((IArithmetic) pos.get(v)).plus(amount));
    }

    private void normalizeUp() {
        final double min = edgeStatistics()[0];
        if (min < 1e-3) {
            throw new RuntimeException("edge got too small while relaxing");
        }
        setGramMatrix((Matrix) getGramMatrix().dividedBy(min * min));
    }

    private void step() {
        // --- scale to make unit average edge length
        normalizeUp();

        // --- initialize displacements
        final Map deltas = new HashMap();
        final Vector zero = Vector.zero(getGraph().getDimension());
        for (final Iterator nodes = getGraph().nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            deltas.put(v, zero);
        }

        // --- compute displacements
        //    ... using edge forces
        for (final Iterator edges = getGraph().edges(); edges.hasNext();) {
            final IEdge e = (IEdge) edges.next();
            final INode v = e.source();
            final INode w = e.target();
            final Point pv = getPosition(v);
            final Point pw = getPosition(w);
            final Vector s = getGraph().getShift(e);
            final Vector d = (Vector) pw.plus(s).minus(pv);
            final double length = length(d);
            if (length > 1.0) {
                final Vector movement = (Vector) d.times(1.0 - 1.0 / length);
                move(deltas, v, (Vector) movement.times(0.5));
                move(deltas, w, (Vector) movement.times(-0.5));
            }
        }
        //    ... using angle forces
        for (final Iterator angles = angles(); angles.hasNext();) {
            final Angle a = (Angle) angles.next();
            final INode v = a.v;
            final INode w = a.w;
            final Point pv = getPosition(v);
            final Point pw = getPosition(w);
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
        
        for (final Iterator nodes = getGraph().nodes(); nodes.hasNext();) {
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
            
            final Point po = getPosition(v);
            final Point pt = (Point) po.plus(delta.times(f));
            setPosition(v, pt);
            final Vector d = (Vector) pt.minus(po); 
            
            final double l = length(d);
            movements += l * l;
        }
        
        this.lastPositionChangeAmount = Math.sqrt(movements);
        resymmetrizePositions();
    }

    
    private void stepCell() {
        final int dim = getGraph().getDimension();
        if (DEBUG) {
            System.err.println("@@@ Entering stepCell");
            System.err.println("@@@   Gram matrix before: " + gramAsString(getGramMatrix()));
        }
        
        // --- scale to make unit average edge length
        normalizeUp();
        if (DEBUG) {
            System.err.println("@@@   Gram matrix normalized: " + gramAsString(getGramMatrix()));
        }

        final Matrix G = getGramMatrix();
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
        final Real f = new FloatingPoint(1.0 / getGraph().numberOfNodes());
        dG = (Matrix) dG.times(vol.raisedTo(-2)).negative().times(f);
        //    ... make it the gradient of the cubed volume
        dG = (Matrix) dG.times(G.determinant()).times(3).times(f).times(f);
        //    ... boost the weight
        dG = (Matrix) dG.times(1000);

        // --- evaluate the gradients of the edge energies
        for (final Iterator edges = getGraph().edges(); edges.hasNext();) {
            final IEdge e = (IEdge) edges.next();
            final INode v = e.source();
            final INode w = e.target();
            final Point pv = getPosition(v);
            final Point pw = getPosition(w);
            final Vector s = getGraph().getShift(e);
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
        for (final Iterator angles = angles(); angles.hasNext();) {
            final Angle a = (Angle) angles.next();
            final INode v = a.v;
            final INode w = a.w;
            final Point pv = getPosition(v);
            final Point pw = getPosition(w);
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

        if (DEBUG) {
            System.err.println("@@@   Gradient: " + gramAsString(dG));
        }
        
       // --- determine the step size
        final IArithmetic norm = dG.norm();
        final IArithmetic scale;
        if (norm.isGreaterThan(new FloatingPoint(0.1))) {
            scale = new FloatingPoint(0.1).dividedBy(norm);
        } else {
            scale = new FloatingPoint(1);
        }
        
        if (DEBUG) {
            System.err.println("@@@   Scale: " + scale);
        }

        // --- apply the step
        final Point before = encodeGramMatrix();
        setGramMatrix((Matrix) G.minus(dG.times(scale)));
        if (DEBUG) {
            System.err.println("@@@   Gram matrix plus gradient: "
                    + gramAsString(getGramMatrix()));
        }
        try {
            resymmetrizeCell();
        } catch (Exception ex) {
            System.err.println(Misc.stackTrace(ex));
        }
        if (DEBUG) {
            System.err.println("@@@   Resymmetrized gram matrix: "
                    + gramAsString(getGramMatrix()));
        }
        final Point after = encodeGramMatrix();
        
        final Vector d = (Vector) after.minus(before);
        this.lastCellChangeAmount = Math.sqrt(((Real) Vector.dot(d, d)).doubleValue());
        
        if (DEBUG) {
            System.err.println("@@@ Leaving stepCell()");
            System.err.println();
            System.err.flush();
        }
    }

    public int go(final int n) {
        for (int i = 1; i <= n; ++i) {
            final double posChange;
            final double cellChange;
            
            if (getRelaxPositions()) {
                step();
                posChange = getLastPositionChangeAmount();
            } else {
                posChange = 0.0;
            }
            if (getRelaxCell()) {
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
    
    /**
     * @return the last cell change amount.
     */
    private double getLastCellChangeAmount() {
        return lastCellChangeAmount;
    }
    
    /**
     * @return the last position change amount.
     */
    private double getLastPositionChangeAmount() {
        return lastPositionChangeAmount;
    }
    
    private Point encodeGramMatrix() {
        final int d = getGraph().getDimension();
        final IArithmetic a[] = new IArithmetic[d * (d + 1) / 2];
        int k = 0;
        for (int i = 0; i < d; ++i) {
            for (int j = i; j < d; ++j) {
                a[k++] = getGramMatrix().get(i, j);
            }
        }
        return new Point(a);
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
                    final int n = relaxer.go(50);
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
