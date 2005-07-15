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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.Iterators;
import org.gavrog.box.Pair;
import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.Matrix;
import org.gavrog.jane.numbers.Real;
import org.gavrog.joss.pgraphs.basic.Embedding;
import org.gavrog.joss.pgraphs.basic.IEdge;
import org.gavrog.joss.pgraphs.basic.IGraph;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.Morphism;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;

import junit.framework.TestCase;

/**
 * Tests class PeriodicGraph.
 * 
 * @author Olaf Delgado
 * @version $Id: TestPeriodicGraph.java,v 1.1 2005/07/15 21:12:51 odf Exp $
 */
public class TestPeriodicGraph extends TestCase {
    private PeriodicGraph G, dia, cds;

    private INode v1, v2;

    private IEdge e1, e2, e3, e4;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        G = new PeriodicGraph(3);
        v1 = G.newNode();
        v2 = G.newNode();
        e1 = G.newEdge(v1, v2);
        e2 = G.newEdge(v2, v2, new int[] { 1, 0, 0 });
        e3 = G.newEdge(v2, v1, new int[] { 0, -1, 0 });
        e4 = G.newEdge(v1, v1, new int[] { 0, 0, 1 });
        dia = diamond();
        cds = CdSO4();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        v1 = v2 = null;
        e1 = e2 = e3 = e4 = null;
        G = dia = cds = null;
    }

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
    
    private PeriodicGraph doubleHexGrid() {
        final PeriodicGraph H = new PeriodicGraph(2);
        final INode v1 = H.newNode();
        final INode v2 = H.newNode();
        final INode v3 = H.newNode();
        final INode v4 = H.newNode();
        H.newEdge(v1, v2, new int[] {0,0});
        H.newEdge(v1, v2, new int[] {1,0});
        H.newEdge(v1, v2, new int[] {0,1});
        H.newEdge(v3, v4, new int[] {0,0});
        H.newEdge(v3, v4, new int[] {1,0});
        H.newEdge(v3, v4, new int[] {0,1});
        H.newEdge(v1, v3, new int[] {-1,-1});
        return H;
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
    
    private PeriodicGraph CdSO4() {
        final PeriodicGraph cds = new PeriodicGraph(3);
        final INode w1 = cds.newNode();
        final INode w2 = cds.newNode();
        final INode w3 = cds.newNode();
        final INode w4 = cds.newNode();
        cds.newEdge(w1, w3, new int[] {-1, 0, 0});
        cds.newEdge(w1, w3, new int[] { 0, 0, 0});
        cds.newEdge(w1, w4, new int[] { 0, 0, 0});
        cds.newEdge(w1, w4, new int[] { 0, 1, 1});
        cds.newEdge(w2, w3, new int[] { 0,-1, 0});
        cds.newEdge(w2, w3, new int[] { 0, 0,-1});
        cds.newEdge(w2, w4, new int[] { 0, 0, 0});
        cds.newEdge(w2, w4, new int[] { 1, 0, 0});
        return cds;
    }
    
    public void testNewEdge() {
        final IEdge e1 = G.newEdge(v1, v2, new int[] { 1, 1, 1 });
        assertEquals(new Matrix(new int[][] { { 1, 1, 1 } }), G.getShift(e1));
        final String s1 = "(1,1,[0,0,-1])(1,2,[0,0,0])(1,2,[0,1,0])(1,2,[1,1,1])"
                          + "(2,2,[-1,0,0])";
        assertEquals(s1, G.toString());
        G.delete(e1);
        final INode v3 = G.newNode();
        final IEdge e2 = G.newEdge(v1, v3);
        assertEquals(new Matrix(new int[1][3]), G.getShift(e2));
        final String s2 = "(1,1,[0,0,-1])(1,2,[0,0,0])(1,2,[0,1,0])(1,3,[0,0,0])"
                          + "(2,2,[-1,0,0])";
        assertEquals(s2, G.toString());
        try { // duplicate edge should be vetoed
            G.newEdge(v1, v2);
            fail("should throw an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
        try { // loop with trivial shift should be vetoed
            G.newEdge(v3, v3);
            fail("should throw an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
        try { // bad shift dimension should be vetoed
            G.newEdge(v3, v3, new int[] { 1, 2, 3, 4 });
            fail("should throw an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
        G.newEdge(v3, v3, new int[] { 1, 2, 3 });
    }

    public void testDelete() {
        G.delete(e3);
        try {
            G.getShift(e3);
            fail("should throw an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
        assertEquals("(1,1,[0,0,-1])(1,2,[0,0,0])(2,2,[-1,0,0])", G.toString());
    }

    public void testGetShift() {
        assertEquals(new Matrix(new int[][] { { 0, 0, 0 } }), G.getShift(e1));
        assertEquals(new Matrix(new int[][] { { 1, 0, 0 } }), G.getShift(e2));
        assertEquals(new Matrix(new int[][] { { 0, -1, 0 } }), G.getShift(e3));
        assertEquals(new Matrix(new int[][] { { 0, 0, 1 } }), G.getShift(e4));
        assertEquals(new Matrix(new int[][] { { 0, 0, 0 } }), G.getShift(e1.reverse()));
        assertEquals(new Matrix(new int[][] { { -1, 0, 0 } }), G.getShift(e2.reverse()));
        assertEquals(new Matrix(new int[][] { { 0, 1, 0 } }), G.getShift(e3.reverse()));
        assertEquals(new Matrix(new int[][] { { 0, 0, -1 } }), G.getShift(e4.reverse()));
    }

    public void testGetEdge() {
        final IEdge test1 = G.getEdge(v1, v2, new Matrix(new int[][] { { 0, 1, 0 } }));
        assertEquals(e3.reverse(), test1);
        assertEquals(G.getShift(e3.reverse()), G.getShift(test1));
        assertEquals(e3, G.getEdge(v2, v1, new Matrix(new int[][] { { 0, -1, 0 } })));
        assertNull(G.getEdge(v2, v1, new Matrix(new int[][] { { 1, -1, 0 } })));
        assertEquals(e2, G.getEdge(v2, v2, new Matrix(new int[][] { { 1, 0, 0 } })));

        final IEdge test2 = G.getEdge(v2, v2, new Matrix(new int[][] { { -1, 0, 0 } }));
        assertEquals(e2.reverse(), test2);
        assertEquals(G.getShift(e2.reverse()), G.getShift(test2));
    }

    public void testHashCodes() {
        final INode v = v1;
        final INode w = (INode) G.getElement(v.id());
        assertNotSame(v, w);
        assertEquals(v, w);
        assertEquals(v.hashCode(), w.hashCode());
        final IEdge e = e1;
        final IEdge f = (IEdge) G.getElement(e.id());
        assertNotSame(e, f);
        assertEquals(e, f);
        assertEquals(e.hashCode(), f.hashCode());
    }
    
    public void testToString() {
        final String s = "(1,1,[0,0,-1])(1,2,[0,0,0])(1,2,[0,1,0])(2,2,[-1,0,0])";
        assertEquals(s, G.toString());
    }
    
    public void testCoordinationSequence() {
        final INode start = (INode) dia.nodes().next();
        final Iterator cs = dia.coordinationSequence(start);
        assertEquals(new Integer(1), cs.next());
        assertEquals(new Integer(4), cs.next());
        assertEquals(new Integer(12), cs.next());
        assertEquals(new Integer(24), cs.next());
        assertEquals(new Integer(42), cs.next());
        assertEquals(new Integer(64), cs.next());
        assertEquals(new Integer(92), cs.next());
        assertEquals(new Integer(124), cs.next());
        assertEquals(new Integer(162), cs.next());
        assertEquals(new Integer(204), cs.next());
        assertEquals(new Integer(252), cs.next());
    }
    
    public void testIsConnected() {
        final PeriodicGraph H = new PeriodicGraph(3);
        final INode v1 = H.newNode();
        final INode v2 = H.newNode();
        assertFalse(H.isConnected());
        H.newEdge(v1, v2, new int[] {1,0,0});
        H.newEdge(v1, v2, new int[] {0,1,0});
        H.newEdge(v1, v2, new int[] {0,0,1});
        assertFalse(H.isConnected());
        H.newEdge(v1, v2, new int[] {3,0,0});
        assertFalse(H.isConnected());
        final IEdge e = H.newEdge(v1, v2, new int[] {2,0,0});
        assertTrue(H.isConnected());
        H.delete(e);
        H.newEdge(v1, v2, new int[] {0,0,0});
        assertTrue(H.isConnected());
    }
    
    private boolean isBarycentric(final PeriodicGraph G, final Map pos) {
        for (final Iterator nodes = G.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            final Matrix p = (Matrix) pos.get(v);
            Matrix t = new Matrix(new int[1][G.getDimension()]);
            for (final Iterator iter = v.incidences(); iter.hasNext();) {
                final IEdge e = (IEdge) iter.next();
                final INode w = e.target();
                if (w.equals(v)) {
                    continue; // loops cancel out with their reverses
                }
                final Matrix s = G.getShift(e);
                final Matrix q = (Matrix) pos.get(w);
                t = (Matrix) t.plus(q).plus(s).minus(p);
            }
            if (!t.isZero()) {
                System.out.println(v + ": " + t + "!");
                return false;
            }
        }
        return true;
    }
    
    public void testBarycentricPositions() {
        assertTrue(isBarycentric(G, G.barycentricPlacement()));
        assertTrue(isBarycentric(dia, dia.barycentricPlacement()));
        assertTrue(isBarycentric(cds, cds.barycentricPlacement()));
    }
    
    public void testIsStableAndIsLocallyStable() {
        assertTrue(G.isStable());
        assertTrue(dia.isStable());
        assertTrue(cds.isStable());
        assertTrue(G.isLocallyStable());
        assertTrue(dia.isLocallyStable());
        assertTrue(cds.isLocallyStable());
        
        final PeriodicGraph H = doubleHexGrid();
        assertFalse(H.isStable());
        assertTrue(H.isLocallyStable());
        
        final PeriodicGraph H2 = new PeriodicGraph(2);
        final INode w1 = H2.newNode();
        final INode w2 = H2.newNode();
        final INode w3 = H2.newNode();
        H2.newEdge(w1, w1, new int[] {1,0});
        H2.newEdge(w1, w2, new int[] {0,0});
        H2.newEdge(w1, w2, new int[] {0,1});
        H2.newEdge(w1, w3, new int[] {0,0});
        H2.newEdge(w1, w3, new int[] {0,1});
        H2.newEdge(w2, w3, new int[] {0,0});
        assertFalse(H2.isStable());
        assertFalse(H2.isLocallyStable());
    }
    
    public void testTranslationalEquivalenceClasses() {
        assertFalse(dia.translationalEquivalenceClasses().hasNext());
        assertFalse(G.translationalEquivalenceClasses().hasNext());
        
        List classes;
        
        classes = Iterators.asList(cds.translationalEquivalenceClasses());
        assertEquals(2, classes.size());
        
        classes = Iterators.asList(doubleHexGrid().translationalEquivalenceClasses());
        assertEquals(2, classes.size());
        
        final PeriodicGraph H = new PeriodicGraph(3);
        final INode w1 = H.newNode();
        final INode w2 = H.newNode();
        H.newEdge(w1, w1, new int[] {1,0,0});
        H.newEdge(w1, w1, new int[] {0,1,0});
        H.newEdge(w2, w2, new int[] {1,0,0});
        H.newEdge(w2, w2, new int[] {0,1,0});
        H.newEdge(w1, w2, new int[] {0,0,0});
        H.newEdge(w1, w2, new int[] {0,0,1});
        new Morphism(w1, w2, Matrix.one(3));
        classes = Iterators.asList(H.translationalEquivalenceClasses());
        assertEquals(1, classes.size());
    }
    
    public void testMinimalImage() {
        final PeriodicGraph cds1 = cds.minimalImage();
        assertEquals(3, cds1.getDimension());
        assertEquals(2, cds1.numberOfNodes());
        assertEquals(4, cds1.numberOfEdges());
        assertTrue(cds1.isConnected());
        assertTrue(cds1.isStable());
        final Iterator nodes = cds1.nodes();
        final INode w1 = (INode) nodes.next();
        final INode w2 = (INode) nodes.next();
        final List loops1 = Iterators.asList(cds1.directedEdges(w1, w1));
        final List loops2 = Iterators.asList(cds1.directedEdges(w2, w2));
        assertEquals(1, loops1.size());
        assertEquals(1, loops2.size());
        
        try {
            doubleHexGrid().minimalImage();
            fail("should throw an UnsupportedOperationException");
        } catch (UnsupportedOperationException success) {
        }
        
        assertSame(dia, dia.minimalImage());
    }
    
    public void testCharacteristicBases() {
        testCharacteristicBases(dia, 48);
        testCharacteristicBases(G, 16);
        testCharacteristicBases(cds.minimalImage(), 16);
        testCharacteristicBases(doubleHexGrid(), 24);
    }
    
    public void testCharacteristicBases(final PeriodicGraph G, final int expectedNr) {
        final List bases = G.characteristicBases();
        assertEquals(expectedNr, bases.size());

        final int d = G.getDimension();
        final Map pos = G.barycentricPlacement();
        
        final Set seen = new HashSet();
        for (final Iterator iter = bases.iterator(); iter.hasNext();) {
            final List basis = (List) iter.next();
            final List key = new ArrayList();
            final Matrix M = new Matrix(d, d);
            for (int i = 0; i < basis.size(); ++i) {
                final IEdge e = (IEdge) basis.get(i);
                final INode v = e.source();
                final INode w = e.target();
                final Matrix pv = (Matrix) pos.get(v);
                final Matrix pw = (Matrix) pos.get(w);
                final Matrix s = G.getShift(e);
                M.setRow(i, (Matrix) pw.minus(pv).plus(s));
                key.add(new Pair(e, s));
            }
            assertEquals(d, M.rank());
            assertFalse(seen.contains(key));
            seen.add(key);
            assertTrue(seen.contains(key));
        }
    }
    
    public void testSymmetries() {
        testSymmetries(dia, 48);
        testSymmetries(G, 16);
        testSymmetries(cds.minimalImage(), 16);
        testSymmetries(doubleHexGrid(), 12);
    }
    
    public void testSymmetries(final PeriodicGraph G, final int expectedNr) {
        final Set symmetries = G.symmetries();
        assertEquals(expectedNr, symmetries.size());
    }
    
    public void testSymmetricBasis() {
        testSymmetricBasis(G);
        testSymmetricBasis(dia);
        testSymmetricBasis(doubleHexGrid());
    }
    
    public void testSymmetricBasis(final PeriodicGraph G) {
        final Real eps = new FloatingPoint(1e-12);
        final Matrix I = Matrix.one(G.getDimension());
        final Matrix B = G.symmetricBasis();
        final Matrix B_1 = (Matrix) B.inverse();
        for (final Iterator syms = G.symmetries().iterator(); syms.hasNext();) {
            final Matrix M = ((Morphism) syms.next()).getMatrix();
            final Matrix A = (Matrix) B_1.times(M).times(B);
            final Matrix D = (Matrix) A.times(A.transposed());
            assertTrue(D.minus(I).norm().isLessThan(eps));
        }
    }
    
    public void testEmbeddedNeighborhood() {
        testEmbeddedNeighborhood(hexGrid(), 3, 19, 21);
        testEmbeddedNeighborhood(dia, 3, 41, 52);
    }
    
    public void testEmbeddedNeighborhood(final PeriodicGraph G, final int d, final int n,
            final int m) {
        final INode v = (INode) G.nodes().next();
        final Map pos = G.barycentricPlacement();
        final Matrix basis = G.symmetricBasis();
        final Embedding E = G.embeddedNeighborhood(v, d, pos, basis);
        final IGraph H = E.getGraph();
        assertEquals(n, H.numberOfNodes());
        assertEquals(m, H.numberOfEdges());
    }
    
    public void testCanonical() {
        assertEquals("(1,2,[0,0,0])(1,2,[1,0,0])(1,2,[0,1,0])(1,2,[0,0,1])",
                dia.canonical().toString());
        assertEquals(G.canonical().toString(), cds.minimalImage().canonical().toString());
        assertFalse(G.canonical().toString().equals(dia.canonical().toString()));
    }
    
    public void testInvariant() {
        assertEquals(G.invariant(), cds.minimalImage().invariant());
        assertFalse(G.invariant().equals(dia.invariant()));
    }
    
    public void testEquals() {
        assertEquals(G, G);
        assertEquals(dia, dia);
        assertEquals(G, cds.minimalImage());
        assertFalse(dia.equals(G));
    }
    
    public void testHashCode() {
        assertEquals(G.hashCode(), G.hashCode());
        assertEquals(dia.hashCode(), dia.hashCode());
        assertEquals(G.hashCode(), cds.minimalImage().hashCode());
        assertFalse(dia.hashCode() == G.hashCode());
    }
}
