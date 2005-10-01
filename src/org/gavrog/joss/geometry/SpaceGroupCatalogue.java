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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.gavrog.box.simple.Strings;
import org.gavrog.joss.pgraphs.io.DataFormatException;

/**
 * This class handles catalogues of known space groups. For the time being, everything
 * here is static and the input files are hardwired.
 * 
 * @author Olaf Delgado
 * @version $Id: SpaceGroupCatalogue.java,v 1.2 2005/10/01 04:12:51 odf Exp $
 */
public class SpaceGroupCatalogue {
    /**
     * Making the constructor private prevents instantiation (I hope).
     */
    private SpaceGroupCatalogue() {
    }
    
    /**
     * This class is used to represent a table of space group settings of a given dimension.
     */
    private static class Table {
        final public int dimension;
        final public Map nameToOps = new HashMap();
        final public Map nameToTransform = new HashMap();
        
        public Table(final int dimension) {
            this.dimension = dimension;
        }
    }
    
    static private Table groupTables[] = new Table[5];
    static private Map aliases = new HashMap();
    
    /**
     * Parses space group settings from a file and stores them statically. Each setting is
     * identified by a name and the transformation used to derive it from the canonical
     * setting of the group, both given in the first input line. The following lines list
     * the operators for the group.
     * 
     * CAVEAT: currently, due to the way the constructors are implemented, a full list of
     * operators must be given. Just a set of generators is not sufficient.
     * 
     * TODO make this accept generator lists
     * 
     * @param filename
     */
    private static void parseGroups(final String filename) {
        final InputStream inStream = ClassLoader.getSystemResourceAsStream(filename);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
    
        Table table = null;
        String currentName = null;
        
        while (true) {
            final String line;
            try {
                line = reader.readLine();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            if (line == null) {
                break;
            }
            if (line.length() == 0 || line.trim().charAt(0) == '#') {
                continue;
            }
            final int i = line.indexOf(' ');
            if (i > 0) {
                if (currentName != null) {
                    final Map map = table.nameToOps;
                    final String key = currentName;
                    map.put(key, Collections.unmodifiableList((List) map.get(key)));
                }

                final String fields[] = line.trim().split("\\s+");
                if (fields[0].equalsIgnoreCase("alias")) {
                    aliases.put(fields[1], fields[2]);
                } else {
                    currentName = fields[0];
                    final Operator T = new Operator(line.substring(i + 1));
                    final int d = T.getDimension();
                    if (groupTables[d] == null) {
                        groupTables[d] = new Table(d);
                    }
                    table = groupTables[d];
                    table.nameToOps.put(currentName, new LinkedList());
                    table.nameToTransform.put(currentName, T);
                }
            } else if (currentName != null) {
                final Operator op = new Operator(line).modZ();
                ((List) table.nameToOps.get(currentName)).add(op);
            } else {
                throw new DataFormatException("error in space group table file");
            }
        }
    }

    /**
     * The name of the file to read space group settings from.
     */
    final private static Package pkg = SpaceGroupCatalogue.class.getPackage();
    final private static String packagePath = pkg.getName().replaceAll("\\.", "/");
    final private static String tablePath = packagePath + "/sgtable.data";
    
    /**
     * Retrieves the list of all known names for group settings for a given dimension.
     * 
     * CAVEAT: a group may have multiple settings, so this method may return more than one
     * name for each individual group.
     * 
     * @param dimension the common dimension of the space groups.
     * @return an iterator over the names of space group settings.
     */
    public static Iterator groupNames(final int dimension) {
        if (groupTables[dimension] == null) {
            parseGroups(tablePath);
        }
    
        return groupTables[dimension].nameToOps.keySet().iterator();
    }

    /**
     * Retrieves information about a given space group setting. The setting is identified
     * by its name. Depending on the value of the <code>getOps</code> parameter, either
     * the operator list for that setting or the transformation used to obtain it from the
     * canonical setting is returned.
     * 
     * @param dim the dimension of the group.
     * @param getOps if true, return the operator list, otherwise, the transformation.
     * @param name the name of the group setting to retrieve.
     * @return the data requested for the given space group setting.
     */
    private static Object retrieve(int dim, final boolean getOps, final String name) {
        if (groupTables[dim] == null) {
            parseGroups(tablePath);
        }
        final Table table = groupTables[dim];
    
        final String parts[] = name.split(":");
        //String base = capitalized(parts[0]);
        String base = parts[0];
        if (aliases.containsKey(base)) {
            base = (String) aliases.get(base);
        }
        final String ext = parts.length > 1 ? Strings.capitalized(parts[1]) : "";
        
        final String candidates[];
        if (base.charAt(0) == 'R') {
            if (ext.equals("R")) {
                candidates = new String[] { base + ":R" };
            } else {
                candidates = new String[] { base + ":H", base + ":R" };
            }
        } else if (ext.equals("1")) {
            candidates = new String[] { base + ":1", base };
        } else {
            candidates = new String[] { base, base + ":2", base + ":1" };
        }
        
        for (int i = 0; i < candidates.length; ++i) {
            final String key = candidates[i];
            if (getOps) {
                if (table.nameToOps.containsKey(key)) {
                    return table.nameToOps.get(key);
                }
            } else {
                if (table.nameToTransform.containsKey(key)) {
                    return table.nameToTransform.get(key);
                }
            }
        }
    
        return null;
    }

    /**
     * Retrieves the list of operators for a given space group setting.
     * 
     * @param dim the dimension of the group.
     * @param name the name of the group setting.
     * @return the list of operators.
     */
    public static List operators(final int dim, final String name) {
        return (List) retrieve(dim, true, name);
    }

    /**
     * Retrieves a transformation to obtain a space group setting from the canonical setting
     * for that group.
     * 
     * @param dim the dimension of the group.
     * @param name the name of the group setting.
     * @return the transformation operator.
     */
    public static Operator transform(final int dim, final String name) {
        return (Operator) retrieve(dim, false, name);
    }
}