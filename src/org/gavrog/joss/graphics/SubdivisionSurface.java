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


package org.gavrog.joss.graphics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Implements Catmull-Clark subdivision surfaces.
 * 
 * @author Olaf Delgado
 * @version $Id: SubdivisionSurface.java,v 1.7 2007/06/02 23:47:55 odf Exp $
 */
public class SubdivisionSurface {
    final public double[][] vertices;
    final public int[][] faces;
    final public int[] fixed;
    public Object[] tags;
    public double[][] faceNormals;
    public double[][] vertexNormals;
    
    public SubdivisionSurface(final double[][] vertices, final int[][] faces,
            final int fixed[], final Object tag[]) {
        this.vertices = (double[][]) vertices.clone();
        this.faces = (int[][]) faces.clone();
        this.fixed = (int[]) fixed.clone();
        if (tag == null) {
            this.tags = null;
        } else {
            this.tags = (Object[]) tag.clone();
        }
    }
    
    public SubdivisionSurface(final double[][] vertices, final int[][] faces,
            final int fixed[]) {
        this(vertices, faces, fixed, null);
    }
    
    public double[][] getFaceNormals() {
        if (this.faceNormals == null) {
            final int nf = this.faces.length;
            this.faceNormals = new double[nf][3];
            for (int i = 0; i < nf; ++i) {
                final int[] face = this.faces[i];
                final int n = face.length;
                final double normal[] = new double[] { 0.0, 0.0, 0.0 };
                for (int j = 0; j < n; ++j) {
                    final double p[] = this.vertices[face[j]];
                    final double q[] = this.vertices[face[(j + 1) % n]];
                    Vec.plus(normal, normal, Vec.crossProduct(null, p, q));
                }
                // --- normalize both vectors
                Vec.normalized(this.faceNormals[i], normal);
            }
        }
        return (double[][]) this.faceNormals.clone();
    }
    
    public double[][] getVertexNormals() {
        if (this.vertexNormals == null) {
            getFaceNormals();
            final int nv = this.vertices.length;
            final int nf = this.faces.length;
            this.vertexNormals = new double[nv][3];
            
            for (int i = 0; i < nf; ++i) {
                final int face[] = this.faces[i];
                final double normal[] = this.faceNormals[i];
                final int n = face.length;
                for (int j = 0; j < n; ++j) {
                    final int v = face[j];
                    Vec.plus(this.vertexNormals[v], this.vertexNormals[v],
                            normal);
                }
            }
            for (int i = 0; i < nv; ++i) {
                Vec.normalized(this.vertexNormals[i], this.vertexNormals[i]);
            }
        }
        return (double[][]) this.vertexNormals.clone();
    }
    
    public void tagAll(final Object tag) {
        if (this.tags == null) {
            this.tags = new Object[this.faces.length];
        }
        for (int i = 0; i < this.faces.length; ++i) {
            this.tags[i] = tag;
        }
    }

    public static SubdivisionSurface concatenation(
            final SubdivisionSurface parts[]) {
        int newNF = 0;
        int newNV = 0;
        for (int i = 0; i < parts.length; ++i) {
            newNF += parts[i].faces.length;
            newNV += parts[i].vertices.length;
        }
        final double newVerts[][] = new double[newNV][];
        final int newFixed[] = new int[newNV];
        final int newFaces[][] = new int[newNF][];
        final Object newTags[] = new Object[newNF];
        
        int offsetV = 0;
        int offsetF = 0;
        for (int i = 0; i < parts.length; ++i) {
            final double verts[][] = parts[i].vertices;
            final int faces[][] = parts[i].faces;
            final int nv = verts.length;
            final int nf = faces.length;
            System.arraycopy(verts, 0, newVerts, offsetV, nv);
            System.arraycopy(parts[i].fixed, 0, newFixed, offsetV, nv);
            System.arraycopy(parts[i].tags, 0, newTags, offsetF, nf);
            for (int j = 0; j < faces.length; ++j) {
                final int face[] = faces[j];
                final int newFace[] = new int[face.length];
                newFaces[j + offsetF] = newFace;
                for (int k = 0; k < face.length; ++k) {
                    newFace[k] = face[k] + offsetV;
                }
            }
            offsetV += nv;
            offsetF += nf;
        }
        return new SubdivisionSurface(newVerts, newFaces, newFixed, newTags);
    }
    
