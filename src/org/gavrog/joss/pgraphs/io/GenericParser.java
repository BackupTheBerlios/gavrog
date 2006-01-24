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

package org.gavrog.joss.pgraphs.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.simple.DataFormatException;
import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.Fraction;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Whole;


/**
 * @author Olaf Delgado
 * @version $Id: GenericParser.java,v 1.4 2006/01/24 22:46:43 odf Exp $
 */
public class GenericParser {
    private BufferedReader input;
    protected Map synonyms;
    protected String defaultKey;
    private int lineno;
    private String dataType;
    private Entry dataEntries[];
    private Map entriesByKey;

    public class Entry {
        public final int lineNumber;
        public final String key;
        public final List values;
        
        public Entry(final int lineNumber, final String key, final List values) {
            this.lineNumber = lineNumber;
            this.key = key;
            this.values = values;
        }
    }
    
    public GenericParser(final BufferedReader input) {
        this.input = input;
        this.lineno = 0;
        this.synonyms = null;
        this.defaultKey = null;
    }
    
    public GenericParser(final Reader input) {
        this(new BufferedReader(input));
    }
    
    private LinkedList nextLineChopped() {
        while (true) {
            final String rawLine;
            try {
                rawLine = this.input.readLine();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            if (rawLine == null) {
                return null;
            }
            ++this.lineno;
            final String line = rawLine.trim();
            if (line.length() == 0 || line.charAt(0) == '#') {
                continue;
            }

            final LinkedList fields = new LinkedList();
            int i = 0;
            while (i < line.length()) {
                while (i < line.length() && Character.isWhitespace(line.charAt(i))) {
                    ++i;
                }
                if (i >= line.length()) {
                    break;
                }
                int j = i;
                if (line.charAt(i) == '"') {
                    ++j;
                    while (j < line.length() && line.charAt(j) != '"') {
                        ++j;
                    }
                    if (line.charAt(j) == '"') {
                        ++j;
                    } else {
                        final String msg = "no closing quotes at line ";
                        throw new DataFormatException(msg + this.lineno);
                    }
                    if (j < line.length()) {
                        final char c = line.charAt(j);
                        if (!Character.isWhitespace(c) && c != '#') {
                            final String msg = "missing space after string at line ";
                            throw new DataFormatException(msg + this.lineno);
                        }
                    }
                } else {
                    while (j < line.length()) {
                        final char c = line.charAt(j);
                        if (Character.isWhitespace(c) || c == '#') {
                            break;
                        }
                        ++j;
                    }
                }
                fields.add(line.substring(i, j));
                i = j;
                if (i < line.length() && line.charAt(i) == '#') {
                    break;
                }
            }
            
            return fields;
        }
    }
    
    public void parseDataBlock() {
        this.dataEntries = null;
        this.entriesByKey = null;
        
        final LinkedList fields0 = nextLineChopped();
        if (fields0 == null) {
            return;
        }
        this.dataType = ((String) fields0.getFirst()).toLowerCase();
        final List result = new LinkedList();
        final Map byKey = new HashMap();
        String key = this.defaultKey;
        
        while (true) {
            final LinkedList fields = nextLineChopped();
            if (fields == null) {
                throw new DataFormatException("end of file while reading block");
            }
            final String first = (String) fields.getFirst();
            if (Character.isLetter(first.charAt(0))) {
                if (first.equalsIgnoreCase("END")) {
                    break;
                }
                key = first.toLowerCase();
                if (this.synonyms != null) {
                    while (this.synonyms.containsKey(key)) {
                        key = (String) this.synonyms.get(key);
                    }
                }
                fields.removeFirst();
            }
            
            final LinkedList row = new LinkedList();
            for (final Iterator iter = fields.iterator(); iter.hasNext();) {
                final String item = (String) iter.next();
                final char c = item.charAt(0);
                if (c == '"') {
                    row.add(item.substring(1, item.length() - 1));
                } else if (Character.isDigit(c) || "+-.".indexOf(c) >= 0) {
                    IArithmetic number = null;
                    if (item.indexOf('/') > 0) {
                        final String parts[] = item.split("/");
                        if (parts.length == 2) {
                            final int n;
                            final int d;
                            try {
                                n = Integer.parseInt(parts[0]);
                                d = Integer.parseInt(parts[1]);
                                number = new Fraction(n, d);
                            } catch (NumberFormatException ex) {
                            }
                        }
                    } else {
                        try {
                            number = new Whole(Integer.parseInt(item));
                        } catch (NumberFormatException ex1) {
                            try {
                                number = new FloatingPoint(Double.parseDouble(item));
                            } catch (NumberFormatException ex2) {
                            }
                        }
                    }
                    if (number != null) {
                        row.add(number);
                    } else {
                        row.add(item);
                    }
                } else {
                    row.add(item);
                }
            }
            if (key != null) {
                if (row.size() > 0) {
                    final Entry entry = new Entry(this.lineno, key, row);
                    result.add(entry);
                    if (!byKey.containsKey(key)) {
                        byKey.put(key, new LinkedList());
                    }
                    ((List) byKey.get(key)).add(entry);
                }
            } else {
                final String msg = "keyless data found at line ";
                throw new DataFormatException(msg + lineno);
            }
        }
        
        this.dataEntries = new Entry[result.size()];
        result.toArray(this.dataEntries);
        this.entriesByKey = byKey;
    }
    
    /**
     * @return the type of the block last parsed.
     */
    public String getDataType() {
        return this.dataType;
    }

    /**
     * @return the last line number.
     */
    public int getLineNumber() {
        return this.lineno;
    }
    
    /**
     * Retrieves the current map of entry key synonyms.
     * 
     * @return the current synonyms map.
     */
    public Map getSynonyms() {
        return synonyms;
    }
    
    /**
     * Sets the map of entry key synonyms.
     * 
     * @param synonyms the new synonyms map.
     */
    public void setSynonyms(Map synonyms) {
        this.synonyms = synonyms;
    }
    
    /**
     * Retrieves the current default for keyless entries.
     * 
     * @return the current default key.
     */
    public String getDefaultKey() {
        return defaultKey;
    }
    /**
     * Set a new default for keyless entries.
     * @param defaultKey the new default key.
     */
    public void setDefaultKey(String defaultKey) {
        this.defaultKey = defaultKey;
    }
    
    /**
     * @return the data entries as they occur in the current block.
     */
    public Entry[] getDataEntries() {
        return dataEntries;
    }
    
    /**
     * Retrieves entries for a specific key only.
     * 
     * @param key the key to look up.
     * @return the entries for the given key.
     */
    public List getDataEntries(final String key) {
        final List entries = (List) entriesByKey.get(key);
        if (entries == null) {
            return null;
        } else {
            return Collections.unmodifiableList((List) entriesByKey.get(key));
        }
    }
    
    /**
     * Retrieves entries for a specific key as a single string.
     * 
     * @param key the key to look up.
     * @return the entries for the given key, concatenated into a single string.
     */
    public String getDataEntriesAsString(final String key) {
        final List entries = getDataEntries(key);
        if (entries == null) {
            return null;
        } else {
            final StringBuffer buf = new StringBuffer(20);
            for  (int i = 0; i < entries.size(); ++i) {
                final Entry entry = (Entry) entries.get(i);
                final List values = entry.values;
                if (i > 0) {
                    buf.append("; ");
                }
                for (int j = 0; j < values.size(); ++j) {
                    if (j > 0) {
                        buf.append(" ");
                    }
                    buf.append(String.valueOf(values.get(j)));
                }
            }
            return buf.toString();
        }
    }
    
    /**
     * Retrieves all keys present in the current data block.
     * @return the set of keys.
     */
    public Set getKeys() {
        return this.entriesByKey.keySet();
    }
}
