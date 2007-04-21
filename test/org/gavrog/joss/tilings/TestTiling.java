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
 * @version $Id: TestTiling.java,v 1.3 2007/04/21 04:52:14 odf Exp $
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
			+ "   1 2 0 0   2 1 1 0   1 3 0 0   3 1 0 1"
			+ "   1 4 0 0   4 1 1 0   4 1 0 1   4 1 1 1"
			+ "   2 4 0 0   4 2 0 1   3 4 0 0   4 3 1 0");
    
    public void testSkeleton() {
        final PeriodicGraph sk1 = t1.getSkeleton();
        final PeriodicGraph sk2 = t2.getSkeleton();
        final PeriodicGraph sk3 = t3.getBarycentricSkeleton();
        assertEquals(gr1, sk1);
        assertEquals(gr2, sk2);
        assertEquals(gr3, sk3);
        assertFalse(gr1.equals(sk2));
        assertFalse(gr2.equals(sk1));
    }
    
    public void testBarycentricPositionsByVertex() {
    	testBarycentricPositionsByVertex(t1);
    	testBarycentricPositionsByVertex(t2);
    	testBarycentricPositionsByVertex(t3);
    }
    
    public void testBarycentricPositionsByVertex(final Tiling til) {
        final DelaneySymbol cover = til.getCover();
        final int dim = cover.dim();
        for (final Iterator elms = cover.elements(); elms.hasNext();) {
        	final Object D = elms.next();
        	for (int i = 0; i <= dim; ++i) {
        		final Object Di = cover.op(i, D);
        		final Vector t = til.edgeTranslation(i, D);
        		if (i != cover.dim()) {
        			assertEquals(Vector.zero(dim), t);
        		}
        		for (int j = 0; j < dim; ++j) {
        			if (j == i) {
        				continue;
        			}
            		final Point p = til.positionByVertex(j, D);
            		final Point q = til.positionByVertex(j, Di);
            		assertEquals(q, p.plus(t));
        		}
        	}
        }
    }
}
