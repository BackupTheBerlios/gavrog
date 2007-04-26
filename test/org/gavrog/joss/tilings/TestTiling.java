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

package org.gavrog.joss.tilings;

import java.util.Iterator;

import junit.framework.TestCase;

import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;

/**
 * @author Olaf Delgado
 * @version $Id: TestTiling.java,v 1.7 2007/04/26 20:51:15 odf Exp $
 */
public class TestTiling extends TestCase {
	final private Tiling t1 = new Tiling(new DSymbol("1 3:1,1,1,1:4,3,4"));
	final private Tiling t2 = new Tiling(new DSymbol("2 3:2,1 2,1 2,2:6,3 2,6"));
	final private Tiling t3 = new Tiling(new DSymbol("1 2:1,1,1:4,4"));
	final private PeriodicGraph gr1 = PeriodicGraph.fromInvariantString("3"
			+ "   1 1 1 0 0   1 1 0 1 0   1 1 0 0 1");
	final private PeriodicGraph gr2 = PeriodicGraph.fromInvariantString("3"
			+ "   1 2 0 0 0   1 2 1 0 0   1 2 0 1 0   1 2 0 0 1");
	final private PeriodicGraph gr3 = PeriodicGraph.fromInvariantString("2"
			+ "   1 1 1 0   1 1 0 1");
    
    public void testSkeleton() {
        final PeriodicGraph sk1 = t1.getSkeleton();
        final PeriodicGraph sk2 = t2.getSkeleton();
        final PeriodicGraph sk3 = t3.getSkeleton();
        assertEquals(gr1, sk1);
        assertEquals(gr2, sk2);
        assertEquals(gr3, sk3);
        assertFalse(gr1.equals(sk2));
        assertFalse(gr2.equals(sk1));
    }
    
    public void testVertexBarycentricPositions() {
    	testVertexBarycentricPositions(t1);
    	testVertexBarycentricPositions(t2);
    	testVertexBarycentricPositions(t3);
    }
    
    public void testVertexBarycentricPositions(final Tiling til) {
        final DelaneySymbol cover = til.getCover();
        final int dim = cover.dim();
        for (final Iterator elms = cover.elements(); elms.hasNext();) {
        	final Object D = elms.next();
        	for (int i = 0; i <= dim; ++i) {
        		final Object Di = cover.op(i, D);
        		final Vector t = til.edgeTranslation(i, D);
                // --- make sure tiles stay connected
        		if (i != cover.dim()) {
        			assertEquals(Vector.zero(dim), t);
        		}
                // --- make sure chambers fit together nicely
        		for (int j = 0; j < dim; ++j) {
        			if (j == i) {
        				continue;
        			}
            		final Point p = til.vertexBarycentricPosition(j, D);
            		final Point q = til.vertexBarycentricPosition(j, Di);
            		assertEquals(q, p.plus(t));
        		}
        	}
        }
    }
    
    public void testSpaceGroup() {
        testSpaceGroup(t1, "Pm-3m");
        testSpaceGroup(t2, "Fd-3m");
        testSpaceGroup(t3, "p4mm");
    }
    
    public void testSpaceGroup(final Tiling til, final String name) {
        assertEquals(name, til.getSpaceGroup().getName());
    }
}
