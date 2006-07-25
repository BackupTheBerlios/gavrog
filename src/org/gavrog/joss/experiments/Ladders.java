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

package org.gavrog.joss.experiments;

import java.util.Iterator;
import java.util.Map;

import org.gavrog.box.collections.Partition;
import org.gavrog.joss.geometry.Operator;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.Morphism;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;

public class Ladders {
    public Iterator ladderEquivalenceClasses(final PeriodicGraph G) {
        // --- check prerequisites
        if (!G.isConnected()) {
            throw new UnsupportedOperationException("graph must be connected");
        }
        if (!G.isLocallyStable()) {
            throw new UnsupportedOperationException("graph must be locally stable");
        }
        
        // --- find equivalence classes w.r.t. ladder translations
        final Operator I = Operator.identity(G.getDimension());
        final Partition P = new Partition();
        final Iterator iter = G.nodes();
        final INode start = (INode) iter.next();
        final Map pos = G.barycentricPlacement();
        final Point pos0 = (Point) pos.get(start);
        
        while (iter.hasNext()) {
			final INode v = (INode) iter.next();
			final Point posv = (Point) pos.get(v);
			if (!((Vector) posv.minus(pos0)).modZ().isZero()) {
				continue;
			}
			if (P.areEquivalent(start, v)) {
				continue;
			}
			final Morphism iso;
			try {
				iso = new Morphism(start, v, I);
			} catch (Morphism.NoSuchMorphismException ex) {
				continue;
			}
			for (final Iterator it = G.nodes(); it.hasNext();) {
				final INode w = (INode) it.next();
				P.unite(w, iso.get(w));
			}
		}

		return P.classes();
	}
}
