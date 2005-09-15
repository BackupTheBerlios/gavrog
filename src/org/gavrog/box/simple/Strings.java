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

package org.gavrog.box.simple;

/**
 * Contains some simple utilities for strings.
 * 
 * @author Olaf Delgado
 * @version $Id: Strings.java,v 1.1 2005/09/15 18:33:19 odf Exp $
 */

public class Strings {
    /**
     * Turns a string's first letter to upper case.
     * 
     * @param s the source string.
     * @return the capitalized version.
     */
    public static String capitalized(final String s) {
        if (s.length() > 1) {
            return s.substring(0, 1).toUpperCase() + s.substring(1);
        } else {
            return s.toUpperCase();
        }
    }
}
