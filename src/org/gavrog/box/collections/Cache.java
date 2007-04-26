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


package org.gavrog.box.collections;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author Olaf Delgado
 * @version $Id: Cache.java,v 1.1 2007/04/26 00:59:05 odf Exp $
 */
public class Cache {
    final private Map content;

    public class NotFoundException extends RuntimeException {
    }
    
    /**
     * Constructs an instance.
     * @param content
     */
    public Cache() {
        this.content = new WeakHashMap();
    }

    /**
     * 
     * @see java.util.Map#clear()
     */
    public void clear() {
        this.content.clear();
    }

    /**
     * @param key
     * @return
     * @see java.util.Map#get(java.lang.Object)
     */
    public Object get(final Object key) {
        final Object result = this.content.get(key);
        if (result != null) {
            return result;
        } else {
            throw new NotFoundException();
        }
    }

    /**
     * @param key
     * @return
     * @see java.util.Map#get(java.lang.Object)
     */
    public boolean getBoolean(final Object key) {
        return ((Boolean) this.get(key)).booleanValue();
    }

    /**
     * @param key
     * @param value
     * @return
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public Object put(final Object key, final Object value) {
        this.content.put(key, value);
        return value;
    }

    /**
     * @param key
     * @param value
     * @return
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public boolean put(final Object key, final boolean value) {
        this.content.put(key, new Boolean(value));
        return value;
    }

    /**
     * @param key
     * @return
     * @see java.util.Map#remove(java.lang.Object)
     */
    public Object remove(final Object key) {
        return this.content.remove(key);
    }
}
