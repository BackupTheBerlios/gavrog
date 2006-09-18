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

package org.gavrog.joss.dsyms.derived;

import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.derived.Skeleton;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;

import junit.framework.TestCase;

/**
 * @author Olaf Delgado
 * @version $Id: TestSkeleton.java,v 1.1 2006/09/18 21:07:56 odf Exp $
 */
public class TestSkeleton extends TestCase {
    final private DSymbol ds1 = new DSymbol("1 3:1,1,1,1:4,3,4");
    final private DSymbol ds2 = new DSymbol("2 3:2,1 2,1 2,2:6,3 2,6");
    final private PeriodicGraph gr1 = PeriodicGraph
            .fromInvariantString("3   1 1 1 0 0   1 1 0 1 0   1 1 0 0 1");
    final private PeriodicGraph gr2 = PeriodicGraph
            .fromInvariantString("3   1 2 0 0 0   1 2 1 0 0   1 2 0 1 0   1 2 0 0 1");
    
    public void testSkeleton() {
        final PeriodicGraph sk1 = new Skeleton(ds1);
        final PeriodicGraph sk2 = new Skeleton(ds2);
        assertEquals(gr1, sk1);
        assertEquals(gr2, sk2);
        assertFalse(gr1.equals(sk2));
        assertFalse(gr2.equals(sk1));
    }
}
