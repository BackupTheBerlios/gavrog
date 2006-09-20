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

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.gavrog.box.collections.NiceIntList;
import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.IEdge;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;

/**
 * @author Olaf Delgado
 * @version $Id: Output.java,v 1.3 2006/09/20 20:53:57 odf Exp $
 */
public class Output {
    public static void writePGR(final Writer out, final PeriodicGraph G, final String name)
			throws IOException {
    	
        out.write("PERIODIC_GRAPH\n");
        if (name != null) {
            out.write("  NAME " + name + "\n");
        }
        final Map node2idx = new HashMap();
        int i = 0;
        for (final Iterator nodes = G.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            node2idx.put(v, new Integer(++i));
        }
        final List[] tmp = new List[G.numberOfEdges()];
        out.write("  EDGES\n");
        i = 0;
        for (final Iterator edges = G.edges(); edges.hasNext();) {
            final IEdge e = (IEdge) edges.next();
            final INode v = e.source();
            final INode w = e.target();
            final Vector s = G.getShift(e);
            final List list = new LinkedList();
            list.add(node2idx.get(v));
            list.add(node2idx.get(w));
            for (int k = 0; k < s.getDimension(); ++k) {
                list.add(s.get(k));
            }
            tmp[i] = new NiceIntList(list);
            ++i;
        }
        Arrays.sort(tmp);
        for (i = 0; i < tmp.length; ++i) {
        	final List e = tmp[i];
        	final StringBuffer line = new StringBuffer(20);
        	line.append("    ");
        	line.append(format(e.get(0), true));
        	line.append(' ');
        	line.append(format(e.get(1), true));
        	line.append("   ");
        	for (int k = 2; k < e.size(); ++k) {
        		line.append(' ');
        		line.append(format(e.get(k), false));
        	}
        	line.append('\n');
            out.write(line.toString());
        }
        out.write("END\n");
    }
    
    private static String format(final Object num, final boolean isIndex) {
    	final StringBuffer tmp = new StringBuffer(5);
    	final int n;
    	if (num instanceof Whole) {
    		n = ((Whole) num).intValue();
    	} else {
    		n = ((Integer) num).intValue();
    	}
		if (isIndex) {
			if (n < 10) {
				tmp.append(' ');
			}
			if (n < 100) {
				tmp.append(' ');
			}
		} else {
			if (n >= 0) {
				tmp.append(' ');
			}
		}
		tmp.append(n);
    	return tmp.toString();
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
				final Net G = parser.parseNet();
				if (G == null) {
					return;
				} else {
					Output.writePGR(w, G.canonical(), G.getName());
					w.write('\n');
					w.flush();
				}
			}
		} catch (final IOException ex) {
			System.err.print(ex);
			System.exit(1);
		}
	}
}
