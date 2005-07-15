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

package org.gavrog.box;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to hold a Delaney symbol invariant.
 * 
 * @author Olaf Delgado
 * @version $Id: Invariant.java,v 1.1.1.1 2005/07/15 21:58:38 odf Exp $
 */
public class Invariant extends ArrayList implements Comparable {

    /**
     * Construct an instance.
     * @param model the contents of the new instance.
     */
    public Invariant(final List model) {
        super(model);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final Object arg) {
        if (!(arg instanceof Invariant)) {
            throw new IllegalArgumentException("argument must be of type Invariant");
        }
        final Invariant other = (Invariant) arg;
        for (int i = 0; i < Math.min(this.size(), other.size()); ++i) {
            final int a = ((Integer) this.get(i)).intValue();
            final int b = ((Integer) other.get(i)).intValue();
            if (a != b) {
                if (a == 0) {
                    return 1;
                } else if (b == 0) {
                    return -1;
                } else {
                    return a - b;
                }
            }
        }
        return this.size() - other.size();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        int res = 0;
        for (int i = 0; i < this.size(); ++i) {
            res = res * 157 + this.get(i).hashCode();
        }
        return res;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        final StringBuffer buffer = new StringBuffer(50);
        buffer.append("(");
        for (int i = 0; i < this.size(); ++i) {
            if (i > 0) {
                buffer.append(", ");
            }
            buffer.append(this.get(i));
        }
        buffer.append(")");
        return buffer.toString();
    }
}
