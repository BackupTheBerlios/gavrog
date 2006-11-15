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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.collections.Iterators;
import org.gavrog.box.collections.Pair;
import org.gavrog.box.collections.Partition;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.IndexList;

/**
 * @author Olaf Delgado
 * @version $Id: OrbifoldGraph.java,v 1.2 2006/11/15 00:15:42 odf Exp $
 */
public class OrbifoldGraph {

    public OrbifoldGraph(final DelaneySymbol input) {
        final DSymbol ds = new DSymbol(input);
        
        // --- check argument
        if (ds.dim() != 3) {
            final String msg = "symbol must be 3-dimensional";
            throw new UnsupportedOperationException(msg);
        }
        if (!ds.isLocallyEuclidean3D()) {
            final String msg = "symbol must be locally euclidean";
            throw new UnsupportedOperationException(msg);
        }
        
        // --- initialize
        final int d = ds.dim();
        final List orbs = new ArrayList();
        final Map edges = new HashMap();
        final Map orb2type = new HashMap();
        final Map orb2rep = new HashMap();
        final Map orb2elms = new HashMap();
        final Partition p = new Partition();
        
        // --- process 0-dimensional orbits (chamber faces)
        for (int i = 0; i <= d; ++i) {
            for (final Iterator elms = ds.elements(); elms.hasNext();) {
                final Object D = elms.next();
                if (ds.op(i, D).equals(D)) {
                    final Pair orb = new Pair(new IndexList(i), D);
                    orbs.add(orb);
                    orb2type.put(orb, "*");
                    edges.put(orb, new ArrayList());
                }
            }
        }
        
        // --- add 1-dimensional orbits (chamber edges)
        
        for (int i = 0; i < d; ++i) {
            final List ili = new IndexList(i);
            for (int j = i+1; j <= d; ++j) {
                final List ilj = new IndexList(j);
                final List idcs = new IndexList(i, j);
                for (final Iterator reps = ds.orbitRepresentatives(idcs); reps
                        .hasNext();) {
                    final Object D = reps.next();
                    
                    // --- find the 0-dim orbits of type "*" in this orbit
                    final List cuts = new ArrayList();
                    for (final Iterator iter = ds.orbit(idcs, D); iter
                            .hasNext();) {
                        final Object E = iter.next();
                        final Pair ci = new Pair(ili, E);
                        final Pair cj = new Pair(ilj, E);
                        if (orb2type.containsKey(ci)) {
                            cuts.add(ci);
                        }
                        if (orb2type.containsKey(cj)) {
                            cuts.add(cj);
                        }
                    }
                    
                    // --- determine the stabilizer type of this orbit
                    String type = (cuts.size() > 0 ? "*" : "");
                    final int v = ds.v(i, j, D);
                    if (v > 1) {
                        type = type + v + v;
                    }
                    
                    // --- store and link this orbit if stabilizer not trivial
                    if (type.length() > 0) {
                        final Pair orb = new Pair(idcs, D);
                        orb2elms.put(orb, Iterators.asList(ds.orbit(idcs, D)));
                        orbs.add(orb);
                        orb2type.put(orb, type);
                        
                        if (cuts.size() > 0) {
                            final Pair ca = (Pair) cuts.get(0);
                            final Pair cb = (Pair) cuts.get(1);
                            ((List) edges.get(ca)).add(orb);
                            ((List) edges.get(cb)).add(orb);
                            edges.put(orb, cuts);
                            if (type.equals("*")) {
                                p.unite(orb, ca);
                                p.unite(orb, cb);
                            }
                        } else {
                            edges.put(orb, new ArrayList());
                        }
                        
                        // --- let this orbit be represented by element D
                        for (final Iterator iter = ds.orbit(idcs, D); iter
                                .hasNext();) {
                            orb2rep.put(new Pair(idcs, iter.next()), orb);
                        }
                    }
                }
            }
        }

        // --- add 2-dimensional orbits (chamber vertices)
        for (int i = 0; i <= 3; ++i) {
            final List idcs = new IndexList(ds);
            idcs.remove(new Integer(i));
            for (final Iterator reps = ds.orbitRepresentatives(idcs); reps
                    .hasNext();) {
                final List sub = Iterators.asList(ds.orbit(idcs, reps.next()));
                final List cones = new ArrayList();
                final List corners = new ArrayList();
                final List neighbors = new ArrayList();
                
                // --- collect all relevant 1-dim orbits this one contains
                for (int j = 0; j <= 2; ++j) {
                    final int n = ((Integer) idcs.get((j + 1) % 3)).intValue();
                    final int m = ((Integer) idcs.get((j + 2) % 3)).intValue();
                    final List ilnm = new IndexList(n, m);
                    final Set seen = new HashSet();
                    for (final Iterator elms = sub.iterator(); elms.hasNext();) {
                        final Object D = elms.next();
                        if (!seen.contains(D)) {
                            final Pair orb0 = new Pair(ilnm, D);
                            final Pair orb = (Pair) orb2rep.get(orb0);    
                            seen.addAll((List) orb2elms.get(orb));
                            final String t = (String) orb2type.get(orb);
                            if (t.length() > 0) {
                                if (t.charAt(0) == '*') {
                                    if (t.length() > 1) {
                                        corners.add(t.substring(1, 2));
                                    }
                                } else {
                                    cones.add(t.substring(0, 1));
                                }
                                neighbors.add(orb);
                            }
                        }
                    }
                }
            }
        }
//                cones.sort()
//                cones.reverse()
//                corners.sort()
//                corners.reverse()
//                neighbors.sort()
//
//                D = sub2d[0]
//
//                if ds.orbit_is_loopless((i, j, k), D):
//                    type = cones
//                else:
//                    type = cones + ['*'] + corners
//                if not ds.orbit_is_weakly_oriented((i, j, k), D):
//                    type.append('x')
//                type = tuple(type)
//
//                if type == ():
//                    continue
//
//                orb = ((i, j, k), D)
//                orbs.append(orb)
//                orb2type[orb] = type
//
//                edges[orb] = neighbors
//                for v in neighbors:
//                    edges[v].append(orb)
//
//                for v in neighbors:
//                    if orb2type[v] == type:
//                        p.unite(orb, v)
//
//        # --- convert stabilizer types to strings ---
//
//        for (orb, val) in orb2type.items():
//            val = string.join(map(str, val), '')
//            if val in ('*', 'x'):
//                val = '1' + val
//            orb2type[orb] = val
//
//        # --- reduce equivalence classes to single nodes ---
//
//        orbs = orb2type.keys()
//        orb2class = {}
//        class2nr = {}
//        nr_classes = 0
//        reps = p.reps(orbs).items()
//        reps.sort(lambda a, b, o2t = orb2type: cmp(o2t[a[0]], o2t[b[0]]))
//
//        for (orb, cl) in reps:
//            n = class2nr.get(cl)
//            if n is None:
//                n = class2nr[cl] = nr_classes
//                nr_classes = nr_classes + 1
//            orb2class[orb] = n
//
//        c_edges = [None] * nr_classes
//        class2type = [None] * nr_classes
//
//        for (orb, cl) in orb2class.items():
//            c_edges[cl] = {}
//            class2type[cl] = orb2type[orb]
//
//        for (orb, cl) in orb2class.items():
//            a = c_edges[cl]
//            for v in edges[orb]:
//                cl_v = orb2class[v]
//                if cl_v != cl:
//                    a[cl_v] = 1
//                    c_edges[cl_v][cl] = 1
//
//        # --- post processing ---
//
//        for cl in range(nr_classes):
//            a = c_edges[cl].keys()
//            a.sort()
//            c_edges[cl] = a
//
//        # --- spit it out ---
//
//        return class2type, c_edges
        
    }
}
