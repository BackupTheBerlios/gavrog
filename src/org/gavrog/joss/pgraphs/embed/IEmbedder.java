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

package org.gavrog.joss.pgraphs.embed;

import java.util.Map;

import org.gavrog.jane.compounds.Matrix;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.pgraphs.basic.INode;

/**
 * @author Olaf Delgado
 * @version $Id: IEmbedder.java,v 1.2 2006/03/18 06:23:13 odf Exp $
 */
public interface IEmbedder {
    public double[] edgeStatistics();

    public void normalize();
    
    public void reset();

    public int go(final int n);

    public void setPositions(final Map map);
    
    public void setPosition(final INode v, final Point p);

    public Map getPositions();
    
    public Point getPosition(final INode v);

    public void setGramMatrix(final Matrix gramMatrix);

    public Matrix getGramMatrix();

    public boolean getRelaxPositions();

    public void setRelaxPositions(boolean value);
    
    public boolean cellRelaxed();
    
    public boolean positionsRelaxed();
}