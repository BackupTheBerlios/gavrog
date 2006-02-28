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

package org.gavrog.joss.pgraphs.basic;

import java.util.Map;

import org.gavrog.jane.compounds.Matrix;

/**
 * @author Olaf Delgado
 * @version $Id: AmoebaEmbedder.java,v 1.3 2006/02/28 23:17:30 odf Exp $
 */
public class AmoebaEmbedder extends EmbedderAdapter {

    /**
     * Constructs an instance.
     * 
     * @param graph
     * @param positions
     * @param gramMatrix
     */
    public AmoebaEmbedder(PeriodicGraph graph, Map positions, Matrix gramMatrix) {
        super(graph, positions, gramMatrix);
        // TODO Auto-generated constructor stub
    }

    public AmoebaEmbedder(final PeriodicGraph G) {
        this(G, G.barycentricPlacement(), null);
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.joss.pgraphs.basic.IEmbedder#go(int)
     */
    public int go(int n) {
        // TODO Auto-generated method stub
        return 0;
    }
}
