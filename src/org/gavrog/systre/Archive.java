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

package org.gavrog.systre;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.gavrog.joss.pgraphs.basic.PeriodicGraph;

/**
 * A class to represent an archive of periodic nets.
 * 
 * @author Olaf Delgado
 * @version $Id: Archive.java,v 1.1 2005/10/24 00:12:50 odf Exp $
 */
public class Archive {
    final private Map entries;
    
    /**
     * Represents an individual archive entry.
     */
    public static class Entry {
        final static char hexDigit[] = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    
        final private String key;
        final private String version;
        final private String name;
        
        /**
         * Constructs an entry with explicit values.
         * 
         * @param key the invariant key describing the structure.
         * @param version the version of the key generation process used.
         * @param name the name of the structure.
         */
        public Entry(final String key, final String version, final String name) {
            this.key = key;
            this.version = version;
            this.name = name;
        }
        
        /**
         * Constructs an entry representing a periodic graph.
         * 
         * @param G the graph to encode.
         * @param name the name for the graph.
         */
        public Entry(final PeriodicGraph G, final String name) {
            this(G.invariant().toString(), G.invariantVersion, name);
        }
        
        /**
         * @return Returns the key.
         */
        public String getKey() {
            return key;
        }
        
        /**
         * @return Returns the name.
         */
        public String getName() {
            return name;
        }
        
        /**
         * @return Returns the version string.
         */
        public String getVersion() {
            return version;
        }
        
        /**
         * Returns a digest string determined by the "md5" algorithm. This
         * string can be used to assert the integrity of this entry when read
         * from a file.
         * 
         * @return the digest string.
         */
        public String getDigestString() {
            try {
                final MessageDigest md = MessageDigest.getInstance("MD5");
                final StringBuffer buf = new StringBuffer(100);
                buf.append(key);
                buf.append("\n");
                buf.append(version);
                buf.append("\n");
                buf.append(name);
                md.update(buf.toString().getBytes());
                final byte digest[] = md.digest();
                final StringBuffer result = new StringBuffer(digest.length * 2);
                for (int i =0; i < digest.length; ++i) {
                    result.append(hexDigit[(digest[i] & 0xf0) >>> 4]);
                    result.append(hexDigit[digest[i] & 0x0f]);
                }
                return result.toString();
            } catch (GeneralSecurityException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
    
    public Archive() {
        this.entries = new HashMap();
    }
    
    public void clear() {
        this.entries.clear();
    }    
}
