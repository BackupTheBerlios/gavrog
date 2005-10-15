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

import java.util.HashMap;
import java.util.Map;

import org.gavrog.joss.geometry.Point;


/**
 * Represents an embedding of a finite graph.
 * 
 * @author Olaf Delgado
 * @version $Id: Embedding.java,v 1.3 2005/10/15 02:20:35 odf Exp $
 */
public class Embedding {
    final private IGraph G;
    final private Map pos;
    
    public Embedding(final IGraph G) {
        this.G = G;
        this.pos = new HashMap();
    }
    
    public IGraph getGraph() {
        return this.G;
    }
    
    public void setPosition(final INode v, final Point p) {
        if (!v.owner().equals(this.G)) {
            throw new IllegalArgumentException("no such node");
        }
        this.pos.put(v, p);
    }
    
    public Point getPosition(final INode v) {
        if (!v.owner().equals(this.G)) {
            throw new IllegalArgumentException("no such node");
        }
        return (Point) this.pos.get(v);
    }
}
