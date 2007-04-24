/*
   Copyright 2007 Olaf Delgado-Friedrichs

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

import java.util.Map;

import org.gavrog.joss.geometry.CoordinateChange;

import junit.framework.TestCase;

/**
 * @author Olaf Delgado
 * @version $Id: TestEmbeddedPortion.java,v 1.1 2007/04/24 19:56:59 odf Exp $
 */
public class TestEmbeddedPortion extends TestCase {
    private PeriodicGraph diamond() {
        final PeriodicGraph dia = new PeriodicGraph(3);
        final INode v1 = dia.newNode();
        final INode v2 = dia.newNode();
        dia.newEdge(v1, v2, new int[] {0,0,0});
        dia.newEdge(v1, v2, new int[] {-1,0,0});
        dia.newEdge(v1, v2, new int[] {0,-1,0});
        dia.newEdge(v1, v2, new int[] {0,0,-1});
        return dia;
    }
    
    private PeriodicGraph hexGrid() {
        final PeriodicGraph H = new PeriodicGraph(2);
        final INode v1 = H.newNode();
        final INode v2 = H.newNode();
        H.newEdge(v1, v2, new int[] {0,0});
        H.newEdge(v1, v2, new int[] {1,0});
        H.newEdge(v1, v2, new int[] {0,1});
        return H;
    }
    
    public void testEmbeddedNeighborhood() {
        testEmbeddedNeighborhood(hexGrid(), 3, 19, 21);
        testEmbeddedNeighborhood(diamond(), 3, 41, 52);
    }
    
    public void testEmbeddedNeighborhood(final PeriodicGraph G, final int d,
            final int n, final int m) {
        final INode v = (INode) G.nodes().next();
        final Map pos = G.barycentricPlacement();
        final CoordinateChange basis = new CoordinateChange(G.symmetricBasis());
        final Embedding E = EmbeddedPortion.neighborhood(v, d, pos,
                basis);
        final IGraph H = E.getGraph();
        assertEquals(n, H.numberOfNodes());
        assertEquals(m, H.numberOfEdges());
    }
}
