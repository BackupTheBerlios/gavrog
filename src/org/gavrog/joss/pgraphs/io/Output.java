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

package org.gavrog.joss.pgraphs.io;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.IEdge;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;

/**
 * @author Olaf Delgado
 * @version $Id: Output.java,v 1.1 2006/04/04 22:15:36 odf Exp $
 */
public class Output {
    public static void writePGR(final Writer writer, final PeriodicGraph G, final String name) {
        final PrintWriter out = new PrintWriter(writer);
        out.println("PERIODIC_GRAPH");
        if (name != null) {
            out.println("  NAME " + name);
        }
        final Map node2idx = new HashMap();
        int i = 0;
        for (final Iterator nodes = G.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            node2idx.put(v, new Integer(++i));
        }
        final String[] tmp = new String[G.numberOfEdges()];
        out.println("  EDGES");
        i = 0;
        for (final Iterator edges = G.edges(); edges.hasNext();) {
            final IEdge e = (IEdge) edges.next();
            final INode v = e.source();
            final INode w = e.target();
            final Vector s = G.getShift(e);
            final StringBuffer line = new StringBuffer(40);
            line.append("    ");
            line.append(node2idx.get(v));
            line.append(" ");
            line.append(node2idx.get(w));
            line.append("  ");
            for (int k = 0; k < s.getDimension(); ++k) {
                line.append(" ");
                line.append(s.get(k));
            }
            tmp[i] = line.toString();
            ++i;
        }
        Arrays.sort(tmp);
        for (i = 0; i < tmp.length; ++i) {
            out.println(tmp[i]);
        }
        out.println("END");
    }
}
