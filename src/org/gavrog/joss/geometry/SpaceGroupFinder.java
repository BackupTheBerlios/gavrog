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

package org.gavrog.joss.geometry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * Takes a two- or three-dimensional crystallographic group and identifies it,
 * producing its name as according to the international tables for
 * Crystallography.
 * 
 * @author Olaf Delgado
 * @version $Id: SpaceGroupFinder.java,v 1.3 2005/09/20 05:14:17 odf Exp $
 */
public class SpaceGroupFinder {
    private SpaceGroup G;
    
    /**
     * Constructs a new instance.
     * 
     * @param G the group to identify.
     */
    public SpaceGroupFinder(final SpaceGroup G) {
        final int d = G.getDimension();
        if (d != 2 && d != 3) {
            final String msg = "group dimension is " + d + ", must be 2 or 3";
            throw new UnsupportedOperationException(msg);
        }
        this.G = G;
    }
    
    /**
     * Returns a map with the occuring operator types as keys and the sets of
     * all operators of the respective types as values.
     * 
     * @return a map assigning operators types to operator sets.
     */
    Map operatorsByType() {
        final Map res = new HashMap();
        for (final Iterator iter = G.getOperators().iterator(); iter.hasNext();) {
            final Operator op = (Operator) iter.next();
            final OperatorType type = new OperatorType(op);
            if (!res.containsKey(type)) {
                res.put(type, new HashSet());
            }
            ((Set) res.get(type)).add(op);
        }
        
        return res;
    }
}
