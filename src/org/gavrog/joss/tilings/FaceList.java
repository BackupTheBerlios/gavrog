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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gavrog.jane.compounds.Matrix;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.io.NetParser.Face;

/**
 * Implements a periodic face set meant to define a tiling.
 * 
 * @author Olaf Delgado
 * @version $Id: FaceList.java,v 1.3 2007/05/29 21:13:25 odf Exp $
 */
public class FaceList {
	/**
	 * Hashable class for edges in the tiling to be constructed.
	 */
	private static class Edge {
		final public int source;
		final public int target;
		final public Vector shift;
		
		public Edge(final int source, final int target, final Vector shift) {
			if (source < target || (source == target && shift.isNonNegative())) {
				this.source = source;
				this.target = target;
				this.shift = shift;
			} else {
				this.source = target;
				this.target = source;
				this.shift = (Vector) shift.negative();
			}
		}
		
		public int hashCode() {
			return ((source * 37) + target * 127) + shift.hashCode();
		}
		
		public boolean equals(final Object other) {
			final Edge e = (Edge) other;
			return source == e.source && target == e.target
					&& shift.equals(e.shift);
		}
	}
	
	private static class Incidence {
		final public Face face;
		final public int index;
		final public boolean reverse;
		
		public Incidence(final Face face, final int index, final boolean rev) {
			this.face = face;
			this.index = index;
			this.reverse = rev;
		}
	}
	
	public FaceList(final List faces, final Map indexToPosition) {
        // --- determine sector normals for each face
        final Map normals = new HashMap();
        for (final Iterator iter = faces.iterator(); iter.hasNext();) {
            final Face f = (Face) iter.next();
            normals.put(f, sectorNormals(f, indexToPosition));
        }
        
		final Map facesAtEdge = collectEdges(faces, false);
	}
	
    /**
     * Computes normals for the sectors of a face.
     * 
     * @param f the input face.
     * @param indexToPos maps symbolic corners to positions.
     * @return the array of sector normals.
     */
    private static Vector[] sectorNormals(final Face f, final Map indexToPos) {
        final int n = f.size();

        // --- compute corners and center of this face
        Matrix sum = Point.origin(3).getCoordinates();
        final Point corners[] = new Point[n];
        for (int i = 0; i < n; ++i) {
            final Integer v = new Integer(f.vertex(i));
            final Vector s = f.shift(i);
            final Point p = (Point) s.plus(indexToPos.get(v));
            corners[i] = p;
            sum = (Matrix) sum.plus(p.getCoordinates());
        }
        final Point center = new Point((Matrix) sum.dividedBy(n));

        // --- use that to compute the normals
        final Vector normals[] = new Vector[n];
        for (int i = 0; i < n; ++i) {
            final int i1 = (i + 1) % n;
            final Vector a = (Vector) corners[i].minus(center);
            final Vector b = (Vector) corners[i1].minus(corners[i]);
            normals[i] = Vector.crossProduct3D(a, b);
        }

        return normals;
    }
    
	private static Map collectEdges(final List faces, final boolean useShifts) {
		final Map facesAtEdge = new HashMap();
		for (final Iterator iter = faces.iterator(); iter.hasNext();) {
			final Face f = (Face) iter.next();
			final int n = f.size();
			for (int i = 0; i < n; ++i) {
				final int i1 = (i + 1) % n;
				final int v = f.vertex(i);
				final int w = f.vertex(i1);
				final Vector s = (Vector) f.shift(i1).minus(f.shift(i));
				final Edge e = new Edge(v, w, s);
				if (!facesAtEdge.containsKey(e)) {
					facesAtEdge.put(e, new ArrayList());
				}
				((List) facesAtEdge.get(e)).add(new Incidence(f, i, v > w));
			}
		}
		
		return facesAtEdge;
	}
}
