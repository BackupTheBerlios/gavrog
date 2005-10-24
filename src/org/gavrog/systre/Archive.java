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

import java.io.BufferedReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.gavrog.box.simple.DataFormatException;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;

/**
 * A class to represent an archive of periodic nets.
 * 
 * @author Olaf Delgado
 * @version $Id: Archive.java,v 1.4 2005/10/24 22:57:52 odf Exp $
 */
public class Archive {
    final String keyVersion;
    final private Map byKey;
    final private Map byName;
    
    /**
     * Represents an individual archive entry.
     */
    public static class Entry {
        final static char hexDigit[] = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    
        final private String key;
        final private String keyVersion;
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
            this.keyVersion = version;
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
        public String getKeyVersion() {
            return keyVersion;
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
                buf.append(keyVersion);
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
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            final StringBuffer buf = new StringBuffer(200);
            buf.append("key      ");
            buf.append(getKey());
            buf.append("\n");
            buf.append("version  ");
            buf.append(getKeyVersion());
            buf.append("\n");
            buf.append("id       ");
            buf.append(getName());
            buf.append("\n");
            buf.append("checksum ");
            buf.append(getDigestString());
            buf.append("\n");
            buf.append("end\n");
            return buf.toString();
        }
        
        /**
         * Reads an entry from a stream.
         * @param input represents the input stream.
         * @return the entry read or null if the stream is at its end.
         */
        public static Entry read(final BufferedReader input) {
            String line;
            final Map fields = new HashMap();
            while (true) {
                try {
                    line = input.readLine();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                if (line == null) {
                    break;
                }
                line = line.trim().replaceAll("\\s+", " ");
                if (line.length() == 0) {
                    continue;
                }
                int k = line.indexOf(" ");
                final String tag;
                final String arg;
                if (k < 0) {
                    tag = line;
                    arg = null;
                } else {
                    tag = line.substring(0, k);
                    arg = line.substring(k + 1);
                }
                if (tag.equals("end")) {
                    final String key = (String) fields.get("key");
                    final String version = (String) fields.get("version");
                    final String name = (String) fields.get("id");
                    final String checksum = (String) fields.get("checksum");
                    final Entry entry = new Entry(key, version, name);
                    if (!entry.getDigestString().equals(checksum)) {
                        throw new DataFormatException("checksum does not match");
                    }
                    return entry;
                } else {
                    fields.put(tag, arg);
                }
            }
            return null;
        }
    }
    
    /**
     * Creates a new empty instance.
     * 
     * @param keyVersion the key creation version to be used for this archive.
     */
    public Archive(final String keyVersion) {
        this.keyVersion = keyVersion;
        this.byKey = new HashMap();
        this.byName = new HashMap();
    }
    
    /**
     * Returns the number of entries in this archive.
     * @return the number of entries.
     */
    public int size() {
        return this.byKey.size();
    }
    
    /**
     * @return Returns the version string.
     */
    public String getKeyVersion() {
        return keyVersion;
    }
    
    /**
     * Removes all entries from this archive.
     */
    public void clear() {
        this.byKey.clear();
        this.byName.clear();
    }
    
    /**
     * Adds the given entry to the archive.
     * 
     * @param entry the new entry.
     */
    public void add(final Entry entry) {
        final String version = entry.getKeyVersion();
        final String key = entry.getKey();
        final String name = entry.getName();
        if (!version.equals(getKeyVersion())) {
            throw new IllegalArgumentException("entry has key of version " + version
                                               + ", but " + getKeyVersion()
                                               + " is required.");
        }
        if (this.byKey.containsKey(key)) {
            final String clashing = ((Entry) this.byKey.get(key)).getName();
            throw new IllegalArgumentException("duplicates key for structure " + clashing);
        }
        if (this.byName.containsKey(name)) {
            throw new IllegalArgumentException("we already have a structure " + name);
        }
        this.byKey.put(key, entry);
        this.byName.put(name, entry);
    }
    
    /**
     * Adds an entry for a given periodic graph to the archive.
     * 
     * @param G the periodic graph to add.
     * @param name the name to use for that graph.
     */
    public void add(final PeriodicGraph G, final String name) {
        add(new Entry(G, name));
    }
    
    /**
     * Removes an entry.
     * 
     * @param entry the entry to remove.
     */
    public void delete(final Entry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("null argument");
        }
        final String key = entry.getKey();
        final String name = entry.getName();
        if (entry != getByKey(key)) {
            throw new IllegalArgumentException("no such entry");
        }
        this.byKey.remove(key);
        this.byName.remove(name);
    }
    
    /**
     * Retrieves an entry.
     * @param key the key for the entry to get.
     * @return the entry with the given key or null.
     */
    public Entry getByKey(final String key) {
        return (Entry) this.byKey.get(key);
    }
    
    /**
     * Retrieves an entry.
     * @param name the name for the entry to get.
     * @return the entry with the given name or null.
     */
    public Entry getByName(final String name) {
        return (Entry) this.byName.get(name);
    }
    
    /**
     * Retrieves an entry. If an entry exists with the given argument as its
     * key, that entry is returned. Otherwise, the method tries to find an entry
     * with the given argument as its name.
     * 
     * @param keyOrName the key or name to look for.
     * @return the corresponding entry or null.
     */
    public Entry get(final String keyOrName) {
        final Entry e = getByKey(keyOrName);
        if (e != null) {
            return e;
        } else {
            return getByName(keyOrName);
        }
    }
    
    /**
     * Adds all archive entries read from a stream.
     * @param input represents the input stream.
     */
    public void addAll(final BufferedReader input) {
        while (true) {
            final Entry entry = Entry.read(input);
            if (entry == null) {
                return;
            } else {
                add(entry);
            }
        }
    }
}
