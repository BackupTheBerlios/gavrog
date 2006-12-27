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

package org.gavrog.joss.dsyms.filters;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.gavrog.box.simple.Misc;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.derived.Covers;
import org.gavrog.joss.dsyms.derived.Skeleton;
import org.gavrog.joss.dsyms.generators.InputIterator;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;


/**
 * A tiling is proper if it has the same symmetry as its underlying net.
 * 
 * @author Olaf Delgado
 * @version $Id: FilterProper.java,v 1.8 2006/12/27 17:36:58 odf Exp $
 */
public class FilterProper {

    public static void main(String[] args) {
		try {
			boolean unique = false;
			boolean canonical = false;
			boolean dualize = false;
			
	        int i = 0;
	        while (i < args.length && args[i].startsWith("-")) {
	        	if (args[i].equalsIgnoreCase("-c")) {
	        		canonical = !canonical;
	        	} else if (args[i].equalsIgnoreCase("-u")){
	        		unique = !unique;
	        	} else if (args[i].equalsIgnoreCase("-d")){
	        		dualize = !dualize;
	        	} else {
	        		System.err.println("Unknown option '" + args[i] + "'");
	        	}
	            ++i;
	        }
			
			final Reader in;
			final Writer out;
			if (args.length > i) {
				in = new FileReader(args[i]);
			} else {
				in = new InputStreamReader(System.in);
			}
			if (args.length > i+1) {
				out = new FileWriter(args[i+1]);
			} else {
				out = new OutputStreamWriter(System.out);
			}

			int inCount = 0;
			int outCount = 0;
			final Set seen = new HashSet();

			for (final Iterator input = new InputIterator(in); input.hasNext();) {
				DSymbol ds = (DSymbol) input.next();
				++inCount;
				if (dualize) {
					ds = ds.dual();
				}
				try {
					final DSymbol min = new DSymbol(ds.minimal());
					final DSymbol cov = new DSymbol(Covers
							.pseudoToroidalCover3D(min));
					final PeriodicGraph gr = new Skeleton(cov);
					if (!gr.isStable() || !gr.isMinimal()) {
						continue;
					}
					if (unique && seen.contains(gr.invariant())) {
						continue;
					}
					if (gr.symmetries().size() != cov.size() / min.size()) {
						continue;
					}
					++outCount;
					if (canonical) {
						out.write(ds.toString());
					} else {
						out.write(ds.canonical().flat().toString());
					}
					if (unique) {
						seen.add(gr.invariant());
					}
				} catch (final Exception ex) {
					out.write(Misc.stackTrace(ex, "# "));
					out.write("# in symbol " + ds);
				}
				out.write('\n');
				out.flush();
			}
	        out.write("### Read " + inCount + " and wrote " + outCount + " symbols.\n");
	        out.flush();
		} catch (final IOException ex) {
			ex.printStackTrace();
		}
    }
}
