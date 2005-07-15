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

package org.gavrog.joss.pgraphs.io;

import java.util.Iterator;
import java.util.List;

import org.gavrog.jane.numbers.Fraction;
import org.gavrog.jane.numbers.Matrix;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;
import org.gavrog.joss.pgraphs.io.DataFormatException;
import org.gavrog.joss.pgraphs.io.NetParser;


import junit.framework.TestCase;

/**
 * @author Olaf Delgado
 * @version $Id: TestNetParser.java,v 1.1 2005/07/15 21:12:50 odf Exp $
 */
public class TestNetParser extends TestCase {

    public void testParseOperator() {
        String s;
        Matrix M;
        
        s = "x-4y+7*z-10, +5/3y-8z+11-2x, +3*x+ 9z-6y - 12";
        M = new Matrix(new int[][] {
                {  1, -2,   3, 0},
                { -4,  5,  -6, 0},
                {  7, -8,   9, 0},
                {-10, 11, -12, 1}}).mutableClone();
        M.set(1, 1, new Fraction(5, 3));
        assertEquals(M, NetParser.parseOperator(s));
        assertFalse(NetParser.parseOperator(s).isMutable());
        
        assertEquals(Matrix.one(4), NetParser.parseOperator("x,y,z"));
        
        try {
            NetParser.parseOperator("1,2,3,4");
            fail("should throw an DataFormatException");
        } catch (DataFormatException success) {
        }
        
        try {
            NetParser.parseOperator("a,2,3");
            fail("should throw an DataFormatException");
        } catch (DataFormatException success) {
        }
        
        try {
            NetParser.parseOperator("1,2/,3");
            fail("should throw an DataFormatException");
        } catch (DataFormatException success) {
        }
        
        try {
            NetParser.parseOperator("x+3x,2,3");
            fail("should throw an DataFormatException");
        } catch (DataFormatException success) {
        }
    }
    
    public void testParseNet() {
        final PeriodicGraph G = NetParser.stringToNet(""
                + "PERIODIC_GRAPH\n"
                + " # the diamond net, of course\n"
                + " 1 2 0 0 0\n"
                + " 1 2 1 0 0\n"
                + " 1 2 0 1 0\n"
                + " 1 2 0 0 1\n"
                + "END\n");
        assertEquals("(1,2,[0,0,0])(1,2,[1,0,0])(1,2,[0,1,0])(1,2,[0,0,1])", G.toString());

        final PeriodicGraph H = NetParser.stringToNet(""
                + "NET # primitive cubic\n"
                + "  Group P432\n"
                + "  Node 1 0,0,0\n"
                + "  Edge 1 1 x+1,y,z\n"
                + "END\n");
        assertEquals(1, H.numberOfNodes());
        assertEquals(3, H.numberOfEdges());
        final INode v = (INode) H.nodes().next();
        assertNotNull(H.getEdge(v, v, new Matrix(new int[][] {{1,0,0}})));
        assertNotNull(H.getEdge(v, v, new Matrix(new int[][] {{0,1,0}})));
        assertNotNull(H.getEdge(v, v, new Matrix(new int[][] {{0,0,1}})));

        final PeriodicGraph D = NetParser.stringToNet(""
                + "NET # the diamond net\n"
                + "  Group Fd-3m\n"
                + "  Node 1 3/8,3/8,3/8\n"
                + "  Edge 1 1 1-x,1-y,1-z\n"
                + "END\n").minimalImage();
        final INode w = (INode) D.nodes().next();
        final Iterator cs = D.coordinationSequence(w);
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
    
    public void testOperators() {
        final List ops = NetParser.operators("Ia-3d");
        assertNotNull(ops);
        assertEquals(96, ops.size());
    }
    
    public void testTransform() {
        final Matrix T = NetParser.transform("Ia-3d");
        assertNotNull(T);
        assertEquals(Matrix.one(4), T);
    }
}
