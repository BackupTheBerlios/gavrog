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

import junit.framework.TestCase;

import org.gavrog.box.collections.Iterators;
import org.gavrog.box.collections.Pair;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.Real;
import org.gavrog.joss.geometry.CoordinateChange;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;

/**
 * Tests class PeriodicGraph.
 * 
 * @author Olaf Delgado
 * @version $Id: TestPeriodicGraph.java,v 1.12 2005/10/22 01:45:26 odf Exp $
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
        assertEquals(new Vector(1, 1, 1), G.getShift(e1));
        final String s1 = "(1,1,[0,0,-1])(1,2,[0,0,0])(1,2,[0,1,0])(1,2,[1,1,1])"
                          + "(2,2,[-1,0,0])";
        assertEquals(s1, G.toString());
        G.delete(e1);
        final INode v3 = G.newNode();
        final IEdge e2 = G.newEdge(v1, v3);
        assertEquals(new Vector(0, 0, 0), G.getShift(e2));
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

    public void testShiftNode() {
        //TODO implement this test.
    }
    
    public void testGetShift() {
        assertEquals(new Vector(0, 0, 0), G.getShift(e1));
        assertEquals(new Vector(1, 0, 0), G.getShift(e2));
        assertEquals(new Vector(0, -1, 0), G.getShift(e3));
        assertEquals(new Vector(0, 0, 1), G.getShift(e4));
        assertEquals(new Vector(0, 0, 0), G.getShift(e1.reverse()));
        assertEquals(new Vector(-1, 0, 0), G.getShift(e2.reverse()));
        assertEquals(new Vector(0, 1, 0), G.getShift(e3.reverse()));
        assertEquals(new Vector(0, 0, -1), G.getShift(e4.reverse()));
    }

    public void testGetEdge() {
        final IEdge test1 = G.getEdge(v1, v2, new Vector(0, 1, 0));
        assertEquals(e3.reverse(), test1);
        assertEquals(G.getShift(e3.reverse()), G.getShift(test1));
        assertEquals(e3, G.getEdge(v2, v1, new Vector(0, -1, 0)));
        assertNull(G.getEdge(v2, v1, new Vector(1, -1, 0)));
        assertEquals(e2, G.getEdge(v2, v2, new Vector(1, 0, 0)));

        final IEdge test2 = G.getEdge(v2, v2, new Vector(-1, 0, 0));
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
            final Point p = (Point) pos.get(v);
            Vector t = Vector.zero(G.getDimension());
            for (final Iterator iter = v.incidences(); iter.hasNext();) {
                final IEdge e = (IEdge) iter.next();
                final INode w = e.target();
                if (w.equals(v)) {
                    continue; // loops cancel out with their reverses
                }
                final Vector s = G.getShift(e);
                final Point q = (Point) pos.get(w);
                t = (Vector) t.plus(q).plus(s).minus(p);
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
                final Point pv = (Point) pos.get(v);
                final Point pw = (Point) pos.get(w);
                final Vector s = G.getShift(e);
                M.setRow(i, ((Vector) pw.minus(pv).plus(s)).getCoordinates());
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
        final int d = G.getDimension();
        final Matrix I = Matrix.one(d);
        final Matrix B = G.symmetricBasis();
        final Matrix B_1 = (Matrix) B.inverse();
        for (final Iterator syms = G.symmetries().iterator(); syms.hasNext();) {
            final Matrix M = ((Morphism) syms.next()).getOperator().getCoordinates()
                    .getSubMatrix(0, 0, d, d);
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
        final CoordinateChange basis = new CoordinateChange(G.symmetricBasis(), Point
                .origin(G.getDimension()));
        final Embedding E = G.embeddedNeighborhood(v, d, pos, basis);
        final IGraph H = E.getGraph();
        assertEquals(n, H.numberOfNodes());
        assertEquals(m, H.numberOfEdges());
    }
    
    public void testCanonical() {
        assertEquals("(1,2,[0,0,0])(1,2,[0,0,1])(1,2,[0,1,0])(1,2,[1,0,0])",
                dia.canonical().toString());
        assertEquals(G.canonical().toString(), cds.minimalImage().canonical().toString());
        assertFalse(G.canonical().toString().equals(dia.canonical().toString()));
        assertEquals(makeTestGraph(2).canonical().toString(), makeTestGraph(1)
                .canonical().toString());
    }
    
    public void testInvariant() {
        assertEquals(G.invariant(), cds.minimalImage().invariant());
        assertFalse(G.invariant().equals(dia.invariant()));
        assertEquals(makeTestGraph(2).invariant(), makeTestGraph(1).invariant());
        
        assertEquals("3 1 2 0 0 0 1 2 0 0 1 1 2 0 1 0 1 2 1 0 0", dia.invariant()
                .toString());
        assertEquals("3 1 1 -1 0 0 1 2 0 0 0 1 2 0 1 0 2 2 0 0 -1", cds.minimalImage()
                .invariant().toString());
        verifyKey("3 1 2 0 0 0 1 2 0 0 1 1 2 0 1 0 1 2 1 0 0");
        verifyKey("3 1 1 -1 0 0 1 2 0 0 0 1 2 0 1 0 2 2 0 0 -1");
        verifyKey("2 1 2 0 0 1 2 0 1 1 2 1 0");
        verifyKey("3 1 2 0 0 0 1 3 0 0 0 1 4 0 0 0 2 3 0 1 0 2 4 1 0 0 3 4 0 0 1");
        verifyKey("3 1 2 0 0 0 1 3 0 0 0 1 4 0 0 0 2 5 0 0 0 2 6 0 0 0 3 4 0 0 0 "
                + "3 7 0 0 0 4 8 0 0 0 5 6 0 0 0 5 9 0 0 0 6 10 0 0 0 7 10 1 0 0 "
                + "7 11 0 0 0 8 9 0 1 0 8 12 0 0 0 9 12 0 -1 0 10 11 -1 0 0 11 12 0 0 1");
        verifyKey("3 1 2 0 0 0 1 2 0 1 0 1 3 0 0 0 1 3 1 0 0 2 3 0 0 1 2 3 1 -1 -1");
        verifyKey("3 1 2 0 0 0 1 3 0 0 0 1 4 0 0 0 2 5 0 0 0 2 6 0 0 0 3 7 0 0 0 "
                + "3 8 0 0 0 4 8 0 0 0 4 9 0 0 0 5 10 0 0 0 5 11 0 0 0 6 10 0 0 0 "
                + "6 12 0 0 0 7 11 0 1 0 7 12 1 0 0 8 10 0 0 1 9 11 -1 0 1 9 12 0 -1 1");
        verifyKey("3 1 2 0 0 0 1 3 0 0 0 1 4 0 0 0 2 5 0 0 0 3 6 0 0 0 4 7 0 0 0 "
                + "5 8 0 0 0 5 9 0 0 0 6 9 1 0 0 6 10 0 0 0 7 8 0 0 1 7 10 0 1 0");
    }
    
    private PeriodicGraph makeTestGraph(final int type) {
        final Vector x = new Vector(1, 0);
        final Vector y = new Vector(0, 1);
        final PeriodicGraph G = new PeriodicGraph(2);
        final INode v1 = G.newNode();
        final INode v2 = G.newNode();
        final INode v3 = G.newNode();
        final INode v4 = G.newNode();
        G.newEdge(v1, v2);
        G.newEdge(v1, v3);
        G.newEdge(v1, v4);
        G.newEdge(v2, v3, x);
        if (type == 1) {
            G.newEdge(v2, v4, x);
        } else {
            G.newEdge(v2, v4, y);
        }
        G.newEdge(v3, v4, y);
        return G;
    }
    
    private void verifyKey(final String key) {
        final List numbers = new ArrayList();
        final String fields[] = key.split("\\s+");
        for (int i = 0; i < fields.length; ++i) {
            numbers.add(new Integer(fields[i]));
        }
        final int d = ((Integer) numbers.get(0)).intValue();
        final int n = (numbers.size() - 1) / (d + 2);
        final PeriodicGraph G = new PeriodicGraph(d);
        final List nodes = new ArrayList();
        nodes.add(null);
        for (int i = 0; i < n; ++i) {
            final int offset = 1 + i * (d + 2);
            final int s = ((Integer) numbers.get(offset)).intValue();
            final int t = ((Integer) numbers.get(offset + 1)).intValue();
            if (s == nodes.size()) {
                nodes.add(G.newNode());
            }
            if (t == nodes.size()) {
                nodes.add(G.newNode());
            }
            if (s >= nodes.size() || t >= nodes.size()) {
                throw new RuntimeException("something's wrong here");
            }
            final int[] shift = new int[d];
            for (int j = 0; j < d; ++j) {
                final Integer x = (Integer) numbers.get(offset + 2 + j);
                shift[j] = x.intValue();
            }
            G.newEdge((INode) nodes.get(s), (INode) nodes.get(t), shift);
        }
        assertEquals(key, G.invariant().toString());
    }
    
    public void testEquals() {
        assertEquals(G, G);
        assertEquals(dia, dia);
        assertEquals(G, cds.minimalImage());
        assertFalse(dia.equals(G));
        assertEquals(makeTestGraph(2), makeTestGraph(1));
    }
    
    public void testHashCode() {
        assertEquals(G.hashCode(), G.hashCode());
        assertEquals(dia.hashCode(), dia.hashCode());
        assertEquals(G.hashCode(), cds.minimalImage().hashCode());
        assertFalse(dia.hashCode() == G.hashCode());
    }
}
