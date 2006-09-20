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

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;

import org.gavrog.box.collections.Partition;
import org.gavrog.joss.geometry.Operator;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.Morphism;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;
import org.gavrog.joss.pgraphs.io.Net;
import org.gavrog.joss.pgraphs.io.NetParser;
import org.gavrog.joss.pgraphs.io.Output;

public class Ladders {
    public static Partition rungPartition(final PeriodicGraph G) {
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
			boolean hasFixedPoints = false;
			for (final Iterator it = G.nodes(); it.hasNext();) {
				final INode w = (INode) it.next();
				final INode u = (INode) iso.get(w);
				if (w.equals(u)) {
					hasFixedPoints = true;
					break;
				}
			}
			if (hasFixedPoints) {
				continue;
			}
			for (final Iterator it = G.nodes(); it.hasNext();) {
				final INode w = (INode) it.next();
				P.unite(w, iso.get(w));
			}
		}

		return P;
	}

    public static void main(final String args[]) {
		try {
			final Reader r;
			final Writer w;
			if (args.length > 0) {
				r = new FileReader(args[0]);
			} else {
				r = new InputStreamReader(System.in);
			}
			if (args.length > 1) {
				w = new FileWriter(args[1]);
			} else {
				w = new OutputStreamWriter(System.out);
			}

			final NetParser parser = new NetParser(r);

			while (true) {
				final Net net = parser.parseNet();
				if (net == null) {
					return;
				} else {
					final PeriodicGraph G = net.canonical();
					Output.writePGR(w, G, net.getName());
					w.write('\n');
					w.write(String.valueOf((rungPartition(G))));
					w.write("\n\n");
					w.flush();
				}
			}
		} catch (final IOException ex) {
			System.err.print(ex);
			System.exit(1);
		}
	}
}
