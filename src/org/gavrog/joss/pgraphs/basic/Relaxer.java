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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Real;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.io.NetParser;
import org.gavrog.systre.Archive;

/**
 * @author Olaf Delgado
 * @version $Id: Relaxer.java,v 1.7 2005/11/02 04:58:18 odf Exp $
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

    public void step() {
        // --- scale so shortest edge has unit length
        IArithmetic minLength = null;
        IArithmetic maxLength = null;
        for (final Iterator edges = this.graph.edges(); edges.hasNext();) {
            final IEdge e = (IEdge) edges.next();
            final Point p = (Point) this.positions.get(e.source());
            final Point q = (Point) this.positions.get(e.target());
            final Vector s = this.graph.getShift(e);
            final Vector d = (Vector) q.plus(s).minus(p);
            final IArithmetic length = Vector.dot(d, d, this.gramMatrix);
            if (minLength == null
                || (length.isPositive() && length.isLessThan(minLength))) {
                minLength = length;
            }
            if (maxLength == null || length.isGreaterThan(maxLength)) {
                maxLength = length;
            }
        }
        this.gramMatrix = (Matrix) this.gramMatrix.dividedBy(minLength);
        System.out.print(maxLength + "  " + minLength);
        
        // --- compute displacements
        final Vector zero = Vector.zero(this.graph.getDimension());
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
            if (length > 1) {
                final Vector movement = (Vector) d.times(0.25 * (length - 1) / length);
                deltas.put(v, ((Vector) deltas.get(v)).plus(movement));
                deltas.put(w, ((Vector) deltas.get(w)).minus(movement));
                if (movement.isNegative()) {
                    globalPull = (Vector) globalPull.minus(movement);
                } else {
                    globalPull = (Vector) globalPull.plus(movement);
                }
            }
        }
        System.out.println("  " + globalPull);
        
        // --- apply displacements
        for (final Iterator nodes = this.graph.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            this.positions.put(v, ((Point) this.positions.get(v)).plus(deltas.get(v)));
        }
        
        // --- apply global pull
        //TODO implement this
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
            System.out.println("  locally stable:\t" + (G.isLocallyStable() ? "yes" : "no"));
            System.out.flush();
            
            if (!G.isStable()) {
                System.out.println("  --- need stable graph ---");
                System.out.println();
                System.out.flush();
            } else {
                System.out.println(" --- relaxing ... ---");
                final Matrix M = (Matrix) G.symmetricBasis().inverse();
                final Matrix gram = (Matrix) M.times(M.transposed());
                final Relaxer relaxer = new Relaxer(G, G.barycentricPlacement(), gram);
                for (int i = 0; i < 100; ++i) {
                    relaxer.step();
                }
                System.out.println();
            }
        }
    }
}