    private boolean equal(final Object a, final Object b) {
        if (a == null) {
            return b == null;
        } else {
            return a.equals(b);
        }
    }
    
    public SubdivisionSurface extract(final Object tag) {
        if (this.tags == null) {
            return null;
        }
        
        final int nf = this.faces.length;
        final int nv = this.vertices.length;
        
        // --- determine which vertices appear on faces with the right tag
        final boolean usedV[] = new boolean[nv];
        final boolean usedF[] = new boolean[nf];
        int newNV = 0;
        int newNF = 0;
        for (int i = 0; i < nf; ++i) {
            if (equal(this.tags[i], tag)) {
                final int face[] = this.faces[i];
                for (int j = 0; j < face.length; ++j) {
                    final int v = face[j];
                    if (!usedV[v]) {
                        ++newNV;
                    }
                    usedV[v] = true;
                }
                usedF[i] = true;
                ++newNF;
            }
        }
        
        // --- collect used vertices and map old vertex numbers to new ones
        final int mapV[] = new int[nv];
        final double newVerts[][] = new double[newNV][3];
        final int newFixed[] = new int[newNV];
        final double newVNormals[][];
        if (this.vertexNormals == null) {
            newVNormals = null;
        } else {
            newVNormals = new double[newNV][3];
        }
        int k = 0;
        for (int i = 0; i < nv; ++i) {
            if (usedV[i]) {
                mapV[i] = k;
                Vec.copy(newVerts[k], this.vertices[i]);
                if (newVNormals != null) {
                    Vec.copy(newVNormals[k], this.vertexNormals[i]);
                }
                newFixed[k] = this.fixed[i];
                ++k;
            }
        }
        
        // --- map and collect faces
        final int newFaces[][] = new int[newNF][];
        final Object newTags[] = new Object[newNF];
        final double newFNormals[][];
        if (this.faceNormals == null) {
            newFNormals = null;
        } else {
            newFNormals = new double[newNF][3];
        }
        k = 0;
        for (int i = 0; i < nf; ++i) {
            if (usedF[i]) {
                final int face[] = this.faces[i];
                final int newFace[] = new int[face.length];
                for (int j = 0; j < face.length; ++j) {
                    newFace[j] = mapV[face[j]];
                }
                newFaces[k] = newFace;
                if (newFNormals != null) {
                    Vec.copy(newFNormals[k], this.faceNormals[i]);
                }
                newTags[k] = tag;
                ++k;
            }
        }
        
        final SubdivisionSurface surf = new SubdivisionSurface(newVerts,
                newFaces, newFixed, newTags);
        surf.vertexNormals = newVNormals;
        surf.faceNormals = newFNormals;
        return surf;
    }
    
