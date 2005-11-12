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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.gavrog.joss.geometry.SpaceGroup;
import org.gavrog.joss.geometry.SpaceGroupFinder;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;
import org.gavrog.joss.pgraphs.io.NetParser;

/**
 * First preview of the upcoming Gavrog version of Systre.
 * 
 * @author Olaf Delgado
 * @version $Id: Demo.java,v 1.10 2005/11/12 05:36:46 odf Exp $
 */
public class Demo {
    public static void main(final String args[]) {
        final Package pkg = Archive.class.getPackage();
        final String packagePath = pkg.getName().replaceAll("\\.", "/");
        final String archivePath = packagePath + "/rcsr.arc";
        final Archive rcsr = new Archive("1.0");
        final InputStream inStream = ClassLoader.getSystemResourceAsStream(archivePath);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
        rcsr.addAll(reader);
        
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
            System.out.println("  dimension:\t\t" + d);
            System.out.println("  number of nodes:\t" + G.numberOfNodes());
            System.out.println("  number of edges:\t" + G.numberOfEdges());
            System.out.flush();
            System.out.println("  connected:\t\t" + (G.isConnected() ? "yes" : "no"));
            System.out.flush();
            System.out.println("  stable:\t\t" + (G.isStable() ? "yes" : "no"));
            System.out.flush();
            System.out.println("  locally stable:\t" + (G.isLocallyStable() ? "yes" : "no"));
            System.out.flush();
            
            if (!G.isStable()) {
                System.out.println("  --- further computations need stable graph ---");
                System.out.println();
                System.out.flush();
            } else {
                final PeriodicGraph G1 = G.minimalImage();
                final int r = G.numberOfNodes() / G1.numberOfNodes();
                System.out.println("  extra translations:\t" + (r - 1));
                if (r > 1) {
                    System.out.println("  --- continuing with minimal repeat unit ---");
                    System.out.println("  number of nodes:\t" + G1.numberOfNodes());
                    System.out.println("  number of edges:\t" + G1.numberOfEdges());
                    System.out.flush();
                }
                final List ops = G1.symmetryOperators();
                System.out.println("  point symmetries:\t" + ops.size());
                System.out.flush();
                final SpaceGroup group = new SpaceGroup(d, ops);
                final SpaceGroupFinder finder = new SpaceGroupFinder(group);
                final String groupName = finder.getGroupName();
                System.out.println("  spacegroup:\t\t"
                                   + (groupName == null ? "not found" : groupName));
                System.out.flush();
                System.out.print("  coordination sequences:");
                for (final Iterator orbits = G1.nodeOrbits(); orbits.hasNext();) {
                    final Set orbit = (Set) orbits.next();
                    final INode v = (INode) orbit.iterator().next();
                    final Iterator cs = G1.coordinationSequence(v);
                    for (int i = 0; i <= 10; ++i) {
                        System.out.print(" " + ((Integer) cs.next()).intValue());
                    }
                    if (orbits.hasNext()) {
                        System.out.print("\n\t\t\t ");
                    }
                }
                System.out.println();
                final String invariant = G1.invariant().toString();
                if (invariant.length() <= 60) {
                    System.out.println("  Systre key:\t\t" + invariant);
                } else {
                    System.out.println("  --- Systre key of length " + invariant.length() + " not displayed ---");
                }
                System.out.flush();
                final Archive.Entry found  = rcsr.getByKey(invariant);
                if (found == null) {
                    System.out.println("  --- no recognized RCSR structure ---");
                } else {
                    System.out.println("  RCSR symbol:\t\t" + found.getName());
                }
                System.out.println("  Gram matrix configuration space: "
                                   + group.configurationSpaceForGramMatrix());
                System.out.println("Done.");
                System.out.println();
                System.out.flush();
            }
        }
    }
}
