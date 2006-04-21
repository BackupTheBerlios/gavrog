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

package org.gavrog.systre;

import java.io.Writer;
import java.lang.reflect.Method;

/**
 * @author Olaf Delgado
 * @version $Id: OutputOption.java,v 1.1 2006/04/21 23:11:45 odf Exp $
 */
public class OutputOption {
    final private String description;
    final private String name;
    final private char key;
    final private Object target;
    final private Method method;
    
    /**
     * Constructs an instance.
     * 
     * @param description
     * @param name
     * @param key
     */
    public OutputOption(final String description, final String name, final char key,
            final Object target, final String method) {
        this.description = description;
        this.name = name;
        this.key = key;
        this.target = target;
        final Class pars[] = new Class[] { Writer.class, ProcessedNet.class };
        try {
            this.method = target.getClass().getMethod(method, pars);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public void write(final Writer writer, final ProcessedNet net) {
        final Object args[] = new Object[] { writer, net };
        try {
            this.method.invoke(this.target, args);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    // --- getters
    public String getDescription() {
        return this.description;
    }

    public char getKey() {
        return this.key;
    }

    public String getName() {
        return this.name;
    }
}
