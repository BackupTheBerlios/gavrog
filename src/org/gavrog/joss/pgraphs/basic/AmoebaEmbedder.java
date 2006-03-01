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

import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.Real;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;

/**
 * @author Olaf Delgado
 * @version $Id: AmoebaEmbedder.java,v 1.5 2006/03/01 05:21:34 odf Exp $
 */
public class AmoebaEmbedder extends EmbedderAdapter {
    private class Edge {
        public final int v;
        public final int w;
        public final double shift[];
        public final double weight;
        
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
    
    final double p[];
    
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
    
    public Point getPosition(final INode v) {
        final int d = this.dimGraph;
        final int offset = ((Integer) this.node2index.get(v)).intValue();
        
        final double coords[] = new double[d];
        for (int i = 0; i < d; ++i) {
            coords[i] = this.p[offset + i];
        }
        return new Point(coords);
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

    public void setGramMatrix(final Matrix gramMatrix) {
        final int d = this.dimGraph;
        for (int i = 0; i < d; ++i) {
            for (int j = i; j < d; ++j) {
                final int k = this.gramIndex[i][j];
                this.p[k] = ((Real) gramMatrix.get(i, j)).doubleValue();
            }
        }
    }

    public Matrix getGramMatrix() {
        final int d = this.dimGraph;
        final Matrix gram = new Matrix(d, d);
        for (int i = 0; i < d; ++i) {
            for (int j = i; j < d; ++j) {
                final int k = this.gramIndex[i][j];
                final Real x = new FloatingPoint(this.p[k]);
                gram.set(i, j, x);
                gram.set(j, i, x);
            }
        }
        return gram;
    }

    /* (non-Javadoc)
     * @see org.gavrog.joss.pgraphs.basic.IEmbedder#go(int)
     */
    public int go(int n) {
        // TODO Auto-generated method stub
        return 0;
    }
}
