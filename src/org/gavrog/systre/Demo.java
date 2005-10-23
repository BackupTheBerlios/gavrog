/*
Copyright 2005 Olaf Delgado-Friedrichs

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

package org.gavrog.systre;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

import org.gavrog.joss.geometry.SpaceGroup;
import org.gavrog.joss.geometry.SpaceGroupFinder;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;
import org.gavrog.joss.pgraphs.io.NetParser;

/**
 * @author Olaf Delgado
 * @version $Id: Demo.java,v 1.1 2005/10/23 21:43:31 odf Exp $
 */
public class Demo {
    public static void main(final String args[]) {
        NetParser parser = null;
        int count = 0;
        try {
            parser = new NetParser(new FileReader(args[0]));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        while (true) {
            final PeriodicGraph G = parser.parseNet();
            if (G == null) {
                break;
            }
            ++count;
            System.out.println("Graph " + count + ":");
            final int d = G.getDimension();
            System.out.println("  dimension: " + d);
            System.out.println("  # of nodes: " + G.numberOfNodes());
            System.out.println("  # of edges: " + G.numberOfEdges());
            System.out.flush();
            System.out.println("  connected: " + (G.isConnected() ? "yes" : "no"));
            System.out.flush();
            System.out.println("  stable       : " + (G.isStable() ? "yes" : "no"));
            System.out.flush();
            System.out.println("  loc. stable: " + (G.isLocallyStable() ? "yes" : "no"));
            System.out.flush();
            
            if (!G.isStable()) {
                System.out.println("  --- further computations need stable graph ---");
                System.out.println();
                System.out.flush();
            } else {
                final PeriodicGraph G1 = G.minimalImage();
                final int r = G.numberOfNodes() / G1.numberOfNodes();
                System.out.println("  extra translations: " + (r - 1));
                if (r > 1) {
                    System.out.println("  --- continuing with minimal repeat unit ---");
                    System.out.println("  # of nodes: " + G1.numberOfNodes());
                    System.out.println("  # of edges: " + G1.numberOfEdges());
                    System.out.flush();
                }
                final List ops = G1.symmetryOperators();
                System.out.println("  point syms: " + ops.size());
                System.out.flush();
                final SpaceGroupFinder finder = new SpaceGroupFinder(new SpaceGroup(d,
                        ops));
                final String group = finder.getGroupName();
                System.out.println("  spacegroup: "
                                   + (group == null ? "not found" : group));
                System.out.flush();
                System.out.println("  invariant: " + G1.invariant().toString());
                System.out.println();
                System.out.flush();
            }
        }
    }
}