    public SubdivisionSurface nextLevel() {
        // --- shortcuts
        final int nv = this.vertices.length;
        final int nf = this.faces.length;

        // --- initialize array to hold map from edges to running numbers
        final int[][] edgeToIndex = new int[nv][nv];
        for (int i = 0; i < nv; ++i) {
            for (int j = 0; j < nv; ++j) {
                edgeToIndex[i][j] = -1;
            }
        }
        
        // --- count edges and map endpoints to running numbers
        int ne = 0;
        int neInterior = 0;
        for (int i = 0; i < nf; ++i) {
            final int[] face = this.faces[i];
            final int n = face.length;
            for (int j = 0; j < n; ++j) {
                final int v = face[j];
                final int w = face[(j + 1) % n];
                if (edgeToIndex[v][w] < 0) {
                    edgeToIndex[v][w] = edgeToIndex[w][v] = ne;
                    ++ne;
                } else {
                    ++neInterior;
                }
            }
        }
        
        // --- create arrays for new surface
        final double newVertices[][] = new double[nf + ne + nv][3];
        final int newFaces[][] = new int[ne + neInterior][4];
        final int newFixed[] = new int[newVertices.length];
        final Object newTags[];
        if (this.tags != null) {
            newTags = new Object[newFaces.length];
        } else {
            newTags = null;
        }
        
        // --- make the new faces
        int facesMade = 0;
        for (int i = 0; i < nf; ++i) {
            final int[] face = this.faces[i];
            final int n = face.length;
            for (int j = 0; j < n; ++j) {
                final int u = face[j];
                final int v = face[(j + 1) % n];
                final int w = face[(j + 2) % n];
                final int k = edgeToIndex[u][v];
                final int k1 = edgeToIndex[v][w];
                newFaces[facesMade] = new int[] { i, nf + k, nf + ne + v,
                        nf + k1 };
                if (newTags != null) {
                    newTags[facesMade] = this.tags[i];
                }
                ++facesMade;
                newFixed[nf + k] = Math.min(this.fixed[u], this.fixed[v]) - 1;
                newFixed[nf + ne + u] = this.fixed[u] - 1;
            }
        }
        
        // --- create arrays to hold temporary data
        final double[][] vertexTmp = new double[newVertices.length][3];
        final int[] vertexDeg = new int[newVertices.length];
        
        // --- find positions for face points
        for (int k = 0; k < newFaces.length; ++k) {
        	final int[] face = newFaces[k];
            final double p[] = vertexTmp[face[0]];
            final double q[] = this.vertices[(face[2] - nf - ne)];
            p[0] += q[0];
            p[1] += q[1];
            p[2] += q[2];
            ++vertexDeg[face[0]];
        }
        for (int i = 0; i < nf; ++i) {
            final double p[] = newVertices[i];
            final double q[] = vertexTmp[i];
            final int d = vertexDeg[i];
            p[0] = q[0] / d;
            p[1] = q[1] / d;
            p[2] = q[2] / d;
        }
        
        // --- find positions for edge points
        for (int k = 0; k < newFaces.length; ++k) {
        	final int[] face = newFaces[k];
            final int e1 = face[1];
            final int e2 = face[3];
            final double p1[] = vertexTmp[e1];
            final double p2[] = vertexTmp[e2];
            final double q1[] = this.vertices[face[2] - nf - ne];
            final double q2[] = newVertices[face[0]];
            for (int i = 0; i < 3; ++i) {
                p1[i] += q1[i];
                p2[i] += q1[i];
            }
            ++vertexDeg[e1];
            ++vertexDeg[e2];
            if (newFixed[e1] < 0) {
                for (int i = 0; i < 3; ++i) {
                    p1[i] += q2[i];
                }
                ++vertexDeg[e1];
            }
            if (newFixed[e2] < 0) {
                for (int i = 0; i < 3; ++i) {
                    p2[i] += q2[i];
                }
                ++vertexDeg[e2];
            }
        }
        for (int i = 0; i < ne; ++i) {
            final double p[] = newVertices[nf + i];
            final double q[] = vertexTmp[nf + i];
            final int d = vertexDeg[nf + i];
            p[0] = q[0] / d;
            p[1] = q[1] / d;
            p[2] = q[2] / d;
        }
        
        // --- adjust positions for original vertices
        for (int k = 0; k < newFaces.length; ++k) {
        	final int[] face = newFaces[k];
            final int v = face[2];
            if (newFixed[v] >= 0) {
                continue;
            }
            final double[] p = vertexTmp[v];
            final double[] r = newVertices[face[0]];
            final double[] q1 = newVertices[face[1]];
            final double[] q2 = newVertices[face[3]];
            for (int nu = 0; nu < 3; ++nu) {
                p[nu] += 2 * q1[nu] + 2 * q2[nu] - r[nu];
            }
            ++vertexDeg[v];
        }
        for (int i = 0; i < nv; ++i) {
            final int v = nf + ne + i;
            final double p[] = newVertices[v];
            final double q[] = this.vertices[i];
            p[0] = q[0];
            p[1] = q[1];
            p[2] = q[2];
            if (newFixed[v] < 0) {
                final double[] r = vertexTmp[v];
                final int d = vertexDeg[v];
                for (int nu = 0; nu < 3; ++nu) {
                    p[nu] = ((d - 3) * p[nu] + r[nu] / d) / d;
                }
            }
        }
        
        return new SubdivisionSurface(newVertices, newFaces, newFixed, newTags);
    }

