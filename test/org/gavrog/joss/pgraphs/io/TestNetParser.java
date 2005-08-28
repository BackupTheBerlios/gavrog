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

import junit.framework.TestCase;

import org.gavrog.jane.compounds.Matrix;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;

/**
 * @author Olaf Delgado
 * @version $Id: TestNetParser.java,v 1.14 2005/08/28 23:24:40 odf Exp $
 */
public class TestNetParser extends TestCase {
    PeriodicGraph pcu, dia, srs, ths, tfa;
    
    public void setUp() throws Exception {
        super.setUp();
        pcu = NetParser.stringToNet(""
                + "PERIODIC_GRAPH\n"
                + "  1 1  1 0 0\n"
                + "  1 1  0 1 0\n"
                + "  1 1  0 0 1\n"
                + "END\n");
        dia = new PeriodicGraph(3);
        final INode v1 = dia.newNode();
        final INode v2 = dia.newNode();
        dia.newEdge(v1, v2, new int[] { 0, 0, 0 });
        dia.newEdge(v1, v2, new int[] { 1, 0, 0 });
        dia.newEdge(v1, v2, new int[] { 0, 1, 0 });
        dia.newEdge(v1, v2, new int[] { 0, 0, 1 });
        srs = NetParser.stringToNet(""
                + "PERIODIC_GRAPH\n"
                + "  1 2  0 0 0\n"
                + "  1 3  0 0 0\n"
                + "  1 4  0 0 0\n"
                + "  2 3  1 0 0\n"
                + "  2 4  0 1 0\n"
                + "  3 4  0 0 1\n"
                + "END\n");
        ths = NetParser.stringToNet(""
                + "PERIODIC_GRAPH\n"
                + "  1 2  0 0 0\n"
                + "  1 3  0 0 0\n"
                + "  2 4  0 0 0\n"
                + "  1 3  1 0 0\n"
                + "  2 4  0 1 0\n"
                + "  3 4  0 0 1\n"
                + "END\n");
        tfa = NetParser.stringToNet(""
                + "PERIODIC_GRAPH\n"
                + "  1 2  0 0 0\n"
                + "  1 3  0 0 0\n"
                + "  1 3  1 0 0\n"
                + "  2 3  0 1 0\n"
                + "  2 3  0 0 1\n"
                + "END\n");
    }

    public void tearDown() throws Exception {
        pcu = dia = srs = tfa = null;
        super.tearDown();
    }
    
    public void testParsePeriodicGraph() {
        final PeriodicGraph G = NetParser.stringToNet(""
                + "PERIODIC_GRAPH\n"
                + " # the diamond net, of course\n"
                + " 1 2 0 0 0\n"
                + " 1 2 1 0 0\n"
                + " 1 2 0 1 0\n"
                + " 1 2 0 0 1\n"
                + "END\n");
        assertEquals("(1,2,[0,0,0])(1,2,[1,0,0])(1,2,[0,1,0])(1,2,[0,0,1])", G.toString());
    }

    public void testParseSymmetricNet() {
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
                + "END\n");

        assertEquals(dia, D);
        
        final PeriodicGraph sq = NetParser.stringToNet(""
                + "NET # square lattice on the plane\n"
                + "  Group p4mm\n"
                + "  Node 1 0,0\n"
                + "  Edge 1 1 x+1,y\n"
                + "END\n");
        assertEquals(1, sq.numberOfNodes());
        assertEquals(2, sq.numberOfEdges());
        final INode w = (INode) sq.nodes().next();
        assertNotNull(sq.getEdge(w, w, new Matrix(new int[][] {{1,0}})));
        assertNotNull(sq.getEdge(w, w, new Matrix(new int[][] {{0,1}})));
        
