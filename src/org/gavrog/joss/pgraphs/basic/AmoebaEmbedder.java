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

package org.gavrog.joss.pgraphs.basic;

import java.util.Map;

import org.gavrog.jane.compounds.Matrix;

/**
 * @author Olaf Delgado
 * @version $Id: AmoebaEmbedder.java,v 1.1 2006/02/28 04:51:15 odf Exp $
 */
public class AmoebaEmbedder implements IEmbedder{

    /* (non-Javadoc)
     * @see org.gavrog.joss.pgraphs.basic.IEmbedder#edgeStatistics()
     */
    public double[] edgeStatistics() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.gavrog.joss.pgraphs.basic.IEmbedder#normalize()
     */
    public void normalize() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.gavrog.joss.pgraphs.basic.IEmbedder#steps(int)
     */
    public int steps(int n) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see org.gavrog.joss.pgraphs.basic.IEmbedder#setPositions(java.util.Map)
     */
    public void setPositions(Map map) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.gavrog.joss.pgraphs.basic.IEmbedder#getPositions()
     */
    public Map getPositions() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.gavrog.joss.pgraphs.basic.IEmbedder#setGramMatrix(org.gavrog.jane.compounds.Matrix)
     */
    public void setGramMatrix(Matrix gramMatrix) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.gavrog.joss.pgraphs.basic.IEmbedder#getGramMatrix()
     */
    public Matrix getGramMatrix() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.gavrog.joss.pgraphs.basic.IEmbedder#getOptimizeCell()
     */
    public boolean getOptimizeCell() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.gavrog.joss.pgraphs.basic.IEmbedder#setOptimizeCell(boolean)
     */
    public void setOptimizeCell(boolean optimizeCell) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.gavrog.joss.pgraphs.basic.IEmbedder#getOptimizePositions()
     */
    public boolean getOptimizePositions() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.gavrog.joss.pgraphs.basic.IEmbedder#setOptimizePositions(boolean)
     */
    public void setOptimizePositions(boolean optimizePositions) {
        // TODO Auto-generated method stub
        
    }
}
