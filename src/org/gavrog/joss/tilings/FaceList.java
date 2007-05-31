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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gavrog.box.collections.Pair;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.Real;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.io.GenericParser;
import org.gavrog.joss.pgraphs.io.NetParser;
import org.gavrog.joss.pgraphs.io.NetParser.Face;

/**
 * Implements a periodic face set meant to define a tiling.
 * 
 * @author Olaf Delgado
 * @version $Id: FaceList.java,v 1.8 2007/05/31 00:34:35 odf Exp $
 */
public class FaceList {
	final private static boolean DEBUG = false;
	
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
		
		public String toString() {
			return "(" + source + "," + target + "," + shift + ")";
		}
	}
	
	private static class Incidence implements Comparable {
		final public Face face;
		final public int index;
		final public boolean reverse;
        final public double angle;
		
		public Incidence(final Face face, final int index, final boolean rev,
                final double angle) {
			this.face = face;
			this.index = index;
			this.reverse = rev;
            this.angle = angle;
		}
        
        public Incidence(final Face face, final int index, final boolean rev) {
            this(face, index, rev, 0.0);
        }
        
        public Incidence(final Incidence source, final double angle) {
            this(source.face, source.index, source.reverse, angle);
        }

        public int compareTo(final Object arg) {
            if (arg instanceof Incidence) {
                final Incidence other = (Incidence) arg;
                if (this.angle < other.angle) {
                    return -1;
                } else if (this.angle > other.angle) {
                    return 1;
                }
                int d = this.face.compareTo(other.face);
                if (d != 0) {
                    return d;
                }
                if (this.index != other.index) {
                    return this.index - other.index;
                }
                if (!this.reverse && other.reverse) {
                    return -1;
                } else if (this.reverse && !other.reverse) {
                    return 1;
                } else {
                    return 0;
                }
            } else {
                throw new IllegalArgumentException("Incidence expected");
            }
        }
        
        public String toString() {
        	return "(" + face + "," + index + "," + reverse + "," + angle + ")";
        }
	}
	
    final private List faces;
    final private Map indexToPos;
    final private int dim;
    final private DSymbol ds;
    
	public FaceList(final List input, final Map indexToPosition) {
		if (DEBUG) {
			System.err.println("\nStarting FaceList constructor");
		}
        if (input == null || input.size() < 1) {
            throw new IllegalArgumentException("no data given");
        }
        
        final List tiles;
        final Map tilesAtFace;
        if (input.get(0) instanceof List) {
            tiles = new ArrayList();
            tilesAtFace = new HashMap();
            for (int i = 0; i < input.size(); ++i) {
                final List tile = (List) input.get(i);
                final List newTile = new ArrayList();
                for (int j = 0; j < tile.size(); ++j) {
                    final Pair entry = (Pair) tile.get(j);
                    final Face face = (Face) entry.getFirst();
                    final Pair normal = NetParser.normalizedFace(face);
                    final Vector shift = (Vector) ((Vector) entry.getSecond())
                            .plus(normal.getSecond());
                    if (!tilesAtFace.containsKey(face)) {
                        tilesAtFace.put(face, new ArrayList());
                    }
                    ((List) tilesAtFace.get(face)).add(new Pair(new Integer(i),
                            shift));
                    newTile.add(new Pair(face, shift));
                }
                tiles.add(newTile);
            }
            this.faces = new ArrayList();
            this.faces.addAll(tilesAtFace.keySet());
        } else {
            tiles = null;
            tilesAtFace = null;
            this.faces = new ArrayList();
            this.faces.addAll(input);
        }
        
        final Face f0 = (Face) this.faces.get(0);
        if (f0.size() < 3) {
            throw new IllegalArgumentException("minimal face-size is 3");
        }
        this.dim = f0.shift(0).getDimension();
        if (this.dim != 3) {
            throw new UnsupportedOperationException("dimension must be 3");
        }
        
        this.indexToPos = indexToPosition;
        
        // --- initialize the intermediate symbol
        final Map faceElements = new HashMap();
        final DynamicDSymbol ds = new DynamicDSymbol(this.dim);
        for (final Iterator iter = this.faces.iterator(); iter.hasNext();) {
            final Face f = (Face) iter.next();
            final int n = f.size();
            final int _2n = 2 * n;
            final List elms = ds.grow(4 * n);
            faceElements.put(f, elms);
            for (int i = 0; i < 4 * n; i += 2) {
                ds.redefineOp(0, elms.get(i), elms.get(i + 1));
            }
            for (int i = 1; i < _2n; i += 2) {
                final int i1 = (i + 1) % _2n;
                ds.redefineOp(1, elms.get(i), elms.get(i1));
                ds.redefineOp(1, elms.get(i + _2n), elms.get(i1 + _2n));
            }
            for (int i = 0; i < _2n; ++i) {
                ds.redefineOp(3, elms.get(i), elms.get(i + _2n));
            }
        }
        
        if (DEBUG) {
        	System.err.println("Symbol without 2-ops: " + new DSymbol(ds));
        }
        
        if (tiles == null) {
            set2opPlainMode(ds, faceElements);
        }
        
        if (DEBUG) {
        	System.err.println("Symbol with 2-ops: " + new DSymbol(ds));
        }
        
        // --- set all v values to 1
        for (int i = 0; i < dim; ++i) {
        	for (final Iterator iter = ds.elements(); iter.hasNext();) {
        		final Object D = iter.next();
        		if (!ds.definesV(i, i + 1, D)) {
					ds.redefineV(i, i + 1, D, 1);
				}
        	}
        }
        
        if (DEBUG) {
        	System.err.println("Completed symbol: " + new DSymbol(ds));
        }
        
        // --- freeze the constructed symbol
        //TODO keep given symmetry
        this.ds = new DSymbol(ds.minimal());
	}
	
    private FaceList(final Pair p) {
        this((List) p.getFirst(), (Map) p.getSecond());
    }
    
    public FaceList(final GenericParser.Block data) {
        this(NetParser.parseFaceList(data));
    }
    
    public DSymbol getSymbol() {
        return this.ds;
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
        if (DEBUG) {
        	System.err.println("Computing normals for face " + f);
        }
        
        // --- compute corners and center of this face
        Matrix sum = null;
        final Point corners[] = new Point[n];
        for (int i = 0; i < n; ++i) {
            final Integer v = new Integer(f.vertex(i));
            final Vector s = f.shift(i);
            final Point p = (Point) s.plus(indexToPos.get(v));
            corners[i] = p;
            if (sum == null) {
                sum = p.getCoordinates();
            } else {
                sum = (Matrix) sum.plus(p.getCoordinates());
            }
        }
        final Point center = new Point((Matrix) sum.dividedBy(n));

        // --- use that to compute the normals
        final Vector normals[] = new Vector[n];
        for (int i = 0; i < n; ++i) {
            final int i1 = (i + 1) % n;
            final Vector a = (Vector) corners[i].minus(center);
            final Vector b = (Vector) corners[i1].minus(corners[i]);
            normals[i] = Vector.unit(Vector.crossProduct3D(a, b));
            if (DEBUG) {
            	System.err.println("  " + normals[i]);
            }
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
				final boolean rev = (e.source != v || !e.shift.equals(s));
				if (!facesAtEdge.containsKey(e)) {
					facesAtEdge.put(e, new ArrayList());
				}
				((List) facesAtEdge.get(e)).add(new Incidence(f, i, rev));
			}
		}
		
		if (DEBUG) {
			System.err.println("Edge to incident faces mapping:");
			for (Iterator iter = facesAtEdge.keySet().iterator(); iter
					.hasNext();) {
				final Edge e = (Edge) iter.next();
				final List inc = (List) facesAtEdge.get(e);
				System.err.println("  " + inc.size() + " at edge " + e + ":");
				for (int i = 0; i < inc.size(); ++i) {
					System.err.println("    " + inc.get(i));
				}
			}
		}
		return facesAtEdge;
	}

    private void set2opPlainMode(final DynamicDSymbol ds, final Map faceElms) {
        // --- determine sector normals for each face
        final Map normals = new HashMap();
        for (final Iterator iter = this.faces.iterator(); iter.hasNext();) {
            final Face f = (Face) iter.next();
            normals.put(f, sectorNormals(f, this.indexToPos));
        }
        
        // --- set 2 operator according to cyclic orders of faces around edges
        final Map facesAtEdge = collectEdges(this.faces, false);

        for (Iterator iter = facesAtEdge.keySet().iterator(); iter.hasNext();) {
            final Edge e = (Edge) iter.next();
            final Point p = (Point) this.indexToPos.get(new Integer(e.source));
            final Point q = (Point) this.indexToPos.get(new Integer(e.target));
            final Vector a = Vector.unit((Vector) q.plus(e.shift).minus(p));
            
            // --- augment all incidences at this edge with their angles
            final List incidences = (List) facesAtEdge.get(e);
            Vector n0 = null;
            for (int i = 0; i < incidences.size(); ++i) {
                final Incidence inc = (Incidence) incidences.get(i);
                Vector normal = ((Vector[]) normals.get(inc.face))[inc.index];
                if (inc.reverse) {
                    normal = (Vector) normal.negative();
                }
                double angle;
                if (i == 0) {
                    n0 = normal;
                    angle = 0.0;
                } else {
                    double x = ((Real) Vector.dot(n0, normal)).doubleValue();
                    x = Math.max(Math.min(x, 1.0), -1.0);
                    angle = Math.acos(x);
                    if (Vector.volume3D(a, n0, normal).isNegative()) {
                        angle = 2 * Math.PI - angle;
                    }
                }
                incidences.set(i, new Incidence(inc, angle));
            }
            if (DEBUG) {
                System.err.println("Augmented incidences at edge " + e + ":");
                for (int i = 0; i < incidences.size(); ++i) {
                    System.err.println("    " + incidences.get(i));
                }
            }
            
            // --- sort by angle
            Collections.sort(incidences);
            
            // --- top off with a copy of the first incidences
            final Incidence inc = (Incidence) incidences.get(0);
            incidences.add(new Incidence(inc, inc.angle + 2 * Math.PI));
            if (DEBUG) {
                System.err.println("Sorted incidences at edge " + e + ":");
                for (int i = 0; i < incidences.size(); ++i) {
                    System.err.println("    " + incidences.get(i));
                }
            }
            
            // --- now set all the connections around this edge
            for (int i = 0; i < incidences.size() - 1; ++i) {
                final Incidence inc1 = (Incidence) incidences.get(i);
                final Incidence inc2 = (Incidence) incidences.get(i + 1);
                if (inc2.angle - inc1.angle < 1e-3) {
                    throw new RuntimeException("tiny dihedral angle");
                }
                final List elms1 = (List) faceElms.get(inc1.face);
                final List elms2 = (List) faceElms.get(inc2.face);
                
                final Object A, B, C, D;
                if (inc1.reverse) {
                    final int k = 2 * (inc1.index + inc1.face.size());
                    A = elms1.get(k + 1);
                    B = elms1.get(k);
                } else {
                    final int k = 2 * inc1.index;
                    A = elms1.get(k);
                    B = elms1.get(k + 1);
                }
                if (inc2.reverse) {
                    final int k = 2 * inc2.index;
                    C = elms2.get(k + 1);
                    D = elms2.get(k);
                } else {
                    final int k = 2 * (inc2.index + inc2.face.size());
                    C = elms2.get(k);
                    D = elms2.get(k + 1);
                }
                ds.redefineOp(2, A, C);
                ds.redefineOp(2, B, D);
            }
        }
    }
}