    public static SubdivisionSurface fromOutline(final double corners[][],
    		final int fixBorder) {
    	final List vertices = new ArrayList();
    	final List faces = new ArrayList();
    	final double tmp[] = new double[3];
    	int startInner = 0;
    	for (int i = 0; i < corners.length; ++i) {
    		vertices.add(corners[i]);
    	}
    	
    	while (true) {
            final int startNew = vertices.size();
			final int n = startNew - startInner;
			if (n <= 4) {
				break;
			}
			
            // --- compute an average normal vector and face center
			final double normal[] = new double[3];
			final double center[] = new double[3];
			for (int i = 0; i < n; ++i) {
				Vec.crossProduct(tmp, (double[]) vertices.get(i + startInner),
						(double[]) vertices.get((i + 1) % n + startInner));
				Vec.plus(normal, normal, tmp);
				Vec.plus(center, center, (double[]) vertices.get(i + startInner));
			}
			// --- normalize both vectors
			Vec.normalized(normal, normal);
			Vec.times(center, 1.0 / n, center);

			// --- determine if vertices lie above, on or below the middle plane
			final int upDown[] = new int[n];
			int nOnMiddle = 0;
			for (int i = 0; i < n; ++i) {
				Vec.minus(tmp, (double[]) vertices.get(i + startInner),
						center);
				final double d0 = Vec.innerProduct(normal, tmp);
				if (d0 > 0.01) {
					upDown[i] = 1;
                } else if (d0 < -0.01) {
                    upDown[i] = -1;
                } else {
                    upDown[i] = 0;
                    ++nOnMiddle;
                }
            }
            if (nOnMiddle == n) {
                break;
            }

            // --- determine where the middle plane is crossed
            final boolean changes[] = new boolean[n];
            int nChanging = 0;
            for (int i0 = 0; i0 < n; ++i0) {
                int i1 = (i0 + 1) % n;
                if (upDown[i0] != upDown[i1]) {
                    changes[i0] = true;
                    ++nChanging;
                } else {
                    changes[i0] = false;
                }
            }
            if (nChanging < 4) {
                break;
            }
            
            // --- add inner vertices
            final int back[] = new int[startNew + n];
			final int forw[] = new int[startNew + n];
			for (int i = startInner; i < back.length; ++i) {
				back[i] = forw[i] = -1;
			}
			
			for (int i0 = 0; i0 < n; ++i0) {
				if (i0 == 0 && upDown[i0] == 0) {
					continue;
				}
				final int i1 = (i0 + 1) % n;
                final int k = vertices.size();
                final double c[] = new double[3];
                final int a;
                final int b;
				if (changes[i0]) {
					if (changes[i1] && upDown[i1] == 0) {
						a = i0;
						b = (i1 + 1) % n;
						Vec.linearCombination(tmp, 0.5, (double[]) vertices
								.get(i0 + startInner), 0.5, (double[]) vertices
								.get(i1 + startInner));
						Vec.linearCombination(c, 0.667, tmp, 0.333,
								(double[]) vertices.get(b + startInner));
						++i0;
					} else {
						a = i0;
						b = i1;
						Vec.linearCombination(c, 0.5, (double[]) vertices
								.get(i0 + startInner), 0.5, (double[]) vertices
								.get(i1 + startInner));
					}
				} else if (!changes[i1]) {
					a = b = i1;
					Vec.copy(c, (double[]) vertices.get(i1 + startInner));
				} else {
					continue;
                }
				back[k] = a + startInner;
				forw[k] = b + startInner;
				forw[a + startInner] = k;
				back[b + startInner] = k;
                final double v[] = new double[3];
                Vec.minus(tmp, c, center);
                Vec.complementProjection(tmp, tmp, normal);
                Vec.linearCombination(tmp, 0.5, tmp, 1, center);
                Vec.linearCombination(v, 0.667, tmp, 0.333, c);
                vertices.add(v);
			}
			
            // --- add new faces pointing outward
			for (int i = startInner; i < startNew; ++i) {
				if (back[i] >= 0 && forw[i] >= 0) {
					final int b = back[i];
					final int f = forw[i];
					if (b != f) {
						faces.add(new int[] { i, f, b });
					}
				}
			}
            
            // --- add new faces pointing inward
			for (int i = startNew; i < vertices.size(); ++i) {
				if (back[i] >= 0 && forw[i] >= 0) {
					final int b = back[i];
					final int f = forw[i];
					if (b != f) {
						final int m = (b + 1 - startInner) % n + startInner;
						if (m == f) {
							faces.add(new int[] { i, b, f });
						} else {
							final double u[] = new double[3];
							final double v[] = new double[3];
							final double w[] = new double[3];
							Vec.minus(u, (double[]) vertices.get(b),
									(double[]) vertices.get(m));
							Vec.minus(v, (double[]) vertices.get(i),
									(double[]) vertices.get(m));
							Vec.minus(w, (double[]) vertices.get(f),
									(double[]) vertices.get(m));
							final double angle = Vec.angle(u, v)
									+ Vec.angle(v, w);
							if (angle > 0.75 * Math.PI) {
								faces.add(new int[] { i, b, m });
								faces.add(new int[] { i, m, f });
							} else {
								faces.add(new int[] { i, b, m, f });
							}
						}
					}
				}
			}
            
            // --- add remaining new faces
			for (int i0 = startInner; i0 < startNew; ++i0) {
                if (forw[i0] < 0 || forw[i0] == back[i0]) {
					if (back[i0] < 0) {
						continue;
					}
					final int i1 = (i0 + 1 - startInner) % n + startInner;
					final int b = back[i0];
					final int f;
					if (back[i1] >= 0) {
						f = back[i1];
					} else {
						f = forw[i1];
					}
					faces.add(new int[] { i0, i1, f, b });
				}
			}
			
			// --- prepare for the next step if any
            final int x = vertices.size() - startNew;
			startInner = startNew;
			if (x == n) {
				break;
			}
    	}
    	// --- add final inner face
    	final int inner[] = new int[vertices.size() - startInner];
    	for (int i = startInner; i < vertices.size(); ++i) {
    		inner[i - startInner] = i;
    	}
    	faces.add(inner);

    	// --- construct a subdivision surface to return
    	final double pos[][] = new double[vertices.size()][];
    	vertices.toArray(pos);
    	final int idcs[][] = new int[faces.size()][];
    	faces.toArray(idcs);
    	final int fixed[] = new int[vertices.size()];
    	for (int i = 0; i < corners.length; ++i) {
    		fixed[i] = fixBorder;
    	}
    	
    	return new SubdivisionSurface(pos, idcs, fixed);
	}
    
    public static void main(final String args[]) {
        final double v[][] = { { 0, 0, 0 }, { 0, 0, 1 }, { 0, 1, 0 },
                { 0, 1, 1 }, { 1, 0, 0 }, { 1, 0, 1 }, { 1, 1, 0 }, { 1, 1, 1 } };
        final int f[][] = { { 0, 1, 3, 2 }, { 5, 4, 6, 7 }, { 1, 0, 4, 5 },
                { 2, 3, 7, 6 }, { 0, 2, 6, 4 }, { 3, 1, 5, 7 } };
        final int fixed[] = new int[8];
        final Object tag[] = { "left", "right", "bottom", "top", "back",
                "front" };
        
        SubdivisionSurface surf = new SubdivisionSurface(v, f, fixed, tag);
        surf = surf.nextLevel();
        surf.getVertexNormals();
        final SubdivisionSurface front = surf.extract("front");
        System.out.println("Front:");
        System.out.println("  vertices = " + Arrays.deepToString(front.vertices));
        System.out.println("  normals = " + Arrays.deepToString(front.vertexNormals));
        System.out.println("  faces = " + Arrays.deepToString(front.faces));
        System.out.println("  normals = " + Arrays.deepToString(front.faceNormals));
    }
}
