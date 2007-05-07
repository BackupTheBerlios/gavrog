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

/**
 * Implements Catmull-Clark subdivision surfaces.
 * 
 * @author Olaf Delgado
 * @version $Id: SubdivisionSurface.java,v 1.1 2007/05/07 01:33:22 odf Exp $
 */
public class SubdivisionSurface {
    final public double[][] vertices;
    final public int[][] faces;
    final public boolean[] fixed;
    
    public SubdivisionSurface(final double[][] vertices, final int[][] faces,
            final boolean fixed[]) {
        this.vertices = vertices;
        this.faces = faces;
        this.fixed = fixed;
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
        final double[][] newVertices = new double[nf + ne + nv][3];
        final int[][] newFaces = new int[ne + neInterior][4];
        final boolean[] newFixed = new boolean[newVertices.length];
        
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
                newFaces[facesMade++] = new int[] { i, nf + k,
                        nf + ne + v, nf + k1 };
                newFixed[nf + k] = this.fixed[u] && this.fixed[v];
                newFixed[nf + ne + u] = this.fixed[u];
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
            if (!newFixed[e1]) {
                for (int i = 0; i < 3; ++i) {
                    p1[i] += q2[i];
                }
                ++vertexDeg[e1];
            }
            if (!newFixed[e2]) {
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
            if (newFixed[v]) {
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
            if (!newFixed[v]) {
                final double[] r = vertexTmp[v];
                final int d = vertexDeg[v];
                for (int nu = 0; nu < 3; ++nu) {
                    p[nu] = ((d - 3) * p[nu] + r[nu] / d) / d;
                }
            }
        }
        
        return new SubdivisionSurface(newVertices, newFaces, newFixed);
    }
}