        final PeriodicGraph hex1 = NetParser.stringToNet(""
                + "NET # planar honeycombs\n"
                + "  Group p6mm\n"
                + "  Node 1 2/3,1/3\n"
                + "  Edge 1 1 y,y-x\n"
                + "END\n");
        final PeriodicGraph hex2 = NetParser.stringToNet(""
                + "PERIODIC_GRAPH # planar honeycombs\n"
                + "  1 2  0 0\n"
                + "  1 2  1 0\n"
                + "  1 2  0 1\n"
                + "END\n");
        assertEquals(hex2, hex1);
    }
    
    public void testParseCrystal3D() {
        final PeriodicGraph _dia = NetParser.stringToNet(""
                + "CRYSTAL # diamond again\n"
                + "  Group Fd-3m\n"
                + "  Cell  2.3094 2.3094 2.3094  90.0 90.0 90.0\n"
                + "  Node  1 4 5/8 5/8 5/8\n"
                + "END\n");
        assertEquals(dia, _dia);
        
        final PeriodicGraph _srs = NetParser.stringToNet(""
                + "CRYSTAL\n"
                + "NAME srs\n"
                + "GROUP I4132\n"
                + "CELL 2.8284 2.8284 2.8284 90.0 90.0 90.0\n"
                + "VERTICES\n"
                + "  1 3 0.125 0.125 0.125\n"
                + "END\n");
        assertEquals(srs, _srs);
        
        final PeriodicGraph _pcu = NetParser.stringToNet(""
                + "CRYSTAL\n"
                + "  Name pcu\n"
                + "  Group P1\n"
                + "  Cell 2.3 2.3 2.9 90.0 90.0 90.0\n"
                + "  Node 1 6 0.345 0.128 0.743\n"
                + "END\n");
        assertEquals(pcu, _pcu);
        
        final PeriodicGraph _ths = NetParser.stringToNet(""
                + "CRYSTAL\n"
                + "NAME ths\n"
                + "GROUP I41/amd\n"
                + "CELL 1.8856 1.8856 5.3344 90.0 90.0 90.0\n"
                + "VERTICES\n"
                + "  1 3 0.0 0.25 0.9687\n"
                + "END\n");
        assertEquals(ths, _ths);
        
        final PeriodicGraph _tfa = NetParser.stringToNet(""
                + "CRYSTAL\n"
                + "NAME tfa\n"
                + "GROUP I-4m2\n"
                + "CELL 1.8016 1.8016 3.737 90.0 90.0 90.0\n"
                + "VERTICES\n"
                + "  1 3 0.0 0.5 0.3838\n"
                + "  2 4 0.0 0.0 0.0\n"
                + "END\n");
        assertEquals(tfa, _tfa);
        
        final PeriodicGraph tri1 = NetParser.stringToNet(""
                + "CRYSTAL # regular triangle tiling\n"
                + "GROUP p1\n"
                + "CELL 1.0 1.0 60.0\n"
                + "NODE 1 6 0 0\n"
                + "END\n");
        final PeriodicGraph tri2 = NetParser.stringToNet(""
                + "PERIODIC_GRAPH # regular triangle tiling\n"
                + "  1 1  1 0\n"
                + "  1 1  0 1\n"
                + "  1 1  1 1\n"
                + "END\n");
        assertEquals(tri2, tri1);
        
        final PeriodicGraph bathroom1 = NetParser.stringToNet(""
                + "CRYSTAL\n"
                + "GROUP p4mm\n"
                + "CELL 2.4142 2.4142 90.0\n"
                + "NODE 1 3 0.2929 0.0\n"
                + "END\n");
        
        final PeriodicGraph bathroom2 = NetParser.stringToNet(""
                + "PERIODIC_GRAPH\n"
                + "  1 2  0 0\n"
                + "  2 3  0 0\n"
                + "  3 4  0 0\n"
                + "  4 1  0 0\n"
                + "  1 3  1 0\n"
                + "  2 4  0 1\n"
                + "END\n");
        System.err.println(bathroom1);
        System.err.println(bathroom1.barycentricPlacement());
        System.err.println(bathroom1.characteristicBases());
        System.err.println(bathroom1.invariant());
        System.err.println(bathroom2);
        System.err.println(bathroom2.barycentricPlacement());
        System.err.println(bathroom2.characteristicBases());
        System.err.println(bathroom2.invariant());
        assertEquals(bathroom2, bathroom1);
        
        // TODO make the following work
//        final PeriodicGraph hex1 = NetParser.stringToNet(""
//                + "CRYSTAL # planar honeycombs\n"
//                + "GROUP p6mm\n"
//                + "CELL 1.732 1.732 120.0\n"
//                + "VERTEX 1 3 2/3 1/3\n"
//                + "END\n");
//        final PeriodicGraph hex2 = NetParser.stringToNet(""
//                + "PERIODIC_GRAPH # planar honeycombs\n"
//                + "  1 2  0 0\n"
//                + "  1 2  1 0\n"
//                + "  1 2  0 1\n"
//                + "END\n");
//        assertEquals(hex2, hex1);
    }
}
