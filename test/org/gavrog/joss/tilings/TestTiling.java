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

import junit.framework.TestCase;

import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;

/**
 * @author Olaf Delgado
 * @version $Id: TestTiling.java,v 1.1 2007/04/18 23:00:12 odf Exp $
 */
public class TestTiling extends TestCase {
	final private DSymbol ds1 = new DSymbol("1 3:1,1,1,1:4,3,4");
	final private DSymbol ds2 = new DSymbol("2 3:2,1 2,1 2,2:6,3 2,6");
	final private DSymbol ds3 = new DSymbol("1 2:1,1,1:4,4");
	final private PeriodicGraph gr1 = PeriodicGraph.fromInvariantString("3"
			+ "   1 1 1 0 0   1 1 0 1 0   1 1 0 0 1");
	final private PeriodicGraph gr2 = PeriodicGraph.fromInvariantString("3"
			+ "   1 2 0 0 0   1 2 1 0 0   1 2 0 1 0   1 2 0 0 1");
	final private PeriodicGraph gr3 = PeriodicGraph.fromInvariantString("2"
			+ "   1 2 0 0   2 1 1 0   1 3 0 0   3 1 0 1"
			+ "   1 4 0 0   4 1 1 0   4 1 0 1   4 1 1 1"
			+ "   2 4 0 0   4 2 0 1   3 4 0 0   4 3 1 0");
    
    public void testSkeleton() {
        final PeriodicGraph sk1 = new Tiling(ds1).getSkeleton();
        final PeriodicGraph sk2 = new Tiling(ds2).getSkeleton();
        final PeriodicGraph sk3 = new Tiling(ds3).getBarycentricSkeleton();
        assertEquals(gr1, sk1);
        assertEquals(gr2, sk2);
        assertEquals(gr3, sk3);
        assertFalse(gr1.equals(sk2));
        assertFalse(gr2.equals(sk1));
    }
}
