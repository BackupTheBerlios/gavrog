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


package org.gavrog.box.simple;


/**
 * Each instance of this class generates a set of unique objects with no special
 * properties. These can essentially be used as symbolic constants.
 * 
 * @author Olaf Delgado
 * @version $Id: TagSet.java,v 1.1 2007/04/25 21:17:19 odf Exp $
 */
public class TagSet {
    // --- the unique id assigned to the next <code>Tag</code> instance
    private int nextId = 1;
    
    /**
     * Instances of this class are generated, each carrying a unique numeric id.
     */
    private class Tag {
        // --- the id of this instance
        final private int id;
        
        /**
         * Constructs an instance.
         */
        private Tag() {
            this.id = nextId++;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        public int hashCode() {
            return this.id;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Comparable#compareTo(T)
         */
        public int compareTo(final Object other) {
            if (other instanceof Tag) {
                return this.id - ((Tag) other).id;
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    /**
     * Creates a new object by calling the private constructor of the internal
     * <code>Tag</code> class.
     * 
     * @return the new unique object.
     */
    public Object newTag() {
        return new Tag();
    }
    
    public static void main(final String args[]) {
        final TagSet tagSet1 = new TagSet();
        final TagSet tagSet2 = new TagSet();
        final Object t11 = tagSet1.newTag();
        final Object t12 = tagSet1.newTag();
        final Object t21 = tagSet2.newTag();
        final Object t22 = tagSet2.newTag();
        
        System.out.println(t11.hashCode() + " " + t12.hashCode() + " "
                + t21.hashCode() + " " + t22.hashCode());
    }
}
